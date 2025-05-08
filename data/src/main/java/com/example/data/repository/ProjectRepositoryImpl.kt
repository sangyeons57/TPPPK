package com.example.data.repository

import com.example.data.datasource.local.project.ProjectLocalDataSource
import com.example.data.datasource.remote.project.ProjectRemoteDataSource
import com.example.data.model.local.ProjectEntity
import com.example.data.model.remote.project.ProjectDto
import com.example.data.util.CurrentUserProvider
import com.example.domain.model.Project
import com.example.domain.model.ProjectInfo
import com.example.domain.repository.ProjectRepository
import com.example.domain.util.NetworkConnectivityMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * ProjectRepository 인터페이스의 실제 구현체
 * 로컬 및 원격 데이터 소스를 조율하여 프로젝트 데이터를 관리합니다.
 *
 * @param remoteDataSource 프로젝트 원격 데이터 소스
 * @param localDataSource 프로젝트 로컬 데이터 소스
 * @param networkMonitor 네트워크 연결 상태 모니터
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectRemoteDataSource,
    private val localDataSource: ProjectLocalDataSource,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val currentUserProvider: CurrentUserProvider // 현재 로그인한 사용자 정보를 제공하는 클래스
) : ProjectRepository {

    override fun getProjectListStream(): Flow<List<Project>> = flow {
        // 로그인한 사용자의 ID 가져오기
        val userId = currentUserProvider.getCurrentUserId()
        
        // 로컬 DB에서 프로젝트 목록 Flow 가져오기
        val localProjectsFlow = localDataSource.getParticipatingProjectsStream(userId)
            .map { projectEntities -> projectEntities.map { it.toDomain() } }
        
        // 로컬 데이터를 우선 방출
        emitAll(localProjectsFlow)
        
        // 네트워크 연결 확인 후 원격 데이터 가져오기 시도
        if (networkMonitor.isNetworkAvailable.first()) {
            try {
                fetchProjectList() // 원격 데이터를 동기화
            } catch (e: Exception) {
                // 네트워크 오류 처리 (로그만 남기고 로컬 데이터는 계속 제공)
                println("Network error while fetching projects: ${e.message}")
            }
        }
    }

    override suspend fun fetchProjectList(): Result<Unit> = runCatching {
        // 로그인한 사용자의 ID 가져오기
        val userId = currentUserProvider.getCurrentUserId()
        
        // 원격 데이터 소스에서 프로젝트 목록 가져오기
        val remoteProjects = remoteDataSource.getParticipatingProjects(userId).getOrThrow()
        
        // 원격 데이터를 로컬 DB에 저장
        remoteProjects.forEach { projectDto ->
            localDataSource.upsertProject(projectDto.toEntity())
        }
    }

    override suspend fun isProjectNameAvailable(name: String): Result<Boolean> = runCatching {
        // 네트워크 연결 확인
        if (!networkMonitor.isNetworkAvailable.first()) {
            throw IllegalStateException("네트워크 연결이 필요합니다.")
        }
        
        // 원격 API를 통해 프로젝트 이름 중복 검사
        // 필요한 원격 데이터 소스 메서드가 추가되어야 함
        // remoteDataSource.checkProjectNameAvailability(name).getOrThrow()
        
        // 임시 구현 (필요에 따라 수정)
        true
    }

    override suspend fun joinProjectWithCode(codeOrLink: String): Result<String> = runCatching {
        // 네트워크 연결 확인
        if (!networkMonitor.isNetworkAvailable.first()) {
            throw IllegalStateException("네트워크 연결이 필요합니다.")
        }
        
        // 원격 API를 통해 코드로 프로젝트 참여
        // 필요한 원격 데이터 소스 메서드가 추가되어야 함
        // val projectId = remoteDataSource.joinProjectWithCode(codeOrLink).getOrThrow()
        
        // 임시 반환 (필요에 따라 수정)
        throw NotImplementedError("원격 데이터 소스 메서드 구현 필요")
    }

    override suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo> = runCatching {
        // 네트워크 연결 확인
        if (!networkMonitor.isNetworkAvailable.first()) {
            throw IllegalStateException("네트워크 연결이 필요합니다.")
        }
        
        // 원격 API를 통해 토큰에서 프로젝트 정보 가져오기
        // 필요한 원격 데이터 소스 메서드가 추가되어야 함
        // val projectInfo = remoteDataSource.getProjectInfoFromToken(token).getOrThrow()
        
        // 임시 반환 (필요에 따라 수정)
        throw NotImplementedError("원격 데이터 소스 메서드 구현 필요")
    }

    override suspend fun joinProjectWithToken(token: String): Result<String> = runCatching {
        // 네트워크 연결 확인
        if (!networkMonitor.isNetworkAvailable.first()) {
            throw IllegalStateException("네트워크 연결이 필요합니다.")
        }
        
        // 원격 API를 통해 토큰으로 프로젝트 참여
        // 필요한 원격 데이터 소스 메서드가 추가되어야 함
        // val projectId = remoteDataSource.joinProjectWithToken(token).getOrThrow()
        
        // 임시 반환 (필요에 따라 수정)
        throw NotImplementedError("원격 데이터 소스 메서드 구현 필요")
    }

    override suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project> = runCatching {
        // 네트워크 연결 확인
        if (!networkMonitor.isNetworkAvailable.first()) {
            throw IllegalStateException("네트워크 연결이 필요합니다.")
        }
        
        // 현재 사용자 ID 가져오기
        val userId = currentUserProvider.getCurrentUserId()
        
        // 프로젝트 DTO 생성
        val projectDto = ProjectDto(
            name = name,
            description = description,
            ownerId = userId,
            memberIds = listOf(userId),
            isPublic = isPublic
        )
        
        // 원격 API를 통해 프로젝트 생성
        val projectId = remoteDataSource.createProject(projectDto).getOrThrow()
        
        // 생성된 프로젝트 상세 정보 가져오기
        val createdProject = remoteDataSource.getProjectDetails(projectId).getOrThrow()
        
        // 로컬 DB에 저장
        localDataSource.upsertProject(createdProject.toEntity())
        
        // 도메인 모델로 변환하여 반환
        createdProject.toDomain()
    }

    override suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> = runCatching {
        // 로그인한 사용자의 ID 가져오기
        val userId = currentUserProvider.getCurrentUserId()
        
        // 사용자가 참여하고 있는 프로젝트 목록 가져오기 (로컬 DB 우선)
        val localProjects = localDataSource.getParticipatingProjectsStream(userId).map { 
            it.map { entity -> entity.toDomain() } 
        }
        
        // 네트워크 연결이 있으면 원격 데이터를 동기화
        if (networkMonitor.isNetworkAvailable.first()) {
            try {
                fetchProjectList()
            } catch (e: Exception) {
                // 네트워크 오류 (무시하고 로컬 데이터 사용)
            }
        }
        
        // 현재 시점의 로컬 데이터 반환
        localProjects.firstOrNull() ?: emptyList()
    }
    
    // DTO와 Entity 간 변환 확장 함수
    private fun ProjectDto.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = this.projectId,
            name = this.name,
            description = this.description,
            ownerId = this.ownerId,
            participantIds = this.memberIds,
            createdAt = this.createdAt.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
            lastUpdatedAt = this.updatedAt.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        )
    }
    
    // Entity와 Domain 모델 간 변환 확장 함수
    private fun ProjectEntity.toDomain(): Project {
        return Project(
            id = this.id,
            name = this.name,
            description = this.description,
            imageUrl = null, // Entity에 imageUrl 필드가 없는 경우 조정 필요
            memberCount = this.participantIds.size,
            isPublic = true // Entity에 isPublic 필드가 없는 경우 조정 필요
        )
    }
    
    // DTO와 Domain 모델 간 변환 확장 함수
    private fun ProjectDto.toDomain(): Project {
        return Project(
            id = this.projectId,
            name = this.name,
            description = this.description,
            imageUrl = this.imageUrl,
            memberCount = this.memberIds.size,
            isPublic = this.isPublic
        )
    }
}