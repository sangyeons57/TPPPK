**Room DB 분리 및 Firebase 캐시 우선 활용 계획**

**버전:** 1.0
**작성일:** 2025년 5월 12일 (Current date: 2025-05-12)
**작성자:** Gemini (AI Assistant)

**1. 목표 (Goal)**

*   기존 Android 애플리케이션에 통합된 Room DB의 역할을 축소하고, Firestore의 로컬 캐시 기능을 우선적으로 활용하여 오프라인 지원 및 데이터 지속성을 관리한다.
*   Room DB 코드를 완전히 제거하기보다는, 향후 Firestore 캐시의 한계점 (예: 복잡한 쿼리, 대용량 데이터 오프라인 접근 등)이 명확해질 경우 다시 활성화할 수 있도록 기능적으로 분리(decouple)하는 것을 목표로 한다.
*   채팅 기능 구현(`websocket-server.md` 참조)과 관련하여, 초기에는 Firestore 캐시와 WebSocket을 통해 실시간 및 과거 메시지를 처리한다.

**2. 현황 및 동기 (Current Status & Motivation)**

*   현재 프로젝트는 Room DB를 사용하여 일부 데이터를 로컬에 저장하고 있다.
*   새로운 채팅 기능은 WebSocket(실시간)과 Firestore(영구 저장 및 과거 기록 로드)를 중심으로 설계되었다. Firestore는 기본적으로 오프라인 캐싱을 지원한다.
*   Room DB를 즉시 제거하는 대신 기능적으로 분리함으로써, 개발 복잡성을 줄이고 Firebase 캐시의 실제 성능과 한계를 먼저 평가할 수 있는 유연성을 확보한다.

**3. 분리 전략 (Decoupling Strategy)**

*   **3.1. Firestore 캐시 활성화 및 의존:**
    *   Firebase Firestore SDK에서 제공하는 디스크 지속성(disk persistence)을 명시적으로 활성화한다. (`FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true)`)
    *   채팅 메시지, 사용자 정보 등 주요 데이터는 Firestore를 통해 조회하고, Firestore 캐시가 오프라인 시 데이터 접근을 담당하도록 한다.
*   **3.2. Room DB 관련 코드 주석 처리 또는 기능 플래그 도입:**
    *   기존 Repository 및 DataSource에서 Room DB와 상호작용하는 코드 경로를 주석 처리하거나, 빌드 구성(Build Variant) 또는 기능 플래그(Feature Flag)를 사용하여 비활성화한다.
    *   DAO 인터페이스, Entity 클래스, Database 클래스 자체는 삭제하지 않고 유지한다.
    *   Room DB 초기화 코드 (`Room.databaseBuilder(...)`) 호출 부분을 비활성화한다.
*   **3.3. 데이터 흐름 변경:**
    *   ViewModel 및 UseCase는 Firestore를 직접 사용하는 Repository (또는 Firestore 전용 DataSource를 통하는 Repository)로부터 데이터를 받도록 수정한다.
    *   Room DB를 통해 데이터를 읽거나 쓰는 로직을 Firestore 기반 로직으로 대체하거나 우회한다.
*   **3.4. 마이그레이션 (선택적):**
    *   만약 Room DB에만 존재하는 중요 데이터가 있다면, 앱 업데이트 시 Firestore로 일회성 마이그레이션을 수행하는 로직을 고려할 수 있다. 단, 이번 분리 작업의 주 목적은 기능 중단이므로, 필수적이지 않다면 마이그레이션은 최소화하거나 다음 단계로 연기한다.
*   **3.5. 테스트:**
    *   Room DB 비활성화 후에도 앱의 주요 기능(특히 Firestore 캐시에 의존하는 부분)이 오프라인 상태에서 정상적으로 동작하는지 테스트한다. (예: 메시지 목록 조회, 사용자 프로필 조회 등)

**4. Room DB 재활성화 가능성 대비 (Preparing for Potential Re-activation)**

