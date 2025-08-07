# 채팅 시스템 테스트 가이드

## 역할 분담 및 중복 제거

### 개요

채팅 시스템의 역할 중복 문제를 해결하고 불필요한 기능들을 제거하여 코드를 정리했습니다.

### 아키텍처 개선

#### 1. 역할 분담 명확화

**WebSocketChatViewModel**

- ✅ UI 상태 관리 (ChatUiState)
- ✅ 사용자 인터페이스 이벤트 처리
- ✅ 초기화 및 생명주기 관리
- ✅ MessageService를 통한 메시지 작업 위임

**MessageService**

- ✅ 메시지 전송/수정/삭제 (WebSocket UseCase 활용)
- ✅ Paging3 플로우 제공 (Room DB 기반)
- ✅ Anchor 기반 양방향 페이징
- ✅ 오프라인 메시지 큐잉

**Core WebSocket**

- ✅ WebSocket 연결 관리
- ✅ 이벤트 스트림 제공
- ✅ 인증 및 연결 상태 관리
- ✅ 방 입장/퇴장 관리

#### 2. 제거된 중복 기능

**WebSocketChatViewModel에서 제거:**

- ❌ 직접적인 메시지 전송/수정/삭제 (MessageService로 위임)
- ❌ WebSocket 연결 관리 (Core WebSocket으로 위임)
- ❌ Paging3 직접 관리 (MessageService로 위임)
- ❌ PagingSource 무효화 로직 (자동 처리됨)
- ❌ 수신 메시지 이벤트 구독 (Core WebSocket에서 처리)
- ❌ 불필요한 import들

**MessageService에서 제거:**

- ❌ WebSocket 연결 관리 (Core WebSocket으로 위임)
- ❌ 방 입장/퇴장 (Core WebSocket으로 위임)
- ❌ 이벤트 구독 (Core WebSocket으로 위임)
- ❌ 초기 메시지 로딩 (SyncUseCase로 위임)
- ❌ ChatUseCases 의존성 (불필요)

#### 3. 개선된 메시지 작업 흐름

```kotlin
// 이전: ViewModel에서 직접 처리
viewModel.sendMessage(text) // 복잡한 로직

// 현재: MessageService 위임
services.messageService.sendTextMessage(senderId, content)
```

#### 4. 정리된 의존성

**WebSocketChatViewModel 의존성:**

```kotlin
@HiltViewModel
class WebSocketChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val chatServiceProvider: ChatServiceProvider,
    private val messageRepository: MessageRepository,
    private val roomDatabaseLogger: RoomDatabaseLogger,
    private val syncUseCase: SyncUseCase,
) : ViewModel()
```

**MessageService 의존성:**

```kotlin
class MessageService @Inject constructor(
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val messageRepository: MessageRepository,
    private val roomId: String,
    private val projectId: String? = null,
    private val channelType: String = "chat"
)
```

### 성능 개선

#### 1. 코드 복잡도 감소

- WebSocketChatViewModel: 1000+ 라인 → ~600 라인
- MessageService: 불필요한 기능 제거로 핵심 기능에 집중

#### 2. 메모리 사용량 최적화

- 불필요한 객체 생성 제거
- 중복된 이벤트 구독 제거
- 효율적인 의존성 주입

#### 3. 유지보수성 향상

- 명확한 역할 분담
- 단일 책임 원칙 준수
- 테스트 용이성 개선

### 테스트 시나리오

#### 1. 역할 분담 검증

- ViewModel: UI 상태 관리만 담당
- MessageService: 메시지 작업만 담당
- Core WebSocket: 연결 관리만 담당

#### 2. 중복 제거 검증

- 동일한 기능이 여러 곳에 구현되지 않음
- 의존성 순환 없음
- 불필요한 import 제거됨

#### 3. 성능 검증

- 메모리 사용량 감소
- 초기화 시간 단축
- 응답성 향상

## 초기 채팅 아이템 불러오는 방법

### 개요

채팅 시스템은 Paging3와 Anchor 기반 양방향 로딩을 사용하여 효율적인 메시지 로딩을 구현합니다.

### 아키텍처

#### 1. 데이터 흐름

```
Firestore (원격 데이터)
    ↓ (동기화)
Room DB (로컬 캐시)
    ↓ (Paging3)
UI (채팅 화면)
```

#### 2. 핵심 컴포넌트

**WebSocketChatViewModel**

- 초기화 로직 관리
- Anchor 설정 및 점프
- 동기화 상태 관리

**MessageService**

- Paging3 플로우 제공
- Anchor 기반 양방향 페이징
- 메시지 전송/수정/삭제

