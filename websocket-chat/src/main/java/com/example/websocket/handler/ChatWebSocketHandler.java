package com.example.websocket.handler;

import com.example.websocket.auth.FirebaseAuthService;
import com.example.websocket.constants.WebSocketEventConstants;
import com.example.websocket.model.ChatMessage;
import com.example.websocket.service.ChatRoomManager;
import com.example.websocket.service.FirestoreMessageService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private com.example.websocket.service.ProjectMemberService projectMemberService;
    
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
        // Configure ObjectMapper to ignore unknown properties and handle null values
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        this.firestoreService = new FirestoreMessageService();
        this.projectMemberService = new com.example.websocket.service.ProjectMemberService();
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
        this.projectMemberService = new com.example.websocket.service.ProjectMemberService();
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // WebSocket connection opened
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
        // Services injected successfully

        // Extract Bearer token from Authorization header
        String authHeader = (String) session.getUserProperties().get("Authorization");
        logger.info("🔑 Authorization header: {}", authHeader != null ? "present" : "missing");

        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            // Token extracted
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
        // 수신 JSON 로그
        logger.info("📥 [WS_RECV_JSON]");
        logPrettyJson(message);
        
        if (userId == null) {
            logger.warn("❌ 인증되지 않은 사용자로부터 메시지 수신");
            closeWithError("Not authenticated");
            return;
        }

        try {
            // WebSocketEnvelope 파싱 (필수)
            WebSocketEnvelope envelope = parseWebSocketEnvelope(message);
            
            logger.info("✅ [WS_RECV_ENVELOPE] type={}, roomId={}", 
                       envelope.getType(), envelope.getRoomId());
            handleWebSocketEnvelope(envelope);
            
        } catch (Exception e) {
            logger.error("❌ [WS_RECV_ERROR] 메시지 처리 실패: {}", e.getMessage());
            logPrettyJson(message);
            sendErrorMessage("Invalid message format: WebSocketEnvelope with 'type' field required");
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

    /**
     * WebSocketEnvelope 파싱
     * 필수: JSON에 type 필드 존재, 파싱 실패 시 예외 발생
     */
    private WebSocketEnvelope parseWebSocketEnvelope(String jsonMessage) throws Exception {
        try {
            // JSON을 Map으로 파싱해서 구조 확인
            Map<String, Object> jsonMap = objectMapper.readValue(jsonMessage, Map.class);
            
            // type 필드 필수 확인
            String type = (String) jsonMap.get("type");
            if (type == null) {
                throw new Exception("Missing required 'type' field in WebSocket message");
            }
            
            // WebSocket envelope로 파싱
            WebSocketEnvelope envelope = objectMapper.readValue(jsonMessage, WebSocketEnvelope.class);
            return envelope;
            
        } catch (Exception e) {
            logger.error("❌ WebSocketEnvelope 파싱 실패: {}", e.getMessage());
            throw new Exception("Invalid WebSocket message format: " + e.getMessage());
        }
    }

    /**
     * WebSocketEnvelope 기반 메시지 처리
     */
    private void handleWebSocketEnvelope(WebSocketEnvelope envelope) {
        String type = envelope.getType();
        String roomId = envelope.getRoomId();
        
        switch (type) {
            case WebSocketEventConstants.AUTH:
                logger.debug("Ignoring AUTH message - authentication already handled during handshake");
                break;
                
            case WebSocketEventConstants.JOIN_ROOM:
                if (roomId != null) {
                    handleJoinRoom(roomId);
                } else {
                    sendErrorMessage("Room ID is required for JOIN_ROOM");
                }
                break;
                
            case WebSocketEventConstants.LEAVE_ROOM:
                if (roomId != null) {
                    handleLeaveRoom(roomId);
                } else {
                    sendErrorMessage("Room ID is required for LEAVE_ROOM");
                }
                break;
                
            case WebSocketEventConstants.MESSAGE:
                ChatMessage message = envelope.getMessage();
                if (message != null) {
                    handleMessage(message);
                } else {
                    sendErrorMessage("Message data is required for MESSAGE");
                }
                break;
                
            case WebSocketEventConstants.EDIT_MESSAGE:
                ChatMessage editMessage = envelope.getMessage();
                if (editMessage != null) {
                    handleEditMessage(editMessage);
                } else {
                    sendErrorMessage("Message data is required for EDIT_MESSAGE");
                }
                break;
                
            case WebSocketEventConstants.DELETE_MESSAGE:
                ChatMessage deleteMessage = envelope.getMessage();
                if (deleteMessage != null) {
                    handleDeleteMessage(deleteMessage);
                } else {
                    sendErrorMessage("Message data is required for DELETE_MESSAGE");
                }
                break;
                
            default:
                logger.warn("Unknown WebSocket message type: {}", type);
                sendErrorMessage("Unknown message type: " + type);
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
        
        // Send join confirmation with correct JOINED_ROOM type
        sendJoinedRoomResponse(roomId);
        logger.info("✅ User {} successfully joined room {}", userId, roomId);
    }

    private void handleLeaveRoom(String roomId) {
        if (currentRoomId != null && currentRoomId.equals(roomId)) {
            logger.info("🚪 User {} leaving room {}", userId, roomId);
            roomManager.leaveRoom(roomId, userId, this);
            currentRoomId = null;
            
            // Send successful leave confirmation with correct LEFT_ROOM type
            sendLeftRoomResponse(roomId);
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

            // Mentions placement validation: mentions must be message-level only
            if (hasPayloadMentions(message)) {
                logger.error("❌ Invalid mentions location in payload for message {}", message.getId());
                sendErrorMessage("Invalid mentions location: use message.mentions (not payload.mentions)");
                return;
            }

            // 평탄(Flat) 스키마를 기본으로 사용: 중첩(message.*)은 수신 시 읽기 전용으로만 지원

            // Ensure message has a non-empty id for persistence/broadcast
            String messageId = message.getId();
            if (messageId == null || messageId.trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                message.setId(generatedId);
                logger.info("🆔 Generated messageId on server: {} for room {}", generatedId, currentRoomId);
            }
            // replyToMessageId는 봉투(envelope) 필드 사용
            
            logger.info("📨 Processing message: roomId={}", currentRoomId);

            // 1. 먼저 Firestore에 저장
            firestoreService.saveMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✅ Message saved to Firestore: {}", message.getId());
                        // Phase 1: Only store and broadcast mentions; skip FCM notifications.
                        // Guarded by env var for future enabling.
                        try {
                            // Enable by default; can be disabled by setting env to "false"
                            boolean enabled = Boolean.parseBoolean(System.getenv().getOrDefault("MENTION_FCM_ENABLED", "true"));
                            if (enabled) {
                                List<String> mentionedUserIds = extractMentionedUserIds(message);
                                if (mentionNotificationService != null && mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
                                    String channelType = (currentRoomId != null && currentRoomId.contains(":")) ? "project" : "dm";
                                    String channelId = currentRoomId;
                                    String notificationMessageId = message.getId();
                                    String senderId = userId;
                                    String senderName = userId;
                                    String fullText = safeGetTextFromPayload(message);
                                    notifyExecutor.submit(() -> mentionNotificationService.notifyMentions(
                                            channelType, channelId, messageId, senderId, senderName, fullText, mentionedUserIds
                                    ));
                                }
                            } else {
                                logger.info("🔕 Mention FCM disabled (MENTION_FCM_ENABLED=false). Stored/broadcast only.");
                            }
                        } catch (Exception ex) {
                            logger.warn("⚠️ Mention FCM scheduling block error: {}", ex.getMessage());
                        }
                    } else {
                        logger.warn("⚠️ Failed to save message to Firestore: {}", message.getId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("💥 Error saving message to Firestore: {}", throwable.getMessage(), throwable);
                    return null;
                });

            // 2. WebSocket으로 다른 클라이언트들에게 브로드캐스트
            roomManager.broadcastToRoom(currentRoomId, message, userId);
            logger.info("📤 Message broadcast to room {} by user {} (messageId: {}) - echo prevented", 
                       currentRoomId, userId, message.getId());
            
            // 3. 송신자에게 ACK 전송
            sendAckResponse(message.getId(), WebSocketEventConstants.MESSAGE_ACK);
            logger.info("📩 ACK sent to sender {} for message {}", userId, message.getId());

        } catch (Exception e) {
            logger.error("💥 Error processing message: {}", e.getMessage(), e);
            sendErrorMessage("Failed to process message");
        }
    }

    // Extract message-level mentions only. No payload fallback allowed.
    private java.util.List<com.example.websocket.model.MentionItem> extractMentions(ChatMessage message) {
        try {
            if (message == null) return java.util.Collections.emptyList();
            java.util.List<com.example.websocket.model.MentionItem> msgMentions = message.getMentions();
            if (msgMentions == null || msgMentions.isEmpty()) return java.util.Collections.emptyList();
            return normalizeMentions(msgMentions);
        } catch (Exception e) {
            logger.warn("⚠️ Failed to extract mentions: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    private java.util.List<com.example.websocket.model.MentionItem> normalizeMentions(java.util.List<com.example.websocket.model.MentionItem> items) {
        java.util.List<com.example.websocket.model.MentionItem> out = new java.util.ArrayList<>(items.size());
        for (com.example.websocket.model.MentionItem it : items) {
            if (it == null) continue;
            String type = it.getType() != null ? it.getType().trim() : "USER";
            type = type.equalsIgnoreCase("userId") ? "USER" : type; // client alias
            if (type.equalsIgnoreCase("role")) type = "ROLE";
            if (type.equalsIgnoreCase("everyone") || type.equalsIgnoreCase("all")) type = "EVERYONE";
            if (type.equalsIgnoreCase("user")) type = "USER";

            String id = it.getId();
            String dn = it.getDisplayName();
            out.add(new com.example.websocket.model.MentionItem(type.toUpperCase(), id, dn));
        }
        return out;
    }

    // Build userId list for all mention types: USER, ROLE, EVERYONE
    private List<String> extractMentionedUserIds(ChatMessage message) {
        java.util.List<com.example.websocket.model.MentionItem> items = extractMentions(message);
        java.util.Set<String> ids = new java.util.HashSet<>();
        boolean isProjectRoom = currentRoomId != null && currentRoomId.contains(":");
        String projectId = isProjectRoom ? currentRoomId.split(":", 2)[0] : null;

        for (com.example.websocket.model.MentionItem it : items) {
            if (it == null) continue;
            String type = it.getType() != null ? it.getType().toUpperCase() : "USER";
            switch (type) {
                case "USER": {
                    String id = it.getId();
                    if (id != null && !id.trim().isEmpty()) ids.add(id.trim());
                    break;
                }
                case "ROLE": {
                    if (!isProjectRoom || projectId == null) break; // roles only in project context
                    String roleId = it.getId();
                    if (roleId == null || roleId.trim().isEmpty()) break;
                    try {
                        java.util.List<String> roleMembers = projectMemberService.getProjectMemberIdsByRole(projectId, roleId.trim());
                        if (roleMembers != null) ids.addAll(roleMembers);
                    } catch (Exception e) {
                        logger.warn("⚠️ Failed to resolve role members for project {} role {}: {}", projectId, roleId, e.getMessage());
                    }
                    break;
                }
                case "EVERYONE": {
                    if (isProjectRoom && projectId != null) {
                        try {
                            java.util.List<String> members = projectMemberService.getProjectMemberIds(projectId);
                            if (members != null) ids.addAll(members);
                        } catch (Exception e) {
                            logger.warn("⚠️ Failed to resolve project members for project {}: {}", projectId, e.getMessage());
                        }
                    } else {
                        // DM: include the other participant only
                        String other = parseOtherUserFromDmRoomId(currentRoomId, userId);
                        if (other != null) ids.add(other);
                    }
                    break;
                }
                default: {
                    // ignore unknown types
                }
            }
        }

        // remove self if present
        ids.remove(userId);
        return new java.util.ArrayList<>(ids);
    }

    private String parseOtherUserFromDmRoomId(String roomId, String selfId) {
        try {
            if (roomId == null) return null;
            if (roomId.startsWith("dm_")) {
                // format: dm_uid1_uid2
                String[] parts = roomId.split("_", 3);
                if (parts.length >= 3) {
                    String u1 = parts[1];
                    String u2 = parts[2];
                    if (selfId != null && selfId.equals(u1)) return u2;
                    if (selfId != null && selfId.equals(u2)) return u1;
                    // if self not matched, return the first different
                    return !u1.equals(selfId) ? u1 : u2;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    // Validate illegal mentions in payload
    private boolean hasPayloadMentions(ChatMessage message) {
        try {
            Map<String, Object> payload = message != null ? message.getPayload() : null;
            if (payload == null) return false;
            return payload.containsKey(ChatMessage.PayloadKeys.MENTIONS) && payload.get(ChatMessage.PayloadKeys.MENTIONS) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String safeGetTextFromPayload(ChatMessage message) {
        try {
            Map<String, Object> payload = message != null ? message.getPayload() : null;
            if (payload == null) return "";
            Object v = payload.get(ChatMessage.PayloadKeys.CONTENT);
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

            // 편집 처리: 봉투(envelope) 기준으로 필수 필드(보낸이/타임스탬프/roomId)만 보정

            // Ensure non-empty id for edit operation as well
            String messageId = message.getId();
            if (messageId == null || messageId.trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                message.setId(generatedId);
                logger.info("🆔 Generated messageId on server (edit): {} for room {}", generatedId, currentRoomId);
            }
            
            logger.info("✏️ Processing message edit: messageId={}, roomId={}", 
                       message.getId(), currentRoomId);

            // Update in Firestore
            firestoreService.updateMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✅ Message updated in Firestore: {}", message.getId());
                    } else {
                        logger.warn("⚠️ Failed to update message in Firestore: {}", message.getId());
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
            sendAckResponse(message.getId(), WebSocketEventConstants.EDIT_MESSAGE_ACK);

        } catch (Exception e) {
            logger.error("💥 Error processing message edit: {}", e.getMessage(), e);
            sendErrorMessage("Failed to process message edit");
        }
    }

    // NOTE: 과거 서버 내부 임시 전송(sendTemporaryMessage) 기능은 제거되었습니다.
    // 클라이언트는 표준 MESSAGE 전송을 사용하고, 필요 시 클라이언트 측에서 방에 임시 입장(join)하여
    // 전송 후 퇴장하는 흐름(현재 sendMessage 경로)을 사용하세요.

    private void handleDeleteMessage(ChatMessage message) {
        if (currentRoomId == null) {
            sendErrorMessage("Must join a room before deleting messages");
            return;
        }

        try {
            message.setSenderId(userId);
            message.setTimestampFromInstant(Instant.now());

            // 삭제 처리: 봉투(envelope) 기준으로 필수 필드(보낸이/타임스탬프/roomId)만 보정
            
            // Ensure non-empty id for delete operation as well
            String messageId = message.getId();
            if (messageId == null || messageId.trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                message.setId(generatedId);
                logger.info("🆔 Generated messageId on server (delete): {} for room {}", generatedId, currentRoomId);
            }

            logger.info("🗑️ Processing message deletion: messageId={}, roomId={}", 
                       message.getId(), currentRoomId);

            // Delete from Firestore
            firestoreService.deleteMessage(currentRoomId, message)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("✅ Message deleted from Firestore: {}", message.getId());
                    } else {
                        logger.warn("⚠️ Failed to delete message from Firestore: {}", message.getId());
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
            sendAckResponse(message.getId(), WebSocketEventConstants.DELETE_MESSAGE_ACK);

        } catch (Exception e) {
            logger.error("💥 Error processing message deletion: {}", e.getMessage(), e);
            sendErrorMessage("Failed to process message deletion");
        }
    }



    private void sendAuthSuccessMessage() {
        ChatMessage authSuccess = ChatMessage.createSystemMessage("system", "Authentication successful", Instant.now());
        sendMessageWithType(authSuccess, WebSocketEventConstants.AUTH_SUCCESS);
        logger.info("✅ AUTH_SUCCESS message sent to user: {}", userId);
    }

    private void sendErrorMessage(String error) {
        ChatMessage errorMessage = ChatMessage.createSystemMessage("server", error, Instant.now());
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
        sendMessageWithType(message, WebSocketEventConstants.MESSAGE);
    }
    
    private void sendMessageWithType(ChatMessage message, String messageType) {
        if (session != null && session.isOpen()) {
            try {
                // ChatMessage를 WebSocketEnvelope로 감싸서 type 필드 추가
                WebSocketEnvelope envelope = new WebSocketEnvelope(
                    messageType, // 지정된 타입 사용
                    currentRoomId, // roomId
                    message // 실제 메시지 데이터
                );
                
                String jsonMessage = objectMapper.writeValueAsString(envelope);
                
                // 전송 JSON 로그
                logger.info("📤 [WS_SEND_JSON]");
                logPrettyJson(jsonMessage);
                
                session.getBasicRemote().sendText(jsonMessage);
            } catch (Exception e) {
                logger.error("💥 메시지 전송 실패: {}", e.getMessage());
            }
        } else {
            logger.warn("⚠️ 닫힌 세션에 메시지 전송 시도: {}", userId);
        }
    }
    
    private void sendJoinedRoomResponse(String roomId) {
        ChatMessage joinConfirmation = ChatMessage.createSystemMessage(
                userId,
                "Successfully joined room: " + roomId,
                Instant.now()
        );
        
        // Send JOINED_ROOM response with explicit roomId to ensure client filter matches
        if (session != null && session.isOpen()) {
            try {
                WebSocketEnvelope envelope = new WebSocketEnvelope(
                    WebSocketEventConstants.JOINED_ROOM, // type
                    roomId, // explicit roomId (not currentRoomId)
                    joinConfirmation // message
                );
                
                String jsonMessage = objectMapper.writeValueAsString(envelope);
                session.getAsyncRemote().sendText(jsonMessage);
                
                logger.info("✅ JOINED_ROOM response sent: type={}, roomId={}, currentRoomId={}", 
                           WebSocketEventConstants.JOINED_ROOM, roomId, currentRoomId);
                
            } catch (Exception e) {
                logger.error("❌ Failed to send JOINED_ROOM response: {}", e.getMessage(), e);
            }
        }
    }
    
    private void sendLeftRoomResponse(String roomId) {
        ChatMessage leaveConfirmation = ChatMessage.createSystemMessage(
                userId,
                "Successfully left room: " + roomId,
                Instant.now()
        );
        
        // Send LEFT_ROOM response with explicit roomId
        if (session != null && session.isOpen()) {
            try {
                WebSocketEnvelope envelope = new WebSocketEnvelope(
                    WebSocketEventConstants.LEFT_ROOM, // type
                    roomId, // explicit roomId
                    leaveConfirmation // message
                );
                
                String jsonMessage = objectMapper.writeValueAsString(envelope);
                session.getAsyncRemote().sendText(jsonMessage);
                
                logger.info("✅ LEFT_ROOM response sent: type={}, roomId={}", 
                           WebSocketEventConstants.LEFT_ROOM, roomId);
                
            } catch (Exception e) {
                logger.error("❌ Failed to send LEFT_ROOM response: {}", e.getMessage(), e);
            }
        }
    }
    
    private void sendAckResponse(String originalMessageId, String ackType) {
        String ackMessage = switch (ackType) {
            case WebSocketEventConstants.MESSAGE_ACK -> "Message delivered";
            case WebSocketEventConstants.EDIT_MESSAGE_ACK -> "Message edit delivered";
            case WebSocketEventConstants.DELETE_MESSAGE_ACK -> "Message deletion delivered";
            default -> "Operation delivered";
        };
        
        ChatMessage ack = ChatMessage.createSystemMessage("server", ackMessage, Instant.now());
        ack.setId(originalMessageId);  // ACK의 ID는 원본 메시지 ID
        // replyToMessageId는 null로 유지 (ACK는 답장이 아님)
        sendMessageWithType(ack, ackType);
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
    
    /**
     * JSON을 보기 좋게 로그로 출력
     */
    private void logPrettyJson(String jsonString) {
        try {
            Object json = objectMapper.readValue(jsonString, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            logger.info(prettyJson);
        } catch (Exception e) {
            // JSON 파싱 실패 시 원본 문자열 출력
            logger.info(jsonString);
        }
    }

    /**
     * WebSocket 통신 전용 Envelope 모델
     * type, roomId 등 통신 필드 + ChatMessage(도메인 데이터)로 구성
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private static class WebSocketEnvelope {
        private String type;           // MESSAGE, JOIN_ROOM, LEAVE_ROOM 등
        private String roomId;         // 라우팅용 방 ID
        private String errorCode;      // 오류 응답용
        private String authToken;      // 인증 토큰용 (null 값도 허용)
        private ChatMessage message;   // 실제 도메인 메시지 데이터

        public WebSocketEnvelope() {}

        public WebSocketEnvelope(String type, String roomId, ChatMessage message) {
            this.type = type;
            this.roomId = roomId;
            this.message = message;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }

        public ChatMessage getMessage() { return message; }
        public void setMessage(ChatMessage message) { this.message = message; }
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
