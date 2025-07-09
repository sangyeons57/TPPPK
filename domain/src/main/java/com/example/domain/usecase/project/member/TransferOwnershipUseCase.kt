package com.example.domain.usecase.project.member

import com.example.core_common.util.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.model.vo.CollectionPath
import javax.inject.Inject

/**
 * 프로젝트 소유권 전달 UseCase
 * 
 * 현재 OWNER가 다른 멤버에게 소유권을 전달하는 기능을 제공합니다.
 */
interface TransferOwnershipUseCase {
    /**
     * 프로젝트 소유권을 다른 멤버에게 전달합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param newOwnerId 새로운 소유자 ID
     * @return 소유권 전달 결과
     */
    suspend operator fun invoke(projectId: DocumentId, newOwnerId: String): CustomResult<Unit, Exception>
}

/**
 * 프로젝트 소유권 전달 UseCase 구현체
 */
class TransferOwnershipUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val memberRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MemberRepositoryFactoryContext, MemberRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) : TransferOwnershipUseCase {

    /**
     * 프로젝트 소유권을 다른 멤버에게 전달합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param newOwnerId 새로운 소유자 ID
     * @return 소유권 전달 결과
     */
    override suspend operator fun invoke(projectId: DocumentId, newOwnerId: String): CustomResult<Unit, Exception> {
        return resultTry {
            // 1. 현재 사용자 세션 확인
            val authRepository = authRepositoryFactory.create(AuthRepositoryFactoryContext())
            val currentSessionResult = authRepository.getCurrentSession()
            val currentUserId = when (currentSessionResult) {
                is CustomResult.Success -> currentSessionResult.data.userId
                else -> throw Exception("사용자 인증 정보를 확인할 수 없습니다.")
            }
            
            // 2. 멤버 레포지토리 생성
            val memberRepository = memberRepositoryFactory.create(
                MemberRepositoryFactoryContext(
                    collectionPath = CollectionPath.projectMembers(projectId.value)
                )
            )
            
            // 3. 현재 사용자 멤버 정보 조회
            val currentUserMemberResult = memberRepository.getById(currentUserId)
            val currentUserMember = when (currentUserMemberResult) {
                is CustomResult.Success -> currentUserMemberResult.data
                else -> throw Exception("프로젝트 멤버가 아닙니다.")
            }
            
            // 4. 현재 사용자가 OWNER인지 확인
            val isCurrentUserOwner = currentUserMember.roleIds.any { roleId -> 
                roleId.value == "OWNER" 
            }
            
            if (!isCurrentUserOwner) {
                throw Exception("소유권을 전달할 권한이 없습니다. OWNER만 소유권을 전달할 수 있습니다.")
            }
            
            // 5. 새로운 소유자 멤버 정보 조회
            val newOwnerMemberResult = memberRepository.getById(DocumentId.from(newOwnerId))
            val newOwnerMember = when (newOwnerMemberResult) {
                is CustomResult.Success -> newOwnerMemberResult.data
                else -> throw Exception("새로운 소유자가 프로젝트 멤버가 아닙니다.")
            }
            
            // 6. 자기 자신에게 전달하는 경우 방지
            if (currentUserId.value == newOwnerId) {
                throw Exception("자기 자신에게는 소유권을 전달할 수 없습니다.")
            }
            
            // 7. 현재 소유자(현재 사용자)에서 OWNER 역할 제거
            val updatedCurrentUserRoleIds = currentUserMember.roleIds.toMutableList()
            updatedCurrentUserRoleIds.removeAll { it.value == "OWNER" }
            
            val updatedCurrentUser = currentUserMember.copy(roleIds = updatedCurrentUserRoleIds)
            memberRepository.save(updatedCurrentUser)
            
            // 8. 새로운 소유자에게 OWNER 역할 추가
            val updatedNewOwnerRoleIds = newOwnerMember.roleIds.toMutableList()
            if (!updatedNewOwnerRoleIds.any { it.value == "OWNER" }) {
                updatedNewOwnerRoleIds.add(DocumentId.from("OWNER"))
            }
            
            val updatedNewOwner = newOwnerMember.copy(roleIds = updatedNewOwnerRoleIds)
            memberRepository.save(updatedNewOwner)
            
            // 9. 프로젝트 소유자 정보 업데이트 (ownerId 변경)
            projectRepository.transferOwnership(projectId, newOwnerId)
        }
    }
}