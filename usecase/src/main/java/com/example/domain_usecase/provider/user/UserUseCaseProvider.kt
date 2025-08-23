package com.example.domain_usecase.provider.user

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.user.CheckNicknameAvailabilityUseCase
import com.example.domain_usecase.usecase.user.CheckNicknameAvailabilityUseCaseImpl
import com.example.domain_usecase.usecase.user.FindUserIdByUserNameUseCase
import com.example.domain_usecase.usecase.user.FindUserIdByUserNameUseCaseImpl
import com.example.domain_usecase.usecase.user.GetCurrentUserStreamUseCase
import com.example.domain_usecase.usecase.user.GetCurrentUserStreamUseCaseImpl
import com.example.domain_usecase.usecase.user.GetUserByIdUseCase
import com.example.domain_usecase.usecase.user.GetUserByIdUseCaseImpl
import com.example.domain_usecase.usecase.user.GetUserStreamUseCase
import com.example.domain_usecase.usecase.user.GetUserStreamUseCaseImpl
import com.example.domain_usecase.usecase.user.GetUsersUseCase
import com.example.domain_usecase.usecase.user.GetUsersUseCaseImpl
import com.example.domain_usecase.usecase.user.ObserveUserUpdatedAtUseCase
import com.example.domain_usecase.usecase.user.ObserveUserUpdatedAtUseCaseImpl
import com.example.domain_usecase.usecase.user.RemoveProfileImageUseCase
import com.example.domain_usecase.usecase.user.RemoveProfileImageUseCaseImpl
import com.example.domain_usecase.usecase.user.SearchUserByNameUseCase
import com.example.domain_usecase.usecase.user.SearchUserByNameUseCaseImpl
import com.example.domain_usecase.usecase.user.UpdateFcmTokenUseCase
import com.example.domain_usecase.usecase.user.UpdateFcmTokenUseCaseImpl
import com.example.domain_usecase.usecase.user.UpdateNameUseCase
import com.example.domain_usecase.usecase.user.UpdateUserMemoUseCase
import com.example.domain_usecase.usecase.user.UpdateUserMemoUseCaseImpl
import com.example.domain_usecase.usecase.user.UpdateUserStatusUseCase
import com.example.domain_usecase.usecase.user.UpdateUserStatusUseCaseImpl
import com.example.domain_usecase.usecase.user.UploadProfileImageUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관련 UseCase들을 제공하는 Provider
 * 
 * 사용자 정보 조회, 프로필 관리, 인증 등의 기능을 담당합니다.
 */
@Singleton
class UserUseCaseProvider @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) {

    /**
     * 특정 사용자에 대한 UseCase들을 생성합니다.
     * 
     * @param userId 대상 사용자 ID (null이면 현재 로그인 사용자)
     * @return 사용자 관련 UseCase 그룹
     */
    fun createForUser(userId: String? = null): UserUseCases {
        userRepository.setCollection(CollectionPath.users)


        return UserUseCases(
            getUserStreamUseCase = GetUserStreamUseCaseImpl(
                userRepository = this.userRepository
            ),
            
            getUserByIdUseCase = GetUserByIdUseCaseImpl(
                userRepository = this.userRepository
            ),
            
            getUsersUseCase = GetUsersUseCaseImpl(
                userRepository = this.userRepository
            ),
            
            getCurrentUserStreamUseCase = GetCurrentUserStreamUseCaseImpl(
                userRepository = this.userRepository,
                authRepository = this.authRepository
            ),

            searchUserByNameUseCase = SearchUserByNameUseCaseImpl(
                userRepository = this.userRepository
            ),
            
            observeUserUpdatedAtUseCase = ObserveUserUpdatedAtUseCaseImpl(
                userRepository = this.userRepository
            ),
            
            updateUserStatusUseCase = UpdateUserStatusUseCaseImpl(
                userRepository = this.userRepository,
                authRepository = this.authRepository,
            ),
            updateUserMemoUseCase = UpdateUserMemoUseCaseImpl(
                userRepository = this.userRepository,
                authRepository = this.authRepository
            ),
            updateNameUseCase = UpdateNameUseCase(
                userRepository = this.userRepository,
                authRepository = this.authRepository
            ),
            updateFcmTokenUseCase = UpdateFcmTokenUseCaseImpl(
                userRepository = this.userRepository,
                authRepository = this.authRepository
            ),


            checkNicknameAvailabilityUseCase = CheckNicknameAvailabilityUseCaseImpl(
                userRepository = this.userRepository
            ),


            removeProfileImageUseCase = RemoveProfileImageUseCaseImpl(
                userRepository = this.userRepository,
                authRepository = this.authRepository
            ),

            uploadProfileImageUseCase = UploadProfileImageUseCase(
                userRepository = this.userRepository
            ),

            findUserIdByUserNameUseCase = FindUserIdByUserNameUseCaseImpl(
                userRepository = this.userRepository
            ),

            userRepository = this.userRepository,
            authRepository = this.authRepository,
        )
    }
}

/**
 * 사용자 관련 UseCase 그룹
 */
data class UserUseCases(
    // 사용자 정보 조회
    val getUserStreamUseCase: GetUserStreamUseCase,
    val getUserByIdUseCase: GetUserByIdUseCase,
    val getUsersUseCase: GetUsersUseCase,
    val getCurrentUserStreamUseCase: GetCurrentUserStreamUseCase,
    val searchUserByNameUseCase: SearchUserByNameUseCase,
    val observeUserUpdatedAtUseCase: ObserveUserUpdatedAtUseCase,
    
    // 프로필 관리 (구현체만)
    val updateUserStatusUseCase: UpdateUserStatusUseCase,
    val updateUserMemoUseCase: UpdateUserMemoUseCase,
    val updateNameUseCase: UpdateNameUseCase,
    val updateFcmTokenUseCase: UpdateFcmTokenUseCase,

    val checkNicknameAvailabilityUseCase: CheckNicknameAvailabilityUseCase,
    val removeProfileImageUseCase: RemoveProfileImageUseCase,
    val uploadProfileImageUseCase: UploadProfileImageUseCase,

    // 사용자 검색
    val findUserIdByUserNameUseCase: FindUserIdByUserNameUseCase,

    val userRepository: UserRepository,
    val authRepository: AuthRepository
)