*   DAO, Entity, Database 클래스를 삭제하지 않고 유지함으로써, 향후 필요시 코드를 다시 활성화하고 데이터 마이그레이션(Room 버전 관리)을 수행할 수 있는 기반을 남겨둔다.
*   분리 작업 중 발생하는 주요 변경 사항 및 결정 사항을 이 문서에 기록하여, 미래의 개발자가 컨텍스트를 이해하고 Room DB를 다시 통합하는 작업을 용이하게 한다.

**5. 단계별 실행 계획 (Phased Implementation)**

*   **단계 1: 분석 및 식별**
    *   [ ] 현재 Room DB를 사용하는 모든 모듈, Repository, DataSource, UseCase, ViewModel 목록화.
    *   [ ] 각 컴포넌트에서 Room DB가 어떤 데이터(Entity)를 어떤 방식(CRUD)으로 사용하는지 분석.
    *   [ ] Firestore로 대체 가능한 부분과 Firestore 캐시로 커버될 수 있는 오프라인 시나리오 식별.

*   **1.1. 주요 엔티티 분석 결과 및 변경 사항 (User, Project)**

    *   **UserEntity (사용자 정보)**
        *   **DAO:** `UserDao`
        *   **Entity:** `UserEntity`
        *   **LocalDataSource:** `UserLocalDataSource` / `UserLocalDataSourceImpl` (내부적으로 `UserDao` 사용)
        *   **Repository:** `UserRepositoryImpl`
            *   **기존 Room DB 사용 방식:** 분석 결과, `UserRepositoryImpl`은 `UserLocalDataSource`를 직접적으로 거의 사용하지 않고, 주로 `UserRemoteDataSource`에 의존하고 있었음.
            *   **분리 작업 진행 상황:**
                *   `DatabaseModule.kt`에서 `provideUserDao` 주석 처리 완료.
                *   `DataSourceModule.kt`에서 `UserLocalDataSource`에 대한 Hilt 바인딩이 없거나 비활성 상태임을 확인.
                *   Firestore 디스크 지속성 활성화 완료 (`FirebaseModule.kt`). `UserRemoteDataSource`를 통해 가져온 사용자 데이터는 Firestore 캐시 활용.
        *   **주요 영향 컴포넌트:** `LoginViewModel`, `SignUpViewModel`, `ProfileViewModel`, `SettingViewModel`, 다수 인증 관련 UseCase. `UserLocalDataSource`를 사용하지 않았으므로 이들 컴포넌트에 대한 직접적인 수정은 적을 것으로 예상.

    *   **ProjectEntity (프로젝트 정보)**
        *   **DAO:** `ProjectDao`
        *   **Entity:** `ProjectEntity`
        *   **LocalDataSource:** `ProjectLocalDataSource` / `ProjectLocalDataSourceImpl` (내부적으로 `ProjectDao` 사용)
        *   **Repository:** `ProjectRepositoryImpl`
            *   **기존 Room DB 사용 방식:** `ProjectRepositoryImpl`은 `ProjectLocalDataSource`를 사용하여 프로젝트 목록을 스트리밍/캐싱하고 (`getProjectListStream`, `getAvailableProjectsForScheduling`), 프로젝트 생성/조회 후 로컬에 저장했음 (`createProject`, `fetchProjectList`).
            *   **분리 작업 진행 상황:**
                *   `ProjectRemoteDataSource` 인터페이스 및 `ProjectRemoteDataSourceImpl` 구현체에 Firestore 스냅샷 리스너를 사용하는 `getParticipatingProjectsStream()` 메소드 추가 완료.
                *   `ProjectRepositoryImpl` 리팩토링 완료:
                    *   `ProjectLocalDataSource` 의존성 제거.
                    *   `getProjectListStream()`이 `ProjectRemoteDataSource.getParticipatingProjectsStream()`을 사용하도록 수정.
                    *   `createProject()`, `fetchProjectList()` 등에서 로컬 DB 저장 로직 제거.
                    *   `getAvailableProjectsForScheduling()`이 `ProjectRemoteDataSource.getParticipatingProjects()`를 사용하도록 수정.
                *   `DatabaseModule.kt`에서 `provideProjectDao` 주석 처리 완료.
                *   `DataSourceModule.kt`에서 `bindProjectLocalDataSource` 주석 처리/제거 완료.
                *   Firestore 디스크 지속성 활성화 완료.
        *   **주요 영향 컴포넌트:** `HomeViewModel` (`getProjectListStream` 직접 사용), `CreateProjectViewModel`, `ProjectSettingsViewModel`, `AddScheduleViewModel` (내부적으로 `GetSchedulableProjectsUseCase` 사용). 이들 컴포넌트는 변경된 `ProjectRepository`의 동작 방식에 맞춰 데이터 흐름 재검토 필요 가능성 있음.

    *   **ScheduleEntity (일정 정보)**
        *   **DAO:** `ScheduleDao`
        *   **Entity:** `ScheduleEntity`
        *   **LocalDataSource:** `ScheduleLocalDataSource` (존재하지 않거나, `DataSourceModule.kt`에 바인딩 없음 확인)
        *   **Repository:** `ScheduleRepositoryImpl`
            *   **기존 Room DB 사용 방식:** `ScheduleRepositoryImpl`은 `ScheduleLocalDataSource`를 사용하지 않고, `ScheduleRemoteDataSource`에 직접 의존하여 Firestore와 통신함.
            *   **분리 작업 진행 상황:**
                *   `DatabaseModule.kt`에서 `provideScheduleDao` 주석 처리 완료.
                *   `ScheduleLocalDataSource` 관련 Hilt 바인딩 없음 확인.
                *   Firestore 디스크 지속성 활성화 완료. `ScheduleRemoteDataSource`를 통해 가져온 데이터는 Firestore 캐시 활용.
            *   **스트림 지원:** 현재 `ScheduleRemoteDataSource`는 suspend 함수만 제공. 실시간 업데이트가 필요한 경우 향후 Flow 기반 스트림 메소드 추가 고려 가능.
        *   **주요 영향 컴포넌트:** `ScheduleViewModel`, `AddScheduleViewModel`, `HomeViewModel` (일정 요약 표시 등). 기존에도 Remote를 사용했으므로 큰 변경 없을 것으로 예상되나, 오프라인 시 동작은 Firestore 캐시에 의존.

    *   **ChatEntity / ChatMessageEntity (채팅 메시지)**
        *   **DAO:** `ChatDao`
        *   **Entity:** `ChatEntity`, `ChatMessageEntity` (또는 유사한 이름)
        *   **LocalDataSource:** `ChatLocalDataSource` (존재하지 않거나, `DataSourceModule.kt`에 바인딩 없음 확인)
        *   **Repository:** `ChatRepositoryImpl`
            *   **기존 Room DB 사용 방식:** `ChatRepositoryImpl`은 `ChatLocalDataSource`를 사용하지 않음. 현재 대부분의 메소드가 WebSocket 구현 대기 중이며, 주석 처리된 코드에서는 `ChatRemoteDataSource` 사용을 계획하고 있음.
            *   **분리 작업 진행 상황:**
                *   `DatabaseModule.kt`에서 `provideChatDao` 주석 처리 완료.
                *   `ChatLocalDataSource` 관련 Hilt 바인딩 없음 확인.
                *   Firestore 디스크 지속성 활성화 완료. 향후 `ChatRemoteDataSource`를 통해 메시지 기록을 가져올 때 Firestore 캐시 활용 예정.
        *   **주요 영향 컴포넌트:** `ChatViewModel` 및 채팅 관련 UI. Room DB 의존성이 원래 없었으므로, 분리 작업으로 인한 직접적 영향은 없음. WebSocket 및 Firestore 연동 구현이 주된 작업.

    *   **FriendEntity / FriendRequestEntity (친구 관계 정보)**
        *   **DAO:** `FriendDao`
        *   **Entity:** `FriendEntity`, `FriendRequestEntity` (또는 유사한 이름)
        *   **LocalDataSource:** `FriendLocalDataSource` / `FriendLocalDataSourceImpl` (내부적으로 `FriendDao` 사용)
        *   **Repository:** `FriendRepositoryImpl`
            *   **기존 Room DB 사용 방식:** `FriendRepositoryImpl`은 `FriendLocalDataSource`를 직접적으로 사용하지 않고, `FriendRemoteDataSource`에 의존하여 Firestore와 통신함 (`getFriendRelationshipsStream` 등).
            *   **분리 작업 진행 상황:**
                *   `DatabaseModule.kt`에서 `provideFriendDao` 주석 처리 완료.
                *   `DataSourceModule.kt`에서 `bindFriendLocalDataSource` 주석 처리/제거 완료.
                *   `FriendRemoteDataSource`는 이미 스트림 기반 메소드(`getFriendRelationshipsStream`)를 제공하고 있었음.
                *   Firestore 디스크 지속성 활성화 완료. `FriendRemoteDataSource`를 통해 가져온 데이터는 Firestore 캐시 활용.
        *   **주요 영향 컴포넌트:** `FriendsViewModel`, 친구 목록 UI, 친구 요청 처리 UI. 기존에도 Remote를 사용했으므로 큰 변경 없을 것으로 예상되나, 오프라인 시 동작은 Firestore 캐시에 의존.

    *   **InviteEntity (초대 정보)**
        *   **DAO:** `InviteDao`
        *   **Entity:** `InviteEntity`
        *   **LocalDataSource:** `InviteLocalDataSource` / `InviteLocalDataSourceImpl` (내부적으로 `InviteDao` 사용)
        *   **Repository:** `InviteRepositoryImpl`
            *   **기존 Room DB 사용 방식:** `InviteRepositoryImpl`은 `InviteLocalDataSource`를 사용하여 초대 정보를 로컬에 캐싱하고 (`getInviteDetails` 호출 시), 만료된 로컬 초대 정보를 정리했음 (`cleanupExpiredInvites`).
            *   **분리 작업 진행 상황:**
                *   `InviteRepositoryImpl` 리팩토링 완료:
                    *   `InviteLocalDataSource` 의존성 제거.
                    *   `getInviteDetails()` 메소드가 원격 데이터 소스(`InviteRemoteDataSource`)만 사용하고 Firestore 캐시에 의존하도록 수정.
                    *   `cleanupExpiredInvites()` 메소드는 서버 사이드 로직으로 이전 고려 대상임을 명시하고 클라이언트 로컬 정리 로직 제거.
                *   `DatabaseModule.kt`에서 `provideInviteDao` 주석 처리 완료.
                *   `DataSourceModule.kt`에서 `bindInviteLocalDataSource` 주석 처리/제거 완료.
                *   Firestore 디스크 지속성 활성화 완료.
        *   **주요 영향 컴포넌트:** 프로젝트 초대 생성/수락/조회 관련 ViewModel 및 UI. Firestore 캐시에 의존하여 오프라인 동작.

    *   **RoleEntity / RolePermissionEntity (프로젝트 역할 및 권한 정보)**
        *   **DAO:** `RoleDao`
        *   **Entity:** `RoleEntity`, `RolePermissionEntity`
        *   **LocalDataSource:** `ProjectRoleLocalDataSource` / `ProjectRoleLocalDataSourceImpl` (내부적으로 `RoleDao` 사용)
        *   **Repository:** `ProjectRoleRepositoryImpl`
            *   **기존 Room DB 사용 방식:** `ProjectRoleRepositoryImpl`은 `ProjectRoleLocalDataSource`를 사용하여 역할 정보를 로컬에 캐싱하고 (`getRoles`, `getRoleDetails`), 원격 변경 사항을 로컬에 동기화했음 (`getRolesStream`의 `onEach`, `fetchRoles`, `createRole`, `updateRole`, `deleteRole`).
            *   **분리 작업 진행 상황:**
                *   `ProjectRoleRepositoryImpl` 리팩토링 완료:
                    *   `ProjectRoleLocalDataSource` 의존성 제거.
                    *   `ProjectRoleRemoteDataSource`의 스트림을 직접 사용.
                    *   관련 Hilt 모듈 (`DatabaseModule.kt`, `DataSourceModule.kt`)에서 DAO 및 LocalDataSource 바인딩 주석 처리.
        *   **주요 영향 컴포넌트:** 프로젝트 설정 내 역할 관리 ViewModel 및 UI. Firestore 캐시에 의존하여 오프라인 동작.

    *   **ProjectMember 엔티티**
        *   **분석:**
            *   `ProjectMemberRepositoryImpl`은 `ProjectMemberLocalDataSource`와 `ProjectMemberRemoteDataSource`를 모두 사용.
            *   `ProjectMemberLocalDataSource`는 Room (`ProjectMemberDao`)을 사용하여 로컬 캐싱 및 오프라인 접근을 구현.
            *   `ProjectMemberRemoteDataSource`는 Firestore를 사용하며, `getProjectMembersStream` 메서드를 이미 제공.
            *   `ProjectMemberRepositoryImpl`의 `getProjectMembersStream`은 `remoteDataSource`의 스트림을 구독하여 `localDataSource`에 저장한 후, `localDataSource`의 스트림을 반환하는 구조.
        *   **조치:**
            *   `ProjectMemberRepositoryImpl` 리팩토링:
                *   `ProjectMemberLocalDataSource` 및 `NetworkConnectivityMonitor` 의존성 제거.
                *   `getProjectMembersStream`이 `remoteDataSource.getProjectMembersStream()`을 직접 반환하도록 수정.
                *   `getProjectMembers`, `getProjectMember`는 `remoteDataSource`를 직접 호출 (Firestore 캐시 활용).
                *   `addMemberToProject`, `removeMemberFromProject`, `updateMemberRoles`에서 `localDataSource` 관련 로직 및 `syncProjectMembers` 호출 제거.
                *   `syncProjectMembers`는 `remoteDataSource.getProjectMembers().map { }`으로 변경 (데이터 페칭은 하지만 반환값 무시, 스트림이 주된 동기화 방식이므로).
            *   `data/src/main/java/com/example/data/di/DatabaseModule.kt`에서 `provideProjectMemberDao` 주석 처리.
            *   `data/src/main/java/com/example/data/di/DataSourceModule.kt`에서 `bindProjectMemberLocalDataSource` 주석 처리.

