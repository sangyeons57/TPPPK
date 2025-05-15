**채팅 시스템 기술 명세서 (Technical Specification Document)**

**버전:** 2.1 (하이브리드 접근 및 세부 아키텍처 반영)
**작성일:** 2025년 5월 12일 (Current date: 2025-05-12)
**작성자:** Gemini (AI Assistant)

**1. 개요 (Overview)**

* **1.1. 목적:** Android 클라이언트와 Java 기반 WebSocket 서버 간의 실시간 채팅 기능을 구현한다. 메시지는 Firestore에 영구 저장되며, 클라이언트는 Firestore에서 직접 과거 기록을 조회한다. 실시간 메시지 전달의 낮은 지연 시간을 우선시하며, 메시지 저장 실패 시 명시적인 피드백을 제공한다.
* **1.2. 주요 기능:**
    * 실시간 1:1 또는 그룹 채팅 메시지 송수신 (WebSocket)
    * 메시지의 Firestore 영구 저장 (서버 -> Firestore Admin SDK)
    * 채팅 기록 스크롤/페이지네이션 기반 로딩 (클라이언트 -> Firestore Client SDK)
    * 메시지 전송 상태 피드백 (클라이언트 Optimistic Update 및 서버 실패/성공 알림)
    * 멀티미디어 메시지 지원 (이미지, 비디오, 파일 등 - Firebase Storage 활용)
* **1.3. 아키텍처:**
    * **클라이언트 (Android):** Kotlin, Jetpack Compose 기반 UI. OkHttp WebSocket 클라이언트 사용. Firestore Client SDK로 직접 기록 조회. Firebase Storage SDK로 미디어 업로드.
    * **WebSocket 서버 (Java):** JSR 356 (Java API for WebSocket) 기반 구현. 임베디드 서블릿 컨테이너(Tomcat/Jetty 등) 사용. 클라이언트 간 메시지 중계 및 Firestore 저장(Admin SDK 사용), FCM 메시지 발송. Google Cloud Run에 Docker 컨테이너로 배포.
    * **백엔드 DB/Auth/Storage (Firebase):** Firestore (데이터 저장), Firebase Authentication (사용자 인증), Firebase Storage (파일 저장), Firebase Cloud Messaging (FCM).

    ```
    +-------------------------+
    |     Client (Android)    |
    | - Kotlin, Jetpack Comp. |
    | - OkHttp (WebSocket)    |
    | - Firestore SDK         |
    | - Storage SDK           |
    +-----------+-------------+
                | (WebSocket: OkHttp <-> JSR 356)
                v
    +-----------+-------------+
    |  Java WebSocket Server  |
    | - JSR 356, Tomcat Emb.  |
    | - Docker on Cloud Run   |
    +-----------+-------------+
                | (Admin SDK: Auth Token Verify)
                |----------------------------+
                | (Admin SDK: Firestore R/W) |
                v                            v
    +-----------+-------------+  +-----------+-------------+
    | Firebase Authentication |  |      Firestore (DB)     |
    +-------------------------+  +-------------------------+
                ^                            ^
                | (Client SDK: Auth)         | (Client SDK: History Load)
                |                            |
    +-----------+-------------+              |
    |   Client (Android)    |<-------------+ (Same as above)
    | (For Auth & History)  |
    +-------------------------+

    +-------------------------+
    |   Client (Android)    |
    | (For Storage Up/Down) |
    +-----------+-------------+
                | (Storage SDK)
                v
    +-----------+-------------+
    |    Firebase Storage     |
    +-------------------------+
                ^
                | (Server stores URL, Client uploads/downloads)
                | (Potentially, Server -> Storage for metadata if needed)
                |
    +-----------+-------------+
    |  Java WebSocket Server  | (Same as above)
    +-------------------------+

    +-------------------------+
    |  Java WebSocket Server  | (Same as above)
    +-----------+-------------+
                | (Admin SDK: FCM Send)
                v
    +-----------+-------------+
    |          FCM            |
    +-------------------------+
                ^
                | (Client SDK: Receive Push)
                |
    +-----------+-------------+
    |   Client (Android)      |
    | (For Push Notifications)|
    +-------------------------+
    ```

