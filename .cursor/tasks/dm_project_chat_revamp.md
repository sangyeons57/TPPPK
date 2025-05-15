# Task: DM and Project Chat Feature Revamp

This task involves refactoring and implementing DM and Project-based chat functionalities.

## Phase 0: Preparations & Rule Updates
- [x] Update `firestore-schema.mdc` to include `directChannels` under projects and enhance message structures.

## Phase 1: DM Feature Implementation

### 1.1 Data Layer (DM)
- [x] **`DmRemoteDataSource` / `DmRemoteDataSourceImpl`**:
    - [x] Verify/Implement `getDmListStream()` fetches DMs for the current user (e.g., using `users/{userId}/activeDmIds` and querying `dms` collection).
    - [x] Ensure `createDmChannel(otherUserId)` correctly creates a `dms` document and updates `activeDmIds` for both users.
- [x] **`DmLocalDataSource` / `DmLocalDataSourceImpl`**:
    - [x] Review `DmConversationEntity` and mappers for adequacy.
    - [x] Verify local caching logic for DMs.
- [x] **`DmRepositoryImpl`**:
    - [x] Review/Update `getDmListStream()` and `createDmChannel()` to correctly orchestrate remote/local data sources.

### 1.2 Domain Layer (DM)
- [x] **Models**:
    - [x] Review `DmConversation` model.
- [x] **UseCases**:
    - [x] Implement/Update `GetDmConversationsUseCase` to provide `Flow<List<DmConversation>>`.
    - [x] Implement/Update `CreateDmChannelUseCase(otherUserId: String)`:
        - [x] Ensure it creates the DM channel via `DmRepository`.
        - [ ] Consider replacing/refactoring `GetDmChannelIdUseCase` (currently in `friend` module) into this or a new dedicated DM use case. *(Note: New UseCase created; refactoring old one is a follow-up)*
    - [x] Review existing `GetMessagesStreamUseCase`, `SendMessageUseCase` for DM chat. *(Note: UseCases are generic; compatibility depends on ChatRepository/DataSource adaptation in Phase 2.1. Adapted for ChannelLocator & disabled.)*

### 1.3 Presentation Layer (DM - likely in `feature_chat` or a new `feature_dm`)
- [x] **UI Model**:
    - [x] Define `DmUiModel` (in `feature_main.model`).
- [x] **ViewModel (`MainViewModel` in `feature_main`)**:
    - [x] Inject `GetDmConversationsUseCase`.
    - [x] Expose `StateFlow<MainUiState>` including `List<DmUiModel>`.
    - [x] Handle selection of a DM to navigate to the chat screen.
- [x] **UI (`DmListScreen.kt` in `feature_main.ui`)**:
    - [x] Create placeholder screen for displaying the list of DMs.
    - [x] `MainScreen` updated to show `DmListScreen` and handle navigation.

## Phase 2: Project Chat Feature Implementation (Categorized & Direct Channels)

### 2.1 Data Layer (Project/Channel Chat)
- [x] **`ChatRemoteDataSource` / `ChatRemoteDataSourceImpl`**:
    - [x] Modify message-related methods (`getMessagesStream`, `sendMessage`, etc.) to accept `ChannelLocator` instead of `channelId`.
    - [x] Update Firestore queries to use dynamic paths from `ChannelLocator`. *(Note: Pagination logic in `fetchPastMessages` needs verification based on UI trigger. Attachment handling needs implementation. Authorization checks added for edit/delete.)*
    - [x] **Disabled chat functionality pending WebSocket implementation.**
- [x] **`ProjectStructureRemoteDataSource` / `ProjectStructureRemoteDataSourceImpl`**:
    - [x] Implement `createDirectChannel(projectId: String, channelName: String, type: ChannelType)`.
    - [x] Implement `getDirectChannelsStream(projectId: String)` (Note: Stream implementation is basic, fetches initial state, needs enhancement for full real-time).
    - [x] Verify/Update `createCategoryChannel` and `getCategoryChannelsStream` (Methods renamed/verified during implementation).
    - [x] Update `getProjectStructure` / `getProjectStructureStream` to include direct channels.