*   **단계 2: Firestore 캐시 설정 및 Repository 수정**
    *   [X] 앱 초기화 시점에 Firestore 디스크 지속성 활성화 코드 추가. (FirebaseModule.kt에서 완료됨)
    *   [X] Room DB를 사용하는 Repository에서 Firestore를 직접 사용하도록 로직 수정. (각 엔티티 분석 시 완료)
    *   [X] Room DAO 호출 부분을 주석 처리하거나 기능 플래그로 비활성화. (각 엔티티 분석 시 완료)
*   **단계 3 & 4: UseCase/ViewModel 수정 및 Room DB 비활성화 후 검증**
    *   [X] Room DB 초기화 코드 비활성화 (`DatabaseModule.kt`의 `provideAppDatabase` 및 모든 DAO Provider 주석 처리 확인) 
    *   [ ] Room DB 비활성화에 따른 UseCase 및 ViewModel 수정:
        *   [ ] Repository 변경으로 인해 영향을 받는 UseCase 식별 및 수정.
        *   [ ] UseCase 변경으로 인해 영향을 받는 ViewModel 식별 및 수정.
        *   [ ] (필요시) 화면(Composable) 레벨에서의 데이터 흐름 변경 검토.
    *   [ ] 관련 Hilt 모듈에서 더 이상 사용되지 않는 LocalDataSource 및 DAO 바인딩 제거 또는 주석 처리 확인.
