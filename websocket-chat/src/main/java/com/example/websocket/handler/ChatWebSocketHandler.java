package com.example.websocket.handler;

import com.example.websocket.auth.FirebaseAuthService;
import com.example.websocket.model.ChatMessage;
import com.example.websocket.service.ChatRoomManager;
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
    
    private String userId;
    private String currentRoomId;
    private Session session;

    public ChatWebSocketHandler(FirebaseAuthService authService, ChatRoomManager roomManager) {
        this.authService = authService;
        this.roomManager = roomManager;
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
                        sendSystemMessage("AUTH_SUCCESS", "Authentication successful");
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
        if (userId == null) {
            logger.warn("Received message from unauthenticated user");
            closeWithError("Not authenticated");
            return;
        }

        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            handleChatMessage(chatMessage);
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage());
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
            case "PING":
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

        // Leave current room if any
        if (currentRoomId != null) {
            roomManager.leaveRoom(currentRoomId, userId, this);
        }

        // Join new room
        currentRoomId = roomId;
        roomManager.joinRoom(roomId, userId, this);
        sendSystemMessage("JOINED_ROOM", "Joined room: " + roomId);
        logger.info("User {} joined room {}", userId, roomId);
    }

    private void handleLeaveRoom(String roomId) {
        if (currentRoomId != null && currentRoomId.equals(roomId)) {
            roomManager.leaveRoom(roomId, userId, this);
            currentRoomId = null;
            sendSystemMessage("LEFT_ROOM", "Left room: " + roomId);
            logger.info("User {} left room {}", userId, roomId);
        }
    }

    private void handleMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before sending messages");
            return;
        }

        // Set message metadata
        message.setSenderId(userId);
        message.setTimestampFromInstant(Instant.now());
        message.setRoomId(currentRoomId);

        // Broadcast to room
        roomManager.broadcastToRoom(currentRoomId, message);
        logger.info("📤 Message broadcast to room {} by user {} (messageId: {})", 
                   currentRoomId, userId, message.getMessageId());
    }

    private void sendPong() {
        ChatMessage pong = new ChatMessage("PONG", null, "server", "pong", Instant.now());
        sendMessage(pong);
    }

    private void sendSystemMessage(String type, String content) {
        ChatMessage systemMessage = new ChatMessage(type, currentRoomId, "system", content, Instant.now());
        sendMessage(systemMessage);
    }

    private void sendErrorMessage(String error) {
        ChatMessage errorMessage = new ChatMessage("ERROR", currentRoomId, "system", error, Instant.now());
        sendMessage(errorMessage);
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