package com.example.data.repository

import android.net.Uri
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import java.util.concurrent.ConcurrentHashMap

/**
 * UserRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 UserRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeUserRepository : UserRepository {
    
    // 인메모리 사용자 데이터 저장소
    private val users = ConcurrentHashMap<String, User>()
    
    // 현재 로그인된 사용자 ID (테스트에서 설정 가능)
    private var currentUserId: String? = null
    
    // 작업 성공/실패 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 현재 로그인된 사용자 ID 설정
     */
    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }
    
    /**
     * 테스트를 위해 사용자 추가
     */
    fun addUser(user: User) {
        users[user.userId] = user
    }
    
    /**
     * 테스트를 위해 모든 사용자 데이터 초기화
     */
    fun clearUsers() {
        users.clear()
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

    override suspend fun getUser(): Result<User> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<User>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 반환
        return users[userId]?.let {
            Result.success(it)
        } ?: Result.failure(IllegalStateException("User profile not found"))
    }

    /**
     * 프로필 이미지 업데이트
     */
    override suspend fun updateProfileImage(imageUri: Uri): Result<String?> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<String?>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 업데이트
        val user = users[userId] ?: return Result.failure(IllegalStateException("User profile not found"))
        
        // 가상의 이미지 URL 생성
        val downloadUrl = "https://fake-storage.example.com/$userId/profile.jpg"
        
        // 사용자 프로필 이미지 업데이트
        users[userId] = user.copy(profileImageUrl = downloadUrl)
        
        return Result.success(downloadUrl)
    }

    // 테스트 전용 메서드
    suspend fun updateProfileImage(imageUri: TestUri): Result<String> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<String>()?.let { return it }
        
        // 현재 사용자 가져오기
        val userId = currentUserId ?: return Result.failure(
            IllegalStateException("User not logged in")
        )
        
        val user = users[userId] ?: return Result.failure(
            IllegalStateException("User not found: $userId")
        )
        
        // 이미지 업로드를 시뮬레이션하고 URL 반환
        val imageUrl = "https://example.com/uploads/${imageUri.toString().substringAfterLast("/")}"
        
        // 사용자 정보 업데이트
        users[userId] = user.copy(profileImageUrl = imageUrl)
        
        return Result.success(imageUrl)
    }

    override suspend fun removeProfileImage(): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 업데이트
        val user = users[userId] ?: return Result.failure(IllegalStateException("User profile not found"))
        
        // 사용자 프로필 이미지 제거
        users[userId] = user.copy(profileImageUrl = null)
        
        return Result.success(Unit)
    }

    override suspend fun updateUserName(newName: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 업데이트
        val user = users[userId] ?: return Result.failure(IllegalStateException("User profile not found"))
        
        // 이름 유효성 검사
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be blank."))
        }
        
        // 사용자 이름 업데이트
        users[userId] = user.copy(name = trimmedName)
        
        return Result.success(Unit)
    }

    override suspend fun updateStatusMessage(newStatus: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 업데이트
        val user = users[userId] ?: return Result.failure(IllegalStateException("User profile not found"))
        
        // 상태 메시지 업데이트
        users[userId] = user.copy(statusMessage = newStatus.trim().takeIf { it.isNotEmpty() })
        
        return Result.success(Unit)
    }

    override suspend fun getCurrentStatus(): Result<UserStatus> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<UserStatus>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 업데이트
        val user = users[userId] ?: return Result.failure(IllegalStateException("User profile not found"))
        
        // 상태 문자열을 UserStatus로 변환
        val userStatus = if(user.status != null) {
            UserStatus.entries.find { it.name.equals(user.status, ignoreCase = true) } ?: UserStatus.OFFLINE
        } else UserStatus.OFFLINE
        
        return Result.success(userStatus)
    }

    override suspend fun updateUserStatus(status: UserStatus): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 현재 사용자 확인
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not logged in"))
        
        // 사용자 데이터 업데이트
        val user = users[userId] ?: return Result.failure(IllegalStateException("User profile not found"))
        
        // 상태 업데이트
        users[userId] = user.copy(status = status.name)
        
        return Result.success(Unit)
    }

    /**
     * 프로필 존재 확인 및 생성
     */
    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<User> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<User>()?.let { return it }
        
        // 이미 존재하는 사용자 확인
        val existingUser = users[firebaseUser.uid]
        if (existingUser != null) {
            return Result.success(existingUser)
        }
        
        // 새 사용자 생성
        val newUser = User(
            userId = firebaseUser.uid,
            name = firebaseUser.displayName?.takeIf { it.isNotBlank() } 
                ?: firebaseUser.email?.substringBefore('@')?.takeIf { it.isNotBlank() } 
                ?: "사용자",
            email = firebaseUser.email ?: "",
            profileImageUrl = firebaseUser.photoUrl?.toString(),
            statusMessage = null
        )
        
        // 사용자 저장
        users[firebaseUser.uid] = newUser
        
        return Result.success(newUser)
    }

    // 테스트 전용 메서드
    suspend fun ensureUserProfileExists(firebaseUser: TestFirebaseUser): Result<User> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<User>()?.let { return it }
        
        // Firebase User ID를 기반으로 사용자 검색
        val userId = firebaseUser.uid
        val existingUser = users[userId]
        
        if (existingUser != null) {
            return Result.success(existingUser)
        }
        
        // 존재하지 않으면 새 사용자 생성
        val newUser = User(
            userId = userId,
            name = firebaseUser.displayName ?: "New User",
            email = firebaseUser.email ?: "",
            profileImageUrl = null,
            statusMessage = null
        )
        
        // 사용자 저장
        users[userId] = newUser
        
        return Result.success(newUser)
    }
} 