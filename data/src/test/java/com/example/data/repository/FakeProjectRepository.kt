package com.example.data.repository

import com.example.domain.model.Project
import com.example.domain.model.ProjectInfo
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ProjectRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 ProjectRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeProjectRepository : ProjectRepository {
    
    // 인메모리 프로젝트 데이터 저장소
    private val projects = ConcurrentHashMap<String, Project>()
    
    // 프로젝트 코드/토큰 매핑
    private val projectCodes = ConcurrentHashMap<String, String>() // code/link -> projectId
    private val projectTokens = ConcurrentHashMap<String, String>() // token -> projectId
    
    // Flow를 사용한 프로젝트 목록 관리
    private val _projectListFlow = MutableStateFlow<List<Project>>(emptyList())
    
    // 에러 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 프로젝트 데이터 추가
     */
    fun addProject(project: Project) {
        projects[project.id] = project
        updateProjectListFlow()
    }
    
    /**
     * 테스트를 위해 프로젝트 초대 코드 설정
     */
    fun setProjectCode(code: String, projectId: String) {
        projectCodes[code] = projectId
    }
    
    /**
     * 테스트를 위해 프로젝트 초대 토큰 설정
     */
    fun setProjectToken(token: String, projectId: String) {
        projectTokens[token] = projectId
    }
    
    /**
     * 테스트를 위해 모든 프로젝트 데이터 초기화
     */
    fun clearProjects() {
        projects.clear()
        projectCodes.clear()
        projectTokens.clear()
        updateProjectListFlow()
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }
    
    /**
     * 프로젝트 목록 Flow 업데이트
     */
    private fun updateProjectListFlow() {
        _projectListFlow.value = projects.values.toList()
    }
    
    /**
     * 에러 시뮬레이션 확인 및 처리
     */
    private fun <T> simulateErrorIfNeeded(): Result<T>? {
        return if (shouldSimulateError) {
            Result.failure(errorToSimulate)
        } else {
            null
        }
    }

    override fun getProjectListStream(): Flow<List<Project>> {
        return _projectListFlow.asStateFlow()
    }

    override suspend fun fetchProjectList(): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 실제로는 원격 데이터 소스에서 가져오는 작업이 필요하지만,
        // 여기서는 이미 메모리에 있는 데이터를 flow에 업데이트만 수행
        updateProjectListFlow()
        
        return Result.success(Unit)
    }

    override suspend fun isProjectNameAvailable(name: String): Result<Boolean> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Boolean>()?.let { return it }
        
        // 동일한 이름의 프로젝트가 있는지 확인
        val isNameUsed = projects.values.any { it.name.equals(name, ignoreCase = true) }
        
        return Result.success(!isNameUsed)
    }

    override suspend fun joinProjectWithCode(codeOrLink: String): Result<String> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<String>()?.let { return it }
        
        // 코드에 해당하는 프로젝트 ID 조회
        val projectId = projectCodes[codeOrLink]
            ?: return Result.failure(IllegalArgumentException("Invalid project code or link"))
        
        // 프로젝트 존재 여부 확인
        if (!projects.containsKey(projectId)) {
            return Result.failure(IllegalStateException("Project not found"))
        }
        
        // 프로젝트 ID 반환 (실제로는 여기서 유저를 프로젝트에 추가하는 작업도 필요)
        return Result.success(projectId)
    }

    override suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<ProjectInfo>()?.let { return it }
        
        // 토큰에 해당하는 프로젝트 ID 조회
        val projectId = projectTokens[token]
            ?: return Result.failure(IllegalArgumentException("Invalid project token"))
        
        // 프로젝트 정보 조회
        val project = projects[projectId]
            ?: return Result.failure(IllegalStateException("Project not found"))
        
        // ProjectInfo 생성하여 반환
        return Result.success(
            ProjectInfo(
                projectName = project.name,
                memberCount = project.memberCount ?: 0
            )
        )
    }

    override suspend fun joinProjectWithToken(token: String): Result<String> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<String>()?.let { return it }
        
        // 토큰에 해당하는 프로젝트 ID 조회
        val projectId = projectTokens[token]
            ?: return Result.failure(IllegalArgumentException("Invalid project token"))
        
        // 프로젝트 존재 여부 확인
        if (!projects.containsKey(projectId)) {
            return Result.failure(IllegalStateException("Project not found"))
        }
        
        // 프로젝트 ID 반환 (실제로는 여기서 유저를 프로젝트에 추가하는 작업도 필요)
        return Result.success(projectId)
    }

    override suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Project>()?.let { return it }
        
        // 이름 유효성 검증
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Project name cannot be blank"))
        }
        
        // 이름 중복 확인
        val isNameAvailable = isProjectNameAvailable(name).getOrElse { return Result.failure(it) }
        if (!isNameAvailable) {
            return Result.failure(IllegalArgumentException("Project name already in use"))
        }
        
        // 새 프로젝트 생성
        val newProject = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description.takeIf { it.isNotBlank() },
            imageUrl = null,
            memberCount = 1, // 생성자만 있는 상태
            isPublic = isPublic
        )
        
        // 프로젝트 저장
        projects[newProject.id] = newProject
        updateProjectListFlow()
        
        return Result.success(newProject)
    }

    override suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<List<Project>>()?.let { return it }
        
        // 사용자가 속한 모든 프로젝트 반환 (테스트에서는 모든 프로젝트를 반환)
        return Result.success(projects.values.toList())
    }
} 