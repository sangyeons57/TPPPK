package com.example.data.datasource.remote.user

import com.example.data.model.remote.user.UserDto
import kotlinx.coroutines.flow.Flow

/**
 * Firestore 'users' 컬렉션 또는 관련 사용자 데이터와 상호작용하는 데이터 소스 인터페이스입니다.
 */
interface UserRemoteDataSource {
    /**
     * Firestore에서 특정 사용자의 프로필 정보를 가져옵니다.
     *
     * @param userId 가져올 사용자의 ID (Firebase Auth UID).
     * @return kotlin.Result 객체. 성공 시 UserDto, 실패 시 Exception 포함.
     */
    suspend fun getUserProfile(userId: String): Result<UserDto>

    /**
     * Firestore에서 특정 사용자의 프로필 정보를 업데이트합니다.
     * 문서가 존재하지 않으면 생성하고, 존재하면 제공된 필드만 병합합니다.
     *
     * @param userId 업데이트할 사용자의 ID (Firebase Auth UID).
     * @param userDto 업데이트할 사용자 정보 DTO.
     * @return kotlin.Result 객체. 성공 시 Unit, 실패 시 Exception 포함.
     */
    suspend fun updateUserProfile(userId: String, userDto: UserDto): Result<Unit>

    // ... 향후 필요한 사용자 관련 원격 데이터 처리 함수 추가 ...
} 