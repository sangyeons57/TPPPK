# 프로젝트 종합 테스트 계획

이 문서는 Projecting Kotlin 프로젝트의 모든 모듈에 대한 체계적인 테스트 계획을 정의합니다. Mockito 대신 순수 JUnit 기반의 테스트를 채택하여 종속성을 최소화합니다.

## 1. 테스트 목표 및 원칙

- **목표**: 모든 모듈에 대한 80% 이상의 테스트 커버리지 달성
- **테스트 유형**: 단위 테스트(Unit), 통합 테스트(Integration), UI 테스트(UI)
- **접근 방식**: 외부 의존성을 최소화한 순수 JUnit 테스트 구현
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

- [ ] **Step 3**: Repository 구현체 단위 테스트
  - Firebase 의존성을 분리하여 각 Repository 구현체 테스트
  - 예: `UserRepositoryImpl`, `ProjectRepositoryImpl` 등

- [ ] **Step 4**: Firebase 유틸리티 테스트
  - `FirebaseUtil`, `FirestoreConstants` 등의 유틸리티 클래스 테스트

### C. 프레젠테이션 계층 - 기능 모듈 (`:feature:*`)

#### C-1. 인증 기능 (`:feature_auth`)

- [ ] **Step 5**: ViewModel 단위 테스트
  - `LoginViewModel`, `SignUpViewModel`, `FindPasswordViewModel`, `SplashViewModel` 테스트

- [ ] **Step 6**: UI 컴포넌트 테스트
  - `LoginScreen`, `SignUpScreen` 등의 UI 컴포넌트 렌더링 테스트

#### C-2. 채팅 기능 (`:feature_chat`)

- [ ] **Step 7**: ViewModel 단위 테스트
  - `ChatViewModel` 단위 테스트 구현

- [ ] **Step 8**: UI 상호작용 테스트
  - `ChatScreen` 메시지 전송, 수신 등 상호작용 테스트

#### C-3. 친구 기능 (`:feature_friends`)

- [ ] **Step 9**: ViewModel 단위 테스트
  - `FriendViewModel`, `AddFriendViewModel`, `AcceptFriendsViewModel` 테스트

- [ ] **Step 10**: UI 상호작용 테스트
  - 친구 추가, 수락 등의 사용자 상호작용 테스트

#### C-4. 메인 기능 (`:feature_main`)

- [ ] **Step 11**: ViewModel 단위 테스트 
  - `HomeViewModel`, `ProfileViewModel` 테스트
  - ✅ `CalendarViewModel` 테스트 (완료)

- [ ] **Step 12**: UI 상호작용 테스트
  - `HomeScreen`, `ProfileScreen` 상호작용 테스트
  - ✅ 캘린더 상호작용 테스트 (완료)

#### C-5. 프로필 기능 (`:feature_profile`)

- [ ] **Step 13**: ViewModel 및 UI 테스트
  - `ChangeStatusViewModel` 테스트 및 관련 UI 검증

#### C-6. 프로젝트 기능 (`:feature_project`)

- [ ] **Step 14**: 프로젝트 생성/가입 테스트
  - `AddProjectViewModel`, `JoinProjectViewModel` 등의 테스트

- [ ] **Step 15**: 프로젝트 구조 관리 테스트
  - 카테고리/채널 생성 및 관리 기능 테스트

- [ ] **Step 16**: 프로젝트 멤버/역할 관리 테스트
  - 멤버 관리, 역할 관리 기능 테스트

#### C-7. 일정 기능 (`:feature_schedule`)

- [ ] **Step 17**: ViewModel 단위 테스트
  - `AddScheduleViewModel`, `Calendar24HourViewModel` 등 테스트

- [ ] **Step 18**: UI 상호작용 테스트
  - 일정 추가, 수정, 삭제 기능 테스트

#### C-8. 검색 기능 (`:feature_search`)

- [ ] **Step 19**: ViewModel 및 UI 테스트
  - `SearchViewModel` 및 검색 UI 기능 테스트

#### C-9. 설정 기능 (`:feature_settings`)

- [ ] **Step 20**: ViewModel 단위 테스트
  - `ChangePasswordViewModel`, `EditProfileViewModel` 등 테스트

- [ ] **Step 21**: UI 상호작용 테스트
  - 비밀번호 변경, 프로필 편집 등 기능 테스트

### D. 코어 모듈 (`:core:*`)

- [ ] **Step 22**: Core Common 유틸리티 테스트
  - `:core:core_common` 모듈의 유틸리티 함수 테스트

- [ ] **Step 23**: Core UI 컴포넌트 테스트
  - `:core:core_ui` 테마, 공통 컴포넌트 테스트

- [ ] **Step 24**: 로깅 기능 테스트
  - `:core:core_logging` Sentry 통합 테스트

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
  - `CalendarViewModel` 테스트
  - 캘린더 UI 상호작용 테스트
  - `User` 모델 테스트
  - `Schedule` 모델 테스트
  - `Project` 모델 테스트
  - `ChatMessage` 모델 테스트
  - `UserRepository` 인터페이스 명세 검증
  - `ScheduleRepository` 인터페이스 명세 검증

- 다음 우선순위:
  1. Repository 구현체 테스트 (Step 3)
  2. Firebase 유틸리티 테스트 (Step 4)
  3. 나머지 ViewModel 테스트 (Step 5, 7, 9, 등)
  4. UI 컴포넌트 테스트 (Step 6, 8, 10, 등) 