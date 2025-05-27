package com.example.domain.repository

import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 채널(프로젝트 채널, DM 채널) 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ChannelRepository {
    /**
     * 특정 프로젝트에 속한 모든 프로젝트 채널 목록을 실시간 스트림으로 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 채널 목록을 담은 Result Flow.
     */
    fun getProjectChannelsStream(projectId: String): Flow<Result<List<Channel>>>

    /**
     * 현재 사용자가 참여하고 있는 모든 DM 채널 목록을 실시간 스트림으로 가져옵니다.
     * @param userId 현재 사용자 ID
     * @return DM 채널 목록을 담은 Result Flow.
     */
    fun getDmChannelsStream(userId: String): Flow<Result<List<Channel>>>

    /**
     * 특정 ID를 가진 채널의 정보를 가져옵니다.
     * @param channelId 채널 ID
     * @return 해당 채널 정보를 담은 Result.
     */
    suspend fun getChannelDetails(channelId: String): Result<Channel>

    /**
     * 새로운 프로젝트 채널을 생성합니다.
     * @param name 채널 이름
     * @param projectId 채널이 속할 프로젝트 ID
     * @param categoryId 채널이 속할 카테고리 ID (선택적)
     * @param channelType 채널 타입 (주로 PROJECT_TEXT 또는 PROJECT_VOICE)
     * @param isPrivate 비공개 채널 여부
     * @param memberIds 초기 멤버 ID 목록 (생성자 포함)
     * @return 생성된 채널의 ID를 담은 Result.
     */
    suspend fun createProjectChannel(
        name: String,
        projectId: String,
        categoryId: String?,
        channelType: ChannelType,
        isPrivate: Boolean,
        memberIds: List<String>
    ): Result<String>

    /**
     * 새로운 DM 채널을 생성하거나 기존 DM 채널을 가져옵니다.
     * @param userId1 사용자1 ID (현재 사용자)
     * @param userId2 사용자2 ID (상대방 사용자)
     * @return 생성되거나 기존에 있던 DM 채널의 ID를 담은 Result.
     */
    suspend fun getOrCreateDmChannel(userId1: String, userId2: String): Result<String>

    /**
     * 채널 정보를 업데이트합니다. (이름, 설명 등)
     * @param channelId 업데이트할 채널 ID
     * @param name 새로운 채널 이름
     * @param description 새로운 채널 설명
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun updateChannelInfo(channelId: String, name: String, description: String?): Result<Unit>

    /**
     * 채널을 삭제합니다.
     * @param channelId 삭제할 채널 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun deleteChannel(channelId: String): Result<Unit>

    /**
     * 채널에 멤버를 추가합니다.
     * @param channelId 대상 채널 ID
     * @param userIds 추가할 사용자 ID 목록
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun addMembersToChannel(channelId: String, userIds: List<String>): Result<Unit>

    /**
     * 채널에서 멤버를 제거합니다.
     * @param channelId 대상 채널 ID
     * @param userId 제거할 사용자 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun removeMemberFromChannel(channelId: String, userId: String): Result<Unit>

    /**
     * 특정 채널의 마지막 메시지를 실시간 스트림으로 가져옵니다.
     * @param channelId 채널 ID
     * @return 마지막 메시지 정보를 담은 Result Flow. 메시지가 없으면 null 또는 빈 Result일 수 있습니다.
     */
    fun getLastMessageStream(channelId: String): Flow<Result<ChatMessage?>>

    /**
     * 특정 채널에서 사용자가 읽지 않은 메시지 수를 실시간 스트림으로 가져옵니다.
     * (구현이 복잡할 수 있으며, MessageRepository와의 협력 또는 별도 로직이 필요할 수 있습니다.)
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 수를 담은 Result Flow.
     */
    fun getUnreadMessageCountStream(channelId: String, userId: String): Flow<Result<Int>>
    
    /**
     * 사용자가 특정 채널의 메시지를 읽었음을 표시합니다. (읽음 처리)
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun markMessagesAsRead(channelId: String, userId: String): Result<Unit>
}
