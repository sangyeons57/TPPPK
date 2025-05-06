# 아키텍처 검증 계획서

## 개요

이 문서는 클린 아키텍처 패턴에 따라 각 계층이 명확히 역할을 수행하고 있는지 검증하기 위한 계획을 설명합니다. 특히 기존 ViewModel에서 Repository 직접 참조를 UseCase로 리팩토링한 후 각 계층의 책임이 적절히 분리되었는지 검증합니다.

## 검증 가이드라인

### 1. DataSource 계층 검증

**역할**: 외부 데이터 소스(네트워크, 로컬 DB 등)에서 데이터를 획득하고 기본적인 가공

- **검증 항목**:
  - 외부 데이터 소스 접근 관련 코드에 집중되어 있는가?
  - 비즈니스 로직이 포함되어 있지 않은가?
  - 원시 데이터 형태나 DTO로 반환하고 있는가?
  - 에러 처리는 적절히 수행하되 세부 에러 메시지 처리는 하지 않는가?
  - 내부 구현 세부사항이 외부로 노출되지 않는가?

### 2. Repository 계층 검증

**역할**: DataSource에서 데이터를 가져와 Domain 모델로 변환하고 추상화된 인터페이스 제공

- **검증 항목**:
  - Domain 계층에 정의된 인터페이스를 구현하고 있는가?
  - 적절한 DataSource를 선택해 데이터를 획득하는가?
  - DTO를 Domain 모델로 변환하는 로직이 포함되어 있는가?
  - 캐싱 전략이 구현되어 있는가? (필요한 경우)
  - Result 타입을 반환하여 에러를 적절히 추상화하는가?
  - 비즈니스 로직이 포함되어 있지 않은가?
  - Repository 구현체를 사용하는 테스트가 작성되어 있는가?

### 3. UseCase 계층 검증

**역할**: 비즈니스 로직을 캡슐화하고 Repository를 통해 데이터 조작

- **검증 항목**:
  - 단일 책임 원칙에 부합하게 구현되어 있는가?
  - 특정 비즈니스 기능을 명확히 표현하는가?
  - 복잡한 비즈니스 로직이 포함되어 있는가? (필요한 경우)
  - Repository를 통해서만 데이터에 접근하는가?
  - operator fun invoke() 패턴을 사용하는가?
  - 적절한 입력 파라미터 검증을 수행하는가?
  - 반환 값은 Domain 모델 또는 Result<T> 타입인가?
  - 독립적으로 테스트 가능한가?

### 4. ViewModel 계층 검증

**역할**: UI 관련 로직 및 상태 관리, UseCase와 UI 연결

- **검증 항목**:
  - Repository를 직접 참조하지 않고 UseCase만 사용하는가?
  - UI 상태(UiState)를 적절히 관리하는가?
  - UI 이벤트를 적절히 처리하는가?
  - 기본적인 입력 검증만 수행하는가? (복잡한 검증은 UseCase로)
  - Domain 모델 → UI 모델 변환 로직이 포함되어 있는가?
  - 비즈니스 로직은 UseCase에 위임하는가?
  - 단위 테스트가 작성되어 있는가?

## 검증 방법

1. **코드 검토**:
   - 각 계층별 파일을 샘플링하여 위 검증 항목에 따라 검토
   - 특히 최근 리팩토링된 UseCase 및 관련 ViewModel 중점 검토

2. **정적 분석**:
   - 의존성 방향 역전 원칙 준수 여부 확인
   - 각 모듈 간 의존성 그래프 분석
   - 순환 의존성 검출

3. **단위 테스트**:
   - 각 계층이 올바르게 역할을 수행하는지 테스트 케이스 작성
   - Mock 객체를 활용한 계층 간 경계 테스트

## 주요 검증 대상 (신규 작성 기준)

### User 관련 검증

- DataSource: UserRemoteDataSource, UserLocalDataSource
- Repository: UserRepositoryImpl
- UseCase: UpdateNicknameUseCase, UpdateProfileImageUseCase, RemoveProfileImageUseCase
- ViewModel: ChangeNameViewModel, EditProfileViewModel

### Auth 관련 검증

- DataSource: AuthRemoteDataSource
- Repository: AuthRepositoryImpl
- UseCase: ChangePasswordUseCase, LoginUseCase, LogoutUseCase
- ViewModel: ChangePasswordViewModel, LoginViewModel

### Project 관련 검증

- DataSource: ProjectRemoteDataSource, ProjectLocalDataSource
- Repository: ProjectRepositoryImpl
- UseCase: CreateProjectUseCase, JoinProjectWithCodeUseCase
- ViewModel: AddProjectViewModel, JoinProjectViewModel

### Chat 관련 검증

- DataSource: ChatRemoteDataSource, ChatLocalDataSource
- Repository: ChatRepositoryImpl
- UseCase: GetMessagesStreamUseCase, SendMessageUseCase
- ViewModel: ChatViewModel

## 검증 결과 문서화

- 각 계층별 검증 결과 및 개선 필요 사항 기록
- 아키텍처 원칙 준수 여부 평가
- 발견된 문제점 및 개선 방안 제시
- 우수 사례 식별 및 공유 