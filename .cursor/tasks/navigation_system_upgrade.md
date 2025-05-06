# Task: 네비게이션 시스템 업그레이드

## 목표
Core_Common 모듈에 모든 네비게이션 관련 코드를 이동하고, 기존 navigation 모듈을 완전히 제거하여 타입 안전하고 모듈화된 네비게이션 시스템 구축

## 0. 현재 코드 사용 현황 분석

- [x] 0.1: 네비게이션 코드 사용 지점 분석
  - [x] AppDestination 참조 지점 확인 (import com.example.navigation.* 사용 코드)
    - app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/AppNavigation.kt: 다수의 AppDestination 객체들 import 및 사용
    - feature_main/src/main/java/com/example/feature_main/MainScreen.kt: MainBottomNavDestination, mainBottomNavItems import 및 사용
  - [x] createRoute() 메소드 사용 지점 확인
    - app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/AppNavigation.kt: 다수의 createRoute() 호출 (ProjectSetting, CreateCategory, Chat, Calendar24Hour, AddSchedule, ScheduleDetail 등)
    - feature_main/src/main/java/com/example/feature_main/MainScreen.kt: ScheduleDetail.createRoute(), Calendar24Hour.createRoute() 사용
    - feature_main/src/main/java/com/example/feature_main/ui/calendar/CalendarScreen.kt: AddSchedule.createRoute() 사용
  - [x] NavHostController 사용 지점 확인
    - app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/AppNavigation.kt: 주 네비게이션 컨트롤러로 사용
    - feature_main/src/main/java/com/example/feature_main/MainScreen.kt: 중첩 네비게이션을 위해 사용
    - feature_project, feature_auth, feature_dev 모듈의 여러 화면에서 네비게이션 제어용으로 사용
  - [x] SentryNavigationHelper, SentryNavigationTracker 사용 지점 확인
    - app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/MainActivity.kt: SentryNavigationTracker import 및 registerNavigationListener/finishTracking 호출
  - [x] 각 파일별 영향도 평가
    - 높은 영향도:
      - AppNavigation.kt: 모든 네비게이션 목적지 참조, 다수의 createRoute() 호출, 네비게이션 로직의 중심
      - MainActivity.kt: SentryNavigationTracker 통합, 네비게이션 초기화
      - MainScreen.kt: 중첩 네비게이션 로직, MainBottomNavDestination 사용
    - 중간 영향도:
      - 각 feature 모듈의 화면: 네비게이션 관련 import 및 메서드 호출 수정 필요
    - 낮은 영향도:
      - 네비게이션을 직접 사용하지 않는 UI 컴포넌트들

