# Date to LocalDateTime Migration Task

## Context
이 프로젝트는 날짜와 시간 처리에 있어 `java.util.Date` 타입에서 보다 현대적인 `java.time.LocalDateTime` 클래스로 마이그레이션하고 있습니다. 이 마이그레이션의 목적은 날짜/시간 처리를 보다 안전하고 직관적으로 만드는 것입니다.

## 완료된 작업
1. 코어 모듈에 `DateTimeUtil` 유틸리티 클래스 생성
2. 다음 클래스들의 `Date` 타입을 `LocalDateTime`으로 변경:
   - DmLocalDataSource 인터페이스
   - DmLocalDataSourceImpl 구현 클래스
   - InviteLocalDataSource 인터페이스
   - InviteLocalDataSourceImpl 구현 클래스
   - FriendRequest 모델 및 관련 엔티티
3. Room TypeConverter 업데이트
4. 필드명 불일치 해결 (id vs channelId, nickname vs userName 등)
5. Firebase 트랜잭션 관련 에러 수정
6. 날짜/시간 포맷팅 유틸리티 메소드 추가

## DateTimeUtil 사용 방법

새로 추가된 `DateTimeUtil` 클래스는 코어 모듈에 위치해 있으며, 날짜/시간 변환을 위한 다양한 메소드를 제공합니다.

### 주요 기능

```kotlin
// 1. LocalDateTime -> Date 변환
val date = DateTimeUtil.toDate(localDateTime)

// 2. Date -> LocalDateTime 변환
val localDateTime = DateTimeUtil.toLocalDateTime(date)

// 3. Firebase Timestamp -> LocalDateTime 변환
val localDateTime = DateTimeUtil.toLocalDateTime(timestamp)

// 4. LocalDateTime -> Firebase Timestamp 변환  
val timestamp = DateTimeUtil.toTimestamp(localDateTime)

// 5. 에포크 밀리초 -> LocalDateTime 변환
val localDateTime = DateTimeUtil.fromEpochMillis(epochMillis)

// 6. LocalDateTime -> 에포크 밀리초 변환
val epochMillis = DateTimeUtil.toEpochMillis(localDateTime)

// 7. 현재 시간 (시스템 시간대)
val now = DateTimeUtil.now()

// 8. UTC 기준 현재 시간
val nowUtc = DateTimeUtil.nowUtc()
```

### 날짜/시간 포맷팅

UI에서 날짜와 시간을 표시할 때는 다음과 같은 포맷팅 메소드를 활용하세요:

```kotlin
// 날짜만 포맷팅 (예: "2023-07-15")
val dateStr = DateTimeUtil.formatDate(localDateTime)

// 시간만 포맷팅 (예: "14:30")
val timeStr = DateTimeUtil.formatTime(localDateTime)

// 채팅 시간 포맷팅 (예: "오전 9:30")
val chatTimeStr = DateTimeUtil.formatChatTime(localDateTime)

// 날짜와 시간 포맷팅 (예: "2023-07-15 14:30")
val dateTimeStr = DateTimeUtil.formatDateTime(localDateTime)

// 초 단위까지 포함한 날짜와 시간 포맷팅 (예: "2023-07-15 14:30:45")
val dateTimeWithSecondsStr = DateTimeUtil.formatDateTimeWithSeconds(localDateTime)

// 사용자 정의 포맷 적용 (예: "2023년 07월 15일")
val customFormattedStr = DateTimeUtil.format(localDateTime, "yyyy년 MM월 dd일")
```

### 권장 사항

1. **일관된 시간대 처리**: 서버에는 항상 UTC 시간으로 저장하고, UI 표시 시 필요에 따라 로컬 시간대로 변환하세요.

2. **Null 안전성**: DateTimeUtil 메소드는 null을 안전하게 처리합니다. 필요한 경우 엘비스 연산자(`?:`)를 사용해 기본값을 제공하세요.

3. **Room TypeConverter 활용**: Room 데이터베이스에서는 `AppTypeConverters`가 이미 DateTimeUtil을 사용하도록 업데이트되었습니다.

4. **Firebase Timestamp 변환**: Firebase에서 타임스탬프를 가져올 때는 항상 `DateTimeUtil.toLocalDateTime(timestamp)` 메소드를 사용하세요.

5. **UI 포맷팅**: UI에서 날짜/시간을 표시할 때는 목적에 맞는 포맷팅 메소드를 사용하세요.

## 남은 작업

1. ViewModel 내 날짜/시간 형식 변환 로직을 DateTimeUtil 사용하도록 업데이트
2. UI 컴포넌트에서 DateTimeUtil의 포맷팅 메소드 활용
3. 단위 테스트 추가

## 마이그레이션 시 주의사항

1. Firebase Firestore에서 timestamp 필드를 가져올 때는 `getDate()` 대신 `getTimestamp()`를 사용해야 합니다.

2. 도메인 모델과 엔티티 간 변환 시 필드 이름 불일치에 주의하세요. (예: `channelId` vs `id`, `userName` vs `nickname`)

3. `NetworkConnectivityMonitor`를 사용할 때는 다음과 같이 업데이트된 방식으로 사용하세요:
   ```kotlin
   // 이전 방식 (지원 중단)
   if (networkMonitor.isConnected) { ... }
   
   // 새로운 방식
   if (networkMonitor.isNetworkAvailable.first()) { ... }
   ```

## 참고 자료

- [Java 8 Date Time API](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)
- [Firebase Timestamp](https://firebase.google.com/docs/reference/android/com/google/firebase/Timestamp)
- [Room TypeConverters](https://developer.android.com/reference/androidx/room/TypeConverters)

# Task: Date에서 LocalDateTime으로 타입 마이그레이션

- [x] Step 1: DmLocalDataSource 인터페이스의 Date 타입을 LocalDateTime으로 변경
- [x] Step 2: DmLocalDataSourceImpl 클래스의 Date → LocalDateTime 변환 코드 수정 (ZoneId import 추가)
- [x] Step 3: 다른 데이터소스 인터페이스에서 Date 타입 사용 현황 확인 및 수정
      - [x] InviteRemoteDataSource 인터페이스 및 구현체 수정
      - [x] Invite 도메인 모델 수정
      - [x] InviteRepository 인터페이스 및 구현체 수정
      - [x] DM 관련 모델 및 엔티티 필드명 불일치 해결 (id vs channelId, otherUser vs partnerUser)
      - [x] Friend 관련 모델 및 엔티티 필드명 불일치 해결
      - [x] DmRemoteDataSourceImpl의 코루틴 호출 문제 해결
      - [x] expression body -> block body 변환 문제 해결 (Returns are prohibited...)
      - [x] NetworkConnectivityMonitor의 isConnected 참조 문제 해결
- [x] Step 4: 관련 Repository 구현체에서 타입 변환 코드 확인 및 수정
      - [x] ScheduleRepositoryImpl의 Date.from 코드를 Instant.toEpochMilli로 변경
      - [x] FriendLocalDataSourceImpl에서 Date.now()를 LocalDateTime.now()로 변경
- [x] Step 5: 도메인 모델에서 Date 타입 사용 현황 확인 및 수정
      - [x] Schedule의 Date 관련 주석 수정
      - [x] InviteEntity의 Date 변환 로직을 LocalDateTime 기반으로 수정
- [x] Step 6: ViewModel 및 UI 레이어에서 날짜 표시 관련 코드 확인 및 수정
      - [x] AddScheduleViewModel에서 사용하지 않는 Date import 제거 