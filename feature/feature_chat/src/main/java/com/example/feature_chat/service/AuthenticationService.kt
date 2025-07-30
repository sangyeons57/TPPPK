package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.UserId
import com.example.domain_usecase.provider.auth.AuthSessionUseCases
import com.example.feature_chat.websocket.ChatWebSocketClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 사용자 인증 및 세션 관리를 담당하는 Service
 * 인증 상태 모니터링, 토큰 관리, 채팅방 입장 등의 기능을 제공합니다.
 */
class AuthenticationService(
    private val authUseCases: AuthSessionUseCases,
    private val webSocketClient: ChatWebSocketClient,
    private val roomId: String
) {
    
    data class AuthState(
        val userSession: UserSession? = null,
        val currentUserId: String? = null,
        val isAuthenticated: Boolean = false
    )
    
    /**
     * 현재 사용자 세션을 스트림으로 제공
     */
    fun getCurrentUserSessionStream(): Flow<CustomResult<AuthState, Exception>> {
        return authUseCases.getCurrentUserSessionStreamUseCase().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val userSession = result.data
                    val currentUserId = userSession.userId.value
                    Log.d("AuthenticationService", "Current user authenticated: $currentUserId")
                    
                    CustomResult.Success(
                        AuthState(
                            userSession = userSession,
                            currentUserId = currentUserId,
                            isAuthenticated = true
                        )
                    )
                }
                is CustomResult.Failure -> {
                    Log.e("AuthenticationService", "Authentication failed", result.error)
                    CustomResult.Failure(result.error)
                }
                is CustomResult.Loading -> {
                    Log.d("AuthenticationService", "Authentication loading...")
                    CustomResult.Loading
                }
                is CustomResult.Initial -> {
                    Log.d("AuthenticationService", "Authentication initial state")
                    CustomResult.Loading
                }
                is CustomResult.Progress -> {
                    Log.d("AuthenticationService", "Authentication progress: ${result.progress}%")
                    CustomResult.Loading
                }
            }
        }
    }
    
    /**
     * 채팅방에 입장
     */
    suspend fun joinChatRoom(userId: String): Result<Unit> {
        Log.d("AuthenticationService", "Joining chat room: $roomId")
        val result = webSocketClient.joinRoom(roomId, UserId(userId))
        
        return if (result.isSuccess) {
            Log.d("AuthenticationService", "Successfully joined chat room: $roomId")
            Result.success(Unit)
        } else {
            Log.e("AuthenticationService", "Failed to join chat room: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to join chat room"))
        }
    }
    
    /**
     * 현재 사용자의 인증 토큰을 가져옴
     */
    suspend fun getCurrentUserAuthToken(): String? {
        return when (val result = authUseCases.getCurrentUserSessionUseCase()) {
            is CustomResult.Success -> {
                val token = result.data.idToken?.value
                Log.d("AuthenticationService", "Got auth token: ${token?.take(10)}...")
                token
            }
            is CustomResult.Failure -> {
                Log.e("AuthenticationService", "Failed to get auth token", result.error)
                null
            }
            else -> {
                Log.d("AuthenticationService", "Auth token loading...")
                null
            }
        }
    }
    
    /**
     * 사용자 세션을 한 번만 가져옴 (스트림이 아닌)
     */
    suspend fun getCurrentUserSession(): CustomResult<UserSession, Exception> {
        return authUseCases.getCurrentUserSessionUseCase()
    }
}
