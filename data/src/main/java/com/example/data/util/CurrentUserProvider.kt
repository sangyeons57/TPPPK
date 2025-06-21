package com.example.data.util

import com.example.data.datasource.remote.AuthRemoteDataSource
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 로그인한 사용자의 ID를 제공하는 인터페이스
 */
interface CurrentUserProvider {
    /**
     * 현재 로그인한 사용자의 ID를 가져옵니다.
     * @return 사용자 ID 또는 로그인되지 않은 경우 예외 발생
     * @throws IllegalStateException 사용자가 로그인되어 있지 않은 경우
     */
    suspend fun getCurrentUserId(): String
}

/**
 * AuthRemoteDataSource를 통해 현재 사용자 ID를 제공하는 구현체
 *
 * @param authRemoteDataSource 인증 관련 원격 데이터 소스
 */
@Singleton
class CurrentUserProviderImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
) : CurrentUserProvider {
    
    /**
     * 현재 로그인한 사용자의 ID를 가져옵니다.
     * @return 사용자 ID
     * @throws IllegalStateException 사용자가 로그인되어 있지 않은 경우
     */
    override suspend fun getCurrentUserId(): String {
        return authRemoteDataSource.getCurrentUserId()
            ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
    }
} 