* **1.4. 기술 스택 (Technology Stack)**
    * **클라이언트 (Client - Android):**
        * 언어: Kotlin
        * UI: Jetpack Compose
        * 플랫폼: Android SDK
        * Firebase: Firebase Authentication SDK, Firestore Client SDK, Firebase Storage SDK, Firebase Messaging SDK (FCM)
        * WebSocket: OkHttp
        * JSON 처리: Kotlinx Serialization
        * 비동기 처리: Kotlin Coroutines (Flow, StateFlow, SharedFlow)
        * 상태 관리: ViewModel
        * DI: Hilt
    * **백엔드 (Backend - DB/Auth/Storage):**
        * 서비스: Firebase
        * 데이터베이스: Firestore
        * 인증: Firebase Authentication
        * 파일 저장소: Firebase Storage
        * 푸시 알림: Firebase Cloud Messaging (FCM)
    * **WebSocket 서버 (WebSocket Server):**
        * 언어: Java (JDK 11 이상)
        * WebSocket API: JSR 356 (Jakarta WebSocket API)
        * 런타임 환경: 임베디드 서블릿 컨테이너 (Tomcat Embedded)
        * Firebase 연동: Firebase Admin SDK for Java (Auth, Firestore, FCM)
        * JSON 처리: Jackson
        * 빌드 도구: Gradle
        * 로깅: SLF4j + Logback
        * 호스팅: Google Cloud Run (Docker 컨테이너 기반)
        * (선택적) DI: Google Guice 또는 직접 관리

**2. 컴포넌트 (Components)**

* **2.1. 클라이언트 (Android - `feature_chat` 또는 별도 `chat` 모듈):**
    * **`ChatRepository`:** WebSocket 연결/메시지 송수신, Firestore 과거 기록 조회/페이지네이션, 미디어 파일 Storage 업로드, 실시간/과거 메시지 통합 및 ViewModel에 Flow 제공.
    * OkHttp WebSocket 클라이언트 및 Listener 구현.
    * Firebase Client SDK (Auth, Firestore, Storage, FCM) 사용.
    * Jetpack Compose 채팅 UI (메시지 목록, 입력창, 상태 표시, 미디어 표시).
    * ViewModel, Coroutines, Hilt 활용.
* **2.2. WebSocket 서버 (Java - JSR 356):**
    * JSR 356 어노테이션 사용 WebSocket 엔드포인트 구현.
    * 임베디드 Tomcat 환경에서 실행.
    * 클라이언트 `Session` 관리, Firebase ID 토큰 검증 (Admin SDK).
    * 메시지 수신, JSON 파싱(Jackson), 유효성 검사, 채팅방 멤버에게 브로드캐스팅.
    * Firebase Admin SDK 사용하여 Firestore 메시지 저장, FCM 푸시 알림 발송.
    * Firestore 저장 실패 시 클라이언트에 `messageSendFailed` 알림.
    * Docker 이미지로 패키징.
* **2.3. Firebase (Firestore, Authentication, Storage, FCM):**
    * **Firestore:** `chats`, `messages` 컬렉션. (3.1. 참조)
    * **Authentication:** 사용자 인증 및 ID 토큰.
    * **Storage:** 채팅 미디어 파일 (이미지, 비디오 등) 저장.
    * **FCM:** 백그라운드 사용자에게 푸시 알림.

**3. 데이터 모델 (Data Models)**

