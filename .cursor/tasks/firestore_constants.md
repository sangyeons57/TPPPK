# Firestore Constants 관리 태스크

## 컨텍스트
Firebase Firestore 쿼리에서 컬렉션 이름과 필드 이름을 하드코딩된 문자열로 사용하는 것은 다음과 같은 문제를 일으킬 수 있습니다:
- 오타로 인한 버그 발생
- 이름 변경 시 모든 참조 지점을 수동으로 찾아 변경해야 함
- 코드 자동완성 지원 부족
- 문자열 불일치로 인한 추적하기 어려운 버그

이를 해결하기 위해 Firestore 관련 문자열 상수를 중앙 집중화하여 관리하는 시스템을 도입하였습니다.

## 구현 내용

`core_common` 모듈에 `FirestoreConstants` 클래스를 구현하여 모든 Firestore 관련 상수를 한 곳에서 관리합니다:

```kotlin
object FirestoreConstants {
    // 컬렉션명
    object Collections {
        const val USERS = "users"
        const val PROJECTS = "projects"
        // ...
    }
    
    // 사용자 필드
    object UserFields {
        const val NICKNAME = "nickname"
        const val PROFILE_IMAGE_URL = "profileImageUrl"
        // ...
    }
    
    // 상태값
    object Status {
        const val ACCEPTED = "accepted"
        const val PENDING_SENT = "pending_sent"
        // ...
    }
    
    // 기타 필드 모음...
}
```

## 완료된 작업

- [x] `FirestoreConstants` 클래스 생성
- [x] 주요 컬렉션 이름 상수화
- [x] 필드 이름 상수화
- [x] 상태값 상수화
- [x] 클래스 마이그레이션:
  - [x] `FriendRemoteDataSourceImpl` 클래스
  - [x] `InviteRemoteDataSourceImpl` 클래스
  - [x] `DmRemoteDataSourceImpl` 클래스
  - [x] `ProjectRemoteDataSourceImpl` 클래스
  - [x] `ScheduleRemoteDataSourceImpl` 클래스
  - [x] `UserRemoteDataSourceImpl` 클래스 
  - [x] `ChatRemoteDataSourceImpl` 클래스
  - [x] `ProjectRoleRemoteDataSourceImpl` 클래스
  - [x] `ProjectMemberRemoteDataSourceImpl` 클래스
  - [x] `ProjectStructureRemoteDataSourceImpl` 클래스
- [x] 상수 확장 및 보완
  - [x] `chatChannels` 컬렉션 추가
  - [x] `activeDmIds` 필드 추가
  - [x] 메시지 관련 필드 추가 및 수정(`sentAt`으로 기존 timestamp 필드 변경)
  - [x] 멤버 관련 필드 추가(`ADDED_AT`, `ADDED_BY`, `UPDATED_AT`, `UPDATED_BY`)
  - [x] 역할 관련 필드 추가(`CREATED_BY`, `UPDATED_BY`)
  - [x] DM 관련 필드 추가(`CREATED_AT`)
  - [x] 프로젝트 관련 필드 추가(`PARTICIPATING_MEMBERS`)
  - [x] 카테고리 관련 필드 추가(`CategoryFields`)
  - [x] 채널 관련 필드 추가(`ChannelFields`)
- [x] 추가 데이터소스 구현체 확인 및 마이그레이션
  - [x] `ProjectStructureRemoteDataSourceImpl` 클래스 마이그레이션 완료
- [x] 단위 테스트 추가 및 업데이트
  - [x] `FirestoreConstantsTest` - 상수값 검증 테스트 추가
  - [x] `RemoteDataSourceConstantsTest` - 원격 데이터소스에서 상수 사용 검증 테스트 예시 추가
- [x] 누락된 필드 모니터링 및 추가 완료

## 마이그레이션 시 사용한 방법

1. 클래스 상단에 적절한 import 구문 추가:
```kotlin
import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.UserFields
// 필요한 다른 상수 그룹
```

2. 하드코딩된 문자열을 상수로 대체:
```kotlin
// 변경 전:
val userDoc = firestore.collection("users").document(userId).get().await()
val name = userDoc.getString("nickname") ?: "Unknown"

// 변경 후:
val userDoc = firestore.collection(Collections.USERS).document(userId).get().await()
val name = userDoc.getString(UserFields.NICKNAME) ?: "Unknown"
```

3. 날짜/시간 처리 개선:
```kotlin
// 변경 전: 복잡한 타임스탬프 변환 코드
val timestamp = doc.getTimestamp("lastMessageTimestamp")
val lastMessageTime = if (timestamp != null) {
    val seconds = timestamp.seconds
    val nanos = timestamp.nanoseconds
    LocalDateTime.ofInstant(
        Instant.ofEpochSecond(seconds, nanos.toLong()),
        ZoneId.systemDefault()
    )
} else {
    null
}

// 변경 후: DateTimeUtil 사용
val lastMessageTimestamp = DateTimeUtil.toLocalDateTime(doc.getTimestamp(DmFields.LAST_MESSAGE_TIMESTAMP))
```

## 이점

- **오타 방지**: IDE가 자동완성 및 오타 검출을 지원
- **일관성**: 동일한 필드나 컬렉션이 다른 방식으로 참조되는 문제 해결
- **리팩토링 용이성**: 이름 변경 시 모든 참조 지점을 자동으로 리팩토링 가능
- **문서화**: 각 필드와 컬렉션의 용도가 주석으로 문서화됨
- **날짜 처리 향상**: DateTimeUtil과 함께 사용하여 날짜/시간 처리 코드 간소화
- **테스트 용이성**: 상수를 사용하면 단위 테스트에서 검증이 더 쉬워짐

## 주의사항

- 마이그레이션 과정에서 기존 쿼리의 동작이 변경되지 않도록 주의해야 합니다.
- 상수 이름을 명확하게 지정하여 코드 가독성을 유지해야 합니다.
- 새로운 필드나 컬렉션이 추가될 때 반드시 상수 클래스에도 추가해야 합니다. 

## 이번 마이그레이션에서 발견된 이슈

- `ChatRemoteDataSourceImpl`에서 사용하는 `sentAt` 필드가 `MessageFields.TIMESTAMP`로 표현되어 있어 일관성을 맞추기 위해 상수명은 유지하고 값을 수정했습니다.
- 일부 클래스에서 사용하는 필드 중 `FirestoreConstants`에 아직 추가되지 않은 것들을 발견하여 추가했습니다.
- `ProjectStructureRemoteDataSourceImpl` 클래스 마이그레이션 과정에서 누락된 카테고리, 채널 관련 필드들을 발견하여 `CategoryFields`와 `ChannelFields` 객체를 새로 추가했습니다.
- 데이터소스의 새로운 구현체가 추가되면 계속해서 상수 클래스를 업데이트해야 합니다. 

## 최종 결과

모든 RemoteDataSource 구현체 클래스의 Firestore 문자열 상수를 마이그레이션하고, 관련 필드들을 `FirestoreConstants` 클래스에 추가했습니다. 또한 단위 테스트를 통해 상수값이 올바르게 정의되어 있는지 검증할 수 있는 기반을 마련했습니다. 이러한 접근 방식은 앞으로 코드베이스에서의 Firestore 관련 작업을 더 쉽고 안정적으로 만들 것입니다.

## 향후 작업

- 새로운 Firestore 관련 클래스가 추가될 때 상수 활용 여부 검토
- 코드 리뷰를 통해 하드코딩된 문자열이 있는지 지속적으로 확인
- 단위 테스트 커버리지 확대 (각 원격 데이터소스 구현체별 테스트) 