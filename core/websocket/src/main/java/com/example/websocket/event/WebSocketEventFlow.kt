package com.example.websocket.event

import android.util.Log
import com.example.websocket.core.WebSocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 이벤트의 중앙 집중 배포 시스템
 *
 * GlobalWebSocketService에서 수신한 원시 WebSocket 메시지를
 * 도메인 이벤트로 변환하여 feature 모듈들에게 배포한다.
 *
 * 특징:
 * - 여러 feature가 동시에 동일한 이벤트를 구독할 수 있음
 * - 방별, 메시지 타입별 필터링 지원
 * - 백프레셔 방지를 위한 버퍼링
 * - 로깅 및 디버깅 지원
 */
@Singleton
class WebSocketEventFlow @Inject constructor(
    private val webSocketDomainMapper: WebSocketDomainMapper
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 이벤트 배포용 SharedFlow (백프레셔 방지를 위해 버퍼 설정)
    private val _domainEvents = MutableSharedFlow<WebSocketDomainEvent>(
        replay = 0, // 새 구독자에게 이전 이벤트 재전송하지 않음
        extraBufferCapacity = 64, // 64개까지 버퍼링 (백프레셔 방지)
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    /**
     * 모든 도메인 이벤트를 배포하는 SharedFlow
     * feature 모듈에서 이 Flow를 구독하여 이벤트를 수신
     */
    val domainEvents: SharedFlow<WebSocketDomainEvent> = _domainEvents.asSharedFlow()

    private var isInitialized = false

    /**
     * GlobalWebSocketService의 메시지 Flow를 연결하여 이벤트 배포 시작
     */
    fun initialize(webSocketMessageFlow: Flow<WebSocketMessage>) {
        if (isInitialized) {
            Log.w(TAG, "WebSocketEventFlow already initialized")
            return
        }

        isInitialized = true
        Log.d(TAG, "Initializing WebSocketEventFlow")

        scope.launch {
            webSocketMessageFlow
                .onEach { message ->
                    Log.d(
                        TAG,
                        "Processing WebSocket message: ${message.type}, roomId: ${message.roomId}"
                    )
                }
                .map { message ->
                    // 원시 WebSocket 메시지를 도메인 이벤트로 변환
                    webSocketDomainMapper.webSocketMessageToDomainEvent(message)
                }
                .onEach { domainEvent ->
                    Log.d(TAG, "Converted to domain event: ${domainEvent::class.simpleName}")
                }
                .collectLatest { domainEvent ->
                    // 변환된 도메인 이벤트를 모든 구독자에게 배포
                    _domainEvents.emit(domainEvent)
                }
        }
    }

    // ================================
    // 필터링된 이벤트 스트림 제공
    // ================================

    /**
     * 특정 방의 이벤트만 필터링하여 반환
     */
    fun getEventsForRoom(roomId: String): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            webSocketDomainMapper.isRoomRelatedEvent(event, roomId)
        }.onEach { event ->
            Log.d(TAG, "Room $roomId event: ${event::class.simpleName}")
        }
    }

    /**
     * 메시지 관련 이벤트만 필터링하여 반환
     */
    fun getMessageEvents(): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            webSocketDomainMapper.isMessageEvent(event)
        }
    }

    /**
     * 특정 방의 메시지 이벤트만 필터링하여 반환
     */
    fun getMessageEventsForRoom(roomId: String): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            webSocketDomainMapper.isMessageEvent(event) &&
                    webSocketDomainMapper.isRoomRelatedEvent(event, roomId)
        }.onEach { event ->
            Log.d(TAG, "Room $roomId message event: ${event::class.simpleName}")
        }
    }

    /**
     * 연결 상태 관련 이벤트만 필터링하여 반환
     */
    fun getConnectionEvents(): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            when (event) {
                is WebSocketDomainEvent.Connected,
                is WebSocketDomainEvent.Disconnected,
                is WebSocketDomainEvent.AuthenticationSucceeded,
                is WebSocketDomainEvent.AuthenticationFailed -> true

                else -> false
            }
        }
    }

    /**
     * 방 관련 이벤트만 필터링하여 반환 (입장/퇴장)
     */
    fun getRoomEvents(): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            when (event) {
                is WebSocketDomainEvent.RoomJoined,
                is WebSocketDomainEvent.RoomLeft,
                is WebSocketDomainEvent.UserJoinedRoom,
                is WebSocketDomainEvent.UserLeftRoom -> true

                else -> false
            }
        }
    }

    /**
     * 특정 방의 방 관련 이벤트만 필터링하여 반환
     */
    fun getRoomEventsForRoom(roomId: String): Flow<WebSocketDomainEvent> {
        return getRoomEvents().filter { event ->
            webSocketDomainMapper.isRoomRelatedEvent(event, roomId)
        }
    }

    /**
     * 시스템 메시지 및 에러 이벤트만 필터링하여 반환
     */
    fun getSystemEvents(): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            when (event) {
                is WebSocketDomainEvent.SystemMessage,
                is WebSocketDomainEvent.Error -> true

                else -> false
            }
        }
    }

    /**
     * ACK/실패 이벤트만 필터링하여 반환
     */
    fun getAckEvents(): Flow<WebSocketDomainEvent> {
        return domainEvents.filter { event ->
            when (event) {
                is WebSocketDomainEvent.MessageAck,
                is WebSocketDomainEvent.MessageFailed -> true

                else -> false
            }
        }
    }

    /**
     * 특정 메시지 ID의 ACK/실패 이벤트만 필터링하여 반환
     */
    fun getAckEventsForMessage(messageId: String): Flow<WebSocketDomainEvent> {
        return getAckEvents().filter { event ->
            when (event) {
                is WebSocketDomainEvent.MessageAck -> event.messageId == messageId
                is WebSocketDomainEvent.MessageFailed -> event.messageId == messageId
                else -> false
            }
        }
    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 현재 구독자 수 반환 (디버깅용)
     */
    fun getSubscriberCount(): Int {
        return _domainEvents.subscriptionCount.value
    }

    /**
     * 이벤트 배포 상태 확인
     */
    fun isActive(): Boolean {
        return isInitialized && !scope.coroutineContext[Job]!!.isCancelled
    }

    companion object {
        private const val TAG = "WebSocketEventFlow"
    }
}