# 작업: MainScreen 탭별 독립적 상태 유지 구현

## 작업 개요
MainScreen에 할당된 3개의 화면(Home, Calendar, Profile)을 분리하면서도 상태를 유지하는 구조로 변경합니다. 바텀 네비게이션은 유지하면서 각 탭이 독립적인 상태를 가질 수 있도록 구현합니다.

## 작업 단계

- [x] 1. NavControllerState 및 상태 저장 클래스 구현
  - [x] 1.1. NavControllerState 데이터 클래스 구현
  - [x] 1.2. NavControllerSaver 구현

- [x] 2. MainContainerScreen 구현
  - [x] 2.1. 기존 MainScreen을 MainContainerScreen으로 리팩토링
  - [x] 2.2. 탭 전환 시 상태 저장/복원 로직 추가

- [x] 3. 각 탭 화면 수정
  - [x] 3.1. HomeScreen 수정 (상태 저장/복원 로직 추가)
  - [ ] 3.2. CalendarScreen 수정 (상태 저장/복원 로직 추가)
  - [ ] 3.3. ProfileScreen 수정 (상태 저장/복원 로직 추가)

- [x] 4. NavigationManager 수정
  - [x] 4.1. 화면 상태 저장/복원 메서드 추가
  - [x] 4.2. navigateToTab 메서드 수정

- [x] 5. AppNavigationGraph 수정
  - [x] 5.1. MainContainerScreen을 최상위 목적지로 등록

- [ ] 6. 테스트 및 디버깅
  - [ ] 6.1. 각 탭 간 전환 시 상태 유지 테스트
  - [ ] 6.2. 딥링크 및 외부 네비게이션 테스트 