package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.DMChannelRepository
import javax.inject.Inject

/**
 * 새로운 DM 채널을 생성하거나 기존 DM 채널을 가져오는 UseCase입니다.
 * 내부적으로 현재 로그인된 사용자와 상대방 사용자 ID를 사용하여 DM 채널을 식별하거나 생성합니다.
 */
class AddDmChannelUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val authRepository: AuthRepository // Current user ID might be needed explicitly or handled by repo
) {
    /**
     * 지정된 상대방 사용자와의 DM 채널을 생성하거나 가져옵니다.
     *
     * @param partnerId DM 채널을 생성할 상대방 사용자의 ID
     * @return 성공 시 DM 채널 ID를 포함하는 CustomResult, 실패 시 에러를 포함하는 CustomResult
     */
    suspend operator fun invoke(partnerId: String): CustomResult<String, Exception> {
        // Validate partnerId if necessary (e.g., not blank, not same as current user)
        if (partnerId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Partner ID cannot be blank."))
        }

        // It's good practice to ensure the partnerId is not the current user's ID.
        // val currentUserResult = authRepository.getCurrentUserSession()
        // when (currentUserResult) {
        //     is CustomResult.Success -> {
        //         if (currentUserResult.data.userId == partnerId) {
        //             return CustomResult.Failure(IllegalArgumentException("Cannot create DM channel with oneself."))
        //         }
        //     }
        //     is CustomResult.Failure -> {
        //         return CustomResult.Failure(Exception("User not authenticated."))
        //     }
        //     else -> { /* Handle other states if necessary */ }
        // }
        
        // The DMChannelRepository.createDmChannel(otherUserId: String) seems to handle
        // the current user context internally, as it only takes the otherUserId.
        return dmChannelRepository.createDmChannel(partnerId)
    }
}
