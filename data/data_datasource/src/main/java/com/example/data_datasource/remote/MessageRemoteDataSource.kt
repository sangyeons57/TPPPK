package com.example.data_datasource.remote

import android.util.Log
import com.example.core_common.constants.ChannelConstants
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.data_datasource.remote.special.DefaultDatasourceImpl
import com.example.data_model.remote.MessageDTO
import com.example.domain.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.vo.CollectionPath
import com.example.mapper.message.MessageMapper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CollectionPath에서 channelId를 추출하는 확장 함수
 */
fun CollectionPath.extractChannelId(): String? {
    Log.d("CollectionPath", "🔍 extractChannelId 호출: $value")

    return when {
        // DM 채널: dm_channels/{channelId}/messages
        value.contains("dm_channels/") -> {
            val parts = value.split("/")
            val channelIndex = parts.indexOf("dm_channels")
            if (channelIndex >= 0 && channelIndex + 1 < parts.size) {
                val channelId = parts[channelIndex + 1]
                Log.d("CollectionPath", "✅ channelId 추출 성공: $channelId")
                channelId
            } else {
                Log.e(
                    "CollectionPath",
                    "❌ channelId 추출 실패: 인덱스 범위 초과 (channelIndex=$channelIndex, parts.size=${parts.size})"
                )
                null
            }
        }

        // 프로젝트 채널: projects/{projectId}/project_channels/{channelId}/messages
        value.contains("project_channels/") -> {
            val parts = value.split("/")
            Log.d("CollectionPath", "🔍 프로젝트 채널 패턴 감지, parts: ${parts.toList()}")

            val channelIndex = parts.indexOf("project_channels")
            Log.d("CollectionPath", "🔍 project_channels 인덱스: $channelIndex")

            if (channelIndex >= 0 && channelIndex + 1 < parts.size) {
                val channelId = parts[channelIndex + 1]
                Log.d("CollectionPath", "✅ channelId 추출 성공: $channelId")
                channelId
            } else {
                Log.e(
                    "CollectionPath",
                    "❌ channelId 추출 실패: 인덱스 범위 초과 (channelIndex=$channelIndex, parts.size=${parts.size})"
                )
                null
            }
        }

        else -> {
            Log.e("CollectionPath", "❌ 지원하지 않는 경로 패턴: $value")
            null
        }
    }
}

/**
 * 메시지 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 채널의 메시지에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 메시지 데이터는 `/{channelPath}/messages/{messageId}` 경로에 저장되며, `messageId`는 자동 생성됩니다.
 * 모든 작업 전에 `setCollection(channelPath)`를 호출하여 채널 컨텍스트를 설정해야 합니다.
 * `channelPath`는 부모 채널 문서의 전체 경로입니다 (예: "dm_channels/channelId123" 또는 "projects/projectId123/channels/channelId456").
 */
interface MessageRemoteDataSource : DefaultDatasource<MessageDTO> {

    /* Outbox 배치를 서버로 업로드 */
    suspend fun push(events: List<OutBoxRecord>): PushResult

    /* updatedAt 커서 기반으로 증분 페이징 */
    suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<MessageDTO>


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
     * 특정 시점 이후 업데이트된 메시지들을 가져옴 (증분 동기화용)
     * updateAt > timestamp 조건으로 변경된 메시지만 효율적으로 가져옴
     * @param collectionPath 메시지 컬렉션 경로
     * @param timestamp 마지막 동기화 시간
     * @return 업데이트된 메시지 목록
     */
    suspend fun getMessagesAfterTimestamp(
        collectionPath: CollectionPath,
        timestamp: Instant
    ): CustomResult<List<Message>, Exception>

    /**
     * 채널의 최신 메시지들을 가져옴 (전체 동기화용)
     * @param collectionPath 메시지 컬렉션 경로
     * @param limit 가져올 메시지 개수
     * @return 최신 메시지 목록
     */
    suspend fun getRecentMessages(
        collectionPath: CollectionPath,
        limit: Int
    ): CustomResult<List<Message>, Exception>

