package com.example.data.datasource.remote.dm

import com.example.domain.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * DM 채널 관련 원격 데이터 소스 인터페이스입니다.
 */
interface DmRemoteDataSource {
    /**
     * 두 사용자 간의 기존 DM 채널을 조회합니다.
     * 채널이 존재하지 않으면 null 또는 Result.failure를 반환할 수 있습니다. (구현에 따라 결정)
     *
     * @param targetUserId DM 상대방 사용자 ID
     * @return DM 채널 정보 또는 null/오류
     */
    suspend fun getDmChannelWithUser(targetUserId: String): Result<Channel?>

    /**
     * 두 사용자 간의 DM 채널 ID를 조회합니다.
     * 채널 문서 ID만 필요할 경우 사용합니다.
     *
     * @param targetUserId DM 상대방 사용자 ID
     * @return DM 채널 ID 또는 null (채널이 없는 경우)
     */
    suspend fun getDmChannelId(targetUserId: String): Result<String?>
    
    /**
     * 새로운 DM 채널을 생성합니다.
     * Firestore 규칙에 따라 'dmSpecificData' 필드에 참여자 ID를 포함해야 합니다.
     *
     * @param targetUserId DM 상대방 사용자 ID
     * @param channelName DM 채널의 이름 (선택적, 없으면 기본값 사용)
     * @return 생성된 DM 채널 정보
     */
    suspend fun createDmChannel(targetUserId: String, channelName: String? = null): Result<Channel>

    // 향후 DM 관련 추가 기능이 필요하면 여기에 정의 (예: DM 채널 목록 스트림, 특정 DM 채널 정보 스트림 등)
    /**
     * 현재 사용자의 모든 DM 채널 목록을 스트림으로 가져옵니다.
     * @return DM 채널 목록을 포함하는 Flow<Result<List<Channel>>>.
     */
    fun getCurrentUserDmChannelsStream(): Flow<Result<List<Channel>>>
} 