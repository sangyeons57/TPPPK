# WebSocket Chat Server

Jetty-based WebSocket server for real-time chat functionality with Firebase authentication.

## Features

- 🔐 Firebase JWT authentication
- 🏠 Room-based chat messaging
- 🔄 Auto-reconnection support
- 📱 Android client integration
- ☁️ Google Cloud Run deployment ready
- 🏥 Health check endpoint

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Firebase project with Authentication enabled
- Firebase service account key

### Local Development

1. **Setup Firebase Service Account**
   ```bash
   # Place your Firebase service account key as:
   # websocket-chat/src/main/resources/firebase-service-account.json
   # OR
   # websocket-chat/firebase-service-account.json
   ```

2. **Build and Run**
   ```bash
   cd websocket-chat
   mvn clean package
   java -jar target/websocket-chat-1.0.0.jar
   ```

3. **Test Connection**
   ```bash
   # Health check
   curl http://localhost:8080/health
   
   # WebSocket endpoint: ws://localhost:8080/chat
   ```

### Cloud Run Deployment

1. **Build with Cloud Build**
   ```bash
   gcloud builds submit --config cloudbuild.yaml
   ```

2. **Manual Deployment**
   ```bash
   # Build image
   docker build -t gcr.io/YOUR_PROJECT_ID/websocket-chat .
   
   # Push to registry
   docker push gcr.io/YOUR_PROJECT_ID/websocket-chat
   
   # Deploy to Cloud Run
   gcloud run deploy websocket-chat \
     --image gcr.io/YOUR_PROJECT_ID/websocket-chat \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated
   ```

## WebSocket Protocol

### Connection
```
ws://localhost:8080/chat?token=FIREBASE_JWT_TOKEN
```

### Message Types

**Join Room**
```json
{
  "type": "JOIN_ROOM",
  "roomId": "dm_channels/abc123"
}
```

**Send Message**
```json
{
  "type": "MESSAGE",
  "content": "Hello world!",
  "roomId": "dm_channels/abc123"
}
```

**Leave Room**
```json
{
  "type": "LEAVE_ROOM",
  "roomId": "dm_channels/abc123"
}
```

**Ping/Pong**
```json
{
  "type": "PING"
}
```

### Server Responses

**Authentication Success**
```json
{
  "type": "AUTH_SUCCESS",
  "content": "Authentication successful",
  "timestamp": "2025-07-19T14:30:00Z"
}
```

**Message Broadcast**
```json
{
  "type": "MESSAGE",
  "roomId": "dm_channels/abc123",
  "senderId": "user123",
  "content": "Hello world!",
  "timestamp": "2025-07-19T14:30:00Z"
}
```

**Error Messages**
```json
{
  "type": "ERROR",
  "content": "Authentication failed",
  "timestamp": "2025-07-19T14:30:00Z"
}
```

## Room ID Format

Support for multiple room types:
- `dm_channels/docId` - Direct message channels
- `dm_wrapper/docId` - DM wrapper rooms  
- `projects_channels/docId` - Project channels
- `project_wrapper/docId` - Project wrapper rooms

## Environment Variables

- `PORT` - Server port (default: 8080)
- `JAVA_OPTS` - JVM options for Cloud Run

## Architecture

```
ChatWebSocketServer
├── ChatWebSocketHandler (WebSocket connection handling)
├── FirebaseAuthService (JWT token verification)
├── ChatRoomManager (Room management)
└── FirebaseConfig (Firebase initialization)
```

## Security

- All connections require valid Firebase JWT tokens
- Tokens are verified on connection and message handling
- Users can only send messages to rooms they've joined
- Input validation for all message types

## Testing

```bash
# Run tests
mvn test

# Integration test with WebSocket client
# (Add your test implementation)
```

## Monitoring

- Health check endpoint: `/health`
- Structured logging with SLF4J
- Cloud Run metrics and logging integration