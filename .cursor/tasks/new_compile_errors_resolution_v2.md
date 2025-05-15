# Task: Resolve New Compilation Errors (v2)

- [x] **Group 1: Unresolved References (Constants, Mappers, Utils)**
    - **Files:** `ChannelRemoteDataSourceImpl.kt`, `ProjectStructureRemoteDataSourceImpl.kt`, `ChannelRepositoryImpl.kt`
    - **Errors:** `PARTICIPANT_IDS`, `METADATA`, `ORDER`, `DM_USERS`, `CHANNEL_TYPE`, `TYPE`, `documents`, `id`, `data`, `toChannelPermission`, `toChatMessage`, `TAG`, `flowOf`, `whereNull`, `exists`
    - **Probable Cause:** Missing or incorrect constants (e.g., in `FirestoreConstants.kt`), incorrect field names for Firestore, or missing imports for mappers/utility functions.
    - **Action:**
        1.  **[x]** `FirestoreConstants.kt` 파일을 확인하여 필요한 상수들이 정의되어 있는지, 올바른 값으로 정의되어 있는지 확인합니다. (`ChannelFields`의 `ORDER`, `PARTICIPANT_IDS`, `METADATA`는 새로운 `projectSpecificData` 및 `dmSpecificData`로 대체된 것을 확인)
        2.  **[x]** Firestore 문서 필드명과 코드상의 참조가 일치하는지 확인합니다. - `ChannelRemoteDataSourceImpl.kt`의 `createChannel`, `getChannels`, `getChannelsStream`, `documentToChannel`, `updateChannel`, `createDmChannel`, `deleteChannel`, `addParticipantToDmChannel`, `removeParticipantFromDmChannel` 함수에서 주요 상수 참조 수정 완료.
        3.  **[x]** `ChannelRemoteDataSourceImpl.kt`의 나머지 `Unresolved reference` 오류 (`DM_USERS`, `CHANNEL_TYPE` 상수, `TYPE` 상수 등) 확인 및 수정 완료 (대부분 이전 단계에서 해결됨).
        4.  **[x]** `ProjectStructureRemoteDataSourceImpl.kt`의 `Unresolved reference` 오류 (`ORDER`, `documents`, `id`, `getLong`) 확인 및 수정 완료 (`ORDER`는 문자열 리터럴로 변경, 나머지는 문제 없어 보임).
        5.  **[x]** `ChannelRepositoryImpl.kt`의 `Unresolved reference` 오류 (`metadata`, `ORDER`, `TYPE`, `whereEqualTo`, `documents`, `METADATA`, `PARTICIPANT_IDS`, `TAG`, `flowOf`, `DM_USERS`, `CHANNEL_TYPE`) 확인 및 주요 함수들 (`createChannel`, `getChannelsByMetadata`, `getChannelsByType`, `getDmChannelByUsers`, `createProjectChannel`) 수정 완료. `TAG` 및 `flowOf` 임포트 추가.
        6.  **[x]** 매퍼 함수(`toChatMessage`, `toChannelPermission` 등) 및 유틸리티 함수(`flowOf`)의 임포트가 올바른지 확인하고, 관련 `Unresolved reference` 오류를 수정합니다. (`flowOf` 임포트 완료, `toChannelPermission`은 `ChannelPermission.toFirestoreMap()`으로 대체, `toChatMessage` 함수 구현 추가)
        7.  A2A Docs 또는 웹 검색을 통해 Firestore 상수 관리, Kotlin 확장 함수, Flow 사용법 등 관련 문맥을 파악합니다.

- [x] **Group 2: Type Inference & Argument Mismatch / No Parameter / Overload Resolution**
    - **Files:** `ChannelRemoteDataSourceImpl.kt`, `ChannelRepositoryImpl.kt`
    - **Errors:** `Cannot infer type...`, `Argument type mismatch...`, `No value passed for parameter...`, `None of the following candidates is applicable...`, `No parameter with name '...' found.`, `Overload resolution ambiguity...`
    - **Probable Cause:** Incorrect map/Pair creation, incorrect function calls, data class instantiation with missing/wrong parameters, ambiguous function calls.
    - **Action:**
        1. **[x]** 타입 추론이 실패하는 부분에 명시적으로 타입을 지정합니다. (ChannelRepositoryImpl.kt의 whereArrayContainsAtLeastOneOf 관련 코드 수정)
        2. **[x]** `mapOf(...)`, `Pair(key, value)` 또는 `key to value` 형태의 Map 생성 문법을 확인하고 수정합니다.
        3. **[x]** 함수 호출 시 시그니처를 확인하여 모든 필수 인자가 정확한 타입으로 전달되었는지 확인합니다. (ProjectStructureRemoteDataSourceImpl.kt의 channelMode 파라미터 수정)
        4. **[x]** 데이터 클래스 생성 시 모든 필요한 파라미터가 이름과 함께 정확히 전달되었는지 확인합니다.
        5. **[x]** 오버로드된 함수 호출 시 모호성이 발생하지 않도록 인자 타입을 명확히 하거나 캐스팅을 사용합니다. (ChannelRemoteDataSourceImpl.kt의 isNotEmpty() 호출 수정)
        6. A2A Docs 또는 웹 검색을 통해 Kotlin 타입 시스템, 컬렉션 함수, 함수 오버로딩 관련 문맥을 파악합니다.

