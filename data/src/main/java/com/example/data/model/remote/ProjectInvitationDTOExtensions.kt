package com.example.data.model.remote

import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.util.Date

/**
 * ProjectInvitation 도메인 모델을 DTO로 변환하는 확장 함수
 */
fun ProjectInvitation.toDto(): ProjectInvitationDTO {
    return ProjectInvitationDTO(
        id = this.id.value,
        inviterId = this.inviterId.value,
        projectId = this.projectId.value,
        status = this.status.value,
        expiresAt = this.expiresAt?.let { Date.from(it) },
        createdAt = Date.from(this.createdAt),
        updatedAt = Date.from(this.updatedAt)
    )
}