*   **단계 5: Room 관련 의존성 제거 (선택 사항)**
    *   [ ] 필요시 별도의 디커플링 작업 수행.

**6. 고려 사항 (Considerations)**

*   **Firestore 캐시의 한계:**
    *   복잡한 로컬 쿼리 (예: 여러 조건의 JOIN, Full-text search 등) 지원 미흡.
    *   캐시 크기 제한 및 자동 관리 정책.
    *   캐시 만료 및 동기화 전략에 대한 세밀한 제어 부족.
*   **사용자 경험:** Firestore 캐시가 활성화되어 있어도, 최초 데이터 로드나 네트워크 상태 변경 시 약간의 지연이 발생할 수 있음을 인지해야 한다.
*   **완전한 오프라인 우선(Offline-first) 아키텍처:** 만약 매우 강력한 오프라인 기능이 필수적이라면, Firestore 캐시만으로는 부족할 수 있으며, 이 경우 Room DB의 적극적인 활용이 다시 고려될 수 있다.

**7. 향후 작업 (Future Work)**

*   Firestore 캐시 사용 현황 모니터링 및 사용자 피드백 수렴.
*   만약 Firestore 캐시의 한계로 인해 주요 기능 구현에 어려움이 발생하거나 사용자 경험이 저하될 경우, Room DB 재통합 및 고도화된 로컬 캐싱/데이터 관리 전략 수립.

