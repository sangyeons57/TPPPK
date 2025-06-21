package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.repository.base.MemberRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 프로젝트 멤버의 역할을 업데이트하는 유스케이스 인터페이스
 */
interface UpdateMemberRolesUseCase {
    suspend operator fun invoke(projectId: String, userId: String, roleIds: List<String>): CustomResult<Unit, Exception>
}

/**
 * UpdateMemberRolesUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class UpdateMemberRolesUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : UpdateMemberRolesUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 멤버의 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleIds 업데이트할 역할 ID 목록
     * @return Result<Unit> 업데이트 처리 결과
     */
    override suspend fun invoke(projectId: String, userId: String, rolesId: List<String>): CustomResult<Unit, Exception> {
        val memberResult = projectMemberRepository.getProjectMemberStream(projectId, userId).first()
        when (memberResult) {
            is CustomResult.Success -> {
                val member : Member = memberResult.data
                val updatedMember = member.copy(roleIds = rolesId)
                return projectMemberRepository.updateProjectMember(projectId, updatedMember)
            }
            else -> {
                return CustomResult.Failure(Exception("멤버 정보를 가져오지 못했습니다."))
            }
        }
    }
} 