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
            // Initialize Firebase
            logger.info("Initializing Firebase...");
            FirebaseConfig.initialize();

            // Create shared services
            FirebaseAuthService authService = new FirebaseAuthService();
            ChatRoomManager roomManager = new ChatRoomManager();

            // Create Jetty server
            Server server = new Server(port);

            // Configure servlet context
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // Add health check endpoint
            context.addServlet(HealthCheckServlet.class, "/health");

            // Configure WebSocket with Jakarta EE 10
            JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                // Set WebSocket configuration
                wsContainer.setDefaultMaxTextMessageBufferSize(65536);
                wsContainer.setDefaultMaxSessionIdleTimeout(Duration.ofMinutes(5).toMillis());

                // Create configurator to provide service instances
                ServerEndpointConfig.Configurator configurator = new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        if (endpointClass.equals(ChatWebSocketHandler.class)) {
                            return endpointClass.cast(new ChatWebSocketHandler(authService, roomManager));
                        }
                        return super.getEndpointInstance(endpointClass);
                    }
                };

                // Create endpoint configuration
                ServerEndpointConfig config = ServerEndpointConfig.Builder
                        .create(ChatWebSocketHandler.class, "/chat")
                        .configurator(configurator)
                        .build();

                // Add endpoint
                wsContainer.addEndpoint(config);
            });

            // Start server
            server.start();
            logger.info("🚀 WebSocket Chat Server started on port {}", port);
            logger.info("WebSocket endpoint: ws://localhost:{}/chat", port);
            logger.info("Health check: http://localhost:{}/health", port);

            // Wait for server to stop
            server.join();

        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    public static class HealthCheckServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            
            String healthStatus = String.format(
                "{\"status\":\"healthy\",\"firebase\":\"" + (FirebaseConfig.isInitialized() ? "initialized" : "not_initialized") + "\",\"timestamp\":\"%s\"}",
                java.time.Instant.now()
            );
            
            resp.getWriter().write(healthStatus);
        }
    }
}