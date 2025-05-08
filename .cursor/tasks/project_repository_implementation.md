# 태스크: 프로젝트 레포지토리 구현 및 샘플 데이터 제거

## 목표
현재 하드코딩된 샘플 데이터 대신 실제 Firebase Firestore에서 프로젝트 데이터를 가져오도록 구현합니다.

## 단계별 계획
- [x] 1. ProjectRepositoryImpl에 필요한 데이터 소스 주입 구현
  - ProjectRemoteDataSource 주입
  - 필요시 ProjectLocalDataSource 구현 및 주입
  - CurrentUserProvider 인터페이스 생성 및 주입
  
- [x] 2. ProjectDto와 Project 모델 간 매핑 로직 구현
  - DTO → Domain 모델 변환 함수 구현
  - Domain → DTO 모델 변환 함수 구현 (필요시)
  
- [x] 3. ProjectRepositoryImpl의 핵심 메소드 구현
  - getProjectListStream() 메소드 구현
  - fetchProjectList() 메소드 구현
  - 기타 필요한 메소드들 구현 확인
  
- [x] 4. HomeViewModel에서 실제 ProjectRepository 사용하도록 수정
  - 하드코딩된 프로젝트 목록 대신 리포지토리 사용
  - 에러 처리 로직 구현

- [x] 5. 전체 애플리케이션에서 샘플 데이터 사용 현황 조사
  - 다른 ViewModel에서 샘플 데이터 사용 여부 확인
  - UI 프리뷰에 사용된 샘플 데이터 식별 (유지 필요)
  
  ### 샘플 데이터 사용 현황
  다음 부분에서 아직 샘플 데이터가 사용되고 있습니다:
  
  1. `HomeViewModel`의 DM 목록 기능
  2. `RoleListViewModel`의 역할 목록 (프로젝트 역할)
  3. `ProjectDetailViewModel`의 프로젝트 상세 정보
  4. UI Preview 관련 코드 (HomeScreen, 등)
  
  이 중 UI Preview 관련 코드는 샘플 데이터를 유지해야 하며, 나머지는 향후 각 Repository 구현 작업에서 실제 데이터로 대체해야 합니다.
  
- [x] 6. 테스트 및 검증
  - 기존 테스트 코드 업데이트 (필요시)
  - 새 기능 수동 테스트 계획 작성
  
  ### 테스트 및 검증 결과
  1. 기존 테스트 코드 분석
     - FakeProjectRepository를 사용한 단위 테스트가 존재하며, 기본적인 테스트 케이스를 다루고 있습니다.
     - 실제 구현은 테스트 코드를 수정할 필요는 없습니다 (FakeProjectRepository는 테스트용이므로).
  
  2. 수동 테스트 계획
     - 애플리케이션 실행 후 홈 화면에서 프로젝트 목록이 Firebase에서 로드되는지 확인
     - 프로젝트가 없는 경우와 있는 경우의 UI 표시 확인
     - 네트워크 연결이 없는 경우 로컬 데이터 캐시에서 표시되는지 확인
     - 로그인 상태에 따른 동작 검증
     - 각 프로젝트 항목 클릭 시 상세 화면으로 이동하는지 확인

## 결론
프로젝트 목록 기능이 하드코딩된 샘플 데이터 대신 실제 Firebase Firestore에서 데이터를 가져오도록 성공적으로 구현되었습니다. 다른 프로젝트 관련 기능(멤버, 역할, 구조 등)도 향후 유사한 방식으로 구현할 수 있을 것입니다. 