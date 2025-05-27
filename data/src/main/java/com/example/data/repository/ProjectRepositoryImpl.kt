package com.example.data._repository

import com.example.core_common.result.resultTry
import com.example.data.datasource._remote.ProjectRemoteDataSource
import com.example.data.datasource._remote.CategoryRemoteDataSource
import com.example.data.datasource._remote.ProjectChannelRemoteDataSource
import com.example.data.datasource._remote.MemberRemoteDataSource // ProjectMember 관리를 위해 필요
// import com.example.domain._repository.MediaRepository // 이미지 업로드를 위해 주입 (또는 직접 Storage 사용)
import com.example.data.model._remote.ProjectDTO
import com.example.data.model.mapper.toDomain
import com.example.data.model.mapper.toDto // Domain -> DTO
import com.example.domain.model.Project
import com.example.domain.model.ProjectMember
import com.example.domain.model.ProjectStructure
import com.example.domain._repository.ProjectRepository
import com.example.domain._repository.MediaRepository // MediaRepository 사용 예시
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import javax.inject.Inject
import kotlin.Result

class ProjectRepositoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource, // ProjectStructure 관리용
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource, // ProjectStructure 관리용
    private val memberRemoteDataSource: MemberRemoteDataSource, // 멤버 관리용
    private val mediaRepository: MediaRepository // 이미지 업로드용
    // private val projectMapper: ProjectMapper // 개별 매퍼 사용시
) : ProjectRepository {

    override suspend fun createProject(
        name: String,
        description: String?,
        ownerId: String,
        isPublic: Boolean,
        projectImageInputStream: InputStream?,
        imageMimeType: String?
    ): Result<Project> = resultTry {
        var imageUrl: String? = null
        if (projectImageInputStream != null && imageMimeType != null) {
            imageUrl = mediaRepository.uploadImage(
                inputStream = projectImageInputStream,
                mimeType = imageMimeType,
                storagePath = \
project_images\, // 예시 경로
                desiredFileName = \project_\_\\
            ).getOrThrow().url
        }

        val projectDto = ProjectDTO(
            // id는 Firestore에서 자동 생성
            name = name,
            description = description,
            ownerId = ownerId,
            isPublic = isPublic,
            imageUrl = imageUrl,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            memberCount = 1 // 초기 멤버는 owner 1명
        )
        val createdProjectDto = projectRemoteDataSource.createProject(projectDto).getOrThrow()
        // 프로젝트 생성 후, owner를 멤버로 추가하는 로직 (DataSource 또는 여기서 처리)
        // memberRemoteDataSource.addInitialMember(createdProjectDto.id!!, ownerId, \OWNER_ROLE_ID\).getOrThrow()
        createdProjectDto.toDomain()
    }

    override suspend fun getProjectDetails(projectId: String): Result<Project> = resultTry {
        projectRemoteDataSource.getProject(projectId).getOrThrow().toDomain()
    }

    override fun getUserProjectsStream(userId: String): Flow<Result<List<Project>>> {
        // ProjectRemoteDataSource에 getUserProjectsStream(userId) 함수 필요
        return projectRemoteDataSource.getUserProjectsStream(userId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun getPublicProjects(): Result<List<Project>> = resultTry {
        // ProjectRemoteDataSource에 getPublicProjects() 함수 필요
        projectRemoteDataSource.getPublicProjects().getOrThrow().map { it.toDomain() }
    }

    override suspend fun updateProjectInfo(
        projectId: String,
        name: String?,
        description: String?,
        isPublic: Boolean?
    ): Result<Unit> = resultTry {
        // ProjectRemoteDataSource에 부분 업데이트 또는 전체 DTO 업데이트 함수 필요
        // 여기서는 전체 DTO를 가져와서 수정 후 다시 저장하는 예시 (비효율적일 수 있음)
        val currentDto = projectRemoteDataSource.getProject(projectId).getOrThrow()
        val updatedDto = currentDto.copy(
            name = name ?: currentDto.name,
            description = description ?: currentDto.description,
            isPublic = isPublic ?: currentDto.isPublic,
            updatedAt = Timestamp.now()
        )
        projectRemoteDataSource.updateProject(updatedDto).getOrThrow()
    }

    override suspend fun updateProjectImage(
        projectId: String,
        projectImageInputStream: InputStream,
        imageMimeType: String
    ): Result<String?> = resultTry {
        val imageUrl = mediaRepository.uploadImage(
            inputStream = projectImageInputStream,
            mimeType = imageMimeType,
            storagePath = \project_images\,
            desiredFileName = \project_\_\\
        ).getOrThrow().url

        // ProjectRemoteDataSource에 updateProjectImageUrl(projectId, imageUrl) 함수 필요
        projectRemoteDataSource.updateProjectImageUrl(projectId, imageUrl).getOrThrow()
        imageUrl
    }

    override suspend fun deleteProject(projectId: String, currentUserId: String): Result<Unit> = resultTry {
        // ProjectRemoteDataSource에 deleteProject(projectId, currentUserId) 함수 필요 (권한 확인 포함)
        projectRemoteDataSource.deleteProject(projectId, currentUserId).getOrThrow()
    }

    override suspend fun checkProjectNameAvailability(projectName: String): Result<Boolean> = resultTry {
        // ProjectRemoteDataSource에 checkProjectNameAvailability(projectName) 함수 필요
        projectRemoteDataSource.checkProjectNameAvailability(projectName).getOrThrow()
    }

    override suspend fun addMemberToProject(projectId: String, userId: String, roleId: String): Result<ProjectMember> = resultTry {
        // MemberRemoteDataSource에 addMemberToProject(projectId, userId, roleId) 함수 필요
        // 이 함수는 MemberDTO를 반환하고, 이를 ProjectMember 도메인 모델로 변환
        memberRemoteDataSource.addMemberToProject(projectId, userId, roleId).getOrThrow().toDomain()
    }

    override suspend fun removeMemberFromProject(projectId: String, userId: String, currentUserId: String): Result<Unit> = resultTry {
        // MemberRemoteDataSource에 removeMemberFromProject(projectId, userId, currentUserId) 함수 필요
        memberRemoteDataSource.removeMemberFromProject(projectId, userId, currentUserId).getOrThrow()
    }

    override suspend fun getProjectStructure(projectId: String): Result<ProjectStructure> {
        // 이 함수는 CategoryRemoteDataSource와 ProjectChannelRemoteDataSource를 사용하여
        // ProjectStructure를 구성해야 합니다. 구현이 복잡할 수 있습니다.
        // ProjectRemoteDataSource에 getProjectStructure(projectId)가 있다면 가장 좋음.
        // 또는 ProjectStructureRemoteDataSource가 별도로 존재할 수 있음.
        // 여기서는 ProjectRemoteDataSource에 해당 기능이 있다고 가정합니다.
        // return projectRemoteDataSource.getProjectStructure(projectId).getOrThrow().toDomain()
        throw NotImplementedError(\getProjectStructure
not
implemented
yet.
Requires
coordination
of
multiple
DataSources
or
a
dedicated
one.\)
    }

    override suspend fun updateProjectStructure(projectId: String, newStructure: ProjectStructure, currentUserId: String): Result<Unit> {
        // 이 함수도 ProjectStructure를 업데이트하기 위해 여러 DataSource와 상호작용하거나,
        // ProjectRemoteDataSource (또는 ProjectStructureRemoteDataSource)에 해당 기능이 필요합니다.
        // throw NotImplementedError(\updateProjectStructure
not
implemented
yet.
Requires
coordination
of
multiple
DataSources
or
a
dedicated
one.\)
        throw NotImplementedError(\updateProjectStructure
not
implemented
yet.\)
    }
}
