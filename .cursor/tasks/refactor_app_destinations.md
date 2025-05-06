# Task: Refactor Navigation to Separate Route Definitions and Graph Construction

**Note:** The primary navigation definition files are located in the `core_navigation` module:
*   `AppRoutes.kt`: `core_navigation/src/main/java/com/example/core_navigation/routes/AppRoutes.kt`
*   `AppNavigationGraph.kt`: `core_navigation/src/main/java/com/example/core_navigation/graph/AppNavigationGraph.kt`
*   `BottomNavConfig.kt`: `core_navigation/src/main/java/com/example/core_navigation/BottomNavConfig.kt`

## Overall Goal:
-   `AppRoutes.kt` (in `core_navigation/src/main/java/com/example/core_navigation/routes/`): 경로 문자열, 인수, 관련 메타데이터를 '정의'. - **DONE**
-   `AppNavigationGraph.kt` (in `core_navigation/src/main/java/com/example/core_navigation/graph/`): `AppRoutes.kt`의 정의를 사용하여 실제 `NavHost`, `composable`, `navigation`으로 네비게이션 '구조'를 '구축'. - **DONE**
-   기존 `AppNavigation.kt`의 `composableFactories` 방식을 새로운 그래프 방식으로 리팩토링. - **DONE (by creating AppNavigationGraph.kt)**
-   `NavigationManager.kt` (as `ComposeNavigationHandler`)를 새 구조에 맞게 업데이트. - **DONE**

---

## 남은 작업 목록 (세부 실행 계획)

*이 목록은 네비게이션 리팩토링 완료를 위해 수행해야 할 구체적인 작업들을 포함합니다.*

### 그룹 1: 코어 네비게이션 로직 최종화

*   **Task 1.1: `NavigationCommand` 핸들링 검증 (in `NavigationManager.kt`):** - **PARTIALLY DONE**
    *   **파일:** [NavigationManager.kt](mdc:core/core_navigation/src/main/java/com/example/core_navigation/core/NavigationManager.kt)
    *   **내용:** `NavigationManager`가 `NavController`를 사용하여 모든 종류의 `NavigationCommand`를 올바르게 처리하는지 확인. `navigateToTab`, `navigateClearingBackStack`, `navigateToNestedGraph` 등의 로직이 새 `AppRoutes`와 연동되어 잘 동작하는지 검토. (이전에 `AppNavigationGraph.kt`로 잘못 기재되었던 부분 수정)

*   **Task 1.2: `feature_schedule` 모듈 네비게이션 업데이트:** - **DONE**
    *   **파일:** `feature_schedule` 모듈 내 모든 화면 Composable 및 ViewModel.
        *   [CalendarScreen.kt](mdc:feature/feature_main/src/main/java/com/example/feature_main/ui/calendar/CalendarScreen.kt) (Main feature에 있지만 schedule 기능) - **DONE**
        *   `Calendar24HourScreen.kt` (경로: `AppRoutes.Main.Calendar.calendar24Hour`) - **DONE**
        *   `AddScheduleScreen.kt` (경로: `AppRoutes.Main.Calendar.addSchedule`) - **DONE**
        *   `ScheduleDetailScreen.kt` (경로: `AppRoutes.Main.Calendar.scheduleDetail`) - **DONE**
    *   **내용:** 각 화면 Composable 함수의 `navigationManager` 파라미터 타입을 `ComposeNavigationHandler`로 변경하고, 네비게이션 호출을 `AppRoutes` 및 `NavigationCommand`를 사용하도록 수정합니다. `SavedStateHandle`을 사용하는 ViewModel은 인수 키를 `AppRoutes` 상수로 변경합니다.

*   **Task 1.3: (완료) 나머지 Feature 모듈 네비게이션 업데이트:** - **DONE**
    *   **파일:** `feature_dev`, `feature_auth`, `feature_main`, `feature_project`, `feature_friends`, `feature_settings`, `feature_chat`, `feature_search` 내 모든 관련 화면 및 ViewModel.
    *   **내용:** `ComposeNavigationHandler` 타입 사용, `AppRoutes` 및 `NavigationCommand` 사용, `SavedStateHandle` 키 업데이트 완료.

### 그룹 2: 하단 네비게이션 (완료)

*   **Task 2.1: (완료) 하단 네비게이션 바 컴포저블 업데이트:** - **DONE**
    *   **파일:** [BottomNavConfig.kt](mdc:core_navigation/src/main/java/com/example/core_navigation/BottomNavConfig.kt), [MainScreen.kt](mdc:feature/feature_main/src/main/java/com/example/feature_main/MainScreen.kt)
    *   **내용:** `BottomNavConfig.kt`가 `AppRoutes`를 사용하고, `MainScreen.kt`의 `mainScreenBottomBar`가 이를 올바르게 사용함.

