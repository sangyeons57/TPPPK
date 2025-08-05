package com.example.websocket.service;

import com.example.websocket.auth.FirebaseAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe Singleton service provider for WebSocket dependencies
 * Ensures consistent service instances across all WebSocket handler creations
 */
public class ServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProvider.class);
    
    // Singleton instance with thread-safe initialization
    private static volatile ServiceProvider instance;
    private static final Object lock = new Object();
    
    // Service instances
    private final FirebaseAuthService authService;
    private final ChatRoomManager roomManager;
    
    /**
     * Private constructor for Singleton pattern
     */
    private ServiceProvider() {
        logger.info("🔧 ServiceProvider constructor started");
        
        try {
            logger.info("🔧 Creating FirebaseAuthService...");
            this.authService = new FirebaseAuthService();
            logger.info("✅ FirebaseAuthService created: {}", authService.getClass().getName());
        } catch (Exception e) {
            logger.error("❌ Failed to create FirebaseAuthService", e);
            throw new RuntimeException("FirebaseAuthService creation failed", e);
        }
        
        try {
            logger.info("🔧 Creating ChatRoomManager...");
            this.roomManager = new ChatRoomManager();
            logger.info("✅ ChatRoomManager created: {}", roomManager.getClass().getName());
        } catch (Exception e) {
            logger.error("❌ Failed to create ChatRoomManager", e);
            throw new RuntimeException("ChatRoomManager creation failed", e);
        }
        
        logger.info("✅ ServiceProvider constructor completed successfully");
    }
    
    /**
     * Initialize the ServiceProvider singleton
     * Thread-safe initialization
     */
    public static void initialize() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    logger.info("🔧 Creating ServiceProvider singleton instance...");
                    instance = new ServiceProvider();
                    logger.info("✅ ServiceProvider singleton created successfully");
                }
            }
        } else {
            logger.info("🔧 ServiceProvider already initialized");
        }
    }
    
    /**
     * Get the ServiceProvider singleton instance
     */
    public static ServiceProvider getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    logger.warn("⚠️ ServiceProvider not initialized, auto-initializing...");
                    instance = new ServiceProvider();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get FirebaseAuthService instance
     */
    public FirebaseAuthService getAuthService() {
        if (authService == null) {
            logger.error("❌ CRITICAL: authService is null in ServiceProvider!");
            throw new IllegalStateException("authService is null - ServiceProvider not properly initialized");
        }
        return authService;
    }
    
    /**
     * Get ChatRoomManager instance
     */
    public ChatRoomManager getRoomManager() {
        if (roomManager == null) {
            logger.error("❌ CRITICAL: roomManager is null in ServiceProvider!");
            throw new IllegalStateException("roomManager is null - ServiceProvider not properly initialized");
        }
        return roomManager;
    }
    
    /**
     * Get service status for debugging
     */
    public String getServiceStatus() {
        return String.format(
            "ServiceProvider[authService: %s, roomManager: %s]",
            authService != null ? authService.getClass().getSimpleName() : "null",
            roomManager != null ? roomManager.getClass().getSimpleName() : "null"
        );
    }
    
    /**
     * Check if all services are properly initialized
     */
    public boolean isProperlyInitialized() {
        return authService != null && roomManager != null;
    }
}