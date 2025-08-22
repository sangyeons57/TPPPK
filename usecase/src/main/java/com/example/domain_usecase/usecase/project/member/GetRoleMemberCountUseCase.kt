package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * 특정 역할에 할당된 멤버 수를 계산하는 UseCase
 * Provider에서 repository의 collection이 프로젝트 컨텍스트로 설정되어 있어야 합니다.
 */
interface GetRoleMemberCountUseCase {
    suspend operator fun invoke(roleId: DocumentId): CustomResult<Int, Exception>
}

class GetRoleMemberCountUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : GetRoleMemberCountUseCase {

    override suspend operator fun invoke(roleId: DocumentId): CustomResult<Int, Exception> {
        return try {
            when (val result = projectMemberRepository.findAll()) {
                is CustomResult.Success -> {
                    val members = result.data
                    val memberCount = when (roleId.value) {
                        "everyone" -> {
                            // everyone 역할은 전체 멤버 수
                            members.size
                        }

                        else -> {
                            // 특정 역할에 할당된 멤버 수 계산
                            members.count { member ->
                                member.roleIds.any { it.value == roleId.value }
                            }
                        }
                    }
                    CustomResult.Success(memberCount)
                }

                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }

                else -> {
                    CustomResult.Failure(Exception("Unexpected result type"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}