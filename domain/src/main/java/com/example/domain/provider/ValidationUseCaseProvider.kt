package com.example.domain.provider

import com.example.domain.repository.local.UserLocalRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 유효성 검사 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 이메일, 비밀번호, 닉네임 등의 유효성 검사 기능을 담당합니다.
 */
@Singleton
class ValidationUseCaseProvider @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) {

    /**
     * 유효성 검사 관련 UseCase들을 생성합니다.
     * 
     * @return 유효성 검사 UseCase 그룹
     */
    fun create(): ValidationLocalUseCases {
        return ValidationLocalUseCases(
            // TODO: 향후 local validation use cases 추가
            userLocalRepository = userLocalRepository
        )
    }
}

/**
 * 유효성 검사 Local UseCase 그룹
 */
data class ValidationLocalUseCases(
    val userLocalRepository: UserLocalRepository
) 