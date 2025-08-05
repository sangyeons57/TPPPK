package com.example.data_datasource.remote

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.data_datasource.remote.special.DefaultDatasourceImpl
import com.example.data_datasource.remote.util.ChannelIdExtractor
import com.example.data_model.remote.MessageDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.vo.CollectionPath
import com.example.mapper.message.MessageMapper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

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
     * @param channelId 채널 ID
     * @param timestamp 마지막 동기화 시간
     * @return 업데이트된 메시지 목록
     */
    suspend fun getMessagesAfterTimestamp(
        channelId: String,
        timestamp: Instant
    ): CustomResult<List<Message>, Exception>

    /**
     * 채널의 최신 메시지들을 가져옴 (전체 동기화용)
     * @param channelId 채널 ID
     * @param limit 가져올 메시지 개수
     * @return 최신 메시지 목록
     */
    suspend fun getRecentMessages(
        channelId: String,
        limit: Int
    ): CustomResult<List<Message>, Exception>

    /**
     * 특정 시점 이전의 과거 메시지들을 가져옴 (페이지네이션용)
     * @param channelId 채널 ID
     * @param beforeTimestamp 기준 시간
     * @param limit 가져올 메시지 개수
     * @return 과거 메시지 목록
     */
    suspend fun getMessagesBeforeTimestamp(
        channelId: String,
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
        if (cursor.isNullOrEmpty()) return null to null
        val p = cursor.split(":")
        return p.getOrNull(0)?.toLongOrNull() to p.getOrNull(1)
    }

    override suspend fun push(events: List<OutBoxRecord>): PushResult {
        return PushResult(successIds = events.map { it.id }, failIds = emptyList())
    }

    override suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<MessageDTO> {
        val (lastTs, lastId) = parseCursor(cursor)

        var q: Query = collection
            .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.ASCENDING)
            .orderBy(AggregateRoot.KEY_ID, Query.Direction.ASCENDING)
            .limit(limit.toLong())

        if (lastTs != null && lastId != null) {
            q = q.startAfter(lastTs, lastId)
        }

        val snap = q.get().await()
        val items = snap.toObjects(MessageDTO::class.java)

        val hasMore = items.size == limit
        val nextCursor = items.lastOrNull()?.let { "${it.updatedAt}:${it.id}" }

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
            // 메시지를 Firestore에 저장
            val docRef = firestore.collection(channelPath).document()

            // 메시지 ID 설정 (Firestore 문서 ID 사용)
            val messageWithId = message.copy(id = docRef.id)

            // Firestore에 저장
            docRef.set(messageWithId).await()

            Log.d(
                "MessageRemoteDataSource",
                "Successfully sent message to Firestore: ${messageWithId.id}"
            )
            CustomResult.Success(messageWithId)
        } catch (e: Exception) {
            Log.e("MessageRemoteDataSource", "Failed to send message to Firestore", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getMessagesAfterTimestamp(
        channelId: String,
        timestamp: Instant
    ): CustomResult<List<Message>, Exception> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                // CollectionPath를 사용하여 적절한 메시지 컬렉션 경로 결정
                val messagesCollectionPath = getMessagesCollectionPath(channelId)

                // updateAt > timestamp 조건으로 증분 동기화
                val query = firestore.collection(messagesCollectionPath.value)
                    .whereGreaterThan(
                        AggregateRoot.KEY_UPDATED_AT,
                        Timestamp(timestamp.epochSecond, timestamp.nano)
                    )
                    .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.ASCENDING)
                    .limit(100) // 한 번에 최대 100개씩 동기화

                val snapshot = query.get().await()
                val messageDTOs = snapshot.toObjects(MessageDTO::class.java)

                // ✅ Firestore에서 온 메시지들에 channelId 할당
                val messages = messageDTOs.map { dto ->
                    // DTO에 이미 channelId가 있으면 그대로 사용
                    if (dto.channelId.isNotEmpty()) {
                        mapper.dtoToDomain(dto)
                    } else {
                        // channelId가 없으면 경로에서 추출
                        val extractedChannelId =
                            ChannelIdExtractor.extractChannelIdFromCollectionPath(
                                messagesCollectionPath.value
                            )

                        if (extractedChannelId != null) {
                            // channelId가 있는 DTO로 변환
                            val dtoWithChannelId = dto.copy(channelId = extractedChannelId)
                            mapper.dtoToDomain(dtoWithChannelId)
                        } else {
                            // 추출 실패 시 원본 DTO 사용 (기본값은 빈 문자열)
                            mapper.dtoToDomain(dto)
                        }
                    }
                }

                CustomResult.Success(messages)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }

    override suspend fun getRecentMessages(
        channelId: String,
        limit: Int
    ): CustomResult<List<Message>, Exception> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d("MessageRemoteDataSource", "🔥 Firestore에서 최근 메시지 로딩 시작")
                Log.d("MessageRemoteDataSource", "📋 요청 정보: channelId=$channelId, limit=$limit")
                
                // CollectionPath를 사용하여 적절한 메시지 컬렉션 경로 결정
                val messagesCollectionPath = getMessagesCollectionPath(channelId)
                Log.d("MessageRemoteDataSource", "📍 Firestore 경로: ${messagesCollectionPath.value}")

                // 최신 메시지들을 생성시간 기준으로 가져오기
                val query = firestore.collection(messagesCollectionPath.value)
                    .orderBy(AggregateRoot.KEY_CREATED_AT, Query.Direction.DESCENDING)
                    .limit(limit.toLong())

                Log.d("MessageRemoteDataSource", "🔍 Firestore 쿼리 실행 중...")
                val snapshot = query.get().await()
                Log.d("MessageRemoteDataSource", "✅ Firestore 쿼리 성공: ${snapshot.size()}개 문서")

                if (snapshot.isEmpty) {
                    Log.d("MessageRemoteDataSource", "📭 해당 채널에 메시지가 없습니다")
                    return@withContext CustomResult.Success(emptyList())
                }

                val messageDTOs = snapshot.toObjects(MessageDTO::class.java)
                Log.d("MessageRemoteDataSource", "📦 MessageDTO 변환 완료: ${messageDTOs.size}개")

                // Firestore에서 온 메시지들에 channelId 할당
                val messagesWithChannelId = messageDTOs.map { dto ->
                    if (dto.channelId.isBlank()) {
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
                Log.e("MessageRemoteDataSource", "💥 Firestore에서 메시지 로딩 실패: channelId=$channelId", e)
                CustomResult.Failure(e)
            }
        }

    override suspend fun getMessagesBeforeTimestamp(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int
    ): CustomResult<List<Message>, Exception> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d("MessageRemoteDataSource", "🔥 Firestore에서 과거 메시지 로딩 시작")
                Log.d(
                    "MessageRemoteDataSource",
                    "📋 요청 정보: channelId=$channelId, beforeTimestamp=$beforeTimestamp, limit=$limit"
                )
                
                // CollectionPath를 사용하여 적절한 메시지 컬렉션 경로 결정
                val messagesCollectionPath = getMessagesCollectionPath(channelId)
                Log.d("MessageRemoteDataSource", "📍 Firestore 경로: ${messagesCollectionPath.value}")

                // 특정 시점 이전의 과거 메시지들 가져오기 (페이지네이션)
                val query = firestore.collection(messagesCollectionPath.value)
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

                // Firestore에서 온 메시지들에 channelId 할당
                val messagesWithChannelId = messageDTOs.map { dto ->
                    if (dto.channelId.isBlank()) {
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
                    "💥 Firestore에서 과거 메시지 로딩 실패: channelId=$channelId",
                    e
                )
                CustomResult.Failure(e)
            }
        }

    /**
     * 채널 ID를 기반으로 적절한 메시지 컬렉션 경로를 결정
     * CollectionPath 헬퍼를 사용하여 DM 채널과 프로젝트 채널을 구분
     */
    private fun getMessagesCollectionPath(channelId: String): CollectionPath {
        // 이미 전체 경로인 경우 그대로 사용 (예: /dm_channels/channelId 또는 dm_channels/channelId/messages)
        return when {
            // Case 1: 이미 완전한 Firestore 경로인 경우 (예: "dm_channels/channelId/messages")
            channelId.contains("/messages") -> {
                Log.d("MessageRemoteDataSource", "📍 Using provided full path: $channelId")
                CollectionPath(channelId)
            }

            // Case 2: 이미 채널 경로이지만 /messages가 없는 경우 (예: "/dm_channels/channelId")
            channelId.startsWith("/dm_channels/") -> {
                val cleanChannelId = channelId.removePrefix("/")
                Log.d(
                    "MessageRemoteDataSource",
                    "📍 Converting path to collection: $cleanChannelId/messages"
                )
                CollectionPath("$cleanChannelId/messages")
            }

            // Case 3: 채널 경로이지만 /messages가 없는 경우 (예: "dm_channels/channelId")
            channelId.startsWith("dm_channels/") -> {
                Log.d(
                    "MessageRemoteDataSource",
                    "📍 Adding messages to DM channel path: $channelId/messages"
                )
                CollectionPath("$channelId/messages")
            }

            // Case 4: 단순 DM 채널 ID (예: "dm_userId1_userId2")
            channelId.startsWith("dm_") -> {
                Log.d("MessageRemoteDataSource", "📍 Creating DM channel path for: $channelId")
                CollectionPath.dmChannelMessages(channelId)
            }

            // Case 5: 프로젝트 채널 (예: "channel_project_projectId")
            else -> {
                // 프로젝트 채널: projects/{projectId}/channels/{channelId}/messages
                val projectId = extractProjectIdFromContext(channelId)
                if (projectId != null) {
                    Log.d(
                        "MessageRemoteDataSource",
                        "📍 Creating project channel path for: projectId=$projectId, channelId=$channelId"
                    )
                    CollectionPath.projectChannelMessages(projectId, channelId)
                } else {
                    // 프로젝트 ID를 찾을 수 없는 경우 DM 경로로 폴백 (임시)
                    Log.w(
                        "MessageRemoteDataSource",
                        "Could not extract projectId for channel: $channelId, using DM path as fallback"
                    )
                    CollectionPath.dmChannelMessages(channelId)
                }
            }
        }
    }

    /**
     * 채널 ID 또는 현재 컨텍스트에서 프로젝트 ID를 추출
     * 향후 더 나은 방법으로 개선 필요 (현재는 간단한 폴백 로직)
     */
    private fun extractProjectIdFromContext(channelId: String): String? {
        return try {
            // TODO: 현재는 간단한 폴백을 사용
            // 실제로는 채널 문서에서 projectId를 조회하거나,
            // 별도의 context 매개변수를 통해 전달받아야 함

            // 임시로 채널 ID가 프로젝트 패턴을 포함하는지 확인
            if (channelId.contains("_project_")) {
                // 예: "channel123_project_projectId456" 같은 패턴
                val parts = channelId.split("_project_")
                if (parts.size >= 2) {
                    return parts[1]
                }
            }

            // 다른 패턴이나 기본값 처리
            null
        } catch (e: Exception) {
            Log.w(
                "MessageRemoteDataSource",
                "Failed to extract projectId from context for channel: $channelId",
                e
            )
            null
        }
    }

}
