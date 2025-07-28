package com.example.domain.usecase.local.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.AuthLocalRepository
import com.example.domain.repository.local.DMChannelLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AddDmChannelLocalUseCase {
    operator fun invoke(partnerName: UserName): Flow<CustomResult<DocumentId, Exception>>
}

class AddDmChannelLocalUseCaseImpl @Inject constructor(
    private val dmChannelLocalRepository: DMChannelLocalRepository,
    private val authRepository: AuthLocalRepository
) : AddDmChannelLocalUseCase {

    override operator fun invoke(partnerName: UserName): Flow<CustomResult<DocumentId, Exception>> {
        return TODO("로컬 저장소에서 새 DM 채널을 생성하거나 기존 DM 채널을 조회")
    }
} 