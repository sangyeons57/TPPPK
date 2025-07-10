package com.example.domain.usecase.project.core

import com.example.domain.model.vo.DocumentId
import javax.inject.Inject

/**
 * ProjectInvitation ID를 기반으로 초대 링크를 생성하는 UseCase
 * Firebase 접근 없이 순수 함수로 작동합니다.
 */
class GenerateInviteLinkFromIdUseCase @Inject constructor() {
    
    /**
     * ProjectInvitation ID로부터 초대 링크를 생성합니다.
     *
     * @param invitationId ProjectInvitation의 DocumentId (초대 코드)
     * @return 완전한 초대 링크 URL
     */
    operator fun invoke(invitationId: DocumentId): String {
        return "https://projecting.com/invite/${invitationId.value}"
    }
} 