### 7. Role 및 RolePermission 엔티티

- **분석:**
    - `RoleEntity` 및 `RolePermissionEntity`는 `ProjectRoleRepositoryImpl`을 통해 관리됩니다.
    - `ProjectRoleRepositoryImpl`은 이미 Firestore 캐시를 사용하도록 리팩토링되었으며, 관련된 `ProjectRoleLocalDataSource` 및 `RoleDao` (`provideRoleDao`)는 비활성화되었습니다.
    - 별도의 `RoleRepository` 또는 `RolePermissionRepository`는 존재하지 않는 것으로 보입니다.
- **조치:**
    - 이전 `ProjectRole 엔티티` 섹션에서 이미 처리 완료되었습니다. `RoleDao`는 `ProjectRoleLocalDataSource`가 사용하던 DAO이며, `provideRoleDao`는 `DatabaseModule.kt`에서 주석 처리되었습니다.

### 8. MediaImage 엔티티

- **분석:**
    - `MediaImageEntity.kt`는 Room `@Entity`로 선언되어 있으나, `AppDatabase.kt`의 엔티티 목록에 포함되어 있지 않습니다.
    - `ChatLocalDataSourceImpl.kt`에서 Android의 `MediaStore`를 통해 로컬 갤러리 이미지를 조회할 때 이 `MediaImageEntity` 데이터 클래스를 사용하여 결과를 구조화합니다.
    - 별도의 `MediaImageDao.kt`는 존재하지 않습니다.
    - 이는 `MediaImageEntity`가 앱의 Room 데이터베이스에 실제로 저장 및 관리되는 엔티티가 아님을 의미합니다. 단순히 `MediaStore` 조회 결과를 담는 로컬 DTO/모델 역할을 합니다.
