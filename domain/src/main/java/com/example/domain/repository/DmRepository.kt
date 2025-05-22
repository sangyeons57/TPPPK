package com.example.domain.repository

import com.example.domain.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * DM 채널 관련 데이터 처리를 위한 Repository 인터페이스입니다.
 */
interface DmRepository {
    /**
     * 특정 사용자와의 기존 DM 채널 정보를 가져옵니다.
     *
     * @param targetUserId DM 상대방 사용자 ID
     * @return Result 객체. 성공 시 {@link Channel} 또는 null (채널이 없는 경우), 실패 시 예외 포함.
     */
    suspend fun getDmChannelWithUser(targetUserId: String): Result<Channel?>

    /**
     * 특정 사용자와의 DM 채널 ID를 가져옵니다.
     * 채널 문서 ID만 필요한 경우 사용합니다.
     *
     * @param targetUserId DM 상대방 사용자 ID
     * @return Result 객체. 성공 시 채널 ID 문자열 또는 null (채널이 없는 경우), 실패 시 예외 포함.
     */
    suspend fun getDmChannelId(targetUserId: String): Result<String?>

    /**
     * 새로운 DM 채널을 생성합니다.
     *
     * @param targetUserId DM 상대방 사용자 ID
     * @param channelName DM 채널의 이름 (선택적, 없으면 기본값 사용)
     * @return Result 객체. 성공 시 생성된 {@link Channel} 정보, 실패 시 예외 포함.
     */
    suspend fun createDmChannel(targetUserId: String, channelName: String? = null): Result<Channel>
    
    /**
     * 현재 사용자의 모든 DM 채널 목록을 스트림으로 가져옵니다.
     * @return DM 채널 목록을 포함하는 Flow<Result<List<Channel>>>.
     */
    fun getCurrentUserDmChannelsStream(): Flow<Result<List<Channel>>>
} 