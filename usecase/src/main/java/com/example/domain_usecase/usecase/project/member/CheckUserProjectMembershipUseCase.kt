package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * 현재 사용자가 특정 프로젝트의 멤버인지 확인하는 UseCase
 */
interface CheckUserProjectMembershipUseCase {
    /**
     * 사용자가 특정 프로젝트의 멤버인지 확인합니다.
     *
     * @param projectId 확인할 프로젝트 ID
     * @param userId 확인할 사용자 ID
     * @return 멤버십 여부 (true: 멤버임, false: 멤버가 아님)
     */
    suspend operator fun invoke(projectId: String, userId: String): Boolean
}

/**
 * CheckUserProjectMembershipUseCase 구현체
 */
class CheckUserProjectMembershipUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository
) : CheckUserProjectMembershipUseCase {

    override suspend fun invoke(projectId: String, userId: String): Boolean {
        return try {
            // MemberRepository를 해당 프로젝트 컨텍스트로 설정
            // provider에서 이미 설정
            //memberRepository.setCollection(CollectionPath.projectMembers(projectId))

            // 해당 사용자 ID로 멤버 문서 조회 시도
            when (val result = memberRepository.findById(DocumentId(userId))) {
                is CustomResult.Success -> {
                    // 멤버 문서가 존재하면 멤버임
                    true
                }

                is CustomResult.Failure -> {
                    // 문서가 존재하지 않거나 오류 발생 시 멤버가 아님
                    false
                }

                else -> false
            }
        } catch (e: Exception) {
            // 예외 발생 시 안전하게 false 반환
            false
        }
    }
}