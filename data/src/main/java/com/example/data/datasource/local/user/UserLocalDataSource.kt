package com.example.data.datasource.local.user

import com.example.data.model.local.UserEntity // Local DB Entity 위치 가정
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 데이터의 로컬 데이터 소스 인터페이스입니다.
 * Room 데이터베이스와 상호작용합니다.
 */
interface UserLocalDataSource {

    /**
     * 특정 사용자의 정보를 Flow 형태로 가져옵니다.
     * @param userId 가져올 사용자의 ID.
     * @return UserEntity의 Flow. 사용자가 없으면 null을 방출하는 Flow.
     */
    fun getUserStream(userId: String): Flow<UserEntity?>

    /**
     * 사용자 정보를 삽입하거나 업데이트합니다 (Upsert).
     * @param user 추가 또는 업데이트할 사용자 엔티티.
     */
    suspend fun upsertUser(user: UserEntity)

    /**
     * 특정 사용자의 정보를 삭제합니다.
     * @param userId 삭제할 사용자의 ID.
     */
    suspend fun deleteUser(userId: String)

    // 예시: 모든 사용자 정보 삭제
    // suspend fun clearAllUsers()

    // ... 향후 필요한 사용자 관련 로컬 데이터 처리 함수 추가 ...
} 