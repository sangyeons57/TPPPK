# Task: core_navigation 모듈 리팩토링 마무리

- [x] Step 1: `NavigationManager` 오버라이드 오류 해결 (`navigateToTab`, `navigateClearingBackStack`, `navigateToNestedGraph`의 `suspend` 및 기본값 불일치 수정).
- [x] Step 2: `NavigationResultListener`의 리스너 방식 메서드(`observeResult`, `removeResultListener`, `clearAllResultListeners`) 제거 결정 및 실행 (인터페이스 및 `NavigationManager`에서 제거).
- [x] Step 3: `NavigationManager.setResult`에서 `CoroutineScope`를 사용하여 안전하게 Flow 발행하도록 수정.
- [x] Step 4: `AppNavHost`가 `navigationManager.navigationCommands`를 구독하여 실제 네비게이션을 실행하도록 수정.
- [x] Step 5: `NavigationManager.setNavController` 로직 검토 및 개선 (최상위 컨트롤러 설정 명확화).
- [x] Step 6: `NavigationHandler` 인터페이스에서 `delegateToParent` 메서드 제거.
- [x] Step 7: 불필요한 `NestedNavigationManagerFactory.kt` 파일 삭제.
- [x] Step 8: `NavigationModule.kt`에서 주석 처리된 `companion object` 및 불필요한 `@Provides` 함수들 완전히 제거.
- [x] Step 9: `AppNavHost.kt`에서 `@Composable` 호출 오류 수정 (`nestedGraphs` 파라미터 타입에서 `@Composable` 제거).
- [x] Step 10: `NavigationResultListener` 중복 선언 오류 해결 (`ComposeNavigationHandler.kt` 파일 내 선언 제거).
- [x] Step 11: (Reverted & Fixed) `NavigationManager.navigate`를 non-suspend로 변경하고 내부에서 `scope.launch` 사용. 관련 인터페이스(`NavigationHandler`, `ComposeNavigationHandler`), 확장 함수(`DestinationExtensions.kt`), ViewModel(`NavigationViewModel.kt`)에서 `suspend` 및 `viewModelScope.launch` 제거.
- [x] Step 12: `NavigationViewModel.kt`에서 변경된 `NavigationManager` API를 사용하도록 업데이트 (삭제된 메서드 호출 수정, `navigate` 및 `getResultFlow` 사용).
- [x] Step 13: (Error Fix) `ComposeNavigationHandler` 인터페이스의 기본 `navigateTo` 메서드들을 `suspend`로 변경.
- [x] Step 14: (Error Fix) `NavigationResultListener.kt` 파일을 확인하고 리스너 메서드가 없는지 재확인 (필요시 수정).
- [x] Step 15: (Error Fix) `NavigationViewModel.kt`의 `navigateAndReplaceTop` 함수에서 `navigationManager.currentDestination` 접근 로직 수정.
- [x] Step 16: (Error Fix) `DestinationExtensions.kt`의 확장 함수들을 `suspend`로 변경하여 `navigateTo` 호출 오류 해결.
- [ ] Step 17: (선택적/추후) 프로젝트 전체에서 `NestedNavHost` 및 `nestedComposable` 사용 부분을 찾아 `NavigationManager`와 `NavHost`를 사용하도록 수정. 