* **3.1. Firestore 데이터 구조:**
    * `users/{userId}`: `fcmToken` (String) 필드 추가.
    * `chats/{chatId}`: 필드: `members` (List<String> - user IDs), `lastMessagePreview` (String), `lastMessageTimestamp` (Timestamp), `createdAt` (Timestamp), `updatedAt` (Timestamp).
    * `messages/{messageId}`:
        * `chatId` (String)
        * `senderId` (String)
        * `content` (String - 텍스트 메시지 또는 미디어 URL)
        * `contentType` (String - "text", "image", "video", "file", "audio")
        * `timestamp` (Timestamp - 서버 시간 기준)
        * `status` (String - "sent", "delivered_to_server" - 사용 안 함, 클라이언트가 직접 관리)
        * `metadata` (Map<String, Any> - optional, for media: `fileName`, `fileSize`, `mimeType`, `thumbnailUrl` 등)
        * `clientTempId` (String - optional, 클라이언트에서 생성한 임시 ID, 디버깅/추적용)
* **3.2. WebSocket 메시지 형식 (JSON - Kotlinx Serialization / Jackson):**
    * 기본 구조: `{ "type": "eventType", "payload": { ... } }` (4. API 명세 참조)
* **3.3. 클라이언트 도메인 메시지 모델 (`ChatMessage.kt` 확장):**
    * `id`: String (Firestore `messageId` 또는 `clientTempId`)
    * `chatId`: String
    * `senderId`: String
    * `senderName`: String (UI 표시용, 필요시 User 정보와 조합)
    * `senderProfileUrl`: String? (UI 표시용)
    * `content`: String (텍스트 또는 미디어 URL)
    * `contentType`: String
    * `timestamp`: Instant (서버 시간 기준)
    * `isSentByCurrentUser`: Boolean (UI 구분용)
    * `status`: Enum (`SENDING`, `SENT`, `FAILED`, `READ`) - 클라이언트 UI 관리용
    * `clientTempId`: String? (메시지 전송 시 사용)
    * `metadata`: Map<String, Any>? (미디어 정보)

**4. 주요 플로우 및 API 명세 (Key Flows & API Specifications)**

* **4.1. WebSocket 연결 및 인증:** (기존과 유사, 토큰 검증 강화)
    * **Client -> Server:** OkHttp 연결, 성공 시 `authenticate` 메시지 전송.
        * `{ "type": "authenticate", "payload": { "token": "Firebase_ID_Token" } }`
    * **Server:** `Session` 저장, `authenticate` 메시지 수신, Admin SDK `verifyIdToken`, userId와 `Session` 매핑.
    * **Server -> Client:**
        * 성공: `{ "type": "authenticated", "payload": { "userId": "..." } }`
        * 실패: `{ "type": "authenticationFailed", "payload": { "reason": "..." } }`, 연결 종료.

* **4.2. 실시간 텍스트/미디어 메시지 전송:**
    * **Client 1 -> Server:** `sendMessage` (텍스트 또는 미디어 URL 포함)
        * `{ "type": "sendMessage", "payload": { "chatId": "...", "content": "...", "contentType": "text/image/video...", "clientTempId": "uuid_client", "metadata": { ... } } }`
        * *Client는 UI에 "보내는 중" 상태로 즉시 표시 (Optimistic Update).*
    * **Server (`@OnMessage`):**
        1.  메시지 수신, JSON 파싱.
        2.  `chatId` 유효성, 사용자 전송 권한 확인.
        3.  `senderId` (인증된 사용자), `timestamp` (서버 시간) 설정.
        4.  **즉시 브로드캐스팅:** 해당 `chatId`의 다른 활성 `Session`들에 `newMessage` 전송.
            * `{ "type": "newMessage", "payload": { /* Firestore `messages` 문서와 유사한 구조 */ "messageId": "temp_server_id_or_clientTempId", "chatId": "...", "senderId": "...", "content": "...", "contentType": "...", "timestamp": ..., "metadata": {...} } }`
        5.  **Firestore 저장 시도 (비동기):** Admin SDK로 `messages` 저장. `clientTempId`도 저장 가능.
    * **Firestore 저장 성공 시:**
        * **Server -> Client 1 (발신자):** `messageSentConfirmation`
            * `{ "type": "messageSentConfirmation", "payload": { "clientTempId": "...", "messageId": "firestore_doc_id", "timestamp": ... } }`
        * **(선택적) Server -> 다른 수신자들:** `messageIdUpdated` (만약 `newMessage`에서 임시 ID를 사용했다면)
            * `{ "type": "messageIdUpdated", "payload": { "tempMessageId": "...", "finalMessageId": "firestore_doc_id", "timestamp": ... } }`
    * **Firestore 저장 실패 시:**
        * **Server:** 오류 로깅.
        * **Server -> Client 1 (발신자):** `messageSendFailed`
            * `{ "type": "messageSendFailed", "payload": { "clientTempId": "...", "reason": "Failed to save" } }`

