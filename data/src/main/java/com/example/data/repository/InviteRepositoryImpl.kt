package com.example.data.repository

import com.example.data.datasource.remote.invite.InviteRemoteDataSource
import com.example.domain.model.Invite
import com.example.domain.model.ProjectInfo
import com.example.domain.repository.InviteRepository
import com.example.domain.util.NetworkConnectivityMonitor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 초대 관련 저장소 구현체
 * 원격 데이터 소스를 사용하여 초대 데이터를 관리하고 Firestore 캐시를 활용합니다.
 * 
 * @param remoteDataSource 초대 원격 데이터 소스
 * @param auth Firebase 인증 인스턴스
 * @param networkMonitor 네트워크 연결 상태 모니터
 */
@Singleton
class InviteRepositoryImpl @Inject constructor(
    private val remoteDataSource: InviteRemoteDataSource,
    private val auth: FirebaseAuth,
    private val networkMonitor: NetworkConnectivityMonitor
) : InviteRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
    
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
        expiresAt: Instant?
    ): Result<String> {
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
        if (!isNetworkConnected()) {
            // Firestore 캐시가 있을 경우 오프라인에서도 유효성 검사 시도 가능 (서버에서 만료되지 않았다는 가정하에)
            // 다만, 여기서는 네트워크 연결 강제
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
        if (!isNetworkConnected()) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        return remoteDataSource.acceptInvite(token, currentUserId)
    }
    
    /**
     * 초대 정보를 가져옵니다. Firestore 캐시를 활용합니다.
     * forceRefresh 파라미터는 Firestore의 get(Source.SERVER) 옵션으로 대체 가능하나, 여기서는 단순화.
     */
    override suspend fun getInviteDetails(token: String, forceRefresh: Boolean): Result<Invite> {
        // forceRefresh는 Firestore의 get(Source.SERVER)로 처리하거나, 
        // 여기서는 네트워크 연결 시 항상 최신 데이터를 가져오도록 remoteDataSource에 의존한다고 가정.
        // Firestore 캐시가 활성화되어 있으므로, 네트워크 미연결 시 캐시된 데이터 시도.
        if (!isNetworkConnected() && !forceRefresh) { // 오프라인이고 강제 새로고침이 아닐 때만 실패 처리 (캐시 의존)
             // 캐시에서 읽는 동작은 remoteDataSource.getInviteDetails 내부에서 처리되어야 함 (Firestore SDK가 자동으로)
             // 따라서 여기서는 네트워크 연결이 안되어 있으면 그냥 호출하고 SDK에 맡김.
             // 만약 명시적으로 오프라인일때 실패처리 하고싶으면 아래 주석 해제.
             // return Result.failure(Exception("네트워크 연결이 필요합니다. 오프라인 상태에서는 초대 정보를 가져올 수 없습니다."))
        }
        // remoteDataSource가 Firestore 캐시를 활용하도록 구현되어 있다고 가정.
        return remoteDataSource.getInviteDetails(token)
    }
    
    /**
     * 초대 토큰에서 프로젝트 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @param forceRefresh 원격 데이터를 강제로 가져올지 여부
     * @return 프로젝트 정보 결과
     */
    override suspend fun getProjectInfoFromToken(token: String, forceRefresh: Boolean): Result<ProjectInfo> {
        if (!isNetworkConnected() && !forceRefresh) {
            // 위 getInviteDetails와 유사한 로직 적용 가능
        }
        return remoteDataSource.getProjectInfoFromToken(token)
    }
    
    /**
     * 만료된 초대 정보 정리 기능은 서버 사이드 로직 또는 Firestore TTL 정책으로 이전 고려.
     * 클라이언트에서 로컬 캐시만 정리하던 기능은 더 이상 유효하지 않음.
     */
    override suspend fun cleanupExpiredInvites(): Result<Unit> {
        // TODO: 서버측에 만료된 토큰 정리 API 요청 또는 Firestore TTL 정책 활용 알림.
        // 현재 클라이언트에서는 특별한 작업 없음.
        return Result.success(Unit) 
    }
} 