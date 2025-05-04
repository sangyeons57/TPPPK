# Task: 나머지 Firebase 기능 구현

## 지침
1. 빌드하지 않기: 모든 기능 구현 시 실제 빌드 없이 코드만 작성합니다. 완료 후 통합 테스트를 진행합니다.
2. 코드 점검 및 개선: 각 기능 구현 후 관련 코드를 검토하여 누락된 요소나 개선점을 파악하고 추가합니다.
3. 모듈화 및 기능 분리: 각 기능은 최대한 독립적으로 모듈화하고, 관심사 분리(Separation of Concerns) 원칙에 따라 구현합니다.
4. 일관된 코드 스타일: [code-conventions.mdc](mdc:.cursor/rules/code-conventions.mdc) 문서에 정의된 코딩 스타일을 준수합니다.
5. 에러 처리: 모든 Firebase 작업에 적절한 예외 처리 및 오류 로깅을 구현합니다.
6. 캐싱 전략: 오프라인 작업을 위한 효율적인 로컬 캐싱 전략을 구현합니다.

- [x] 1. Friend 데이터 소스 CRUD 구현
    - [x] 1.1. Remote (Firestore) CRUD 구현 (`FriendRemoteDataSourceImpl`)
        - [x] `getFriendsListStream` 구현 (실시간 스트림)
        - [x] `fetchFriendsList` 구현
        - [x] `getDmChannelId` 구현
        - [x] `sendFriendRequest` 구현
        - [x] `getFriendRequests` 구현
        - [x] `acceptFriendRequest` 구현
        - [x] `denyFriendRequest` 구현
    - [x] 1.2. Local (Room) 설정 및 CRUD 구현
        - [x] 1.2.1. `FriendEntity` 및 `FriendRequestEntity` 정의 (`data/model/local`)
        - [x] 1.2.2. `FriendDao` 정의 및 CRUD 메서드 추가 (`data/db/dao`)
        - [x] 1.2.3. `AppDatabase`에 `FriendEntity`, `FriendRequestEntity` 및 `FriendDao` 추가 (`data/db`)
        - [x] 1.2.4. Hilt 모듈에서 `FriendDao` 제공 설정 (`data/di`)
        - [x] 1.2.5. `FriendLocalDataSourceImpl`에서 `FriendDao` 사용하여 CRUD 구현

- [x] 2. DM (Direct Message) 데이터 소스 구현
    - [x] 2.1. Remote (Firestore) 구현 (`DmRemoteDataSourceImpl`)
        - [x] `getDmListStream` 구현 (실시간 스트림)
        - [x] `fetchDmList` 구현
        - [x] `createDmChannel` 구현
        - [x] `deleteDmChannel` 구현 (옵션)
    - [x] 2.2. Local (Room) 설정 및 구현
        - [x] 2.2.1. `DmConversationEntity` 정의 (`data/model/local`)
        - [x] 2.2.2. `DmDao` 정의 및 메서드 추가 (`data/db/dao`)
        - [x] 2.2.3. `AppDatabase`에 `DmConversationEntity` 및 `DmDao` 추가 (`data/db`)
        - [x] 2.2.4. Hilt 모듈에서 `DmDao` 제공 설정 (`data/di`)
        - [x] 2.2.5. `DmLocalDataSourceImpl`에서 `DmDao` 사용하여 구현

- [x] 3. Project Role 데이터 소스 구현
    - [x] 3.1. Remote (Firestore) 구현 (`ProjectRoleRemoteDataSourceImpl`)
        - [x] `getRoles` 구현
        - [x] `getRolesStream` 구현 (실시간 스트림)
        - [x] `fetchRoles` 구현
        - [x] `getRoleDetails` 구현
        - [x] `createRole` 구현
        - [x] `updateRole` 구현
        - [x] `deleteRole` 구현
    - [x] 3.2. Local (Room) 설정 및 구현
        - [x] 3.2.1. `RoleEntity` 및 `RolePermissionEntity` 정의 (`data/model/local`)
        - [x] 3.2.2. `RoleDao` 정의 및 메서드 추가 (`data/db/dao`)
        - [x] 3.2.3. `AppDatabase`에 `RoleEntity`, `RolePermissionEntity` 및 `RoleDao` 추가 (`data/db`)
        - [x] 3.2.4. Hilt 모듈에서 `RoleDao` 제공 설정 (`data/di`)
        - [x] 3.2.5. `ProjectRoleLocalDataSourceImpl`에서 `RoleDao` 사용하여 구현

- [x] 4. Project Member 데이터 소스 구현
    - [x] 4.1. Remote (Firestore) 구현 (`ProjectMemberRemoteDataSourceImpl`)
        - [x] `getProjectMembers` 구현
        - [x] `getProjectMembersStream` 구현 (실시간 스트림)
        - [x] `addMemberToProject` 구현
        - [x] `removeMemberFromProject` 구현
        - [x] `updateMemberRoles` 구현
    - [x] 4.2. Local (Room) 설정 및 구현
        - [x] 4.2.1. `ProjectMemberEntity` 정의 (`data/model/local`)
        - [x] 4.2.2. `ProjectMemberDao` 정의 및 메서드 추가 (`data/db/dao`)
        - [x] 4.2.3. `AppDatabase`에 `ProjectMemberEntity` 및 `ProjectMemberDao` 추가 (`data/db`)
        - [x] 4.2.4. Hilt 모듈에서 `ProjectMemberDao` 제공 설정 (`data/di`)
        - [x] 4.2.5. `ProjectMemberLocalDataSourceImpl`에서 `ProjectMemberDao` 사용하여 구현

