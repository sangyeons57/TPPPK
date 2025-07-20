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
            
            // Use Application Default Credentials (Workload Identity)
            logger.info("🔥 Loading Application Default Credentials...");
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            logger.info("🔥 Credentials loaded successfully");
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId("teamnovaprojectprojecting")
                    .build();

            logger.info("🔥 Initializing Firebase app...");
            FirebaseApp.initializeApp(options);
            initialized = true;
            logger.info("✅ Firebase initialized successfully using Workload Identity");
            
        } catch (java.io.IOException e) {
            logger.warn("❌ Failed to load Google credentials ({}): {}. Running in mock authentication mode.", 
                       e.getClass().getSimpleName(), e.getMessage());
            logger.info("💡 To fix: Set GOOGLE_APPLICATION_CREDENTIALS environment variable or deploy to Google Cloud");
            initialized = false;
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