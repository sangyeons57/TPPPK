package com.example.data.model.channel

import com.example.core_common.constants.FirestoreConstants

/**
 * Firestore 메시지 컬렉션의 경로를 특정하는 데이터 계층의 클래스입니다.
 * 모든 채널의 메시지는 글로벌 `channels/{channelId}/messages` 경로를 사용합니다.
 * 이 클래스는 채널의 컨텍스트(DM, 프로젝트 등)를 유지하면서 일관된 메시지 경로를 제공합니다.
 */
sealed class ChannelLocator {
    /**
     * 이 채널에 대한 Firestore 'messages' 서브컬렉션의 전체 경로를 반환합니다.
     * 모든 채널 유형에 대해 동일한 경로 형식을 사용하며, 각 서브클래스에 저장된 channelId를 사용합니다.
     */
    abstract fun getMessagesPath(): String

    /**
     * DM 채널의 컨텍스트를 나타냅니다.
     * @param channelId DM 채널의 ID (글로벌 채널 ID).
     */
    data class Dm(val channelId: String) : ChannelLocator() {
        override fun getMessagesPath(): String =
            "${FirestoreConstants.Collections.CHANNELS}/$channelId/${FirestoreConstants.Collections.MESSAGES}"
    }

    /**
     * 프로젝트 내 카테고리에 속한 채널의 컨텍스트를 나타냅니다.
     * @param projectId 프로젝트 ID.
     * @param categoryId 카테고리 ID.
     * @param channelId 채널 ID (글로벌 채널 ID).
     */
    data class ProjectCategoryChannel(
        val projectId: String,
        val categoryId: String,
        val channelId: String
    ) : ChannelLocator() {
        override fun getMessagesPath(): String =
            "${FirestoreConstants.Collections.CHANNELS}/$channelId/${FirestoreConstants.Collections.MESSAGES}"
    }

    /**
     * 프로젝트에 직접 속한 채널의 컨텍스트를 나타냅니다.
     * @param projectId 프로젝트 ID.
     * @param channelId 직속 채널 ID (글로벌 채널 ID).
     */
    data class ProjectDirectChannel(
        val projectId: String,
        val channelId: String
    ) : ChannelLocator() {
        override fun getMessagesPath(): String =
            "${FirestoreConstants.Collections.CHANNELS}/$channelId/${FirestoreConstants.Collections.MESSAGES}"
    }
} 