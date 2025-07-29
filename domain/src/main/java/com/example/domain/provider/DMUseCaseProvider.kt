package com.example.domain.provider

import com.example.domain.repository.local.LocalDMChannelRepository
import com.example.domain.repository.local.LocalDMWrapperRepository
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.usecase.local.dm.AddDmChannelLocalUseCase
import com.example.domain.usecase.local.dm.AddDmChannelLocalUseCaseImpl
import com.example.domain.usecase.local.dm.BlockDMChannelLocalUseCase
import com.example.domain.usecase.local.dm.BlockDMChannelLocalUseCaseImpl
import com.example.domain.usecase.local.dm.GetDmChannelLocalUseCase
import com.example.domain.usecase.local.dm.GetDmChannelLocalUseCaseImpl
import com.example.domain.usecase.local.dm.GetUserDmChannelsLocalUseCase
import com.example.domain.usecase.local.dm.GetUserDmChannelsLocalUseCaseImpl
import com.example.domain.usecase.local.dm.UnblockDMChannelLocalUseCase
import com.example.domain.usecase.local.dm.UnblockDMChannelLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct Message 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 DM 채널 관리, 차단/해제 등의 기능을 담당합니다.
 */
@Singleton
class DMUseCaseProvider @Inject constructor(
    private val dmChannelLocalRepository: LocalDMChannelRepository,
    private val dmWrapperLocalRepository: LocalDMWrapperRepository,
    private val authRepository: AuthRepository
) {

    /**
     * DM 채널 관리 관련 UseCase들을 생성합니다.
     * 
     * @return DM 채널 관리 UseCase 그룹
     */
    fun createChannelUseCases(): DMLocalChannelUseCases {
        return DMLocalChannelUseCases(
            // 채널 조회
            getDmChannelLocalUseCase = GetDmChannelLocalUseCaseImpl(
                dmChannelLocalRepository = dmChannelLocalRepository
            ),
            
            getUserDmChannelsLocalUseCase = GetUserDmChannelsLocalUseCaseImpl(
                dmChannelLocalRepository = dmChannelLocalRepository,
                authRepository = authRepository,
                dmWrapperLocalRepository = dmWrapperLocalRepository
            ),
            
            // 채널 생성
            addDmChannelLocalUseCase = AddDmChannelLocalUseCaseImpl(
                dmChannelLocalRepository = dmChannelLocalRepository,
                authRepository = authRepository
            ),
            
            dmChannelLocalRepository = dmChannelLocalRepository,
            dmWrapperLocalRepository = dmWrapperLocalRepository,
            authRepository = authRepository
        )
    }

    /**
     * DM 채널 차단 관리 관련 UseCase들을 생성합니다.
     * 
     * @return DM 채널 차단 관리 UseCase 그룹
     */
    fun createBlockingUseCases(): DMLocalBlockingUseCases {
        return DMLocalBlockingUseCases(
            // 차단 관리
            blockDMChannelLocalUseCase = BlockDMChannelLocalUseCaseImpl(
                dmChannelLocalRepository = dmChannelLocalRepository,
                authRepository = authRepository
            ),
            
            unblockDMChannelLocalUseCase = UnblockDMChannelLocalUseCaseImpl(
                dmChannelLocalRepository = dmChannelLocalRepository,
                authRepository = authRepository
            ),
            
            dmChannelLocalRepository = dmChannelLocalRepository,
            authRepository = authRepository
        )
    }
}

/**
 * DM 채널 관리 Local UseCase 그룹
 */
data class DMLocalChannelUseCases(
    // 채널 조회
    val getDmChannelLocalUseCase: GetDmChannelLocalUseCase,
    val getUserDmChannelsLocalUseCase: GetUserDmChannelsLocalUseCase,
    
    // 채널 생성
    val addDmChannelLocalUseCase: AddDmChannelLocalUseCase,

    val dmChannelLocalRepository: LocalDMChannelRepository,
    val dmWrapperLocalRepository: LocalDMWrapperRepository,
    val authRepository: AuthRepository
)

/**
 * DM 채널 차단 관리 Local UseCase 그룹
 */
data class DMLocalBlockingUseCases(
    // 차단 관리
    val blockDMChannelLocalUseCase: BlockDMChannelLocalUseCase,
    val unblockDMChannelLocalUseCase: UnblockDMChannelLocalUseCase,

    val dmChannelLocalRepository: LocalDMChannelRepository,
    val authRepository: AuthRepository
) 