- [x] 5. Project Structure (카테고리/채널) 데이터 소스 구현
    - [x] 5.1. Remote (Firestore) 구현 (`ProjectStructureRemoteDataSourceImpl`)
        - [x] `getProjectStructure` 구현 (카테고리 및 채널 계층 구조)
        - [x] `getProjectStructureStream` 구현 (실시간 스트림)
        - [x] `createCategory` 구현
        - [x] `createChannel` 구현
        - [x] `getCategoryDetails` 구현
        - [x] `updateCategory` 구현
        - [x] `getChannelDetails` 구현
        - [x] `updateChannel` 구현
        - [x] `deleteCategory` 구현
        - [x] `deleteChannel` 구현
    - [x] 5.2. Local (Room) 설정 및 구현
        - [x] 5.2.1. `CategoryEntity` 및 `ChannelEntity` 정의 (`data/model/local`)
        - [x] 5.2.2. `ProjectStructureDao` 정의 및 메서드 추가 (`data/db/dao`)
        - [x] 5.2.3. `AppDatabase`에 `CategoryEntity`, `ChannelEntity` 및 `ProjectStructureDao` 추가 (`data/db`)
        - [x] 5.2.4. Hilt 모듈에서 `ProjectStructureDao` 제공 설정 (`data/di`)
        - [x] 5.2.5. `ProjectStructureLocalDataSourceImpl`에서 `ProjectStructureDao` 사용하여 구현

- [x] 6. Invite 데이터 소스 구현
    - [x] 6.1. Remote (Firestore) 구현 (`InviteRemoteDataSourceImpl`)
        - [x] `createInviteToken` 구현
        - [x] `validateInviteToken` 구현
        - [x] `acceptInvite` 구현
        - [x] `getInviteDetails` 구현
    - [x] 6.2. Local (Room) 설정 및 구현 (선택적 - 초대 토큰 캐싱이 필요한 경우)
        - [x] 6.2.1. `InviteEntity` 정의 (`data/model/local`)
        - [x] 6.2.2. `InviteDao` 정의 및 메서드 추가 (`data/db/dao`)
        - [x] 6.2.3. `AppDatabase`에 `InviteEntity` 및 `InviteDao` 추가 (`data/db`)
        - [x] 6.2.4. Hilt 모듈에서 `InviteDao` 제공 설정 (`data/di`)
        - [x] 6.2.5. `InviteLocalDataSourceImpl`에서 `InviteDao` 사용하여 구현

- [x] 7. Firestore 보안 규칙 상세화 (`firestore.rules`)
    - [x] 7.1. 친구 관련 보안 규칙 구현
        - [x] 친구 요청은 인증된 사용자만 보낼 수 있도록 설정
        - [x] 친구 목록은 본인만 읽을 수 있도록 설정
    - [x] 7.2. 프로젝트 관련 보안 규칙 구현
        - [x] 프로젝트 멤버만 프로젝트 데이터 읽기 가능하도록 설정
        - [x] 프로젝트 역할에 따른 쓰기 권한 차등 적용
    - [x] 7.3. 채팅 관련 보안 규칙 구현
        - [x] DM은 참가자만 읽고 쓸 수 있도록 설정
        - [x] 채널 메시지는 프로젝트 멤버만 읽고 쓸 수 있도록 설정
    - [x] 7.4. 초대 관련 보안 규칙 구현
        - [x] 초대 토큰 생성은 권한 있는 사용자만 가능하도록 설정
        - [x] 유효한 초대 토큰만 사용 가능하도록 설정

- [x] 8. Firestore 복합 색인 추가 정의 (`firestore.indexes.json`)
    - [x] 8.1. 친구 관련 색인 추가
        - [x] 친구 요청 상태 필터링 및 생성 시간 정렬
    - [x] 8.2. 채팅 관련 색인 추가
        - [x] DM 목록 필터링 및 최근 메시지 시간 정렬
    - [x] 8.3. 프로젝트 및 채널 관련 색인 추가
        - [x] 프로젝트 멤버 역할 및 생성 시간 정렬
        - [x] 채널 유형 필터링 및 정렬 순서

- [x] 9. 리포지토리 구현체 작성
    - [x] 9.1. `FriendRepositoryImpl` 구현
    - [x] 9.2. `DmRepositoryImpl` 구현
    - [x] 9.3. `ProjectRoleRepositoryImpl` 구현
    - [x] 9.4. `ProjectMemberRepositoryImpl` 구현
    - [x] 9.5. `ProjectStructureRepositoryImpl` 구현
    - [x] 9.6. `InviteRepositoryImpl` 구현 