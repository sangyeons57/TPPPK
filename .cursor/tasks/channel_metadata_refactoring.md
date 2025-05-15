# 채널 관리 구조 단순화: channelReferences에서 metadata 기반으로 전환

현재 프로젝트/카테고리 채널의 구조적 정보와 순서는 별도의 참조 문서(`channelReferences`)를 통해 관리되고 있습니다. 이 방식은 구조적으로 명확하지만 복잡성을 증가시킵니다. 이를 단순화하기 위해 채널의 모든 정보를 `metadata` 필드(및 필요시 몇 가지 별도 필드)에 통합하는 리팩토링을 진행합니다.

## 주요 변경 사항

- **기존 방식**:
  - `channels/{channelId}`: 기본 채널 정보
  - `projects/{projectId}/categories/{categoryId}/channelReferences/{channelId}`: 카테고리 내 채널 참조 및 순서
  - `projects/{projectId}/projectChannelReferences/{channelId}`: 프로젝트 직접 채널 참조 및 순서

- **신규 방식**:
  - `channels/{channelId}`: 모든 채널 정보를 단일 문서로 통합
  - `order` 필드: 자주 변경되는 순서 정보를 별도 최상위 필드로
  - `metadata`: 채널의 컨텍스트(프로젝트, 카테고리 소속 등) 정보

## 작업 단계

- [ ] **1단계: 스키마 및 모델 수정**
  - [ ] `Channel` 모델에 `order` 필드 추가
  - [ ] `FirestoreConstants.kt`에서 `ChannelFields.ORDER` 정의 업데이트
  - [ ] `ChannelReferenceCollections` 및 `ChannelReferenceFields` 삭제 표시 (당장 제거하지 않고 deprecated 처리)

- [ ] **2단계: `ChannelRemoteDataSource` 리팩토링**
  - [ ] 채널 생성/업데이트 메서드에서 `order` 필드 지원
  - [ ] 채널 조회 메서드에서 정렬 로직 수정 (참조 대신 `order` 필드 사용)
  - [ ] 필요시 특정 프로젝트/카테고리 채널 조회를 위한 쿼리 최적화

- [ ] **3단계: `ProjectStructureRemoteDataSource` 리팩토링**
  - [ ] `channelReferences` 관련 메서드 제거 또는 deprecated 처리
  - [ ] 카테고리 내 채널 조회를 `ChannelRemoteDataSource`에 위임 (metadata 기반 필터링)
  - [ ] 채널 순서 변경 로직을 채널 직접 업데이트로 수정

- [ ] **4단계: Repository 계층 리팩토링**
  - [ ] `ProjectStructureRepository` 인터페이스에서 채널 참조 관련 메서드 제거/수정
  - [ ] `ChannelRepository` 인터페이스에 순서 관리 메서드 추가:
    - `updateChannelOrder(channelId: String, order: Int)`
    - `getChannelsInCategory(projectId: String, categoryId: String, sortByOrder: Boolean = true)`
    - `getDirectProjectChannels(projectId: String, sortByOrder: Boolean = true)`

- [ ] **5단계: 기존 데이터 마이그레이션 로직 구현**
  - [ ] 기존 `channelReferences` 문서 데이터를 채널 `order` 필드로 이전하는 마이그레이션 작업 구현
  - [ ] 마이그레이션 중 데이터 일관성 유지를 위한 트랜잭션 처리
  - [ ] 마이그레이션 완료 확인 후 참조 문서 최종 삭제 계획 수립

- [ ] **6단계: UI 및 UseCase 계층 업데이트**
  - [ ] 채널 목록 정렬 로직 수정 (참조 대신 `order` 필드 사용)
  - [ ] 채널 이동 및 순서 변경 기능 업데이트
  - [ ] 프로젝트/카테고리 전환 UI 로직 수정

- [ ] **7단계: 테스트 작성 및 검증**
  - [ ] 메타데이터 기반 채널 관리의 핵심 기능 테스트 작성
  - [ ] 순서 관리, 필터링, 정렬 기능 검증
  - [ ] 마이그레이션 로직 테스트

- [ ] **8단계: 문서 업데이트**
  - [ ] `.cursor/rules/firestore-schema.mdc` 업데이트하여 변경된 데이터 구조 반영
  - [ ] 개발자용 주석 및 가이드 업데이트

## 구현 세부 지침

### 채널 구조 설계

```
channels/{channelId}
├── id: String
├── name: String
├── description: String
├── type: String (DM, PROJECT, CATEGORY)
├── order: Number (자주 변경되는 순서 정보)
├── metadata: Map<String, Any>
│   ├── source: String (dm, project)
│   ├── projectId: String
│   ├── categoryId: String (해당하는 경우)
│   ├── channelType: String (TEXT, VOICE 등)
├── participantIds: Array<String> (DM 채널의 경우)
├── lastMessagePreview: String
├── lastMessageTimestamp: Timestamp
└── ... 기타 필드
```

### 쿼리 최적화 고려사항

- 특정 프로젝트/카테고리의 채널을 효율적으로 조회하기 위한 복합 인덱스 설정:
  - `metadata.projectId` + `order` (프로젝트 내 직접 채널 조회용)
  - `metadata.projectId` + `metadata.categoryId` + `order` (카테고리 내 채널 조회용)

### 마이그레이션 순서

1. 새 구조로 채널 생성 로직 업데이트 (신규 채널은 새 구조 사용)
2. 기존 채널 데이터를 읽을 때 두 방식 모두 지원 (읽기 호환성)
3. 배치 작업으로 기존 데이터 변환
4. 모든 데이터가 새 구조로 변환된 것을 확인한 후 참조 문서 삭제 