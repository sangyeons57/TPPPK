package com.example.data._repository

import com.example.core_common.result.resultTry
import com.example.data.datasource._remote.InviteRemoteDataSource
import com.example.data.model._remote.InviteDTO
import com.example.data.model.mapper.toDomain
import com.example.data.model.mapper.toDto // Invite -> InviteDTO (필요시)
import com.example.domain.model.Invite
import com.example.domain.model.InviteType
import com.example.domain._repository.InviteRepository
import com.google.firebase.Timestamp // Timestamp 사용 시
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID // 초대 코드 생성용
import javax.inject.Inject
import kotlin.Result

class InviteRepositoryImpl @Inject constructor(
    private val inviteRemoteDataSource: InviteRemoteDataSource
    // private val inviteMapper: InviteMapper // 개별 매퍼 사용시
) : InviteRepository {

    override suspend fun createInvite(
        type: InviteType,
        targetId: String,
        creatorId: String,
        expiresInMillis: Long?,
        maxUses: Int?
    ): Result<Invite> = resultTry {
        val uniqueCode = UUID.randomUUID().toString().take(8).uppercase() // 예시: 8자리 랜덤 코드
        val currentTime = Timestamp.now().seconds * 1000 // 현재 시간을 millis로
        val expiryTime = expiresInMillis?.let { currentTime + it }

        val inviteDto = InviteDTO(
            // id는 DataSource에서 자동 생성되거나 code 자체가 ID가 될 수 있음
            code = uniqueCode,
            type = type.name,
            targetId = targetId,
            creatorId = creatorId,
            createdAt = Timestamp.now(),
            expiresAt = expiryTime?.let { Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt()) },
            maxUses = maxUses,
            currentUses = 0,
            isActive = true
            // targetName 등 비정규화 데이터는 DataSource에서 채울 수 있음
        )
        // DataSource의 createInvite 함수는 생성된 InviteDTO (ID 포함)를 반환하거나, 생성된 Invite의 ID를 반환할 수 있음.
        // 여기서는 DataSource가 생성된 DTO를 반환하고, 그것을 도메인 모델로 변환한다고 가정
        inviteRemoteDataSource.createInvite(inviteDto).getOrThrow().toDomain()
    }

    override suspend fun getInviteByCode(inviteCode: String): Result<Invite> = resultTry {
        val inviteDto = inviteRemoteDataSource.getInviteByCode(inviteCode).getOrThrow()
        if (!inviteDto.isActive) {
            throw IllegalStateException(\
Invite
code
is
no
longer
active.\)
        }
        inviteDto.expiresAt?.let {
            if (Timestamp.now().seconds > it.seconds) {
                // TODO: 만료된 경우 DataSource에서 isActive를 false로 업데이트하는 로직이 있으면 좋음
                throw IllegalStateException(\Invite
code
has
expired.\)
            }
        }
        inviteDto.maxUses?.let {
            if (inviteDto.currentUses >= it) {
                // TODO: 최대 사용 횟수 도달 시 DataSource에서 isActive를 false로 업데이트하는 로직이 있으면 좋음
                throw IllegalStateException(\Invite
code
has
reached
its
maximum
number
of
uses.\)
            }
        }
        inviteDto.toDomain()
    }

    override fun getActiveProjectInvitesStream(projectId: String): Flow<Result<List<Invite>>> {
        // InviteRemoteDataSource에 getActiveInvitesForProjectStream(projectId)와 같은 함수 필요
        return inviteRemoteDataSource.getActiveInvitesForProjectStream(projectId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun consumeInvite(inviteCode: String, userId: String): Result<Invite> = resultTry {
        // 1. 코드로 초대 정보 가져오기 (유효성 검사 포함)
        val invite = getInviteByCode(inviteCode).getOrThrow() // 내부적으로 isActive, 만료, 사용횟수 검사

        // 2. 초대 사용 처리 (DataSource에 consumeInvite(inviteId) 또는 incrementInviteUses(inviteId) 함수 필요)
        //    이 함수는 currentUses를 증가시키고, 필요시 maxUses에 도달하면 isActive를 false로 변경해야 함.
        //    userId는 누가 사용했는지 로그를 남기거나, 중복 사용 방지에 활용될 수 있음.
        inviteRemoteDataSource.incrementInviteUses(invite.id, userId).getOrThrow()
        
        // 업데이트된 초대 정보 반환 (또는 최소한 targetId 등 필요한 정보 포함)
        // getInviteByCode를 다시 호출하여 최신 상태를 가져올 수도 있지만, 여기서는 consume 후의 invite 객체를 그대로 반환
        invite // 이 시점의 invite는 currentUses가 증가되기 전일 수 있으므로 주의.
               // DataSource의 incrementInviteUses가 업데이트된 InviteDTO를 반환하면 더 좋음.
               // 여기서는 consume 행위 자체의 성공을 알리고, 반환된 Invite는 참조용.
    }

    override suspend fun revokeInvite(inviteId: String, currentUserId: String): Result<Unit> = resultTry {
        // InviteRemoteDataSource에 revokeInvite(inviteId, currentUserId) 함수 필요
        // currentUserId는 권한 검사용 (생성자 또는 특정 권한자만 비활성화 가능)
        inviteRemoteDataSource.revokeInvite(inviteId, currentUserId).getOrThrow()
    }
}
