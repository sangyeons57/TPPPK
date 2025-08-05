# WebSocket Chat Server

Jetty-based WebSocket server for real-time chat functionality with Firebase authentication.

## Features

- 🔐 Firebase JWT authentication
- 🏠 Room-based chat messaging
- 🔄 Auto-reconnection support
- 📱 Android client integration
- ☁️ Google Cloud Run deployment ready
- 🏥 Health check endpoint
- 🔑 Application Default Credentials (ADC) support

## Quick Start

### Prerequisites

- Java 17+
- Gradle 8.6+
- Firebase project with Authentication enabled
- Google Cloud project with proper IAM permissions

### Local Development

1. **Setup Firebase Service Account (Local Development Only)**
   ```bash
   # For local development, you can use a service account key file
   # Place your Firebase service account key as:
   # websocket-chat/src/main/resources/firebase-service-account.json
   # OR set GOOGLE_APPLICATION_CREDENTIALS environment variable
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account-key.json"
   ```

2. **Build and Run**
   ```bash
   cd websocket-chat
   ./gradlew clean shadowJar
   java -jar build/libs/websocket-chat-1.0.0-all.jar
   ```

3. **Test Connection**
   ```bash
   # Health check
   curl http://localhost:8080/health
   
   # WebSocket endpoint: ws://localhost:8080/chat
   ```

### Cloud Run Deployment

#### 1. Service Account Setup

먼저 전용 서비스 계정을 생성하고 필요한 권한을 부여합니다:

```bash
# 서비스 계정 설정 스크립트 실행
chmod +x setup-service-account.sh
./setup-service-account.sh
```

또는 수동으로 설정:

```bash
PROJECT_ID="teamnovaprojectprojecting"
SERVICE_ACCOUNT_NAME="websocket-chat-sa"
SERVICE_ACCOUNT_EMAIL="${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

# 서비스 계정 생성
gcloud iam service-accounts create ${SERVICE_ACCOUNT_NAME} \
    --display-name="WebSocket Chat Service Account" \
    --project=${PROJECT_ID}

# Firestore 권한
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/datastore.user"

# Firebase Auth 권한
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/firebaseauth.admin"

# IAM Token Creator 권한 (커스텀 토큰용)
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/iam.serviceAccountTokenCreator"
```

#### 2. Deploy with Cloud Build

```bash
# 자동 배포 (cloudbuild.yaml 사용)
gcloud builds submit --config cloudbuild.yaml
```

#### 3. Manual Deployment

```bash
# Build image
docker build -t gcr.io/YOUR_PROJECT_ID/websocket-chat .

# Push to registry
docker push gcr.io/YOUR_PROJECT_ID/websocket-chat

# Deploy to Cloud Run with service account
gcloud run deploy websocket-chat \
  --image gcr.io/YOUR_PROJECT_ID/websocket-chat \
  --platform managed \
  --region asia-northeast3 \
  --allow-unauthenticated \
  --service-account websocket-chat-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --timeout 3600
```

## Firebase Authentication

### Application Default Credentials (ADC)

Cloud Run에서는 **Application Default Credentials (ADC)**를 사용합니다:

- ✅ **Cloud Run**: 자동으로 Workload Identity 사용
- ✅ **로컬 개발**: `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 또는 gcloud auth
- ❌ **서비스 계정 키 파일**: Cloud Run에서는 권장하지 않음

### 권한 요구사항

서비스 계정에 필요한 최소 권한:

- **Firestore 읽기/쓰기**: `roles/datastore.user`
- **Firebase Auth 관리**: `roles/firebaseauth.admin`
- **커스텀 토큰 생성**: `roles/iam.serviceAccountTokenCreator`

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

## Health Check

서비스 상태 확인:

```bash
curl https://websocket-chat-xxxxx-xx.a.run.app/health
```

응답 예시:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-27T10:30:00Z",
  "firebase": {
    "status": "initialized",
    "auth": "accessible",
    "firestore": "accessible"
  },
  "system": {
    "java_version": "17.0.9",
    "os_name": "Linux",
    "memory_total_mb": 1024,
    "memory_used_mb": 256
  },
  "websocket": {
    "endpoint": "/chat",
    "protocol": "ws",
    "authentication": "firebase_jwt"
  }
}
```

## Architecture

```
ChatWebSocketServer
├── ChatWebSocketHandler (WebSocket connection handling)
├── FirebaseAuthService (JWT token verification)
├── ChatRoomManager (Room management)
└── FirebaseConfig (ADC-based Firebase initialization)
```

## Security

- All connections require valid Firebase JWT tokens
- Tokens are verified on connection and message handling
- Users can only send messages to rooms they've joined
- Input validation for all message types
- Application Default Credentials for secure authentication

## Troubleshooting

### Firebase 초기화 실패

1. **서비스 계정 권한 확인**:
   ```bash
   gcloud projects get-iam-policy YOUR_PROJECT_ID \
     --flatten="bindings[].members" \
     --format="table(bindings.role)" \
     --filter="bindings.members:websocket-chat-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com"
   ```

2. **API 활성화 확인**:
   ```bash
   gcloud services list --enabled --filter="name:firebase"
   ```

3. **로그 확인**:
   ```bash
   gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=websocket-chat" --limit=50
   ```

### WebSocket 연결 문제

- Cloud Run 타임아웃: 기본 5분, 최대 60분
- 클라이언트 ping/pong 설정 확인
- 방화벽 및 네트워크 설정 확인

## Testing

```bash
# Run tests
./gradlew test

# Integration test with WebSocket client
# (Add your test implementation)
```

## Monitoring

- Health check endpoint: `/health`
- Structured logging with SLF4J
- Cloud Run metrics and logging integration
- Firebase Auth/Firestore 권한 상태 모니터링