package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.DMWrapper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * DM 채널 정보와 상대방 정보를 함께 나타내는 DTO 클래스
 */
data class DMWrapperDTO(
    @DocumentId val dmChannelId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserProfileImageUrl: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Timestamp? = null
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return DMWrapper 도메인 모델
     */
    fun toDomain(): DMWrapper {
        return DMWrapper(
            dmChannelId = dmChannelId,
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            otherUserProfileImageUrl = otherUserProfileImageUrl,
            lastMessagePreview = lastMessagePreview,
            lastMessageTimestamp = lastMessageTimestamp?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/*
 * DMWrapper 도메인 모델을 DTO로 변환하는 확장 함수
 * @return DMWrapperDTO 객체
 */
fun DMWrapper.toDto(): DMWrapperDTO {
    return DMWrapperDTO(
        dmChannelId = dmChannelId,
        otherUserId = otherUserId,
        otherUserName = otherUserName,
        otherUserProfileImageUrl = otherUserProfileImageUrl,
        lastMessagePreview = lastMessagePreview,
        lastMessageTimestamp = lastMessageTimestamp?.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
