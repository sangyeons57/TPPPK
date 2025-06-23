package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.event.AggregateRoot
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.RoleRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 프로젝트 멤버의 역할을 업데이트하는 유스케이스 인터페이스
 */
interface UpdateMemberRolesUseCase {
    suspend operator fun invoke(userId: DocumentId, roleIds: List<String>): CustomResult<Unit, Exception>
}

/**
 * UpdateMemberRolesUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class UpdateMemberRolesUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository
) : UpdateMemberRolesUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 멤버의 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleIds 업데이트할 역할 ID 목록
     * @return Result<Unit> 업데이트 처리 결과
     */
    override suspend fun invoke(userId: DocumentId, rolesIds: List<String>): CustomResult<Unit, Exception> {
        when (val memberResult : CustomResult<AggregateRoot, Exception> = memberRepository.observe(userId).first()) {
            is CustomResult.Success -> {
                val member : Member = memberResult.data as Member
                member.updateRoles(rolesIds.map { DocumentId(it) })
                return when (val saveResult = memberRepository.save(member)) {
                    is CustomResult.Success -> {
                        EventDispatcher.publish(member)
                        CustomResult.Success(Unit)
                    }
                    is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(saveResult.progress)
                }
            }
            else -> {
                return CustomResult.Failure(Exception("멤버 정보를 가져오지 못했습니다."))
            }
        }
    }
} 