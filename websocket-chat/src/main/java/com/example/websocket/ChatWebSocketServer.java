package com.example.websocket;

import com.example.websocket.config.FirebaseConfig;
import com.example.websocket.handler.HealthWebSocketHandler;
import com.example.websocket.service.ServiceProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            logger.info("🔧 Firebase initialization check: {}", FirebaseConfig.isInitialized());
            
            // Create shared services using Singleton pattern for thread safety
            logger.info("🔧 Creating shared services using Singleton pattern...");
            
            try {
                logger.info("🔧 Initializing ServiceProvider singleton...");
                ServiceProvider.initialize();
                logger.info("✅ ServiceProvider initialized successfully");
            } catch (Exception e) {
                logger.error("❌ Failed to initialize ServiceProvider: {}", e.getMessage(), e);
                throw new RuntimeException("ServiceProvider initialization failed", e);
            }
            
            // Validate ServiceProvider initialization
            ServiceProvider serviceProvider = ServiceProvider.getInstance();
            logger.info("🔧 Services validation:");
            logger.info("🔧   - ServiceProvider status: {}", serviceProvider.getServiceStatus());
            logger.info("🔧   - Services properly initialized: {}", serviceProvider.isProperlyInitialized());

            // Create Jetty server
            logger.info("🚀 Creating Jetty server on port {}...", port);
            Server server = new Server(port);

            // Configure servlet context
            logger.info("🔧 Configuring servlet context...");
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            
            // Explicitly register health servlet
            logger.info("🔧 Registering health servlet...");
            ServletHolder healthServlet = new ServletHolder(HealthWebSocketHandler.class);
            context.addServlet(healthServlet, "/health");
            
            server.setHandler(context);

            // Final service validation before WebSocket configuration
            logger.info("🔧 Final service validation before WebSocket configuration:");
            logger.info("🔧   - ServiceProvider: {}", serviceProvider.getServiceStatus());
            
            // Configure WebSocket with Jakarta EE 10
            logger.info("🔧 Configuring WebSocket endpoints...");
            JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                try {
                    logger.info("🔧 Setting WebSocket container configuration...");
                    // Set WebSocket configuration
                    wsContainer.setDefaultMaxTextMessageBufferSize(65536);
                    wsContainer.setDefaultMaxSessionIdleTimeout(Duration.ofMinutes(4).toMillis());
                    logger.info("🔧 WebSocket buffer size: 65536, idle timeout: 4 minutes (client ping: 3 minutes)");
                    
                    logger.info("✅ WebSocket endpoint /chat configured via @ServerEndpoint annotation");
                    
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
}