* **4.3. 메시지 상태 관리 및 UI 업데이트 (클라이언트):**
    * `sendMessage` 호출 시: `clientTempId` 생성, UI에 `status: SENDING`으로 추가.
    * `messageSentConfirmation` 수신: 해당 `clientTempId` 메시지를 `messageId`, `timestamp`로 업데이트, `status: SENT`.
    * `messageSendFailed` 수신: 해당 `clientTempId` 메시지를 `status: FAILED`.
    * `newMessage` 수신: UI 목록에 새 메시지 추가 (타인 메시지는 즉시 `status: SENT` 또는 `status: READ` 가능).

* **4.4. 채팅 기록 로딩 (클라이언트 - Firestore 직접 접근):**
    * `ChatRepository`에서 Firestore SDK 사용. 페이지네이션 구현.

* **4.5. 미디어 파일 업로드 (클라이언트 - Firebase Storage):**
    1.  사용자가 이미지/비디오/파일 선택.
    2.  `ChatRepository` 통해 Firebase Storage에 파일 업로드 (클라이언트 SDK 사용).
    3.  업로드 중 진행률 UI 표시 (ViewModel 통해). Firebase Storage SDK의 `OnProgressListener` 등을 활용하여 실시간 진행률 업데이트.
    4.  업로드 성공 시 Storage 다운로드 URL 획득.
    5.  획득한 URL과 미디어 메타데이터를 `payload`에 담아 `sendMessage` WebSocket 메시지 전송 (4.2. 참조).
    6.  업로드 실패 시 사용자에게 오류 알림.
    *   **참고:** 매우 큰 파일이나 불안정한 네트워크 환경에서는 청크(Chunk) 단위 업로드를 고려할 수 있으나, 구현 복잡도가 증가합니다. 초기에는 단일 파일 업로드를 우선으로 합니다.

* **4.6. FCM 푸시 알림:**
    * **Server:** `sendMessage` 처리 중, 수신자가 WebSocket에 연결되어 있지 않거나 특정 조건 만족 시 FCM 메시지 발송 결정.
        1.  수신자 User Document (Firestore)에서 FCM 토큰 조회.
        2.  Admin SDK 사용하여 데이터 메시지 발송 (페이로드: `chatId`, `senderName`, `messagePreview` 등).
    * **Client:**
        *   `FirebaseMessagingService`에서 데이터 메시지 수신.
        *   앱이 포그라운드이고 해당 채팅방 활성화 시: 알림 무시 또는 인앱 알림.
        *   앱이 백그라운드/종료 시: 시스템 트레이 알림 생성. 알림 클릭 시 해당 채팅방으로 이동.

**5. 보안 (Security)**