- [x] **Group 3: `ChannelRepositoryImpl.kt` - Abstract Member Implementation & Overrides Nothing**
    - **File:** `data/src/main/java/com/example/data/repository/ChannelRepositoryImpl.kt`
    - **Errors:** `Class 'ChannelRepositoryImpl' is not abstract and does not implement abstract members...`, `'someFunction' overrides nothing.`
    - **Probable Cause:** `ChannelRepository` 인터페이스 변경 후 `ChannelRepositoryImpl`에 해당 변경사항이 반영되지 않았거나, 오타 등으로 인해 오버라이드 하려는 함수의 시그니처가 일치하지 않는 경우.
    - **Action:**
        1. **[x]** `ChannelRepository` 인터페이스를 확인하고, `ChannelRepositoryImpl`에 누락된 모든 추상 함수들을 구현합니다. (addDmParticipant, removeDmParticipant, getDmParticipants, getDmParticipantsStream, getProjectChannels, getProjectChannelsStream, getCategoryChannels, getCategoryChannelsStream 함수 구현)
        2. **[x]** 오버라이드하려는 함수의 시그니처(이름, 파라미터 타입 및 개수, 반환 타입)가 인터페이스의 함수와 정확히 일치하는지 확인하고 수정합니다. (createProjectChannel 함수 시그니처 수정)
        3. A2A Docs 또는 웹 검색을 통해 Kotlin 인터페이스 구현 및 함수 오버라이딩 규칙을 확인합니다.

- [x] **Group 4: `ProjectStructureRemoteDataSourceImpl.kt` - Smart Cast Impossible & Unresolved References**
    - **File:** `data/src/main/java/com/example/data/datasource/remote/projectstructure/ProjectStructureRemoteDataSourceImpl.kt`
    - **Errors:** `Smart cast to 'ProjectSpecificData' is impossible...`, `Unresolved reference 'ORDER'`, `Unresolved reference 'saveChannel'`
    - **Probable Cause:** Kotlin 스마트 캐스트 제약, Firestore 필드명 상수 누락, `saveChannel` 함수의 이름 변경 또는 삭제.
    - **Action:**
        1. **[x]** 스마트 캐스트 오류: 해당 속성을 지역 변수에 할당한 후 사용하거나, `as?` 등을 사용한 명시적 캐스팅을 고려합니다. (지역 변수 projData 사용)
        2. **[x]** `ORDER` 상수 참조 오류: Group 1의 해결 방법을 참고합니다. (이미 `ChannelProjectDataFields.ORDER`로 수정되었을 수 있음) -> 문자열 리터럴 "order"로 수정 완료.
        3. **[x]** `saveChannel` 참조 오류: 해당 함수가 실제로 어떤 작업을 수행해야 하는지, 이름이 변경되었는지, 또는 다른 함수로 대체되었는지 확인하고 수정합니다. (updateChannel로 변경)
        4. A2A Docs 또는 웹 검색을 통해 Kotlin 스마트 캐스트, Firestore 데이터 조작 관련 문맥을 파악합니다.

- [x] **Group 5: `ChannelMapper.kt` - Argument Type Mismatch (Instant? to Instant)**
    - **File:** `data/src/main/java/com/example/data/model/mapper/ChannelMapper.kt`
    - **Errors:** `Argument type mismatch: actual type is 'Instant?', but 'Instant' was expected.`
    - **Probable Cause:** Nullable 타입인 `Instant?` 값을 Non-nullable 타입인 `Instant`을 받는 곳에 전달하려고 할 때 발생합니다.
    - **Action:**
        1. **[x]** Nullable `Instant` 값을 처리합니다. (예: `?:` 엘비스 연산자를 사용하여 기본값 제공, `!!` 단언 연산자 사용(주의 필요), 또는 null을 허용하도록 수신 측 변경) -> `mapToDomain`에서 `Instant.now()`로 폴백 처리 완료.
        2. A2A Docs 또는 웹 검색을 통해 Kotlin null 안전성 및 `java.time.Instant` 사용법을 확인합니다.

