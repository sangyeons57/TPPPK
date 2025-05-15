# 에러 수정 계획: DataSource, Repository, Mapper 오류 해결

최근 대규모 리팩토링 이후 발생한 컴파일 에러들을 수정합니다. 주요 원인은 모델 변경, 상수 변경, 인터페이스 변경에 따른 구현체의 미반영입니다.

## 작업 컨텍스트 및 주요 변경 사항 숙지

- **DM 채널 통합**: 기존 `dms` 컬렉션 및 관련 모델(`DmChannelDto`, `DmFields` 등)이 삭제되고, 모든 채널 정보는 글로벌 `channels` 컬렉션으로 통합되었습니다. `Channel` 모델 및 `ChannelType.DM`을 사용해야 합니다.
- **`channelReferences` 제거**: 프로젝트 및 카테고리 내 채널 순서/구조 관리를 위해 사용되던 `channelReferences` 및 `projectChannelReferences` 컬렉션이 제거되었습니다. 채널의 `order` 필드와 `metadata` 필드 ( `metadata.projectId`, `metadata.categoryId` 등)를 사용하여 채널 구조 및 순서를 관리합니다.
- **`participants` 제거 및 `members` 통합**: 프로젝트 채널의 참여자 관리는 `projects/{projectId}/members/{userId}` 문서 내 `channelIds` 필드로 통합되었습니다. `participants` 컬렉션은 삭제되었습니다.
- **시간 표현 통일**: 데이터 모델에서 시간 정보는 주로 `Instant` 타입을 사용하도록 통일하는 추세입니다. `LocalDateTime`과의 혼용에 주의하여 필요시 `DateTimeUtil`을 사용해 변환합니다.
- **상수 관리**: Firestore 관련 상수는 `FirestoreConstants.kt` 파일에 정의된 값을 사용합니다.

## 에러 그룹 및 해결 전략

### 그룹 1: DM 관련 DataSource 및 Repository 오류 (`DmLocalDataSourceImpl`, `DmRemoteDataSource`, `DmRemoteDataSourceImpl`)

- **문제**: 삭제된 DM 관련 클래스(`DmChannelDto`, `DmFields`, `DMS`) 참조, `Channel` 모델과의 필드명/타입 불일치, 인터페이스 미구현.
- **이유**: DM 채널이 글로벌 `channels`로 통합됨에 따라 DM 전용 로직이 `ChannelRepository` 및 관련 DataSource로 이전/통합되어야 하나, 기존 DM 관련 파일들이 제대로 정리되지 않음.
- **해결 방법**:
    - [x] `DmLocalDataSourceImpl.kt`:
        - [x] `toDmChannelEntity`: `DmChannel` 대신 `Channel` 모델을 참조하도록 수정. `lastMessageTimestamp` 필드를 사용하고 `Instant` 타입으로 처리. `participantIds` 필드 추가.
        - [x] `toDmChannel`: `DmChannelEntity` 대신 `ChannelEntity`를 참조하도록 수정. `lastMessageTimestamp` 필드를 사용하고 `Instant` 타입으로 처리.
    - [x] `DmRemoteDataSource.kt` 및 `DmRemoteDataSourceImpl.kt`:
        - [x] 원칙적으로 `DmRemoteDataSource` 및 구현체는 **제거 대상**입니다. DM 관련 기능은 `ChannelRemoteDataSource`에서 `ChannelType.DM` 조건으로 처리해야 합니다.
        - [x] 만약 특정 DM 관련 로직(예: `users/{userId}/activeDmIds` 업데이트)이 남아있다면, 해당 로직만 유지하고 나머지는 제거하거나 `ChannelRemoteDataSource`로 기능을 이전합니다.
        - [x] `DmChannelDto` 대신 `ChannelDto` (또는 `Channel` 모델)를 사용하도록 인터페이스 및 구현 수정.
        - [x] `FirestoreConstants.Collections.CHANNELS`를 사용하여 쿼리.
- **적용 위치**: `data/src/main/java/com/example/data/datasource/local/dm/`, `data/src/main/java/com/example/data/datasource/remote/dm/`

### 그룹 2: `ChannelRemoteDataSourceImpl` 및 `ChannelRepositoryImpl` 오류