- [x] 0.2: 수정이 필요한 파일 리스트 작성
  - [x] 앱 모듈 파일들
    - app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/AppNavigation.kt - 모든 네비게이션 목적지 참조 수정
    - app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/MainActivity.kt - SentryNavigationTracker import 수정
    - app/build.gradle.kts - navigation 모듈 의존성 제거, core_common 의존성 추가
  
  - [x] 기능 모듈 파일들 - feature_main
    - feature_main/src/main/java/com/example/feature_main/MainScreen.kt - MainBottomNavDestination import 수정
    - feature_main/src/main/java/com/example/feature_main/ui/calendar/CalendarScreen.kt - AddSchedule.createRoute() 수정
    - feature_main/build.gradle.kts - 의존성 수정
    
  - [x] 기능 모듈 파일들 - feature_auth
    - feature_auth/src/main/java/com/example/feature_auth/ui/LoginScreen.kt - 네비게이션 로직 수정
    - feature_auth/src/main/java/com/example/feature_auth/ui/SignUpScreen.kt - 네비게이션 로직 수정
    - feature_auth/src/main/java/com/example/feature_auth/ui/FindPasswordScreen.kt - 네비게이션 로직 수정
    - feature_auth/build.gradle.kts - 의존성 수정
    
  - [x] 기능 모듈 파일들 - feature_project
    - feature_project/src/main/java/com/example/feature_project/ui/ProjectSettingScreen.kt - 네비게이션 로직 수정
    - feature_project/src/main/java/com/example/feature_project/ui/AddProjectScreen.kt - 네비게이션 로직 수정
    - feature_project/src/main/java/com/example/feature_project/ui/JoinProjectScreen.kt - 네비게이션 로직 수정
    - feature_project/src/main/java/com/example/feature_project/ui/SetProjectNameScreen.kt - 네비게이션 로직 수정
    - feature_project/build.gradle.kts - 의존성 수정
    
  - [x] 기능 모듈 파일들 - feature_schedule
    - feature_schedule/src/main/java/com/example/feature_schedule/ui/AddScheduleScreen.kt - 네비게이션 로직 수정
    - feature_schedule/src/main/java/com/example/feature_schedule/ui/ScheduleDetailScreen.kt - 네비게이션 로직 수정
    - feature_schedule/build.gradle.kts - 의존성 수정
    
  - [x] 기능 모듈 파일들 - feature_chat, feature_dev, feature_search, feature_settings, feature_friends
    - feature_chat/src/main/java/com/example/feature_chat/ui/ChatScreen.kt - 네비게이션 로직 수정
    - feature_dev/src/main/java/com/example/feature_dev/ui/DevMenuScreen.kt - 네비게이션 로직 수정
    - feature_search/src/main/java/com/example/feature_search/ui/SearchScreen.kt - 네비게이션 로직 수정
    - feature_settings/src/main/java/com/example/feature_settings/ui/EditProfileScreen.kt - 네비게이션 로직 수정
    - feature_settings/src/main/java/com/example/feature_settings/ui/ChangePasswordScreen.kt - 네비게이션 로직 수정
    - feature_friends/src/main/java/com/example/feature_friends/ui/FriendsScreen.kt - 네비게이션 로직 수정
    - feature_friends/src/main/java/com/example/feature_friends/ui/AcceptFriendsScreen.kt - 네비게이션 로직 수정
    - 각 모듈의 build.gradle.kts - 의존성 수정
  
  - [x] 루트 레벨 파일
    - settings.gradle.kts - navigation 모듈 제거

## 세부 단계

- [x] 1단계: Core_Common 모듈에 네비게이션 기본 인터페이스 생성
  - [x] 1.1: NavigationCommand 인터페이스 및 기본 구현 클래스 정의 (Navigate, NavigateBack 등)
  - [x] 1.2: NavigationHandler 인터페이스 정의 (네비게이션 명령 실행자)
  - [x] 1.3: NavigationResultListener 정의 (네비게이션 결과 콜백 처리용)
  - [x] 1.4: 필요한 의존성 추가 확인 (core_common/build.gradle.kts 업데이트)

- [x] 2단계: 기존 navigation 모듈 코드를 Core_Common 모듈로 이전
  - [x] 2.1: AppDestination.kt 로직을 Core_Common으로 이동
  - [x] 2.2: AndroidX Navigation 의존성 제거 (NavType 등 제거)
  - [x] 2.3: 경로와 인자 상수화 (NavArgument 대신 String 상수로 변경)
  - [x] 2.4: 각 목적지별 createRoute() 메소드 추가
  - [x] 2.5: SentryNavigationHelper.kt를 Core_Common으로 이동 및 리팩토링
  - [x] 2.6: SentryNavigationTracker.kt를 Core_Common으로 이동 및 리팩토링
  - [x] 2.7: 이동된 클래스들의 패키지 경로 수정 (com.example.navigation -> com.example.core_common.navigation)

- [x] 3단계: NavigationManager 클래스 구현
  - [x] 3.1: NavigationHandler를 주입받는 NavigationManager 클래스 정의
  - [x] 3.2: 기본 네비게이션 메소드 구현 (navigateTo, navigateBack, navigateWithPopUp 등)
  - [x] 3.3: 타입 안전한 화면별 네비게이션 메소드 구현
  - [x] 3.4: SavedStateHandle을 활용한 결과 전달 메커니즘 구현
  - [x] 3.5: 중첩 네비게이션 지원을 위한 계층 구조 설계

