# Task: Integrate Firestore DB and Remove Mock Data for All Features

## Overview

This task involves connecting all feature modules (except Calendar) to the Firestore database according to the defined architecture and removing any existing mock data implementations.

**Key Reference Documents:**

*   **Schema:** [`firestore-schema.mdc`](mdc:.cursor/rules/firestore-schema.mdc) - Defines the target Firestore structure.
*   **Architecture:** [`architecture.mdc`](mdc:.cursor/rules/architecture.mdc) - Outlines Clean Architecture, MVVM, module structure, and design patterns.
*   **Rules Overview:** [`rule-refer-doc.mdc`](mdc:.cursor/rules/rule-refer-doc.mdc) - Provides context for all rule documents.

**Core Architectural Guidelines:**

*   **Clean Architecture:** Strictly adhere to layer separation (Domain, Data, Presentation) and dependency rules.
*   **Modularity:** Ensure changes are contained within the appropriate modules (`:domain`, `:data`, `:feature:*`, `:core:*`).
*   **Single Responsibility Principle (SRP):** Each class (ViewModel, UseCase, Repository, DataSource, Mapper) should have a single, well-defined responsibility.
*   **MVVM:** Follow the Model-View-ViewModel pattern within the presentation layer (`:feature:*` modules), utilizing `UiState` and event handling via ViewModels.

**Schema Evolution Strategy (Handling Missing Schema Info):**

If, during implementation of any step, required fields or collection structures are found to be missing or inadequately defined in [`firestore-schema.mdc`](mdc:.cursor/rules/firestore-schema.mdc):

## :data Module Structure Clarification

현재 구현된 데이터 모듈은 다음과 같은 구조로 정리됩니다:

```
data/src/main/java/com/example/data/
├── model/
│   ├── mapper/         # 도메인 모델 <-> DTO 변환 매퍼 (UserMapper.kt, FriendMapper.kt 등)
│   ├── remote/         # 원격 데이터 관련 모델
│   │   ├── user/       # 사용자 관련 DTO
│   │   ├── media/      # 미디어 관련 DTO
│   │   ├── schedule/   # 일정 관련 DTO
│   │   ├── project/    # 프로젝트 관련 DTO  
│   │   └── chat/       # 채팅 관련 DTO
│   └── local/          # 로컬 저장소용 엔티티 (Room DB 등)
├── datasource/
│   ├── remote/         # 원격 데이터 소스 인터페이스 및 구현체
│   │   ├── auth/       # 인증 관련
│   │   ├── user/       # 사용자 관련
│   │   ├── friend/     # 친구 관련
│   │   ├── project/    # 프로젝트 관련
│   │   └── chat/       # 채팅 관련
│   └── local/          # 로컬 데이터 소스 (필요시)
├── repository/         # 도메인 레포지토리 인터페이스의 구현체
├── db/                 # 데이터베이스 관련 클래스
└── di/                 # 의존성 주입 모듈
    ├── DataSourceModule.kt   # 데이터소스 DI
    ├── MapperModule.kt       # 매퍼 DI
    ├── RepositoryModule.kt   # 레포지토리 DI
    └── FirebaseModule.kt     # Firebase 관련 DI
```

**핵심 구성요소 요약:**
- **DTO**: Firestore 문서 구조에 맞는 데이터 클래스 (`model/remote/` 하위 디렉토리별 배치)
- **Mapper**: DTO ↔ 도메인 모델 변환 로직 (`model/mapper/`)
- **Entity**: 로컬 DB 저장용 엔티티 클래스 (`model/local/`)
- **DataSource**: 원격/로컬 저장소 접근 인터페이스 및 구현 (`datasource/`)
- **Repository**: 데이터 조회/저장 로직 구현체 (`repository/`)
- **DI**: Hilt 의존성 주입 모듈 (`di/`)

모든 기능 구현 시 이 구조를 따르며, 각 기능별 디렉토리를 적절히 구성합니다.

---

## Phase 1: Authentication (`:feature:auth`)

