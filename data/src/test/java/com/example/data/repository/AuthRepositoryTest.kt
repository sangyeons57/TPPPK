package com.example.data.repository

import com.example.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * AuthRepository 기능 테스트
 *
 * 이 테스트는 FakeAuthRepository를 사용하여 AuthRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class AuthRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var authRepository: FakeAuthRepository
    
    // 테스트 데이터
    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testName = "테스트 사용자"
    private val testUser = User(
        userId = "1",
        email = testEmail,
        name = testName,
        profileImageUrl = null,
        statusMessage = null
    )
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeAuthRepository 초기화
        authRepository = FakeAuthRepository()
    }
    
    /**
     * 로그인 성공 테스트
     */
    @Test
    fun `login should succeed with correct credentials`() = runBlocking {
        // Given: 테스트 사용자 추가
        authRepository.addUser(testUser, testPassword)
        
        // When: 로그인 시도
        val result = authRepository.login(testEmail, testPassword)
        
        // Then: 로그인 성공 확인
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        
        // 로그인 상태 확인
        assertTrue(authRepository.isLoggedIn())
    }
    
    /**
     * 로그인 실패 테스트 - 사용자 없음
     */
    @Test
    fun `login should fail with non-existent user`() = runBlocking {
        // When: 존재하지 않는 사용자로 로그인 시도
        val result = authRepository.login("nonexistent@example.com", testPassword)
        
        // Then: 로그인 실패 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
        
        // 로그인 상태 확인
        assertFalse(authRepository.isLoggedIn())
    }
    
    /**
     * 로그인 실패 테스트 - 잘못된 비밀번호
     */
    @Test
    fun `login should fail with incorrect password`() = runBlocking {
        // Given: 테스트 사용자 추가
        authRepository.addUser(testUser, testPassword)
        
        // When: 잘못된 비밀번호로 로그인 시도
        val result = authRepository.login(testEmail, "wrongpassword")
        
        // Then: 로그인 실패 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        
        // 로그인 상태 확인
        assertFalse(authRepository.isLoggedIn())
    }
    
    /**
     * 로그아웃 테스트
     */
    @Test
    fun `logout should clear current user`() = runBlocking {
        // Given: 로그인된 사용자 설정
        authRepository.addUser(testUser, testPassword)
        authRepository.login(testEmail, testPassword)
        assertTrue(authRepository.isLoggedIn())
        
        // When: 로그아웃
        val result = authRepository.logout()
        
        // Then: 로그아웃 성공 확인
        assertTrue(result.isSuccess)
        assertFalse(authRepository.isLoggedIn())
    }
    
    /**
     * 회원가입 테스트
     */
    @Test
    fun `signUp should create new user and login`() = runBlocking {
        // When: 회원가입
        val result = authRepository.signUp(testEmail, testPassword, testName, Instant.now())
        
        // Then: 회원가입 성공 확인
        assertTrue(result.isSuccess)
        val newUser = result.getOrNull()
        assertNotNull(newUser)
        assertEquals(testEmail, newUser?.email)
        assertEquals(testName, newUser?.name)
        
        // 자동 로그인 확인
        assertTrue(authRepository.isLoggedIn())
        
        // 생성된 사용자로 다시 로그인 가능 확인
        authRepository.logout()
        val loginResult = authRepository.login(testEmail, testPassword)
        assertTrue(loginResult.isSuccess)
    }
    
    /**
     * 중복 이메일 회원가입 실패 테스트
     */
    @Test
    fun `signUp should fail with duplicate email`() = runBlocking {
        // Given: 기존 사용자
        authRepository.addUser(testUser, testPassword)
        
        // When: 같은 이메일로 회원가입 시도
        val result = authRepository.signUp(testEmail, "newpassword", "새 사용자", Instant.now())
        
        // Then: 회원가입 실패 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalStateException)
    }
    
    /**
     * 비밀번호 재설정 코드 요청 테스트
     */
    @Test
    fun `requestPasswordResetCode should store code for valid email`() = runBlocking {
        // Given: 테스트 사용자 추가
        authRepository.addUser(testUser, testPassword)
        
        // When: 비밀번호 재설정 코드 요청
        val result = authRepository.requestPasswordResetCode(testEmail)
        
        // Then: 요청 성공 확인
        assertTrue(result.isSuccess)
        
        // 코드 검증 준비 (내부 테스트 메서드로 확인)
        authRepository.setResetCode(testEmail, "123456")
        val verifyResult = authRepository.verifyPasswordResetCode(testEmail, "123456")
        assertTrue(verifyResult.isSuccess)
    }
    
    /**
     * 존재하지 않는 이메일 비밀번호 재설정 실패 테스트
     */
    @Test
    fun `requestPasswordResetCode should fail for non-existent email`() = runBlocking {
        // When: 존재하지 않는 이메일로 코드 요청
        val result = authRepository.requestPasswordResetCode("nonexistent@example.com")
        
        // Then: 요청 실패 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 비밀번호 재설정 코드 검증 테스트
     */
    @Test
    fun `verifyPasswordResetCode should succeed with correct code`() = runBlocking {
        // Given: 테스트 사용자와 재설정 코드 설정
        authRepository.addUser(testUser, testPassword)
        authRepository.setResetCode(testEmail, "123456")
        
        // When: 코드 검증
        val result = authRepository.verifyPasswordResetCode(testEmail, "123456")
        
        // Then: 검증 성공 확인
        assertTrue(result.isSuccess)
    }
    
    /**
     * 잘못된 코드 검증 실패 테스트
     */
    @Test
    fun `verifyPasswordResetCode should fail with incorrect code`() = runBlocking {
        // Given: 테스트 사용자와 재설정 코드 설정
        authRepository.addUser(testUser, testPassword)
        authRepository.setResetCode(testEmail, "123456")
        
        // When: 잘못된 코드로 검증
        val result = authRepository.verifyPasswordResetCode(testEmail, "999999")
        
        // Then: 검증 실패 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
    }
    
    /**
     * 비밀번호 재설정 테스트
     */
    @Test
    fun `resetPassword should change password when code is valid`() = runBlocking {
        // Given: 테스트 사용자와 재설정 코드 설정
        authRepository.addUser(testUser, testPassword)
        authRepository.setResetCode(testEmail, "123456")
        
        // 새 비밀번호
        val newPassword = "newPassword123"
        
        // When: 비밀번호 재설정
        val result = authRepository.resetPassword(testEmail, "123456", newPassword)
        
        // Then: 재설정 성공 확인
        assertTrue(result.isSuccess)
        
        // 새 비밀번호로 로그인 가능 확인
        val loginResult = authRepository.login(testEmail, newPassword)
        assertTrue(loginResult.isSuccess)
        
        // 기존 비밀번호로는 로그인 실패 확인
        val oldLoginResult = authRepository.login(testEmail, testPassword)
        assertTrue(oldLoginResult.isFailure)
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("테스트 에러")
        authRepository.setShouldSimulateError(true, testError)
        
        // When: 다양한 작업 시도
        val loginResult = authRepository.login(testEmail, testPassword)
        val signUpResult = authRepository.signUp(testEmail, testPassword, testName)
        val resetCodeResult = authRepository.requestPasswordResetCode(testEmail)
        
        // Then: 모든 결과가 동일한 에러를 반환해야 함
        assertTrue(loginResult.isFailure)
        assertEquals(testError, loginResult.exceptionOrNull())
        
        assertTrue(signUpResult.isFailure)
        assertEquals(testError, signUpResult.exceptionOrNull())
        
        assertTrue(resetCodeResult.isFailure)
        assertEquals(testError, resetCodeResult.exceptionOrNull())
    }
    
    /**
     * 에러 메시지 포맷 테스트
     */
    @Test
    fun `getLoginErrorMessage should format errors correctly`() = runBlocking {
        // 다양한 예외 유형에 대한 메시지 확인
        val noUserMessage = authRepository.getLoginErrorMessage(NoSuchElementException())
        val wrongPassMessage = authRepository.getLoginErrorMessage(IllegalArgumentException())
        val generalMessage = authRepository.getLoginErrorMessage(Exception("일반 오류"))
        
        // 메시지 포맷 확인
        assertTrue(noUserMessage.contains("사용자를 찾을 수 없습니다"))
        assertTrue(wrongPassMessage.contains("비밀번호가 일치하지 않습니다"))
        assertTrue(generalMessage.contains("로그인 중 오류가 발생했습니다"))
    }
    
    /**
     * 회원가입 에러 메시지 테스트
     */
    @Test
    fun `getSignUpErrorMessage should format errors correctly`() = runBlocking {
        // 다양한 예외 유형에 대한 메시지 확인
        val duplicateMessage = authRepository.getSignUpErrorMessage(IllegalStateException())
        val generalMessage = authRepository.getSignUpErrorMessage(Exception("일반 오류"))
        
        // 메시지 포맷 확인
        assertTrue(duplicateMessage.contains("이미 사용 중인 이메일입니다"))
        assertTrue(generalMessage.contains("회원가입 중 오류가 발생했습니다"))
    }
} 