- [x] **Group 6: Firestore API & Kotlin Collection/Syntax Errors in `ChannelRepositoryImpl.kt`**
    - **File:** `data/src/main/java/com/example/data/repository/ChannelRepositoryImpl.kt`
    - **Errors:** `Unresolved reference 'whereEqualTo'`, `Unresolved reference 'addSnapshotListener'`, `Function invocation 'isEmpty()' expected.` 등
    - **Probable Cause:** Firestore SDK 사용법 미숙, Kotlin 컬렉션 함수나 일반 문법 오류.
    - **Action:**
        1. **[x]** Firestore 쿼리 (`whereEqualTo`, `orderBy` 등) 및 리스너 (`addSnapshotListener`) 사용법을 공식 문서나 예제를 통해 확인하고 수정합니다. (getUserChannels, getUserChannelsStream 함수 재구현)
        2. **[x]** Kotlin 컬렉션 함수(`mapNotNull`, `firstOrNull` 등)와 일반적인 문법(반환 값, 표현식 본문 등)을 확인하고 수정합니다. (currentUserId 함수를 블록 본문으로 변경)
        3. A2A Docs 또는 Firebase 공식 문서, Kotlin 공식 문서를 참조하여 올바른 사용법을 익힙니다.

- [x] **Group 7: `ChatRepositoryImpl.kt` - DM 데이터 관련 문제**
    - **File:** `data/src/main/java/com/example/data/repository/ChatRepositoryImpl.kt`
    - **Errors:** 메타데이터 제거 후 DM 특화 데이터 누락
    - **Probable Cause:** FirestoreConstants 구조 변경(metadata → dmSpecificData/projectSpecificData)에 따른 코드 미반영
    - **Action:**
        1. **[x]** `ChatRepositoryImpl.kt` 파일에서 기존 metadata 관련 코드를 찾아 dmSpecificData로 적절히 변경합니다. (FirestoreConstants 상수 임포트 추가)
        2. **[x]** DM 채널 생성/조회/수정 시 dmSpecificData.participantIds 필드를 올바르게 사용하도록 수정합니다.
        3. **[x]** 관련 쿼리와 데이터 매핑 코드를 FirestoreConstants의 새 구조에 맞게 업데이트합니다.

- [x] **Group 8: 기타 컴파일 에러**
    - **Files:** `ChannelRemoteDataSourceImpl.kt`, `ProjectStructureRemoteDataSourceImpl.kt`, `ChannelRepositoryImpl.kt`
    - **Errors:** 
        - `Overload resolution ambiguity between candidates: fun <T> Array<out T>.isNotEmpty(): Boolean...`
        - `Smart cast to 'ProjectSpecificData' is impossible, because 'projectSpecificData' is a public API property declared in different module.`
        - `No parameter with name 'channelMode' found.`
        - `Unresolved reference 'saveChannel'.`
        - `Returns are prohibited for functions with an expression body. Use block body '{...}'.`
        - `Unresolved reference 'whereArrayContainsAtLeastOneOf'.`
        - `Cannot infer type for this parameter. Specify it explicitly.`
        - `Unresolved reference 'Path'.`
        - `Assignment type mismatch: actual type is 'Query', but 'CollectionReference' was expected.`
        - `Unresolved reference 'whereNull'.`
        - `None of the following candidates is applicable: suspend fun <T> Task<T>.await(): T...`
    - **Probable Cause:** 다양한 문법 오류, 타입 불일치, 함수 시그니처 불일치, 미구현 API 사용 시도 등
    - **Action:**
        1. **[x]** `isNotEmpty()` 모호성: 컬렉션 타입을 명확히 지정하거나 명시적 캐스팅을 사용합니다. (value as Map<*, *>).isNotEmpty()로 수정)
        2. **[x]** 스마트 캐스트 불가: 명시적 캐스팅(`as?`)을 사용하거나 지역 변수에 할당 후 사용합니다. (projectData 지역 변수 사용)
        3. **[x]** 파라미터 이름 오류: 함수 시그니처를 확인하고 올바른 파라미터 이름을 사용합니다. (ProjectStructureRemoteDataSourceImpl.kt의 channelMode 파라미터 수정)
        4. **[x]** `saveChannel` 참조 오류: 함수 이름이 변경되었거나 다른 함수로 대체되었는지 확인합니다. (updateChannel로 대체)
        5. **[x]** 표현식 본문 오류: 함수 본문을 중괄호 `{}`로 감싸 블록 본문으로 변경합니다. (currentUserId 함수 수정)
        6. **[x]** 지원되지 않는 API 참조 오류(`whereArrayContainsAtLeastOneOf`, `Path`, `whereNull`): Firestore에서 실제 지원하는 API를 확인하고 대체 방법을 사용합니다. (getUserChannels 함수 재구현, getNextChannelOrder 함수 추가)
        7. **[x]** 타입 불일치: 명시적 타입 캐스팅을 사용하거나 중간 변수를 통해 타입을 명확히 합니다. (스마트 캐스트 문제 해결과 동일한 방법으로 해결) 

