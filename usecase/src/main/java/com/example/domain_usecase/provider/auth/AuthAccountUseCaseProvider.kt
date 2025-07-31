package com.example.domain_usecase.provider.auth

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.auth.account.DeleteAuthUserUseCase
import com.example.domain_usecase.usecase.auth.account.ReactivateAccountUseCase
import com.example.domain_usecase.usecase.auth.account.WithdrawMembershipUseCase
import com.example.domain_usecase.usecase.auth.account.WithdrawMembershipUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 계정 관리 UseCase들을 제공하는 Provider
 * 
 * 계정 삭제, 재활성화, 탈퇴 등의 계정 관리 기능을 담당합니다.
 */
@Singleton
class AuthAccountUseCaseProvider @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {

    /**
     * 계정 관리 UseCase들을 생성합니다.
     * 
     * @return 계정 관리 UseCase 그룹
     */
    fun create(): AuthAccountUseCases {
        // Set collection path for user repository
        userRepository.setCollection(CollectionPath.users)

        return AuthAccountUseCases(
            // 계정 관리
            deleteAuthUserUseCase = DeleteAuthUserUseCase(
                authRepository = this.authRepository
            ),
            
            reactivateAccountUseCase = ReactivateAccountUseCase(
                authRepository = this.authRepository,
                userRepository = this.userRepository
            ),
            
            withdrawMembershipUseCase = WithdrawMembershipUseCaseImpl(
                authRepository = this.authRepository,
                userRepository = this.userRepository
            )
        )
    }
}

/**
 * 계정 관리 UseCase 그룹
 */
data class AuthAccountUseCases(
    // 계정 관리
    val deleteAuthUserUseCase: DeleteAuthUserUseCase,
    val reactivateAccountUseCase: ReactivateAccountUseCase,
    val withdrawMembershipUseCase: WithdrawMembershipUseCase
)