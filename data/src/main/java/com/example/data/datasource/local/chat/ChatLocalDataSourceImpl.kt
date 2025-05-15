package com.example.data.datasource.local.chat

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import com.example.data.db.dao.ChatMessageDao
import com.example.data.model.local.MediaImageEntity
import com.example.data.model.local.chat.ChatMessageEntity
import com.example.data.model.mapper.ChatMessageMapper
import com.example.data.model.mapper.toDomainModel
import com.example.data.model.mapper.toDomainModelWithTime
import com.example.data.model.mapper.toDtoWithTime
import com.example.data.model.mapper.toEntity
import com.example.data.model.remote.chat.ChatMessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ChatLocalDataSource 인터페이스의 Room 구현체
 * 로컬 데이터베이스와 안드로이드 미디어 스토어를 통해 채팅 관련 로컬 데이터 액세스를 구현합니다.
 * 
 * @property context 안드로이드 컨텍스트
 * @property chatMessageDao 채팅 메시지 DAO
 * @property chatMessageMapper 채팅 메시지 매퍼
 */
class ChatLocalDataSourceImpl @Inject constructor(
    private val context: Context,
    private val chatMessageDao: ChatMessageDao,
    private val chatMessageMapper: ChatMessageMapper,
    private val contentResolver: ContentResolver
) : ChatLocalDataSource {

    /**
     * 특정 채널의 메시지 스트림을 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형 (예: "DM", "PROJECT_CATEGORY")
     * @return 채팅 메시지 엔티티 리스트를 포함하는 Flow
     */
    override fun getMessagesStream(channelId: String, channelType: String): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getMessagesStream(channelId, channelType)
    }

    /**
     * 특정 채널의 메시지 목록을 저장합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     * @param messages 저장할 채팅 메시지 엔티티 목록
     */
    override suspend fun saveMessages(channelId: String, channelType: String, messages: List<ChatMessageEntity>) {
        chatMessageDao.insertOrReplaceMessages(messages)
    }

    /**
     * 단일 메시지를 추가하거나 업데이트합니다. (chatId 기준)
     * ChatMessageEntity는 channelId와 channelType을 이미 가지고 있어야 합니다.
     *
     * @param message 추가 또는 업데이트할 채팅 메시지 엔티티
     */
    override suspend fun upsertMessage(message: ChatMessageEntity) {
        chatMessageDao.upsertMessageByChatId(message)
    }

    /**
     * 메시지를 삭제합니다. (chatId 기준)
     *
     * @param chatId 삭제할 메시지의 Firestore ID
     */
    override suspend fun deleteMessage(chatId: String) {
        chatMessageDao.deleteMessageByChatId(chatId)
    }

    /**
     * 특정 채널의 모든 메시지를 삭제합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     */
    override suspend fun clearMessagesForChannel(channelId: String, channelType: String) {
        chatMessageDao.clearMessagesForChannel(channelId, channelType)
    }

    /**
     * 채팅 메시지를 로컬 데이터베이스에 저장합니다.
     *
     * @param message 저장할 채팅 메시지 DTO
     * @param channelType 이 메시지가 속한 채널의 유형
     */
    override suspend fun insertMessage(message: ChatMessageDto, channelType: String) {
        val domainModel = message.toDomainModelWithTime()
        val entity = domainModel.toEntity(channelType)
        chatMessageDao.upsertMessageByChatId(entity)
    }

    /**
     * 여러 채팅 메시지를 로컬 데이터베이스에 저장합니다.
     *
     * @param messages 저장할 채팅 메시지 DTO 목록
     * @param channelType 이 메시지들이 속한 채널의 유형
     */
    override suspend fun insertMessages(messages: List<ChatMessageDto>, channelType: String) {
        val entities = messages.map {
            val domainModel = it.toDomainModelWithTime()
            domainModel.toEntity(channelType)
        }
        chatMessageDao.insertOrReplaceMessages(entities)
    }

    /**
     * 특정 채널의 모든 메시지를 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     * @return 해당 채널의 모든 메시지 DTO 목록
     */
    override suspend fun getAllMessages(channelId: String, channelType: String): List<ChatMessageDto> {
        val entities = chatMessageDao.getAllMessages(channelId, channelType)
        return entities.map { entity -> 
            val domainModel = entity.toDomainModel()
            domainModel.toDtoWithTime()
        }
    }

    /**
     * 특정 시간 이전의 메시지를 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     * @param beforeSentAt 이 타임스탬프 이전의 메시지를 가져옴 (milliseconds)
     * @param limit 가져올 메시지 최대 개수
     * @return 메시지 DTO 목록
     */
    override suspend fun getMessagesBefore(channelId: String, channelType: String, beforeSentAt: Long, limit: Int): List<ChatMessageDto> {
        val entities = chatMessageDao.getMessagesBefore(channelId, channelType, beforeSentAt, limit)
        return entities.map { entity ->
            val domainModel = entity.toDomainModel()
            domainModel.toDtoWithTime()
        }
    }

    /**
     * 메시지 내용을 업데이트합니다.
     *
     * @param chatId 수정할 메시지의 Firestore ID
     * @param newMessage 새 메시지 내용
     */
    override suspend fun updateMessageContent(chatId: String, newMessage: String) {
        chatMessageDao.updateMessageContentByChatId(chatId, newMessage, true)
    }

    /**
     * 메시지를 삭제합니다. (Firestore ID chatId 기준)
     *
     * @param chatId 삭제할 메시지의 Firestore ID
     */
    override suspend fun deleteMessageByChatId(chatId: String) {
        chatMessageDao.deleteMessageByChatId(chatId)
    }

    /**
     * 로컬 갤러리 이미지를 페이징 방식으로 가져옵니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지당 이미지 수
     * @return 미디어 이미지 엔티티 목록
     */
    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): List<MediaImageEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaImageEntity>()
        val imagesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        // 정렬 순서 (최신 순)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        // 페이징 오프셋
        val offset = page * pageSize
        
        // ContentResolver 쿼리
        // Android Q 이상에서는 limit/offset 지원
        val query = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Bundle().apply {
                // Limit
                putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                // Offset
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                // Sorting
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            }
        } else {
            // Bundle 쿼리 미지원 버전은 직접 쿼리 문자열 생성 (오프셋 처리 어려움, 아래 로직에서 커서 이동으로 처리)
            null
        }

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.query(imagesUri, projection, query, null)
        } else {
            // 구 버전에서는 limit/offset 직접 지원 안됨, 커서에서 처리 필요
             contentResolver.query(imagesUri, projection, null, null, "$sortOrder LIMIT $pageSize OFFSET $offset")
             // LIMIT/OFFSET이 모든 기기에서 보장되지 않을 수 있음 -> 아래 커서 이동 로직으로 보완
        }

        cursor?.use { data ->
            val idColumn = data.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = data.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = data.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeTypeColumn = data.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateAddedColumn = data.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            
             // 구 버전을 위한 오프셋 처리
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !data.moveToPosition(offset)) {
                 return@withContext emptyList() // 오프셋 이동 실패 시 빈 리스트 반환
             }

            // 데이터 읽기
            var count = 0
            while (data.moveToNext() && count < pageSize) {
                val id = data.getLong(idColumn)
                val name = data.getString(nameColumn)
                val size = data.getLong(sizeColumn)
                val mimeType = data.getString(mimeTypeColumn)
                val dateAdded = data.getLong(dateAddedColumn)
                
                // content URI 생성 (String으로 변환 X)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                result.add(
                    // MediaImageEntity 생성 시 Uri를 String으로 변환 필요
                    MediaImageEntity(
                        id = id.toString(),
                        contentPath = contentUri.toString(), // Mapper가 없으므로 직접 변환
                        name = name,
                        size = size,
                        mimeType = mimeType,
                        dateAdded = dateAdded
                    )
                )
                count++
            }
        }
        
        result
    }
} 