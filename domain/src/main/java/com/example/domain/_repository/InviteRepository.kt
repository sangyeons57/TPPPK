package com.example.domain._repository

import com.example.domain.model.Invite
import com.example.domain.model.InviteType
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 초대 코드 생성, 조회, 사용 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface InviteRepository {
    /**
     * 새로운 초대 코드를 생성합니다.
     * @param type 초대 유형 (PROJECT, CHANNEL 등)
     * @param targetId 초대 대상 ID (예: 프로젝트 ID)
     * @param creatorId 생성자 ID
     * @param expiresInMillis 만료 기간 (밀리초 단위, null이면 무기한)
     * @param maxUses 최대 사용 횟수 (null이면 무제한)
     * @return 생성된 초대 정보를 담은 Result.
     */
    suspend fun createInvite(
        type: InviteType,
        targetId: String,
        creatorId: String,
        expiresInMillis: Long?,
        maxUses: Int?
    ): Result<Invite>

    /**
     * 초대 코드를 사용하여 초대 정보를 조회합니다.
     * @param inviteCode 조회할 초대 코드 문자열
     * @return 해당 초대 코드의 상세 정보를 담은 Result. 코드가 유효하지 않으면 실패.
     */
    suspend fun getInviteByCode(inviteCode: String): Result<Invite>

    /**
     * 특정 프로젝트에 대해 생성된 활성 초대 코드 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 활성 초대 코드 목록을 담은 Result Flow.
     */
    fun getActiveProjectInvitesStream(projectId: String): Flow<Result<List<Invite>>>

    /**
     * 초대 코드를 사용 처리합니다.
     * 초대 코드의 유효성을 검사하고, 사용 횟수를 증가시키거나 만료 처리합니다.
     * @param inviteCode 사용할 초대 코드
     * @param userId 사용하는 사용자 ID
     * @return 사용된 초대 정보(targetId 등 포함)를 담은 Result.
     */
    suspend fun consumeInvite(inviteCode: String, userId: String): Result<Invite>

    /**
     * 특정 초대 코드를 비활성화(또는 삭제)합니다.
     * @param inviteId 비활성화할 초대 코드의 ID
     * @param currentUserId 작업을 요청한 사용자 ID (권한 확인용)
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun revokeInvite(inviteId: String, currentUserId: String): Result<Unit>
}
