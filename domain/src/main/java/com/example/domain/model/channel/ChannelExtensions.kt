package com.example.domain.model.channel

import com.example.domain.model.Channel
import com.example.domain.model.ChannelType

/**
 * 채널 데이터 접근을 위한 확장 함수들
 */

/**
 * 채널이 속한 프로젝트 ID를 가져옵니다.
 * 프로젝트/카테고리 채널에서만 사용 가능합니다.
 */
fun Channel.getProjectId(): String? = projectSpecificData?.projectId

/**
 * 채널이 속한 카테고리 ID를 가져옵니다.
 * 카테고리 채널에서만 의미 있는 값을 반환합니다.
 */
fun Channel.getCategoryId(): String? = projectSpecificData?.categoryId

/**
 * 채널의 표시 순서를 가져옵니다.
 * 프로젝트/카테고리 채널에서만 사용합니다.
 */
fun Channel.getOrder(): Int = projectSpecificData?.order ?: 0

/**
 * DM 채널의 참여자 목록을 가져옵니다.
 */
fun Channel.getParticipantIds(): List<String> = dmSpecificData?.participantIds ?: emptyList()

/**
 * 프로젝트 직속 채널인지 확인합니다.
 */
fun Channel.isDirectProjectChannel(): Boolean = 
    type == ChannelType.PROJECT && projectSpecificData?.categoryId == null

/**
 * 카테고리에 속한 채널인지 확인합니다.
 */
fun Channel.isBelongToCategory(): Boolean = 
    type == ChannelType.CATEGORY && projectSpecificData?.categoryId != null

/**
 * 지정된 사용자가 DM 채널에 참여자인지 확인합니다.
 */
fun Channel.isUserParticipant(userId: String): Boolean =
    dmSpecificData?.participantIds?.contains(userId) ?: false

/**
 * 채널에 참여자를 추가한 새 채널 객체를 생성합니다.
 * DM 채널에서만 유효한 작업입니다.
 */
fun Channel.addParticipant(userId: String): Channel {
    if (type != ChannelType.DM || dmSpecificData == null) return this
    
    val currentParticipants = dmSpecificData.participantIds
    if (currentParticipants.contains(userId)) return this
    
    val updatedParticipants = currentParticipants + userId
    val updatedDmData = dmSpecificData.copy(participantIds = updatedParticipants)
    
    return copy(dmSpecificData = updatedDmData)
}

/**
 * 채널에서 참여자를 제거한 새 채널 객체를 생성합니다.
 * DM 채널에서만 유효한 작업입니다.
 */
fun Channel.removeParticipant(userId: String): Channel {
    if (type != ChannelType.DM || dmSpecificData == null) return this
    
    val currentParticipants = dmSpecificData.participantIds
    if (!currentParticipants.contains(userId)) return this
    
    val updatedParticipants = currentParticipants.filter { it != userId }
    val updatedDmData = dmSpecificData.copy(participantIds = updatedParticipants)
    
    return copy(dmSpecificData = updatedDmData)
} 