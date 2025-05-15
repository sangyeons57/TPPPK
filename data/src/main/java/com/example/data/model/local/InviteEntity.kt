package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Invite
import com.example.domain.model.InviteType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 초대 토큰 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(tableName = "invites")
data class InviteEntity(
    /**
     * 초대 토큰 ID (Primary Key)
     */
    @PrimaryKey
    val token: String,
    
    /**
     * 초대 유형 (예: "project_invite")
     */
    val type: InviteType,
    
    /**
     * 초대를 생성한 사용자 ID
     */
    val inviterId: String,
    
    /**
     * 초대를 생성한 사용자 이름
     */
    val inviterName: String,
    
    /**
     * 대상 프로젝트 ID (프로젝트 초대의 경우)
     */
    val projectId: String?,
    
    /**
     * 대상 프로젝트 이름
     */
    val projectName: String,
    
    /**
     * 초대 만료 시간 (밀리초 단위)
     */
    val expiresAt: Long,
    
    /**
     * 초대 생성 시간 (밀리초 단위)
     */
    val createdAt: Long,
    
    /**
     * 로컬 캐시 저장 시간 (밀리초 단위)
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Entity를 도메인 모델로 변환
     */
    fun toDomain(): Invite {
        return Invite(
            token = token,
            type = type,
            inviterId = inviterId,
            inviterName = inviterName,
            projectId = projectId,
            projectName = projectName,
            expiresAt = Instant.ofEpochMilli(expiresAt),
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }
    
    companion object {
        /**
         * 도메인 모델을 Entity로 변환
         */
        fun fromDomain(invite: Invite): InviteEntity {
            return InviteEntity(
                token = invite.token,
                type = invite.type,
                inviterId = invite.inviterId,
                inviterName = invite.inviterName,
                projectId = invite.projectId,
                projectName = invite.projectName,
                expiresAt = invite.expiresAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                createdAt = invite.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        }
    }
} 