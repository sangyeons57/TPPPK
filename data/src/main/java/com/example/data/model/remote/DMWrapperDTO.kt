package com.example.data.model.remote

import com.example.domain.model.base.DMWrapper
import com.google.firebase.firestore.DocumentId

/**
 * DM 채널 정보와 상대방 ID를 나타내는 DTO 클래스
 */
data class DMWrapperDTO(
    @DocumentId val dmChannelId: String = "",
    val otherUserId: String = ""
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return DMWrapper 도메인 모델
     */
    fun toDomain(): DMWrapper {
        return DMWrapper(
            dmChannelId = dmChannelId,
            otherUserId = otherUserId
        )
    }
}

/**
 * DMWrapper 도메인 모델을 DTO로 변환하는 확장 함수
 * @return DMWrapperDTO 객체
 */
fun DMWrapper.toDto(): DMWrapperDTO {
    return DMWrapperDTO(
        dmChannelId = dmChannelId,
        otherUserId = otherUserId
    )
}