- **문제**: `Query` vs `CollectionReference` 타입 불일치, `ChannelType` 및 `ChannelMetadataKeys` 등 상수 참조 오류, 인터페이스 미구현, 메서드 오버라이드 오류, 반환 타입 불일치.
- **이유**: `channelReferences` 제거 및 `metadata` 기반 조회로 변경하는 과정에서 쿼리 로직 수정 미흡, 인터페이스 변경 미반영.
- **해결 방법**:
    - [x] `ChannelRemoteDataSourceImpl.kt`:
        - [x] `getChannels`, `getChannelsStream` 등 Firestore 쿼리 시 `.collection()` 결과는 `CollectionReference`이며, `.whereEqualTo()` 등 필터링 결과는 `Query`입니다. 변수 타입 선언을 확인하고, 필요한 경우 `firestore.collection(CHAINS).whereArrayContains(...)` 형태로 직접 쿼리합니다.
        - [x] `ChannelTypeValues` (예: `ChannelTypeValues.DM`) 또는 `ChannelType` enum 클래스를 올바르게 참조합니다.
        - [x] `ChannelMetadataKeys`를 올바르게 참조하여 `metadata` 필드 쿼리.
    - [x] `ChannelRepositoryImpl.kt`:
        - [x] `ChannelRepository` 인터페이스에 정의된 모든 추상 메서드를 구현합니다. (시그니처 일치 확인)
        - [x] `Query` vs `CollectionReference` 타입 문제 수정 (위와 동일).
        - [x] `ChannelType` 및 `ChannelMetadataKeys` 참조 수정.
        - [x] `createProjectChannel` 등 채널 생성/수정 시 `order` 필드 및 `metadata` (projectId, categoryId 등)를 올바르게 설정하도록 수정.
        - [x] `addParticipant`, `removeParticipant` 등은 DM 채널의 `participantIds` 필드를 업데이트하거나, 프로젝트 채널의 경우 `ProjectMemberRepository` 관련 로직으로 처리해야 합니다. (스키마 변경에 따라 로직 재검토)
- **적용 위치**: `data/src/main/java/com/example/data/datasource/remote/channel/`, `data/src/main/java/com/example/data/repository/`

### 그룹 3: `ChatRemoteDataSourceImpl` 오류

- **문제**: `FirestoreConstants` 참조 오류, 인터페이스 미구현, 메서드 오버라이드 오류.
- **이유**: `FirestoreConstants` 경로 변경 또는 임포트 누락, 인터페이스 변경 미반영.
- **해결 방법**:
    - [x] `FirestoreConstants` 임포트 확인 및 경로 수정 (`com.example.core_common.constants.FirestoreConstants`).
    - [x] `ChatRemoteDataSource` 인터페이스에 정의된 모든 추상 메서드를 구현합니다.
    - [x] 메서드 시그니처가 인터페이스와 일치하는지 확인하여 오버라이드 오류 수정.
- **적용 위치**: `data/src/main/java/com/example/data/datasource/remote/chat/`

### 그룹 4: 기타 DataSource 및 Repository 오류 (`Friend`, `Project`, `ProjectRole`, `ProjectStructure`, `User`)

- **문제**: 존재하지 않는 상수/필드/메서드 참조, 인터페이스 미구현, 타입 불일치.
- **이유**: 전반적인 리팩토링 과정에서 발생한 자잘한 누락 및 변경사항 미반영.
- **해결 방법**:
    - [x] 각 파일의 에러 로그를 기반으로 존재하지 않는 상수(`DmFields`, `DISPLAY_NAME`, `PARTICIPATING_MEMBERS` 등)를 `FirestoreConstants`의 올바른 값으로 대체하거나, 스키마 변경에 따라 로직을 수정합니다.
    - [x] `ProjectStructureRemoteDataSourceImpl`: 인터페이스 미구현된 메서드 추가. `getCurrentUserId`는 `auth.currentUser?.uid` 등으로 대체. 채널 생성/수정 시 `ChannelType`을 올바르게 사용.
    - [x] `ProjectRepositoryImpl`: `createDirectChannel` 등 삭제된 기능 호출부 수정 (아마도 `ChannelRepository.createChannel` 등으로 대체).
    - [x] `ProjectStructureRepositoryImpl`: 삭제된 메서드(`getProjectCategories` 등) 호출부 수정. `reorderCategory` 등은 채널 직접 업데이트 로직으로 변경.
    - [x] `UserRepositoryImpl`: `FirestoreConstants` 및 `FC` (아마도 `FCM_TOKEN`의 오타) 참조 수정.
- **적용 위치**: 각 해당 DataSource 및 Repository 파일.

## 진행 상황 요약

모든 계획된 에러 수정 작업이 완료되었습니다. 주요 변경사항은 다음과 같습니다:

1. DM 채널 관련 코드를 Channel 모델을 사용하도록 통합하였습니다.
2. 참조되지 않거나 사용되지 않는 DM 관련 코드를 제거하고, DM 기능을 Channel 관련 로직에 통합하였습니다.
3. ChannelType 및 메타데이터 관련 상수를 올바르게 참조하도록 수정하였습니다.
4. 인터페이스가 변경된 구현체들의 메서드 시그니처를 일치시키고, 미구현된 메서드를 추가하였습니다.
5. 프로젝트 구조 관련 리포지토리의 메서드를 새로운 데이터 모델에 맞게 업데이트하였습니다.

이제 앱은 최신 스키마와 모델 구조를 사용하여 컴파일되고 실행될 수 있습니다. 