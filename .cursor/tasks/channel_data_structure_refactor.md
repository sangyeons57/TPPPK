# 채널 데이터 구조 개선 계획

## 1. 개요

채널 데이터 구조를 개선하여 타입별로 특화된 데이터를 효율적으로 관리하고, 일관된 접근 인터페이스를 제공합니다. 주요 변경 사항은 다음과 같습니다:

- 기존의 meta 필드 기반 구조에서 타입별 특화 데이터 구조로 변경
- DM과 프로젝트 채널에 대한 명확한 데이터 모델 분리
- 새로운 구조에 맞춘 DB 스키마 마이그레이션
- 이전 ProjectChannel 모델을 새로운 Channel 모델로 일원화

## 2. 주요 변경 사항

### 2.1 도메인 모델 변경

```kotlin
// 1. 기존 Channel 모델에서 제거된 필드
- metadata: Map<String, Any>?
- participantIds: List<String>
- order: Int

// 2. 추가된 필드
+ projectSpecificData: ProjectSpecificData?
+ dmSpecificData: DmSpecificData?
+ channelMode: String?

// 3. 제거되는 모델
- ProjectChannel (이제 Channel로 대체됨)
```

### 2.2 특화 데이터 클래스 추가

```kotlin
// ProjectSpecificData
data class ProjectSpecificData(
    val projectId: String,
    val categoryId: String? = null,
    val order: Int = 0
)

// DmSpecificData
data class DmSpecificData(
    val participantIds: List<String> = emptyList()
)
```

### 2.3 Mapper 변경

- `ChannelMapper`: Firestore 문서와 도메인 모델 간 변환 로직 업데이트
- 타입별 특화 데이터 매핑 구현

### 2.4 DB 마이그레이션

- Room DB 스키마 업데이트
- Room `@TypeConverter` 추가 (`ListConverter`)
- 마이그레이션 스크립트 작성 (ChannelMigration)

### 2.5 Repository 관련 변경

- `ChannelRepositoryImpl`: 메타데이터 기반 메서드 대신 타입별 특화 메서드 사용
- `ProjectStructureRemoteDataSourceImpl`: 채널 참조 로직 업데이트

## 3. 작업 리스트

- [x] 도메인 모델 업데이트 (Channel, ProjectSpecificData, DmSpecificData)
- [x] ChannelMapper 업데이트
- [x] Room 엔티티 및 TypeConverter 업데이트
- [x] ProjectCategory에서 ProjectChannel 대신 Channel 사용 
- [x] GetProjectStructureUseCase 샘플 코드에서 ProjectChannel 대신 Channel 사용
- [x] Room 데이터베이스 마이그레이션 스크립트 작성
- [x] ChannelRepository 구현체에서 불필요한 메서드 제거 및 업데이트
- [x] ChannelDao 인터페이스 생성 및 구현
- [ ] 채널 생성 로직 업데이트
- [ ] UI 및 ViewModel 로직 업데이트 
- [ ] 단위 테스트 업데이트

## 4. 마이그레이션 방안

### 4.1 Firestore 데이터

1. 기존 채널 문서를 순회하며 새로운 구조로 변환
2. 타입에 따라 특화 데이터 필드 추가
3. 더 이상 사용하지 않는 필드 제거 (participantIds, metadata, order 등)

### 4.2 로컬 Room 데이터베이스

1. 마이그레이션 버전 증가 (7 -> 8)
2. 새로운 스키마에 맞게 테이블 구조 변경
3. 기존 데이터를 새로운 구조로 변환

```kotlin
// Room 마이그레이션 구현
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 채널 테이블 마이그레이션
        // 1. 임시 테이블 생성
        // 2. 기존 데이터를 새 구조로 복사
        // 3. 기존 테이블 삭제 및 이름 교체
    }
}
```

## 5. 주의 사항

- 호환성을 위해 `ChannelBackwardCompatibility` 클래스를 통해 이전 버전 호환성 지원
- 기존 코드 중 metadata나 참가자 목록을 직접 참조하는 부분을 새 메서드로 대체
- 테스트를 통해 모든 기능이 제대로 작동하는지 확인
- 다음을 제외하고는 `ProjectChannel`이란 이름의 클래스를 모두 `Channel`로 변경
  - `ChannelIdentifier.ProjectChannel`: 이 클래스는 채널 식별자로 사용되며 도메인 모델과는 다른 용도

## 6. 다음 단계 작업

현재 완료된 작업:
- 도메인 모델과 데이터 계층 클래스 업데이트 (Channel, DmSpecificData, ProjectSpecificData)
- Room 데이터베이스 마이그레이션 스크립트 작성
- ChannelDao 인터페이스 생성
- 메타데이터 접근 방식을 사용한 메서드의 Deprecated 처리와 대체 메서드 제공
- ProjectChannel 클래스를 Channel 클래스로 통합

앞으로 진행할 작업:
- 채널 생성 로직 업데이트 (createChannel, createDmChannel 등의 메서드 개선)
- ViewModel과 UI 계층에서 채널 접근 방식 업데이트
- Firestore 데이터 마이그레이션 스크립트 작성 (기존 데이터를 새로운 구조로 변환) 