package com.example.domain_usecase.provider.friend

import android.util.Log
import com.example.core_common.result.getOrNull
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.FriendRepository
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.friend.AcceptFriendRequestUseCase
import com.example.domain_usecase.usecase.friend.GetFriendsListStreamUseCase
import com.example.domain_usecase.usecase.friend.GetPendingFriendRequestsUseCase
import com.example.domain_usecase.usecase.friend.RejectFriendRequestUseCase
import com.example.domain_usecase.usecase.friend.RemoveFriendUseCase
import com.example.domain_usecase.usecase.friend.RemoveOrDenyFriendUseCase
import com.example.domain_usecase.usecase.friend.SendFriendRequestUseCaseImpl
import com.example.domain_usecase.usecase.friend.ValidateSearchQueryUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 관련 UseCase들을 제공하는 Provider
 * 
 * 친구 요청, 수락, 거절, 친구 목록 조회 등의 기능을 담당합니다.
 */
@Singleton
class FriendUseCaseProvider @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {

    private val TAG = "FriendUseCaseProvider"

    /**
     * 특정 사용자의 친구 관련 UseCase들을 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return 친구 관련 UseCase 그룹
     */
    fun createForUser(userId: String): FriendUseCases {
        Log.d(TAG, "createForUser called with userId=$userId")

        // Set collection paths for repositories
        friendRepository.setCollection(CollectionPath.userFriends(userId))
        userRepository.setCollection(CollectionPath.users)

        val useCases = FriendUseCases(
            sendFriendRequestUseCase = SendFriendRequestUseCaseImpl(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            acceptFriendRequestUseCase = AcceptFriendRequestUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            removeOrDenyFriendUseCase = RemoveOrDenyFriendUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            removeFriendUseCase = RemoveFriendUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            rejectFriendRequestUseCase = RejectFriendRequestUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            getFriendsListStreamUseCase = GetFriendsListStreamUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCase(
                friendRepository = this.friendRepository
            ),
            
            validateSearchQueryUseCase = ValidateSearchQueryUseCase(),
            
            // 공통 Repository
            authRepository = this.authRepository,
            friendRepository = this.friendRepository,
            userRepository = this.userRepository
        )
        Log.d(TAG, "FriendUseCases for user created")
        return useCases
    }

    /**
     * 현재 로그인한 사용자의 친구 관련 UseCase들을 생성합니다.
     * 
     * @return 친구 관련 UseCase 그룹 (현재 사용자 기준)
     */
    suspend fun createForCurrentUser(): FriendUseCases {
        Log.d(TAG, "createForCurrentUser called")

        // 현재 사용자 ID를 가져와서 FriendRepository 경로 설정
        val currentUserResult = authRepository.getCurrentUserSession()
        val currentUserId = currentUserResult.getOrNull()?.userId?.value 
            ?: throw IllegalStateException("User not authenticated")

        // Set collection paths for repositories
        friendRepository.setCollection(CollectionPath.userFriends(currentUserId))
        userRepository.setCollection(CollectionPath.users)

        val useCases = FriendUseCases(
            sendFriendRequestUseCase = SendFriendRequestUseCaseImpl(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            acceptFriendRequestUseCase = AcceptFriendRequestUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            removeOrDenyFriendUseCase = RemoveOrDenyFriendUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            removeFriendUseCase = RemoveFriendUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            rejectFriendRequestUseCase = RejectFriendRequestUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            getFriendsListStreamUseCase = GetFriendsListStreamUseCase(
                friendRepository = this.friendRepository,
                authRepository = this.authRepository
            ),
            
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCase(
                friendRepository = this.friendRepository
            ),
            
            validateSearchQueryUseCase = ValidateSearchQueryUseCase(),
            
            // 공통 Repository
            authRepository = this.authRepository,
            friendRepository = this.friendRepository,
            userRepository = this.userRepository
        )
        Log.d(TAG, "FriendUseCases for current user created (userId=$currentUserId)")
        return useCases
    }
}

/**
 * 친구 관련 UseCase 그룹
 */
data class FriendUseCases(
    val sendFriendRequestUseCase: SendFriendRequestUseCaseImpl,
    val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    val removeOrDenyFriendUseCase: RemoveOrDenyFriendUseCase,
    val removeFriendUseCase: RemoveFriendUseCase,
    val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    val getFriendsListStreamUseCase: GetFriendsListStreamUseCase,
    val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    val validateSearchQueryUseCase: ValidateSearchQueryUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val friendRepository: FriendRepository,
    val userRepository: UserRepository
)