- [x] 1.1 **Domain Layer**: Define/Verify `User` model in `:domain:model`.
- [x] 1.2 **Domain Layer**: Define/Verify `AuthRepository` interface in `:domain:repository` (e.g., `login`, `signUp`, `logout`, `checkSession`).
- [x] 1.3 **Domain Layer**: Define/Verify `UserRepository` interface in `:domain:repository` (e.g., `createUserProfile`, `getUserProfile`, `updateUserProfile`, `checkNicknameAvailability`).
- [x] 1.4 **Domain Layer**: Define/Verify relevant UseCases (e.g., `LoginUseCase`, `SignUpUseCase`, `GetUserProfileUseCase`) in `:domain:usecase`.
- [x] 1.5 **Data Layer**: Define `UserDto` in `:data:remote/dto` mapping to `users` collection.
- [x] 1.6 **Data Layer**: Define `UserMapper` in `:data:remote/mapper` (UserDto <-> User).
- [x] 1.7 **Data Layer**: Implement `AuthRemoteDataSource` in `:data:remote/datasource/auth` using Firebase Auth SDK.
- [x] 1.8 **Data Layer**: Implement `UserRemoteDataSource` in `:data:remote/datasource/user` for Firestore `users` collection interactions.
- [x] 1.9 **Data Layer**: Implement `AuthRepositoryImpl` in `:data:repository` using `AuthRemoteDataSource`.
- [x] 1.10 **Data Layer**: Implement `UserRepositoryImpl` in `:data:repository` using `UserRemoteDataSource` and `UserMapper`.
- [x] 1.11 **Data Layer**: Configure Hilt DI modules in `:data:repository/di` for Auth/User Repositories and DataSources.
- [x] 1.12 **Presentation Layer**: Update `AuthViewModel`, `SignUpViewModel`, etc. in `:feature:auth/viewmodel` to use respective UseCases.
- [x] 1.13 **Presentation Layer**: Update UI Composables in `:feature:auth/ui` to reflect `UiState` from ViewModels.
- [x] 1.14 **Presentation Layer**: Remove any mock data generation/injection related to Auth/User in `:feature:auth`.
- [ ] 1.15 **Testing**: Add/Update unit tests for Auth/User UseCases, Repositories, DataSources.

## Phase 2: Friends (`:feature:friends`)

- [x] 2.1 **Domain Layer**: Define/Verify `FriendRelationship` model in `:domain:model`.
- [x] 2.2 **Domain Layer**: Define/Verify `FriendRepository` interface in `:domain:repository` (e.g., `getFriendList`, `sendFriendRequest`, `acceptFriendRequest`, `removeFriend`).
- [x] 2.3 **Domain Layer**: Define/Verify relevant UseCases (e.g., `GetFriendsUseCase`, `SendFriendRequestUseCase`) in `:domain:usecase`.
- [x] 2.4 **Data Layer**: Define `FriendRelationshipDto` in `:data:remote/dto` mapping to `users/{userId}/friends` subcollection.
- [x] 2.5 **Data Layer**: Define `FriendMapper` in `:data:remote/mapper` (FriendRelationshipDto <-> FriendRelationship).
- [x] 2.6 **Data Layer**: Implement `FriendRemoteDataSource` in `:data:remote/datasource/friend`.
- [x] 2.7 **Data Layer**: Implement `FriendRepositoryImpl` in `:data:repository`.
- [x] 2.8 **Data Layer**: Update Hilt DI modules in `:data:repository/di`.
- [x] 2.9 **Presentation Layer**: Update `FriendsViewModel` in `:feature:friends/viewmodel` to use UseCases.
- [x] 2.10 **Presentation Layer**: Update UI Composables in `:feature:friends/ui`.
- [x] 2.11 **Presentation Layer**: Remove mock friend data in `:feature:friends`.
- [ ] 2.12 **Testing**: Add/Update unit tests for Friend UseCases, Repository, DataSource.

## Phase 3: Profile (`:feature:profile`)

