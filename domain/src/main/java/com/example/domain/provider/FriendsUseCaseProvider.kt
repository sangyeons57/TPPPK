package com.example.domain.provider

import com.example.domain.repository.local.LocalFriendRepository
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.usecase.local.friends.AcceptFriendRequestLocalUseCase
import com.example.domain.usecase.local.friends.AcceptFriendRequestLocalUseCaseImpl
import com.example.domain.usecase.local.friends.DeleteFriendUseCase
import com.example.domain.usecase.local.friends.DeleteFriendUseCaseImpl
import com.example.domain.usecase.local.friends.GetFriendUseCase
import com.example.domain.usecase.local.friends.GetFriendUseCaseImpl
import com.example.domain.usecase.local.friends.GetFriendsUseCase
import com.example.domain.usecase.local.friends.GetFriendsUseCaseImpl
import com.example.domain.usecase.local.friends.GetPendingFriendRequestsUseCase
import com.example.domain.usecase.local.friends.GetPendingFriendRequestsUseCaseImpl
import com.example.domain.usecase.local.friends.InsertFriendUseCase
import com.example.domain.usecase.local.friends.InsertFriendUseCaseImpl
import com.example.domain.usecase.local.friends.SendFriendRequestLocalUseCase
import com.example.domain.usecase.local.friends.SendFriendRequestLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 친구 요청, 수락, 삭제, 조회 등의 기능을 담당합니다.
 */
@Singleton
class FriendsUseCaseProvider @Inject constructor(
    private val friendLocalRepository: LocalFriendRepository,
    private val authRepository: AuthRepository
) {

    /**
     * 친구 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 친구 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): FriendsLocalBasicUseCases {
        return FriendsLocalBasicUseCases(
            // 친구 조회
            getFriendsUseCase = GetFriendsUseCaseImpl(
                friendLocalRepository = friendLocalRepository
            ),
            
            getFriendUseCase = GetFriendUseCaseImpl(
                friendLocalRepository = friendLocalRepository
            ),
            
            // 친구 관리
            insertFriendUseCase = InsertFriendUseCaseImpl(
                friendLocalRepository = friendLocalRepository
            ),
            
            deleteFriendUseCase = DeleteFriendUseCaseImpl(
                friendLocalRepository = friendLocalRepository
            ),
            
            friendLocalRepository = friendLocalRepository
        )
    }

    /**
     * 친구 요청 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 친구 요청 관리 UseCase 그룹
     */
    fun createRequestUseCases(): FriendsLocalRequestUseCases {
        return FriendsLocalRequestUseCases(
            // 친구 요청 전송
            sendFriendRequestLocalUseCase = SendFriendRequestLocalUseCaseImpl(
                friendLocalRepository = friendLocalRepository,
                authRepository = authRepository
            ),
            
            // 친구 요청 수락
            acceptFriendRequestLocalUseCase = AcceptFriendRequestLocalUseCaseImpl(
                friendLocalRepository = friendLocalRepository,
                authRepository = authRepository
            ),
            
            // 대기중인 친구 요청 조회
            getPendingFriendRequestsUseCase = GetPendingFriendRequestsUseCaseImpl(
                friendLocalRepository = friendLocalRepository
            ),
            
            friendLocalRepository = friendLocalRepository,
            authRepository = authRepository
        )
    }
}

/**
 * 친구 기본 관리 Local UseCase 그룹
 */
data class FriendsLocalBasicUseCases(
    // 친구 조회
    val getFriendsUseCase: GetFriendsUseCase,
    val getFriendUseCase: GetFriendUseCase,
    
    // 친구 관리
    val insertFriendUseCase: InsertFriendUseCase,
    val deleteFriendUseCase: DeleteFriendUseCase,

    val friendLocalRepository: LocalFriendRepository
)

/**
 * 친구 요청 관리 Local UseCase 그룹
 */
data class FriendsLocalRequestUseCases(
    // 친구 요청 전송
    val sendFriendRequestLocalUseCase: SendFriendRequestLocalUseCase,
    
    // 친구 요청 수락
    val acceptFriendRequestLocalUseCase: AcceptFriendRequestLocalUseCase,
    
    // 대기중인 친구 요청 조회
    val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,

    val friendLocalRepository: LocalFriendRepository,
    val authRepository: AuthRepository
) 