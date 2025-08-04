package com.example.websocket.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            logger.info("🔥 Firebase already initialized");
            return;
        }

        try {
            logger.info("🔥 Starting Firebase initialization...");
            logger.info("🔥 Project ID: teamnovaprojectprojecting");
            
            // Check environment variables
            String googleAppCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            logger.info("🔥 GOOGLE_APPLICATION_CREDENTIALS: {}", googleAppCreds != null ? "set" : "not_set");
            
            // Use Workload Identity (Cloud Run default)
            logger.info("🔥 Using Workload Identity for Firebase initialization...");
            
            // Cloud Run 등에서는 별도 JSON 없이 아래처럼 사용 가능
            FirebaseOptions options = FirebaseOptions.builder()
                    .setProjectId("teamnovaprojectprojecting")
                    .build();

            logger.info("🔥 Initializing Firebase app...");
            FirebaseApp.initializeApp(options);
            initialized = true;
            logger.info("✅ Firebase initialized successfully using Workload Identity");
            
        } catch (Exception e) {
            logger.warn("❌ Failed to initialize Firebase ({}): {}. Running in mock authentication mode.", 
                       e.getClass().getSimpleName(), e.getMessage());
            logger.debug("🔍 Full stack trace:", e);
            initialized = false;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}