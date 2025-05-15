# 채널 통합 및 스키마 변경에 따른 Repository 인터페이스 리팩토링

최근 Firestore 스키마 변경으로 인해, 프로젝트 채널의 참가자 관리 방식이 변경되었고, 모든 채널에 `type` 필드가 도입되었습니다. 이에 따라 기존 레포지토리 인터페이스 및 구현체의 역할을 재조정하고, 변경된 스키마에 맞게 리팩토링합니다.

## 주요 변경 사항 (스키마 기준)

-   **Global `channels` 컬렉션**:
    -   모든 채널 (DM, 프로젝트, 카테고리)은 여기에 저장됩니다.
    -   최상위 `type` 필드 (`DM`, `PROJECT`, `CATEGORY`)로 채널 종류를 구분합니다.
    -   DM 채널: `participantIds` 필드를 직접 가집니다.
    -   프로젝트/카테고리 채널: `participantIds` 필드를 가지지 않습니다. (참가자는 프로젝트 하위에서 관리)
    -   `metadata` 필드는 각 채널 타입에 따른 부가 정보를 저장합니다. (예: `projectId`, `categoryId`, `channelType` 등)
-   **`projects/{projectId}/participants/{channelId}` 컬렉션**:
    -   프로젝트 또는 카테고리 채널의 참가자 목록을 관리합니다.
    -   `participantIds`: Array<String>
-   **`projects/{projectId}/categories/{categoryId}/channelReferences/{channelId}` 컬렉션**:
    -   카테고리 내 채널의 순서 등 참조 정보를 관리합니다. (이 부분은 유지될 수 있습니다.)
-   **변경사항 추가: Participants → Members 통합**:
    -   `projects/{projectId}/participants/{channelId}` 컬렉션을 제거하고, 채널 접근 권한을 `projects/{projectId}/members/{userId}` 문서 내에 관리합니다.
    -   `projects/{projectId}/members/{userId}.channelIds` 필드에 해당 사용자가 접근 가능한 채널 ID 목록을 저장합니다.

## 작업 단계

- [x] **1단계: 현재 레포지토리 인터페이스 상호 의존성 분석**
    -   `ChannelRepository`, `DmRepository`, `ProjectChannelRepository`, `ChatRepository`, `ProjectStructureRepository` 간 기능 중복 및 의존성 관계 재파악 (변경된 스키마 기준)

- [x] **2단계: 통합 인터페이스 재설계**
    -   `ChannelRepository`: 모든 채널의 핵심 CRUD, `type` 필드 기반 조회, DM 채널의 `participantIds` 관리.
    -   `ProjectRepository` (또는 신규 `ProjectParticipantRepository`): `projects/{projectId}/participants/{channelId}` 관리, 프로젝트 멤버의 채널 참여/탈퇴 처리.
    -   `ProjectStructureRepository`: 카테고리 관리, 카테고리 내 채널 참조(`channelReferences`) 관리.
    -   `ChatRepository`: 메시지 전용 관리.
    -   `DmRepository`는 `ChannelRepository`로 기능 완전 통합 또는 최소화 (DM 특화 로직만 남김).

- [x] **3단계: `ChannelRepository` 인터페이스 리팩토링**
    -   **신규 스키마 반영**:
        -   채널 생성/수정 시 `type` 필드 설정 로직 추가.
        -   DM 채널 생성 시 `participantIds` 직접 관리.
        -   프로젝트/카테고리 채널 생성 시 `metadata` (`projectId`, `categoryId`, `channelType` 등) 관리. 참가자 정보는 다루지 않음.
    -   메서드 시그니처 변경:
        -   `getChannels(type: ChannelType, userId?: String, projectId?: String, categoryId?: String)`: 다양한 조건으로 채널 조회.
        -   `createDmChannel(userId1: String, userId2: String)`: DM 채널 생성.
        -   `createProjectOrCategoryChannel(name: String, type: ChannelType, metadata: Map<String, Any>)`: 프로젝트/카테고리 채널 생성.
    -   참가자 관리:
        -   DM 채널: `addParticipantToDmChannel`, `removeParticipantFromDmChannel` (내부적으로 `participantIds` 업데이트).
        -   프로젝트/카테고리 채널의 참가자 관리는 `ProjectRepository` (또는 `ProjectParticipantRepository`)로 위임.

