# Core Navigation Module

이 모듈은 Projecting Kotlin 앱의 네비게이션 기능을 담당합니다.

## 개요

이전에는 네비게이션 관련 코드가 `core_common` 모듈에 포함되어 있었지만, 네비게이션 기능이 확장됨에 따라 별도의 모듈로 분리되었습니다. 이 모듈화를 통해 다음과 같은 이점을 얻을 수 있습니다:

- 네비게이션 로직의 명확한 분리
- 의존성 관리 개선
- 필요한 경우에만 네비게이션 컴포넌트 포함
- 더 나은 코드 구조화 및 유지보수성

## 구조

모듈의 주요 구성 요소는 다음과 같습니다:

```
core_navigation/
├── di/                        # 의존성 주입 관련 클래스
│   └── NavigationModule.kt    # 네비게이션 컴포넌트 제공
├── NavigationCommand.kt       # 네비게이션 작업 정의
├── NavigationHandler.kt       # 네비게이션 명령 실행 인터페이스
├── NavigationManager.kt       # 상위 수준 네비게이션 API
└── ...                        # 기타 네비게이션 유틸리티
```

## 사용 방법

이 모듈을 사용하려면 다음과 같이 의존성을 추가하세요:

```kotlin
// settings.gradle.kts
implementation(project(":core:core_navigation"))
```

자세한 사용법은 [Navigation System Documentation](src/main/java/com/example/core_navigation/README.md)을 참조하세요.

## 모듈 마이그레이션

이 모듈은 다음과 같은 파일들을 `core_common/navigation`에서 이동했습니다:

- AppDestinations.kt
- SavedStateHandleUtils.kt
- NavigationManagerExtensions.kt
- NavigationManager.kt
- NavControllerExtensions.kt
- NavigationManagerComposables.kt
- NestedNavigationManagerFactory.kt
- SavedStateNavigationResultHandler.kt
- NavigationResultListener.kt
- NavigationHandler.kt
- NavigationCommand.kt
- ComposeNavigationHandler.kt

마이그레이션 작업을 완료하려면 다음을 확인하세요:

1. 모든 모듈의 `build.gradle.kts` 파일을 업데이트하여 `core_navigation` 의존성을 추가
2. 임포트 경로 업데이트: `com.example.core_common.navigation.*` → `com.example.core_navigation.*`
3. 기존 core_common 모듈에서 네비게이션 관련 코드 제거 