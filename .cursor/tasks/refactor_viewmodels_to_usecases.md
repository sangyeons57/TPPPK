# Task: ViewModel에서 UseCase 사용하도록 리팩토링

- [x] Step 1: 기존 UseCase-ViewModel 아키텍처 분석 (예: `HomeViewModel` 또는 다른 리팩토링 완료된 ViewModel)
- [x] Step 2: 분석된 아키텍처 패턴 문서화 (이 파일에 기록)
  - **ViewModel:** UseCase 주입, UI 상태/이벤트 관리, 간단한 UI 로직 처리.
  - **UseCase:** 특정 비즈니스 로직 캡슐화, Repository 조합 가능.
  - **Repository:** 데이터 소스 추상화, CRUD, 데이터 모델 변환.
- [x] Step 3: [`ProfileViewModel.kt`](mdc:feature/feature_main/src/main/java/com/example/feature_main/viewmodel/ProfileViewModel.kt) 리팩토링 (UseCase 생성 및 적용, Repository 역할 재정의, Hilt 모듈 업데이트)
- [x] Step 4: [`EditMemberViewModel.kt`](mdc:feature/feature_project/src/main/java/com/example/feature_project/members/viewmodel/EditMemberViewModel.kt) 리팩토링
- [x] Step 5: [`EditRoleViewModel.kt`](mdc:feature/feature_project/src/main/java/com/example/feature_project/roles/viewmodel/EditRoleViewModel.kt) 리팩토링
- [x] Step 6: [`MemberListViewModel.kt`](mdc:feature/feature_project/src/main/java/com/example/feature_project/members/viewmodel/MemberListViewModel.kt) 리팩토링
- [x] Step 7: [`AddScheduleViewModel.kt`](mdc:feature/feature_schedule/src/main/java/com/example/feature_schedule/viewmodel/AddScheduleViewModel.kt) 리팩토링
- [x] Step 8: [`ProjectSettingViewModel.kt`](mdc:feature/feature_project/src/main/java/com/example/feature_project/setting/viewmodel/ProjectSettingViewModel.kt) 리팩토링
- [x] Step 9: [`Calendar24HourViewModel.kt`](mdc:feature/feature_schedule/src/main/java/com/example/feature_schedule/viewmodel/Calendar24HourViewModel.kt) 리팩토링
- [x] Step 10: [`ScheduleDetailViewModel.kt`](mdc:feature/feature_schedule/src/main/java/com/example/feature_schedule/viewmodel/ScheduleDetailViewModel.kt) 리팩토링
- [x] Step 11: [`SplashViewModel.kt`](mdc:feature/feature_auth/src/main/java/com/example/feature_auth/viewmodel/SplashViewModel.kt) 리팩토링
- [x] Step 12: 모든 의존성 및 연결 확인 및 수정 완료 (단, UseCase 내 TODO 및 관련 Repository 구현/검증 필요) 