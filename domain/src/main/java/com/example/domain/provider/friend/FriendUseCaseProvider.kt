package com.example.domain.provider.friend

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.FriendRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.friend.AcceptFriendRequestUseCase
import com.example.domain.usecase.friend.GetFriendsListStreamUseCase
import com.example.domain.usecase.friend.GetPendingFriendRequestsUseCase
import com.example.domain.usecase.friend.RemoveOrDenyFriendUseCase
import com.example.domain.usecase.friend.SendFriendRequestUseCase
import com.example.domain.usecase.friend.ValidateSearchQueryUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 관련 UseCase들을 제공하는 Provider
 * 
 * 친구 요청, 수락, 거절, 친구 목록 조회 등의 기능을 담당합니다.
 */
@Singleton
class FriendUseCaseProvider @Inject constructor(
    private val friendRepositoryFactory: RepositoryFactory<FriendRepositoryFactoryContext, FriendRepository>,
    private val userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>,
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 사용자의 친구 관련 UseCase들을 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return 친구 관련 UseCase 그룹
     */
    fun createForUser(userId: String): FriendUseCases {
        val friendRepository = friendRepositoryFactory.create(
            FriendRepositoryFactoryContext(
                collectionPath = CollectionPath.userFriends(userId)
            )
        )

        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return FriendUseCases(
            sendFriendRequestUseCase = SendFriendRequestUseCase(
                friendRepository = friendRepository,
                userRepository = userRepository,
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
            
            getFriendsListStreamUseCase = GetFriendsListStreamUseCase(
                friendRepository = friendRepository,
                userRepository = userRepository,
                authRepository = authRepository
            ),
            
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCase(
                friendRepository = friendRepository,
                userRepository = userRepository,
                authRepository = authRepository
            ),
            
            validateSearchQueryUseCase = ValidateSearchQueryUseCase(),
            
            // 공통 Repository
            authRepository = authRepository,
            friendRepository = friendRepository,
            userRepository = userRepository
        )
    }

    /**
     * 현재 로그인한 사용자의 친구 관련 UseCase들을 생성합니다.
     * 
     * @return 친구 관련 UseCase 그룹 (현재 사용자 기준)
     */
    fun createForCurrentUser(): FriendUseCases {
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )

        // 현재 사용자 ID를 기반으로 FriendRepository 생성 (AuthRepository에서 가져오도록 구성)
        val friendRepository = friendRepositoryFactory.create(
            FriendRepositoryFactoryContext(
                collectionPath = CollectionPath.friends // 전역 친구 컬렉션 사용
            )
        )

        return FriendUseCases(
            sendFriendRequestUseCase = SendFriendRequestUseCase(
                friendRepository = friendRepository,
                userRepository = userRepository,
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
            
            getFriendsListStreamUseCase = GetFriendsListStreamUseCase(
                friendRepository = friendRepository,
                userRepository = userRepository,
                authRepository = authRepository
            ),
            
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCase(
                friendRepository = friendRepository,
                userRepository = userRepository,
                authRepository = authRepository
            ),
            
            validateSearchQueryUseCase = ValidateSearchQueryUseCase(),
            
            // 공통 Repository
            authRepository = authRepository,
            friendRepository = friendRepository,
            userRepository = userRepository
        )
    }
}

/**
 * 친구 관련 UseCase 그룹
 */
data class FriendUseCases(
    val sendFriendRequestUseCase: SendFriendRequestUseCase,
    val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    val removeOrDenyFriendUseCase: RemoveOrDenyFriendUseCase,
    val getFriendsListStreamUseCase: GetFriendsListStreamUseCase,
    val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    val validateSearchQueryUseCase: ValidateSearchQueryUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val friendRepository: FriendRepository,
    val userRepository: UserRepository
)