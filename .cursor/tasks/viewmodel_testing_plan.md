# ViewModel 테스트 계획

이 문서는 Projecting Kotlin 프로젝트의 ViewModel 테스트 계획을 정의합니다. 기존 Repository 테스트와 마찬가지로 외부 의존성을 최소화하고 순수 JUnit 기반의 테스트를 구현합니다.

## 1. ViewModel 테스트 접근 방식

- **의존성 주입**: 각 ViewModel에서 사용하는 Repository 의존성을 FakeRepository로 대체
- **Coroutines 테스트**: `TestCoroutineScope` 또는 `TestCoroutineDispatcher` 사용
- **Flow 테스트**: StateFlow, SharedFlow 등의 테스트를 위한 유틸리티 함수 구현
- **상태와 이벤트 검증**: 모든 UI 상태 변화와 이벤트 방출 확인
- **에러 처리 검증**: 오류 상황에서의 동작 및 에러 메시지 검증

## 2. ViewModel 테스트 우선순위

1. 핵심 기능 ViewModel:
   - [ ] `CalendarViewModel`
   - [ ] `HomeViewModel`
   - [ ] `ChatViewModel`
   - [ ] `LoginViewModel` 및 인증 관련 ViewModel

2. 사용자 상호작용이 많은 ViewModel:
   - [ ] `AddScheduleViewModel`
   - [ ] `AddProjectViewModel`
   - [ ] `FriendViewModel`

3. 기타 ViewModel:
   - [ ] 각 기능별 보조 ViewModel

## 3. 테스트 환경 준비

- [ ] **Step 1**: Coroutines 테스트 환경 구성
  - `TestCoroutineDispatcher` 설정
  - JUnit Rules 정의

- [ ] **Step 2**: ViewModel 테스트 유틸리티 구현
  - StateFlow, SharedFlow 수집 및 검증 헬퍼 함수
  - UiState, UiEvent 비교 유틸리티

## 4. ViewModel 테스트 템플릿

```kotlin
class ViewModelNameTest {

    // Coroutines 테스트 환경
    private val testDispatcher = TestCoroutineDispatcher()
    
    // Fake Repository 구현체
    private lateinit var fakeRepository1: FakeRepository1
    private lateinit var fakeRepository2: FakeRepository2
    
    // 테스트 대상 ViewModel
    private lateinit var viewModel: ViewModelName
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Fake Repository 초기화
        fakeRepository1 = FakeRepository1()
        fakeRepository2 = FakeRepository2()
        
        // 테스트 데이터 설정
        // ...
        
        // ViewModel 초기화 (의존성 주입)
        viewModel = ViewModelName(
            repository1 = fakeRepository1,
            repository2 = fakeRepository2,
            dispatcher = testDispatcher
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `초기 상태 확인`() = runBlockingTest {
        // 초기 상태 검증
        assertEquals(ViewModelName.UiState(), viewModel.uiState.value)
    }
    
    @Test
    fun `액션 실행 시 상태 변경 확인`() = runBlockingTest {
        // Given
        val expectedState = ViewModelName.UiState(isLoading = true)
        
        // When
        viewModel.handleAction(ViewModelName.UiAction.SomeAction)
        
        // Then
        assertEquals(expectedState, viewModel.uiState.value)
    }
    
    @Test
    fun `에러 발생 시 처리 확인`() = runBlockingTest {
        // Given
        fakeRepository1.setShouldSimulateError(true)
        
        // When
        viewModel.handleAction(ViewModelName.UiAction.SomeAction)
        
        // Then
        assertTrue(viewModel.uiState.value.error != null)
        // 이벤트 검증 등...
    }
}
```

## 5. 공통 테스트 시나리오

각 ViewModel 테스트에서 검증해야 할 공통 시나리오:

1. **초기 상태**: ViewModel 생성 시 초기 상태 확인
2. **데이터 로딩**: 데이터 로드 시작/완료/오류 시 상태 변화
3. **사용자 액션**: 각 UI 액션 처리 시 상태 및 이벤트 변화
4. **에러 처리**: 다양한 오류 상황에서의 동작 검증
5. **네비게이션**: 화면 이동 이벤트 발생 확인

## 6. 시작할 첫 번째 ViewModel 테스트

`CalendarViewModel` 테스트를 첫 번째로 구현하여 테스트 패턴을 확립하겠습니다. 이 테스트는 다음을 포함합니다:

- [ ] 초기 상태 및 데이터 로딩 검증
- [ ] 캘린더 날짜 변경 시 일정 로딩 검증
- [ ] 날짜 선택 동작 검증
- [ ] 월 변경 시 일정 요약 정보 갱신 검증
- [ ] 오류 상황 처리 및 재시도 검증 