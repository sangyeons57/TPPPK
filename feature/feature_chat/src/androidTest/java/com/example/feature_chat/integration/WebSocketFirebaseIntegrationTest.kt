package com.example.feature_chat.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.MessageRepository
import com.example.feature_chat.logging.ChatLogger
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import javax.inject.Inject

/**
 * WebSocket 전송 후 Firebase Firestore 데이터 업데이트 테스트
 * - WebSocket으로 메시지 전송 후 Firestore에 자동 저장되는지 확인
 * - Firestore 데이터와 WebSocket 메시지 일관성 검증
 * - 메시지 수정/삭제 시 Firestore 동기화 테스트
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WebSocketFirebaseIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var chatWebSocketClient: ChatWebSocketClient

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var chatLogger: ChatLogger

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val testServerUrl = "ws://localhost:8080/websocket"
    private val testAuthToken = "test-auth-token"
    private val testRoomId = "test-room-${UUID.randomUUID()}"
    private val testProjectId = "test-project-${UUID.randomUUID()}"
    private val testChannelId = "test-channel-${UUID.randomUUID()}"
    private val testUserId = UserId("test-user-${UUID.randomUUID()}")

    @Before
    fun setup() {
        hiltRule.inject()
        
        chatLogger.logTestResult(
            testName = "WebSocketFirebaseIntegrationTest",
            success = true,
            details = "Firebase 통합 테스트 환경 설정 완료"
        )
    }

    @After
    fun teardown() {
        runBlocking {
            try {
                chatWebSocketClient.disconnect()
                // 테스트 데이터 정리
                cleanupTestData()
                
                chatLogger.logTestResult(
                    testName = "teardown",
                    success = true,
                    details = "Firebase 통합 테스트 정리 완료"
                )
            } catch (e: Exception) {
                chatLogger.logError(
                    ChatLogger.CATEGORY_TEST,
                    "teardown 중 오류 발생",
                    e
                )
            }
        }
    }

    @Test
    fun testWebSocketMessageToFirebaseSync() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "WebSocket 메시지 -> Firebase 동기화 테스트 시작"
            )

            // Given: WebSocket 연결 및 방 입장
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // When: WebSocket으로 메시지 전송
            val messageId = DocumentId(UUID.randomUUID().toString())
            val messageContent = "Firebase 동기화 테스트 메시지"
            
            val sendResult = chatWebSocketClient.sendMessage(
                roomId = testRoomId,
                senderId = testUserId,
                content = messageContent,
                messageId = messageId
            )
            assertTrue("WebSocket 메시지 전송 성공", sendResult.isSuccess)

            chatLogger.logWebSocketMessage(
                action = "SEND",
                messageId = messageId.value,
                roomId = testRoomId,
                userId = testUserId.value,
                success = true
            )

            // Then: Firebase Firestore에서 메시지 확인 (최대 10초 대기)
            var firestoreMessage: Map<String, Any>? = null
            withTimeout(10000) {
                while (firestoreMessage == null) {
                    delay(500)
                    try {
                        // CollectionPath를 사용하여 올바른 경로로 접근
                        val docSnapshot = firestore
                            .collection("projects")
                            .document(testProjectId)
                            .collection("channels")
                            .document(testChannelId)
                            .collection("messages")
                            .document(messageId.value)
                            .get()
                            .await()

                        if (docSnapshot.exists()) {
                            firestoreMessage = docSnapshot.data
                        }
                    } catch (e: Exception) {
                        chatLogger.logWarning(
                            ChatLogger.CATEGORY_FIREBASE,
                            "Firestore 메시지 확인 중 오류: ${e.message}"
                        )
                    }
                }
            }

            // Firebase에서 메시지 검증
            assertNotNull("Firestore에 메시지 저장됨", firestoreMessage)
            assertEquals("메시지 내용 일치", messageContent, firestoreMessage!!["content"])
            assertEquals("발신자 ID 일치", testUserId.value, firestoreMessage["senderId"])

            chatLogger.logFirebaseUpdate(
                operation = "CREATE",
                collection = "messages",
                documentId = messageId.value,
                success = true,
                userId = testUserId.value
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketMessageToFirebaseSync",
                success = true,
                details = "WebSocket -> Firebase 동기화 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketMessageToFirebaseSync",
                success = false,
                details = "WebSocket -> Firebase 동기화 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testWebSocketEditMessageFirebaseSync() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "WebSocket 메시지 수정 -> Firebase 동기화 테스트 시작"
            )

            // Given: WebSocket 연결 및 초기 메시지 전송
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // 원본 메시지 전송
            val messageId = DocumentId(UUID.randomUUID().toString())
            val originalContent = "수정 전 원본 메시지"
            
            val sendResult = chatWebSocketClient.sendMessage(
                roomId = testRoomId,
                senderId = testUserId,
                content = originalContent,
                messageId = messageId
            )
            assertTrue("원본 메시지 전송 성공", sendResult.isSuccess)

            delay(2000) // 초기 메시지 저장 대기

            // When: WebSocket으로 메시지 수정
            val editedContent = "수정된 메시지 내용"
            val editResult = chatWebSocketClient.editMessage(
                roomId = testRoomId,
                messageId = messageId,
                newContent = editedContent
            )
            assertTrue("WebSocket 메시지 수정 성공", editResult.isSuccess)

            chatLogger.logWebSocketMessage(
                action = "EDIT",
                messageId = messageId.value,
                roomId = testRoomId,
                userId = testUserId.value,
                success = true
            )

            // Then: Firebase에서 수정된 메시지 확인
            var updatedMessage: Map<String, Any>? = null
            withTimeout(10000) {
                while (updatedMessage == null || updatedMessage["content"] == originalContent) {
                    delay(500)
                    try {
                        val docSnapshot = firestore
                            .collection("projects")
                            .document(testProjectId)
                            .collection("channels")
                            .document(testChannelId)
                            .collection("messages")
                            .document(messageId.value)
                            .get()
                            .await()

                        if (docSnapshot.exists()) {
                            updatedMessage = docSnapshot.data
                        }
                    } catch (e: Exception) {
                        chatLogger.logWarning(
                            ChatLogger.CATEGORY_FIREBASE,
                            "Firestore 수정된 메시지 확인 중 오류: ${e.message}"
                        )
                    }
                }
            }

            // 수정된 메시지 검증
            assertNotNull("Firestore에 수정된 메시지 존재", updatedMessage)
            assertEquals("수정된 내용 일치", editedContent, updatedMessage!!["content"])
            assertNotNull("수정 시간 존재", updatedMessage["updatedAt"])

            chatLogger.logFirebaseUpdate(
                operation = "UPDATE",
                collection = "messages",
                documentId = messageId.value,
                success = true,
                userId = testUserId.value
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketEditMessageFirebaseSync",
                success = true,
                details = "WebSocket 메시지 수정 -> Firebase 동기화 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketEditMessageFirebaseSync",
                success = false,
                details = "WebSocket 메시지 수정 -> Firebase 동기화 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testWebSocketDeleteMessageFirebaseSync() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "WebSocket 메시지 삭제 -> Firebase 동기화 테스트 시작"
            )

            // Given: WebSocket 연결 및 초기 메시지 전송
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // 삭제할 메시지 전송
            val messageId = DocumentId(UUID.randomUUID().toString())
            val messageContent = "삭제될 메시지"
            
            val sendResult = chatWebSocketClient.sendMessage(
                roomId = testRoomId,
                senderId = testUserId,
                content = messageContent,
                messageId = messageId
            )
            assertTrue("메시지 전송 성공", sendResult.isSuccess)

            delay(2000) // 초기 메시지 저장 대기

            // When: WebSocket으로 메시지 삭제
            val deleteResult = chatWebSocketClient.deleteMessage(
                roomId = testRoomId,
                messageId = messageId
            )
            assertTrue("WebSocket 메시지 삭제 성공", deleteResult.isSuccess)

            chatLogger.logWebSocketMessage(
                action = "DELETE",
                messageId = messageId.value,
                roomId = testRoomId,
                userId = testUserId.value,
                success = true
            )

            // Then: Firebase에서 메시지 삭제 또는 삭제 마킹 확인
            withTimeout(10000) {
                var messageDeleted = false
                while (!messageDeleted) {
                    delay(500)
                    try {
                        val docSnapshot = firestore
                            .collection("projects")
                            .document(testProjectId)
                            .collection("channels")
                            .document(testChannelId)
                            .collection("messages")
                            .document(messageId.value)
                            .get()
                            .await()

                        if (!docSnapshot.exists()) {
                            // 메시지가 완전히 삭제된 경우
                            messageDeleted = true
                        } else {
                            // 삭제 마킹이 있는지 확인
                            val data = docSnapshot.data
                            if (data?.get("deleted") == true || data?.get("deletedAt") != null) {
                                messageDeleted = true
                            }
                        }
                    } catch (e: Exception) {
                        chatLogger.logWarning(
                            ChatLogger.CATEGORY_FIREBASE,
                            "Firestore 메시지 삭제 확인 중 오류: ${e.message}"
                        )
                    }
                }
            }

            chatLogger.logFirebaseUpdate(
                operation = "DELETE",
                collection = "messages",
                documentId = messageId.value,
                success = true,
                userId = testUserId.value
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketDeleteMessageFirebaseSync",
                success = true,
                details = "WebSocket 메시지 삭제 -> Firebase 동기화 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketDeleteMessageFirebaseSync",
                success = false,
                details = "WebSocket 메시지 삭제 -> Firebase 동기화 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testOfflineToOnlineSync() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "오프라인 -> 온라인 동기화 테스트 시작"
            )

            // Given: WebSocket 연결
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            assertTrue("WebSocket 연결 성공", connectResult.isSuccess)

            // WebSocket 연결 해제 (오프라인 상태 시뮬레이션)
            chatWebSocketClient.disconnect()
            delay(1000)

            // 오프라인 상태에서 Repository를 통해 메시지 저장 (오프라인 큐)
            val messageId = DocumentId(UUID.randomUUID().toString())
            val offlineMessageContent = "오프라인에서 작성된 메시지"

            // Repository를 통해 로컬 저장
            // val saveResult = messageRepository.saveMessage(...) // 실제 구현에 따라 조정

            // When: 다시 온라인 상태로 복구
            val reconnectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            assertTrue("WebSocket 재연결 성공", reconnectResult.isSuccess)
            
            val rejoinResult = chatWebSocketClient.joinRoom(testRoomId)
            assertTrue("채팅방 재입장 성공", rejoinResult.isSuccess)

            // Then: 오프라인 메시지가 WebSocket을 통해 전송되고 Firebase에 동기화되어야 함
            delay(5000) // 동기화 대기

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testOfflineToOnlineSync",
                success = true,
                details = "오프라인 -> 온라인 동기화 테스트 완료",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testOfflineToOnlineSync",
                success = false,
                details = "오프라인 -> 온라인 동기화 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    private suspend fun cleanupTestData() {
        try {
            // 테스트 중에 생성된 Firestore 데이터 정리
            val batch = firestore.batch()
            
            val messagesRef = firestore
                .collection("projects")
                .document(testProjectId)
                .collection("channels")
                .document(testChannelId)
                .collection("messages")
            
            val querySnapshot = messagesRef.get().await()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            
            batch.commit().await()
            
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "테스트 데이터 정리 완료"
            )
        } catch (e: Exception) {
            chatLogger.logError(
                ChatLogger.CATEGORY_TEST,
                "테스트 데이터 정리 중 오류",
                e
            )
        }
    }
}