    /**
     * 특정 시점 이전의 과거 메시지들을 가져옴 (페이지네이션용)
     * @param collectionPath 메시지 컬렉션 경로
     * @param beforeTimestamp 기준 시간
     * @param limit 가져올 메시지 개수
     * @return 과거 메시지 목록
     */
    suspend fun getMessagesBeforeTimestamp(
        collectionPath: CollectionPath,
        beforeTimestamp: Instant,
        limit: Int
    ): CustomResult<List<Message>, Exception>

}

@Singleton
open class MessageRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mapper: MessageMapper,
) : DefaultDatasourceImpl<MessageDTO>(firestore), MessageRemoteDataSource {
    override val dtoClass = MessageDTO::class.java

    protected var currentChannelPath: String? = null

    private fun parseCursor(cursor: String?): Pair<Long?, String?> {
        // Cursor format: "epochMillis:id"
        if (cursor.isNullOrEmpty()) return null to null
        val p = cursor.split(":", limit = 2)
        return p.getOrNull(0)?.toLongOrNull() to p.getOrNull(1)
    }

    override suspend fun push(events: List<OutBoxRecord>): PushResult {
        val success = mutableListOf<String>()
        val failed = mutableListOf<com.example.domain.model.sync.FailedEvent>()

        // Best-effort per event to avoid whole-batch failure
        for (e in events) {
            try {
                val payloadJson = JSONObject(e.payload)
                val id = e.aggregateId
                val channelIdRaw = payloadJson.optString(ChannelConstants.KEY_CHANNEL_ID)
                    .takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Missing channelId in payload")
                // Composite format support is handled by CollectionPath.messages(ChannelId(...))

                // Map payload to DTO (store leaf channelId)
                val channelIdLeaf = com.example.domain.vo.ChannelId(channelIdRaw).last()
                val dto = MessageDTO(
                    id = id,
                    channelId = channelIdLeaf,
                    senderId = payloadJson.optString("senderId", ""),
                    messageType = payloadJson.optString("messageType", "TEXT"),
                    payload = payloadJson.optString("payload", "{}"),
                    replyToMessageId = payloadJson.optString("replyToMessageId")
                        .takeIf { it.isNotEmpty() },
                    isDeleted = payloadJson.optBoolean("isDeleted", false),
                    mentions = emptyList(),
                    createdAt = payloadJson.optLong("createdAt").takeIf { it > 0 }
                        ?.let { java.util.Date(it) },
                    updatedAt = payloadJson.optLong("updatedAt").takeIf { it > 0 }
                        ?.let { java.util.Date(it) }
                )

                // Set the collection path from channel
                val path = CollectionPath.messages(com.example.domain.vo.ChannelId(channelIdRaw))
                setCollection(path)

                when (e.op) {
                    OutBoxRecord.Op.UPSERT -> {
                        collection.document(dto.id).set(dto).await()
                        success.add(e.id)
                    }

                    OutBoxRecord.Op.DELETE -> {
                        collection.document(dto.id).delete().await()
                        success.add(e.id)
                    }
                }
            } catch (ex: Exception) {
                failed.add(
                    com.example.domain.model.sync.FailedEvent(
                        id = e.id,
                        reason = ex.message ?: "Unknown",
                        retryAfterMillis = 5000
                    )
                )
            }
        }

        return PushResult(successIds = success, failIds = failed)
    }

    override suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<MessageDTO> {
        val (lastTs, lastId) = parseCursor(cursor)

        var q: Query = collection
            .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.ASCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
            .limit(limit.toLong())

        // Firestore expects a Date/Timestamp for Timestamp-typed fields
        if (lastTs != null && lastId != null) {
            q = q.startAfter(java.util.Date(lastTs), lastId)
        }

        val snap = q.get().await()
        val items = snap.toObjects(MessageDTO::class.java)

        val hasMore = items.size == limit
        val nextCursor = items.lastOrNull()?.let { dto ->
            val millis = dto.updatedAt?.time ?: return@let null
            "$millis:${dto.id}"
        }

        return RemoteBatch(
            items = items,
            tombstones = emptyList(),
            nextCursor = nextCursor,
            hasMore = hasMore,
            watermark = items.lastOrNull()?.updatedAt?.time
        )
    }

    private fun checkCollectionInitialized(methodName: String, expectedChannelPath: String? = null) {
        super.checkCollectionInitialized(methodName)
        if (expectedChannelPath != null && currentChannelPath != expectedChannelPath) {
            throw IllegalStateException(
                "Channel path mismatch for $methodName. Expected: $expectedChannelPath, Current: $currentChannelPath. Ensure setCollection() was called with the correct channel path."
            )
        }
    }

    override suspend fun sendMessage(channelPath: String, message: MessageDTO): CustomResult<MessageDTO, Exception> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 현재 사용자 ID 확인
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            Log.d(
                "MessageRemoteDataSource",
                "🔍 메시지 전송: senderId=${message.senderId}, currentUserId=$currentUserId"
            )

            // senderId가 현재 사용자 ID와 다르면 수정
            val correctedMessage = if (message.senderId != currentUserId) {
                Log.w(
                    "MessageRemoteDataSource",
                    "⚠️ senderId 불일치 수정: ${message.senderId} → $currentUserId"
                )
                message.copy(senderId = currentUserId ?: "")
            } else {
                Log.d("MessageRemoteDataSource", "✅ senderId 정상: ${message.senderId}")
                message
            }

            // 메시지를 Firestore에 저장
            val docRef = firestore.collection(channelPath).document()

            // 메시지 ID 설정 (Firestore 문서 ID 사용)
            val messageWithId = correctedMessage.copy(id = docRef.id)

            // Firestore에 저장
            docRef.set(messageWithId).await()

            Log.d(
                "MessageRemoteDataSource",
                "Successfully sent message to Firestore: ${messageWithId.id}"
            )
            CustomResult.Success(messageWithId)
        } catch (e: Exception) {
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    Log.e("MessageRemoteDataSource", "❌ 권한 없음: 메시지 전송 실패", e)
                    CustomResult.Failure(Exception("메시지 전송 권한이 없습니다"))
                }

                e.message?.contains("UNAUTHENTICATED") == true -> {
                    Log.e("MessageRemoteDataSource", "❌ 인증 실패: 재로그인 필요", e)
                    CustomResult.Failure(Exception("재로그인이 필요합니다"))
                }

                else -> {
                    Log.e("MessageRemoteDataSource", "❌ 기타 오류: 메시지 전송 실패", e)
                    CustomResult.Failure(e)
                }
            }
        }
    }

    override suspend fun getMessagesAfterTimestamp(
        collectionPath: CollectionPath,
        timestamp: Instant
    ): CustomResult<List<Message>, Exception> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d(
                    "MessageRemoteDataSource",
                    "🔄 Getting messages after timestamp from: ${collectionPath.value}"
                )

                // updateAt > timestamp 조건으로 증분 동기화
                val query = firestore.collection(collectionPath.value)
                    .whereGreaterThan(
                        AggregateRoot.KEY_UPDATED_AT,
                        Timestamp(timestamp.epochSecond, timestamp.nano)
                    )
                    .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.ASCENDING)
                    .limit(100) // 한 번에 최대 100개씩 동기화

                val snapshot = query.get().await()
                val messageDTOs = snapshot.toObjects(MessageDTO::class.java)

                // CollectionPath에서 channelId 추출하여 메시지에 할당
                val channelId = collectionPath.extractChannelId()
                Log.d(
                    "MessageRemoteDataSource",
                    "🔍 추출된 channelId: $channelId (경로: ${collectionPath.value})"
                )
                
                val messages = messageDTOs.map { dto ->
                    // DTO에 channelId가 없으면 경로에서 추출한 것으로 보정
                    val dtoWithChannelId = if (dto.channelId.isBlank() && channelId != null) {
                        Log.d(
                            "MessageRemoteDataSource",
                            "🔧 channelId 보정: 빈 값 → $channelId (메시지 ID: ${dto.id})"
                        )
                        dto.copy(channelId = channelId)
                    } else if (dto.channelId.isBlank() && channelId == null) {
                        Log.e(
                            "MessageRemoteDataSource",
                            "⚠️ channelId 추출 실패 및 DTO에도 없음: 메시지 ID=${dto.id}, 경로=${collectionPath.value}"
                        )
                        dto
                    } else {
                        Log.d(
                            "MessageRemoteDataSource",
                            "✅ channelId 정상: ${dto.channelId} (메시지 ID: ${dto.id})"
                        )
                        dto
                    }
                    mapper.dtoToDomain(dtoWithChannelId)
                }

                Log.d(
                    "MessageRemoteDataSource",
                    "✅ Retrieved ${messages.size} messages after timestamp"
                )
                CustomResult.Success(messages)
            } catch (e: Exception) {
                Log.e("MessageRemoteDataSource", "❌ Failed to get messages after timestamp", e)
                CustomResult.Failure(e)
            }
        }

    override suspend fun getRecentMessages(
        collectionPath: CollectionPath,
        limit: Int
    ): CustomResult<List<Message>, Exception> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d("MessageRemoteDataSource", "🔥 Firestore에서 최근 메시지 로딩 시작")
                Log.d(
                    "MessageRemoteDataSource",
                    "📋 요청 정보: collectionPath=${collectionPath.value}, limit=$limit"
                )

                // 최신 메시지들을 생성시간 기준으로 가져오기
                val query = firestore.collection(collectionPath.value)
                    .orderBy(AggregateRoot.KEY_CREATED_AT, Query.Direction.DESCENDING)
                    .limit(limit.toLong())

                Log.d("MessageRemoteDataSource", "🔍 Firestore 쿼리 실행 중...")
                val snapshot = query.get().await()
                Log.d("MessageRemoteDataSource", "✅ Firestore 쿼리 성공: ${snapshot.size()}개 문서")

                if (snapshot.isEmpty) {
                    Log.d("MessageRemoteDataSource", "📭 해당 컬렉션에 메시지가 없습니다")
                    return@withContext CustomResult.Success(emptyList())
                }

                val messageDTOs = snapshot.toObjects(MessageDTO::class.java)
                Log.d("MessageRemoteDataSource", "📦 MessageDTO 변환 완료: ${messageDTOs.size}개")

                // CollectionPath에서 channelId 추출하여 메시지에 할당
                val channelId = collectionPath.extractChannelId()
                Log.d(
                    "MessageRemoteDataSource",
                    "🔍 추출된 channelId: $channelId (경로: ${collectionPath.value})"
                )

                val messagesWithChannelId = messageDTOs.map { dto ->
                    if (dto.channelId.isBlank() && channelId != null) {
                        Log.d(
                            "MessageRemoteDataSource",
                            "🔧 channelId 보정: 빈 값 → $channelId (메시지 ID: ${dto.id})"
                        )
                        dto.copy(channelId = channelId)
                    } else if (dto.channelId.isBlank() && channelId == null) {
                        Log.e(
                            "MessageRemoteDataSource",
                            "⚠️ channelId 추출 실패 및 DTO에도 없음: 메시지 ID=${dto.id}, 경로=${collectionPath.value}"
                        )
                        dto
                    } else {
                        Log.d(
                            "MessageRemoteDataSource",
                            "✅ channelId 정상: ${dto.channelId} (메시지 ID: ${dto.id})"
                        )
                        dto
                    }
                }
                Log.d(
                    "MessageRemoteDataSource",
                    "🔧 channelId 보정 완료: ${messagesWithChannelId.size}개"
                )

                val messages = messagesWithChannelId.map { dto ->
                    try {
                        val message = mapper.dtoToDomain(dto)
                        Log.d(
                            "MessageRemoteDataSource",
                            "✅ 도메인 변환 성공: id=${message.id.value}, channelId=${message.channelId.value}"
                        )
                        message
                    } catch (e: Exception) {
                        Log.e(
                            "MessageRemoteDataSource",
                            "❌ 도메인 변환 실패: id=${dto.id}, channelId=${dto.channelId}",
                            e
                        )
                        null
                    }
                }.filterNotNull()

                Log.d("MessageRemoteDataSource", "🎉 최종 변환 완료: ${messages.size}개 메시지")
                CustomResult.Success(messages)
            } catch (e: Exception) {
                Log.e(
                    "MessageRemoteDataSource",
                    "💥 Firestore에서 메시지 로딩 실패: collectionPath=${collectionPath.value}",
                    e
                )
                CustomResult.Failure(e)
            }
        }

    override suspend fun getMessagesBeforeTimestamp(
        collectionPath: CollectionPath,
        beforeTimestamp: Instant,
        limit: Int
    ): CustomResult<List<Message>, Exception> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d("MessageRemoteDataSource", "🔥 Firestore에서 과거 메시지 로딩 시작")
                Log.d(
                    "MessageRemoteDataSource",
                    "📋 요청 정보: collectionPath=${collectionPath.value}, beforeTimestamp=$beforeTimestamp, limit=$limit"
                )

                // 특정 시점 이전의 과거 메시지들 가져오기 (페이지네이션)
                val query = firestore.collection(collectionPath.value)
                    .whereLessThan(
                        AggregateRoot.KEY_CREATED_AT,
                        Timestamp(
                            beforeTimestamp.epochSecond,
                            beforeTimestamp.nano
                        )
                    )
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())

                Log.d("MessageRemoteDataSource", "🔍 Firestore 쿼리 실행 중...")
                val snapshot = query.get().await()
                Log.d("MessageRemoteDataSource", "✅ Firestore 쿼리 성공: ${snapshot.size()}개 문서")

                if (snapshot.isEmpty) {
                    Log.d("MessageRemoteDataSource", "📭 해당 시점 이전에 메시지가 없습니다")
                    return@withContext CustomResult.Success(emptyList())
                }

                val messageDTOs = snapshot.toObjects(MessageDTO::class.java)
                Log.d("MessageRemoteDataSource", "📦 MessageDTO 변환 완료: ${messageDTOs.size}개")

                // CollectionPath에서 channelId 추출하여 메시지에 할당
                val channelId = collectionPath.extractChannelId()

                val messagesWithChannelId = messageDTOs.map { dto ->
                    if (dto.channelId.isBlank() && channelId != null) {
                        Log.w(
                            "MessageRemoteDataSource",
                            "⚠️ channelId가 비어있음. 보정: $channelId, id=${dto.id}"
                        )
                        dto.copy(channelId = channelId)
                    } else {
                        Log.d(
                            "MessageRemoteDataSource",
                            "✅ channelId 정상: ${dto.channelId}, id=${dto.id}"
                        )
                        dto
                    }
                }
                Log.d(
                    "MessageRemoteDataSource",
                    "🔧 channelId 보정 완료: ${messagesWithChannelId.size}개"
                )

                val messages = messagesWithChannelId.map { dto ->
                    try {
                        val message = mapper.dtoToDomain(dto)
                        Log.d(
                            "MessageRemoteDataSource",
                            "✅ 도메인 변환 성공: id=${message.id.value}, channelId=${message.channelId.value}"
                        )
                        message
                    } catch (e: Exception) {
                        Log.e(
                            "MessageRemoteDataSource",
                            "❌ 도메인 변환 실패: id=${dto.id}, channelId=${dto.channelId}",
                            e
                        )
                        null
                    }
                }.filterNotNull()

                Log.d("MessageRemoteDataSource", "🎉 최종 변환 완료: ${messages.size}개 메시지")
                CustomResult.Success(messages)
            } catch (e: Exception) {
                Log.e(
                    "MessageRemoteDataSource",
                    "💥 Firestore에서 과거 메시지 로딩 실패: collectionPath=${collectionPath.value}",
                    e
                )
                CustomResult.Failure(e)
            }
        }


}