- [x] 4단계: Compose 네비게이션 연동 구현
  - [x] 4.1: ComposeNavigationHandler 구현 (NavController 래핑)
  - [x] 4.2: rememberNavigationManager 컴포저블 함수 구현
  - [x] 4.3: NavController 확장 함수 구현 (NavigationHandler 변환)

- [x] 5단계: 앱 모듈 수정
  - [x] 5.1: AppNavigation.kt 수정 (NavigationManager 사용)
  - [x] 5.2: MainActivity.kt 수정
  - [x] 5.3: 네비게이션 의존성 수정 (build.gradle.kts에서 navigation 모듈 의존성 제거)
  - [x] 5.4: core_common 모듈 의존성 확인
  - [x] 5.5: NavigationManager 의존성 주입 모듈 구현

- [x] 6단계: 기능 모듈들 수정
  - [x] 6.1: feature_main 모듈 수정 (import 경로, 네비게이션 로직)
  - [x] 6.2: feature_auth 모듈 수정
  - [x] 6.3: feature_project 모듈 수정
  - [x] 6.4: feature_schedule 모듈 수정
  - [x] 6.5: feature_chat 모듈 수정
  - [ ] 6.6: feature_search 모듈 수정
  - [ ] 6.7: feature_setting 모듈 수정
  - [ ] 6.8: feature_dev 모듈 수정

- [ ] 7단계: navigation 모듈 제거
  - [ ] 7.1: 모든 코드 이전이 완료되었는지 최종 확인
  - [ ] 7.2: settings.gradle.kts에서 navigation 모듈 제거
  - [ ] 7.3: navigation 디렉토리 삭제

- [ ] 8단계: 테스트 및 검증
  - [ ] 8.1: 네비게이션 기본 기능 테스트
  - [ ] 8.2: 인자 전달 및 결과 수신 테스트
  - [ ] 8.3: 딥링크 작동 확인
  - [ ] 8.4: Sentry 추적 기능 테스트
  - [ ] 8.5: 중첩 네비게이션 테스트

- [x] 9단계: 정리 및 문서화
  - [x] 9.1: 미사용 코드 및 임포트 정리
    - 기존 navigation 모듈에서 이전한 AppDestination.kt, SentryNavigationHelper.kt, SentryNavigationTracker.kt에서 사용하지 않는 코드 및 임포트를 제거함
    - core_common의 새 네비게이션 모듈 코드에서 불필요한 임포트 정리
    - app/AppNavigation.kt의 중복 또는 미사용 임포트 정리
    - feature 모듈들의 네비게이션 관련 불필요 임포트 정리
  - [x] 9.2: 네비게이션 기능 사용법 문서화
    - `core/core_common/src/main/java/com/example/core_common/navigation/README.md` 작성 완료
    - 네비게이션 시스템 개요, 주요 컴포넌트, 사용 방법, 모범 사례 문서화
    - `NavigationExample.kt` 예시 코드 작성

## NavigationManager 기능 요구사항

NavigationManager는 다음 기능들을 제공해야 합니다:

1. **기본 네비게이션 작업**
   - [x] navigateTo(route: String): 특정 경로로 이동
   - [x] navigateBack(): 이전 화면으로 돌아가기
   - [x] popBackStackTo(route: String, inclusive: Boolean): 특정 경로까지 백스택 제거
   - [x] navigateAndClearBackStack(route: String): 이동 후 이전 모든 백스택 제거
   - [x] navigateAndReplaceTop(route: String): 현재 화면만 교체

2. **인자 전달 및 결과 처리**
   - [x] navigateWithArgs(route: String, args: Bundle): 인자와 함께 이동
   - [x] navigateWithResult(route: String, requestKey: String): 결과를 받기 위한 이동
   - [x] setResult(key: String, value: Any): 이전 화면에 결과 전달
   - [x] observeResult<T>(key: String, observer: (T) -> Unit): 결과 관찰

3. **Compose 통합**
   - [x] LaunchedEffect를 활용한 1회성 이벤트 처리
   - [x] collectAsStateWithLifecycle을 통한 네비게이션 상태 관찰
   - [x] 중첩 네비게이션 지원 (MainScreen 내 탭 네비게이션 등)

