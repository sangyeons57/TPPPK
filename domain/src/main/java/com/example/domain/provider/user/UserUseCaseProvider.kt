package com.example.domain.provider.user

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.user.CheckNicknameAvailabilityUseCase
import com.example.domain.usecase.user.CheckNicknameAvailabilityUseCaseImpl
import com.example.domain.usecase.user.GetCurrentUserStreamUseCase
import com.example.domain.usecase.user.GetCurrentUserStreamUseCaseImpl
import com.example.domain.usecase.user.GetUserStreamUseCase
import com.example.domain.usecase.user.GetUserStreamUseCaseImpl
import com.example.domain.usecase.user.SearchUserByNameUseCase
import com.example.domain.usecase.user.SearchUserByNameUseCaseImpl
import com.example.domain.usecase.user.UpdateNameUseCase
// import com.example.domain.usecase.user.UpdateUserImageUseCase // interface
// import com.example.domain.usecase.user.UpdateUserStatusUseCase // interface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관련 UseCase들을 제공하는 Provider
 * 
 * 사용자 정보 조회, 프로필 관리, 인증 등의 기능을 담당합니다.
 */
@Singleton
class UserUseCaseProvider @Inject constructor(
    private val userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>,
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 사용자에 대한 UseCase들을 생성합니다.
     * 
     * @param userId 대상 사용자 ID (null이면 현재 로그인 사용자)
     * @return 사용자 관련 UseCase 그룹
     */
    fun createForUser(userId: String? = null): UserUseCases {
        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return UserUseCases(
            getUserStreamUseCase = GetUserStreamUseCaseImpl(
                userRepository = userRepository
            ),
            
            getCurrentUserStreamUseCase = GetCurrentUserStreamUseCaseImpl(
                userRepository = userRepository,
                authRepository = authRepository
            ),

            searchUserByNameUseCase = SearchUserByNameUseCaseImpl(
                userRepository = userRepository
            ),
            
            updateNameUseCase = UpdateNameUseCase(
                userRepository = userRepository,
                authRepository = authRepository
            ),

            checkNicknameAvailabilityUseCase = CheckNicknameAvailabilityUseCaseImpl(
                userRepository = userRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            userRepository = userRepository
        )
    }
}

/**
 * 사용자 관련 UseCase 그룹
 */
data class UserUseCases(
    // 사용자 정보 조회
    val getUserStreamUseCase: GetUserStreamUseCase,
    val getCurrentUserStreamUseCase: GetCurrentUserStreamUseCase,
    val searchUserByNameUseCase: SearchUserByNameUseCase,
    
    // 프로필 관리 (구현체만)
    val updateNameUseCase: UpdateNameUseCase,
    val checkNicknameAvailabilityUseCase: CheckNicknameAvailabilityUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val userRepository: UserRepository
)