package com.example.domain.usecase.auth.account

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.repository.remote.AuthRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 탈퇴한 계정을 재활성화하는 유스케이스입니다.
 */
class ReactivateAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: LocalUserRepository
) {

    suspend operator fun invoke(
        email: String,
        nickname: String,
        consentTimeStamp: Instant
    ): CustomResult<User, Exception> {
        // TODO: 계정 재활성화 로직 구현 필요
        // - userRepository.observeByEmail()로 탈퇴한 사용자 조회
        // - user.reactivateAccount() 호출
        // - user.changeName(nickname) 호출
        // - 변경된 사용자 정보 저장
        TODO("ReactivateAccountUseCase implementation needed")
    }
}
