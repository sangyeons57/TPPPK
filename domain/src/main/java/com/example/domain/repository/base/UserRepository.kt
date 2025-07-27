package com.example.domain.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User

/**
 * Remote User Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 *
 * 🔒 제약사항:
 * - Room 접근 금지 (LocalUserRepository 사용)
 * - Flow/LiveData 반환 금지 (비동기 fetch-only)
 * - 직접적인 CRUD 작업 불가능
 *
 * ✅ 역할:
 * - 서버에서 증분 데이터 가져오기 (updatedAt > cursor)
 * - 로컬 변경사항을 서버에 반영 (Outbox → Firestore)
 * - 동기화 충돌 해결 (서버 vs 로컬)
 * - 커서 기반 동기화 메타데이터 관리
 * - Firebase Functions 호출 (프로필 업데이트, 이미지 업로드 등)
 */
interface UserRepository {

    // === 동기화 메서드 ===

    /**
     * 서버에서 증분 데이터 가져오기 (Client-driven Sync)
     * @param lastSyncCursor 마지막 동기화 커서 (null이면 전체 동기화)
     * @param userIds 특정 사용자들만 동기화 (null이면 전체 사용자)
     * @return 새로운 사용자 목록과 다음 커서
     */
    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        userIds: List<String>? = null
    ): CustomResult<SyncResult<User>, Exception>

    /**
     * 로컬 변경사항을 서버에 반영 (Outbox Processing)
     * @param userIds 특정 사용자들의 Outbox만 처리 (null이면 전체)
     * @return 처리된 Outbox 작업 수
     */
    suspend fun syncToServer(
        userIds: List<String>? = null
    ): CustomResult<Int, Exception>

    /**
     * 강제 전체 동기화 (예: 첫 로그인, 데이터 불일치 해결)
     * @return 동기화된 사용자 수
     */
    suspend fun forceSyncAll(): CustomResult<Int, Exception>

    /**
     * 동기화 충돌 해결 (서버 우선 정책)
     * @param conflictedUserIds 충돌이 발생한 사용자 ID 목록
     * @return 해결된 충돌 수
     */
    suspend fun resolveConflicts(
        conflictedUserIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions 호출 (서버 작업) ===

    /**
     * 사용자 프로필 이미지를 업로드합니다.
     * Firebase Storage에 업로드 후 자동으로 Firebase Functions가 처리합니다.
     *
     * @param uri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun uploadProfileImage(uri: Uri): CustomResult<Unit, Exception>

    /**
     * 사용자 프로필 이미지를 삭제합니다.
     * Firebase Functions를 통해 프로필 이미지를 제거합니다.
     *
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun removeProfileImage(): CustomResult<Unit, Exception>

    /**
     * 사용자 프로필을 업데이트합니다.
     * Firebase Functions를 통해 이름, 메모 등의 프로필 정보를 업데이트합니다.
     *
     * @param name 새로운 사용자 이름 (nullable)
     * @param memo 새로운 사용자 메모 (nullable)
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun updateProfile(
        name: String? = null,
        memo: String? = null
    ): CustomResult<Unit, Exception>

    /**
     * Firebase Functions의 callable function을 호출합니다.
     * 
     * @param functionName 호출할 함수 이름
     * @param data 함수에 전달할 데이터 (nullable)
     * @return 함수 실행 결과를 담은 CustomResult
     */
    suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * "Hello World" 메시지를 반환하는 함수를 호출합니다.
     * 
     * @return Hello World 메시지를 담은 CustomResult
     */
    suspend fun getHelloWorld(): CustomResult<String, Exception>

    /**
     * 사용자 정의 데이터와 함께 함수를 호출합니다.
     * 
     * @param functionName 호출할 함수 이름
     * @param userId 사용자 ID
     * @param customData 사용자 정의 데이터
     * @return 함수 실행 결과를 담은 CustomResult
     */
    suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * FCM 테스트용 Functions 호출
     */
    suspend fun sendFcmTestNotification(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception>
}

/**
 * 동기화 결과 데이터 클래스
 */
data class SyncResult<T>(
    val data: List<T>,
    val nextCursor: Long?,
    val hasMore: Boolean = false
)