- **조치:**
    - Room 데이터베이스와 직접적인 결합이 없으므로 별도의 디커플링 작업이 필요하지 않습니다.
    - `getLocalGalleryImages` 기능은 `ChatLocalDataSourceImpl`에서 `MediaStore`를 직접 쿼리하여 유지됩니다.

### 9. FriendRequest 엔티티

- **분석:**
    - `FriendRequestEntity`는 Room `@Entity`로 선언되어 있으며, `FriendDao`를 통해 관리되었습니다.
    - `FriendRepositoryImpl`은 친구 및 친구 요청 관련 로직을 모두 처리하며, 이미 `FriendRemoteDataSource`에 의존하고 `FriendLocalDataSource`를 주입받지 않도록 수정되었습니다.
    - `FriendDao`를 제공하는 `provideFriendDao` (DatabaseModule) 및 `FriendLocalDataSource`를 바인딩하는 `bindFriendLocalDataSource` (DataSourceModule)는 이전 `FriendEntity` 분리 과정에서 이미 주석 처리되었습니다.
- **조치:**
    - `FriendEntity`를 처리하면서 `FriendRequestEntity`에 대한 Room DB 의존성도 함께 제거되었습니다. 별도의 추가 조치는 필요하지 않습니다.

### 10. CategoryEntity (및 ProjectStructure 관련)

