package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.repository.base.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 특정 프로젝트의 멤버 목록 변경 사항을 실시간으로 관찰하는 유스케이스 인터페이스
 */
interface ObserveProjectMembersUseCase {
    operator fun invoke(): Flow<CustomResult<List<Member>, Exception>>
}

/**
 * ObserveProjectMembersUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class ObserveProjectMembersUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : ObserveProjectMembersUseCase {

    /**
     * 유스케이스를 실행하여 특정 프로젝트의 멤버 목록 스트림을 반환합니다.
     * @param projectId 프로젝트 ID
     * @return Flow<List<ProjectMember>> 멤버 목록 스트림
     */
    override fun invoke(): Flow<CustomResult<List<Member>, Exception>> {
        return projectMemberRepository.observeAll().map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map { it as Member })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
} 