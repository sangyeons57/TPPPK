# Task: ViewModel에서 Repository 직접 참조 제거 및 UseCase 전환 리팩토링

## 개요

이 태스크는 Clean Architecture 원칙에 따라 모든 ViewModel이 Repository를 직접 참조하지 않고 UseCase만 주입받아 사용하도록 리팩토링하는 것을 목표로 합니다. 특히 에러 메시지 처리와 같은 로직도 UseCase 내부로 이동시켜 관심사를 명확히 분리합니다.

## 단계별 계획

### 1단계: 현황 분석

- [x] 1.1 모든 ViewModel 클래스에서 Repository 직접 참조 현황 조사
- [x] 1.2 Repository 직접 참조의 주요 사용 사례 식별 (에러 메시지 처리 등)
- [x] 1.3 모든 Feature 모듈의 의존성 구조 확인
- [x] 1.4 기존에 잘 구현된 UseCase와 그렇지 않은 UseCase 식별

### 2단계: 에러 처리를 위한 UseCase 구현

- [x] 2.1 `GetAuthErrorMessageUseCase` 설계 및 구현
- [x] 2.2 필요한 경우 다른 도메인별 에러 처리 UseCase 구현
- [x] 2.3 에러 처리 UseCase 단위 테스트 작성
- [x] 2.4 Hilt에 새로운 UseCase 등록

### 3단계: feature_auth 모듈 리팩토링

- [x] 3.1 LoginViewModel에서 Repository 직접 참조 제거
- [x] 3.2 SignUpViewModel에서 Repository 직접 참조 제거
- [x] 3.3 FindPasswordViewModel에서 Repository 직접 참조 제거
- [x] 3.4 필요한 경우 추가 UseCase 구현
- [x] 3.5 모듈 단위 테스트 수정 및 실행

### 4단계: feature_profile 모듈 리팩토링

- [x] 4.1 ProfileViewModel에서 Repository 직접 참조 제거
- [x] 4.2 필요한 경우 추가 UseCase 구현
- [x] 4.3 모듈 단위 테스트 수정 및 실행

### 5단계: feature_friends 모듈 리팩토링

- [x] 5.1 FriendsViewModel에서 Repository 직접 참조 제거
- [x] 5.2 필요한 경우 추가 UseCase 구현
- [x] 5.3 모듈 단위 테스트 수정 및 실행

### 6단계: feature_project 모듈 리팩토링

- [x] 6.1 모든 Project 관련 ViewModel에서 Repository 직접 참조 제거
- [x] 6.2 필요한 경우 추가 UseCase 구현
- [x] 6.3 모듈 단위 테스트 수정 및 실행

### 7단계: feature_chat 모듈 리팩토링

- [x] 7.1 ChatViewModel에서 Repository 직접 참조 제거
- [x] 7.2 필요한 경우 추가 UseCase 구현
- [x] 7.3 모듈 단위 테스트 수정 및 실행

### 8단계: feature_search 모듈 리팩토링

- [x] 8.1 SearchViewModel에서 Repository 직접 참조 제거
- [x] 8.2 필요한 경우 추가 UseCase 구현
- [x] 8.3 모듈 단위 테스트 수정 및 실행

### 9단계: feature_settings 모듈 리팩토링

- [x] 9.1 SettingsViewModel에서 Repository 직접 참조 제거
- [x] 9.2 필요한 경우 추가 UseCase 구현
- [x] 9.3 모듈 단위 테스트 수정 및 실행

### 10단계: 통합 테스트 및 검증

- [x] 10.1 전체 앱 통합 테스트 수행
- [x] 10.2 성능 영향 확인 (있다면)
- [x] 10.3 코드 품질 지표 검토 (의존성 방향, 결합도 등)

### 11단계: 문서화 및 마무리

- [x] 11.1 아키텍처 문서 업데이트
- [x] 11.2 변경사항 PR 검토 및 최종 승인
- [x] 11.3 향후 개발 가이드라인 업데이트 

### 12단계: 아키텍처 검증 및 모듈 역할 확인

- [x] 12.1 각 계층별 역할 검증
  - [x] 12.1.1 DataSource: 데이터 소스에서 데이터 획득 및 1차 정제
  - [x] 12.1.2 Repository: DTO → Domain Model 변환 및 적절한 추상화
  - [x] 12.1.3 UseCase: 주요 비즈니스 로직 구현 확인
  - [x] 12.1.4 ViewModel: UI 관련 로직만 처리하는지 확인

- [x] 12.2 의존성 방향 및 모듈화 검증
  - [x] 12.2.1 UI → ViewModel → UseCase → Repository → DataSource 방향 확인
  - [x] 12.2.2 각 모듈이 적절한 의존성만 가지고 있는지 확인
  - [x] 12.2.3 순환 의존성 검증

- [x] 12.3 UseCase별 Repository 확인 (신규 작성 기준)
  - [x] 12.3.1 User 관련 UseCase 및 Repository 확인
    - [x] UpdateNicknameUseCase
    - [x] UpdateProfileImageUseCase
    - [x] RemoveProfileImageUseCase
    - [x] GetUserProfileUseCase
  - [x] 12.3.2 Auth 관련 UseCase 및 Repository 확인  
    - [x] ChangePasswordUseCase
    - [x] LoginUseCase
    - [x] LogoutUseCase
    - [x] SignUpUseCase
  - [x] 12.3.3 Project 관련 UseCase 및 Repository 확인
    - [x] CreateProjectUseCase
    - [x] JoinProjectWithCodeUseCase
    - [x] JoinProjectWithTokenUseCase
  - [x] 12.3.4 Chat 관련 UseCase 및 Repository 확인
    - [x] GetMessagesStreamUseCase
    - [x] SendMessageUseCase
    - [x] FetchPastMessagesUseCase
    - [x] EditMessageUseCase
    - [x] DeleteMessageUseCase

### 13단계: Repository 구현체 책임 검증 및 에러 처리 표준화

- [x] 13.1 Repository 책임 검증
  - [x] 13.1.1 모델 매핑 책임: Repository가 DTO/Entity와 Domain Model 간 변환만 담당하는지 확인
  - [x] 13.1.2 데이터 결정 로직: 데이터 소스 선택 및 조합 로직만 포함하는지 확인
  - [x] 13.1.3 단일 진실 공급원 역할: 상위 계층에 일관된 데이터 접근 방식 제공하는지 확인
  - [x] 13.1.4 비즈니스 로직 포함 여부: UseCase가 담당해야 할 비즈니스 로직 존재 여부 확인

- [x] 13.2 Common Result Wrapping 구현
  - [x] 13.2.1 Result<T> 표준 확장 함수 구현 (mapSuccess, mapError 등)
  - [x] 13.2.2 Repository 메서드의 일관된 Result 반환 패턴 적용
  - [x] 13.2.3 에러 타입 표준화 및 도메인 예외 정의

- [x] 13.3 Repository 구현체 개선
  - [x] 13.3.1 ChatRepositoryImpl 구현 완료 및 책임 검증
  - [x] 13.3.2 UserRepositoryImpl 책임 검증 및 필요시 개선
  - [x] 13.3.3 AuthRepositoryImpl 책임 검증 및 필요시 개선
  - [x] 13.3.4 ProjectRepositoryImpl 책임 검증 및 필요시 개선 