* **5.1. WebSocket 인증:** Firebase ID Token 검증.
* **5.2. Firestore 보안 규칙:**
    * `users/{userId}`: 본인만 `fcmToken` 쓰기 가능, 나머지는 인증된 사용자 읽기 가능.
    * `chats/{chatId}`: `members`에 포함된 사용자만 읽기/쓰기.
    * `messages/{messageId}`: `chatId`에 해당하는 `chats` 문서의 `members`에 포함된 사용자만 읽기. 서버(Admin SDK)만 쓰기 허용 (클라이언트 직접 쓰기 금지).
    * `storage.rules`: 인증된 사용자만 자신의 경로(`user/{uid}/...`)에 업로드 가능, 다운로드는 해당 메시지 접근 권한 있는 사용자만 가능하도록 (메타데이터 검증 등 복잡도 증가 가능, 초기에는 인증된 사용자 읽기 허용).
* **5.3. 서버 보안:** Admin SDK 서비스 계정 키 보안 관리.

**6. 확장성 및 성능 (Scalability & Performance)**

* **6.1. WebSocket 서버 (JSR 356 on Cloud Run with Docker):**
    * Cloud Run 자동 확장. Stateless 유지. `@OnMessage` 비동기 처리.
    * **Cloud Run 설정 시 `--session-affinity` 플래그 사용 권장** (WebSocket 연결이 동일 인스턴스에 유지되도록).
* **6.2. 클라이언트 (Android):** 메시지 목록 `LazyColumn` 최적화, 이미지 로딩 라이브러리(Coil/Glide) 활용.

**7. 오류 처리 및 로깅 (Error Handling & Logging)**

* **7.1. 서버:** `@OnError`, SLF4j/Logback, Cloud Logging.
* **7.2. 클라이언트:** OkHttp `onFailure`, Firestore 쿼리 실패 처리, UI 피드백.

**8. 배포 (Deployment)**

* **8.1. WebSocket 서버 (Docker & Cloud Run):**
    * **빌드:** Gradle 사용하여 실행 가능한 Fat Jar 빌드 (`./gradlew bootJar` 또는 유사 명령어).
    * **Dockerfile 작성:**
        ```Dockerfile
        # 1. JRE 기반 이미지 선택 (애플리케이션에 맞는 버전 선택)
        FROM openjdk:11-jre-slim 
        # 또는 FROM eclipse-temurin:11-jre-jammy 등

        # 2. 작업 디렉토리 설정
        WORKDIR /app

        # 3. 빌드된 Jar 파일 복사
        #    build.gradle에서 bootJar.archiveFileName.set("app.jar") 등으로 파일명 고정 권장
        COPY build/libs/*.jar app.jar 
        # 또는 COPY build/libs/your-server-app-1.0.0.jar app.jar

        # 4. (선택적) Firebase Admin SDK 서비스 계정 키 복사 (권장: Cloud Run 서비스 계정 권한 사용)
        # COPY path/to/your-service-account-key.json /app/service-account-key.json
        # ENV GOOGLE_APPLICATION_CREDENTIALS /app/service-account-key.json

        # 5. 애플리케이션이 리슨할 포트 노출 (Cloud Run이 이 포트로 트래픽 라우팅)
        EXPOSE 8080 

        # 6. 애플리케이션 실행 명령어
        CMD ["java", "-jar", "app.jar"]
        ```
    * **Docker 이미지 빌드:** `docker build -t gcr.io/[GCP_PROJECT_ID]/[SERVICE_NAME]:[TAG] .`
        * `[GCP_PROJECT_ID]`: 본인의 Google Cloud Project ID
        * `[SERVICE_NAME]`: Cloud Run 서비스 이름 (예: `chat-websocket-server`)
        * `[TAG]`: 이미지 버전 태그 (예: `v1.0.0`, `latest`)
    * **Docker 이미지 GCR/Artifact Registry에 푸시:**
        * `gcloud auth configure-docker` (최초 1회 또는 인증 필요시)
        * `docker push gcr.io/[GCP_PROJECT_ID]/[SERVICE_NAME]:[TAG]`
    * **Google Cloud Run 서비스 배포 (gcloud CLI):**
        ```bash
        gcloud run deploy [SERVICE_NAME] \
            --image gcr.io/[GCP_PROJECT_ID]/[SERVICE_NAME]:[TAG] \
            --platform managed \
            --region [YOUR_REGION] \ # 예: asia-northeast3
            --allow-unauthenticated \ # WebSocket 엔드포인트 자체는 공개, 인증은 내부 로직으로
            --port 8080 \ # Dockerfile에서 EXPOSE한 포트와 일치
            --set-env-vars GOOGLE_APPLICATION_CREDENTIALS="" \ # 서비스 계정 권한 사용 시 명시적으로 비워두거나 설정 안함
            --service-account [SERVICE_ACCOUNT_EMAIL] \ # Firestore/FCM 접근 권한 있는 서비스 계정
            --min-instances 0 \ # 비용 최적화 (트래픽 없을 시 0)
            --max-instances 5 \ # 최대 인스턴스 (부하에 맞게 조절)
            --session-affinity \ # WebSocket 사용 시 필수!
            --timeout 3600 # 초 단위, WebSocket 연결 유지 시간 고려 (기본값 및 최대값 확인)
        ```
        * **중요:** 서비스 계정 키 파일을 Docker 이미지에 포함하는 것보다, Cloud Run 서비스가 실행되는 서비스 계정(`[SERVICE_ACCOUNT_EMAIL]`)에 필요한 IAM 역할(예: `roles/datastore.user`, `roles/firebase.sdkAdmin`)을 부여하는 것이 훨씬 안전하고 권장되는 방식입니다. Admin SDK는 Cloud Run 환경에서 자동으로 해당 서비스 계정의 자격 증명을 사용합니다.
