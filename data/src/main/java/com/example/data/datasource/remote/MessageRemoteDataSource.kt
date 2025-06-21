package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.MessageDTO
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메시지 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 채널의 메시지에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 메시지 데이터는 `/{channelPath}/messages/{messageId}` 경로에 저장되며, `messageId`는 자동 생성됩니다.
 * 모든 작업 전에 `setCollection(channelPath)`를 호출하여 채널 컨텍스트를 설정해야 합니다.
 * `channelPath`는 부모 채널 문서의 전체 경로입니다 (예: "dm_channels/channelId123" 또는 "projects/projectId123/channels/channelId456").
 */
interface MessageRemoteDataSource : DefaultDatasource {

    /**
     * 특정 채널에 새로운 메시지를 전송합니다. Firestore가 메시지 ID를 자동 생성합니다.
     * **중요:** 이 메서드를 호출하기 전에 `setCollection(channelPath)`를 통해 컨텍스트를 설정해야 합니다.
     * @param channelPath 메시지를 보낼 채널의 전체 경로. 이 경로는 `setCollection`에 전달된 경로와 일치해야 합니다.
     * @param message 전송할 메시지의 상세 정보. `id` 필드는 무시되거나 비워두며, `senderId`, `senderName` 등은 미리 채워져 있어야 합니다.
     * @return 생성된 MessageDTO (자동 생성된 ID 포함)를 포함한 CustomResult.
     * @throws IllegalStateException `setCollection(channelPath)`가 호출되지 않았거나 `channelPath`가 일치하지 않는 경우.
     * @throws IllegalArgumentException `channelPath`가 유효한 Firestore 문서 경로 형식이 아닌 경우.
     */
    suspend fun sendMessage(channelPath: String, message: MessageDTO): CustomResult<MessageDTO, Exception>

    /**
     * 기존 메시지의 내용을 수정합니다. `updatedAt` 필드도 서버 시간으로 갱신됩니다.
     * **중요:** 이 메서드를 호출하기 전에 `setCollection(channelPath)`를 통해 컨텍스트를 설정해야 합니다.
     * @param channelPath 대상 채널의 전체 경로. 이 경로는 `setCollection`에 전달된 경로와 일치해야 합니다.
     * @param messageId 수정할 메시지의 ID.
     * @param newContent 새로운 메시지 내용.
     * @return 작업 성공 여부 또는 오류를 포함한 CustomResult.
     * @throws IllegalStateException `setCollection(channelPath)`가 호출되지 않았거나 `channelPath`가 일치하지 않는 경우.
     * @throws IllegalArgumentException `channelPath`가 유효한 Firestore 문서 경로 형식이 아니거나 `messageId` 또는 `newContent`가 비어있는 경우.
     */
    suspend fun updateMessageContent(channelPath: String, messageId: String, newContent: String): CustomResult<Unit, Exception>

    /**
     * 메시지를 논리적으로 삭제합니다 (soft delete). `isDeleted` 플래그를 true로 설정하고 `updatedAt`을 갱신합니다.
     * **중요:** 이 메서드를 호출하기 전에 `setCollection(channelPath)`를 통해 컨텍스트를 설정해야 합니다.
     * @param channelPath 대상 채널의 전체 경로. 이 경로는 `setCollection`에 전달된 경로와 일치해야 합니다.
     * @param messageId 삭제할 메시지의 ID.
     * @return 작업 성공 여부 또는 오류를 포함한 CustomResult.
     * @throws IllegalStateException `setCollection(channelPath)`가 호출되지 않았거나 `channelPath`가 일치하지 않는 경우.
     * @throws IllegalArgumentException `channelPath`가 유효한 Firestore 문서 경로 형식이 아니거나 `messageId`가 비어있는 경우.
     */
    suspend fun softDeleteMessage(channelPath: String, messageId: String): CustomResult<Unit, Exception>
}

@Singleton
class MessageRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<MessageDTO>(firestore, MessageDTO::class.java), MessageRemoteDataSource {

    private var currentChannelPath: String? = null


    private fun checkCollectionInitialized(methodName: String, expectedChannelPath: String? = null) {
        super.checkCollectionInitialized(methodName)
        if (expectedChannelPath != null && currentChannelPath != expectedChannelPath) {
            throw IllegalStateException(
                "Channel path mismatch for $methodName. Expected: $expectedChannelPath, Current: $currentChannelPath. Ensure setCollection() was called with the correct channel path."
            )
        }
    }

    override suspend fun sendMessage(channelPath: String, message: MessageDTO): CustomResult<MessageDTO, Exception> = withContext(Dispatchers.IO) {
        return@withContext TODO()
    }

    override suspend fun updateMessageContent(channelPath: String, messageId: String, newContent: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        setCollection(channelPath)
        checkCollectionInitialized("updateMessageContent", channelPath)
        require(messageId.isNotBlank()) { "messageId cannot be blank for updateMessageContent." }
        require(newContent.isNotBlank()) { "newContent cannot be blank for updateMessageContent." }

        try {
            val updates = mapOf(
                MessageDTO.SEND_MESSAGE to newContent,
                MessageDTO.UPDATED_AT to FieldValue.serverTimestamp()
            )
            super.collection.document(messageId).update(updates).await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun softDeleteMessage(channelPath: String, messageId: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        setCollection(channelPath)
        checkCollectionInitialized("softDeleteMessage", channelPath)
        require(messageId.isNotBlank()) { "messageId cannot be blank for softDeleteMessage." }

        try {
            val updates = mapOf(
                MessageDTO.IS_DELETED to true,
                MessageDTO.UPDATED_AT to FieldValue.serverTimestamp()
            )
            super.collection.document(messageId).update(updates).await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
