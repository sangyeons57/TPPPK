package com.example.domain_usecase.provider.dm

import com.example.domain.model.vo.UserId
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.RepositoryFactory
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.DMChannelRepository
import com.example.domain_repository.base.DMWrapperRepository
import com.example.domain_repository.base.UserRepository
import com.example.domain_repository.context.AuthRepositoryFactoryContext
import com.example.domain_repository.context.DMChannelRepositoryFactoryContext
import com.example.domain_repository.context.DMWrapperRepositoryFactoryContext
import com.example.domain_repository.context.UserRepositoryFactoryContext
import com.example.domain_usecase.usecase.dm.AddDmChannelUseCase
import com.example.domain_usecase.usecase.dm.BlockDMChannelUseCase
import com.example.domain_usecase.usecase.dm.GetCurrentUserDmChannelsUseCase
import com.example.domain_usecase.usecase.dm.GetDmChannelUseCase
import com.example.domain_usecase.usecase.dm.GetUserDmChannelsUseCase
import com.example.domain_usecase.usecase.dm.GetUserDmWrappersUseCase
import com.example.domain_usecase.usecase.dm.UnblockDMChannelUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DM(Direct Message) 관련 UseCase들을 제공하는 Provider
 * 
 * 사용자 간 직접 메시지 채널 관리를 담당합니다.
 */
@Singleton
class DMUseCaseProvider @Inject constructor(
    private val dmChannelRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<DMChannelRepositoryFactoryContext, DMChannelRepository>,
    private val dmWrapperRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<DMWrapperRepositoryFactoryContext, DMWrapperRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val userRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
) {

    /**
     * 특정 사용자의 DM 관련 UseCase들을 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return DM 관련 UseCase 그룹
     */
    fun createForUser(userId: UserId): DMUseCases {
        val dmChannelRepository = dmChannelRepositoryFactory.create(
            DMChannelRepositoryFactoryContext(
                collectionPath = CollectionPath.dmChannels
            )
        )

        val dmWrapperRepository = dmWrapperRepositoryFactory.create(
            DMWrapperRepositoryFactoryContext(
                collectionPath = CollectionPath.userDmWrappers(userId.value)
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        userRepositoryFactory.create(
            UserRepositoryFactoryContext(CollectionPath.users)
        )

        return DMUseCases(
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
            )
        )
    }

    /**
     * 특정 DM 채널에 대한 UseCase들을 생성합니다.
     * 
     * @param dmChannelId DM 채널 ID
     * @return DM 채널별 UseCase 그룹
     */
    fun createForDMChannel(dmChannelId: String): DMChannelUseCases {
        val dmChannelRepository = dmChannelRepositoryFactory.create(
            DMChannelRepositoryFactoryContext(
                collectionPath = CollectionPath.dmChannels
            )
        )

        authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return DMChannelUseCases(
            getDmChannelUseCase = GetDmChannelUseCase(
                dmRepository = dmChannelRepository
            )
            
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
    val getUserDmWrappersUseCase: GetUserDmWrappersUseCase
)

/**
 * DM 채널별 UseCase 그룹
 */
data class DMChannelUseCases(
    val getDmChannelUseCase: GetDmChannelUseCase
)