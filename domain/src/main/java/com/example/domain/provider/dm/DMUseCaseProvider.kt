package com.example.domain.provider.dm

import com.example.domain.model.base.DMChannel
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.UserId
import com.example.domain.model.base.User
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.usecase.dm.AddDmChannelUseCase
import com.example.domain.usecase.dm.BlockDMChannelUseCase
import com.example.domain.usecase.dm.UnblockDMChannelUseCase
import com.example.domain.usecase.dm.GetCurrentUserDmChannelsUseCase
import com.example.domain.usecase.dm.GetDmChannelUseCase
import com.example.domain.usecase.dm.GetUserDmChannelsUseCase
import com.example.domain.usecase.dm.GetUserDmWrappersUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DM(Direct Message) 관련 UseCase들을 제공하는 Provider
 * 
 * 사용자 간 직접 메시지 채널 관리를 담당합니다.
 */
@Singleton
class DMUseCaseProvider @Inject constructor(
    private val dmChannelRepository: DefaultRepository<DMChannel>,
    private val dmWrapperRepository: DefaultRepository<DMWrapper>,
    private val authRepository: AuthRepository,
    private val userRepository: DefaultRepository<User>
) {

    /**
     * 특정 사용자의 DM 관련 UseCase들을 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return DM 관련 UseCase 그룹
     */
    fun createForUser(userId: UserId): DMUseCases {
        dmChannelRepository.setCollection(CollectionPath.dmChannels)
        dmWrapperRepository.setCollection(CollectionPath.userDmWrappers(userId.value))
        userRepository.setCollection(CollectionPath.users)

        return DMUseCases(
            dmChannelRepository = dmChannelRepository,
            dmWrapperRepository = dmWrapperRepository,
            authRepository = authRepository,
            userRepository = userRepository,

            getUserDmChannelsUseCase = GetUserDmChannelsUseCase(
                dmChannelRepository = dmChannelRepository,
                authRepository = authRepository,
                dmWrapperRepository = dmWrapperRepository
            ),

            getCurrentUserDmChannelsUseCase = GetCurrentUserDmChannelsUseCase(
                dmRepository = dmChannelRepository
            ),

            addDmChannelUseCase = AddDmChannelUseCase(
                dmChannelRepository = dmChannelRepository,
                authRepository = authRepository
            ),

            blockDMChannelUseCase = BlockDMChannelUseCase(
                dmChannelRepository = dmChannelRepository,
                authRepository = authRepository
            ),

            unblockDMChannelUseCase = UnblockDMChannelUseCase(
                dmChannelRepository = dmChannelRepository,
                authRepository = authRepository
            ),

            getDmChannelUseCase = GetDmChannelUseCase(
                dmRepository = dmChannelRepository
            ),

            getUserDmWrappersUseCase = GetUserDmWrappersUseCase(
                authRepository = authRepository,
                dmWrapperRepository = dmWrapperRepository
            ),
        )
    }

    /**
     * 특정 DM 채널에 대한 UseCase들을 생성합니다.
     * 
     * @param dmChannelId DM 채널 ID
     * @return DM 채널별 UseCase 그룹
     */
    fun createForDMChannel(dmChannelId: String): DMChannelUseCases {
        dmChannelRepository.setCollection(CollectionPath.dmChannels)

        return DMChannelUseCases(
            getDmChannelUseCase = GetDmChannelUseCase(
                dmRepository = dmChannelRepository
            ),
            dmChannelRepository = dmChannelRepository,
            authRepository = authRepository
            
            // 향후 DM 메시지 관련 UseCase들 추가 예정
        )
    }
}

/**
 * 사용자별 DM 관련 UseCase 그룹
 */
data class DMUseCases(
    val getUserDmChannelsUseCase: GetUserDmChannelsUseCase,
    val getCurrentUserDmChannelsUseCase: GetCurrentUserDmChannelsUseCase,
    val addDmChannelUseCase: AddDmChannelUseCase,
    val blockDMChannelUseCase: BlockDMChannelUseCase,
    val unblockDMChannelUseCase: UnblockDMChannelUseCase,
    val getDmChannelUseCase: GetDmChannelUseCase,
    val getUserDmWrappersUseCase: GetUserDmWrappersUseCase,
    val dmChannelRepository: DefaultRepository<DMChannel>,
    val dmWrapperRepository: DefaultRepository<DMWrapper>,
    val authRepository: AuthRepository,
    val userRepository: DefaultRepository<User>
)

/**
 * DM 채널별 UseCase 그룹
 */
data class DMChannelUseCases(
    val getDmChannelUseCase: GetDmChannelUseCase,
    val dmChannelRepository: DefaultRepository<DMChannel>,
    val authRepository: AuthRepository
)