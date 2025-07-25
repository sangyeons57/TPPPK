package com.example.websocket.handler;

import com.example.websocket.auth.FirebaseAuthService;
import com.example.websocket.model.ChatMessage;
import com.example.websocket.service.ChatRoomManager;
import com.example.websocket.service.FirestoreMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@ServerEndpoint(value = "/chat")
public class ChatWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ObjectMapper objectMapper;
    private final FirebaseAuthService authService;
    private final ChatRoomManager roomManager;
    private final FirestoreMessageService firestoreService;
    
    private String userId;
    private String currentRoomId;
    private Session session;

    public ChatWebSocketHandler(FirebaseAuthService authService, ChatRoomManager roomManager) {
        this.authService = authService;
        this.roomManager = roomManager;
        this.firestoreService = new FirestoreMessageService();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("🔌 WebSocket connection established - Session ID: {}", session.getId());
        logger.info("🔌 Remote address: {}", session.getRequestURI());
        logger.info("🔌 Protocol version: {}", session.getProtocolVersion());
        
        // Log all user properties for debugging
        logger.debug("🔍 Session user properties: {}", session.getUserProperties());
        
        // Extract token from Authorization header (stored in user properties by configurator)
        String token = (String) session.getUserProperties().get("auth_token");
        
        logger.info("🔑 Token extraction result - Token present: {}", token != null);
        if (token != null) {
            logger.debug("🔑 Token length: {}", token.length());
            logger.debug("🔑 Token starts with: {}", token.length() > 10 ? token.substring(0, 10) + "..." : token);
        }
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("❌ No authentication token provided in Authorization header");
            logger.warn("❌ Available user properties: {}", session.getUserProperties().keySet());
            closeWithError("Authentication required");
            return;
        }

        logger.info("🔑 Starting token verification...");
        authService.verifyToken(token)
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
            case "AUTH":
                // Skip AUTH messages - authentication is handled during handshake
                logger.debug("Ignoring AUTH message - authentication already handled during handshake");
                break;
            case "JOIN_ROOM":
                handleJoinRoom(message.getRoomId());
                break;
            case "LEAVE_ROOM":
                handleLeaveRoom(message.getRoomId());
                break;
            case "MESSAGE":
                handleMessage(message);
                break;
            case "EDIT_MESSAGE":
                handleEditMessage(message);
                break;
            case "DELETE_MESSAGE":
                handleDeleteMessage(message);
                break;
            case "PING":
            case "HEARTBEAT":
                sendPong();
                break;
            default:
                logger.warn("Unknown message type: {}", message.getType());
                sendErrorMessage("Unknown message type");
        }
    }

    private void handleJoinRoom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            sendErrorMessage("Room ID is required");
            return;
        }

        logger.info("🚪 User {} attempting to join room {}", userId, roomId);

        // Leave current room if any
        if (currentRoomId != null) {
            logger.info("🚪 User {} leaving current room {} to join {}", userId, currentRoomId, roomId);
            roomManager.leaveRoom(currentRoomId, userId, this);
        }

        // Join new room
        currentRoomId = roomId;
        roomManager.joinRoom(roomId, userId, this);
        
        // Send successful join confirmation
        sendJoinRoomSuccessMessage(roomId);
        logger.info("✅ User {} successfully joined room {} (room size: {})", 
                   userId, roomId, roomManager.getRoomSize(roomId));
    }

    private void handleLeaveRoom(String roomId) {
        if (currentRoomId != null && currentRoomId.equals(roomId)) {
            logger.info("🚪 User {} leaving room {}", userId, roomId);
            roomManager.leaveRoom(roomId, userId, this);
            currentRoomId = null;
            
            // Send successful leave confirmation
            sendLeaveRoomSuccessMessage(roomId);
            logger.info("✅ User {} successfully left room {}", userId, roomId);
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
            // Set message metadata (클라이언트에서 보낸 channelType, projectId는 유지)
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());
            message.setRoomId(currentRoomId);
            
            logger.info("📨 Processing message: channelType={}, projectId={}, roomId={}", 
                       message.getChannelType(), message.getProjectId(), currentRoomId);

            // 1. 먼저 Firestore에 저장
            firestoreService.saveMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("💾 Message saved to Firestore: messageId={}", message.getMessageId());
                    } else {
                        logger.warn("⚠️ Failed to save message to Firestore: messageId={}", message.getMessageId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Error saving message to Firestore: messageId={}, error={}", 
                               message.getMessageId(), throwable.getMessage());
                    return null;
                });

            // 2. WebSocket으로 다른 클라이언트들에게 브로드캐스트
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("📤 Message broadcast to room {} by user {} (messageId: {}) - echo prevented", 
                       currentRoomId, userId, message.getMessageId());
            
            // 3. 송신자에게 ACK 전송
            sendMessageAck(message.getMessageId(), "MESSAGE_ACK");
            logger.info("✅ MESSAGE_ACK sent to sender {} for messageId: {}", userId, message.getMessageId());
            
        } catch (Exception e) {
            logger.error("❌ Error processing message from user {}: {}", userId, e.getMessage());
            sendMessageAck(message.getMessageId(), "MESSAGE_FAILED");
        }
    }

    private void handleEditMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before editing messages");
            return;
        }

        try {
            // Set message metadata (클라이언트에서 보낸 channelType, projectId는 유지)
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());
            message.setRoomId(currentRoomId);
            message.setType("EDIT_MESSAGE");
            
            logger.info("✏️ Processing edit message: channelType={}, projectId={}, roomId={}", 
                       message.getChannelType(), message.getProjectId(), currentRoomId);

            // 1. Firestore에서 메시지 업데이트
            firestoreService.updateMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✏️ Message updated in Firestore: messageId={}", message.getMessageId());
                    } else {
                        logger.warn("⚠️ Failed to update message in Firestore: messageId={}", message.getMessageId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Error updating message in Firestore: messageId={}, error={}", 
                               message.getMessageId(), throwable.getMessage());
                    return null;
                });

            // 2. WebSocket으로 편집 알림 브로드캐스트
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("✏️ Message edit broadcast to room {} by user {} (messageId: {}) - echo prevented", 
                       currentRoomId, userId, message.getMessageId());
            
            // 3. 송신자에게 ACK 전송
            sendMessageAck(message.getMessageId(), "EDIT_MESSAGE_ACK");
            logger.info("✅ EDIT_MESSAGE_ACK sent to sender {} for messageId: {}", userId, message.getMessageId());
            
        } catch (Exception e) {
            logger.error("❌ Error processing edit message from user {}: {}", userId, e.getMessage());
            sendMessageAck(message.getMessageId(), "EDIT_MESSAGE_FAILED");
        }
    }

    private void handleDeleteMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before deleting messages");
            return;
        }

        try {
            // Set message metadata (클라이언트에서 보낸 channelType, projectId는 유지)
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());
            message.setRoomId(currentRoomId);
            message.setType("DELETE_MESSAGE");
            
            logger.info("🗑️ Processing delete message: channelType={}, projectId={}, roomId={}", 
                       message.getChannelType(), message.getProjectId(), currentRoomId);

            // 1. Firestore에서 메시지 삭제 표시
            firestoreService.deleteMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("🗑️ Message marked as deleted in Firestore: messageId={}", message.getMessageId());
                    } else {
                        logger.warn("⚠️ Failed to delete message in Firestore: messageId={}", message.getMessageId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Error deleting message in Firestore: messageId={}, error={}", 
                               message.getMessageId(), throwable.getMessage());
                    return null;
                });

            // 2. WebSocket으로 삭제 알림 브로드캐스트
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("🗑️ Message delete broadcast to room {} by user {} (messageId: {}) - echo prevented", 
                       currentRoomId, userId, message.getMessageId());
            
            // 3. 송신자에게 ACK 전송
            sendMessageAck(message.getMessageId(), "DELETE_MESSAGE_ACK");
            logger.info("✅ DELETE_MESSAGE_ACK sent to sender {} for messageId: {}", userId, message.getMessageId());
            
        } catch (Exception e) {
            logger.error("❌ Error processing delete message from user {}: {}", userId, e.getMessage());
            sendMessageAck(message.getMessageId(), "DELETE_MESSAGE_FAILED");
        }
    }

    private void sendPong() {
        ChatMessage pong = new ChatMessage("PONG", null, "server", "pong", Instant.now());
        sendMessage(pong);
    }

    private void sendAuthSuccessMessage() {
        ChatMessage authSuccessMessage = new ChatMessage("AUTH_SUCCESS", null, "system", "Authentication successful", Instant.now());
        sendMessage(authSuccessMessage);
        logger.info("📤 AUTH_SUCCESS message sent to user {}", userId);
    }

    private void sendJoinRoomSuccessMessage(String roomId) {
        ChatMessage joinSuccessMessage = new ChatMessage("JOINED_ROOM", roomId, "system", 
                                                         "Successfully joined room: " + roomId, Instant.now());
        sendMessage(joinSuccessMessage);
        logger.info("📤 JOINED_ROOM confirmation sent to user {} for room {}", userId, roomId);
    }

    private void sendLeaveRoomSuccessMessage(String roomId) {
        ChatMessage leaveSuccessMessage = new ChatMessage("LEFT_ROOM", roomId, "system", 
                                                          "Successfully left room: " + roomId, Instant.now());
        sendMessage(leaveSuccessMessage);
        logger.info("📤 LEFT_ROOM confirmation sent to user {} for room {}", userId, roomId);
    }

    private void sendSystemMessage(String type, String content) {
        ChatMessage systemMessage = new ChatMessage(type, currentRoomId, "system", content, Instant.now());
        sendMessage(systemMessage);
    }

    private void sendErrorMessage(String error) {
        ChatMessage errorMessage = new ChatMessage("ERROR", currentRoomId, "system", error, Instant.now());
        sendMessage(errorMessage);
    }

    private void sendMessageAck(String originalMessageId, String ackType) {
        ChatMessage ackMessage = new ChatMessage(ackType, currentRoomId, "system", "Message processed", Instant.now());
        // Set the messageId to match the original message for correlation
        ackMessage.setMessageId(originalMessageId);
        sendMessage(ackMessage);
    }

    public void sendMessage(ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (IOException e) {
            logger.error("Error sending message: {}", e.getMessage());
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

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    /**
     * Configurator for extracting Authorization header and storing token in session properties
     */
    public static class AuthConfigurator extends ServerEndpointConfig.Configurator {
        private static final Logger logger = LoggerFactory.getLogger(AuthConfigurator.class);
        
        @Override
        public void modifyHandshake(ServerEndpointConfig config, 
                                   jakarta.websocket.server.HandshakeRequest request, 
                                   jakarta.websocket.HandshakeResponse response) {
            
            logger.info("🤝 WebSocket handshake started");
            logger.info("🤝 Request URI: {}", request.getRequestURI());
            
            try {
                // Extract Authorization header
                Map<String, java.util.List<String>> headers = request.getHeaders();
                logger.info("🔍 Total headers received: {}", headers.size());
                
                // Log all headers for debugging (be careful not to log sensitive data in production)
                headers.forEach((key, values) -> {
                    if (key.toLowerCase().contains("auth")) {
                        logger.info("🔍 Header {}: [REDACTED] (length: {})", key, 
                                   values.isEmpty() ? 0 : values.get(0).length());
                    } else {
                        logger.debug("🔍 Header {}: {}", key, values);
                    }
                });
                
                String authHeader = null;
                
                // Check both lowercase and uppercase variants
                if (headers.containsKey("authorization") && !headers.get("authorization").isEmpty()) {
                    authHeader = headers.get("authorization").get(0);
                    logger.info("🔑 Found Authorization header (lowercase key)");
                } else if (headers.containsKey("Authorization") && !headers.get("Authorization").isEmpty()) {
                    authHeader = headers.get("Authorization").get(0);
                    logger.info("🔑 Found Authorization header (uppercase key)");
                } else {
                    logger.warn("❌ No Authorization header found");
                    logger.info("🔍 Available header keys: {}", headers.keySet());
                }
                
                String token = null;
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7); // Remove "Bearer " prefix
                    logger.info("✅ Token extracted from Authorization header (length: {})", token.length());
                } else if (authHeader != null) {
                    logger.warn("❌ Authorization header does not start with 'Bearer ': {}", 
                               authHeader.length() > 20 ? authHeader.substring(0, 20) + "..." : authHeader);
                } else {
                    logger.warn("❌ No Authorization header to process");
                }
                
                // Store token in user properties for later use in onOpen
                config.getUserProperties().put("auth_token", token);
                
                logger.info("🤝 Handshake processing completed - Token present: {}", token != null);
                
            } catch (Exception e) {
                logger.error("💥 Error during handshake processing: {}", e.getMessage(), e);
                // Don't fail the handshake, but log the error
                config.getUserProperties().put("auth_token", null);
                config.getUserProperties().put("handshake_error", e.getMessage());
            }
        }
    }
}