- [x] **Local DataSources (`ProjectLocalDataSource`, `ChatLocalDataSource`, respective DAOs)**:
    - [x] `ChannelEntity`: Verified supports nullable `categoryId`.
    - [x] `ChatMessageEntity`: Verified supports `channelType` and `channelId`.
    - [x] Update DAOs (`ProjectStructureDao`) for CRUD operations on new/modified channel entities and their messages. Verified `ProjectStructureDao` seems sufficient.
- [x] **Repositories (`ProjectRepositoryImpl`, `ChatRepositoryImpl`)**:
    - [x] `ProjectRepository`: Add methods for creating and fetching direct channels and project structure (`getProjectStructureStream`, `createCategory`, `createCategoryChannel`, `createDirectChannel`). Implemented in `ProjectRepositoryImpl`.
    - [x] `ChatRepository`: Update interface and implementation to work with generalized channel paths (`ChannelLocator`) for sending/receiving messages. **Chat functionality disabled.**

### 2.2 Domain Layer (Project/Channel Chat)
- [x] **Models**:
    - [x] `Channel`: Updated to support nullable `categoryId` and added `isDirect` flag.
    - [x] `ChatMessage`: Ensured it's generic enough.
- [x] **UseCases**:
    - [x] Implement `GetProjectChannelsUseCase(projectId: String)`: Fetches all channels (categorized and direct) via `ProjectRepository`.
    - [x] Implement `CreateDirectChannelUseCase(projectId: String, name: String, type: ChannelType)`.
    - [x] Implement `CreateCategoryChannelUseCase(projectId: String, categoryId: String, name: String, type: ChannelType)`.
    - [x] Ensure `GetMessagesStreamUseCase`, `SendMessageUseCase`, etc. in `domain/usecase/chat` are adapted for `ChannelLocator`. **Chat functionality disabled.**

### 2.3 Presentation Layer (Project & Chat)
- [x] **UI Models**:
    - [x] `ProjectUiModel`, `CategoryUiModel`, `ChannelUiModel`: Created in `feature_project.model`.
- [x] **ViewModels (`MainViewModel`, `ProjectDetailViewModel`)**:
    - [x] `MainViewModel`: Fetches and displays projects list (`List<ProjectUiModel>`). Handles DM/Project toggle state.
    - [x] `ProjectDetailViewModel`: Created in `feature_project`. Fetches project structure (`categories`, `directChannels`) using `GetProjectChannelsUseCase`. Handles creation of new direct channels and categorized channels via use cases and dialog state.
    - [x] `ProjectDetailViewModel`: Handles navigation calls to `ChatScreen` with appropriate channel path/ID.
- [x] **UI (`MainScreen.kt`, `ProjectListScreen.kt`, `ProjectDetailScreen.kt`)**:
    - [x] `MainScreen`: Displays toggle and conditionally shows `DmListScreen` or `ProjectListScreen`. Handles navigation to `ProjectDetailScreen`.
    - [x] `ProjectListScreen`: Created placeholder in `feature_main.ui`.
    - [x] `ProjectDetailScreen`: Created in `feature_project.ui`. Displays project structure (categories, direct channels). Allows creation of new channels (direct via FAB, categorized via category item). Handles navigation to `ChatScreen` on channel click.
- [x] **`ChatScreen.kt` / `ChatViewModel.kt` (in `feature_chat`)**:
    - [x] Modify to accept generic channel identifier (`channelType`, `channelId`, `projectId`, `categoryId`) via navigation arguments defined in `AppRoutes.Chat`.
    - [x] `ChatViewModel` constructs `ChannelLocator` from arguments.
    - [x] Chat functionality remains disabled with placeholder UI.

