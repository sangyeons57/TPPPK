# Task: 네비게이션 파라미터 처리 리팩토링

## 목표
AppNavigation.kt에서 화면 Composable로 직접 파라미터를 전달하는 대신, 모든 화면이 ViewModel에서 SavedStateHandle을 통해 네비게이션 파라미터를 일관되게 가져오도록 리팩토링합니다. SavedStateHandle 사용의 편의성을 높이기 위한 유틸리티를 도입합니다.

## 세부 단계

- [x] 1단계: 파라미터 직접 수신 화면 식별
    - `AppNavigation.kt` 파일을 분석하여 `composable` 람다 내에서 `it.arguments?.get...`을 사용하거나, 화면 Composable 함수 시그니처에 직접 파라미터(예: `projectId: String`)를 받는 화면들을 모두 찾아 리스트업합니다.
    - **확인된 화면 목록:**
        - ProjectSettingScreen
        - CreateCategoryScreen
        - CreateChannelScreen
        - EditCategoryScreen
        - EditChannelScreen
        - MemberListScreen
        - EditMemberScreen
        - RoleListScreen
        - EditRoleScreen
        - ScheduleDetailScreen

- [x] 2단계: `SavedStateHandle` 확장 함수/유틸리티 구현
    - `SavedStateHandle`에서 타입 안전하게 필수/옵션 파라미터를 가져오는 확장 함수 또는 유틸리티 클래스를 설계하고 구현합니다. (예: `getRequiredString(key)`, `getOptionalInt(key)`)
    - `AppDestinations.kt`의 `XXX_ARG` 상수들을 활용하도록 합니다.
    - `core_common/src/main/java/com/example/core_common/navigation/SavedStateHandleUtils.kt` 에 구현 완료.

- [x] 3단계: 식별된 화면 리팩토링 (개별 또는 그룹 진행)
    - 식별된 각 화면에 대해 다음 작업을 수행했습니다:
        - [x] ProjectSettingScreen 완료
        - [x] CreateCategoryScreen 완료
        - [x] CreateChannelScreen 완료
        - [x] EditCategoryScreen 완료
        - [x] EditChannelScreen 완료
        - [x] MemberListScreen 완료 (ViewModel 생성 포함)
        - [x] EditMemberScreen 완료
        - [x] RoleListScreen 완료
        - [x] EditRoleScreen 완료
        - [x] ScheduleDetailScreen 완료
        
        *(작업 내용)*
        - [x] 3.1: 해당 화면의 ViewModel이 `SavedStateHandle`을 주입받도록 합니다.
        - [x] 3.2: ViewModel의 `init` 블록이나 관련 로직에서 2단계에서 만든 유틸리티와 `AppDestinations.kt`의 상수를 사용하여 필요한 파라미터를 `SavedStateHandle`로부터 가져오도록 수정합니다.
        - [x] 3.3: `AppNavigation.kt`에서 해당 화면의 `composable` 호출 시 직접 전달하던 파라미터를 제거합니다. Composable 함수 시그니처에서도 해당 파라미터를 제거합니다.

- [x] 3.5: 컴파일 오류 수정
    - `MemberListScreen.kt` 및 `MemberListViewModel.kt`:
        - ViewModel에 누락된 함수(`onSearchQueryChanged`, `requestDeleteMember`, `confirmDeleteMember`) 추가 및 `onMemberClick` 시그니처 수정 (`userId: String` -> `member: ProjectMember`).
        - Screen에서 ViewModel 함수 호출 시 올바른 파라미터 사용 (`onMemberClick`에 `ProjectMember` 전달).
        - Screen UI 컴포저블 (`MemberListContent`, `ProjectMemberListItemComposable`)에서 `ProjectMemberItem` 모델 사용 일관성 확보.
        - ViewModel의 `uiState.members`가 `List<ProjectMemberItem>`을 가지도록 수정하고, Screen의 `LazyColumn`에서 이를 사용하도록 수정.
        - 누락된 `ShowAddMemberDialog` 이벤트 정의 및 `Icons.Default.Search` import 추가.
    - `ChannelType` 관련 (`CreateChannel*`, `EditChannel*` 파일):
        - ViewModel 파일 내 로컬 `ChannelType` enum 제거.
        - 모든 관련 파일에서 `com.example.domain.model.ChannelType` 사용하도록 통일 및 import 경로 수정.

- [ ] 4단계: 테스트 및 검증
    - 리팩토링된 화면들이 네비게이션 시 파라미터를 올바르게 수신하고 동작하는지 테스트합니다.
    - 특히 필수 파라미터 누락 시 등의 예외 케이스를 확인합니다.

- [ ] 5단계: 정리
    - 관련 없는 코드나 임포트 등을 정리합니다. 