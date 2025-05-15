package com.example.domain.repository

import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.RolePermission
import com.example.domain.model.channel.DmSpecificData
import com.example.domain.model.channel.ProjectSpecificData
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 채널 관련 데이터 작업을 위한 리포지토리 인터페이스입니다.
 * 이 인터페이스는 채널 생성, 조회, 업데이트, 삭제 및 특정 조건에 따른 필터링 기능을 포함합니다.
 * 또한, 채널 내 메시지 스트림 및 과거 메시지 로드 기능도 정의합니다.
 */
interface ChannelRepository {

    // ---------- 기본 채널 CRUD ----------

    /**
     * 새 채널을 생성합니다.
     *
     * @param channel 생성할 채널 정보.
     * @return 생성된 채널 정보 또는 실패 Result.
     */
    suspend fun createChannel(channel: Channel): Result<Channel>

    /**
     * 특정 ID의 채널 정보를 가져옵니다.
     *
     * @param channelId 가져올 채널의 ID.
     * @return 채널 정보 또는 실패 Result.
     */
    suspend fun getChannel(channelId: String): Result<Channel>

    /**
     * 채널 정보를 업데이트합니다.
     *
     * @param channel 업데이트할 채널 정보.
     * @return 작업 성공 여부.
     */
    suspend fun updateChannel(channel: Channel): Result<Unit>

    /**
     * 특정 ID의 채널을 삭제합니다.
     *
     * @param channelId 삭제할 채널의 ID.
     * @return 작업 성공 여부.
     */
    suspend fun deleteChannel(channelId: String): Result<Unit>

    /**
     * 특정 채널 정보의 실시간 스트림을 제공합니다.
     *
     * @param channelId 구독할 채널의 ID.
     * @return 채널 정보 Flow.
     */
    fun getChannelStream(channelId: String): Flow<Channel>

    // ---------- 채널 필터링 및 조회 ----------

    /**
     * 특정 사용자가 참여하고 있는 채널 목록을 가져옵니다.
     * 채널 타입으로 필터링할 수 있습니다.
     *
     * @param userId 사용자 ID.
     * @param type 필터링할 채널 타입 (옵션).
     * @return 채널 목록 또는 실패 Result.
     */
    suspend fun getUserChannels(userId: String, type: ChannelType? = null): Result<List<Channel>>

    /**
     * 특정 사용자가 참여하고 있는 채널 목록의 실시간 스트림을 제공합니다.
     * 채널 타입으로 필터링할 수 있습니다.
     *
     * @param userId 사용자 ID.
     * @param type 필터링할 채널 타입 (옵션).
     * @return 채널 목록 Flow.
     */
    fun getUserChannelsStream(userId: String, type: ChannelType? = null): Flow<List<Channel>>

    /**
     * 특정 타입의 모든 채널 목록을 가져옵니다.
     * DM 채널의 경우, userId를 제공하여 해당 사용자가 참여한 DM 채널만 가져올 수 있습니다.
     *
     * @param type 필터링할 채널 타입.
     * @param userId DM 채널 필터링 시 사용할 사용자 ID (옵션).
     * @return 채널 목록 또는 실패 Result.
     */
    suspend fun getChannelsByType(type: ChannelType, userId: String? = null): Result<List<Channel>>

    /**
     * 특정 타입의 모든 채널 목록에 대한 실시간 스트림을 제공합니다.
     * DM 채널의 경우, userId를 제공하여 해당 사용자가 참여한 DM 채널만 가져올 수 있습니다.
     *
     * @param type 필터링할 채널 타입.
     * @param userId DM 채널 필터링 시 사용할 사용자 ID (옵션).
     * @return 채널 목록 Flow.
     */
    fun getChannelsByTypeStream(type: ChannelType, userId: String? = null): Flow<List<Channel>>


    // ---------- DM 채널 참가자 관리 ----------