- **분석:**
    - `CategoryEntity`는 `ProjectStructureDao`를 통해 관리되며, `ProjectStructureLocalDataSource` 및 `ProjectStructureRepositoryImpl`에서 사용됩니다.
    - `ProjectStructureRepositoryImpl`은 카테고리 및 채널(프로젝트 구조의 일부) 관리를 위해 `ProjectStructureLocalDataSource`를 사용하여 로컬 캐싱을 수행했습니다.
    - `ProjectStructureRemoteDataSource`는 `getProjectStructureStream`을 제공하여 전체 프로젝트 구조(카테고리 및 채널 포함)에 대한 실시간 스트림을 지원합니다.
- **조치:**
    - `ProjectStructureRepositoryImpl` 리팩토링 완료:
        - `ProjectStructureLocalDataSource` 및 `NetworkConnectivityMonitor` 의존성 제거.
        - `getProjectStructureStream` 및 `getProjectCategoriesStream`이 `ProjectStructureRemoteDataSource`의 스트림을 직접 사용하고 Firestore 캐시에 의존하도록 수정.
        - 카테고리 관련 CRUD 메소드 (`createCategory`, `getCategoryDetails`, `getProjectCategories`, `updateCategory`, `deleteCategory`) 및 순서 변경 메소드 (`reorderCategory`, `batchReorderCategories`)에서 로컬 데이터 소스 관련 로직 제거.
    - `data/src/main/java/com/example/data/di/DatabaseModule.kt`에서 `provideProjectStructureDao` 주석 처리 완료.
    - `data/src/main/java/com/example/data/di/DataSourceModule.kt`에는 `ProjectStructureLocalDataSource`에 대한 Hilt 바인딩이 존재하지 않아 별도 조치 필요 없음.
- **참고:**
    - 이 변경으로 `ProjectStructureDao`가 관리하는 `ChannelEntity` 부분도 `ProjectStructureRepositoryImpl` 수준에서는 Room DB 의존성이 제거되었습니다. 
    - `ChannelEntity`는 `ChannelDao` 및 `ChannelRepositoryImpl`을 통해서도 관리될 수 있으므로, 해당 부분은 별도로 `ChannelEntity` 분석 시 확인 필요합니다.

### 11. ChannelEntity

- **분석:**
    - `ChannelEntity`는 `ChannelDao`를 통해 직접 관리되었으며, 또한 `ProjectStructureDao`를 통해 프로젝트 구조의 일부로도 간접적으로 관리되었습니다.
    - `ChannelRepositoryImpl`은 분석 결과 `ChannelLocalDataSource`나 `ChannelDao`를 직접 주입받지 않고, `ChannelRemoteDataSource`에만 의존하고 있었습니다. 이는 Firestore 캐시를 활용하기에 적합한 구조입니다.
    - `ProjectStructureRepositoryImpl`은 이전 `CategoryEntity` (및 ProjectStructure 관련) 리팩토링 과정에서 `ProjectStructureLocalDataSource` 의존성이 제거되면서, 해당 경로를 통한 `ChannelEntity`의 Room 접근도 차단되었습니다.
- **조치:**
    - `data/src/main/java/com/example/data/di/DatabaseModule.kt`에서 `provideChannelDao` 주석 처리 완료.
    - `data/src/main/java/com/example/data/di/DatabaseModule.kt`에서 `provideProjectStructureDao`는 이전 단계에서 이미 주석 처리 완료.
    - `data/src/main/java/com/example/data/di/DataSourceModule.kt`에는 `ChannelLocalDataSource` 또는 `ProjectStructureLocalDataSource`에 대한 활성 Hilt 바인딩이 없어 별도 조치 불필요.
- **결론:** `ChannelEntity`는 Room 데이터베이스와의 주요 연결 지점들이 비활성화되어 성공적으로 분리되었습니다.

### 12. ChatMessageEntity

