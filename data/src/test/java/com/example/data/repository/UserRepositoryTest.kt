package com.example.data.repository

import android.net.Uri
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * UserRepository 기능 테스트
 *
 * 이 테스트는 FakeUserRepository를 사용하여 UserRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class UserRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var userRepository: FakeUserRepository
    
    // 테스트 데이터
    private val testUserId = "test-user-123"
    private val testUser = User(
        userId = testUserId,
        name = "테스트 사용자",
        email = "test@example.com",
        profileImageUrl = null,
        status = null,
        statusMessage = null
    )
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeUserRepository 초기화
        userRepository = FakeUserRepository()
        
        // 테스트 사용자 ID 설정
        userRepository.setCurrentUserId(testUserId)
        
        // 테스트 사용자 추가
        userRepository.addUser(testUser)
    }
    
    /**
     * 사용자 정보 조회 테스트
     */
    @Test
    fun `getUser should return current user when logged in`() = runBlocking {
        // When: 사용자 정보 조회
        val result = userRepository.getUser()
        
        // Then: 성공 및 올바른 사용자 정보 반환
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }
    
    /**
     * 로그인되지 않은 상태에서 사용자 정보 조회 실패 테스트
     */
    @Test
    fun `getUser should fail when not logged in`() = runBlocking {
        // Given: 로그인되지 않은 상태
        userRepository.setCurrentUserId(null)
        
        // When: 사용자 정보 조회
        val result = userRepository.getUser()
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalStateException)
        assertEquals("User not logged in", exception?.message)
    }
    
    /**
     * 프로필 이미지 업데이트 테스트
     */
    @Test
    fun `updateProfileImage should update user's profile image URL`() = runBlocking {
        // Given: 가상의 이미지 URI
        val imageUri = mock(Uri::class.java)
        
        // When: 프로필 이미지 업데이트
        val result = userRepository.updateProfileImage(imageUri)
        
        // Then: 성공 및 이미지 URL 반환
        assertTrue(result.isSuccess)
        val imageUrl = result.getOrNull()
        assertNotNull(imageUrl)
        
        // Then: 사용자 정보에 이미지 URL 업데이트 확인
        val userResult = userRepository.getUser()
        assertEquals(imageUrl, userResult.getOrNull()?.profileImageUrl)
    }
    
    /**
     * 프로필 이미지 제거 테스트
     */
    @Test
    fun `removeProfileImage should set profile image URL to null`() = runBlocking {
        // Given: 프로필 이미지가 있는 사용자
        val userWithImage = testUser.copy(profileImageUrl = "https://example.com/image.jpg")
        userRepository.addUser(userWithImage)
        
        // When: 프로필 이미지 제거
        val result = userRepository.removeProfileImage()
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 사용자 정보에 이미지 URL이 null로 설정됨을 확인
        val userResult = userRepository.getUser()
        assertNull(userResult.getOrNull()?.profileImageUrl)
    }
    
    /**
     * 사용자 이름 업데이트 테스트
     */
    @Test
    fun `updateUserName should update user's name`() = runBlocking {
        // Given: 새 이름
        val newName = "새로운 이름"
        
        // When: 사용자 이름 업데이트
        val result = userRepository.updateUserName(newName)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 사용자 정보에 이름이 업데이트됨을 확인
        val userResult = userRepository.getUser()
        assertEquals(newName, userResult.getOrNull()?.name)
    }
    
    /**
     * 빈 이름으로 업데이트 시 실패 테스트
     */
    @Test
    fun `updateUserName should fail with blank name`() = runBlocking {
        // Given: 빈 이름
        val blankName = "   "
        
        // When: 사용자 이름 업데이트
        val result = userRepository.updateUserName(blankName)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Username cannot be blank.", exception?.message)
        
        // Then: 사용자 이름이 변경되지 않음을 확인
        val userResult = userRepository.getUser()
        assertEquals(testUser.name, userResult.getOrNull()?.name)
    }
    
    /**
     * 상태 메시지 업데이트 테스트
     */
    @Test
    fun `updateStatusMessage should update user's status message`() = runBlocking {
        // Given: 새 상태 메시지
        val newStatusMessage = "열심히 일하는 중"
        
        // When: 상태 메시지 업데이트
        val result = userRepository.updateStatusMessage(newStatusMessage)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 사용자 정보에 상태 메시지가 업데이트됨을 확인
        val userResult = userRepository.getUser()
        assertEquals(newStatusMessage, userResult.getOrNull()?.statusMessage)
    }
    
    /**
     * 현재 상태 조회 테스트
     */
    @Test
    fun `getCurrentStatus should return user's status`() = runBlocking {
        // Given: 상태가 설정된 사용자
        val userWithStatus = testUser.copy(status = UserStatus.ONLINE.name)
        userRepository.addUser(userWithStatus)
        
        // When: 현재 상태 조회
        val result = userRepository.getCurrentStatus()
        
        // Then: 성공 및 올바른 상태 반환
        assertTrue(result.isSuccess)
        assertEquals(UserStatus.ONLINE, result.getOrNull())
    }
    
    /**
     * 상태 업데이트 테스트
     */
    @Test
    fun `updateUserStatus should update user's status`() = runBlocking {
        // Given: 새 상태
        val newStatus = UserStatus.AWAY
        
        // When: 상태 업데이트
        val result = userRepository.updateUserStatus(newStatus)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 사용자 정보에 상태가 업데이트됨을 확인
        val userResult = userRepository.getUser()
        assertEquals(newStatus.name, userResult.getOrNull()?.status)
    }
    
    /**
     * 사용자 프로필 존재 확인 및 생성 테스트 - 이미 존재하는 경우
     */
    @Test
    fun `ensureUserProfileExists should return existing user if found`() = runBlocking {
        // Given: 이미 존재하는 사용자와 FirebaseUser 모의 객체
        val firebaseUser = mock(FirebaseUser::class.java)
        `when`(firebaseUser.uid).thenReturn(testUserId)
        
        // When: 사용자 프로필 존재 확인
        val result = userRepository.ensureUserProfileExists(firebaseUser)
        
        // Then: 성공 및 기존 사용자 정보 반환
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }
    
    /**
     * 사용자 프로필 존재 확인 및 생성 테스트 - 존재하지 않는 경우
     */
    @Test
    fun `ensureUserProfileExists should create new user if not found`() = runBlocking {
        // Given: 존재하지 않는 사용자와 FirebaseUser 모의 객체
        val newUserId = "new-user-456"
        val firebaseUser = mock(FirebaseUser::class.java)
        `when`(firebaseUser.uid).thenReturn(newUserId)
        `when`(firebaseUser.displayName).thenReturn("새 사용자")
        `when`(firebaseUser.email).thenReturn("new@example.com")
        
        // 기존 사용자 지우기
        userRepository.clearUsers()
        
        // When: 사용자 프로필 존재 확인
        val result = userRepository.ensureUserProfileExists(firebaseUser)
        
        // Then: 성공 및 새 사용자 정보 반환
        assertTrue(result.isSuccess)
        val newUser = result.getOrNull()
        assertNotNull(newUser)
        assertEquals(newUserId, newUser?.userId)
        assertEquals("새 사용자", newUser?.name)
        assertEquals("new@example.com", newUser?.email)
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("Test error")
        userRepository.setShouldSimulateError(true, testError)
        
        // When: 작업 수행
        val result = userRepository.getUser()
        
        // Then: 실패 및 시뮬레이션된 에러 반환
        assertTrue(result.isFailure)
        assertEquals(testError, result.exceptionOrNull())
    }
} 