
package com.example.data.datasource._remote

import com.example.data.model._remote.MessageAttachmentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageAttachmentRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MessageAttachmentRemoteDataSource {

    companion object {
        private const val ATTACHMENTS_COLLECTION = "message_attachments"
    }

    private fun getAttachmentsCollection(messagePath: String) =
        firestore.document(messagePath).collection(ATTACHMENTS_COLLECTION)

    override fun getAttachments(messagePath: String): Flow<List<MessageAttachmentDTO>> {
        if (messagePath.isBlank() || !messagePath.contains("/")) {
            return kotlinx.coroutines.flow.flow { throw IllegalArgumentException("Invalid message path.") }
        }
        return getAttachmentsCollection(messagePath).dataObjects()
    }

    override suspend fun addAttachment(
        messagePath: String,
        attachment: MessageAttachmentDTO
    ): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val docRef = getAttachmentsCollection(messagePath).add(attachment).await()
            docRef.id
        }
    }

    override suspend fun removeAttachment(
        messagePath: String,
        attachmentId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 참고: 이 작업은 Firestore의 문서만 삭제할 뿐,
            // Firebase Storage에 업로드된 실제 파일을 삭제하지는 않습니다.
            // 실제 파일 삭제는 별도의 로직이 필요합니다.
            getAttachmentsCollection(messagePath).document(attachmentId).delete().await()
            Unit
        }
    }
    
    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

