package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectRemoteDataSource
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSource // ProjectMember 관리를 위해 필요
import com.example.data.model.remote.ProjectDTO
import com.example.domain.model.base.Project
import com.example.domain.repository.ProjectRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import javax.inject.Inject


class ProjectRepositoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource, // ProjectStructure 관리용
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource, // ProjectStructure 관리용
    private val memberRemoteDataSource: MemberRemoteDataSource, // 멤버 관리용
    private val mediaRepository: MediaRepository // 이미지 업로드용
    // private val projectMapper: ProjectMapper // 개별 매퍼 사용시
) : ProjectRepository {

    /**
     * 새로운 프로젝트를 생성합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param name 프로젝트 이름
     * @param description 프로젝트 설명 (선택적)
     * @param ownerId 프로젝트 소유자 ID
     * @param isPublic 공개 여부
     * @param projectImageInputStream 프로젝트 이미지 입력 스트림 (선택적)
     * @param imageMimeType 이미지 MIME 타입 (선택적)
     * @return 생성된 프로젝트 정보
     */
    override suspend fun createProject(
        name: String,
        ownerId: String,
    ): CustomResult<Project, Exception> {
        return try {

            val projectDto = ProjectDTO(
                // id는 Firestore에서 자동 생성
                name = name,
                ownerId = ownerId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            val createdProjectResult = projectRemoteDataSource.createProject(projectDto)
            if (createdProjectResult is CustomResult.Failure) {
                return CustomResult.Failure(createdProjectResult.error)
            }
            val createdProjectDto = (createdProjectResult as CustomResult.Success).data
            // 프로젝트 생성 후, owner를 멤버로 추가
            val memberResult = memberRemoteDataSource.addInitialMember(createdProjectDto.id!!, ownerId, "OWNER_ROLE_ID")
            if (memberResult is CustomResult.Failure) {
                return CustomResult.Failure(memberResult.error)
            }
            CustomResult.Success(createdProjectDto.toDomain())
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 프로젝트 상세 정보를 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun getProjectDetails(projectId: String): CustomResult<Project, Exception> {
        return try {
            val projectResult = projectRemoteDataSource.getProject(projectId)
            if (projectResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            val project = (projectResult as CustomResult.Success).data
            CustomResult.Success(project.toDomain())
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 사용자의 프로젝트 목록을 스트림으로 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용하여 실시간 업데이트를 처리합니다.
     */
    override fun getUserProjectsStream(userId: String): Flow<CustomResult<List<Project>, Unit>> {
        return projectRemoteDataSource.getUserProjectsStream(userId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    try {
                        val domainProjects = result.data.map { it.toDomain() }
                        CustomResult.Success(domainProjects)
                    } catch (e: Exception) {
                        CustomResult.Failure(Unit)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        }
    }

    /**
     * 공개된 프로젝트 목록을 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun getPublicProjects(): CustomResult<List<Project>, Unit> {
        return try {
            val projectsResult = projectRemoteDataSource.getPublicProjects()
            when (projectsResult) {
                is CustomResult.Success -> {
                    val domainProjects = projectsResult.data.map { it.toDomain() }
                    CustomResult.Success(domainProjects)
                }
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트 정보를 업데이트합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param name 새 프로젝트 이름 (선택적)
     * @param description 새 프로젝트 설명 (선택적)
     * @param isPublic 새 공개 여부 (선택적)
     */
    override suspend fun updateProjectInfo(
        projectId: String,
        name: String?,
        description: String?,
        isPublic: Boolean?
    ): CustomResult<Unit, Unit> {
        return try {
            // 필드 업데이트 맵 생성
            val updates = mutableMapOf<String, Any>()
            name?.let { updates["name"] = it }
            description?.let { updates["description"] = it }
            isPublic?.let { updates["isPublic"] = it }
            updates["updatedAt"] = Timestamp.now()
            
            // 부분 업데이트 수행
            val updateResult = projectRemoteDataSource.updateProjectFields(projectId, updates)
            when (updateResult) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트 이미지를 업데이트합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param projectImageInputStream 새 이미지 입력 스트림
     * @param imageMimeType 이미지 MIME 타입
     * @return 업로드된 이미지 URL
     */
    override suspend fun updateProjectImage(
        projectId: String,
        projectImageInputStream: InputStream,
        imageMimeType: String
    ): CustomResult<String, Unit> {
        return try {
            // 기존 이미지가 있으면 삭제
            val currentProjectResult = projectRemoteDataSource.getProject(projectId)
            if (currentProjectResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            val currentProject = (currentProjectResult as CustomResult.Success).data
            currentProject.imageUrl?.let {
                try {
                    mediaRepository.deleteImageByUrl(it)
                } catch (e: Exception) {
                    // 이미지 삭제 실패는 무시하고 계속 진행
                }
            }
            
            // 새 이미지 업로드
            val uploadResult = mediaRepository.uploadImage(
                inputStream = projectImageInputStream,
                mimeType = imageMimeType,
                storagePath = "project_images",
                desiredFileName = "project_${projectId}"
            )
            
            if (uploadResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            val imageUrl = (uploadResult as CustomResult.Success).data.url

            // 이미지 URL 업데이트
            val updateResult = projectRemoteDataSource.updateProjectImageUrl(projectId, imageUrl)
            if (updateResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            CustomResult.Success(imageUrl)
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트를 삭제합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 삭제할 프로젝트 ID
     * @param currentUserId 삭제 요청자 ID (권한 확인용)
     */
    override suspend fun deleteProject(projectId: String, currentUserId: String): CustomResult<Unit, Unit> {
        return try {
            // 프로젝트 이미지가 있으면 함께 삭제
            val projectResult = projectRemoteDataSource.getProject(projectId)
            if (projectResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            val project = (projectResult as CustomResult.Success).data
            project.imageUrl?.let {
                try {
                    mediaRepository.deleteImageByUrl(it)
                } catch (e: Exception) {
                    // 이미지 삭제 실패는 무시하고 계속 진행
                }
            }
            
            // 프로젝트 삭제
            val deleteResult = projectRemoteDataSource.deleteProject(projectId, currentUserId)
            if (deleteResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트 이름 사용 가능 여부를 확인합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectName 확인할 프로젝트 이름
     * @return 사용 가능 여부
     */
    override suspend fun checkProjectNameAvailability(projectName: String): CustomResult<Boolean, Unit> {
        return try {
            val availabilityResult = projectRemoteDataSource.checkProjectNameAvailability(projectName)
            when (availabilityResult) {
                is CustomResult.Success -> CustomResult.Success(availabilityResult.data)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트에 멤버를 추가합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @param roleId 부여할 역할 ID
     * @return 추가된 프로젝트 멤버 정보
     */
    override suspend fun addMemberToProject(projectId: String, userId: String, roleId: String): CustomResult<Member, Unit> {
        return try {
            val memberResult = memberRemoteDataSource.addMemberToProject(projectId, userId, roleId)
            when (memberResult) {
                is CustomResult.Success -> CustomResult.Success(memberResult.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트에서 멤버를 제거합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 제거할 사용자 ID
     * @param currentUserId 요청자 ID (권한 확인용)
     */
    override suspend fun removeMemberFromProject(projectId: String, userId: String, currentUserId: String): CustomResult<Unit, Unit> {
        return try {
            val removeResult = memberRemoteDataSource.removeMemberFromProject(projectId, userId, currentUserId)
            when (removeResult) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트 구조를 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 정보
     */
    override suspend fun getProjectStructure(projectId: String): CustomResult<List<Category>, Unit> {
        return try {
            // 카테고리 목록 가져오기
            val categoriesResult = categoryRemoteDataSource.getCategories(projectId)
            if (categoriesResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            val categories = (categoriesResult as CustomResult.Success).data
            
            // 채널 목록 가져오기
            val channelsResult = projectChannelRemoteDataSource.getChannels(projectId)
            if (channelsResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            val channels = (channelsResult as CustomResult.Success).data
            
            // 프로젝트 구조 생성
            val projectStructure = categories.map { it.toDomain() }
            CustomResult.Success(projectStructure)
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    /**
     * 프로젝트 구조를 업데이트합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param newStructure 새 프로젝트 구조
     * @param currentUserId 요청자 ID (권한 확인용)
     */
    override suspend fun updateProjectStructure(projectId: String, newStructure: ProjectStructure, currentUserId: String): CustomResult<Unit, Unit> {
        return try {
            // 권한 확인 (프로젝트 소유자 또는 관리자인지)
            val projectResult = projectRemoteDataSource.getProject(projectId)
            if (projectResult is CustomResult.Failure) {
                return CustomResult.Failure(Unit)
            }
            
            val project = (projectResult as CustomResult.Success).data
            if (project.ownerId != currentUserId) {
                // 소유자가 아닌 경우 관리자 권한 확인
                val memberResult = memberRemoteDataSource.getProjectMember(projectId, currentUserId)
                if (memberResult is CustomResult.Failure) {
                    return CustomResult.Failure(Unit)
                }
                
                val member = (memberResult as CustomResult.Success).data
                if (member.roleId != "ADMIN_ROLE_ID" && member.roleId != "OWNER_ROLE_ID") {
                    return CustomResult.Failure(Unit) // 권한 없음
                }
            }
            
            // 트랜잭션으로 처리하는 것이 이상적이지만, 여기서는 개별 호출로 구현
            
            // 1. 카테고리 업데이트
            for (category in newStructure) {
                val categoryResult = categoryRemoteDataSource.updateCategory(projectId, category.toDto())
                if (categoryResult is CustomResult.Failure) {
                    return CustomResult.Failure(Unit)
                }
            }
            
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }
}
