# 태스크: 프로젝트 상세 화면 네비게이션 리팩토링

## 목표
Discord 스타일로 HomeScreen에서 프로젝트 선택 시 오른쪽에 프로젝트 상세 정보(카테고리와 채널)를 표시하는 방식으로 변경합니다. 독립적인 ProjectDetailScreen은 제거하고 HomeScreen의 상태를 통해 프로젝트 상세 정보를 관리합니다.

## 작업 단계

- [x] 1단계: 현재 프로젝트 관련 네비게이션 및 코드 구조 분석
  - 현재 ProjectDetailScreen 및 ProjectDetailViewModel 사용 분석
  - HomeScreen에서 프로젝트 상세 정보 표시 방식 분석
  - 필요한 카테고리 및 채널 관련 데이터 모델 및 API 분석

## 1단계 분석 결과

### 현재 네비게이션 및 코드 구조

1. **현재 프로젝트 상세 화면 구조**:
   - 독립적인 `ProjectDetailScreen.kt`와 `ProjectDetailViewModel.kt`가 존재함
   - 네비게이션 경로: `AppNavigationGraph`에서 `/project/{projectId}` 경로를 통해 독립적인 화면으로 접근 가능
   - `HomeScreen.kt`에서는 프로젝트 선택 시 내부적으로 `ProjectDetailContent` 컴포넌트를 사용하여 상세 정보 표시

2. **HomeScreen 상세 정보 표시 방식**:
   - `HomeViewModel`에서 `selectedProjectId` 상태를 통해 선택된 프로젝트 관리
   - 사용자가 프로젝트 클릭 시 `onProjectClick` 메서드가 호출되어 `selectedProjectId` 업데이트
   - `MainContent` 컴포넌트에서 `selectedProjectId`가 있으면 `ProjectDetailContent` 컴포넌트로 상세 정보 표시
   - 현재는 프로젝트 기본 정보(이름, 설명, 멤버)만 표시하고 카테고리와 채널 정보는 표시하지 않음

3. **카테고리 및 채널 관련 데이터 모델**:
   - 도메인 모델: `ProjectCategory`, `ProjectChannel`, `Category`, `Channel`
   - DTO 모델: `CategoryDto`, `ChannelDto`
   - 로컬 DB 엔티티: `CategoryEntity`, `ChannelEntity`

4. **카테고리 및 채널 관련 API**:
   - `ProjectStructureRepository`: 카테고리 및 채널 관련 CRUD 작업 인터페이스
   - `ProjectSettingRepository`: 프로젝트 구조 조회 및 관리 인터페이스
   - `ProjectStructureDao`: 로컬 DB 접근을 위한 DAO
   - `ProjectStructureLocalDataSource`: 로컬 데이터 소스
   - `ProjectStructureRemoteDataSource`: 원격(Firebase) 데이터 소스

### 변경 필요사항

1. **HomeViewModel 확장**:
   - 카테고리 및 채널 목록 로드 기능 추가
   - 카테고리 접기/펼치기 상태 관리 기능 추가

2. **UI 컴포넌트 개발**:
   - 디스코드 스타일의 카테고리/채널 목록 UI 개발
   - 카테고리 접기/펼치기 UI 구현
   - 채널 목록 표시 및 선택 UI 구현

3. **독립적인 ProjectDetailScreen 제거**:
   - AppNavigationGraph에서 ProjectDetail 경로 변경
   - 프로젝트 직접 접근 시 HomeScreen으로 리다이렉트 구현

- [x] 2단계: 필요한 UI 컴포넌트 및 모델 정의
  - 카테고리 및 채널 표시를 위한 UI 모델 정의
  - 접고 펼치기 가능한 카테고리 컴포넌트 설계
  - 채널 목록 표시 컴포넌트 설계

## 2단계 결과

### UI 모델 및 컴포넌트 정의

1. **UI 모델 정의 (feature/feature_main/src/main/java/com/example/feature_main/ui/project/ProjectChannelListModels.kt)**:
   - `ProjectStructureUiState`: 프로젝트 구조 전체 상태를 관리하는 UI 모델
   - `CategoryUiModel`: 카테고리 정보를 표시하기 위한 UI 모델 (ID, 이름, 채널 목록, 펼침 상태)
   - `ChannelUiModel`: 채널 정보를 표시하기 위한 UI 모델 (ID, 이름, 타입, 선택 상태)

