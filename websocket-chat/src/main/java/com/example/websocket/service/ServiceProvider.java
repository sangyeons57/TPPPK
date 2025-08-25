package com.example.websocket.service;

import com.example.websocket.auth.FirebaseAuthService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final FcmNotificationService fcmNotificationService;
    private final ExecutorService notifyExecutor;
    
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

        // FCM notification service (shared singleton instance)
        FcmNotificationService tmpFcmSvc;
        ExecutorService tmpExec;
        try {
            logger.info("🔧 Creating FcmNotificationService and shared executor...");
            tmpFcmSvc = new FcmNotificationService("app://channel", 100);
            tmpExec = Executors.newFixedThreadPool(8);
            logger.info("✅ FcmNotificationService created: {}", tmpFcmSvc.getClass().getName());
        } catch (Exception e) {
            logger.warn("⚠️ Failed to initialize FcmNotificationService; FCM notifications disabled: {}", e.getMessage());
            tmpFcmSvc = null;
            tmpExec = Executors.newFixedThreadPool(2);
        }
        this.fcmNotificationService = tmpFcmSvc;
        this.notifyExecutor = tmpExec;
        
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

    /** Shared FcmNotificationService */
    public FcmNotificationService getFcmNotificationService() {
        return fcmNotificationService; // can be null if init failed
    }

    /** Shared executor for async notifications */
    public ExecutorService getNotifyExecutor() {
        return notifyExecutor;
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
