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
            
            // Firebase Cloud 환경으로 연결
            logger.info("🔥 Connecting to Firebase Cloud...");
            
            // Application Default Credentials (ADC)를 사용하여 Firebase 초기화
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId("teamnovaprojectprojecting")
                    .build();

            logger.info("🔥 Initializing Firebase app with ADC...");
            FirebaseApp.initializeApp(options);
            initialized = true;
            logger.info("✅ Firebase initialized successfully using Application Default Credentials");
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize Firebase ({}): {}", 
                       e.getClass().getSimpleName(), e.getMessage());
            logger.debug("🔍 Full stack trace:", e);
            initialized = false;
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}