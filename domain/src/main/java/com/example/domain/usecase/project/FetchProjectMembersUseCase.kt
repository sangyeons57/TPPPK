package com.example.domain.usecase.project

import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject

/**
 * 원격 서버로부터 최신 프로젝트 멤버 목록을 가져와 로컬 캐시/스트림을 업데이트하는 유스케이스 인터페이스
 */
interface FetchProjectMembersUseCase {
    suspend operator fun invoke(projectId: String): Result<Unit>
}

/**
 * FetchProjectMembersUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class FetchProjectMembersUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : FetchProjectMembersUseCase {

    /**
     * 유스케이스를 실행하여 원격 프로젝트 멤버 목록을 fetch합니다.
     * @param projectId 프로젝트 ID
     * @return Result<Unit> Fetch 작업 결과
     */
    override suspend fun invoke(projectId: String): Result<Unit> {
        return projectMemberRepository.fetchProjectMembers(projectId)
    }
} 