* **8.2. Firestore/Authentication/Storage:** Firebase 콘솔에서 설정.
* **8.3. 클라이언트 (Android):** Google Play Store 통해 배포.

**9. 클라이언트 개발 작업 목록 (세분화 - `feature_chat` 또는 `chat` 모듈 기준)**

* **[ ] 환경 설정 및 기본 라이브러리 연동:**
    * [ ] Firebase SDK (Auth, Firestore, Storage, FCM), OkHttp, Kotlinx Serialization, Hilt, Coroutines, ViewModel, Compose 관련 의존성 `build.gradle.kts`에 추가.
    * [ ] `google-services.json` 추가.
    * [ ] Hilt 기본 설정 (`@HiltAndroidApp`, `@AndroidEntryPoint` 등).
* **[ ] DI 모듈 설정 (Hilt):**
    * [ ] AppModule (OkHttpClient, Gson/Moshi), FirebaseModule (Firebase 서비스 인스턴스), WebSocketModule, ChatRepositoryModule, ViewModelModule.
* **[ ] 데이터 계층 (`feature_chat/data`):**
    * [ ] WebSocket DTOs, Firestore DTOs 정의.
    * [ ] `WebSocketListenerImpl.kt`: `callbackFlow` 사용하여 WebSocket 이벤트 Flow 생성.
    * [ ] `ChatRemoteDataSource.kt` (Interface), `ChatRemoteDataSourceImpl.kt`: WebSocket 메시지 송수신 로직, Firestore 데이터 로드/페이지네이션 로직, Firebase Storage 업로드 로직.
    * [ ] `ChatRepositoryImpl.kt`: `ChatRemoteDataSource` 사용하여 데이터 취합 및 가공, 도메인 모델로 변환, ViewModel에 Flow 제공.
    * [ ] Mapper 클래스들 (DTO <-> Domain, Domain <-> UI Model).
* **[ ] 도메인 계층 (`feature_chat/domain`):**
    * [ ] `ChatMessage.kt` (기존 모델 확장 또는 UI 특화 모델 별도 정의), `ChatRoom.kt`, `ChatUiEvent.kt` 등.
    * [ ] `ChatRepository.kt` (Interface).
    * [ ] (선택적) UseCases: `SendMessageUseCase.kt`, `LoadChatHistoryUseCase.kt`, `UploadMediaUseCase.kt`.
