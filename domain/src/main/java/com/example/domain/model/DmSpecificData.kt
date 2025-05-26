package com.example.domain.model.channel

/**
 * DM 채널에 특화된 데이터를 담는 클래스입니다.
 */
data class DmSpecificData(
    /**
     * 채널 참여자 ID 목록 (보통 2명)
     */
    val participantIds: List<String> = emptyList()
) 