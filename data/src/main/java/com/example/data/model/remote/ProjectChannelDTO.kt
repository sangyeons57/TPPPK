package com.example.data.model.remote

import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.ProjectChannel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName

/*
 * 프로젝트 채널 정보를 나타내는 DTO 클래스
 */
data class ProjectChannelDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(CHANNEL_NAME)
    val channelName: String = "",
    @get:PropertyName(CHANNEL_TYPE)
    val channelType: ProjectChannelType = ProjectChannelType.MESSAGES, // "MESSAGES", "TASKS" 등
    @get:PropertyName(ORDER)
    val order: Double = 0.0, // Added order field
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) {

    companion object {
        const val COLLECTION_NAME = ProjectChannel.COLLECTION_NAME
        const val CHANNEL_NAME = ProjectChannel.KEY_CHANNEL_NAME
        const val CHANNEL_TYPE = ProjectChannel.KEY_CHANNEL_TYPE
        const val ORDER = ProjectChannel.KEY_ORDER
        const val CREATED_AT = ProjectChannel.KEY_CREATED_AT
        const val UPDATED_AT = ProjectChannel.KEY_UPDATED_AT
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return ProjectChannel 도메인 모델
     */
    fun toDomain(): ProjectChannel {
        return ProjectChannel.fromDataSource(
            id = VODocumentId(id),
            channelName = Name(channelName),
            channelType = channelType,
            order = ProjectChannelOrder(order), // Added order mapping
            createdAt = createdAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * ProjectChannel 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ProjectChannelDTO 객체
 */
fun ProjectChannel.toDto(): ProjectChannelDTO {
    return ProjectChannelDTO(
        id = id.value,
        channelName = channelName.value,
        channelType = channelType,
        order = order.value, // Added order mapping
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}
