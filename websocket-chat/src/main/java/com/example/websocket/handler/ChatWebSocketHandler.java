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
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@ServerEndpoint("/chat")
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
        logger.info("WebSocket connection established: {}", session.getId());
        
        // Extract token from query parameters
        Map<String, Object> userProperties = session.getUserProperties();
        String token = session.getRequestParameterMap().get("token") != null ? 
                      session.getRequestParameterMap().get("token").get(0) : null;
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("No authentication token provided");
            closeWithError("Authentication required");
            return;
        }

        authService.verifyToken(token)
                .thenAccept(uid -> {
                    if (uid != null) {
                        this.userId = uid;
                        logger.info("User authenticated: {}", userId);
                        sendSystemMessage("AUTH_SUCCESS", "Authentication successful");
                    } else {
                        logger.warn("Authentication failed for token");
                        closeWithError("Authentication failed");
                    }
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
        message.setTimestamp(Instant.now());
        message.setRoomId(currentRoomId);

        // Broadcast to room
        roomManager.broadcastToRoom(currentRoomId, message);
        logger.info("Message broadcast to room {} by user {}", currentRoomId, userId);
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
}