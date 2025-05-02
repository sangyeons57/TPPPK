# 프로젝트 종합 테스트 계획

이 문서는 Projecting Kotlin 프로젝트의 모든 모듈에 대한 체계적인 테스트 계획을 정의합니다. 외부 의존성을 최소화하기 위해 Mockito를 사용하지 않고 순수 JUnit 기반의 테스트를 채택합니다.

## 1. 테스트 목표 및 원칙

- **목표**: 모든 모듈에 대한 80% 이상의 테스트 커버리지 달성
- **테스트 유형**: 단위 테스트(Unit), 통합 테스트(Integration), UI 테스트(UI)
- **접근 방식**: 
  - 외부 의존성을 최소화한 순수 JUnit 테스트 구현
  - **Mockito 사용 금지**: 모킹이 필요한 경우 테스트 전용 구현체(Fake) 직접 작성
  - 의존성 주입을 통한 테스트 격리
- **우선순위**: 1) 핵심 비즈니스 로직, 2) UI 상호작용, 3) 에러 처리

## 2. 테스트 계획 (모듈별)

### A. 도메인 계층 (`:domain`)

- [x] **Step 1**: 모델(Entities) 단위 테스트
  - 모든 도메인 모델 클래스의 생성자, getter/setter, 유틸리티 메서드 검증
  - ✅ `User` 모델 테스트 완료
  - ✅ `Schedule` 모델 테스트 완료
  - ✅ `Project` 모델 테스트 완료
  - ✅ `ChatMessage` 모델 테스트 완료

- [x] **Step 2**: Repository 인터페이스 명세 검증
  - 모든 Repository 인터페이스의 메서드 시그니처 문서화 및 검증
  - ✅ `UserRepository` 인터페이스 명세 검증 완료
  - ✅ `ScheduleRepository` 인터페이스 명세 검증 완료

### B. 데이터 계층 (`:data`)

- [x] **Step 3**: Repository 구현체 단위 테스트 (완료)
  - Firebase 의존성을 분리하여 각 Repository 구현체 테스트
  - ✅ `FakeUserRepository` 구현 및 단위 테스트 작성 완료
  - ✅ `FakeScheduleRepository` 구현 및 단위 테스트 작성 완료
  - ✅ `FakeProjectRepository` 구현 및 단위 테스트 작성 완료
  - ✅ `FakeChatRepository` 구현 및 단위 테스트 작성 완료
  - ✅ `FakeAuthRepository` 구현 및 단위 테스트 작성 완료
  - ✅ `FakeDmRepository` 구현 및 단위 테스트 작성 완료
  - ✅ `FakeFriendRepository` 구현 및 단위 테스트 작성 완료

- [x] **Step 4**: Firebase 유틸리티 테스트
  - ✅ `FirebaseUtil` 테스트 구현 완료
  - ✅ `FirestoreConstants` 테스트 구현 완료

### C. 프레젠테이션 계층 - 기능 모듈 (`:feature:*`)

#### C-1. 인증 기능 (`:feature_auth`)

- [x] **Step 5**: ViewModel 단위 테스트
  - ✅ `LoginViewModel` 테스트 완료
  - ✅ `SignUpViewModel` 테스트 완료
  - ✅ `FindPasswordViewModel` 테스트 완료
  - ✅ `SplashViewModel` 테스트 완료

- [x] **Step 6**: UI 컴포넌트 테스트
  - ✅ `LoginScreen` 테스트 완료 (렌더링, 이벤트 핸들링, 상태 표시)
  - ✅ `SignUpScreen` 테스트 완료 (렌더링, 입력 검증, 에러 메시지, 이벤트 핸들링)
  - ✅ `FindPasswordScreen` 테스트 완료 (단계별 흐름, 입력 검증, 에러 처리, 비밀번호 재설정)

#### C-2. 채팅 기능 (`:feature_chat`)

- [x] **Step 7**: ViewModel 단위 테스트
  - ✅ `ChatViewModel` 단위 테스트 구현 완료

- [x] **Step 8**: UI 상호작용 테스트
  - ✅ `ChatScreen` 메시지 전송, 수신, 첨부 등 상호작용 테스트 완료

#### C-3. 메인 기능 (`:feature_main`)

- [x] **Step 9**: ViewModel 단위 테스트
  - ✅ `HomeViewModel` 단위 테스트 구현 완료
  - ✅ `CalendarViewModel` 단위 테스트 구현 완료
  - ✅ `ProfileViewModel` 단위 테스트 구현 완료

