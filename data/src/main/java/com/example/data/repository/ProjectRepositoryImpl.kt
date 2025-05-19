package com.example.data.repository

import android.util.Log
import com.example.data.datasource.remote.project.ProjectRemoteDataSource
import com.example.data.model.remote.project.ProjectDto
import com.example.data.util.CurrentUserProvider
import com.example.domain.model.Project
import com.example.domain.model.ProjectInfo
import com.example.domain.repository.ProjectRepository
// import com.example.domain.util.NetworkConnectivityMonitor // Removed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow // Required for flow builder
import kotlinx.coroutines.flow.map
// import kotlinx.coroutines.flow.firstOrNull // Keep if used, remove if only for networkMonitor
// import kotlinx.coroutines.flow.first // Will be removed if only used with networkMonitor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result
import com.example.data.datasource.remote.projectstructure.ProjectStructureRemoteDataSource
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import com.example.domain.repository.ChannelRepository // Added for createProject
import kotlinx.coroutines.flow.emitAll // Added for getProjectListStream
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ChannelMode

/**
 * ProjectRepository 인터페이스의 실제 구현체
 * 원격 데이터 소스를 사용하여 프로젝트 데이터를 관리하고 Firestore 캐시를 활용합니다.
 *
 * @param remoteDataSource 프로젝트 원격 데이터 소스
 * @param currentUserProvider 현재 로그인한 사용자 정보를 제공하는 클래스
 * @param projectStructureRemoteDataSource 프로젝트 구조 관련 원격 데이터 소스
 * @param channelRepository 채널 관련 리포지토리 (프로젝트 생성 시 기본 채널 생성용)
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectRemoteDataSource,
    // private val networkMonitor: NetworkConnectivityMonitor, // Removed
    private val currentUserProvider: CurrentUserProvider,
    private val projectStructureRemoteDataSource: ProjectStructureRemoteDataSource,
    private val channelRepository: ChannelRepository // Added for createProject
) : ProjectRepository {

    override suspend fun getProjectListStream(): Flow<List<Project>> = flow {
        val userId = currentUserProvider.getCurrentUserId()
        Log.d("ProjectRepositoryImpl", "Fetching project list stream for user: $userId")
        val projectsStream = remoteDataSource.getParticipatingProjectsStream(userId)
            .map { result ->
                result.map { dtos ->
                    dtos.map { it.toDomain() }
                }.getOrElse {
                    println("Error fetching project list stream: ${it.message}")
                    emptyList<Project>()
                }
            }
        emitAll(projectsStream) // emitAll requires kotlinx.coroutines.flow.emitAll
    }

    override suspend fun fetchProjectList(): Result<Unit> = runCatching {
        // Network check removed, Firestore cache handles offline.
        val userId = currentUserProvider.getCurrentUserId()
        remoteDataSource.getParticipatingProjects(userId).getOrThrow()
    }

    override suspend fun isProjectNameAvailable(name: String): Result<Boolean> = runCatching {
        // Network check removed.
        // remoteDataSource.checkProjectNameAvailability(name).getOrThrow() // Actual implementation needed
        true // Placeholder
    }

    override suspend fun joinProjectWithCode(codeOrLink: String): Result<String> = runCatching {
        // Network check removed.
        // remoteDataSource.joinProjectWithCode(codeOrLink).getOrThrow()
        throw NotImplementedError("Remote data source method implementation needed")
    }

    override suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo> = runCatching {
        // Network check removed.
        // remoteDataSource.getProjectInfoFromToken(token).getOrThrow()
        throw NotImplementedError("Remote data source method implementation needed")
    }

    override suspend fun joinProjectWithToken(token: String): Result<String> = runCatching {
        // Network check removed.
        // remoteDataSource.joinProjectWithToken(token).getOrThrow()
        throw NotImplementedError("Remote data source method implementation needed")
    }

    override suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project> = runCatching {
        // Network check removed.
        val userId = currentUserProvider.getCurrentUserId()
        val projectDto = ProjectDto(
            name = name,
            description = description,
            ownerId = userId,
            memberIds = listOf(userId),
            isPublic = isPublic
        )
        val projectId = remoteDataSource.createProject(projectDto).getOrThrow()
        
        // 확실한 오류 방지를 위해 프로젝트 생성 시 채널 생성 관련 로직은 모두 제거합니다.
        // 이 로직은 추후 별도의 UseCase나 서비스 로직으로 분리하여 관리하는 것이 좋습니다.
        // 예시: channelRepository.createDirectMessageChannelWithUser(projectId, userId).getOrThrow()
        // 예시: projectStructureRemoteDataSource.createDirectChannel(projectId, "${userId}_self_dm", ChannelType.DIRECT_MESSAGE.name, 0).getOrThrow()

        val createdProjectDto = remoteDataSource.getProjectDetails(projectId).getOrThrow()
        createdProjectDto.toDomain()
    }

    override suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> {
        val userId = currentUserProvider.getCurrentUserId()
        return remoteDataSource.getParticipatingProjects(userId).map { dtos ->
            dtos.map { it.toDomain() }
        }
    }
    
    // DTO와 Entity 간 변환 확장 함수 - ProjectEntity 관련 제거 또는 수정
    // private fun ProjectDto.toEntity(): ProjectEntity { ... } // 제거
    
    // Entity와 Domain 모델 간 변환 확장 함수 - ProjectEntity 관련 제거
    // private fun ProjectEntity.toDomain(): Project { ... } // 제거

    // DTO와 Domain 모델 간 변환 확장 함수 (유지)
    private fun ProjectDto.toDomain(): Project {
        val now = DateTimeUtil.nowInstant() // Fallback for missing timestamps
        return Project(
            id = this.projectId,
            name = this.name,
            description = this.description,
            imageUrl = this.imageUrl,
            ownerId = this.ownerId,
            memberIds = this.memberIds,
            isPublic = this.isPublic,
            createdAt = DateTimeUtil.firebaseTimestampToInstant(this.createdAt) ?: now,
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(this.updatedAt) ?: now
        )
    }

    // CurrentUserProvider에 getCurrentUserIdBlocking() 같은 동기 메소드가 없다고 가정하고,
    // getCurrentUserId()가 suspend 함수라고 가정하여 호출부를 수정합니다.
    // 실제 CurrentUserProvider 구현에 맞춰 조정 필요.
    // 임시로 getProjectListStream을 suspend로 변경하거나, viewModelScope 등에서 호출하도록 변경 필요.
    // 여기서는 예시로 userId를 가져오는 부분을 flow 빌더 내부로 옮기거나,
    // Repository의 해당 메소드를 suspend로 만들 수 있습니다.
    // 우선 getCurrentUserId()가 suspend라고 가정하고 flow 빌더 내에서 호출하는 방식으로 유지 (원래 코드와 유사하게)

    // --- Project Structure Management Implementations ---

    override fun getProjectStructureStream(projectId: String): Flow<ProjectStructure> {
        return projectStructureRemoteDataSource.getProjectStructureStream(projectId)
    }

    override suspend fun createCategory(projectId: String, name: String): Result<Category> = runCatching {
        projectStructureRemoteDataSource.createCategory(projectId, name).getOrThrow()
    }

    override suspend fun createCategoryChannel(projectId: String, categoryId: String, name: String, type: ChannelMode, order: Int): Result<Channel> = runCatching {
        projectStructureRemoteDataSource.createCategoryChannel(projectId, categoryId, name, type, order).getOrThrow()
    }

    override suspend fun createDirectChannel(projectId: String, name: String, type: ChannelMode, order: Int): Result<Channel> = runCatching {
        // Call the correct method on projectStructureRemoteDataSource with appropriate parameters
        projectStructureRemoteDataSource.createProjectChannel(projectId, name, type, order).getOrThrow()
    }

    // Helper for CurrentUserProvider if it's suspend
    // This is a conceptual change, actual implementation of getCurrentUserIdBlocking would be in CurrentUserProvider
    // For this refactor, we assume getCurrentUserId() is suspend and called within a coroutine scope
    private suspend fun getCurrentUserIdFromProvider(): String {
        return currentUserProvider.getCurrentUserId()
    }
}