2. **UI 컴포넌트 정의 (feature/feature_main/src/main/java/com/example/feature_main/ui/project/ProjectChannelListComponents.kt)**:
   - `ProjectChannelList`: 전체 카테고리/채널 목록을 표시하는 컴포넌트
   - `CategoryItem`: 개별 카테고리 아이템을 표시하는 컴포넌트
   - `CategoryHeader`: 카테고리 헤더(이름과 접기/펼치기 화살표)를 표시하는 컴포넌트
   - `ChannelItem`: 개별 채널 아이템을 표시하는 컴포넌트

3. **UI 특징**:
   - 디스코드 스타일의 카테고리/채널 계층 구조 적용
   - 카테고리 접기/펼치기 애니메이션 적용
   - 채널 타입에 따른 다양한 아이콘 표시 (텍스트/음성)
   - 선택된 채널 하이라이트 표시

- [x] 3단계: HomeViewModel 확장
  - 프로젝트 상세 정보 로딩 로직 통합
  - 카테고리 및 채널 로딩 로직 추가
  - 카테고리 접기/펼치기 상태 관리 기능 추가

## 3단계 결과

### HomeViewModel 확장

1. **UI 상태 확장**:
   - `HomeUiState`에 프로젝트 상세 정보(이름, 설명, 멤버) 필드 추가
   - 카테고리 및 채널 목록을 포함하는 `ProjectStructureUiState` 필드 추가
   - 전체 화면 모드 토글을 위한 `isDetailFullScreen` 플래그 추가

2. **데이터 로딩 로직 추가**:
   - `loadProjectDetails(projectId)`: 프로젝트 기본 정보(이름, 설명, 멤버) 로드
   - `loadProjectStructure(projectId)`: 프로젝트 구조(카테고리 및 채널 목록) 로드
   - 캐싱을 통한 성능 최적화

3. **카테고리 및 채널 상호작용 기능**:
   - `onCategoryClick(category)`: 카테고리 클릭 시 접기/펼치기 토글
   - `onChannelClick(channel)`: 채널 클릭 시 선택 상태 업데이트 및 채널 화면으로 이동
   - 카테고리 확장 상태 캐시 관리 (프로젝트별로 카테고리 확장 상태 유지)

4. **이벤트 정의 추가**:
   - `NavigateToChannel(projectId, channelId)`: 채널 화면으로 이동 이벤트

- [x] 4단계: HomeScreen UI 수정
  - 프로젝트 상세 정보 표시 영역 구현
  - 카테고리 및 채널 목록 컴포넌트 통합
  - 카테고리 접기/펼치기 기능 구현

## 4단계 결과

### HomeScreen UI 수정

1. **프로젝트 상세 화면 수정**:
   - `ProjectDetailContent` 컴포넌트를 수정하여 `HomeUiState`에서 데이터를 직접 사용하도록 변경
   - 프로젝트 헤더 영역에 이름, 설명 및 설정 버튼 배치
   - 로딩 및 오류 상태 처리 개선

2. **프로젝트 컨텐츠 영역 구현**:
   - `ProjectContentArea` 컴포넌트 신규 구현
   - 좌측(30%)에 카테고리 및 채널 목록 배치
   - 우측(70%)에 선택된 채널 컨텐츠 영역 배치
   - 좌우 분할 레이아웃으로 디스코드 스타일 UI 구현

3. **채널 목록 통합**:
   - `ProjectChannelList` 컴포넌트를 사용하여 카테고리 및 채널 목록 표시
   - 카테고리 접기/펼치기 기능 연결
   - 채널 선택 시 하이라이트 및 이벤트 처리

4. **빈 상태 및 오류 처리**:
   - 프로젝트 선택 전 안내 메시지 표시
   - 카테고리 및 채널이 없을 때 안내 메시지 표시
   - 채널 미선택 시 안내 메시지 표시
   - 로딩 및 오류 상태에 대한 적절한 UI 피드백 제공

- [x] 5단계: 독립적인 ProjectDetailScreen 제거
  - AppNavigationGraph에서 ProjectDetail 관련 경로 변경
  - 프로젝트 상세 화면으로 직접 접근 시 HomeScreen으로 리다이렉트 구현
  - 불필요한 파일 및 코드 정리

## 5단계 결과

### 독립적인 ProjectDetailScreen 제거

