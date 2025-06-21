package com.example.data.repository

import com.example.domain.model.User
import com.example.domain.repository.base.AuthRepository
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant

/**
 * AuthRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 AuthRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 인증 기능을 테스트할 수 있습니다.
 */
class FakeAuthRepository : AuthRepository {
    
    // 테스트 사용자 데이터 저장소
    private val users = ConcurrentHashMap<String, User>()
    
    // 비밀번호 저장소 (이메일 -> 비밀번호)
    private val passwords = ConcurrentHashMap<String, String>()
    
    // 현재 로그인된 사용자
    private var currentUser: User? = null
    
    // 비밀번호 재설정 코드 저장소 (이메일 -> 코드)
    private val resetCodes = ConcurrentHashMap<String, String>()
    
    // 에러 시뮬레이션 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 사용자 추가
     */
    fun addUser(user: User, password: String) {
        users[user.email] = user
        passwords[user.email] = password
    }
    
    /**
     * 테스트를 위해 현재 로그인된 사용자 설정
     */
    fun setCurrentUser(user: User?) {
        currentUser = user
    }
    
    /**
     * 테스트를 위해 비밀번호 재설정 코드 설정
     */
    fun setResetCode(email: String, code: String) {
        resetCodes[email] = code
    }
    
    /**
     * 테스트를 위해 모든 사용자 데이터 초기화
     */
    fun clearUsers() {
        users.clear()
        passwords.clear()
        currentUser = null
        resetCodes.clear()
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
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
    
    override suspend fun isLoggedIn(): Boolean {
        return currentUser != null
    }
    
    override suspend fun login(email: String, pass: String): Result<User?> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<User?>()?.let { return it }
        
        // 이메일로 사용자 찾기
        val user = users[email]
        val storedPassword = passwords[email]
        
        return when {
            user == null -> {
                Result.failure(NoSuchElementException("사용자를 찾을 수 없습니다: $email"))
            }
            storedPassword != pass -> {
                Result.failure(IllegalArgumentException("비밀번호가 일치하지 않습니다"))
            }
            else -> {
                currentUser = user
                Result.success(user)
            }
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        currentUser = null
        return Result.success(Unit)
    }
    
    override suspend fun requestPasswordResetCode(email: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 사용자가 존재하는지 확인
        if (!users.containsKey(email)) {
            return Result.failure(NoSuchElementException("사용자를 찾을 수 없습니다: $email"))
        }
        
        // 테스트 코드 생성 (실제로는 랜덤 생성 및 이메일 전송)
        val code = "123456"
        resetCodes[email] = code
        
        return Result.success(Unit)
    }
    
    override suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 저장된 코드 확인
        val storedCode = resetCodes[email]
        
        return when {
            storedCode == null -> {
                Result.failure(IllegalStateException("비밀번호 재설정 코드를 요청하지 않았습니다"))
            }
            storedCode != code -> {
                Result.failure(IllegalArgumentException("인증 코드가 일치하지 않습니다"))
            }
            else -> {
                Result.success(Unit)
            }
        }
    }
    
    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 코드 검증
        val verifyResult = verifyPasswordResetCode(email, code)
        if (verifyResult.isFailure) {
            return verifyResult
        }
        
        // 비밀번호 변경
        passwords[email] = newPassword
        resetCodes.remove(email) // 사용한 코드 제거
        
        return Result.success(Unit)
    }
    
    override suspend fun signUp(email: String, pass: String, name: String, consentTimeStamp: Instant): Result<User?> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<User?>()?.let { return it }
        
        // 이메일 중복 확인
        if (users.containsKey(email)) {
            return Result.failure(IllegalStateException("이미 사용 중인 이메일입니다: $email"))
        }
        
        // 새 사용자 생성
        val newUser = User(
            userId = (users.size + 1).toString(),
            email = email,
            name = name,
            profileImageUrl = null,
            statusMessage = null,
            consentTimeStamp = consentTimeStamp
        )
        
        // 사용자 및 비밀번호 저장
        users[email] = newUser
        passwords[email] = pass
        
        // 사용자 자동 로그인
        currentUser = newUser
        
        return Result.success(newUser)
    }
    
    override suspend fun getLoginErrorMessage(exception: Throwable): String {
        return when (exception) {
            is NoSuchElementException -> "사용자를 찾을 수 없습니다"
            is IllegalArgumentException -> "비밀번호가 일치하지 않습니다"
            else -> "로그인 중 오류가 발생했습니다: ${exception.message}"
        }
    }
    
    override suspend fun getSignUpErrorMessage(exception: Throwable): String {
        return when (exception) {
            is IllegalStateException -> "이미 사용 중인 이메일입니다"
            else -> "회원가입 중 오류가 발생했습니다: ${exception.message}"
        }
    }
} 