- [x] 3.1 **Domain Layer**: Ensure `UserRepository` interface (`getUserProfile`, `updateUserProfile`) is sufficient (from Phase 1).
- [x] 3.2 **Domain Layer**: Ensure `User` model is sufficient.
- [x] 3.3 **Domain Layer**: Define/Verify `UpdateUserProfileUseCase` in `:domain:usecase`.
- [x] 3.4 **Data Layer**: Ensure `UserRemoteDataSource`, `UserRepositoryImpl`, DTO, Mapper are sufficient (from Phase 1).
- [x] 3.5 **Presentation Layer**: Update `ProfileViewModel` in `:feature:profile/viewmodel` to use UseCases.
- [x] 3.6 **Presentation Layer**: Update UI Composables in `:feature:profile/ui`.
- [x] 3.7 **Presentation Layer**: Remove mock profile data in `:feature:profile`.
- [ ] 3.8 **Testing**: Add/Update unit tests related to profile functionality.

## Phase 4: Project (`:feature:project`) - *High Complexity*

- [x] 4.1 **Domain Layer**: Define/Verify `Project`, `Member`, `Role`, `Category`, `Channel` models in `:domain:model`.
- [x] 4.2 **Domain Layer**: Define/Verify `ProjectRepository` interface in `:domain:repository` (covering project CRUD, member management, role management, category/channel management).
- [x] 4.3 **Domain Layer**: Define/Verify multiple relevant UseCases (e.g., `CreateProjectUseCase`, `GetProjectDetailsUseCase`, `AddProjectMemberUseCase`, `CreateChannelUseCase`) in `:domain:usecase`.
- [x] 4.4 **Data Layer**: Define DTOs (`ProjectDto`, `MemberDto`, etc.) in `:data:remote/dto` mapping to `projects` collection and subcollections.
- [x] 4.5 **Data Layer**: Define Mappers for each DTO <-> Model pair.
- [x] 4.6 **Data Layer**: Implement `ProjectRemoteDataSource` in `:data:remote/datasource/project`.
- [x] 4.7 **Data Layer**: Implement `ProjectRepositoryImpl` in `:data:repository`.
- [x] 4.8 **Data Layer**: Update Hilt DI modules in `:data:repository/di`.
- [x] 4.9 **Presentation Layer**: Update relevant ViewModels (e.g., `ProjectCreationViewModel`, `ProjectSettingsViewModel`, `ChannelListViewModel`) in `:feature:project/viewmodel`.
- [x] 4.10 **Presentation Layer**: Update UI Composables in `:feature:project/ui`.
- [x] 4.11 **Presentation Layer**: Remove mock project data in `:feature:project`.
- [ ] 4.12 **Testing**: Add/Update unit tests for Project UseCases, Repository, DataSource.

## Phase 5: Chat (`:feature:chat`) - *Real-time Complexity*

- [x] 5.1 **Domain Layer**: Define/Verify `Message` model in `:domain:model`. Define `DmChannel` or similar if needed.
- [x] 5.2 **Domain Layer**: Define/Verify `ChatRepository` (or `DmRepository`, `ChannelMessageRepository`) interface in `:domain:repository` (e.g., `getMessages`, `sendMessage`, `listenForNewMessages`). Consider Flow for real-time updates.
- [x] 5.3 **Domain Layer**: Define/Verify relevant UseCases (e.g., `GetMessagesUseCase`, `SendMessageUseCase`) in `:domain:usecase`.
- [x] 5.4 **Data Layer**: Define `MessageDto` in `:data:remote/dto`.
- [x] 5.5 **Data Layer**: Define `MessageMapper`.
- [x] 5.6 **Data Layer**: Implement `ChatRemoteDataSource` (handling DM, Channel, Schedule messages, potentially using `addSnapshotListener`).
- [x] 5.7 **Data Layer**: Implement `ChatRepositoryImpl` (including logic to update `lastMessage` in project channels).
- [x] 5.8 **Data Layer**: Update Hilt DI modules in `:data:repository/di`.
- [x] 5.9 **Presentation Layer**: Update relevant ViewModels in `:feature:chat/viewmodel` to use UseCases.
- [x] 5.10 **Presentation Layer**: Update UI Composables in `:feature:chat/ui`.
- [x] 5.11 **Presentation Layer**: Remove mock chat data in `:feature:chat`.
- [ ] 5.12 **Testing**: Add/Update unit tests for Chat UseCases, Repository, DataSource.

