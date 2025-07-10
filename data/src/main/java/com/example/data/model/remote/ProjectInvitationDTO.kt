package com.example.data.model.remote

import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import java.util.Date

/**
 * 프로젝트 초대 정보를 나타내는 DTO 클래스
 * Firestore document 구조와 매핑
 */
data class ProjectInvitationDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(ProjectInvitation.KEY_INVITER_ID)
    val inviterId: String = "",
    @get:PropertyName(ProjectInvitation.KEY_PROJECT_ID)
    val projectId: String = "",
    @get:PropertyName(ProjectInvitation.KEY_STATUS)
    val status: String = InviteStatus.ACTIVE.value,
    @get:PropertyName(ProjectInvitation.KEY_EXPIRES_AT)
    val expiresAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @ServerTimestamp
    val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @ServerTimestamp
    val updatedAt: Date? = null
) : DTO<ProjectInvitation> {

    /**
     * DTO를 도메인 모델로 변환
     */
    override fun toDomain(): ProjectInvitation {
        return ProjectInvitation.fromDataSource(
            id = DocumentId(id),
            status = InviteStatus.fromString(status),
            inviterId = UserId(inviterId),
            projectId = DocumentId(projectId),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant(),
            expiresAt = expiresAt?.toInstant()
        )
    }

    companion object {
        /**
         * 도메인 모델을 DTO로 변환
         */
        fun fromDomain(projectInvitation: ProjectInvitation): ProjectInvitationDTO {
            return ProjectInvitationDTO(
                id = projectInvitation.id.value,
                inviterId = projectInvitation.inviterId.value,
                projectId = projectInvitation.projectId.value,
                status = projectInvitation.status.value,
                expiresAt = projectInvitation.expiresAt?.let { Date.from(it) },
                createdAt = Date.from(projectInvitation.createdAt),
                updatedAt = Date.from(projectInvitation.updatedAt)
            )
        }

        /**
         * 새로운 초대 생성을 위한 팩토리 메서드
         */
        fun createNew(
            inviteCodeId: String,
            inviterId: String,
            projectId: String,
            expiresAt: Date? = null
        ): ProjectInvitationDTO {
            return ProjectInvitationDTO(
                id = inviteCodeId,
                inviterId = inviterId,
                projectId = projectId,
                status = InviteStatus.ACTIVE.value,
                expiresAt = expiresAt,
                createdAt = null, // Will be set by @ServerTimestamp
                updatedAt = null  // Will be set by @ServerTimestamp
            )
        }
    }
}