* **[ ] 프레젠테이션 계층 (`feature_chat/presentation`):**
    * [ ] `ChatViewModel.kt`: `ChatRepository` 의존, UI 상태 관리 (`StateFlow`), UI 이벤트 처리 (`SharedFlow`). WebSocket 연결/재연결 요청, 메시지 전송, 기록 로드, 미디어 업로드 로직 호출.
    * [ ] `ChatScreen.kt` (Composable):
        * 메시지 목록 (`LazyColumn`), 메시지 아이템 (내/상대방, 텍스트/미디어, 상태, 시간).
        * 메시지 입력창 (텍스트, 미디어 선택 버튼), 전송 버튼.
        * 미디어 선택기 연동 (Photo Picker, Document Picker).
        * 업로드 진행률 표시.
    * [ ] 공통 UI 컴포넌트 (`MessageItem.kt`, `ChatInput.kt`, `MediaPreview.kt`).
* **[ ] FCM 연동:**
    * [ ] `MyFirebaseMessagingService.kt` 구현.
    * [ ] FCM 토큰 관리 로직 (획득, 서버 전송/업데이트).
* **[ ] 네트워크 연결 상태 처리:**
    * [ ] `ConnectivityManager` 사용, Repository/ViewModel에서 상태 반영 및 재연결 로직 트리거.
    * [ ] WebSocket 연결 끊김 감지 시, 지수 백오프(Exponential Backoff) 전략을 사용하여 자동 재연결 시도.
    * [ ] 재연결 성공 시, WebSocket 인증 절차 (4.1.) 다시 수행.

**10. 서버 개발 작업 목록 (세분화 - JSR 356, Gradle, Docker, Cloud Run)**

* **[ ] 환경 설정 및 기본 라이브러리 연동 (`build.gradle.kts`):**
    * [ ] Java, Application 플러그인. JSR 356 API, Tomcat Embedded, Firebase Admin SDK, Jackson, SLF4j/Logback 의존성.
    * [ ] `bootJar` 태스크 설정 (실행 가능한 Jar 생성).
* **[ ] Firebase Admin SDK 초기화:**
    * [ ] `FirebaseApp.initializeApp()` 로직 (서비스 계정은 Cloud Run 환경 변수 또는 권한 상속으로 처리).
* **[ ] WebSocket 엔드포인트 (`ChatServerEndpoint.java`):**
    * [ ] `@ServerEndpoint("/chat")` 클래스.
    * [ ] `Session` 관리 (UserId-Session, ChatId-SessionSet 맵, 동시성 보장).
    * [ ] `@OnOpen`, `@OnClose`, `@OnError`, `@OnMessage` 구현.
    * [ ] `authenticate` 메시지 처리 (ID 토큰 검증).
    * [ ] `sendMessage` 메시지 처리 (JSON 파싱, 권한 검증, `newMessage` 브로드캐스팅, Firestore 저장 비동기 호출, FCM 발송 비동기 호출).
    * [ ] JSON (역)직렬화 (Jackson `ObjectMapper` 활용).
* **[ ] Firestore/FCM 연동 서비스 클래스:**
    * [ ] `FirestoreService.java`: 메시지 저장 로직.
    * [ ] `FcmService.java`: FCM 메시지 발송 로직.
* **[ ] 메인 애플리케이션 클래스 (`Main.java`):**
    * [ ] 임베디드 Tomcat 서버 설정 및 시작, WebSocket 엔드포인트 등록.
* **[ ] Dockerfile 작성 및 이미지 빌드/푸시.**
* **[ ] Cloud Run 배포 스크립트 또는 `gcloud` 명령어 준비.**
* **[ ] 로깅 설정 (`logback.xml`).**

**(이하 내용은 기존 문서와 유사하게 유지)**
* 테스트 (Client & Server)
* 문서화 (Client & Server)