## Summary of Remaining Tasks

현재 Firestore DB 통합 작업의 대부분이 완료되었으며, 남은 작업은 다음과 같습니다:

1. **테스트 코드 작성**: 각 기능별 UseCase, Repository, DataSource에 대한 단위 테스트 추가
   - [ ] Auth/User 관련 테스트 (1.15)
   - [ ] Friend 관련 테스트 (2.12)
   - [ ] Profile 관련 테스트 (3.8)
   - [ ] Project 관련 테스트 (4.12)
   - [ ] Chat 관련 테스트 (5.12)

2. **유지 보수 및 코드 품질 향상**:
   - [ ] 코드 리팩토링
   - [ ] 리포지토리 및 데이터소스 구현체의 일관성 검토
   - [ ] 오류 처리 및 예외 상황 검증

3. **빌드 에러 해결 (UI 및 ViewModel)**:
    - [x] `:feature:feature_dev` 모듈 에러 해결:
        - [x] `DevMenuScreen.kt:65` - `AppRoutes.SPLASH` 참조 에러
        - [x] `DevMenuScreen.kt:66` - `AppRoutes.LOGIN` 참조 에러
        - [x] `DevMenuScreen.kt:67` - `AppRoutes.SIGN_UP` 참조 에러
        - [x] `DevMenuScreen.kt:68` - `AppRoutes.FIND_PASSWORD` 참조 에러
        - [x] `DevMenuScreen.kt:74` - `AppRoutes.ADD_PROJECT` 참조 에러
        - [x] `DevMenuScreen.kt:75` - `AppRoutes.SET_PROJECT_NAME` 참조 에러
        - [x] `DevMenuScreen.kt:76` - `AppRoutes.JOIN_PROJECT` 참조 에러
        - [x] `DevMenuScreen.kt:95` - `AppRoutes.EDIT_PROFILE` 참조 에러
        - [x] `DevMenuScreen.kt:96` - `AppRoutes.CHANGE_PASSWORD` 참조 에러
        - [x] `DevMenuScreen.kt:99` - `AppRoutes.chat` 참조 에러
        - [x] `DevMenuScreen.kt:113` - `AppRoutes.Search.ROOT` 접근성 에러 (private)
    - [x] `:feature:feature_main` 모듈 에러 해결:
        - [x] `MainScreen.kt:39` - `MainViewModel` 참조 에러 및 타입 추론 불가
        - [x] `MainScreen.kt:47` - `setActiveChildNavController` 참조 에러
        - [x] `MainScreen.kt:49` - `getActiveChildNavController` 참조 에러
        - [x] `MainScreen.kt:50` - `setActiveChildNavController` 참조 에러 (중복)
        - [x] `MainScreen.kt:73` - `AppRoutes.Calendar.ROOT` 참조 에러
        - [x] `MainScreen.kt:76` - `composable` 및 `AppRoutes.Home.ROOT` 참조 에러
        - [x] `MainScreen.kt:77` - `@Composable` 부적절한 호출 위치
        - [x] `MainScreen.kt:82` - `composable` 및 `AppRoutes.Friends.ROOT` 참조 에러
        - [x] `MainScreen.kt:83` - `@Composable` 부적절한 호출 위치
        - [x] `MainScreen.kt:89` - `composable` 및 `AppRoutes.Profile.ROOT` 참조 에러
        - [x] `MainScreen.kt:90` - `@Composable` 부적절한 호출 위치
        - [x] `ui/ProfileScreen.kt:65` - `AppRoutes.EDIT_PROFILE` 참조 에러
        - [x] `ui/ProfileScreen.kt:67` - `AppRoutes.CHANGE_PASSWORD` 참조 에러
        - [x] `ui/ProfileScreen.kt:78` - `AppRoutes.LOGIN` 참조 에러
        - [x] `ui/calendar/CalendarScreen.kt:24` - `LocalNavController` 참조 에러
        - [x] `ui/calendar/CalendarScreen.kt:76` - `LocalNavController` 참조 에러
        - [x] `ui/calendar/CalendarScreen.kt:101` - `getResultFlow` 참조 에러 및 파라미터 타입 추론 불가
    - [x] `:feature:feature_auth` 모듈 에러 해결:
        - [x] `ui/LoginScreen.kt:62` - `AppRoutes.SIGN_UP` 참조 에러
        - [x] `ui/LoginScreen.kt:63` - `AppRoutes.FIND_PASSWORD` 참조 에러
        - [x] `ui/SignUpScreen.kt:58` - `AppRoutes.LOGIN` 참조 에러
        - [x] `ui/SplashScreen.kt:39` - `AppRoutes.LOGIN` 참조 에러
    - [x] `:feature:feature_friends` 모듈 에러 해결:
        - [x] `ui/FriendScreen.kt:53` - `AppRoutes.chat` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:106` - `FriendRequestItem` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:116` - `it` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:117` - 파라미터 타입 추론 불가
        - [x] `ui/AcceptFriendsScreen.kt:118` - `@Composable` 부적절한 호출 위치
        - [x] `ui/AcceptFriendsScreen.kt:120` - `userId` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:121` - `userId` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:123` - `@Composable` 부적절한 호출 위치
        - [x] `ui/AcceptFriendsScreen.kt:133` - `FriendRequestItem` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:146` - `profileImageUrl` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:150` - `userName` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:156` - `userName` 참조 에러
        - [x] `ui/AcceptFriendsScreen.kt:186` - 파라미터 타입 추론 불가
        - [x] `ui/AcceptFriendsScreen.kt:187-189` - `FriendRequestItem` 다수 참조 에러
        - [x] `ui/AddFriendDialog.kt:44` - `ShowSnackbar` 참조 에러
        - [x] `ui/AddFriendDialog.kt:47` - `message` 참조 에러
        - [x] `viewmodel/AddFriendViewModel.kt:66` - `searchUsers` 참조 에러
        - [x] `viewmodel/AddFriendViewModel.kt:70` - `isNotEmpty` 참조 에러
        - [x] `viewmodel/AddFriendViewModel.kt:73` - `Flow.first()` 사용 에러
        - [x] `viewmodel/AddFriendViewModel.kt:74` - `id` 참조 에러
    - [x] `:feature:feature_schedule` 모듈 에러 해결:
        - [x] `ui/AddScheduleScreen.kt:20` - `LocalNavController` 참조 에러
        - [x] `ui/AddScheduleScreen.kt:51` - `LocalNavController` 참조 에러
        - [x] `ui/EditScheduleScreen.kt:41` - `LocalNavController` 참조 에러
        - [x] `ui/EditScheduleScreen.kt:60` - `LocalNavController` 참조 에러
        - [x] `ui/EditScheduleScreen.kt:181` - `size` 참조 에러
        - [x] `ui/ScheduleDetailScreen.kt:20` - `livedata` 참조 에러
        - [x] `ui/ScheduleDetailScreen.kt:21` - `LocalNavController` 참조 에러
        - [x] `ui/ScheduleDetailScreen.kt:45` - `LocalNavController` 참조 에러
        - [x] `ui/ScheduleDetailScreen.kt:46` - `getResultFlow` 참조 에러
        - [x] `viewmodel/ScheduleDetailViewModel.kt:66` - `MainScreens` 참조 에러
    - [ ] `:feature:feature_project` 모듈 에러 해결:
        - [ ] `members/ui/MemberListScreen.kt:127` - `projectId` 파라미터/참조 에러
        - [ ] `members/ui/MemberListScreen.kt:129` - `onMemberAdded` 및 다수 파라미터 누락 에러
        - [ ] `members/viewmodel/MemberListViewModel.kt:125` - `roles`, `it` 참조 에러
        - [ ] `ui/SetProjectNameScreen.kt:52` - `navigateToSelectProjectType` 참조 에러
    - [ ] `:app` 모듈 에러 해결: 
        - [ ] 다양한 네비게이션 관련 참조 에러
        - [ ] 통합 관련 오류