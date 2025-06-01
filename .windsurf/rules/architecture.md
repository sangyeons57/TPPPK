---
trigger: always_on
---

-
# Projecting Kotlin: Architecture & Design Principles

## 핵심 원칙

- **Clean Architecture**: 계층 간 명확한 책임 분리와 단방향 의존성
- **모듈화**: 세분화된 모듈로 빌드 성능 최적화 및 코드 관리 용이성 확보
- **테스트 용이성**: 모든 레이어가 독립적으로 테스트 가능하도록 설계
- **오프라인 우선 (전략 변경 중)**: Firestore 캐시를 우선 활용하며, 필요시 Room DB를 통한 고도화된 로컬 캐싱 전략 적용 ([room_db_decoupling.md](mdc:.cursor/tasks/room_db_decoupling.md) 참조)
- **Data흐름 구조**: data -> datasource -> repository -> viewmodel -> ui (또한 각 계층은 추상화 되어있음)

## 추가 원칙
- LocalDB 사용금지, firebase 캐싱 시스탬 활용해서 우선 복잡도 감소 시키기

## 계층 구조
- **datasource**: 가장 기본적인 구조 DTO형테로 DB에서 데이터 베이스 가지고 오는 역할
- **repository**: 데이터에 구조를 Domain model에 정의된 형태로 usecase에 전달하거나 usecase에서 위임한 직접적인 편집을 하는 계층.
- **usecase**: usecase는 강한 결합이 없어야 한다. 그리고 하나의 시나리오만 책임저야한다. 따라서 usecase에서 다른 usecase를 사용할수 없다. 따라서 다시 구현하거나 과하게 반복되는 경우 repository에게 해당 구현을 일부 위임한다.
- **viewmodel**: ui에 상호작용과 중간 정보 전달을 담당하는 계층 으로 기능에 대한 구현을 최대한 usecase에 위임해야 하고, 기능과 ui의 결합과 전달 반응 이라는 역할로 책임을 최소화해야한다 (복잡해지기 매우 쉽기 때문).
- **ui**: 실제 사용자가 보는 화면 정의하는 계층

## 모듈 구조 및 의존성

현재 프로젝트는 다음과 같은 모듈로 구성되어 있습니다 (settings.gradle.kts 기준):

```
:app                    # 앱 진입점 및 통합 모듈
:navigation             # 앱 전체 네비게이션 관리
:data                   # 데이터 레이어
:domain                 # 도메인 레이어
:core:core_common       # 공통 유틸리티 및 기본 기능
:core:core_logging      # 로깅 관련 기능
:core:core_ui           # UI 공통 컴포넌트
:core:core_navigation   # 네비게이션 관련 핵심 기능
:core:core_fcm          # Firebase Cloud Messaging 관련 기능
:feature:feature_chat   # 채팅 기능
:feature:feature_auth   # 인증 기능
:feature:feature_dev    # 개발자 전용 기능
:feature:feature_friends # 친구 관리 기능
:feature:feature_main   # 메인 화면 기능
:feature:feature_profile # 프로필 관리 기능
:feature:feature_project # 프로젝트 관리 기능
:feature:feature_schedule # 일정 관리 기능
:feature:feature_search # 검색 기능
:feature:feature_settings # 설정 기능
:app_api                # 앱 API 모듈
```

## 데이터 관리 전략

### 시간 처리 전략
- **시간 저장 방식**:
  - 서버에는 모든 시간 정보를 **Instant** 형식(UTC 시간)으로 저장
  - Firebase Timestamp는 내부적으로 UTC 시간 기준으로 처리됨
  - core_common > DateTimeUtil.kt 을 활용 하여 작업
  
- **시간 표현 클래스**:
  - `LocalTime`: 시간만 표현, 시간대 정보 없음
  - `LocalDateTime`: 날짜와 시간 표현, 시간대 정보 없음
  - `ZonedDateTime`: 날짜, 시간, 시간대를 모두 포함
  
- **시간대 변환 흐름**:
  - 네트워크/DB에서 UTC 시간 로드 → 필요시 사용자 시간대로 변환 → UI에 표시
  - UI 입력 → UTC로 변환 → 네트워크/DB에 저장
- **로컬 저장소 미사용:
  - 로컬 DB를 사용하지 않고 firbase에 캐시 시스탬을 활용