- [x] **4단계: `DmRepository` 인터페이스 통합 또는 재정의**
    -   **통합 결정 시**:
        -   `DmRepository` 인터페이스 및 구현체 제거.
        -   DM 채널 생성/조회 로직은 `ChannelRepository`에서 `type="DM"` 조건으로 처리.
        -   필요시 `users/{userId}/activeDmIds` 업데이트 로직은 `ChannelRepository` 또는 관련 UseCase에서 처리.
    -   **최소화 결정 시**:
        -   `DmRepository`는 `ChannelRepository`를 위임하고, `activeDmIds` 업데이트 등 DM 특화 로직만 수행.

- [x] **5단계: `ProjectParticipantRepository` (가칭) 인터페이스 설계 및 구현** (기존 `ProjectChannelRepository` 역할 변경)
    -   **역할**: 프로젝트 내 채널 참가자 관리 (`projects/{projectId}/participants/{channelId}`).
    -   **메서드**:
        -   `addParticipantToProjectChannel(projectId: String, channelId: String, userId: String)`
        -   `removeParticipantFromProjectChannel(projectId: String, channelId: String, userId: String)`
        -   `getChannelParticipants(projectId: String, channelId: String): Flow<List<String>>`
        -   `getChannelsForUserInProject(projectId: String, userId: String): Flow<List<String>>` (참여 중인 채널 ID 목록)
    -   채널 자체의 CRUD는 `ChannelRepository`에 위임.

- [x] **6단계: `ChatRepository` 리팩토링** (기존 계획 유지)
    -   메시지 처리에만 집중.
    -   `ChannelRepository`와의 책임 중복 제거.

- [x] **7단계: `ProjectStructureRepository` 리팩토링**
    -   카테고리 CRUD 및 순서 관리.
    -   카테고리 내 채널 참조(`projects/{projectId}/categories/{categoryId}/channelReferences/{channelId}`) 관리:
        -   `addChannelToCategory(projectId: String, categoryId: String, channelId: String, order: Int)`
        -   `removeChannelFromCategory(projectId: String, categoryId: String, channelId: String)`
        -   `getChannelsInCategory(projectId: String, categoryId: String): Flow<List<ChannelReference>>`
    -   채널 생성 자체는 `ChannelRepository`에 위임.

- [x] **8단계: 의존성 주입 구조 조정** (기존 계획 유지, 변경된 인터페이스 반영)
    -   새로운/변경된 Repository 인터페이스에 맞게 Hilt 모듈 업데이트.

- [x] **9단계: 구현체 리팩토링**
    -   [x] `ChannelRepositoryImpl` 업데이트 (변경된 스키마 및 메서드 시그니처 반영)
    -   [x] `DmRepositoryImpl` 업데이트/제거 (4단계 결정에 따름)
    -   [x] `ProjectParticipantRepositoryImpl` (가칭) 신규 구현 또는 `ProjectChannelRepositoryImpl` 수정
    -   [x] `ChatRepositoryImpl` 업데이트 (기존 계획대로)
    -   [x] `ProjectStructureRepositoryImpl` 업데이트 (변경된 역할 및 `channelReferences` 관리 반영)

- [x] **10단계: DataSource 계층 리팩토링**
    -   `ChannelRemoteDataSource`:
        -   `type` 필드, `metadata`, `participantIds` (DM 채널용) 사용 로직 수정.
    -   `ProjectParticipantRemoteDataSource` (가칭, 신규 또는 기존 수정):
        -   `projects/{projectId}/participants/{channelId}` 컬렉션 접근 로직.
    -   `ProjectStructureRemoteDataSource`:
        -   카테고리 및 `channelReferences` 접근 로직.
    -   기타 DataSource들도 변경된 Repository 역할에 맞춰 조정.

- [x] **11단계: Participants 컬렉션 제거 및 Members 통합 준비**
    -   [x] `FirestoreConstants.kt`에서 `PARTICIPANTS` 컬렉션 제거 및 `MemberFields.CHANNEL_IDS` 추가
    -   [x] `ProjectStructureRemoteDataSourceImpl`에서 Participants 컬렉션 참조 코드를 Members 컬렉션으로 전환
       - 채널 접근 권한을 member 문서의 channelIds 필드에서 관리하도록 변경
       - 기존 참가자 조회/관리 로직을 멤버 기반으로 수정
    -   [x] `ProjectMemberRemoteDataSourceImpl` 수정하여 채널 액세스 권한 관리 추가:
       - 멤버 추가/제거시 채널 접근 권한 관리
       - 채널 생성시 생성자에게 자동 접근 권한 부여
       - 프로젝트 내 채널별 접근 가능한 사용자 조회 기능

