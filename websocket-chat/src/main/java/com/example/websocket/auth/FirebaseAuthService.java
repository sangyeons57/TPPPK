package com.example.websocket.auth;

import com.example.websocket.config.FirebaseConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.api.core.ApiFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class FirebaseAuthService {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthService.class);
    private final FirebaseAuth firebaseAuth;
    private final boolean firebaseEnabled;

    public FirebaseAuthService() {
        this.firebaseEnabled = FirebaseConfig.isInitialized();
        this.firebaseAuth = firebaseEnabled ? FirebaseAuth.getInstance() : null;
        
        if (!firebaseEnabled) {
            logger.warn("Firebase not initialized - authentication will be bypassed");
        }
    }

    /**
     * Verify Firebase JWT token
     * @param idToken JWT token from Android client
     * @return CompletableFuture with user ID if valid, null if invalid
     */
    public CompletableFuture<String> verifyToken(String idToken) {
        if (!firebaseEnabled) {
            // For demo purposes - return a mock user ID when Firebase is disabled
            logger.info("Firebase disabled - using mock authentication");
            return CompletableFuture.completedFuture("demo_user_" + System.currentTimeMillis());
        }
        
        if (idToken == null || idToken.trim().isEmpty()) {
            logger.warn("Empty or null token provided");
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        
        ApiFutures.addCallback(firebaseAuth.verifyIdTokenAsync(idToken), 
            new com.google.api.core.ApiFutureCallback<FirebaseToken>() {
                @Override
                public void onSuccess(FirebaseToken result) {
                    future.complete(result.getUid());
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("Token verification failed: {}", t.getMessage());
                    future.complete(null);
                }
            }, Runnable::run);
            
        return future;
    }

    /**
     * Extract user ID from verified token (synchronous version for testing)
     * @param idToken JWT token
     * @return user ID if valid, null if invalid
     */
    public String verifyTokenSync(String idToken) {
        if (!firebaseEnabled) {
            // For demo purposes - return a mock user ID when Firebase is disabled
            logger.info("Firebase disabled - using mock authentication (sync)");
            return "demo_user_sync_" + System.currentTimeMillis();
        }
        
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            return decodedToken.getUid();
        } catch (Exception e) {
            logger.error("Synchronous token verification failed: {}", e.getMessage());
            return null;
        }
    }
}