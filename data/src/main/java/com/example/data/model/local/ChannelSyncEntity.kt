package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 채널별 동기화 상태 관리를 위한 Room Entity
 * 증분 동기화와 페이지네이션 상태를 추적
 */
@Entity(tableName = "channel_sync_info")
data class ChannelSyncEntity(
    @PrimaryKey
    val channelId: String,

    /**
     * 마지막 동기화 시간 (UTC)
     * Firestore 쿼리에서 updateAt > lastSyncTimestamp 조건으로 사용
     */
    val lastSyncTimestamp: Instant,

    /**
     * 로컬에 저장된 메시지 개수
     */
    val messageCount: Int = 0,

    /**
     * 서버에 더 오래된 메시지가 있는지 여부
     * 페이지네이션에서 "더 보기" 버튼 표시 결정
     */
    val hasMoreOlderMessages: Boolean = true,

    /**
     * 가장 오래된 메시지의 생성 시간
     * 과거 메시지 로딩 시 beforeTimestamp로 사용
     */
    val oldestMessageTimestamp: Instant? = null,

    /**
     * 가장 최신 메시지의 생성 시간
     * 최신 메시지 확인 시 afterTimestamp로 사용
     */
    val newestMessageTimestamp: Instant? = null,

    /**
     * 마지막 동기화 실행 시간
     */
    val lastSyncAt: Instant = Instant.now(),

    /**
     * 동기화 실패 횟수 (에러 추적용)
     */
    val syncFailureCount: Int = 0
)