- [x] **Step 10**: UI 컴포넌트 테스트
  - ✅ `HomeScreen` 테스트 완료 (프로젝트/DM 모드 전환, 리스트 렌더링, 상호작용) - Mockito에서 JUnit으로 리팩토링 완료
  - ✅ `CalendarScreen` 테스트 완료
  - ✅ `ProfileScreen` 테스트 완료 (프로필 정보 표시, 메뉴 상호작용, 버튼 이벤트 처리) - Mockito에서 JUnit으로 리팩토링 완료

#### C-4. 프로필 기능 (`:feature_profile`)

- [x] **Step 13**: ViewModel 및 UI 테스트
  - ✅ `ChangeStatusViewModel` 테스트 및 관련 UI 검증 완료

#### C-5. 프로젝트 기능 (`:feature_project`)

- [x] **Step 14**: 프로젝트 생성/가입 테스트
  - ✅ `AddProjectViewModel`, `JoinProjectViewModel` 테스트 완료

- [ ] **Step 15**: 프로젝트 구조 관리 테스트
  - 카테고리/채널 생성 및 관리 기능 테스트

- [ ] **Step 16**: 프로젝트 멤버/역할 관리 테스트
  - 멤버 관리, 역할 관리 기능 테스트

#### C-6. 일정 기능 (`:feature_schedule`)

- [x] **Step 17**: ViewModel 단위 테스트
  - ✅ `AddScheduleViewModel`, `Calendar24HourViewModel` 테스트 완료

- [x] **Step 18**: UI 상호작용 테스트
  - ✅ 일정 추가, 수정, 삭제 기능 테스트 완료

#### C-7. 검색 기능 (`:feature_search`)

- [x] **Step 19**: ViewModel 및 UI 테스트
  - ✅ `SearchViewModel` 테스트 구현 완료
  - ✅ 검색 UI 기능 테스트 완료

#### C-8. 설정 기능 (`:feature_settings`)

- [x] **Step 20**: ViewModel 단위 테스트
  - ✅ `ChangePasswordViewModel` 테스트 구현 완료 (입력 검증, 비밀번호 일치 검사, 현재 비밀번호 검증, 비밀번호 변경 성공/실패)
  - ✅ `EditProfileViewModel` 테스트 구현 완료 (프로필 로드, 이미지 선택/업로드/제거, 오류 처리, 이벤트 발생)

- [x] **Step 21**: UI 상호작용 테스트
  - ✅ `ChangePasswordScreen` 테스트 완료 (입력 처리, 에러 표시, 로딩 상태 표시)
  - ✅ `EditProfileScreen` 테스트 완료 (프로필 표시, 이미지 변경, 업로드 인디케이터) - Mockito에서 JUnit으로 리팩토링 완료

### D. 코어 모듈 (`:core:*`)

- [x] **Step 22**: Core Common 유틸리티 테스트
  - ✅ `:core:core_common` 모듈의 유틸리티 함수 테스트 완료

- [x] **Step 23**: Core UI 컴포넌트 테스트
  - ✅ `:core:core_ui` 테마, 공통 컴포넌트 테스트 완료 (Color, Theme, Dimens)

- [x] **Step 24**: 로깅 기능 테스트
  - ✅ `:core:core_logging` Sentry 통합 테스트 완료 (SentryUtil)

### E. 통합 테스트 (앱 전체)

- [ ] **Step 25**: 내비게이션 통합 테스트
  - `:navigation` 모듈과 앱 내비게이션 흐름 테스트

- [ ] **Step 26**: 엔드-투-엔드 테스트
  - 주요 사용자 시나리오에 대한 E2E 테스트

## 3. 테스트 구현 가이드라인

### 단위 테스트 스타일

```kotlin
/**
 * [클래스명] 단위 테스트
 */
class ClassNameTest {

    @Test
    fun `기능 설명 - 예상 결과`() {
        // Given: 테스트 조건 설정
        
        // When: 테스트할 기능 실행
        
        // Then: 예상 결과 검증
    }
}
```

### UI 테스트 스타일

```kotlin
/**
 * [화면명] UI 테스트
 */
class ScreenNameTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `사용자 액션 - 예상 UI 반응`() {
        // Given: UI 상태 및 콜백 설정
        
        // When: 컴포넌트 렌더링
        composeTestRule.setContent { /* ... */ }
        
        // Then: UI 요소 확인 및 인터랙션 테스트
    }
}
```

### Repository 테스트 스타일

```kotlin
/**
 * [Repository명] 테스트
 */
class RepositoryImplTest {
    
    // 가짜(Fake) Repository 구현
    private lateinit var fakeRepository: FakeRepository
    
    @Before
    fun setup() {
        // 테스트 데이터 및 환경 설정
        fakeRepository = FakeRepository()
    }
    
    @Test
    fun `기능 설명 - 예상 결과`() = runBlocking {
        // Given: 테스트 조건 설정
        
        // When: Repository 메소드 호출
        val result = fakeRepository.someMethod()
        
        // Then: 예상 결과 검증
        assertTrue(result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
    }
}
```

