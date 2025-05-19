package com.example.domain.usecase.project

import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import com.example.domain.model.channel.ProjectSpecificData
import com.example.domain.repository.ProjectStructureRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 특정 프로젝트의 구조(카테고리 및 채널 목록)를 가져오는 유스케이스 인터페이스
 */
interface GetProjectStructureUseCase {
    // 프로젝트 구조를 반환
    suspend operator fun invoke(projectId: String): Result<ProjectStructure>
}

/**
 * GetProjectStructureUseCase의 구현체
 */
class GetProjectStructureUseCaseImpl @Inject constructor(
    private val projectStructureRepository: ProjectStructureRepository
) : GetProjectStructureUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 구조를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return Result<ProjectStructure> 프로젝트 구조 로드 결과
     */
    override suspend fun invoke(projectId: String): Result<ProjectStructure> {
        return projectStructureRepository.getProjectStructure(projectId)
    }
} 