- [x] **12단계: ProjectParticipantRepository 제거 및 ProjectMemberRepository로 통합**
    -   [x] `ProjectParticipantRepository` 인터페이스와 구현체 제거
    -   [x] `ProjectMemberRepository` 인터페이스에 채널 접근 관리 메서드 추가:
       - `addChannelAccessToMember(projectId: String, userId: String, channelId: String)`
       - `removeChannelAccessFromMember(projectId: String, userId: String, channelId: String)`
       - `getMemberChannelAccess(projectId: String, userId: String): Flow<List<String>>`
       - `getMembersWithChannelAccess(projectId: String, channelId: String): Flow<List<String>>`

- [ ] **13단계: 테스트 작성 및 검증** (변경된 구조 반영)
    -   [ ] 각 Repository 인터페이스별 핵심 기능 테스트 재작성/추가
    -   [ ] DM 채널, 프로젝트 채널, 카테고리 채널 생성 및 조회 로직 검증
    -   [ ] 프로젝트 멤버의 채널 접근 권한 관리 로직 검증
    -   [ ] Member 문서를 통한 채널 접근 권한 관리 검증

## 구현 세부 지침 (업데이트)

### `ChannelRepository` 구현 지침
-   **주요 책임**: 모든 유형의 채널(`channels` 컬렉션)의 기본 CRUD 및 조회. `type` 필드를 적극 활용.
-   DM 채널의 `participantIds` 필드 직접 관리.
-   프로젝트/카테고리 채널 생성 시 `metadata` (`projectId`, `categoryId`, `channelType` 등) 관리. 참가자 정보는 다루지 않음.
-   채널 삭제 시, 관련 `projects/{projectId}/participants/{channelId}` 문서 및 `projects/{projectId}/categories/{categoryId}/channelReferences/{channelId}` 문서 삭제는 해당 책임을 가진 다른 Repository (예: `ProjectParticipantRepository`, `ProjectStructureRepository`) 또는 UseCase에서 처리하도록 연동. (CASCADE 삭제 로직 고려)

### `DmRepository` 구현 지침 (통합 또는 최소화)
-   **통합 시**: 관련 로직은 `ChannelRepository` 및 UseCase로 이동.
-   **최소화 시**: `ChannelRepository`를 위임하고, `users/{userId}/activeDmIds` 필드 업데이트 등 극히 일부 DM 특화 로직만 담당.

### `ProjectMemberRepository` 구현 지침 (Participants 통합)
-   **주요 책임**: `projects/{projectId}/members/{userId}` 문서 관리 (채널 접근 권한 포함).
-   사용자의 역할(roleIds) 및 채널 접근 권한(channelIds) 관리.
-   프로젝트 멤버 추가/제거 및 역할 관리.
-   특정 채널에 접근 가능한 멤버 목록 조회 기능.
-   특정 멤버가 접근 가능한 채널 목록 조회 기능.

### `ChatRepository` 구현 지침 (변경 없음)
-   메시지 관리에 집중.

### `ProjectStructureRepository` 구현 지침
-   **주요 책임**: 카테고리 CRUD, 카테고리 순서 관리, 카테고리 내 채널 참조(`projects/{projectId}/categories/{categoryId}/channelReferences/{channelId}`) 관리.
-   채널 자체의 생성은 `ChannelRepository`에 위임.
-   카테고리 삭제 시, 해당 카테고리에 속한 모든 `channelReferences` 삭제 및 관련 채널 삭제는 `ChannelRepository`와 연동 (UseCase 레벨에서 처리 권장).

### DataSource 구현 지침 (업데이트)
-   변경된 Firestore 스키마 구조(`type` 필드, `members.channelIds` 필드, `channelReferences` 등)에 맞춰 각 DataSource의 쿼리 및 데이터 매핑 로직 수정.
-   `ChannelRemoteDataSource`, `ProjectMemberRemoteDataSource`, `ProjectStructureRemoteDataSource` 등의 책임 분담 명확화.
-   멤버의 채널 접근 권한을 `MemberFields.CHANNEL_IDS` 필드를 통해 관리하도록 관련 DataSource 수정. 