**BidirectionalPagingMediator**

- 특정 메시지를 기준으로 위/아래 양방향 로딩
- Anchor 주변 메시지 범위 로딩

### 초기화 과정

#### 1단계: Room DB 캐시 확인

```kotlin
private suspend fun checkRoomDBCache(): CacheStatus {
    val recentMessages = messageRepository.getRecentMessages(channelId, 1)
    // 캐시된 메시지 존재 여부 확인
}
```

#### 2단계: Firestore 동기화

```kotlin
private fun performInitialSync() {
    // 백그라운드에서 Firestore → Room DB 동기화
    syncUseCase.syncChannel(channelId)
}
```

#### 3단계: Anchor 설정

```kotlin
when {
    initialMessageId != null -> jumpToMessage(initialMessageId)
    cacheStatus.hasMessages -> jumpToLatest()
    else -> jumpToLatest()
}
```

#### 4단계: 사용자 경험 최적화

```kotlin
private fun optimizeUserExperience(cacheStatus: CacheStatus) {
    when {
        cacheStatus.hasMessages -> "캐시된 메시지를 불러왔습니다"
        else -> "메시지를 동기화하고 있습니다..."
    }
}
```

### 성능 최적화

#### 1. 캐시 우선 전략

- Room DB에 캐시된 메시지가 있으면 즉시 표시
- 백그라운드에서 Firestore 동기화 진행

#### 2. Anchor 기반 로딩

- 특정 메시지를 기준점으로 설정
- 위/아래 양방향으로 효율적 로딩

#### 3. 동기화 빈도 제한

```kotlin
private fun shouldThrottleSync(syncType: String): Boolean {
    // Anchor Jump 후 3초 지연
    // 일반 동기화 30초 간격
    // Anchor 작업 중 1분 간격
}
```

### 사용자 경험 개선

#### 1. 로딩 상태 표시

```kotlin
// ChatUiState에 isLoadingHistory 추가
val isLoadingHistory: Boolean = false

// ChatScreen에서 로딩 UI 표시
if (uiState.isLoadingHistory) {
    CircularProgressIndicator()
    Text("채팅 메시지를 불러오는 중...")
}
```

#### 2. 스마트 알림

- 캐시된 메시지: "캐시된 메시지를 불러왔습니다"
- 동기화 중: "메시지를 동기화하고 있습니다..."
- 동기화 완료: "메시지 동기화가 완료되었습니다"

### 테스트 시나리오

#### 1. 빈 채널 진입

- Room DB에 메시지 없음
- Firestore 동기화 진행
- 최신 메시지로 Anchor 설정

#### 2. 캐시된 채널 진입

- Room DB에 메시지 있음
- 즉시 캐시된 메시지 표시
- 백그라운드 동기화

#### 3. 특정 메시지로 진입

- initialMessageId가 설정됨
- 해당 메시지를 Anchor로 설정
- 주변 메시지 로딩

#### 4. 네트워크 오프라인

- Room DB 캐시만 사용
- 오프라인 상태 표시
- 재연결 시 동기화

### 로그 분석

#### 성공적인 초기화

```
🚀 === 초기 채팅 아이템 로딩 시작 ===
📊 Channel: channel123
📋 Room DB 캐시 상태: CacheStatus(hasMessages=true, messageCount=있음)
🎯 특정 메시지로 Anchor 설정
✅ 초기 채팅 아이템 로딩 완료
```

#### 캐시 없는 경우

```
🚀 === 초기 채팅 아이템 로딩 시작 ===
📋 Room DB 캐시 상태: CacheStatus(hasMessages=false, messageCount=없음)
🆕 빈 채널 - 최신으로 Anchor 설정
🔄 메시지 동기화 진행 중
✅ 초기 채팅 아이템 로딩 완료
```

### 문제 해결

#### 1. 메시지가 로딩되지 않는 경우

- Room DB 상태 확인
- Firestore 연결 상태 확인
- 동기화 로그 확인

#### 2. 성능 문제

- 동기화 빈도 제한 확인
- Paging3 설정 최적화
- 메모리 사용량 모니터링

#### 3. UI 반응성 문제

- Anchor Jump 진행 상태 확인
- Paging3 LoadState 모니터링
- 스크롤 위치 조정

### 향후 개선 사항

1. **프리페치 최적화**: 사용자 패턴 기반 메시지 프리페치
2. **압축 캐시**: 메시지 압축 저장으로 메모리 절약
3. **스마트 동기화**: 변경된 메시지만 동기화
4. **오프라인 지원**: 완전한 오프라인 모드 지원