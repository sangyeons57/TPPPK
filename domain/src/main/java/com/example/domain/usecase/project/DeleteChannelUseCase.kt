// Required new interface:
// interface ProjectChannelRepository {
//     // ... other methods
//     fun deleteProjectChannel(channelId: String): Result<Unit>
// }
package com.example.domain.usecase.project

import com.example.domain.repository.ProjectChannelRepository // Changed from ProjectSettingRepository
import javax.inject.Inject

/**
 * 프로젝트 채널을 삭제하는 유스케이스 인터페이스
 */
interface DeleteChannelUseCase {
    suspend operator fun invoke(channelId: String): Result<Unit>
}

/**
 * DeleteChannelUseCase의 구현체. This Usecase is for deleting a *Project Channel*.
 * @param projectChannelRepository 프로젝트 채널 관련 데이터 접근을 위한 Repository.
 */
class DeleteChannelUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : DeleteChannelUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 채널을 삭제합니다.
     * @param channelId 삭제할 채널의 ID
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(channelId: String): Result<Unit> {
        return projectChannelRepository.deleteProjectChannel(channelId)
    }
}