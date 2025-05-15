package com.example.data.migration

import com.example.domain.model.Channel
import com.example.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채널 시스템 마이그레이션 도구
 * 기존 DM과 프로젝트 채팅 데이터를 새로운 채널 구조로 마이그레이션하는 기능을 제공합니다.
 */
@Singleton
class ChannelMigrationTool @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // 마이그레이션 상태 및 통계
    data class MigrationStats(
        var channelsCreated: Int = 0,
        var messagesTransferred: Int = 0,
        var failureCount: Int = 0,
        var errors: MutableList<String> = mutableListOf()
    )
    
    /**
     * DM 대화를 새 채널 시스템으로 마이그레이션합니다.
     * 
     * @param batchSize 한 번에 처리할 DM 수
     * @return 마이그레이션 결과 통계
     */
    suspend fun migrateDmConversations(batchSize: Int = 20): Result<MigrationStats> {
        val stats = MigrationStats()
        
        try {
            val currentUser = auth.currentUser ?: return Result.failure(
                IllegalStateException("User must be logged in to perform migration")
            )
            
            // 1. 사용자가 참여 중인 DM 목록 가져오기
            val dmsSnapshot = firestore.collection("dms")
                .whereArrayContains("participants", currentUser.uid)
                .limit(batchSize.toLong())
                .get()
                .await()
            
            for (dmDoc in dmsSnapshot.documents) {
                try {
                    val dmId = dmDoc.id
                    val data = dmDoc.data ?: continue
                    
                    // 이미 마이그레이션된 DM은 건너뛰기
                    if (data.containsKey("channelId")) {
                        continue
                    }
                    
                    // 2. 참여자 정보 추출
                    @Suppress("UNCHECKED_CAST")
                    val participants = data["participants"] as? List<String> ?: continue
                    
                    if (participants.size != 2) {
                        stats.errors.add("DM $dmId has invalid participant count: ${participants.size}")
                        continue
                    }
                    
                    // 3. 채널 생성
                    val otherUserId = participants.first { it != currentUser.uid }
                    val otherUserDoc = firestore.collection("users").document(otherUserId).get().await()
                    val otherUserName = otherUserDoc.getString("displayName") ?: otherUserId
                    
                    val channelId = createChannelForDm(dmId, participants, otherUserName)
                    
                    // 4. DM 문서 업데이트
                    firestore.collection("dms").document(dmId)
                        .update("channelId", channelId)
                        .await()
                    
                    // 5. 메시지 마이그레이션
                    val messageCount = migrateMessages(dmId, channelId, "dms")
                    stats.messagesTransferred += messageCount
                    stats.channelsCreated++
                    
                } catch (e: Exception) {
                    stats.failureCount++
                    stats.errors.add("Error migrating DM ${dmDoc.id}: ${e.message}")
                }
            }
            
            return Result.success(stats)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * 프로젝트 채널을 새 채널 시스템으로 마이그레이션합니다.
     * 
     * @param projectId 마이그레이션할 프로젝트 ID
     * @return 마이그레이션 결과 통계
     */
    suspend fun migrateProjectChannels(projectId: String): Result<MigrationStats> {
        val stats = MigrationStats()
        
        try {
            val currentUser = auth.currentUser ?: return Result.failure(
                IllegalStateException("User must be logged in to perform migration")
            )
            
            // 1. 프로젝트 정보 로드
            val projectDoc = firestore.collection("projects").document(projectId).get().await()
            val projectData = projectDoc.data ?: return Result.failure(
                IllegalStateException("Project not found: $projectId")
            )
            
            // 2. 프로젝트 챌넝 로드
            val channelsSnapshot = firestore.collection("projects")
                .document(projectId)
                .collection("channels")
                .get()
                .await()
            
            for (channelDoc in channelsSnapshot.documents) {
                try {
                    val channelId = channelDoc.id
                    val data = channelDoc.data ?: continue
                    
                    // 이미 마이그레이션된 채널은 건너뛰기
                    if (data.containsKey("newChannelId")) {
                        continue
                    }
                    
                    val channelName = data["name"] as? String ?: "프로젝트 채널"
                    val categoryId = data["categoryId"] as? String
                    
                    // 3. 프로젝트 멤버 목록 로드
                    @Suppress("UNCHECKED_CAST")
                    val projectMembers = projectData["memberIds"] as? List<String> ?: listOf(currentUser.uid)
                    
                    // 4. 새 채널 생성
                    val newChannelId = createChannelForProject(
                        oldChannelId = channelId,
                        projectId = projectId,
                        categoryId = categoryId,
                        name = channelName,
                        participants = projectMembers
                    )
                    
                    // 5. 기존 채널 문서 업데이트
                    firestore.collection("projects")
                        .document(projectId)
                        .collection("channels")
                        .document(channelId)
                        .update("newChannelId", newChannelId)
                        .await()
                    
                    // 6. 프로젝트 채널 참조 문서 생성
                    createProjectChannelRef(projectId, newChannelId, categoryId)
                    
                    // 7. 메시지 마이그레이션
                    val messageCount = migrateMessages(
                        oldId = channelId, 
                        newChannelId = newChannelId, 
                        oldCollection = "projects/$projectId/channels"
                    )
                    
                    stats.messagesTransferred += messageCount
                    stats.channelsCreated++
                    
                } catch (e: Exception) {
                    stats.failureCount++
                    stats.errors.add("Error migrating channel ${channelDoc.id}: ${e.message}")
                }
            }
            
            return Result.success(stats)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * DM을 위한 새 채널을 생성합니다.
     */
    private suspend fun createChannelForDm(
        dmId: String,
        participants: List<String>,
        otherUserName: String
    ): String {
        val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val now = Date()
        
        // 채널 문서 생성
        val channelRef = firestore.collection("channels").document()
        val channelId = channelRef.id
        
        val metadata = mapOf(
            "source" to "dm",
            "dmId" to dmId,
            "avatarUrl" to "" // 추후 상대방 프로필 이미지로 대체
        )
        
        val channel = hashMapOf(
            "id" to channelId,
            "name" to otherUserName, // DM은 상대방 이름을 채널명으로 사용
            "description" to null,
            "ownerId" to currentUserId, // DM은 현재 사용자가 소유
            "participantIds" to participants,
            "lastMessagePreview" to null,
            "lastMessageTimestamp" to null,
            "metadata" to metadata,
            "createdAt" to now,
            "updatedAt" to now
        )
        
        channelRef.set(channel).await()
        return channelId
    }
    
    /**
     * 프로젝트 채널을 위한 새 채널을 생성합니다.
     */
    private suspend fun createChannelForProject(
        oldChannelId: String,
        projectId: String,
        categoryId: String?,
        name: String,
        participants: List<String>
    ): String {
        val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val now = Date()
        
        // 채널 문서 생성
        val channelRef = firestore.collection("channels").document()
        val channelId = channelRef.id
        
        val metadata = hashMapOf<String, Any>(
            "source" to "project",
            "projectId" to projectId,
            "oldChannelId" to oldChannelId
        )
        
        if (categoryId != null) {
            metadata["categoryId"] = categoryId
        }
        
        val channel = hashMapOf(
            "id" to channelId,
            "name" to name,
            "description" to null,
            "ownerId" to currentUserId,
            "participantIds" to participants,
            "lastMessagePreview" to null,
            "lastMessageTimestamp" to null,
            "metadata" to metadata,
            "createdAt" to now,
            "updatedAt" to now
        )
        
        channelRef.set(channel).await()
        return channelId
    }
    
    /**
     * 프로젝트-채널 참조 문서를 생성합니다.
     */
    private suspend fun createProjectChannelRef(
        projectId: String,
        channelId: String,
        categoryId: String?
    ) {
        val now = Date()
        
        val refData = hashMapOf(
            "projectId" to projectId,
            "channelId" to channelId,
            "order" to 0,
            "createdAt" to now
        )
        
        if (categoryId != null) {
            // 카테고리에 속한 채널
            firestore.collection("projects")
                .document(projectId)
                .collection("categories")
                .document(categoryId)
                .collection("channelRefs")
                .document()
                .set(refData)
                .await()
        } else {
            // 프로젝트 직속 채널
            firestore.collection("projects")
                .document(projectId)
                .collection("directChannelRefs")
                .document()
                .set(refData)
                .await()
        }
    }
    
    /**
     * 기존 메시지를 새 채널로 마이그레이션합니다.
     * 
     * @param oldId 기존 채널/DM ID
     * @param newChannelId 새 채널 ID
     * @param oldCollection 기존 컬렉션 경로
     * @return 마이그레이션된 메시지 수
     */
    private suspend fun migrateMessages(
        oldId: String,
        newChannelId: String,
        oldCollection: String
    ): Int {
        var messageCount = 0
        
        // 기존 메시지 로드
        val messagesSnapshot = firestore.collection("$oldCollection/$oldId/messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        
        // 새 채널에 메시지 추가
        for (messageDoc in messagesSnapshot.documents) {
            val data = messageDoc.data ?: continue
            
            // 기본 필드
            val newMessage = hashMapOf<String, Any>(
                "id" to messageDoc.id,
                "channelId" to newChannelId,
                "senderId" to (data["senderId"] as? String ?: "unknown"),
                "text" to (data["text"] as? String ?: ""),
                "timestamp" to (data["timestamp"] as? Date ?: Date()),
                "isEdited" to (data["isEdited"] as? Boolean ?: false),
                "isDeleted" to (data["isDeleted"] as? Boolean ?: false)
            )
            
            // 선택적 필드
            data["replyToMessageId"]?.let { newMessage["replyToMessageId"] = it }
            data["senderName"]?.let { newMessage["senderName"] = it }
            data["senderProfileUrl"]?.let { newMessage["senderProfileUrl"] = it }
            
            // 리액션 처리
            @Suppress("UNCHECKED_CAST")
            val reactions = data["reactions"] as? Map<String, List<String>>
            if (!reactions.isNullOrEmpty()) {
                newMessage["reactions"] = reactions
            }
            
            // 첨부파일 처리
            @Suppress("UNCHECKED_CAST")
            val attachments = data["attachments"] as? List<Map<String, Any>>
            if (!attachments.isNullOrEmpty()) {
                newMessage["attachments"] = attachments
            }
            
            // 새 메시지 저장
            firestore.collection("channels")
                .document(newChannelId)
                .collection("messages")
                .document(messageDoc.id)
                .set(newMessage)
                .await()
            
            messageCount++
        }
        
        return messageCount
    }
    
    /**
     * Date를 LocalDateTime으로 변환합니다.
     */
    private fun Date.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(this.toInstant(), ZoneOffset.UTC)
    }
} 