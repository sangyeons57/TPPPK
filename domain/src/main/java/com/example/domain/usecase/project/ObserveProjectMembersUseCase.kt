package com.example.domain.usecase.project

import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 프로젝트의 멤버 목록 변경 사항을 실시간으로 관찰하는 유스케이스 인터페이스
 */
interface ObserveProjectMembersUseCase {
    operator fun invoke(projectId: String): Flow<List<ProjectMember>>
}

/**
 * ObserveProjectMembersUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class ObserveProjectMembersUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : ObserveProjectMembersUseCase {

    /**
     * 유스케이스를 실행하여 특정 프로젝트의 멤버 목록 스트림을 반환합니다.
     * @param projectId 프로젝트 ID
     * @return Flow<List<ProjectMember>> 멤버 목록 스트림
     */
    override fun invoke(projectId: String): Flow<List<ProjectMember>> {
        return projectMemberRepository.getProjectMembersStream(projectId)
    }
} 