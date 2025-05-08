# 네비게이션 아키텍처 리팩토링 실행 계획

## 1단계: 인터페이스 정리 및 역할 명확화

- [x] 1.1: NavigationHandler 인터페이스에서 기본 구현 제거 및 순수 추상 인터페이스로 변경
- [x] 1.2: ComposeNavigationHandler 인터페이스에서 불필요한 구현 제거 및 중첩 네비게이션 지원 메서드 추가
- [x] 1.3: NavigationManager 구현체 업데이트로 모든 인터페이스 메서드 구현 및 AppRoutes 일관성 확보

## 2단계: 중첩 네비게이션 처리 개선

- [x] 2.1: MainScreen의 중첩 NavController를 NavigationHandler에 등록하는 메커니즘 구현
- [x] 2.2: Bottom Navigation을 직접 NavController 호출에서 NavigationHandler 사용으로 전환
- [x] 2.3: 자식 컴포넌트로의 네비게이션 일관성 확보

## 3단계: 일관성 개선 작업

- [x] 3.1: 모든 컴포넌트의 매개변수 이름을 'navigationHandler'로 통일
- [x] 3.2: AppNavigationGraph에서 NavigationCommand 처리 로직 정리
- [x] 3.3: 표준 네비게이션 패턴 가이드 문서화

## 4단계: 테스트 지원 강화

- [x] 4.1: TestNavigationHandler 구현
- [x] 4.2: 네비게이션 테스트 헬퍼 및 유틸리티 함수 추가
- [ ] 4.3: 주요 네비게이션 시나리오에 대한 테스트 작성

## 5단계: 배포 및 교육

- [ ] 5.1: 변경 사항 검증을 위한 통합 테스트 실행
- [ ] 5.2: 개발 팀을 위한 문서 및 예제 작성
- [ ] 5.3: 점진적인 마이그레이션 전략 수립

## 진행 상황

### 1단계 완료 (인터페이스 정리 및 역할 명확화)

- NavigationHandler 인터페이스를 순수 추상 인터페이스로 변경하여 불필요한 기본 구현을 제거했습니다.
- ComposeNavigationHandler 인터페이스에 중첩 네비게이션 지원을 위한 메서드(setChildNavController)를 추가했습니다.
- NavigationManager 구현체를 업데이트하여 새로운 인터페이스 메서드들을 모두 구현하고, AppRoutes를 일관성 있게 사용하도록 수정했습니다.

### 2단계 완료 (중첩 네비게이션 처리 개선)

- MainScreen에 DisposableEffect를 추가하여 중첩 NavController를 NavigationHandler에 등록하고 화면이 사라질 때 등록 해제하도록 구현했습니다.
- Bottom Navigation 탭 전환을 NavigationHandler.navigateToTab() 메서드를 사용하도록 변경했습니다.
- NavigationManager의 navigateToTab() 메서드를 개선하여 중첩 NavController가 있는 경우 직접 처리하고, 없는 경우 먼저 Main 화면으로 이동하는 로직을 구현했습니다.
- AppNavigationGraph의 NavigationCommand 처리 로직을 더 명확하고 안정적으로 개선했습니다.
- NavigationCommand 클래스를 정리하여 필요한 명령만 유지하고 문서화했습니다.

### 3단계 완료 (일관성 개선 작업)

- AppNavigationGraph에서 NavigationCommand 처리 로직을 별도의 함수로 분리하여 정리했습니다.
- 일부 UI 컴포넌트(SplashScreen, LoginScreen 등)에서 navigationManager 변수명을 navigationHandler로 변경하여 일관성을 개선했습니다.
- 표준 네비게이션 패턴 가이드 문서(navigation_patterns.mdc)를 작성하여 개발자들이 참조할 수 있도록 했습니다.
- NavigationCommand 클래스를 정리하고 모든 명령에 자세한 문서화를 추가했습니다.

### 4단계 진행 중 (테스트 지원 강화)

- TestNavigationHandler 클래스를 구현하여 네비게이션 명령을 캡처하고 검증할 수 있도록 했습니다.
- NavigationTestUtils 클래스를 추가하여 네비게이션 테스트를 위한 다양한 유틸리티 함수를 제공했습니다.
- 남은 작업: 주요 네비게이션 시나리오에 대한 테스트 코드 작성이 필요합니다. 