## 새로운 컴파일 에러 해결 계획 v2

### 1. 변경된 필드 구조로 인한 에러 ✅
- **원인**: `channelMode` 필드가 `Channel` 클래스에서 `ProjectSpecificData`로 이동함
- **해결방법**: 코드베이스에서 Channel 모델의 channelMode 필드를 참조하는 부분을 ProjectSpecificData의 channelMode로 변경

### 2. 데이터소스 레이어 마이그레이션 에러 ✅
- **Files:** `ChannelRemoteDataSourceImpl.kt`, `ChannelRepositoryImpl.kt`
- **원인**: Firestore 상수와 데이터 필드 참조 방식이 변경됨
- **해결방법**: 변경된 경로에 맞게 코드 수정

### 3. ChannelRepositoryImpl.kt의 누락된 구현 ✅
- **원인**: ChannelRepository 인터페이스가 확장되었으나 일부 메서드가 구현되지 않음
- **해결방법**: 인터페이스에 정의된 메서드 구현 추가

#### 주요 구현된 메서드
1. **getChannelsByMetadata / getChannelsByMetadataStream** ✅
   - 메타데이터 필드를 기반으로 채널 필터링 기능 구현
   - 프로젝트/DM 특화 데이터 필드 접근 방식 통일

2. **searchMessages** ✅
   - 텍스트 기반 메시지 검색 기능 구현
   - 접두어 매칭 방식으로 효율적인 검색 구현

3. **권한 관련 메서드** ✅
   - `setChannelPermission`, `getChannelPermission` 구현
   - 권한 설정 및 조회 로직 구현

4. **DM 채널 관련 메서드** ✅
   - `createOrGetDmChannel`, `getDmChannelByUsers` 구현
   - `getUserDmChannels`, `getUserDmChannelsStream` 구현
   - 사용자 ID 기반 DM 채널 관리 로직 구현

5. **프로젝트 채널 관련 메서드** ✅
   - `createProjectChannel` 구현
   - `getProjectChannels`, `getProjectChannelsStream` 구현
   - `getCategoryChannels`, `getCategoryChannelsStream` 구현
   - 프로젝트/카테고리 ID 기반 채널 관리 로직 구현

6. **채널 활동 관리 메서드** ✅
   - `markChannelAsRead`, `getUnreadCount` 구현
   - 읽음 상태 및 읽지 않은 메시지 수 관리 로직 구현

### 4. 유틸리티 메서드 참조 에러 ✅
- **원인**: 참조되는 유틸리티 메서드(`toFirestore`, `toChatMessage` 등)가 없음
- **해결방법**: 필요한 유틸리티 메서드 구현 또는 대체 로직 사용

#### 사용된 대체 메서드:
1. **toFirestore 대체**: `channelMapper.mapToFirestore` 사용
2. **toChatMessage 대체**: `messageMapper.mapToDomain` 사용
3. **DocumentSnapshot.getSafely 확장 함수** 추가

### 5. 멤버 함수 구문 오류 ✅
- **원인**: 클래스 내부의 로컬 함수에 잘못된 접근 제한자(`override`, `private`) 사용
- **해결방법**: 로컬 함수로 정의된 메서드를 클래스의 멤버 메서드로 변경

### 6. 타입 추론 오류 ✅
- **원인**: 제네릭 함수 호출 시 타입 매개변수가 명시되지 않음
- **해결방법**: 타입 매개변수를 명시적으로 지정
  ```kotlin
  doc.getSafely<List<String>>(path)
  ```

### 7. 파라미터 누락 오류 ✅
- **원인**: 함수 호출 시 필요한 파라미터가 누락됨(`senderName`, `senderProfileUrl` 등)
- **해결방법**: 누락된 파라미터 추가 또는 기본값 설정

### 작업 완료 사항
1. **채널 모드 필드 이동 적용** ✅
   - `ProjectSpecificData`에 `channelMode` 추가
   - `Channel` 클래스에서 `channelMode` 속성 제거하고 계산 프로퍼티로 대체
   - `ChannelMapper` 수정하여 매핑 로직 조정

2. **유틸리티 함수 추가** ✅
   - `DocumentSnapshot.getSafely` 확장 함수 추가
   - 필요한 Import 추가

3. **미구현 메서드 구현** ✅
   - `ChannelRepository` 인터페이스의 모든 메서드 구현
   - 적절한 오류 처리 및 타입 검사 추가

4. **로컬 함수 수정** ✅
   - 로컬 함수의 `override` / `private` 제거
   - 중첩된 함수를 클래스의 메서드로 이동

### 남은 작업
1. **테스트 및 검증**
   - 변경사항을 테스트하기 위한 단위 테스트 작성
   - 컴파일 오류 해결 확인 