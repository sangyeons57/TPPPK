package com.example.data.datasource.remote.dm

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.DmFields
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.DmConversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DM 관련 원격 데이터 소스 구현체
 * Firebase Firestore를 사용하여 DM 관련 기능을 구현합니다.
 *
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 */
@Singleton
class DmRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope // 비동기 작업을 위한 코루틴 스코프 주입 (Hilt 등으로)
) : DmRemoteDataSource {

    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")

    /**
     * DM 목록 실시간 스트림을 가져옵니다.
     * @return DM 목록의 Flow
     */
    override fun getDmListStream(): Flow<List<DmConversation>> = callbackFlow {
        val userId = currentUserId
        
        // 사용자 문서에서 활성 DM ID 목록 가져오기
        val userRef = firestore.collection(Collections.USERS).document(userId)
        
        // 실시간 스냅샷 리스너 설정
        val subscription = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList()) // 에러 발생 시 빈 목록 전송
                return@addSnapshotListener
            }
            
            if (snapshot == null || !snapshot.exists()) {
                trySend(emptyList()) // 사용자 문서가 없으면 빈 목록 전송
                return@addSnapshotListener
            }
            
            // 활성 DM ID 목록 안전하게 가져오기
            val activeDmIds = snapshot.get("activeDmIds")?.let { data ->
                if (data is List<*>) {
                    data.mapNotNull { it as? String }
                } else {
                    emptyList()
                } 
            } ?: emptyList()
            
            if (activeDmIds.isEmpty()) {
                trySend(emptyList()) // DM ID 목록이 비어있으면 빈 목록 전송
                return@addSnapshotListener
            }
            
            // 비동기 처리로 DM 목록 가져오기
            scope.launch { // 주입된 코루틴 스코프 사용
                try {
                    val dmConversations = loadDmConversations(userId, activeDmIds)
                    trySend(dmConversations) // 로드된 DM 목록 전송
                } catch (e: Exception) {
                    trySend(emptyList()) // 로딩 중 에러 발생 시 빈 목록 전송
                }
            }
        }
        
        // 구독 취소 시 스냅샷 리스너 제거
        awaitClose { subscription.remove() }
    }
    
    /**
     * DM 대화 목록을 비동기적으로 로드합니다.
     */
    private suspend fun loadDmConversations(userId: String, dmIds: List<String>): List<DmConversation> {
        val dmConversations = mutableListOf<DmConversation>()
        
        // DM ID 목록을 10개씩 나누어 처리 (Firestore는 in 쿼리에 최대 10개만 허용)
        for (chunk in dmIds.chunked(10)) {
            // DM 문서 쿼리
            val dmQuery = firestore.collection(Collections.DMS)
                .whereIn("__name__", chunk)
                .get()
                .await()
            
            // 각 DM 문서 처리
            for (dmDoc in dmQuery.documents) {
                // 참가자 목록 안전하게 가져오기
                val participants = dmDoc.get(DmFields.PARTICIPANTS)?.let { data ->
                    if (data is List<*>) {
                        data.mapNotNull { it as? String }
                    } else {
                        null
                    }
                }
                if (participants == null) continue // 참가자 정보 없으면 스킵
                
                // 다른 참가자 ID 가져오기
                val otherUserId = participants.firstOrNull { it != userId } ?: continue
                
                // 다른 참가자 정보 가져오기
                val otherUserDoc = firestore.collection(Collections.USERS).document(otherUserId).get().await()
                
                // 타임스탬프 가져오기
                val lastMessageTimestamp = DateTimeUtil.toLocalDateTime(dmDoc.getTimestamp(DmFields.LAST_MESSAGE_TIMESTAMP))
                
                // DM 정보 생성
                val dmConversation = DmConversation(
                    channelId = dmDoc.id,
                    partnerUserId = otherUserId,
                    partnerUserName = otherUserDoc.getString(UserFields.NICKNAME) ?: "알 수 없음",
                    partnerProfileImageUrl = otherUserDoc.getString(UserFields.PROFILE_IMAGE_URL),
                    lastMessage = dmDoc.getString(DmFields.LAST_MESSAGE),
                    lastMessageTimestamp = lastMessageTimestamp,
                    unreadCount = 0 // TODO: 미읽은 메시지 수 계산 추가
                )
                
                dmConversations.add(dmConversation)
            }
        }
        
        // DM 목록 정렬 (최근 메시지 기준)
        return dmConversations.sortedByDescending { it.lastMessageTimestamp }
    }

    /**
     * DM 목록을 Firestore에서 가져옵니다.
     * @return 작업 성공 여부
     */
    override suspend fun fetchDmList(): Result<Unit> {
        return try {
            // 실시간 스트림 구현이 있으므로 단순히 성공을 반환
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 새 DM 채널을 생성합니다.
     * @param otherUserId 대화 상대 사용자 ID
     * @return 생성된 DM 채널 ID
     */
    override suspend fun createDmChannel(otherUserId: String): Result<String> {
        return try {
            val userId = currentUserId
            
            // 두 사용자 ID를 알파벳 순으로 정렬하여 DM 채널 ID 생성
            val sortedUserIds = listOf(userId, otherUserId).sorted()
            val dmId = "${sortedUserIds[0]}_${sortedUserIds[1]}"
            
            // 해당 DM이 존재하는지 확인
            val dmDoc = firestore.collection(Collections.DMS).document(dmId).get().await()
            
            if (dmDoc.exists()) {
                // 이미 존재하면 ID 반환
                return Result.success(dmId)
            }
            
            // 존재하지 않으면 새로 생성
            val dmData = hashMapOf(
                DmFields.PARTICIPANTS to sortedUserIds,
                "createdAt" to FieldValue.serverTimestamp()
            )
            
            firestore.collection(Collections.DMS).document(dmId).set(dmData).await()
            
            // 사용자 문서에 활성 DM ID 추가
            sortedUserIds.forEach { uid ->
                firestore.collection(Collections.USERS).document(uid)
                    .update("activeDmIds", FieldValue.arrayUnion(dmId))
                    .await()
            }
            
            Result.success(dmId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DM 채널을 삭제합니다.
     * @param dmId 삭제할 DM 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteDmChannel(dmId: String): Result<Unit> {
        return try {
            // DM 문서 가져오기
            val dmDoc = firestore.collection(Collections.DMS).document(dmId).get().await()
            
            if (!dmDoc.exists()) {
                return Result.failure(IllegalArgumentException("존재하지 않는 DM 채널입니다."))
            }
            
            // 참가자 목록 안전하게 가져오기
            val participants = dmDoc.get(DmFields.PARTICIPANTS)?.let { data ->
                if (data is List<*>) {
                    data.mapNotNull { it as? String }
                } else {
                    null
                }
            } ?: return Result.failure(IllegalArgumentException("DM 참가자 정보가 없습니다."))
            
            // 트랜잭션 실행
            firestore.runTransaction { transaction ->
                // DM 문서 삭제
                transaction.delete(firestore.collection(Collections.DMS).document(dmId))
                
                // 참가자 문서에서 활성 DM ID 제거
                participants.forEach { uid ->
                    val userRef = firestore.collection(Collections.USERS).document(uid)
                    transaction.update(userRef, "activeDmIds", FieldValue.arrayRemove(dmId))
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 