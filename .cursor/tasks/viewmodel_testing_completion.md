# ViewModel 테스트 완료 현황

이 문서는 Projecting Kotlin 프로젝트의 ViewModel 테스트 구현 현황을 추적합니다.

## 메인 기능 (`:feature_main`)

- [x] `CalendarViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_main/src/test/java/com/example/feature_main/viewmodel/CalendarViewModelTest.kt`
  - 주요 기능 테스트: 월 이동, 날짜 선택, 일정 로드, 이벤트 발생, 오류 처리

- [x] `HomeViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_main/src/test/java/com/example/feature_main/viewmodel/HomeViewModelTest.kt`
  - 주요 기능 테스트: 탭 전환, 프로젝트/DM 로드, 버튼 클릭 이벤트, 오류 처리

- [x] `ProfileViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_main/src/test/java/com/example/feature_main/viewmodel/ProfileViewModelTest.kt`
  - 주요 기능 테스트: 프로필 로드, 상태 메시지 변경, 이미지 변경, 화면 이동 이벤트, 로그아웃, 오류 처리

## 인증 기능 (`:feature_auth`)

- [x] `LoginViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_auth/src/test/java/com/example/feature_auth/viewmodel/LoginViewModelTest.kt`
  - 주요 기능 테스트: 이메일/비밀번호 입력, 입력 유효성 검사, 로그인 성공/실패, 네비게이션 이벤트, 포커스 요청

- [x] `SignUpViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_auth/src/test/java/com/example/feature_auth/viewmodel/SignUpViewModelTest.kt`
  - 주요 기능 테스트: 이메일/비밀번호/이름 입력 및 유효성 검사, 회원가입 성공/실패, 폼 포커스 처리

- [x] `FindPasswordViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_auth/src/test/java/com/example/feature_auth/viewmodel/FindPasswordViewModelTest.kt`
  - 주요 기능 테스트: 이메일 인증, 인증코드 검증, 비밀번호 변경, 유효성 검사, 네비게이션 이벤트

- [x] `SplashViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_auth/src/test/java/com/example/feature_auth/viewmodel/SplashViewModelTest.kt`
  - 주요 기능 테스트: 로그인 상태 확인, 화면 전환 이벤트, 오류 처리, 스플래시 지연 시간

## 채팅 기능 (`:feature_chat`)

- [x] `ChatViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_chat/src/test/java/com/example/feature_chat/viewmodel/ChatViewModelTest.kt`
  - 주요 기능 테스트: 메시지 로드, 메시지 전송/수정/삭제, 과거 메시지 로드, 에러 처리

## 친구 기능 (`:feature_friends`)

- [x] `FriendViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_friends/src/test/java/com/example/feature_friends/viewmodel/FriendViewModelTest.kt`
  - 주요 기능 테스트: 친구 목록 로드, 친구 클릭 이벤트, 친구 요청 화면 이동, 오류 처리

- [x] `AddFriendViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_friends/src/test/java/com/example/feature_friends/viewmodel/AddFriendViewModelTest.kt`
  - 주요 기능 테스트: 사용자 이름 입력, 친구 요청 성공/실패, 다이얼로그 관련 이벤트, 오류 처리

- [x] `AcceptFriendsViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_friends/src/test/java/com/example/feature_friends/viewmodel/AcceptFriendsViewModelTest.kt`
  - 주요 기능 테스트: 친구 요청 목록 로드, 친구 요청 수락/거절, 낙관적 UI 업데이트, 오류 처리

## 프로필 기능 (`:feature_profile`)

- [x] `ChangeStatusViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_profile/src/test/java/com/example/feature_profile/viewmodel/ChangeStatusViewModelTest.kt`
  - 주요 기능 테스트: 현재 상태 로딩, 상태 선택, 상태 업데이트, 이벤트 발생, 오류 처리

## 프로젝트 기능 (`:feature_project`)

- [x] `AddProjectViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_project/src/test/java/com/example/feature_project/viewmodel/AddProjectViewModelTest.kt`
  - 주요 기능 테스트: 모드 변경, 입력 필드 업데이트, 프로젝트 참여, 프로젝트 생성, 오류 처리

- [x] `JoinProjectViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_project/src/test/java/com/example/feature_project/viewmodel/JoinProjectViewModelTest.kt`
  - 주요 기능 테스트: 코드 입력, 프로젝트 참여 성공/실패, 이벤트 발생, 오류 처리, 로딩 중 상태 관리

## 일정 기능 (`:feature_schedule`)

- [x] `AddScheduleViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_schedule/src/test/java/com/example/feature_schedule/viewmodel/AddScheduleViewModelTest.kt`
  - 주요 기능 테스트: 프로젝트 목록 로드, 프로젝트 선택, 제목/내용 입력, 시간 선택 및 유효성 검사, 일정 저장, 오류 처리

- [x] `Calendar24HourViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_schedule/src/test/java/com/example/feature_schedule/viewmodel/Calendar24HourViewModelTest.kt`
  - 주요 기능 테스트: 일정 로딩, 일정 삭제, 이벤트 발생(뒤로가기, 일정 추가, 일정 상세 보기 등), 오류 처리
  - 고급 기능 테스트: 지능형 일정 색상 시스템, 프로젝트별 색상 매핑, 고대비 모드 지원, 시간대 기반 그라데이션 효과

## 검색 기능 (`:feature_search`)

- [x] `SearchViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_search/src/test/java/com/example/feature_search/viewmodel/SearchViewModelTest.kt`
  - 주요 기능 테스트: 쿼리 처리, 검색 범위 필터링, 결과 처리, 네비게이션 이벤트, 오류 처리

## 설정 기능 (`:feature_settings`)

- [x] `ChangePasswordViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_settings/src/test/java/com/example/feature_settings/viewmodel/ChangePasswordViewModelTest.kt`
  - 주요 기능 테스트: 입력 검증, 비밀번호 일치 검사, 현재 비밀번호 검증, 비밀번호 변경 성공/실패
- [x] `EditProfileViewModel` 테스트 구현 완료
  - 테스트 파일: `feature/feature_settings/src/test/java/com/example/feature_settings/viewmodel/EditProfileViewModelTest.kt`
  - 주요 기능 테스트: 프로필 로드, 이미지 선택/업로드/제거, 오류 처리, 이벤트 발생

## 테스트 패턴 및 접근 방식

모든 ViewModel 테스트는 다음 패턴을 따릅니다:

1. **의존성 주입**: Fake 레포지토리 사용하여 외부 의존성 격리
2. **Coroutines 테스트**: `CoroutinesTestRule`을 사용한 코루틴 테스트 환경 설정
3. **Flow 테스트**: `FlowTestExtensions`를 사용한 StateFlow 및 SharedFlow 테스트
4. **이벤트 검증**: `EventCollector`를 사용한 이벤트 발생 검증
5. **오류 시나리오**: `setShouldSimulateError` 메서드를 통한 에러 상황 테스트

## 다음 우선순위

1. `ChangePasswordViewModel` 테스트 구현 