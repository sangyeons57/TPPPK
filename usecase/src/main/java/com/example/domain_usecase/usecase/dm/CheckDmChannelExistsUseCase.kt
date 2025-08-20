package com.example.domain_usecase.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.DMWrapperRepository
import javax.inject.Inject

/**
 * DM 채널 존재 여부를 확인하는 UseCase
 *
 * 기능:
 * - 현재 사용자와 대상 사용자 간의 DM 채널이 존재하는지 확인
 * - 존재할 경우 채널 ID 반환, 없을 경우 null 반환
 */
interface CheckDmChannelExistsUseCase {
    /**
     * DM 채널 존재 여부 확인
     *
     * @param targetUserId 대상 사용자 ID
     * @return 채널 ID (존재할 경우) 또는 null (존재하지 않을 경우)
     */
    suspend operator fun invoke(targetUserId: String): CustomResult<String?, Exception>
}

/**
 * DM 채널 존재 확인 UseCase 구현체
 */
class CheckDmChannelExistsUseCaseImpl @Inject constructor(
    private val dmWrapperRepository: DMWrapperRepository
) : CheckDmChannelExistsUseCase {

    override suspend operator fun invoke(targetUserId: String): CustomResult<String?, Exception> {
        return try {
            // DMWrapper를 통해 대상 사용자와의 DM 채널 찾기
            val result = dmWrapperRepository.findByOtherUserId(UserId(targetUserId))

            when (result) {
                is CustomResult.Success -> {
                    // DM 채널 존재 - 채널 ID 반환
                    CustomResult.Success(result.data.id.value)
                }

                is CustomResult.Failure -> {
                    // DM 채널 없음 - null 반환 (오류가 아님)
                    CustomResult.Success(null)
                }

                is CustomResult.Initial -> {
                    CustomResult.Failure(Exception("DM 채널 확인이 초기화되지 않았습니다."))
                }

                is CustomResult.Loading -> {
                    CustomResult.Failure(Exception("DM 채널 확인이 아직 로딩 중입니다."))
                }

                is CustomResult.Progress -> {
                    CustomResult.Failure(Exception("DM 채널 확인이 진행 중입니다."))
                }
            }

        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}