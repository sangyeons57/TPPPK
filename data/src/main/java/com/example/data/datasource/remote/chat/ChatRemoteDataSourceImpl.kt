package com.example.data.datasource.remote.chat

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.MessageFields
import com.example.data.model.remote.chat.ChatMessageDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.example.data.model.mapper.ChatMessageMapper
import com.example.data.model.remote.media.MediaImageDto
import com.example.data.util.FirestoreConstants
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.NoSuchElementException
import kotlin.Result
import androidx.core.net.toUri

/**
 * ChatRemoteDataSource 인터페이스의 Firestore 구현체입니다.
 */
@Singleton
class ChatRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ChatRemoteDataSource {

    // 채팅 채널별 서브컬렉션 사용
    private fun getChatCollection(channelId: String) = 
        firestore.collection(Collections.CHAT_CHANNELS).document(channelId).collection(Collections.MESSAGES)

    /**
     * 특정 채널의 메시지 스트림을 실시간으로 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @return 채팅 메시지 DTO 리스트를 포함하는 Flow
     */
    override fun getMessagesStream(channelId: String): Flow<List<ChatMessageDto>> = callbackFlow {
        // 채널 타입에 따라 컬렉션 경로 결정 (프로젝트 채널 vs DM)
        val collectionPath = getMessagesCollectionPath(channelId)
        
        // Firestore 쿼리 생성: 시간 기준 최신 50개 메시지
        val query = firestore.collection(collectionPath)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(50)
        
        // 스냅샷 리스너 등록
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // 에러 발생 시 채널 닫지 않고 빈 리스트 전송
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            // 스냅샷이 없거나 비어있는 경우
            if (snapshot == null || snapshot.isEmpty) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            // 문서를 ChatMessageDto로 변환하여 리스트 생성
            val messages = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    ChatMessageDto.fromMap(data, doc.id)
                } else null
            }.sortedBy { it.sentAt } // 오름차순 정렬 (시간순)
            
            // Flow에 메시지 리스트 전송
            trySend(messages)
        }
        
        // Flow가 취소될 때 리스너 해제
        awaitClose { registration.remove() }
    }

    /**
     * 특정 메시지 ID 이전의 과거 메시지를 페이징 방식으로 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 가져옴
     * @param limit 가져올 메시지 최대 개수
     * @return 과거 메시지 DTO 리스트를 포함한 Result
     */
    override suspend fun fetchPastMessages(
        channelId: String,
        beforeMessageId: Int,
        limit: Int
    ): Result<List<ChatMessageDto>> = try {
        val collectionPath = getMessagesCollectionPath(channelId)
        
        // 특정 메시지 ID의 데이터를 먼저 조회 (기준점 찾기)
        val referenceMessage = firestore.collection(collectionPath)
            .whereEqualTo("chatId", beforeMessageId)
            .get()
            .await()
            .documents
            .firstOrNull()
        
        // 기준 메시지가 없는 경우 빈 리스트 반환
        if (referenceMessage == null) {
            Result.success(emptyList())
        } else {
            // 기준 메시지의 sentAt 값 가져오기
            val refSentAt = referenceMessage.getLong("sentAt") ?: 0L
            
            // 기준 메시지보다 이전 메시지 가져오기
            val querySnapshot = firestore.collection(collectionPath)
                .whereLessThan("sentAt", refSentAt)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            // 문서를 ChatMessageDto로 변환
            val messages = querySnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    ChatMessageDto.fromMap(data, doc.id)
                } else null
            }.sortedBy { it.sentAt } // 오름차순 정렬 (시간순)
            
            Result.success(messages)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 새 메시지를 전송합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param message 메시지 내용
     * @param attachmentPaths 첨부 이미지 경로 목록
     * @return 전송된 메시지 DTO를 포함한 Result
     */
    override suspend fun sendMessage(
        channelId: String,
        message: String,
        attachmentPaths: List<String>
    ): Result<ChatMessageDto> = try {
        val collectionPath = getMessagesCollectionPath(channelId)
        
        // 첨부 이미지가 있는 경우 Storage에 업로드
        val attachmentUrls = if (attachmentPaths.isNotEmpty()) {
            uploadAttachments(channelId, attachmentPaths)
        } else {
            emptyList()
        }
        
        // 새 메시지 ID 생성 (서버 측에서 자동 생성될 수도 있음)
        val messageId = System.currentTimeMillis().toInt()
        
        // 메시지 DTO 생성
        val messageDto = ChatMessageDto(
            chatId = messageId,
            channelId = channelId,
            userId = 1, // TODO: 현재 사용자 ID 가져오기
            userName = "Current User", // TODO: 현재 사용자 이름 가져오기
            userProfileUrl = null, // TODO: 현재 사용자 프로필 URL 가져오기
            message = message,
            sentAt = System.currentTimeMillis(),
            isModified = false,
            attachmentImageUrls = attachmentUrls
        )
        
        // Firestore에 메시지 저장
        firestore.collection(collectionPath)
            .document() // Firestore가 문서 ID 자동 생성
            .set(messageDto.toMap())
            .await()
        
        // 저장 성공 시 DTO 반환
        Result.success(messageDto)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 기존 메시지를 수정합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param chatId 수정할 메시지 ID
     * @param newMessage 새 메시지 내용
     * @return 작업 결과
     */
    override suspend fun editMessage(
        channelId: String,
        chatId: Int,
        newMessage: String
    ): Result<Unit> = try {
        val collectionPath = getMessagesCollectionPath(channelId)
        
        // chatId로 메시지 문서 찾기
        val querySnapshot = firestore.collection(collectionPath)
            .whereEqualTo("chatId", chatId)
            .get()
            .await()
        
        // 메시지가 없는 경우 에러 반환
        if (querySnapshot.isEmpty) {
            Result.failure(NoSuchElementException("Message with ID $chatId not found"))
        } else {
            // 첫 번째 일치하는 문서 가져오기
            val document = querySnapshot.documents.first()
            
            // 메시지 내용과 수정 상태 업데이트
            firestore.collection(collectionPath)
                .document(document.id)
                .update(
                    mapOf(
                        "message" to newMessage,
                        "isModified" to true
                    )
                )
                .await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 메시지를 삭제합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param chatId 삭제할 메시지 ID
     * @return 작업 결과
     */
    override suspend fun deleteMessage(
        channelId: String,
        chatId: Int
    ): Result<Unit> = try {
        val collectionPath = getMessagesCollectionPath(channelId)
        
        // chatId로 메시지 문서 찾기
        val querySnapshot = firestore.collection(collectionPath)
            .whereEqualTo("chatId", chatId)
            .get()
            .await()
        
        // 메시지가 없는 경우 에러 반환
        if (querySnapshot.isEmpty) {
            Result.failure(NoSuchElementException("Message with ID $chatId not found"))
        } else {
            // 첫 번째 일치하는 문서 가져오기
            val document = querySnapshot.documents.first()
            
            // 메시지에 첨부된 이미지가 있는지 확인
            val data = document.data
            val attachmentUrls = (data?.get("attachmentImageUrls") as? List<*>)?.mapNotNull { it as? String }
            
            // 첨부된 이미지가 있으면 Storage에서 삭제
            if (!attachmentUrls.isNullOrEmpty()) {
                deleteAttachments(attachmentUrls)
            }
            
            // Firestore에서 메시지 문서 삭제
            firestore.collection(collectionPath)
                .document(document.id)
                .delete()
                .await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Firebase Storage에서 이미지를 페이징 방식으로 가져옵니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지당 이미지 수
     * @return 미디어 이미지 DTO 목록
     */
    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): List<MediaImageDto> {
        try {
            // chat_attachments 폴더의 이미지들을 가져오기
            val imagesRef = storage.reference.child("chat_attachments")
            
            // 폴더 내의 모든 항목을 나열
            val listResult = imagesRef.listAll().await()
            
            // 페이징 처리
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, listResult.items.size)
            
            // 잘못된 페이지 번호인 경우 빈 목록 반환
            if (startIndex >= listResult.items.size) {
                return emptyList()
            }
            
            // 페이지에 해당하는 항목들을 가져옴
            val paginatedItems = listResult.items.subList(startIndex, endIndex)
            
            // 각 항목의 다운로드 URL을 가져와 MediaImageDto 객체 생성
            return paginatedItems.mapNotNull { ref ->
                try {
                    val downloadUrl = ref.downloadUrl.await().toString()
                    val fileName = ref.name
                    
                    MediaImageDto(
                        id = fileName,
                        uri = downloadUrl,
                        name = fileName,
                        path = "chat_attachments/$fileName",
                        mimeType = "image/*", // 실제 MIME 타입 정보가 없으므로 일반적인 이미지 타입으로 설정
                        size = 0, // 크기 정보를 가져오기 어려움
                        dateAdded = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    println("Failed to get download URL for ${ref.path}, Error: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Failed to list images from Firebase Storage: ${e.message}")
            return emptyList()
        }
    }

    /**
     * 채널 ID에 따른 메시지 컬렉션 경로를 반환합니다.
     * 
     * @param channelId 채널 ID
     * @return Firestore 컬렉션 경로
     */
    private fun getMessagesCollectionPath(channelId: String): String {
        // 채널 ID 형식에 따라 프로젝트 채널 또는 DM으로 구분
        return if (channelId.contains("_dm_")) {
            // DM 채널인 경우
            "${FirestoreConstants.Collections.DM_CHANNELS}/$channelId/${FirestoreConstants.ChatMessages.SUBCOLLECTION_NAME}"
        } else {
            // 프로젝트 채널인 경우
            // 채널 ID 형식이 "projectId_categoryId_channelId"라고 가정
            val parts = channelId.split("_")
            if (parts.size >= 3) {
                val projectId = parts[0]
                val categoryId = parts[1]
                val actualChannelId = parts[2]
                "${FirestoreConstants.Collections.PROJECTS}/$projectId/${FirestoreConstants.Projects.CategoriesSubcollection.NAME}/$categoryId/${FirestoreConstants.Projects.ChannelsSubcollection.NAME}/$actualChannelId/${FirestoreConstants.ChatMessages.SUBCOLLECTION_NAME}"
            } else {
                // 형식이 맞지 않는 경우 기본 경로 사용
                "${FirestoreConstants.Collections.PROJECTS}/$channelId/${FirestoreConstants.ChatMessages.SUBCOLLECTION_NAME}"
            }
        }
    }

    /**
     * 첨부 이미지를 Firebase Storage에 업로드합니다.
     * 
     * @param channelId 채널 ID
     * @param attachmentPaths 로컬 이미지 경로 목록
     * @return 업로드된 이미지 URL 목록
     */
    private suspend fun uploadAttachments(channelId: String, attachmentPaths: List<String>): List<String> {
        val uploadedUrls = mutableListOf<String>()
        
        for (path in attachmentPaths) {
            try {
                // 파일 URI 생성
                val uri = path.toUri()
                
                // Storage 참조 생성 (경로: chat_attachments/{channelId}/{fileName})
                val fileName = "${UUID.randomUUID()}_${uri.lastPathSegment ?: "image"}"
                val storageRef = storage.reference
                    .child("chat_attachments")
                    .child(channelId)
                    .child(fileName)
                
                // 파일 업로드
                val uploadTask = storageRef.putFile(uri).await()
                
                // 다운로드 URL 가져오기
                val downloadUrl = storageRef.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
            } catch (e: Exception) {
                // 개별 업로드 실패 시 로그 기록하고 계속 진행
                println("Failed to upload attachment: $path, Error: ${e.message}")
            }
        }
        
        return uploadedUrls
    }

    /**
     * 첨부 이미지를 Firebase Storage에서 삭제합니다.
     * 
     * @param attachmentUrls 삭제할 이미지 URL 목록
     */
    private suspend fun deleteAttachments(attachmentUrls: List<String>) {
        for (url in attachmentUrls) {
            try {
                // URL에서 경로 추출
                val path = storage.getReferenceFromUrl(url)
                
                // 파일 삭제
                path.delete().await()
            } catch (e: Exception) {
                // 개별 삭제 실패 시 로그 기록하고 계속 진행
                println("Failed to delete attachment: $url, Error: ${e.message}")
            }
        }
    }
} 