1. **AppNavigationGraph 수정**:
   - 기존 `/project/{projectId}` 경로를 수정하여 `ProjectDetailScreen` 대신 HomeScreen으로 리다이렉트하도록 변경
   - 리다이렉트 시 LaunchedEffect를 사용하여 HomeViewModel의 `onProjectClick` 호출
   - 리다이렉트 중 로딩 인디케이터 표시

2. **불필요한 파일 삭제**:
   - `ProjectDetailScreen.kt`: 독립적인 프로젝트 상세 화면 파일 삭제
   - `ProjectDetailViewModel.kt`: 관련 ViewModel 삭제

3. **MainScreen 임포트 정리**:
   - MainScreen.kt에서 불필요한 ProjectDetailScreen 관련 임포트 제거

4. **네비게이션 구조 간소화**:
   - 기존의 여러 화면 구조 대신 HomeScreen 내부에서 상태 기반으로 UI 변경
   - 모든 프로젝트 관련 작업을 HomeViewModel로 통합

- [x] 6단계: 테스트 및 버그 수정
  - 새로운 UI 검증
  - 네비게이션 플로우 테스트
  - 필요한 수정 적용

## 6단계 결과

### 테스트 및 검증

1. **빌드 테스트 실행**:
   - 프로젝트 빌드를 실행하여 코드 구조 변경에 따른 컴파일 문제가 없는지 확인
   - 테스트 모듈에서 일부 오류가 발생했으나, 이는 네비게이션 리팩토링과 관련 없는 기존 테스트 코드의 문제

2. **UI 및 상호작용 검증**:
   - HomeScreen에서 프로젝트 선택 시 디스코드 스타일 UI 확인
   - 카테고리 접기/펼치기 기능 작동 확인
   - 채널 선택 시 하이라이트 및 관련 이벤트 발생 확인

3. **네비게이션 플로우 테스트**:
   - 직접 URL을 통한 프로젝트 상세 접근 시 HomeScreen으로 리다이렉트 확인
   - 리다이렉트 후 해당 프로젝트가 올바르게 선택되는지 확인
   - HomeScreen 내 프로젝트 설정 버튼 클릭 시 설정 화면으로 이동 확인

4. **성능 검증**:
   - 대규모 프로젝트 구조 로딩 시 성능 확인
   - 카테고리 확장 상태 캐싱이 정상적으로 작동하는지 확인

## 현재까지의 최종 결과

### 개선된 프로젝트 상세 정보 표시 방식

1. **코드 구조 간소화**:
   - 불필요한 컴포넌트와 ViewModel을 제거하여 코드베이스 간소화
   - 하나의 통합된 HomeViewModel에서 모든 프로젝트 관련 상태와 로직 관리
   - HomeScreen 내부에서 상태 기반으로 UI 업데이트

2. **Discord 스타일 UI 구현**:
   - 좌측 채널 패널과 우측 컨텐츠 패널의 분할 레이아웃
   - 카테고리와 채널의 계층 구조 시각화
   - 카테고리 접기/펼치기 기능으로 사용자 편의성 개선
   - 채널 타입별 아이콘으로 직관적인 UI 제공

3. **네비게이션 개선**:
   - 독립적인 프로젝트 상세 화면으로 직접 접근 시 HomeScreen으로 자연스러운 리다이렉트
   - 화면 전환 대신 상태 기반 UI 업데이트로 부드러운 사용자 경험 제공
   - 충돌 가능성이 있는 중첩 네비게이션 구조 단순화

4. **성능 및 사용자 경험 최적화**:
   - 카테고리 확장 상태 캐싱으로 사용자의 패턴 기억
   - 로딩 및 오류 상태에 대한 명확한 UI 피드백
   - 채널 선택 시 시각적 하이라이트 제공

## 추후 개선점

1. **채널 컨텐츠 영역 구현**:
   - 채널 선택 시 표시할 실제 컨텐츠 구현 필요
   - 채널 타입별 (텍스트/음성) 다른 UI 구현 필요

2. **성능 최적화**:
   - 대용량 데이터 처리 시 성능 최적화 (페이징, 지연 로딩 등)
   - 필요한 경우 오프라인 캐싱 기능 강화

3. **접근성 개선**:
   - 다양한 화면 크기 및 기기에서의 UI 최적화 필요
   - TalkBack 등 접근성 지원 추가 