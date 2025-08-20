package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.AuthRepository
import javax.inject.Inject

/**
 * 프로젝트 멤버십 검증 UseCase
 *
 * 현재 사용자가 실제로 프로젝트의 멤버인지 확인합니다.
 */
interface VerifyProjectMembershipUseCase {
    /**
     * 현재 사용자가 프로젝트 멤버인지 확인합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 멤버인 경우 true, 아닌 경우 false
     */
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception>
}

/**
 * 프로젝트 멤버십 검증 UseCase 구현체
 */
class VerifyProjectMembershipUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) : VerifyProjectMembershipUseCase {

    override suspend fun invoke(projectId: DocumentId): CustomResult<Boolean, Exception> {
        return try {
            // 1. 현재 사용자 ID 가져오기
            val sessionResult = authRepository.getCurrentUserSession()
            if (sessionResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("사용자 세션을 확인할 수 없습니다"))
            }

            val currentUserId = sessionResult.data.userId

            // 2. 멤버 컬렉션에서 해당 사용자 조회 (userId를 DocumentId로 사용)
            val memberResult = memberRepository.findById(DocumentId(currentUserId.value))
            when (memberResult) {
                is CustomResult.Success -> {
                    val member = memberResult.data
                    // 3. 멤버 상태 확인 (ACTIVE 상태만 유효한 멤버로 인정)
                    val isActiveMember = member.isActive()
                    CustomResult.Success(isActiveMember)
                }

                is CustomResult.Failure -> {
                    // 멤버를 찾을 수 없으면 멤버가 아님
                    CustomResult.Success(false)
                }

                is CustomResult.Initial -> CustomResult.Failure(Exception("멤버 조회가 초기화되지 않았습니다"))
                is CustomResult.Loading -> CustomResult.Failure(Exception("멤버 조회 중입니다"))
                is CustomResult.Progress -> CustomResult.Failure(Exception("멤버 조회 진행 중입니다"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}