- **분석:**
    - `ChatMessageEntity`는 `ChatMessageDao`를 통해 관리되도록 설계되었고, `ChatLocalDataSourceImpl`이 이 DAO를 사용합니다.
    - `ChatRepositoryImpl` (메시지 주 담당 리포지토리)은 `ChatLocalDataSource`를 주입받지 않으며, 현재 WebSocket 구현을 위해 스텁 처리되어 있고, 실제 구현 시 `ChatRemoteDataSource`를 사용하도록 계획되어 있습니다.
    - `ChannelRepositoryImpl` (채널 정보 담당, 일부 메시지 메소드 포함) 또한 로컬 데이터 소스를 사용하지 않고 `ChannelRemoteDataSource`에 의존합니다.
    - `AppDatabase.kt`에는 `ChatMessageEntity`가 엔티티로 등록되어 있지만, `abstract fun chatMessageDao(): ChatMessageDao`와 같은 추상 메소드가 정의되어 있지 않습니다.
    - `DatabaseModule.kt`에는 `ChatMessageDao`를 제공하는 Hilt 프로바이더가 없습니다. (이전 `provideChatDao`는 `ChatEntity`용이었고 이미 주석 처리됨)
    - `DataSourceModule.kt`에는 `ChatLocalDataSource`에 대한 Hilt 바인딩이 없습니다.
- **조치:**
    - `ChatRepositoryImpl` 및 `ChannelRepositoryImpl`이 이미 로컬 메시지 저장소를 사용하지 않으므로, 애플리케이션의 주요 메시지 흐름에서는 Room DB와 분리되어 있습니다.
    - `ChatMessageDao`가 Hilt를 통해 제대로 제공되지 않고, `ChatLocalDataSource` 바인딩도 없어 `ChatLocalDataSourceImpl`이 활성화될 경로가 없습니다.
    - 따라서 `ChatMessageEntity`와 관련된 Room DB 사용은 실질적으로 비활성화된 상태입니다.
- **결론:** `ChatMessageEntity`는 주요 리포지토리에서의 미사용 및 Hilt 설정 부재로 인해 Room 데이터베이스와 효과적으로 분리되어 있습니다. 별도의 코드 변경 없이 문서화만으로 충분합니다.

### 최종 검토 및 다음 단계

*   **단계 2: Firestore 캐시 설정 및 Repository 수정**
    *   [X] 앱 초기화 시점에 Firestore 디스크 지속성 활성화 코드 추가. (FirebaseModule.kt에서 완료됨)
    *   [X] Room DB를 사용하는 Repository에서 Firestore를 직접 사용하도록 로직 수정. (각 엔티티 분석 시 완료)
    *   [X] Room DAO 호출 부분을 주석 처리하거나 기능 플래그로 비활성화. (각 엔티티 분석 시 완료)
*   **단계 3 & 4: UseCase/ViewModel 수정 및 Room DB 비활성화 후 검증**
    *   [X] Room DB 초기화 코드 비활성화 (`DatabaseModule.kt`의 `provideAppDatabase` 및 모든 DAO Provider 주석 처리 확인) 
    *   [ ] Room DB 비활성화에 따른 UseCase 및 ViewModel 수정:
        *   [ ] Repository 변경으로 인해 영향을 받는 UseCase 식별 및 수정.
        *   [ ] UseCase 변경으로 인해 영향을 받는 ViewModel 식별 및 수정.
        *   [ ] (필요시) 화면(Composable) 레벨에서의 데이터 흐름 변경 검토.
    *   [ ] 관련 Hilt 모듈에서 더 이상 사용되지 않는 LocalDataSource 및 DAO 바인딩 제거 또는 주석 처리 확인.
*   **단계 5: Room 관련 의존성 제거 (선택 사항)**
    *   [ ] 필요시 별도의 디커플링 작업 수행.

이제 다음 단계인 "단계 3: UseCase 및 ViewModel 수정 (검토)" 또는 좀 더 확실한 "단계 4: Room DB 초기화 코드 비활성화"로 진행할 수 있습니다. 사용자님의 지시에 따라 다음 작업을 시작하겠습니다. 