package com.example.data.model.collection

data class CategoryWithChannelsCollectionDTO(
    val category: CategoryDocumentDTO,
    // ProjectChannel 자체에 메시지가 포함될 수도 있지만, 여기서는 채널 목록만 포함하고,
    // 메시지는 ProjectChannelWithMessagesCollectionDTO를 통해 필요시 로드하는 것으로 가정합니다.
    val channels: List<ProjectChannelDocumentDTO>? = null
)
