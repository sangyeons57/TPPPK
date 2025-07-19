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
            logger.info("Firebase already initialized");
            return;
        }

        try {
            // Use Application Default Credentials (Workload Identity)
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId("teamnovaprojectprojecting")
                    .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            logger.info("Firebase initialized successfully using Workload Identity");
            
        } catch (Exception e) {
            logger.warn("Failed to initialize Firebase with Workload Identity: {}. Running without Firebase integration.", e.getMessage());
            initialized = false;
        }
    }


    public static boolean isInitialized() {
        return initialized;
    }
}