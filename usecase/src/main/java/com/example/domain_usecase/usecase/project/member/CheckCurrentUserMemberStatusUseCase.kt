package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.MemberStatus
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * 현재 사용자의 프로젝트 멤버 상태를 확인하는 UseCase
 */
interface CheckCurrentUserMemberStatusUseCase {
    /**
     * 현재 사용자의 프로젝트 멤버 상태를 확인합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 멤버 상태, 멤버가 아니면 null
     */
    suspend operator fun invoke(projectId: DocumentId): CustomResult<MemberStatus?, Exception>
}

/**
 * 현재 사용자의 프로젝트 멤버 상태 확인 UseCase 구현체
 */
class CheckCurrentUserMemberStatusUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) : CheckCurrentUserMemberStatusUseCase {

    override suspend fun invoke(projectId: DocumentId): CustomResult<MemberStatus?, Exception> {
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
                    // 멤버 상태 반환
                    CustomResult.Success(member.status)
                }

                is CustomResult.Failure -> {
                    // 멤버를 찾을 수 없으면 null 반환
                    CustomResult.Success(null)
                }

                else -> CustomResult.Failure(Exception("멤버 조회 중 오류가 발생했습니다"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}