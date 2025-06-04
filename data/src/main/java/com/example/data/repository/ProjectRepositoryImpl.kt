package com.example.data.repository

import com.example.core_common.constants.Constants
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.ProjectRemoteDataSource
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSource // ProjectMember 관리를 위해 필요
import com.example.data.model.remote.ProjectDTO
import com.example.domain.model.base.Category
import com.example.domain.model.base.Project
import com.example.domain.repository.MediaRepository
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectsWrapperRepository // Changed from UserRepository
import com.example.domain.model.base.ProjectsWrapper // Added for creating ProjectsWrapper domain object
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import java.io.InputStream
import javax.inject.Inject



class ProjectRepositoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource, // ProjectStructure 관리용
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource, // ProjectStructure 관리용
    private val memberRemoteDataSource: MemberRemoteDataSource, // 멤버 관리용
    private val mediaRepository: MediaRepository, // 이미지 업로드용
    private val projectsWrapperRepository: ProjectsWrapperRepository // Changed from UserRepository
) : ProjectRepository {

    /**
     * 새로운 프로젝트를 생성합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param name 프로젝트 이름
     * @param ownerId 프로젝트 소유자 ID
     * @return 생성된 프로젝트 정보
     */
    override suspend fun createProject(
        name: String,
        ownerId: String,
    ): CustomResult<String, Exception> {
        return resultTry {

            val projectDto = ProjectDTO(
                // id는 Firestore에서 자동 생성
                name = name,
                ownerId = ownerId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            val projectIdResult = projectRemoteDataSource.createProject(projectDto)
            if (projectIdResult is CustomResult.Failure) {
                return CustomResult.Failure(projectIdResult.error)
            }
            val projectId = (projectIdResult as CustomResult.Success).data

            val createdProjectDtoResult = projectRemoteDataSource.getProject(projectId)
            if (createdProjectDtoResult is CustomResult.Failure) {
                throw createdProjectDtoResult.error
            }

            val createdProjectDto = (createdProjectDtoResult as CustomResult.Success).data

            // 프로젝트 생성 후, owner를 멤버로 추가
            val memberResult = memberRemoteDataSource.addMember(projectId, ownerId, listOf(Constants.OWNER))
            if (memberResult is CustomResult.Failure) {
                throw memberResult.error
            }


            // Create ProjectsWrapper domain object
            val projectsWrapper = ProjectsWrapper(
                projectName = createdProjectDto.name,
                projectImageUrl = createdProjectDto.imageUrl,
                // lastUpdatedAt and other fields can be set to default/current values if needed by domain model
                // or if ProjectsWrapperRemoteDataSourceImpl handles their initial setting.
                // For now, assuming they are optional or handled downstream.
            )

            val projectWrapperResult = projectsWrapperRepository.addProjectToUser(
                userId = ownerId,
                projectId = projectId,
                projectsWrapper = projectsWrapper
            )

            if (projectWrapperResult is CustomResult.Failure) {
                // Log the error, but proceed with returning project creation success 
                // as the primary operation (project creation) was successful.
                // Consider how to handle this failure more robustly if needed (e.g., cleanup, retry)
                Log.e("ProjectRepositoryImpl", "Failed to add project wrapper for project ${createdProjectDto.id}", projectWrapperResult.error)
            }

            projectId
        }
    }

    override fun getProjectDetailsStream(projectId: String): Flow<CustomResult<Project, Exception>> {
        return projectRemoteDataSource.observeProject(projectId)
            .map { projectDto ->
                if (projectDto != null) {
                    try {
                        CustomResult.Success(projectDto.toDomain())
                    } catch (e: Exception) {
                        // Handle potential errors during toDomain() conversion, though less likely if DTO is valid
                        CustomResult.Failure(Exception("Error converting project DTO to domain model: ${e.message}", e))
                    }
                } else {
                    // This case might mean the project doesn't exist or was deleted.
                    CustomResult.Failure(NoSuchElementException("Project with ID $projectId not found or stream returned null."))
                }
            }
            .catch { e ->
                // Catch exceptions from the upstream flow (e.g., network issues in observeProject)
                emit(CustomResult.Failure(Exception("Error observing project details: ${e.message}", e)))
            }
            .onStart { emit(CustomResult.Loading) } // Emit Loading state when the flow collection starts
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
    ): CustomResult<Unit, Exception> {
        return resultTry {
            val originResult = projectRemoteDataSource.getProject(projectId)
            val originProject = if(originResult is CustomResult.Success) {
                originResult.data
            } else if(originResult is CustomResult.Failure) {
                throw Exception(originResult.error)
            } else {
                throw Exception("Unknown error")
            }
            // 필드 업데이트 맵 생성

            val newProject = originProject.copy(name = name?: originProject.name, updatedAt = Timestamp.now())

            // 부분 업데이트 수행
            val updateResult = projectRemoteDataSource.updateProjectDetails(projectId, newProject)
            when (updateResult) {
                is CustomResult.Success -> Unit
                is CustomResult.Failure -> Exception(updateResult.error)
                else -> CustomResult.Failure(Exception("Unknown error"))
            }
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
    ): CustomResult<String, Exception> {
        TODO("나중에 다시 구현 필요")
        /**
        return resultTry {
            // 기존 이미지가 있으면 삭제
            val currentProjectResult = projectRemoteDataSource.getProject(projectId)
            if (currentProjectResult is CustomResult.Failure) {
                throw Exception(currentProjectResult.error)
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
                throw uploadResult.error
            }
            
            val imageUrl = (uploadResult as CustomResult.Success).data.url

            // 이미지 URL 업데이트
            val updateResult = projectRemoteDataSource.updateProjectImageUrl(projectId, imageUrl)
            if (updateResult is CustomResult.Failure<*>) {
                throw Exception(updateResult.error)
            }

            imageUrl
        }
        **/
    }

    /**
     * 프로젝트를 삭제합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 삭제할 프로젝트 ID
     * @param currentUserId 삭제 요청자 ID (권한 확인용)
     */
    override suspend fun deleteProject(projectId: String, currentUserId: String): CustomResult<Unit, Exception> {
        return resultTry {
            // 프로젝트 이미지가 있으면 함께 삭제
            val projectResult = projectRemoteDataSource.getProject(projectId)
            if (projectResult is CustomResult.Failure) {
                throw projectResult.error
            }
            
            val project = (projectResult as CustomResult.Success).data

            // 프로젝트 삭제
            val deleteResult = projectRemoteDataSource.deleteProject(projectId)
            if (deleteResult is CustomResult.Failure) {
                return CustomResult.Failure(Exception("Failed to delete project: ${deleteResult.error}"))
            }
            
            Unit
        }
    }

    /**
     * 프로젝트 구조를 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 정보
     */
    override suspend fun getProjectStructureStream(projectId: String): Flow<CustomResult<List<Category>, Exception>> {
        return categoryRemoteDataSource.observeCategories(projectId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val categories = result.data.map { it.toDomain() }
                    CustomResult.Success(categories)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in getProjectStructureStream"))
                }
            }
        }
    }
}