    /**
     * DM 채널에 참가자를 추가합니다.
     * @param channelId 대상 DM 채널 ID.
     * @param userId 추가할 사용자 ID.
     * @return 작업 성공 여부.
     */
    suspend fun addDmParticipant(channelId: String, userId: String): Result<Unit>

    /**
     * DM 채널에서 참가자를 제거합니다.
     * @param channelId 대상 DM 채널 ID.
     * @param userId 제거할 사용자 ID.
     * @return 작업 성공 여부.
     */
    suspend fun removeDmParticipant(channelId: String, userId: String): Result<Unit>

    /**
     * DM 채널의 모든 참가자 ID 목록을 가져옵니다.
     * @param channelId 대상 DM 채널 ID.
     * @return 참가자 ID 목록 또는 실패 Result.
     */
    suspend fun getDmParticipants(channelId: String): Result<List<String>>

    /**
     * DM 채널의 모든 참가자 ID 목록에 대한 실시간 스트림을 제공합니다.
     * @param channelId 대상 DM 채널 ID.
     * @return 참가자 ID 목록 Flow.
     */
    fun getDmParticipantsStream(channelId: String): Flow<List<String>>

    // ---------- 채널 권한 관리 (Override) ----------
    /**
     * 채널에 대한 특정 사용자의 권한 오버라이드를 설정합니다.
     *
     * @param channelId 채널 ID.
     * @param userId 사용자 ID.
     * @param permission 설정할 권한.
     * @param value 권한 값 (true: 허용, false: 거부, null: 오버라이드 제거).
     * @return 작업 성공 여부.
     */
    suspend fun setChannelPermissionOverride(
        channelId: String,
        userId: String,
        permission: RolePermission,
        value: Boolean? // null to remove override
    ): Result<Unit>

    /**
     * 특정 채널에서 특정 사용자의 모든 권한 오버라이드를 가져옵니다.
     *
     * @param channelId 채널 ID.
     * @param userId 사용자 ID.
     * @return 권한과 해당 값의 맵 또는 실패 Result.
     */
    suspend fun getChannelPermissionOverridesForUser(
        channelId: String,
        userId: String
    ): Result<Map<RolePermission, Boolean>>

    /**
     * 특정 채널의 모든 사용자 권한 오버라이드를 가져옵니다.
     *
     * @param channelId 채널 ID.
     * @return 사용자 ID를 키로, 해당 사용자의 권한 오버라이드 맵을 값으로 하는 맵 또는 실패 Result.
     */
    suspend fun getAllChannelPermissionOverrides(channelId: String): Result<Map<String, Map<RolePermission, Boolean>>>


    // ---------- 채널 타입별 특수 기능 ----------

    /**
     * 새 프로젝트 채널(카테고리 또는 직속)을 생성합니다.
     *
     * @param name 채널 이름.
     * @param projectId 프로젝트 ID.
     * @param categoryId 카테고리 ID (직속 채널인 경우 null).
     * @param channelMode 채널 모드 (예: TEXT, VOICE).
     * @param description 채널 설명 (옵션).
     * @param order 채널 순서 (옵션).
     * @return 생성된 채널 정보 또는 실패 Result.
     */
    suspend fun createProjectChannel(
        name: String,
        projectId: String,
        categoryId: String? = null,
        channelMode: ChannelMode, // FirestoreConstants.ChannelModeValues 중 하나
        description: String? = null,
        order: Int? = null
    ): Result<Channel>

    /**
     * 두 사용자 간의 DM 채널을 가져오거나, 없으면 새로 생성합니다.
     * 이미 존재하는 DM 채널이 있으면 해당 채널 정보를 반환하고, 없으면 새로 생성하여 반환합니다.
     *
     * @param myUserId 현재 사용자 ID.
     * @param otherUserId 상대방 사용자 ID.
     * @return DM 채널 정보 또는 실패 Result.
     */
    suspend fun getOrCreateDmChannel(myUserId: String, otherUserId: String): Result<Channel>
}