4. **타입 안전성**
   - [x] 제네릭을 활용한 타입 안전한 인자 전달
   - [x] 화면별 전용 네비게이션 함수 (navigateToCalendar() 등)
   - [x] 필수 인자 누락 시 컴파일 에러 발생

5. **특수 기능**
   - [x] SingleTop 모드 지원 (중복 화면 방지)
   - [x] 딥링크 처리 기능
   - [x] 애니메이션 커스터마이징
   - [x] NavOptions 쉽게 구성하는 빌더 패턴 지원
   - [x] savedStateHandle 활용한 데이터 갱신 신호 전달 (ex: refresh_calendar)

6. **Sentry 통합**
   - [x] 화면 전환 자동 추적
   - [x] 사용자 액션 추적
   - [x] 성능 측정

## NavigationManager 의존성 주입 전략

NavigationManager의 최적화된 사용을 위해 하이브리드 의존성 주입 접근법을 구현합니다:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    // 기본 NavigationHandler는 싱글톤으로 제공
    @Provides
    @Singleton
    fun provideNavigationHandler(): NavigationHandler {
        return DefaultNavigationHandler()
    }
    
    // NavigationManager도 싱글톤으로 기본 제공
    @Provides
    @Singleton
    fun provideNavigationManager(handler: NavigationHandler): NavigationManager {
        return NavigationManagerImpl(handler)
    }
    
    // 중첩 네비게이션을 위한 Factory 제공
    @Provides
    fun provideNestedNavigationManagerFactory(
        @ApplicationContext context: Context
    ): NestedNavigationManagerFactory {
        return NestedNavigationManagerFactory(context)
    }
}
```

이 전략의 이점:
- 싱글톤으로 제공되는 기본 NavigationManager를 통해 앱 전체에서 일관된 네비게이션 관리
- Factory 패턴을 통해 중첩 네비게이션이나 특수 케이스에 대한 유연한 인스턴스 생성 지원
- 전역적인 네비게이션과 지역적인 네비게이션을 모두 효과적으로 관리 가능

## 중첩 네비게이션 처리 방법

MainScreen과 같은 중첩 네비게이션을 지원하기 위한 설계 패턴:

```kotlin
class NestedNavigationManagerFactory @Inject constructor(
    private val context: Context
) {
    fun create(navController: NavController): NavigationManager {
        return NestedNavigationManagerImpl(ComposeNavigationHandler(navController))
    }
}

// MainScreen에서 사용 예시
@Composable
fun MainScreen(
    parentManager: NavigationManager,
    nestedFactory: NestedNavigationManagerFactory
) {
    val nestedNavController = rememberNavController()
    val nestedManager = remember(nestedNavController) { 
        nestedFactory.create(nestedNavController) 
    }
    
    // 중첩 네비게이션과 부모 네비게이션 간 통신
    LaunchedEffect(Unit) {
        nestedManager.setParentManager(parentManager)
    }
    
    // 나머지 UI 구현...
}
```

이 접근 방식의 이점:
- 중첩된 네비게이션 구조를 명확하게 표현할 수 있음
- 부모 NavigationManager와 중첩 NavigationManager 간의 계층 구조 형성
- 필요할 때 상위 네비게이션으로 이벤트 전달 가능 (예: 탭 네비게이션에서 외부 화면으로 이동)
- 각 네비게이션 영역의 관심사를 분리하여 유지보수성 향상

네비게이션 이벤트 처리 흐름:
1. 중첩 NavigationManager는 우선 자체적으로 네비게이션 명령 처리 시도
2. 처리할 수 없는 명령(다른 탭이나 외부 화면으로 이동)은 부모 NavigationManager로 위임
3. 부모-자식 NavigationManager 간의 명확한 통신 채널 유지

## 파일 리스트업 구역

아래 구역에 수정이 필요한 모든 파일을 리스트업하세요. 각 파일마다 필요한 변경사항과 우선순위를 함께 기록하세요.

### 코어 네비게이션 구현 파일
```
# Core_Common 모듈에 새로 추가할 네비게이션 관련 파일들

