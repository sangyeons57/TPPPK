package com.example.websocket.handler;

import com.example.websocket.auth.FirebaseAuthService;
import com.example.websocket.constants.WebSocketEventConstants;
import com.example.websocket.model.ChatMessage;
import com.example.websocket.service.ChatRoomManager;
import com.example.websocket.service.FirestoreMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.websocket.constants.PayloadConstants;
import com.example.websocket.fcm.MentionNotificationService;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@ServerEndpoint(
    value = "/chat",
    configurator = ChatWebSocketHandler.ChatEndpointConfigurator.class)
public class ChatWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private FirebaseAuthService authService;
    private ChatRoomManager roomManager;
    private FirestoreMessageService firestoreService;
    private ObjectMapper objectMapper;
    
    // Mention notification dependencies (shared from ServiceProvider)
    private MentionNotificationService mentionNotificationService;
    private java.util.concurrent.ExecutorService notifyExecutor;
    
    private String userId;
    private String currentRoomId;
    private Session session;
    
    // 자동 Ping/Pong 모니터링을 위한 필드
    private long lastPongReceivedTime = 0;
    private int pongCount = 0;
    private static final long PONG_TIMEOUT_MS = 120000; // 2분 (30초 Ping * 4)

    // Default constructor required by Jakarta WebSocket
    public ChatWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.firestoreService = new FirestoreMessageService();
    }

    // Constructor for dependency injection
    public ChatWebSocketHandler(FirebaseAuthService authService, ChatRoomManager roomManager) {
        this();
        this.authService = authService;
        this.roomManager = roomManager;
        // Pull shared services from ServiceProvider
        com.example.websocket.service.ServiceProvider provider = com.example.websocket.service.ServiceProvider.getInstance();
        this.mentionNotificationService = provider.getMentionNotificationService();
        this.notifyExecutor = provider.getNotifyExecutor();
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        logger.debug("🔌 [AUTO-PING-PONG] WebSocket connection opened - ping/pong will start automatically");
        this.session = session;
        logger.info("🔌 WebSocket connection opened for session: {}", session.getId());

        // Services are already injected via constructor from ChatWebSocketServer
        // No need to get from UserProperties as they're set in constructor
        if (this.authService == null) {
            logger.error("❌ authService is null - dependency injection failed");
            closeWithError("Server configuration error");
            return;
        }
        if (this.roomManager == null) {
            logger.error("❌ roomManager is null - dependency injection failed");
            closeWithError("Server configuration error");
            return;
        }
        logger.debug("✅ Services injected successfully - authService and roomManager are ready");

        // Extract Bearer token from Authorization header
        String authHeader = (String) session.getUserProperties().get("Authorization");
        logger.info("🔑 Authorization header: {}", authHeader != null ? "present" : "missing");

        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.info("🔑 Extracted token (first 20 chars): {}...", token.substring(0, Math.min(20, token.length())));
        }

        if (token == null || token.trim().isEmpty()) {
            logger.warn("❌ No authentication token provided in Authorization header");
            logger.warn("❌ Available user properties: {}", session.getUserProperties().keySet());
            closeWithError("Authentication required");
            return;
        }

        // Verify Firebase token asynchronously
        logger.info("🔑 Starting Firebase token verification...");
        authService.verifyIdTokenAsync(token)
                .thenAccept(uid -> {
                    if (uid != null) {
                        this.userId = uid;
                        logger.info("✅ User authenticated successfully: {}", userId);
                        // Send explicit AUTH_SUCCESS message to client
                        sendAuthSuccessMessage();
                    } else {
                        logger.warn("❌ Authentication failed for token");
                        closeWithError("Authentication failed");
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("💥 Exception during token verification: {}", throwable.getMessage(), throwable);
                    closeWithError("Authentication error");
                    return null;
                });
    }

    @OnMessage
    public void onMessage(String message) {
        // 모든 원본 메시지(문자열) 로깅 (문제가 있는 메시지도 포함)
        logger.info("[WS-RAW] 수신 메시지 원본 (String):\n{}", message);
        if (userId == null) {
            logger.warn("Received message from unauthenticated user");
            closeWithError("Not authenticated");
            return;
        }

        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            handleChatMessage(chatMessage);
        } catch (Exception e) {
            logger.error("[WS-RAW] 메시지 파싱 실패: {}", e.getMessage(), e);
            sendErrorMessage("Invalid message format");
        }
    }

    @OnMessage
    public void onPong(PongMessage pongMessage) {
        long currentTime = System.currentTimeMillis();
        lastPongReceivedTime = currentTime;
        pongCount++;
        
        logger.debug("🏓 [AUTO-PING-PONG] Received pong frame from user {}: {} bytes (count: {}, time: {})", 
                   userId, pongMessage.getApplicationData().remaining(), pongCount, 
                   java.time.Instant.ofEpochMilli(currentTime));
        
        // 연결 상태 모니터링
        monitorConnectionHealth();
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("WebSocket closed for user {}: {} - {}", userId, closeReason.getCloseCode(), closeReason.getReasonPhrase());
        if (currentRoomId != null && userId != null) {
            roomManager.leaveRoom(currentRoomId, userId, this);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("WebSocket error for user {}: {}", userId, error.getMessage());
    }

    private void handleChatMessage(ChatMessage message) {
        // 들어온 메시지(ChatMessage 객체)를 pretty print로 보기 좋게 출력
        try {
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
            logger.info("[WS-PARSED] 수신 메시지 객체 (Pretty JSON):\n{}", prettyJson);
        } catch (JsonProcessingException e) {
            logger.warn("[WS-PARSED] 메시지 객체 JSON 변환 실패: {}", e.getMessage());
        }
        // 각 필드별 값 요약 로그
        logger.info("[WS-FIELDS] {}", message.toSummaryString());
        switch (message.getType()) {
            case WebSocketEventConstants.AUTH:
                // Skip AUTH messages - authentication is handled during handshake
                logger.debug("Ignoring AUTH message - authentication already handled during handshake");
                break;
            case WebSocketEventConstants.JOIN_ROOM:
                handleJoinRoom(message.getRoomId());
                break;
            case WebSocketEventConstants.LEAVE_ROOM:
                handleLeaveRoom(message.getRoomId());
                break;
            case WebSocketEventConstants.MESSAGE:
                handleMessage(message);
                break;
            case WebSocketEventConstants.EDIT_MESSAGE:
                handleEditMessage(message);
                break;
            case WebSocketEventConstants.DELETE_MESSAGE:
                handleDeleteMessage(message);
                break;
            default:
                logger.warn("Unknown message type: {}", message.getType());
                sendErrorMessage("Unknown message type: " + message.getType());
        }
    }

    private void handleJoinRoom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            sendErrorMessage("Room ID is required");
            return;
        }

        logger.info("🚪 User {} joining room {}", userId, roomId);

        // Leave current room if any
        if (currentRoomId != null) {
            logger.info("🚪 User {} leaving current room {} to join {}", userId, currentRoomId, roomId);
            roomManager.leaveRoom(currentRoomId, userId, this);
        }

        // Join new room
        currentRoomId = roomId;
        roomManager.joinRoom(roomId, userId, this);
        
        // Send join confirmation (standardized type name)
        ChatMessage joinConfirmation = ChatMessage.createSystemMessage(
                WebSocketEventConstants.JOINED_ROOM,
                roomId,
                userId,
                "Successfully joined room: " + roomId,
                Instant.now()
        );
        sendMessage(joinConfirmation);
        logger.info("✅ User {} successfully joined room {}", userId, roomId);
    }

    private void handleLeaveRoom(String roomId) {
        if (currentRoomId != null && currentRoomId.equals(roomId)) {
            logger.info("🚪 User {} leaving room {}", userId, roomId);
            roomManager.leaveRoom(roomId, userId, this);
            currentRoomId = null;
            
            // Send successful leave confirmation (standardized type name)
            ChatMessage leaveConfirmation = ChatMessage.createSystemMessage(
                    WebSocketEventConstants.LEFT_ROOM,
                    roomId,
                    userId,
                    "Successfully left room: " + roomId,
                    Instant.now()
            );
            sendMessage(leaveConfirmation);
        } else {
            logger.warn("❌ User {} attempted to leave room {} but is in room {}", 
                       userId, roomId, currentRoomId);
            sendErrorMessage("You are not in room: " + roomId);
        }
    }

    private void handleMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before sending messages");
            return;
        }

        try {
            // Set server-side fields
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());
            message.setRoomId(currentRoomId);

            // Normalize nested message fields for domain ownership
            if (message.getMessage() == null) {
                message.setMessage(new com.example.websocket.model.MessageData());
            }
            if (message.getMessage().getSenderId() == null) {
                message.getMessage().setSenderId(userId);
            }
            if (message.getMessage().getTimestamp() == null) {
                message.getMessage().setTimestamp((double) Instant.now().getEpochSecond());
            }
            if (message.getMessage().getId() == null) {
                message.getMessage().setId(message.getMessageId());
            }

            // Ensure message has a non-empty id for persistence/broadcast
            String effectiveId = message.getEffectiveMessageId();
            if (effectiveId == null || effectiveId.trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                message.setMessageId(generatedId);
                if (message.getMessage() != null) {
                    message.getMessage().setId(generatedId);
                }
                logger.info("🆔 Generated messageId on server: {} for room {}", generatedId, currentRoomId);
            }
            if (message.getMessage().getReplyToMessageId() == null) {
                message.getMessage().setReplyToMessageId(message.getReplyToMessageId());
            }
            
            logger.info("📨 Processing message: projectId={}, roomId={}", 
                       message.getProjectId(), currentRoomId);

            // 1. 먼저 Firestore에 저장
            firestoreService.saveMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✅ Message saved to Firestore: {}", message.getMessageId());
                        // After persistence, trigger mention notifications asynchronously
                        try {
                            List<String> mentionedUserIds = extractMentionedUserIds(message);
                            if (mentionNotificationService != null && mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
                                String channelType = (message.getProjectId() != null && !message.getProjectId().trim().isEmpty()) ? "project" : "dm";
                                String channelId = currentRoomId;
                                String messageId = message.getEffectiveMessageId();
                                String senderId = userId;
                                String senderName = userId;
                                String fullText = safeGetTextFromPayload(message);
                                notifyExecutor.submit(() -> mentionNotificationService.notifyMentions(
                                        channelType, channelId, messageId, senderId, senderName, fullText, mentionedUserIds
                                ));
                            }
                        } catch (Exception ex) {
                            logger.warn("⚠️ Failed to schedule mention notifications: {}", ex.getMessage());
                        }
                    } else {
                        logger.warn("⚠️ Failed to save message to Firestore: {}", message.getMessageId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("💥 Error saving message to Firestore: {}", throwable.getMessage(), throwable);
                    return null;
                });

            // 2. WebSocket으로 다른 클라이언트들에게 브로드캐스트
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("📤 Message broadcast to room {} by user {} (messageId: {}) - echo prevented", 
                       currentRoomId, userId, message.getMessageId());
            
            // 3. 송신자에게 ACK 전송
            ChatMessage ack = ChatMessage.createSystemMessage(WebSocketEventConstants.ACK, currentRoomId, "server", 
                                            "Message delivered", Instant.now());
            ack.setReplyToMessageId(message.getEffectiveMessageId());
            sendMessage(ack);
            logger.info("📩 ACK sent to sender {} for message {}", userId, message.getMessageId());

        } catch (Exception e) {
            logger.error("💥 Error processing message: {}", e.getMessage(), e);
            sendErrorMessage("Failed to process message");
        }
    }

    // Extract mentioned user IDs from the incoming message payload.
    // Supports formats:
    // - payload["mentions"] = List<String>
    // - payload["mentions"] = List<Map> with key "userId"
    private List<String> extractMentionedUserIds(ChatMessage message) {
        try {
            Map<String, Object> payload = message != null ? message.getEffectivePayload() : null;
            if (payload == null) return java.util.Collections.emptyList();
            Object raw = payload.get(PayloadConstants.MENTIONS);
            if (!(raw instanceof java.util.List)) return java.util.Collections.emptyList();
            java.util.List<?> arr = (java.util.List<?>) raw;
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (Object el : arr) {
                if (el instanceof String s) {
                    if (s != null && !s.trim().isEmpty()) ids.add(s);
                } else if (el instanceof java.util.Map<?, ?> m) {
                    Object uid = m.get("userId");
                    if (uid instanceof String s && !s.trim().isEmpty()) ids.add(s);
                }
            }
            // remove self if present
            ids.remove(userId);
            return new java.util.ArrayList<>(ids);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    private String safeGetTextFromPayload(ChatMessage message) {
        try {
            Map<String, Object> payload = message != null ? message.getEffectivePayload() : null;
            if (payload == null) return "";
            Object v = payload.get(PayloadConstants.CONTENT);
            return v != null ? String.valueOf(v) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void handleEditMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before editing messages");
            return;
        }

        try {
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());
            message.setRoomId(currentRoomId);

            // Normalize nested message fields for domain ownership
            if (message.getMessage() == null) {
                message.setMessage(new com.example.websocket.model.MessageData());
            }
            if (message.getMessage().getSenderId() == null) {
                message.getMessage().setSenderId(userId);
            }
            if (message.getMessage().getTimestamp() == null) {
                message.getMessage().setTimestamp((double) Instant.now().getEpochSecond());
            }
            if (message.getMessage().getId() == null) {
                message.getMessage().setId(message.getMessageId());
            }

            // Ensure non-empty id for edit operation as well
            String effectiveId = message.getEffectiveMessageId();
            if (effectiveId == null || effectiveId.trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                message.setMessageId(generatedId);
                if (message.getMessage() != null) {
                    message.getMessage().setId(generatedId);
                }
                logger.info("🆔 Generated messageId on server (edit): {} for room {}", generatedId, currentRoomId);
            }
            
            logger.info("✏️ Processing message edit: messageId={}, roomId={}", 
                       message.getMessageId(), currentRoomId);

            // Update in Firestore
            firestoreService.updateMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✅ Message updated in Firestore: {}", message.getMessageId());
                    } else {
                        logger.warn("⚠️ Failed to update message in Firestore: {}", message.getMessageId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("💥 Error updating message in Firestore: {}", throwable.getMessage(), throwable);
                    return null;
                });

            // Broadcast edit to room
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("📤 Message edit broadcast to room {} by user {}", currentRoomId, userId);
            
            // Send ACK to sender
            ChatMessage ack = ChatMessage.createSystemMessage(WebSocketEventConstants.ACK, currentRoomId, "server", 
                                            "Message edit delivered", Instant.now());
            ack.setReplyToMessageId(message.getEffectiveMessageId());
            sendMessage(ack);

        } catch (Exception e) {
            logger.error("💥 Error processing message edit: {}", e.getMessage(), e);
            sendErrorMessage("Failed to process message edit");
        }
    }

    private void handleDeleteMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before deleting messages");
            return;
        }

        try {
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());
            message.setRoomId(currentRoomId);

            // Normalize nested message fields for domain ownership
            if (message.getMessage() == null) {
                message.setMessage(new com.example.websocket.model.MessageData());
            }
            if (message.getMessage().getSenderId() == null) {
                message.getMessage().setSenderId(userId);
            }
            if (message.getMessage().getTimestamp() == null) {
                message.getMessage().setTimestamp((double) Instant.now().getEpochSecond());
            }
            if (message.getMessage().getId() == null) {
                message.getMessage().setId(message.getMessageId());
            }
            
            // Ensure non-empty id for delete operation as well
            String effectiveId = message.getEffectiveMessageId();
            if (effectiveId == null || effectiveId.trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                message.setMessageId(generatedId);
                if (message.getMessage() != null) {
                    message.getMessage().setId(generatedId);
                }
                logger.info("🆔 Generated messageId on server (delete): {} for room {}", generatedId, currentRoomId);
            }

            logger.info("🗑️ Processing message deletion: messageId={}, roomId={}", 
                       message.getMessageId(), currentRoomId);

            // Delete from Firestore
            firestoreService.deleteMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✅ Message deleted from Firestore: {}", message.getMessageId());
                    } else {
                        logger.warn("⚠️ Failed to delete message from Firestore: {}", message.getMessageId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("💥 Error deleting message from Firestore: {}", throwable.getMessage(), throwable);
                    return null;
                });

            // Broadcast deletion to room
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("📤 Message deletion broadcast to room {} by user {}", currentRoomId, userId);
            
            // Send ACK to sender
            ChatMessage ack = ChatMessage.createSystemMessage(WebSocketEventConstants.ACK, currentRoomId, "server", 
                                            "Message deletion delivered", Instant.now());
            ack.setReplyToMessageId(message.getEffectiveMessageId());
            sendMessage(ack);

        } catch (Exception e) {
            logger.error("💥 Error processing message deletion: {}", e.getMessage(), e);
            sendErrorMessage("Failed to process message deletion");
        }
    }



    private void sendAuthSuccessMessage() {
        ChatMessage authSuccess = ChatMessage.createSystemMessage(WebSocketEventConstants.AUTH_SUCCESS, null, "system", "Authentication successful", Instant.now());
        sendMessage(authSuccess);
        logger.info("✅ AUTH_SUCCESS message sent to user: {}", userId);
    }

    private void sendErrorMessage(String error) {
        ChatMessage errorMessage = ChatMessage.createSystemMessage(WebSocketEventConstants.ERROR, null, "server", error, Instant.now());
        sendMessage(errorMessage);
        logger.warn("❌ Error message sent: {}", error);
    }
    
    /**
     * 자동 Ping/Pong 연결 상태 모니터링
     */
    private void monitorConnectionHealth() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastPong = currentTime - lastPongReceivedTime;
        
        // Pong 수신 통계 로깅 (매번 출력하도록 변경)
        logger.debug("📊 [AUTO-PING-PONG] Connection health check - User: {}, Pong count: {}, Last pong: {}ms ago", 
                   userId, pongCount, timeSinceLastPong);
        
        // 타임아웃 체크 (2분 이상 Pong이 없으면 경고)
        if (lastPongReceivedTime > 0 && timeSinceLastPong > PONG_TIMEOUT_MS) {
            logger.warn("⚠️ [AUTO-PING-PONG] Connection timeout detected - User: {}, Time since last pong: {}ms", 
                       userId, timeSinceLastPong);
        }
        
        // 매번 ping/pong 상태 로그 출력 (디버깅용)
        logger.debug("🏓 [AUTO-PING-PONG] Ping/Pong Status - User: {}, Count: {}, Last: {}ms ago", 
                   userId, pongCount, timeSinceLastPong);
    }
    
    /**
     * 연결 상태 정보 반환 (모니터링용)
     */
    public String getConnectionHealthInfo() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastPong = lastPongReceivedTime > 0 ? currentTime - lastPongReceivedTime : 0;
        
        return String.format(
            "User: %s, Pong count: %d, Last pong: %dms ago, Connected: %s",
            userId, pongCount, timeSinceLastPong, 
            session != null && session.isOpen() ? "YES" : "NO"
        );
    }

    public void sendMessage(ChatMessage message) {
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.getBasicRemote().sendText(jsonMessage);
                logger.debug("📤 Sent message: {}", jsonMessage);
            } catch (Exception e) {
                logger.error("💥 Error sending message: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("⚠️ Attempted to send message to closed session for user: {}", userId);
        }
    }

    private void closeWithError(String reason) {
        try {
            sendErrorMessage(reason);
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
            }
        } catch (Exception e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }

    public String getUserId() {
        return userId;
    }

    public static class ChatEndpointConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
            // ServiceProvider를 통해 DI 수행
            com.example.websocket.service.ServiceProvider provider = com.example.websocket.service.ServiceProvider.getInstance();
            return clazz.cast(new ChatWebSocketHandler(
                    provider.getAuthService(), provider.getRoomManager()));
        }
        
        @Override
        public void modifyHandshake(ServerEndpointConfig config, 
                                   jakarta.websocket.server.HandshakeRequest request, 
                                   jakarta.websocket.HandshakeResponse response) {
            // Extract Authorization header from HTTP request
            Map<String, List<String>> headers = request.getHeaders();
            List<String> authHeaders = headers.get("authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                authHeaders = headers.get("Authorization");
            }
            
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                config.getUserProperties().put("Authorization", authHeader);
            }
        }
    }
}