*   **Task 2.2: (완료) 메인 화면에서 탭 클릭 처리 로직 업데이트:** - **DONE**
    *   **파일:** [MainScreen.kt](mdc:feature/feature_main/src/main/java/com/example/feature_main/MainScreen.kt)
    *   **내용:** `MainScreen.kt`의 `NavHost` 및 `mainScreenBottomBar`가 `AppRoutes` 및 `NavigationManager`를 통해 탭 전환을 올바르게 처리함.

### 그룹 3: 코드베이스 전체 네비게이션 호출 업데이트 (부분 완료)

*   **Task 3.1: 기존 네비게이션 패턴 추가 검색/검증:** - **TODO (Final Sweep)**
    *   **파일:** 전체 코드베이스 (`feature_*` 모듈의 ViewModel, UI 상태 홀더, Composable 등)
    *   **내용:** 모든 feature 모듈 검토 완료. 최종적으로 다음 패턴들을 재검색/검증합니다:
        *   삭제된 `AppDestination` 객체 사용 흔적.
        *   문자열 리터럴을 사용하는 네비게이션 호출.
        *   (레거시) `ScreenRoute` 또는 `ScreenRouteWithArgs` 타입을 사용하는 호출 (존재 시 마이그레이션 또는 @Deprecated 처리).

*   **Task 3.2: (완료된 부분 외) 타입 안전한 네비게이션 호출로 리팩토링:** - **DONE / TODO for Task 3.1 findings**
    *   **파일:** Task 3.1에서 식별된 모든 파일.
    *   **내용:** 식별된 네비게이션 호출들을 새로운 타입 안전한 방식으로 수정.

*   **Task 3.3: (완료된 부분 외) `SavedStateHandle` 인수 키 업데이트:** - **DONE / TODO for Task 3.1 findings**
    *   **파일:** Task 3.1에서 식별된 `SavedStateHandle`을 사용하는 ViewModel.
    *   **내용:** 하드코딩된 인수 키 대신 `AppRoutes` 상수를 사용.

### 그룹 4: 최종 정리 및 테스트

*   **Task 4.1: (완료) Import 문 정리 및 추가 (in `AppNavigationGraph.kt`):** - **DONE**

*   **Task 4.2: TODO 주석 처리:** - **TODO**
    *   **파일:** 전체 코드베이스 (특히 [AppNavigationGraph.kt](mdc:core_navigation/src/main/java/com/example/core_navigation/graph/AppNavigationGraph.kt))
    *   **내용:** `ProjectDetailScreen`, `UserProfileScreen`, `EditScheduleScreen` 플레이스홀더 실제 구현 또는 별도 작업으로 연결. 기타 `// TODO:` 주석들 처리.

*   **Task 4.3: 네비게이션 테스트:** - **TODO** (사용자 메뉴얼 테스트)
    *   **내용:** 앱을 실행하여 모든 화면 간 네비게이션, 인수 전달, 뒤로 가기, 하단 탭 전환 등이 예상대로 동작하는지 철저히 테스트합니다.

*   **Task 4.4: 코드 리뷰:** - **TODO** (사용자/팀 리뷰)
    *   **파일:** 변경된 모든 파일 (주요 파일: [AppRoutes.kt](mdc:core_navigation/src/main/java/com/example/core_navigation/routes/AppRoutes.kt), [AppNavigationGraph.kt](mdc:core_navigation/src/main/java/com/example/core_navigation/graph/AppNavigationGraph.kt), [NavigationManager.kt](mdc:core/core_navigation/src/main/java/com/example/core_navigation/core/NavigationManager.kt), [MainActivity.kt](mdc:app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/MainActivity.kt), 기타 `feature_*` 모듈 파일들)
    *   **내용:** 변경된 코드 전체에 대해 가독성, 유지보수성, 컨벤션 준수 등을 검토합니다.

*   **Task 4.5: 네비게이션 결과 리스너 검토:** - **TODO**
    *   **파일:** [NavigationManager.kt](mdc:core/core_navigation/src/main/java/com/example/core_navigation/core/NavigationManager.kt), `AddScheduleScreen.kt`, `EditScheduleScreen.kt`, `CalendarScreen.kt`, `ScheduleDetailScreen.kt`
    *   **내용:**
        *   `SavedStateHandle`을 사용한 새로운 네비게이션 결과 전달 메커니즘 (키: `"schedule_added_or_updated"`)이 `Add/EditScheduleScreen`과 `CalendarScreen`/`ScheduleDetailScreen` 간에 올바르게 동작하는지 검토.
        *   기존 프로젝트의 `setResult`/`getResultFlow` 메커니즘 (`NavigationManager` 및 `MainScreen`의 `calendarRefreshKey` 등)을 실제로 사용하는지 확인. 불필요하거나 중복될 경우 관련 코드 단순화 또는 제거 고려.

*   **Task 4.6: (신규) `core` 모듈 경로 수정:** - **DONE** (이번 대화에서 발생한 경로 오류 수정)
    *   **내용:** `core/core_navigation` 대신 `core_navigation` 모듈 경로를 사용하도록 수정 완료.

---