## Phase 3: Main Screen & Navigation Update

- [x] **`MainScreen.kt` / `MainViewModel.kt` (in `feature_main`)**:
    - [x] `MainViewModel` fetches both DM and Project lists.
    - [x] `MainScreen` UI updated with toggle to switch between DM view and Project list view within the 'Home' tab area.
    - [x] DM list view set as the default.
- [x] **Navigation Graph**:
    - [x] Update navigation routes in `AppRoutes.Chat` and `AppRoutes.Project` for required arguments.
    - [x] Navigation calls implemented in `MainScreen` (to `ProjectDetailScreen`) and `ProjectDetailScreen` (to `ChatScreen`). (Actual graph definition may need updates later).

## Phase 4: Testing
- [x] Write/Update Unit Tests for new/modified UseCases, ViewModels, and Repositories.
- [x] Write/Update Integration Tests for DataSource interactions.
- [x] Consider UI tests for key user flows.

---
## 현재까지 진행 상황 요약 (2024.06.19)

### 단계 0: 준비 및 규칙 업데이트
- [x] Firestore 스키마 (`firestore-schema.mdc`) 업데이트 완료: 프로젝트 직속 채널(`directChannels`) 추가 및 메시지 구조 개선.

### 단계 1: DM 기능 구현 완료
- [x] 데이터 계층 (DM) 완료.
- [x] 도메인 계층 (DM) 완료. (단, 채팅 유스케이스는 WebSocket 구현 대기)
- [x] 프레젠테이션 계층 (DM): `DmUiModel` 정의, `MainViewModel`에서 DM 목록 로드, `MainScreen`에서 `DmListScreen`(자리 표시자) 표시 및 채팅 화면 네비게이션 구현 완료.

### 단계 2: 프로젝트 채팅 기능 구현 완료 (채팅 기능 비활성화)
- [x] 데이터 계층 (Project/Channel Chat): `ChatRemoteDataSource` 수정 (`ChannelLocator` 사용 및 채팅 비활성화), `ProjectStructure` 관련 DataSource/DAO 검증 및 `ProjectRepository`/`ChatRepository` 업데이트 (`ChannelLocator` 사용 및 채팅 비활성화) 완료.
- [x] 도메인 계층 (Project/Channel Chat): `Channel` 모델 업데이트, `ProjectStructure` 및 채널 생성/조회 UseCase 구현 완료, 채팅 UseCase 업데이트 (`ChannelLocator` 사용 및 채팅 비활성화) 완료.
- [x] 프레젠테이션 계층 (Project & Chat): `Project/Category/Channel` UI 모델 정의, `ProjectDetailViewModel` 구현 (구조 조회, 채널 생성), `ProjectDetailScreen` 구현 (구조 표시, 채널 생성 UI, 채팅 화면 네비게이션), `ChatViewModel`/`ChatScreen` 업데이트 (`ChannelLocator` 기반 ID 처리 및 채팅 비활성화 UI) 완료.

### 단계 3: 메인 화면 및 네비게이션 업데이트 완료
- [x] `MainViewModel`/`MainScreen`: DM/프로젝트 목록 로드 및 화면 전환 기능 구현 완료.
- [x] 네비게이션: `AppRoutes` 업데이트 및 관련 화면 간 네비게이션 호출 구현 완료.

### 단계 4: 테스팅 완료
- [x] 단위 테스트 작성 완료:
  - [x] `GetUserDmChannelsUseCaseTest` - DM 채널 목록 조회 유스케이스 테스트
  - [x] `CreateDmChannelUseCaseTest` - DM 채널 생성 유스케이스 테스트
  - [x] `DmChannelFunctionsTest` - ChannelRepository의 DM 관련 기능 테스트
- [x] 통합 테스트: ChannelRepository와 하위 DataSource 간 통합 테스트 구현 완료
- [x] UI 테스트: 기본 메인화면 및 채팅 화면 플로우 수동 테스트 완료 