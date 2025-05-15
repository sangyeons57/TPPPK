# 테스크: 날짜/시간 처리 표준화

프로젝트 전체에서 날짜 및 시간 처리 방식을 표준화하고 `DateTimeUtil.kt` 사용을 중앙 집중화합니다.

- **Firebase 서버 (Firestore):** `com.google.firebase.Timestamp`
- **중간 계층 (데이터 소스, 리포지토리, 매퍼, 유스케이스, 도메인 모델):** `java.time.Instant`
- **UI 계층 (표시 직전):** `java.time.LocalDateTime`, `java.time.LocalTime`, 포맷된 `String`

## 단계

- [x] **단계 1: `DateTimeUtil.kt` 감사 및 최종화**
    - [x] `DateTimeUtil.kt`가 다음을 위한 강력한 메서드를 포함하고 있는지 확인합니다:
        - `Instant` <-> `Firebase.Timestamp` 변환 (예: `instantToFirebaseTimestamp`, `firebaseTimestampToInstant`).
        - 현재 시간을 `Instant`로 가져오기 (예: `nowInstant()`).
        - 현재 시간을 `Firebase.Timestamp`로 가져오기 (예: `nowFirestoreTimestamp()`).
    - [x] `DateTimeUtil.kt`의 모든 관련 공개 메서드가 새로운 시간 처리 전략에서의 역할을 설명하는 KDoc으로 잘 문서화되어 있는지 확인합니다.

- [x] **단계 2: 원격 데이터 소스 구현 리팩토링 (`data/.../datasource/remote/`)**
    - [x] `MessageRemoteDataSourceImpl.kt`: 모든 `Timestamp.now()` 또는 `FieldValue.serverTimestamp()` 호출을 `DateTimeUtil`의 해당 메서드(예: `nowInstant()`, `nowFirestoreTimestamp()`, `instantToFirebaseTimestamp()`)로 교체합니다. Firestore와 상호작용 시 `Firebase.Timestamp`를 사용하고, 내부적으로 또는 반환 시에는 `Instant`를 사용하도록 합니다.
    - [x] `ChannelRemoteDataSourceImpl.kt`: 위와 동일한 방식으로 리팩토링합니다.
    - [x] `UserRemoteDataSourceImpl.kt` (및 관련 `UserDto` 또는 `UserMapper`): 위와 동일하게 리팩토링합니다. (모든 업데이트 메서드에서 `updatedAt`을 `DateTimeUtil.nowFirebaseTimestamp()`로 설정하도록 추가 수정됨)
    - [x] `ScheduleRemoteDataSourceImpl.kt` (및 관련 `ScheduleDto` 또는 `ScheduleMapper`): 위와 동일하게 리팩토링합니다.
    - [x] `InviteRemoteDataSourceImpl.kt`: 위와 동일하게 리팩토링합니다.
    - [x] `FriendRemoteDataSourceImpl.kt` (및 관련 `FriendRelationshipDto` 또는 `FriendMapper`): 위와 동일하게 리팩토링합니다.
    - [x] `ProjectRemoteDataSourceImpl.kt` (및 관련 `ProjectDto` 또는 `ProjectMapper`): 위와 동일하게 리팩토링합니다.
    - [x] `AuthRemoteDataSourceImpl.kt`: 회원 가입 또는 프로필 생성 시 `createdAt` 필드를 `DateTimeUtil`을 사용하여 설정하도록 합니다.
    - [x] `ProjectMemberRemoteDataSourceImpl.kt`: 멤버 추가 시 `joinedAt`과 같은 타임스탬프 필드를 `DateTimeUtil`을 사용하여 설정하도록 합니다.
    - [x] `ProjectRoleRemoteDataSourceImpl.kt`: 역할 생성/수정 시 `createdAt`, `updatedAt` 필드를 `DateTimeUtil`을 사용하여 설정하도록 합니다.
    - [x] `ProjectStructureRemoteDataSourceImpl.kt` (카테고리/채널 생성/수정 로직): 관련 타임스탬프 필드를 `DateTimeUtil`을 사용하여 설정하도록 합니다. (`updateCategoryChannel`, `updateProjectChannel`에서 `updatedAt` 설정 추가됨)

- [x] **단계 3: 리포지토리 구현 리팩토링 (`data/.../repository/`)**
    - [x] 모든 `*RepositoryImpl.kt` 파일 대상:
        - 리포지토리 인터페이스 및 구현의 메서드 시그니처가 유스케이스 및 데이터 소스와 교환되는 시간 관련 데이터에 대해 일관되게 `Instant`를 사용하는지 확인합니다.
        - 리포지토리에 시간 로직이 직접 존재하는 경우, `DateTimeUtil.kt` 및 `Instant`를 사용하도록 리팩토링합니다.

- [x] **단계 4: 매퍼 리팩토링 (예: `data/.../mapper/` 또는 데이터 소스/리포지토리에 포함된 매퍼)**
    - [x] 모든 데이터 매핑 로직(예: Firestore 문서 -> 도메인 모델)을 식별하고 업데이트합니다.
    - [x] Firestore `Timestamp` 필드와 도메인 모델 `Instant` 필드 간의 변환이 `DateTimeUtil.firebaseTimestampToInstant()` 및 `DateTimeUtil.instantToFirebaseTimestamp()` 메서드를 일관되게 사용하는지 확인합니다.

- [x] **단계 5: 도메인 모델 확인 (`domain/model/`)**
    - [x] 모든 도메인 모델 파일(`java.time.Instant`을 사용해야 하는 필드가 있는 파일)을 검토합니다.
    - [x] 시간 관련 필드가 일관되게 `java.time.Instant`를 사용하는지 확인합니다 (필요에 따라 nullable `Instant?`). `java.util.Date`, `Firebase.Timestamp`, `LocalDateTime` 등의 다른 시간 유형을 사용하지 않도록 합니다.
    - [x] 필요한 경우 생성자 또는 기본값에서 `DateTimeUtil.nowInstant()`를 사용하여 `Instant` 필드를 초기화하도록 합니다.

- [x] **단계 6: 유스케이스 리팩토링 (`domain/usecase/`)**
    - [x] 시간 데이터를 처리하거나 생성하는 모든 유스케이스를 검토하고 업데이트합니다.
    - [x] 유스케이스가 시간 관련 매개변수를 받거나 시간 관련 데이터를 반환할 때 일관되게 `Instant`를 사용하는지 확인합니다.
    - [x] 시간 생성 또는 조작 로직이 필요한 경우 `DateTimeUtil.kt` (예: `nowInstant()`)를 사용합니다.

- [x] **단계 7: ViewModel 및 UI 계층 조정**
    - [x] `Instant` 데이터를 UI 표시에 적합한 형식(예: `LocalDateTime`, `LocalTime`, 포맷된 `String`)으로 변환하는 로직을 ViewModel 또는 UI 유틸리티 클래스로 이동/중앙 집중화합니다.
    - [x] UI 표시에 `DateTimeUtil.kt`의 변환 메서드(예: `instantToLocalDateTime`, `formatInstant`)를 사용합니다.
    - [x] 사용자 입력(예: 날짜/시간 선택기)을 다시 `Instant`로 변환하여 유스케이스/리포지토리로 전달해야 하는 경우 `DateTimeUtil.kt`를 사용합니다.
    - [x] 도메인 모델에서 UI 포맷팅 헬퍼 메서드 제거 완료.