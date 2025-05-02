package com.example.data.util

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.quality.Strictness
import kotlinx.coroutines.tasks.await
import org.mockito.junit.MockitoRule
import org.mockito.junit.MockitoJUnit

/**
 * FirebaseAuthWrapper 테스트
 * 
 * Mockito를 사용해 Firebase Auth의 비동기 콜백 패턴을 테스트합니다.
 * 이는 Firebase SDK 직접 테스트에 적합한 예시입니다.
 */
@ExperimentalCoroutinesApi
class FirebaseAuthWrapperTest {

    // MockitoRule with LENIENT 설정으로 불필요한 스터빙 오류 방지
    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT)

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth
    
    @Mock
    private lateinit var mockAuthResult: AuthResult
    
    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser
    
    @Mock
    private lateinit var mockTask: Task<AuthResult>
    
    private lateinit var firebaseAuthWrapper: FirebaseAuthWrapper
    
    @Before
    fun setUp() {
        // Mock 객체들이 올바르게 초기화되었는지 확인
        firebaseAuthWrapper = FirebaseAuthWrapper(mockFirebaseAuth)
        
        // Task.await() 처리를 위한 공통 모킹 설정
        `when`(mockTask.isComplete).thenReturn(true)
        `when`(mockTask.isCanceled).thenReturn(false)
        `when`(mockTask.exception).thenReturn(null)
    }
    
    /**
     * 로그인 성공 시나리오 테스트
     */
    @Test
    fun `signInWithEmail should return success result when auth succeeds`() = runTest {
        // Given: 로그인 성공 상황 설정
        val email = "test@example.com"
        val password = "password123"
        
        // Task 모킹 설정
        `when`(mockTask.isSuccessful).thenReturn(true)
        `when`(mockTask.result).thenReturn(mockAuthResult)
        `when`(mockAuthResult.user).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword(email, password)).thenReturn(mockTask)
        
        // Task.await()가 직접 결과를 반환하도록 모킹
        // mockAwait(mockTask) - 불필요한 헬퍼 메서드 제거
        
        // When: 로그인 시도
        val result = firebaseAuthWrapper.signInWithEmail(email, password)
        
        // Then: 결과 검증
        verify(mockFirebaseAuth).signInWithEmailAndPassword(email, password)
        assert(result.isSuccess)
        assert(result.getOrNull() == mockFirebaseUser)
    }
    
    /**
     * 로그인 실패 시나리오 테스트
     */
    @Test
    fun `signInWithEmail should return failure result when auth fails`() = runTest {
        // Given: 로그인 실패 상황 설정
        val email = "test@example.com"
        val password = "wrong_password"
        val exception = Exception("잘못된 비밀번호")
        
        // Task 모킹 설정
        `when`(mockTask.isSuccessful).thenReturn(false)
        `when`(mockTask.exception).thenReturn(exception)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword(email, password)).thenReturn(mockTask)
        
        // 실패 케이스에서는 exception을 던지도록 설정
        `when`(mockTask.isComplete).thenReturn(true)
        `when`(mockTask.isCanceled).thenReturn(false)
        
        // When: 로그인 시도
        val result = firebaseAuthWrapper.signInWithEmail(email, password)
        
        // Then: 결과 검증
        verify(mockFirebaseAuth).signInWithEmailAndPassword(email, password)
        assert(result.isFailure)
    }
    
    /**
     * 회원가입 성공 시나리오 테스트
     */
    @Test
    fun `createUserWithEmail should return success result when auth succeeds`() = runTest {
        // Given: 회원가입 성공 상황 설정
        val email = "newuser@example.com"
        val password = "newpassword123"
        
        // Task 모킹 설정
        `when`(mockTask.isSuccessful).thenReturn(true)
        `when`(mockTask.result).thenReturn(mockAuthResult)
        `when`(mockAuthResult.user).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseAuth.createUserWithEmailAndPassword(email, password)).thenReturn(mockTask)
        
        // When: 회원가입 시도
        val result = firebaseAuthWrapper.createUserWithEmail(email, password)
        
        // Then: 결과 검증
        verify(mockFirebaseAuth).createUserWithEmailAndPassword(email, password)
        assert(result.isSuccess)
        assert(result.getOrNull() == mockFirebaseUser)
    }
    
    /**
     * 비밀번호 재설정 이메일 전송 테스트
     */
    @Test
    fun `sendPasswordResetEmail should return success when email is sent`() = runTest {
        // Given: 비밀번호 재설정 이메일 전송 성공 상황 설정
        val email = "user@example.com"
        val mockResetTask = mock(Task::class.java) as Task<Void>
        
        // Task 모킹 설정
        `when`(mockResetTask.isSuccessful).thenReturn(true)
        `when`(mockResetTask.isComplete).thenReturn(true)
        `when`(mockResetTask.isCanceled).thenReturn(false)
        `when`(mockResetTask.exception).thenReturn(null)
        `when`(mockFirebaseAuth.sendPasswordResetEmail(email)).thenReturn(mockResetTask)
        
        // When: 비밀번호 재설정 이메일 전송 시도
        val result = firebaseAuthWrapper.sendPasswordResetEmail(email)
        
        // Then: 결과 검증
        verify(mockFirebaseAuth).sendPasswordResetEmail(email)
        assert(result.isSuccess)
    }
    
    /**
     * getCurrentUser 테스트
     */
    @Test
    fun `getCurrentUser should return current user from Firebase Auth`() {
        // Given: 현재 사용자 설정
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        
        // When: 현재 사용자 가져오기
        val user = firebaseAuthWrapper.getCurrentUser()
        
        // Then: 결과 검증
        verify(mockFirebaseAuth).currentUser
        assert(user == mockFirebaseUser)
    }
    
    /**
     * signOut 테스트
     */
    @Test
    fun `signOut should call Firebase Auth signOut`() {
        // When: 로그아웃
        firebaseAuthWrapper.signOut()
        
        // Then: Firebase Auth의 signOut 메서드 호출 확인
        verify(mockFirebaseAuth).signOut()
    }
} 