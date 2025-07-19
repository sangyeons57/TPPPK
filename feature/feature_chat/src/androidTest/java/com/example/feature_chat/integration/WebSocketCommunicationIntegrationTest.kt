package com.example.feature_chat.integration

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.core_common.websocket.WebSocketManager
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.feature_chat.logging.ChatLogger
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * WebSocket 실제 통신 테스트
 * - WebSocket 서버와 클라이언트 간 실제 통신 테스트
 * - 다중 클라이언트 간 메시지 교환 테스트
 * - 연결, 메시지 전송/수신, 수정, 삭제 등 전체 플로우 테스트
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WebSocketCommunicationIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var webSocketManager: WebSocketManager

    @Inject
    lateinit var chatLogger: ChatLogger

    private lateinit var client1: ChatWebSocketClient
    private lateinit var client2: ChatWebSocketClient
    
    // 테스트용 WebSocket 서버 URL (로컬 또는 테스트 서버)
    private val testServerUrl = "ws://localhost:8080/websocket"
    private val testAuthToken = "test-auth-token"
    private val testRoomId = "test-room-${UUID.randomUUID()}"
    
    private val testUser1 = UserId("user1-${UUID.randomUUID()}")
    private val testUser2 = UserId("user2-${UUID.randomUUID()}")

    @Before
    fun setup() {
        hiltRule.inject()
        
        // 두 개의 클라이언트 인스턴스 생성
        client1 = ChatWebSocketClient(webSocketManager, chatLogger)
        client2 = ChatWebSocketClient(webSocketManager, chatLogger)
        
        chatLogger.logTestResult(
            testName = "WebSocketCommunicationIntegrationTest",
            success = true,
            details = "테스트 환경 설정 완료"
        )
    }

    @After
    fun teardown() {
        runBlocking {
            try {
                client1.disconnect()
                client2.disconnect()
                chatLogger.logTestResult(
                    testName = "teardown",
                    success = true,
                    details = "WebSocket 연결 정리 완료"
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
    fun testWebSocketConnection() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            // Given: WebSocket 서버가 실행 중이어야 함
            chatLogger.logTestResult(
                testName = "testWebSocketConnection",
                success = true,
                details = "WebSocket 연결 테스트 시작"
            )

            // When: 클라이언트가 서버에 연결 시도
            val result = client1.connect(testServerUrl, testAuthToken)

            // Then: 연결이 성공해야 함
            assertTrue("WebSocket 연결 성공", result.isSuccess)
            
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketConnection",
                success = true,
                details = "연결 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testWebSocketConnection",
                success = false,
                details = "연결 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testRoomJoinAndLeave() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            // Given: WebSocket 연결이 성공한 상태
            val connectResult = client1.connect(testServerUrl, testAuthToken)
            assertTrue("연결 필요", connectResult.isSuccess)

            // When: 채팅방에 입장
            val joinResult = client1.joinRoom(testRoomId)

            // Then: 입장이 성공해야 함
            assertTrue("채팅방 입장 성공", joinResult.isSuccess)

            // When: 채팅방에서 퇴장
            val leaveResult = client1.leaveRoom(testRoomId)

            // Then: 퇴장이 성공해야 함
            assertTrue("채팅방 퇴장 성공", leaveResult.isSuccess)
            
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testRoomJoinAndLeave",
                success = true,
                details = "방 입장/퇴장 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testRoomJoinAndLeave",
                success = false,
                details = "방 입장/퇴장 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testMultiClientMessageExchange() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "다중 클라이언트 메시지 교환 테스트 시작"
            )

            // Given: 두 클라이언트가 모두 연결되고 같은 방에 입장
            val connect1 = client1.connect(testServerUrl, testAuthToken)
            val connect2 = client2.connect(testServerUrl, testAuthToken)
            
            assertTrue("클라이언트1 연결 성공", connect1.isSuccess)
            assertTrue("클라이언트2 연결 성공", connect2.isSuccess)

            val join1 = client1.joinRoom(testRoomId)
            val join2 = client2.joinRoom(testRoomId)
            
            assertTrue("클라이언트1 방 입장 성공", join1.isSuccess)
            assertTrue("클라이언트2 방 입장 성공", join2.isSuccess)

            // 메시지 수신 준비
            val receivedMessages = mutableListOf<ChatWebSocketEvent.MessageReceived>()
            val messageLatch = CountDownLatch(2) // 두 개의 메시지를 기대

            val job2 = launch {
                client2.getChatMessages(testRoomId).collect { event ->
                    if (event is ChatWebSocketEvent.MessageReceived) {
                        receivedMessages.add(event)
                        messageLatch.countDown()
                        chatLogger.logInfo(
                            ChatLogger.CATEGORY_TEST,
                            "클라이언트2가 메시지 수신: ${event.content}",
                            messageId = event.messageId
                        )
                    }
                }
            }

            // 약간의 지연 후 메시지 전송
            delay(1000)

            // When: 클라이언트1이 메시지 전송
            val messageId1 = DocumentId(UUID.randomUUID().toString())
            val testContent1 = "안녕하세요! 클라이언트1에서 보낸 메시지입니다."
            
            val sendResult1 = client1.sendMessage(
                roomId = testRoomId,
                senderId = testUser1,
                content = testContent1,
                messageId = messageId1
            )
            assertTrue("메시지1 전송 성공", sendResult1.isSuccess)

            // And: 클라이언트2도 메시지 전송
            val messageId2 = DocumentId(UUID.randomUUID().toString())
            val testContent2 = "반갑습니다! 클라이언트2에서 보낸 답장입니다."
            
            val sendResult2 = client2.sendMessage(
                roomId = testRoomId,
                senderId = testUser2,
                content = testContent2,
                messageId = messageId2
            )
            assertTrue("메시지2 전송 성공", sendResult2.isSuccess)

            // Then: 메시지들이 상대방에게 수신되어야 함
            val received = messageLatch.await(10, TimeUnit.SECONDS)
            assertTrue("메시지 수신 완료", received)

            job2.cancel()
            
            // 수신된 메시지 검증
            assertEquals("수신된 메시지 개수", 2, receivedMessages.size)
            
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testMultiClientMessageExchange",
                success = true,
                details = "다중 클라이언트 메시지 교환 성공, 수신 메시지: ${receivedMessages.size}개",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testMultiClientMessageExchange",
                success = false,
                details = "다중 클라이언트 테스트 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testMessageEditAndDelete() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "메시지 수정/삭제 테스트 시작"
            )

            // Given: 클라이언트 연결 및 방 입장
            val connectResult = client1.connect(testServerUrl, testAuthToken)
            assertTrue("연결 성공", connectResult.isSuccess)
            
            val joinResult = client1.joinRoom(testRoomId)
            assertTrue("방 입장 성공", joinResult.isSuccess)

            // 원본 메시지 전송
            val messageId = DocumentId(UUID.randomUUID().toString())
            val originalContent = "수정될 원본 메시지입니다."
            
            val sendResult = client1.sendMessage(
                roomId = testRoomId,
                senderId = testUser1,
                content = originalContent,
                messageId = messageId
            )
            assertTrue("원본 메시지 전송 성공", sendResult.isSuccess)

            delay(1000) // 메시지 전송 완료 대기

            // When: 메시지 수정
            val editedContent = "수정된 메시지 내용입니다."
            val editResult = client1.editMessage(
                roomId = testRoomId,
                messageId = messageId,
                newContent = editedContent
            )

            // Then: 수정이 성공해야 함
            assertTrue("메시지 수정 성공", editResult.isSuccess)

            delay(1000) // 수정 완료 대기

            // When: 메시지 삭제
            val deleteResult = client1.deleteMessage(
                roomId = testRoomId,
                messageId = messageId
            )

            // Then: 삭제가 성공해야 함
            assertTrue("메시지 삭제 성공", deleteResult.isSuccess)
            
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testMessageEditAndDelete",
                success = true,
                details = "메시지 수정/삭제 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testMessageEditAndDelete",
                success = false,
                details = "메시지 수정/삭제 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testConnectionStateTracking() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "연결 상태 추적 테스트 시작"
            )

            // Given: 초기 상태 확인
            val initialState = client1.connectionState.value
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "초기 연결 상태: $initialState"
            )

            // When: 연결 시도
            val connectResult = client1.connect(testServerUrl, testAuthToken)
            assertTrue("연결 성공", connectResult.isSuccess)

            // Then: 연결 상태가 변경되어야 함
            withTimeout(5000) {
                // 연결 상태 변화 기다리기
                var currentState = client1.connectionState.value
                while (currentState.toString().contains("Disconnected") || currentState.toString().contains("Connecting")) {
                    delay(100)
                    currentState = client1.connectionState.value
                }
                
                chatLogger.logInfo(
                    ChatLogger.CATEGORY_TEST,
                    "연결 후 상태: $currentState"
                )
            }

            // When: 연결 해제
            client1.disconnect()

            // Then: 연결 상태가 다시 변경되어야 함
            delay(1000)
            val disconnectedState = client1.connectionState.value
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "연결 해제 후 상태: $disconnectedState"
            )
            
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testConnectionStateTracking",
                success = true,
                details = "연결 상태 추적 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testConnectionStateTracking",
                success = false,
                details = "연결 상태 추적 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }

    @Test
    fun testErrorHandling() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "에러 처리 테스트 시작"
            )

            // Given: 잘못된 서버 URL로 연결 시도
            val invalidServerUrl = "ws://invalid-server:9999/websocket"
            
            // When: 연결 시도
            val connectResult = client1.connect(invalidServerUrl, testAuthToken)

            // Then: 연결이 실패해야 함
            assertTrue("잘못된 URL 연결 실패", connectResult.isFailure)
            
            chatLogger.logInfo(
                ChatLogger.CATEGORY_TEST,
                "예상된 연결 실패 확인: ${connectResult.exceptionOrNull()?.message}"
            )

            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testErrorHandling",
                success = true,
                details = "에러 처리 테스트 성공",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            chatLogger.logTestResult(
                testName = "testErrorHandling",
                success = false,
                details = "에러 처리 테스트 실패: ${e.message}",
                duration = duration
            )
            throw e
        }
    }
}