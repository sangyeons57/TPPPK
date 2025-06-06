package com.example.data.datasource.remote

import com.example.data.model.remote.DMWrapperDTO
import kotlinx.coroutines.flow.Flow

interface DMWrapperRemoteDataSource {

    /**
     * 현재 로그인한 사용자의 DM 채널 요약 정보 목록을 실시간으로 관찰합니다.
     * 목록은 마지막 메시지 시간 순서대로 정렬됩니다.
     */
    fun observeDmWrappers(): Flow<List<DMWrapperDTO>>
    
    /**
     * 특정 사용자의 DM 채널 요약 정보 목록을 실시간으로 관찰합니다.
     * 목록은 마지막 메시지 시간 순서대로 정렬됩니다.
     * 
     * @param userId 조회할 사용자의 ID
     */
    fun observeDmWrappers(userId: String): Flow<List<DMWrapperDTO>>

}
