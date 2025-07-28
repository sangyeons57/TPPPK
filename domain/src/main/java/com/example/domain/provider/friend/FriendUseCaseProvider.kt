package com.example.domain.provider.friend

import com.example.core_common.result.getOrNull
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.base.User
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.usecase.friend.AcceptFriendRequestUseCase
import com.example.domain.usecase.friend.GetFriendsListStreamUseCase
import com.example.domain.usecase.friend.GetPendingFriendRequestsUseCase
import com.example.domain.usecase.friend.RemoveOrDenyFriendUseCase
import com.example.domain.usecase.friend.RemoveFriendUseCase
import com.example.domain.usecase.friend.RejectFriendRequestUseCase
import com.example.domain.usecase.friend.SendFriendRequestUseCaseImpl
import com.example.domain.usecase.friend.ValidateSearchQueryUseCase
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.domain.model.base.Friend
import com.example.domain.repository.remote.AuthRepository

/**
 * 친구 관련 UseCase들을 제공하는 Provider
 * 
 * 친구 요청, 수락, 거절, 친구 목록 조회 등의 기능을 담당합니다.
 */

@Singleton
class FriendUseCaseProvider @Inject constructor(
    private val friendRepository: DefaultRepository<Friend>,
    private val userRepository: DefaultRepository<User>,
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
        friendRepository.setCollection(CollectionPath.userFriends(userId))
        userRepository.setCollection(CollectionPath.users)

        val useCases = FriendUseCases(
            sendFriendRequestUseCase = SendFriendRequestUseCaseImpl(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            acceptFriendRequestUseCase = AcceptFriendRequestUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            removeOrDenyFriendUseCase = RemoveOrDenyFriendUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            removeFriendUseCase = RemoveFriendUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            rejectFriendRequestUseCase = RejectFriendRequestUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            getFriendsListStreamUseCase = GetFriendsListStreamUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCase(
                friendRepository = friendRepository
            ),
            
            validateSearchQueryUseCase = ValidateSearchQueryUseCase(),
            
            // 공통 Repository
            authRepository = authRepository,
            friendRepository = friendRepository,
            userRepository = userRepository
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

        userRepository.setCollection(CollectionPath.users)

        // 현재 사용자 ID를 가져와서 FriendRepository 생성
        val currentUserResult = authRepository.getCurrentUserSession()
        val currentUserId = currentUserResult.getOrNull()?.userId?.value 
            ?: throw IllegalStateException("User not authenticated")
        
        friendRepository.setCollection(CollectionPath.userFriends(currentUserId))

        val useCases = FriendUseCases(
            sendFriendRequestUseCase = SendFriendRequestUseCaseImpl(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            acceptFriendRequestUseCase = AcceptFriendRequestUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            removeOrDenyFriendUseCase = RemoveOrDenyFriendUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            removeFriendUseCase = RemoveFriendUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            rejectFriendRequestUseCase = RejectFriendRequestUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            getFriendsListStreamUseCase = GetFriendsListStreamUseCase(
                friendRepository = friendRepository,
                authRepository = authRepository
            ),
            
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCase(
                friendRepository = friendRepository
            ),
            
            validateSearchQueryUseCase = ValidateSearchQueryUseCase(),
            
            // 공통 Repository
            authRepository = authRepository,
            friendRepository = friendRepository,
            userRepository = userRepository
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
    val friendRepository: DefaultRepository<Friend>,
    val userRepository: DefaultRepository<User>
)