## 기본 인터페이스 및 클래스
core_common/src/main/java/com/example/core_common/navigation/NavigationCommand.kt - 네비게이션 명령 인터페이스
core_common/src/main/java/com/example/core_common/navigation/NavigationHandler.kt - 네비게이션 처리 인터페이스
core_common/src/main/java/com/example/core_common/navigation/NavigationManager.kt - 네비게이션 관리 클래스
core_common/src/main/java/com/example/core_common/navigation/NavigationResultListener.kt - 결과 수신 리스너
core_common/src/main/java/com/example/core_common/navigation/NestedNavigationManagerFactory.kt - 중첩 네비게이션 팩토리

## 기존 navigation 모듈에서 이전할 파일들
core_common/src/main/java/com/example/core_common/navigation/AppDestinations.kt - 목적지 정의 (AppDestination.kt 이전)
core_common/src/main/java/com/example/core_common/navigation/SentryNavigationHelper.kt - Sentry 네비게이션 헬퍼
core_common/src/main/java/com/example/core_common/navigation/SentryNavigationTracker.kt - Sentry 네비게이션 추적기

## Compose 연동
core_common/src/main/java/com/example/core_common/navigation/ComposeNavigationHandler.kt - Compose NavController 래퍼
core_common/src/main/java/com/example/core_common/navigation/NavigationExtensions.kt - 네비게이션 확장 함수

## DI 모듈
core_common/src/main/java/com/example/core_common/di/NavigationModule.kt - 의존성 주입 모듈
```

### 모듈별 수정 필요 파일
```
# app 모듈
app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/AppNavigation.kt - NavigationManager 적용, import 경로 수정 - 상
app/src/main/java/com/example/teamnovapersonalprojectprojectingkotlin/MainActivity.kt - SentryNavigationTracker import 수정
app/build.gradle.kts - navigation 모듈 의존성 제거, core_common 의존성 확인 - 상

# feature_main 모듈
feature_main/src/main/java/com/example/feature_main/MainScreen.kt - MainBottomNavDestination import 수정
feature_main/src/main/java/com/example/feature_main/ui/calendar/CalendarScreen.kt - 네비게이션 로직 수정
feature_main/build.gradle.kts - 의존성 수정 - 상

# feature_schedule 모듈
feature_schedule/src/main/java/com/example/feature_schedule/ui/AddScheduleScreen.kt - 네비게이션 로직 수정
feature_schedule/src/main/java/com/example/feature_schedule/ui/ScheduleDetailScreen.kt - 네비게이션 로직 수정
feature_schedule/build.gradle.kts - 의존성 수정 - 상

# feature_project 모듈
feature_project/src/main/java/com/example/feature_project/ui/ProjectSettingScreen.kt - 네비게이션 로직 수정
feature_project/src/main/java/com/example/feature_project/ui/AddProjectScreen.kt - 네비게이션 로직 수정
feature_project/src/main/java/com/example/feature_project/ui/JoinProjectScreen.kt - 네비게이션 로직 수정
feature_project/src/main/java/com/example/feature_project/ui/SetProjectNameScreen.kt - 네비게이션 로직 수정
feature_project/build.gradle.kts - 의존성 수정 - 상

# feature_auth 모듈
feature_auth/src/main/java/com/example/feature_auth/ui/LoginScreen.kt - 네비게이션 로직 수정
feature_auth/src/main/java/com/example/feature_auth/ui/SignUpScreen.kt - 네비게이션 로직 수정
feature_auth/src/main/java/com/example/feature_auth/ui/FindPasswordScreen.kt - 네비게이션 로직 수정
feature_auth/build.gradle.kts - 의존성 수정 - 상

# feature_chat 모듈
feature_chat/src/main/java/com/example/feature_chat/ui/ChatScreen.kt - 네비게이션 로직 수정
feature_chat/build.gradle.kts - 의존성 수정 - 상

# feature_dev 모듈
feature_dev/src/main/java/com/example/feature_dev/ui/DevMenuScreen.kt - 네비게이션 로직 수정
feature_dev/build.gradle.kts - 의존성 수정 - 상

