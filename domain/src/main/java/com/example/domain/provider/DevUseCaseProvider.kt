package com.example.domain.provider

import com.example.domain.repository.local.AuthLocalRepository
import com.example.domain.repository.local.CacheLocalRepository
import com.example.domain.repository.local.NotificationLocalRepository
import com.example.domain.usecase.local.dev.ClearLocalCacheUseCase
import com.example.domain.usecase.local.dev.ClearLocalCacheUseCaseImpl
import com.example.domain.usecase.local.dev.GetLocalConnectionStatusUseCase
import com.example.domain.usecase.local.dev.GetLocalConnectionStatusUseCaseImpl
import com.example.domain.usecase.local.dev.SendLocalTestNotificationUseCase
import com.example.domain.usecase.local.dev.SendLocalTestNotificationUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 개발자 도구 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 개발/디버깅 도구들을 담당합니다.
 */
@Singleton
class DevUseCaseProvider @Inject constructor(
    private val cacheLocalRepository: CacheLocalRepository,
    private val authLocalRepository: AuthLocalRepository,
    private val notificationLocalRepository: NotificationLocalRepository
) {

    /**
     * 개발자 도구 관련 UseCase들을 생성합니다.
     * 
     * @return 개발자 도구 UseCase 그룹
     */
    fun create(): DevLocalUseCases {
        return DevLocalUseCases(
            // 캐시 관리
            clearLocalCacheUseCase = ClearLocalCacheUseCaseImpl(
                cacheLocalRepository = cacheLocalRepository
            ),
            
            // 연결 상태 확인
            getLocalConnectionStatusUseCase = GetLocalConnectionStatusUseCaseImpl(
                authLocalRepository = authLocalRepository
            ),
            
            // 테스트 도구
            sendLocalTestNotificationUseCase = SendLocalTestNotificationUseCaseImpl(
                notificationLocalRepository = notificationLocalRepository
            ),
            
            cacheLocalRepository = cacheLocalRepository,
            authLocalRepository = authLocalRepository,
            notificationLocalRepository = notificationLocalRepository
        )
    }
}

/**
 * 개발자 도구 Local UseCase 그룹
 */
data class DevLocalUseCases(
    // 캐시 관리
    val clearLocalCacheUseCase: ClearLocalCacheUseCase,
    
    // 연결 상태 확인
    val getLocalConnectionStatusUseCase: GetLocalConnectionStatusUseCase,
    
    // 테스트 도구
    val sendLocalTestNotificationUseCase: SendLocalTestNotificationUseCase,
    
    val cacheLocalRepository: CacheLocalRepository,
    val authLocalRepository: AuthLocalRepository,
    val notificationLocalRepository: NotificationLocalRepository
) 