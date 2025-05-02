package com.example.data.repository

import com.example.domain.model.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ProjectRepository 기능 테스트
 *
 * 이 테스트는 FakeProjectRepository를 사용하여 ProjectRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class ProjectRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var projectRepository: FakeProjectRepository
    
    // 테스트 데이터
    private val testProjectId = "test-project-123"
    private val testProject = Project(
        id = testProjectId,
        name = "테스트 프로젝트",
        description = "테스트 프로젝트 설명",
        imageUrl = null,
        memberCount = 5,
        isPublic = true
    )
    
    // 초대 코드 및 토큰 테스트 데이터
    private val testInviteCode = "invite-code-abc"
    private val testInviteToken = "invite-token-xyz"
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeProjectRepository 초기화
        projectRepository = FakeProjectRepository()
        
        // 테스트 프로젝트 추가
        projectRepository.addProject(testProject)
        
        // 초대 코드 및 토큰 설정
        projectRepository.setProjectCode(testInviteCode, testProjectId)
        projectRepository.setProjectToken(testInviteToken, testProjectId)
    }
    
    /**
     * 프로젝트 목록 스트림 테스트
     */
    @Test
    fun `getProjectListStream should emit project list`() = runBlocking {
        // When: 프로젝트 목록 스트림 가져오기
        val projects = projectRepository.getProjectListStream().first()
        
        // Then: 올바른 프로젝트 목록 확인
        assertEquals(1, projects.size)
        assertEquals(testProject, projects.first())
    }
    
    /**
     * 프로젝트 목록 새로고침 테스트
     */
    @Test
    fun `fetchProjectList should update project list`() = runBlocking {
        // Given: 프로젝트 목록이 비어있는 상태로 설정
        projectRepository.clearProjects()
        
        // 기존 목록이 비어있는지 확인
        val emptyProjects = projectRepository.getProjectListStream().first()
        assertEquals(0, emptyProjects.size)
        
        // 테스트 프로젝트 다시 추가
        projectRepository.addProject(testProject)
        
        // When: 프로젝트 목록 새로고침
        val result = projectRepository.fetchProjectList()
        
        // Then: 성공 및 프로젝트 목록 업데이트됨
        assertTrue(result.isSuccess)
        
        // Then: 스트림에서 업데이트된 목록 확인
        val updatedProjects = projectRepository.getProjectListStream().first()
        assertEquals(1, updatedProjects.size)
        assertEquals(testProject, updatedProjects.first())
    }
    
    /**
     * 프로젝트 이름 사용 가능 여부 테스트 - 사용 가능한 경우
     */
    @Test
    fun `isProjectNameAvailable should return true for available name`() = runBlocking {
        // When: 사용 가능한 이름 확인
        val result = projectRepository.isProjectNameAvailable("새 프로젝트")
        
        // Then: 성공 및 true 반환
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }
    
    /**
     * 프로젝트 이름 사용 가능 여부 테스트 - 이미 사용 중인 경우
     */
    @Test
    fun `isProjectNameAvailable should return false for unavailable name`() = runBlocking {
        // When: 이미 사용 중인 이름 확인 (대소문자 구분 없이)
        val result = projectRepository.isProjectNameAvailable("테스트 프로젝트")
        
        // Then: 성공 및 false 반환
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }
    
    /**
     * 초대 코드로 프로젝트 참여 테스트
     */
    @Test
    fun `joinProjectWithCode should return projectId for valid code`() = runBlocking {
        // When: 유효한 초대 코드로 참여
        val result = projectRepository.joinProjectWithCode(testInviteCode)
        
        // Then: 성공 및 프로젝트 ID 반환
        assertTrue(result.isSuccess)
        assertEquals(testProjectId, result.getOrNull())
    }
    
    /**
     * 잘못된 초대 코드로 프로젝트 참여 실패 테스트
     */
    @Test
    fun `joinProjectWithCode should fail for invalid code`() = runBlocking {
        // When: 잘못된 초대 코드로 참여
        val result = projectRepository.joinProjectWithCode("invalid-code")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Invalid project code or link", exception?.message)
    }
    
    /**
     * 토큰으로 프로젝트 정보 조회 테스트
     */
    @Test
    fun `getProjectInfoFromToken should return project info for valid token`() = runBlocking {
        // When: 유효한 토큰으로 프로젝트 정보 조회
        val result = projectRepository.getProjectInfoFromToken(testInviteToken)
        
        // Then: 성공 및 프로젝트 정보 반환
        assertTrue(result.isSuccess)
        val projectInfo = result.getOrNull()
        assertNotNull(projectInfo)
        assertEquals(testProject.name, projectInfo?.projectName)
        assertEquals(testProject.memberCount, projectInfo?.memberCount)
    }
    
    /**
     * 잘못된 토큰으로 프로젝트 정보 조회 실패 테스트
     */
    @Test
    fun `getProjectInfoFromToken should fail for invalid token`() = runBlocking {
        // When: 잘못된 토큰으로 프로젝트 정보 조회
        val result = projectRepository.getProjectInfoFromToken("invalid-token")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Invalid project token", exception?.message)
    }
    
    /**
     * 토큰으로 프로젝트 참여 테스트
     */
    @Test
    fun `joinProjectWithToken should return projectId for valid token`() = runBlocking {
        // When: 유효한 토큰으로 참여
        val result = projectRepository.joinProjectWithToken(testInviteToken)
        
        // Then: 성공 및 프로젝트 ID 반환
        assertTrue(result.isSuccess)
        assertEquals(testProjectId, result.getOrNull())
    }
    
    /**
     * 잘못된 토큰으로 프로젝트 참여 실패 테스트
     */
    @Test
    fun `joinProjectWithToken should fail for invalid token`() = runBlocking {
        // When: 잘못된 토큰으로 참여
        val result = projectRepository.joinProjectWithToken("invalid-token")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Invalid project token", exception?.message)
    }
    
    /**
     * 프로젝트 생성 테스트
     */
    @Test
    fun `createProject should create and return new project`() = runBlocking {
        // Given: 새 프로젝트 정보
        val newProjectName = "새 프로젝트"
        val newProjectDesc = "새 프로젝트 설명"
        
        // When: 프로젝트 생성
        val result = projectRepository.createProject(newProjectName, newProjectDesc, false)
        
        // Then: 성공 및 생성된 프로젝트 반환
        assertTrue(result.isSuccess)
        val newProject = result.getOrNull()
        assertNotNull(newProject)
        assertEquals(newProjectName, newProject?.name)
        assertEquals(newProjectDesc, newProject?.description)
        assertEquals(false, newProject?.isPublic)
        
        // Then: 프로젝트 목록에 새 프로젝트 포함 확인
        val projects = projectRepository.getProjectListStream().first()
        assertTrue(projects.any { it.name == newProjectName })
    }
    
    /**
     * 중복된 이름으로 프로젝트 생성 실패 테스트
     */
    @Test
    fun `createProject should fail for duplicate name`() = runBlocking {
        // When: 이미 사용 중인 이름으로 프로젝트 생성
        val result = projectRepository.createProject(testProject.name, "설명", true)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Project name already in use", exception?.message)
    }
    
    /**
     * 빈 이름으로 프로젝트 생성 실패 테스트
     */
    @Test
    fun `createProject should fail for blank name`() = runBlocking {
        // When: 빈 이름으로 프로젝트 생성
        val result = projectRepository.createProject("   ", "설명", true)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Project name cannot be blank", exception?.message)
    }
    
    /**
     * 일정 추가용 프로젝트 목록 조회 테스트
     */
    @Test
    fun `getAvailableProjectsForScheduling should return projects list`() = runBlocking {
        // When: 일정 추가용 프로젝트 목록 조회
        val result = projectRepository.getAvailableProjectsForScheduling()
        
        // Then: 성공 및 프로젝트 목록 반환
        assertTrue(result.isSuccess)
        val projects = result.getOrNull()
        assertNotNull(projects)
        assertEquals(1, projects?.size)
        assertEquals(testProject, projects?.first())
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("Test error")
        projectRepository.setShouldSimulateError(true, testError)
        
        // When: 작업 수행
        val result = projectRepository.fetchProjectList()
        
        // Then: 실패 및 시뮬레이션된 에러 반환
        assertTrue(result.isFailure)
        assertEquals(testError, result.exceptionOrNull())
    }
} 