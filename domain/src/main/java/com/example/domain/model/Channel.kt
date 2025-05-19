// 경로: domain/model/Channel.kt (ProjectSettingViewModel, ProjectStructure 관련 기반)
package com.example.domain.model

import com.example.domain.model.channel.DmSpecificData
import com.example.domain.model.channel.ProjectSpecificData
import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * 채널 정보를 나타내는 데이터 클래스입니다.
 * 모든 타입의 채팅(DM, 프로젝트, 스케줄)에 사용되는 통합 모델입니다.
 */
data class Channel(
    /**
     * 채널의 고유 ID (Firestore Document ID)
     */
    @DocumentId
    val id: String,
    
    /**
     * 채널의 이름입니다.
     */
    val name: String,
    
    /**
     * 채널 설명 또는 토픽입니다.
     */
    val description: String? = null,
    
    /**
     * 채널 타입입니다. DM, PROJECT, CATEGORY와 같은 컨텍스트 타입을 나타냅니다.
     * FirestoreConstants.ChannelTypeValues의 값 중 하나여야 합니다.
     */
    val type: ChannelType,

    /**
     * 마지막 메시지 미리보기입니다.
     */
    val lastMessagePreview: String? = null,
    
    /**
     * 마지막 메시지가 전송된 시간입니다.
     * UTC 기준 시간으로 저장됩니다.
     */
    val lastMessageTimestamp: Instant? = null,
    
    /**
     * 채널 생성자 ID입니다.
     */
    val createdBy: String? = null,
    
    /**
     * 채널 생성 시간입니다.
     * UTC 기준 시간으로 저장됩니다.
     */
    val createdAt: Instant,
    
    /**
     * 채널 마지막 업데이트 시간입니다.
     * UTC 기준 시간으로 저장됩니다.
     */
    val updatedAt: Instant,
    
    /**
     * 프로젝트 채널 특화 데이터입니다.
     * PROJECT 또는 CATEGORY 타입 채널에서만 사용됩니다.
     */
    val projectSpecificData: ProjectSpecificData? = null,
    
    /**
     * DM 채널 특화 데이터입니다.
     * DM 타입 채널에서만 사용됩니다.
     */
    val dmSpecificData: DmSpecificData? = null
) {

    /**
     * 이 채널이 DM 채널인지 확인합니다.
     */
    val isDm: Boolean
        get() = type == ChannelType.DM
    
    /**
     * 이 채널이 프로젝트 채널인지 확인합니다.
     */
    val isProjectChannel: Boolean
        get() = type == ChannelType.PROJECT
    
    /**
     * 이 채널이 카테고리 채널인지 확인합니다.
     * 참고: 카테고리는 제거되고 프로젝트로 통합되었습니다.
     * @deprecated 사용하지 않음
     */
    @Deprecated("카테고리는 PROJECT로 통합되었습니다. isProjectChannel을 사용하세요.")
    val isPROJECTChannel: Boolean
        get() = type == ChannelType.PROJECT

    /**
     * 채널 모드입니다. TEXT, VOICE 등 채널의 실제 형식을 나타냅니다.
     * 프로젝트/카테고리 채널의 경우 projectSpecificData에서 가져오며,
     * DM 채널의 경우 항상 "TEXT"로 간주합니다.
     */
    val channelMode: ChannelMode? = projectSpecificData?.channelMode


    /**
     * 채널이 속한 프로젝트 ID를 가져옵니다(프로젝트/카테고리 채널인 경우).
     */
    val projectId: String?
        get() = projectSpecificData?.projectId
    
    /**
     * 채널이 속한 카테고리 ID를 가져옵니다(카테고리 채널인 경우).
     */
    val categoryId: String?
        get() = projectSpecificData?.categoryId

    /**
     * 채널의 표시 순서를 가져옵니다.
     */
    val order: Int
        get() = projectSpecificData?.order ?: 0
        
    /**
     * 채널 참여자 ID 목록을 가져옵니다.
     */
    val participantIds: List<String>
        get() = dmSpecificData?.participantIds ?: emptyList()

    /**
     * DM 채널의 경우 상대방 ID를 가져옵니다.
     * @param myUserId 현재 사용자 ID
     * @return 상대방 ID 또는 참여자가 2명이 아닌 경우 null
     */
    fun getOtherParticipantId(myUserId: String): String? {
        val participants = dmSpecificData?.participantIds ?: return null
        return if (isDm && participants.size == 2) {
            participants.find { it != myUserId }
        } else {
            null
        }
    }
    
    /**
     * DM 채널의 경우 참여자 ID 목록을 가져옵니다.
     * DM이 아닌 경우에는 빈 리스트를 반환합니다.
     */
    val dmParticipantIds: List<String>
        get() = if (isDm) participantIds else emptyList()
        
    /**
     * 생성 시간을 UI 표시용 LocalDateTime으로 변환합니다.
     */
    // fun getCreatedAtLocal(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    //     return LocalDateTime.ofInstant(createdAt, zoneId)
    // }
    
    /**
     * 업데이트 시간을 UI 표시용 LocalDateTime으로 변환합니다.
     */
    // fun getUpdatedAtLocal(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    //     return LocalDateTime.ofInstant(updatedAt, zoneId)
    // }
    
    /**
     * 마지막 메시지 시간을 UI 표시용 LocalDateTime으로 변환합니다.
     */
    // fun getLastMessageTimestampLocal(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime? {
    //     return lastMessageTimestamp?.let { LocalDateTime.ofInstant(it, zoneId) }
    // }

    companion object {
        /**
         * 프로젝트 채널 또는 카테고리 채널에 대한 ProjectSpecificData를 생성하는 헬퍼 함수
         */
        fun createProjectSpecificData(
            projectId: String,
            categoryId: String? = null,
            order: Int = 0,
            channelMode: ChannelMode
        ): ProjectSpecificData {
            return ProjectSpecificData(
                projectId = projectId,
                categoryId = categoryId,
                order = order,
                channelMode = channelMode
            )
        }
    }
}

/**
 * Channel 객체에서 이전 버전과의 호환성을 위한 확장 함수들
 */
object ChannelBackwardCompatibility {
    /**
     * 이전 버전과의 호환성을 위해 metadata 맵을 생성합니다.
     * 새 모델에서는 사용하지 않지만 기존 코드와의 호환성을 위해 제공됩니다.
     */
    fun getMetadataMap(channel: Channel): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        
        // 프로젝트 채널인 경우
        channel.projectSpecificData?.let { projectData ->
            map["projectId"] = projectData.projectId
            projectData.categoryId?.let { map["categoryId"] = it }
            
            // source 필드 추가
            if (channel.type == ChannelType.PROJECT || channel.type == ChannelType.PROJECT) {
                map["source"] = "project"
            }
        }
        
        // DM 채널인 경우
        if (channel.type == ChannelType.DM) {
            map["source"] = "dm"
        }
        
        return map
    }
}