package com.example.domain.usecase.local.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectInvitationLocalRepository
import javax.inject.Inject

interface SendProjectInvitationLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        recipientEmail: String,
        senderMessage: String? = null
    ): CustomResult<ProjectInvitation, Exception>
}

class SendProjectInvitationLocalUseCaseImpl @Inject constructor(
    private val projectInvitationLocalRepository: ProjectInvitationLocalRepository
) : SendProjectInvitationLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        recipientEmail: String,
        senderMessage: String?
    ): CustomResult<ProjectInvitation, Exception> {
        return TODO("로컬 저장소에 프로젝트 초대장을 생성하여 저장")
    }
} 