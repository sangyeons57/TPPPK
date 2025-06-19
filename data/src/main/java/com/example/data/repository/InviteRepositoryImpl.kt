package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.InviteRemoteDataSource
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.base.Invite
import com.example.domain.repository.InviteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val inviteRemoteDataSource: InviteRemoteDataSource
    // private val inviteMapper: InviteMapper // 개별 매퍼 사용시
) : InviteRepository {


    override suspend fun createInvite(
        type: InviteStatus,
        targetId: String,
        creatorId: String,
        expiresInMillis: Long?,
        maxUses: Int?
    ): CustomResult<Invite, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getInviteByCode(inviteCode: String): CustomResult<Invite, Exception> {
        TODO("Not yet implemented")
    }

    override fun getActiveProjectInvitesStream(projectId: String): Flow<CustomResult<List<Invite>, Exception>> {
        TODO("Not yet implemented")
    }

    override suspend fun consumeInvite(
        inviteCode: String,
        userId: String
    ): CustomResult<Invite, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun revokeInvite(
        inviteId: String,
        currentUserId: String
    ): CustomResult<Unit, Exception> {
        TODO("Not yet implemented")
    }
}