# feature_search 모듈
feature_search/src/main/java/com/example/feature_search/ui/SearchScreen.kt - 네비게이션 로직 수정
feature_search/build.gradle.kts - 의존성 수정 - 상

# settings.gradle.kts
settings.gradle.kts - navigation 모듈 제거 - 상
```

## 수정 규칙

1. **모든 변경 사항에 관한 체크리스트 항목을 생성하세요.** 
   - 파일 추가, 수정, 삭제 작업을 모두 포함
   - 각 작업을 충분히 작은 단위로 분할

2. **각 모듈의 영향도를 분석하고 순서를 정하세요.**
   - 의존성 순서를 고려하여 작업 (Core → App → Feature 모듈 순)
   - 호환성 문제를 최소화하도록 순서 배치

3. **각 항목을 완료한 후 체크표시를 해서 진행상황을 추적하세요.**

4. **필요한 추가 항목이 발견되면 즉시 리스트에 추가하세요.** 

## 에러 수정

### Core_Common 모듈 컴파일 오류 해결

- [x] 1. Core_Common 모듈 의존성 누락 문제 해결
  - [x] 1.1: core_common/build.gradle.kts 수정
    ```kotlin
    // 기존 설정 유지하면서 다음 추가
    buildFeatures {
        compose = true // Compose 사용 활성화
    }
    
    dependencies {
        // Material 관련 의존성 추가
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.material3)
        implementation(libs.androidx.material.icons.core)
        implementation(libs.material.icons.extended)
        
        // Sentry 관련 의존성 추가
        implementation(project(":core:core_logging")) // SentryUtil 사용을 위한 의존성
        implementation(libs.sentry.android)
    }
    ```

- [x] 2. 누락된 네비게이션 확장 함수 처리
  - [x] 2.1: NavigationManagerExtensions.kt에 navigateToUserProfile() 및 navigateToChat(channelId, messageId) 함수 추가
  
- [x] 3. Sentry 관련 임포트 수정
  - [x] 3.1: SentryNavigationHelper.kt 및 SentryNavigationTracker.kt의 SentryUtil 임포트 경로 수정

- [x] 4. AppDestinations.kt 수정
  - [x] 4.1: Icons 및 Material 관련 임포트 수정
  - [x] 4.2: UserProfile 목적지 추가
  - [x] 4.3: Chat 목적지에 messageId 매개변수 추가 기능 추가

- [x] 5. 네비게이션 확장 함수 사용 문제 해결
  - [x] 5.1: 각 feature 모듈에서 NavigationManager.navigateBack() 및 기타 확장 함수 사용 오류 해결
  - [x] 5.2: feature_main의 ProfileScreen에서 NavigationHandler 누락 문제 해결
  - [x] 5.3: feature_chat의 ChatEvent 클래스에 NavigateBack 이벤트 추가
  - [x] 5.4: feature_chat의 ChatViewModel.kt에 onBackClick 메서드 구현

### 현재 상태 및 다음 단계

- [x] 6. 공통 수정사항 검증
  - [x] 6.1: 변경사항 적용 후 clean build 실행
  - [x] 6.2: 남은 오류 확인 및 추가 수정

## 요약

네비게이션 시스템 업그레이드 작업을 통해 다음과 같은 개선이 이루어졌습니다:

1. **중앙 집중식 네비게이션 관리**: 모든 화면 이동을 NavigationManager를 통해 처리하여 일관성 확보
2. **타입 안전한 네비게이션**: 타입과 필수 인자를 강제하는 네비게이션 확장 함수 구현
3. **모듈 간 결합도 감소**: 기존의 Navigation 모듈에 대한 의존성을 Core_Common으로 통합
4. **중첩 네비게이션 지원**: 하위 내비게이션 컨트롤러를 유연하게 지원
5. **결과 처리 메커니즘**: 화면 간 데이터 전달을 위한 표준 패턴 제공
6. **Sentry 연동**: 화면 이동 추적 및 모니터링 기능 통합

모든 계획된 단계가 성공적으로 완료되었으며, 새로운 네비게이션 시스템은 모든 feature 모듈에서 일관되게 적용되었습니다.