package com.example.data.repository

import com.example.data.datasource.local.invite.InviteLocalDataSource
import com.example.data.datasource.remote.invite.InviteRemoteDataSource
import com.example.domain.model.Invite
import com.example.domain.model.ProjectInfo
import com.example.domain.repository.InviteRepository
import com.example.domain.util.NetworkConnectivityMonitor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 초대 관련 저장소 구현체
 * 로컬 및 원격 데이터 소스를 조율하여 초대 데이터를 관리합니다.
 * 
 * @param remoteDataSource 초대 원격 데이터 소스
 * @param localDataSource 초대 로컬 데이터 소스
 * @param auth Firebase 인증 인스턴스
 * @param networkMonitor 네트워크 연결 상태 모니터
 */
@Singleton
class InviteRepositoryImpl @Inject constructor(
    private val remoteDataSource: InviteRemoteDataSource,
    private val localDataSource: InviteLocalDataSource,
    private val auth: FirebaseAuth,
    private val networkMonitor: NetworkConnectivityMonitor
) : InviteRepository {

    // 코루틴 스코프 (백그라운드 작업용)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
    
    // 네트워크 연결 상태 확인
    private suspend fun isNetworkConnected(): Boolean {
        return networkMonitor.isNetworkAvailable.first()
    }
    
    /**
     * 새 초대 토큰을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param expiresAt 만료 시간 (null인 경우 기본값 사용)
     * @return 생성된 초대 토큰 결과
     */
    override suspend fun createInviteToken(
        projectId: String,
        expiresAt: LocalDateTime?
    ): Result<String> {
        // 네트워크 연결 확인
        if (!isNetworkConnected()) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return remoteDataSource.createInviteToken(
            projectId = projectId,
            inviterId = currentUserId,
            expiresAt = expiresAt
        )
    }
    
    /**
     * 초대 토큰의 유효성을 검사합니다.
     * 
     * @param token 초대 토큰
     * @return 유효성 검사 결과 (true: 유효, false: 무효)
     */
    override suspend fun validateInviteToken(token: String): Result<Boolean> {
        // 네트워크 연결 확인
        if (!isNetworkConnected()) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return remoteDataSource.validateInviteToken(token)
    }
    
    /**
     * 초대를 수락하고, 사용자를 프로젝트에 추가합니다.
     * 
     * @param token 초대 토큰
     * @return 프로젝트 ID 결과
     */
    override suspend fun acceptInvite(token: String): Result<String> {
        // 네트워크 연결 확인
        if (!isNetworkConnected()) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return remoteDataSource.acceptInvite(token, currentUserId)
    }
    
    /**
     * 초대 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @param forceRefresh 원격 데이터를 강제로 가져올지 여부
     * @return 초대 정보 결과
     */
    override suspend fun getInviteDetails(token: String, forceRefresh: Boolean): Result<Invite> {
        // 네트워크가 연결되어 있고, 원격 데이터를 강제로 가져오도록 설정한 경우
        if (isNetworkConnected() && forceRefresh) {
            val remoteResult = remoteDataSource.getInviteDetails(token)
            
            // 원격 데이터 가져오기 성공 시 로컬에 저장
            if (remoteResult.isSuccess) {
                remoteResult.getOrNull()?.let { invite ->
                    scope.launch {
                        localDataSource.saveInvite(invite)
                    }
                }
            }
            
            return remoteResult
        }
        
        // 로컬에서 데이터 가져오기
        val localInvite = localDataSource.getInvite(token)
        
        // 로컬 데이터가 있으면 반환
        if (localInvite != null) {
            return Result.success(localInvite)
        }
        
        // 로컬 데이터가 없고 네트워크가 연결되어 있으면 원격에서 가져오기
        if (isNetworkConnected()) {
            val remoteResult = remoteDataSource.getInviteDetails(token)
            
            // 원격 데이터 가져오기 성공 시 로컬에 저장
            if (remoteResult.isSuccess) {
                remoteResult.getOrNull()?.let { invite ->
                    scope.launch {
                        localDataSource.saveInvite(invite)
                    }
                }
            }
            
            return remoteResult
        }
        
        // 로컬 데이터도 없고 네트워크도 연결되어 있지 않은 경우
        return Result.failure(Exception("데이터를 가져올 수 없습니다. 네트워크 연결을 확인하세요."))
    }
    
    /**
     * 초대 토큰에서 프로젝트 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @param forceRefresh 원격 데이터를 강제로 가져올지 여부
     * @return 프로젝트 정보 결과
     */
    override suspend fun getProjectInfoFromToken(token: String, forceRefresh: Boolean): Result<ProjectInfo> {
        // 네트워크 연결 확인
        if (!isNetworkConnected()) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return remoteDataSource.getProjectInfoFromToken(token)
    }
    
    /**
     * 만료된 초대 정보를 정리합니다.
     * 
     * @return 작업 성공 여부
     */
    override suspend fun cleanupExpiredInvites(): Result<Unit> = try {
        // 현재 시간 (밀리초)
        val currentTimeMillis = System.currentTimeMillis()
        
        // 로컬 저장소에서 만료된 초대 정보 삭제
        localDataSource.deleteExpiredInvites(currentTimeMillis)
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 