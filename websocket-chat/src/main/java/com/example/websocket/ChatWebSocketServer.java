package com.example.websocket;

import com.example.websocket.auth.FirebaseAuthService;
import com.example.websocket.config.FirebaseConfig;
import com.example.websocket.handler.ChatWebSocketHandler;
import com.example.websocket.service.ChatRoomManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.time.Duration;

public class ChatWebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketServer.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        // Get port from environment variable or use default
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : DEFAULT_PORT;

        ChatWebSocketServer server = new ChatWebSocketServer();
        server.start(port);
    }

    public void start(int port) {
        try {
            // Initialize Firebase with detailed logging
            logger.info("🔧 Starting WebSocket Chat Server initialization...");
            logger.info("🔧 Port: {}", port);
            logger.info("🔧 Environment PORT variable: {}", System.getenv("PORT"));
            
            logger.info("🔥 Initializing Firebase...");
            FirebaseConfig.initialize();
            logger.info("🔥 Firebase initialization status: {}", FirebaseConfig.isInitialized());

            // Create shared services with detailed logging
            logger.info("🔧 Creating shared services...");
            FirebaseAuthService authService = new FirebaseAuthService();
            ChatRoomManager roomManager = new ChatRoomManager();
            logger.info("🔧 Services created successfully");

            // Create Jetty server
            logger.info("🚀 Creating Jetty server on port {}...", port);
            Server server = new Server(port);

            // Configure servlet context
            logger.info("🔧 Configuring servlet context...");
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // Add health check endpoint
            logger.info("🔧 Adding health check endpoint...");
            context.addServlet(HealthCheckServlet.class, "/health");

            // Configure WebSocket with Jakarta EE 10
            logger.info("🔧 Configuring WebSocket endpoints...");
            JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                try {
                    logger.info("🔧 Setting WebSocket container configuration...");
                    // Set WebSocket configuration
                    wsContainer.setDefaultMaxTextMessageBufferSize(65536);
                    wsContainer.setDefaultMaxSessionIdleTimeout(Duration.ofMinutes(4).toMillis());
                    logger.info("🔧 WebSocket buffer size: 65536, idle timeout: 4 minutes (client ping: 3 minutes)");

                    // Create configurator that combines dependency injection with authentication
                    logger.info("🔧 Creating enhanced endpoint configurator...");
                    ServerEndpointConfig.Configurator configurator = new ServerEndpointConfig.Configurator() {
                        @Override
                        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                            logger.debug("🔧 Creating endpoint instance for class: {}", endpointClass.getName());
                            if (endpointClass.equals(ChatWebSocketHandler.class)) {
                                ChatWebSocketHandler handler = new ChatWebSocketHandler(authService, roomManager);
                                logger.debug("🔧 ChatWebSocketHandler instance created successfully");
                                return endpointClass.cast(handler);
                            }
                            return super.getEndpointInstance(endpointClass);
                        }
                        
                        @Override
                        public void modifyHandshake(ServerEndpointConfig config, 
                                                   jakarta.websocket.server.HandshakeRequest request, 
                                                   jakarta.websocket.HandshakeResponse response) {
                            // Delegate to the AuthConfigurator logic
                            new ChatWebSocketHandler.AuthConfigurator().modifyHandshake(config, request, response);
                        }
                    };

                    // Create endpoint configuration
                    logger.info("🔧 Creating endpoint configuration for /chat...");
                    ServerEndpointConfig config = ServerEndpointConfig.Builder
                            .create(ChatWebSocketHandler.class, "/chat")
                            .configurator(configurator)
                            .build();

                    // Add endpoint
                    logger.info("🔧 Adding WebSocket endpoint to container...");
                    wsContainer.addEndpoint(config);
                    logger.info("✅ WebSocket endpoint /chat configured successfully");
                    
                } catch (Exception e) {
                    logger.error("❌ Error during WebSocket configuration: {}", e.getMessage(), e);
                    throw new RuntimeException("WebSocket configuration failed", e);
                }
            });

            // Start server
            logger.info("🚀 Starting Jetty server...");
            server.start();
            
            // Success logging
            logger.info("✅ WebSocket Chat Server started successfully!");
            logger.info("🌐 Server URL: http://localhost:{}", port);
            logger.info("🔌 WebSocket endpoint: ws://localhost:{}/chat", port);
            logger.info("❤️ Health check: http://localhost:{}/health", port);
            logger.info("🔥 Firebase enabled: {}", FirebaseConfig.isInitialized());

            // Wait for server to stop
            logger.info("🕐 Server running, waiting for shutdown signal...");
            server.join();

        } catch (Exception e) {
            logger.error("❌ Failed to start server: {}", e.getMessage(), e);
            logger.error("💥 Stack trace:", e);
            System.exit(1);
        }
    }

    public static class HealthCheckServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            
            try {
                // Detailed health check information
                String firebaseStatus = FirebaseConfig.isInitialized() ? "initialized" : "not_initialized";
                String javaVersion = System.getProperty("java.version");
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");
                long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024; // MB
                long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024; // MB
                long usedMemory = totalMemory - freeMemory;
                String timestamp = java.time.Instant.now().toString();
                
                // Environment variables
                String portEnv = System.getenv("PORT");
                String googleAppCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                
                String healthStatus = String.format(
                    "{" +
                    "\"status\":\"healthy\"," +
                    "\"timestamp\":\"%s\"," +
                    "\"firebase\":{\"%s\":\"%s\"}," +
                    "\"system\":{" +
                        "\"java_version\":\"%s\"," +
                        "\"os_name\":\"%s\"," +
                        "\"os_version\":\"%s\"," +
                        "\"memory_total_mb\":%d," +
                        "\"memory_free_mb\":%d," +
                        "\"memory_used_mb\":%d" +
                    "}," +
                    "\"environment\":{" +
                        "\"PORT\":\"%s\"," +
                        "\"GOOGLE_APPLICATION_CREDENTIALS\":\"%s\"" +
                    "}," +
                    "\"websocket\":{" +
                        "\"endpoint\":\"/chat\"," +
                        "\"protocol\":\"ws\"," +
                        "\"authentication\":\"firebase_jwt\"" +
                    "}" +
                    "}",
                    timestamp,
                    "status", firebaseStatus,
                    javaVersion, osName, osVersion,
                    totalMemory, freeMemory, usedMemory,
                    portEnv != null ? portEnv : "not_set",
                    googleAppCreds != null ? "set" : "not_set"
                );
                
                resp.getWriter().write(healthStatus);
                logger.info("🏥 Health check requested - Status: healthy, Firebase: {}", firebaseStatus);
                
            } catch (Exception e) {
                logger.error("💥 Error in health check: {}", e.getMessage(), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(String.format(
                    "{\"status\":\"error\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    e.getMessage(),
                    java.time.Instant.now()
                ));
            }
        }
    }
}