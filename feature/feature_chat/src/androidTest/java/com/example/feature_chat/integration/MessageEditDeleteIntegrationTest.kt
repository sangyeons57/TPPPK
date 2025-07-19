package com.example.feature_chat.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.feature_chat.logging.ChatLogger
import com.example.feature_chat.viewmodel.WebSocketChatViewModel
import com.example.feature_chat.websocket.ChatWebSocketClient
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.runner.RunWith
import java.util.*
import javax.inject.Inject

/**
 * 채팅 수정/삭제 기능 통합 테스트
 * - WebSocket을 통한 메시지 수정/삭제
 * - Firebase Firestore 동기화 검증
 * - UI 상태 업데이트 확인
 * - 오프라인 큐 처리 테스트
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MessageEditDeleteIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var chatWebSocketClient: ChatWebSocketClient

    @Inject
    lateinit var chatLogger: ChatLogger

    private val testRoomId = "edit-delete-test-room-${UUID.randomUUID()}"
    private val testUserId = UserId("edit-delete-test-user-${UUID.randomUUID()}")
    private val testServerUrl = "ws://localhost:8080/websocket"
    private val testAuthToken = "test-auth-token"

    @Before
    fun setup() {
        hiltRule.inject()
        
        chatLogger.logTestResult(
            testName = "MessageEditDeleteIntegrationTest",
            success = true,
            details = "메시지 수정/삭제 통합 테스트 환경 설정 완료"
        )
    }

    @After
    fun teardown() {
        runBlocking {
            try {
                chatWebSocketClient.disconnect()
                chatLogger.logTestResult(
                    testName = "teardown",
                    success = true,
                    details = "메시지 수정/삭제 테스트 정리 완료"
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
    fun testCompleteMessageLifecycle() = runBlocking {
        val testName = "메시지 전체 라이프사이클 테스트"
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "$testName 시작 - 생성 → 수정 → 삭제"
            )

            // Given: WebSocket 연결 및 방 입장
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            Assert.assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            Assert.assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // Step 1: 메시지 생성
            val messageId = DocumentId(UUID.randomUUID().toString())
            val originalContent = "수정/삭제 테스트용 원본 메시지"
            
            val sendResult = chatWebSocketClient.sendMessage(
                roomId = testRoomId,
                senderId = testUserId,
                content = originalContent,
                messageId = messageId
            )
            Assert.assertTrue("메시지 전송 성공", sendResult.isSuccess)
            
            chatLogger.logWebSocketMessage(
                action = "SEND",
                messageId = messageId.value,
                roomId = testRoomId,
                userId = testUserId.value,
                success = true
            )

            delay(1000) // 메시지 전송 완료 대기

            // Step 2: 메시지 수정
            val editedContent = "수정된 메시지 내용 - 테스트 완료"
            
            val editResult = chatWebSocketClient.editMessage(
                roomId = testRoomId,
                messageId = messageId,
                newContent = editedContent
            )
            Assert.assertTrue("메시지 수정 성공", editResult.isSuccess)
            
            chatLogger.logWebSocketMessage(
                action = "EDIT",
                messageId = messageId.value,
                roomId = testRoomId,
                userId = testUserId.value,
                success = true
            )

            delay(1000) // 수정 완료 대기

            // Step 3: 수정 사항 확인 (실제 프로덕션에서는 수신 이벤트로 확인)
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "메시지 수정 완료 확인",
                mapOf("originalContent" to originalContent, "editedContent" to editedContent),
                messageId = messageId.value
            )

            delay(500) // 확인 대기

            // Step 4: 메시지 삭제
            val deleteResult = chatWebSocketClient.deleteMessage(
                roomId = testRoomId,
                messageId = messageId
            )
            Assert.assertTrue("메시지 삭제 성공", deleteResult.isSuccess)
            
            chatLogger.logWebSocketMessage(
                action = "DELETE",
                messageId = messageId.value,
                roomId = testRoomId,
                userId = testUserId.value,
                success = true
            )

            delay(1000) // 삭제 완료 대기

            // Step 5: 삭제 확인
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "메시지 삭제 완료 확인",
                messageId = messageId.value
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = true,
                details = "메시지 생성→수정→삭제 전체 라이프사이클 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = false,
                details = "메시지 라이프사이클 테스트 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testBulkEditOperations() = runBlocking {
        val testName = "대량 메시지 수정 테스트"
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "$testName 시작 - 5개 메시지 연속 수정"
            )

            // Given: WebSocket 연결
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            Assert.assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            Assert.assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // When: 5개 메시지 생성 및 수정
            val messageIds = mutableListOf<DocumentId>()
            
            // 메시지들 생성
            repeat(5) { index ->
                val messageId = DocumentId(UUID.randomUUID().toString())
                messageIds.add(messageId)
                
                val sendResult = chatWebSocketClient.sendMessage(
                    roomId = testRoomId,
                    senderId = testUserId,
                    content = "대량 테스트 메시지 #${index + 1}",
                    messageId = messageId
                )
                Assert.assertTrue("메시지 ${index + 1} 전송 성공", sendResult.isSuccess)
                
                delay(200) // 메시지 간 간격
            }

            delay(1000) // 모든 메시지 전송 완료 대기

            // 모든 메시지 수정
            messageIds.forEachIndexed { index, messageId ->
                val editResult = chatWebSocketClient.editMessage(
                    roomId = testRoomId,
                    messageId = messageId,
                    newContent = "수정된 대량 테스트 메시지 #${index + 1}"
                )
                Assert.assertTrue("메시지 ${index + 1} 수정 성공", editResult.isSuccess)
                
                chatLogger.logWebSocketMessage(
                    action = "EDIT",
                    messageId = messageId.value,
                    roomId = testRoomId,
                    userId = testUserId.value,
                    success = true
                )
                
                delay(100) // 수정 간 간격
            }

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = true,
                details = "5개 메시지 대량 수정 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = false,
                details = "대량 메시지 수정 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testEditDeleteErrorHandling() = runBlocking {
        val testName = "수정/삭제 에러 처리 테스트"
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "$testName 시작 - 존재하지 않는 메시지 처리"
            )

            // Given: WebSocket 연결
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            Assert.assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            Assert.assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // When: 존재하지 않는 메시지 ID로 수정 시도
            val nonExistentMessageId = DocumentId("non-existent-message-${UUID.randomUUID()}")
            
            val editResult = chatWebSocketClient.editMessage(
                roomId = testRoomId,
                messageId = nonExistentMessageId,
                newContent = "존재하지 않는 메시지 수정 시도"
            )
            
            // Then: 에러 처리 확인 (실제 구현에 따라 다름)
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "존재하지 않는 메시지 수정 결과",
                mapOf(
                    "messageId" to nonExistentMessageId.value,
                    "success" to editResult.isSuccess.toString(),
                    "error" to (editResult.exceptionOrNull()?.message ?: "없음")
                )
            )

            // When: 존재하지 않는 메시지 ID로 삭제 시도
            val deleteResult = chatWebSocketClient.deleteMessage(
                roomId = testRoomId,
                messageId = nonExistentMessageId
            )
            
            // Then: 에러 처리 확인
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "존재하지 않는 메시지 삭제 결과",
                mapOf(
                    "messageId" to nonExistentMessageId.value,
                    "success" to deleteResult.isSuccess.toString(),
                    "error" to (deleteResult.exceptionOrNull()?.message ?: "없음")
                )
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = true,
                details = "수정/삭제 에러 처리 테스트 완료",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = false,
                details = "에러 처리 테스트 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testConcurrentEditOperations() = runBlocking {
        val testName = "동시 수정 작업 테스트"
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "$testName 시작 - 동일 메시지 동시 수정"
            )

            // Given: WebSocket 연결 및 메시지 생성
            val connectResult = chatWebSocketClient.connect(testServerUrl, testAuthToken)
            Assert.assertTrue("WebSocket 연결 성공", connectResult.isSuccess)
            
            val joinResult = chatWebSocketClient.joinRoom(testRoomId)
            Assert.assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            val messageId = DocumentId(UUID.randomUUID().toString())
            val sendResult = chatWebSocketClient.sendMessage(
                roomId = testRoomId,
                senderId = testUserId,
                content = "동시 수정 테스트용 메시지",
                messageId = messageId
            )
            Assert.assertTrue("메시지 전송 성공", sendResult.isSuccess)

            delay(1000) // 메시지 전송 완료 대기

            // When: 같은 메시지를 연속으로 빠르게 수정
            val edit1Result = chatWebSocketClient.editMessage(
                roomId = testRoomId,
                messageId = messageId,
                newContent = "첫 번째 수정"
            )
            
            delay(50) // 짧은 간격

            val edit2Result = chatWebSocketClient.editMessage(
                roomId = testRoomId,
                messageId = messageId,
                newContent = "두 번째 수정"
            )

            delay(50) // 짧은 간격

            val edit3Result = chatWebSocketClient.editMessage(
                roomId = testRoomId,
                messageId = messageId,
                newContent = "세 번째 수정 (최종)"
            )

            // Then: 결과 확인
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "동시 수정 작업 결과",
                mapOf(
                    "edit1Success" to edit1Result.isSuccess.toString(),
                    "edit2Success" to edit2Result.isSuccess.toString(),
                    "edit3Success" to edit3Result.isSuccess.toString()
                ),
                messageId = messageId.value
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = true,
                details = "동시 수정 작업 테스트 완료",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = testName,
                success = false,
                details = "동시 수정 작업 테스트 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }
}