## 4. 테스트 실행 방법

각 모듈별 테스트 실행:
```bash
./gradlew :module_path:test
```

특정 테스트 클래스 실행:
```bash
./gradlew :module_path:test --tests "com.example.TestClassName"
```

## 5. 우선순위 및 진행 상황

- 현재까지 완료된 테스트:
  - UI 테스트 Mockito 의존성 제거 및 JUnit 기반으로 리팩토링 완료:
    - ✅ `HomeScreen` (feature_main)
    - ✅ `ProfileScreen` (feature_main)
    - ✅ `EditProfileScreen` (feature_settings)
    - ✅ `ChangePasswordScreen` (feature_settings) - 이미 JUnit 기반으로 구현됨
  - `CalendarViewModel` 테스트
  - `HomeViewModel` 테스트
  - `LoginViewModel` 테스트
  - `SignUpViewModel` 테스트
  - `FindPasswordViewModel` 테스트
  - `SplashViewModel` 테스트
  - `ChatViewModel` 테스트
  - `FriendViewModel` 테스트
  - `AddFriendViewModel` 테스트
  - `AcceptFriendsViewModel` 테스트  
  - `ProfileViewModel` 테스트
  - `ChangeStatusViewModel` 테스트
  - `AddProjectViewModel` 테스트
  - `JoinProjectViewModel` 테스트
  - `Calendar24HourViewModel` 테스트
  - `AddScheduleViewModel` 테스트
  - `SearchViewModel` 테스트
  - `ChangePasswordViewModel` 테스트
  - `EditProfileViewModel` 테스트
  - 캘린더 UI 상호작용 테스트
  - `User` 모델 테스트
  - `Schedule` 모델 테스트
  - `Project` 모델 테스트
  - `ChatMessage` 모델 테스트
  - `UserRepository` 인터페이스 명세 검증
  - `ScheduleRepository` 인터페이스 명세 검증
  - `FakeUserRepository` 구현 및 단위 테스트
  - `FakeScheduleRepository` 구현 및 단위 테스트
  - `FakeProjectRepository` 구현 및 단위 테스트
  - `FakeChatRepository` 구현 및 단위 테스트
  - `FakeAuthRepository` 구현 및 단위 테스트
  - `FakeDmRepository` 구현 및 단위 테스트
  - `FakeFriendRepository` 구현 및 단위 테스트
  - `FirebaseUtil` 테스트
  - `FirestoreConstants` 테스트
  - 공통 테스트 유틸리티 클래스 (TestUri, TestFirebaseUser) 구현
  - `SentryUtil` 테스트 완료 (core_logging)
  - `:core:core_ui` 테마 및 공통 컴포넌트 테스트 완료 (Color, Theme, Dimens)

- 다음 우선순위:
  1. 남은 UI 컴포넌트 테스트 리팩토링
  2. 더 많은 UI 컴포넌트 테스트 구현
  3. 통합 테스트 (Step 25, 26)

## 6. Fake Repository 패턴

Firebase와 같은 외부 의존성을 테스트하기 위해 "Fake" 구현체 패턴을 채택했습니다. Mockito를 사용하지 않고 직접 구현한 테스트용 클래스를 활용합니다. 이 패턴의 주요 특징:

1. **의존성 분리**: 외부 시스템에 의존하지 않고 테스트할 수 있음
2. **인메모리 데이터 관리**: 실제 데이터베이스 대신 인메모리 맵/리스트 사용
3. **에러 시뮬레이션**: 각종 에러 상황을 쉽게 시뮬레이션 가능
4. **테스트 격리**: 각 테스트가 서로 영향을 주지 않도록 격리
5. **외부 객체 대체**: 테스트하기 어려운 외부 객체(예: FirebaseUser, Uri)의 테스트 구현체 작성

### Fake Repository 구현 예시

```kotlin
class FakeUserRepository : UserRepository {
    // 인메모리 데이터 저장
    private val users = ConcurrentHashMap<String, User>()
    
    // 다양한 상황 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    
    // 테스트 지원 메소드
    fun addUser(user: User) { users[user.userId] = user }
    fun clearUsers() { users.clear() }
    fun setShouldSimulateError(value: Boolean) { shouldSimulateError = value }
    
    // UserRepository 인터페이스 구현
    override suspend fun getUser(): Result<User> {
        // 테스트 로직 구현
    }
    
    // 다른 메소드 구현...
}
```