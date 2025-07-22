package com.example.websocket.service;

import com.example.websocket.handler.ChatWebSocketHandler;
import com.example.websocket.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

public class ChatRoomManager {
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomManager.class);
    
    // roomId -> Set of WebSocket handlers
    private final ConcurrentHashMap<String, Set<ChatWebSocketHandler>> rooms = new ConcurrentHashMap<>();
    
    // userId -> current room
    private final ConcurrentHashMap<String, String> userRooms = new ConcurrentHashMap<>();

    public void joinRoom(String roomId, String userId, ChatWebSocketHandler handler) {
        logger.info("User {} joining room {}", userId, roomId);
        
        // Add to room
        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(handler);
        
        // Track user's current room
        userRooms.put(userId, roomId);
        
        logger.info("Room {} now has {} participants", roomId, rooms.get(roomId).size());
    }

    public void leaveRoom(String roomId, String userId, ChatWebSocketHandler handler) {
        logger.info("User {} leaving room {}", userId, roomId);
        
        Set<ChatWebSocketHandler> roomHandlers = rooms.get(roomId);
        if (roomHandlers != null) {
            roomHandlers.remove(handler);
            
            // Remove empty rooms
            if (roomHandlers.isEmpty()) {
                rooms.remove(roomId);
                logger.info("Room {} removed (empty)", roomId);
            } else {
                logger.info("Room {} now has {} participants", roomId, roomHandlers.size());
            }
        }
        
        // Remove user tracking
        userRooms.remove(userId);
    }

    public void broadcastToRoom(String roomId, ChatMessage message) {
        broadcastToRoom(roomId, message, null);
    }
    
    public void broadcastToRoom(String roomId, ChatMessage message, String senderUserId) {
        Set<ChatWebSocketHandler> roomHandlers = rooms.get(roomId);
        if (roomHandlers == null || roomHandlers.isEmpty()) {
            logger.warn("Attempted to broadcast to empty room: {}", roomId);
            return;
        }

        // Create a copy to avoid ConcurrentModificationException
        Set<ChatWebSocketHandler> handlersCopy = new CopyOnWriteArraySet<>(roomHandlers);
        
        // Filter out sender if senderUserId is provided (echo prevention)
        int totalHandlers = handlersCopy.size();
        int targetHandlers = senderUserId != null ? 
            (int) handlersCopy.stream().filter(h -> !senderUserId.equals(h.getUserId())).count() : 
            totalHandlers;
            
        logger.info("Broadcasting message to room {} - total: {}, targets: {} (echo prevention: {})", 
                   roomId, totalHandlers, targetHandlers, senderUserId != null);
        
        handlersCopy.forEach(handler -> {
            try {
                // Skip sender if echo prevention is enabled
                if (senderUserId != null && senderUserId.equals(handler.getUserId())) {
                    logger.debug("Skipping echo to sender {} in room {}", senderUserId, roomId);
                    return;
                }
                
                handler.sendMessage(message);
            } catch (Exception e) {
                logger.error("Error sending message to handler (user: {}): {}", 
                           handler.getUserId(), e.getMessage());
                // Remove failed handler safely
                roomHandlers.remove(handler);
                logger.info("Removed failed handler for user {} from room {}", 
                           handler.getUserId(), roomId);
            }
        });
        
        // Clean up empty rooms
        if (roomHandlers.isEmpty()) {
            rooms.remove(roomId);
            logger.info("Room {} removed (no active handlers)", roomId);
        }
    }

    public int getRoomSize(String roomId) {
        Set<ChatWebSocketHandler> roomHandlers = rooms.get(roomId);
        return roomHandlers != null ? roomHandlers.size() : 0;
    }

    public boolean isUserInRoom(String userId, String roomId) {
        String userCurrentRoom = userRooms.get(userId);
        return roomId.equals(userCurrentRoom);
    }

    public String getUserCurrentRoom(String userId) {
        return userRooms.get(userId);
    }

    public Set<String> getAllRooms() {
        return rooms.keySet();
    }

    public void removeUser(String userId, ChatWebSocketHandler handler) {
        String currentRoom = userRooms.get(userId);
        if (currentRoom != null) {
            leaveRoom(currentRoom, userId, handler);
        }
    }
}