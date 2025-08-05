package com.example.websocket.handler;

import com.example.websocket.config.FirebaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

public class HealthWebSocketHandler extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HealthWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        logger.info("🏥 Health check requested from: {}", request.getRemoteAddr());
        
        try {
            // Create health info
            HealthInfo healthInfo = createHealthInfo();
            String jsonResponse = objectMapper.writeValueAsString(healthInfo);
            
            // Set response headers
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            
            // Write response
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            
            logger.info("✅ Health check completed successfully");
            
        } catch (Exception e) {
            logger.error("💥 Error during health check: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Internal server error\"}");
        }
    }
    
    private HealthInfo createHealthInfo() {
        String firebaseStatus = FirebaseConfig.isInitialized() ? "initialized" : "not_initialized";
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024; // MB
        long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024; // MB
        long usedMemory = totalMemory - freeMemory;
        String timestamp = Instant.now().toString();
        String portEnv = System.getenv("PORT");
        
        return new HealthInfo(
            "healthy",
            firebaseStatus,
            javaVersion,
            osName,
            osVersion,
            totalMemory,
            freeMemory,
            usedMemory,
            timestamp,
            portEnv
        );
    }
    
    public static class HealthInfo {
        public String status;
        public String firebaseStatus;
        public String javaVersion;
        public String osName;
        public String osVersion;
        public long totalMemory;
        public long freeMemory;
        public long usedMemory;
        public String timestamp;
        public String portEnv;
        
        public HealthInfo(String status, String firebaseStatus, String javaVersion, 
                         String osName, String osVersion, long totalMemory, 
                         long freeMemory, long usedMemory, String timestamp, String portEnv) {
            this.status = status;
            this.firebaseStatus = firebaseStatus;
            this.javaVersion = javaVersion;
            this.osName = osName;
            this.osVersion = osVersion;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.usedMemory = usedMemory;
            this.timestamp = timestamp;
            this.portEnv = portEnv;
        }
    }
} 