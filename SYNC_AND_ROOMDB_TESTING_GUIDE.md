# 동기화 & Room DB 검증 가이드

## 🎯 개요

이 가이드는 현재 동기화 시스템과 Room DB에 데이터가 실제로 어떻게 저장되는지 확인하는 방법을 설명합니다.

## 🛠️ 검증 도구

### 1. DevMenuScreen의 Room DB 검사 기능

개발자 메뉴(`DevMenuScreen`)에 다음 기능들이 추가되었습니다:

#### 📊 DB 검사 기능들

- **전체 DB 검사**: 모든 테이블의 상태와 데이터 개수 확인
- **메시지 테이블**: messages 테이블의 스키마와 샘플 데이터 확인
- **OutBox 테이블**: 동기화 대기열 상태 확인
- **동기화 메타데이터**: 커서 정보와 마지막 동기화 시간 확인
- **채널 메시지 검사**: 특정 채널의 메시지 상세 상태 확인
- **동기화 + 비교**: 동기화 실행 전후 DB 상태 비교

#### 🎮 사용 방법

1. **앱 실행** → **개발 메뉴** 이동
2. **Room DB 검사** 섹션에서 원하는 기능 선택
3. 채널별 검사 시 채널 ID 입력 (예: `temp_dm_channel_123`)
4. **로그 확인**: Android Studio Logcat에서 상세 결과 확인

## 🔍 테스트 시나리오

### 시나리오 1: 메시지 송신 후 Room DB 저장 확인

```
1. 개발 메뉴에서 "전체 DB 검사" 실행 (초기 상태)
2. 채팅 화면 진입 → 메시지 송신
3. 개발 메뉴에서 "채널 메시지 검사" 실행
4. 로그에서 새 메시지가 Room DB에 저장되었는지 확인
```

**확인 포인트:**

- 메시지가 `messages` 테이블에 저장됨
- `outBoxStatus`가 `PENDING`으로 설정됨
- OutBox에 동기화 대기 레코드 생성됨

### 시나리오 2: 동기화 실행 전후 상태 비교

```
1. 채널 ID 입력 (예: temp_dm_channel_123)
2. "동기화 + 비교" 버튼 클릭
3. 로그에서 다음 내용 확인:
   - === DB STATE BEFORE SYNC ===
   - 동기화 실행 과정
   - === DB STATE AFTER SYNC ===
```

**확인 포인트:**

- 동기화 전: 로컬 메시지만 존재
- 동기화 후: Firestore에서 새로운 메시지 추가됨
- 커서 정보 업데이트됨

### 시나리오 3: WebSocket 실시간 메시지 수신 확인

```
1. 개발 메뉴에서 WebSocket 연결
2. 다른 클라이언트에서 메시지 발송
3. "채널 메시지 검사"로 실시간 수신 메시지 확인
```

**확인 포인트:**

- WebSocket으로 수신된 메시지가 즉시 Room DB에 저장됨
- Paging3가 자동으로 UI 업데이트 트리거함

## 📋 로그 해석 가이드

### Room DB 상태 로그 예시

```
📊 ROOM DATABASE STATE REPORT
================================================================================

📋 TABLE: messages
📝 Description: 채팅 메시지
📊 Record Count: 15
📄 Sample Data:
   1. {id="abc123...", channelId="temp_dm_channel_123", content="Hello World", outBoxStatus="SYNCED"}
   2. {id="def456...", channelId="temp_dm_channel_123", content="How are you?", outBoxStatus="PENDING"}
```

### 동기화 상태 로그 예시

```
🔍 === SYNC WITH DB COMPARISON START ===
📋 Channel: temp_dm_channel_123

📊 === DB STATE BEFORE SYNC ===
💬 === CHANNEL MESSAGES: temp_dm_channel_123 ===
📊 Total messages in channel: 3
📈 Messages by sync status:
   SYNCED: 2 messages
   PENDING: 1 messages

📥 OutBox 상태: 1개 레코드 대기중

🔄 동기화 실행 중...
✅ Sync completed successfully

📊 === DB STATE AFTER SYNC ===  
💬 === CHANNEL MESSAGES: temp_dm_channel_123 ===
📊 Total messages in channel: 5  (← 2개 메시지 추가됨)
📈 Messages by sync status:
   SYNCED: 5 messages
   PENDING: 0 messages

📥 OutBox 상태: 0개 레코드 (← 모두 처리됨)
```

## 🎯 핵심 확인 사항

### ✅ 정상 동작 확인 체크리스트

1. **메시지 저장**
    - [ ] 송신 메시지가 Room DB에 즉시 저장됨
   - [ ] `outBoxStatus`가 적절히 설정됨 (PENDING → SYNCED)
    - [ ] 채널 ID가 올바르게 저장됨

2. **동기화 동작**
    - [ ] SyncUseCase가 정상 실행됨
    - [ ] Firestore에서 새 메시지 가져옴
    - [ ] 커서 정보가 업데이트됨
    - [ ] OutBox 레코드가 처리됨

3. **Paging3 연동**
    - [ ] Room DB 변경 시 UI가 자동 업데이트됨
    - [ ] 페이징이 올바르게 동작함
    - [ ] 메시지 순서가 시간순으로 정렬됨

### ❌ 문제 상황 식별

- **메시지가 저장되지 않음**: Repository 또는 Mapper 문제
- **동기화가 실행되지 않음**: UseCase 또는 Port 설정 문제
- **UI가 업데이트되지 않음**: Paging3 연결 문제
- **커서가 진행되지 않음**: SyncCursorStore 문제

## 🚀 추가 디버깅 팁

### Android Studio Logcat 필터링

```
tag:RoomDB OR tag:DevMenuViewModel-DB OR tag:DevMenuViewModel-Sync OR tag:PagingSource OR tag:Paging3
```

### 실시간 모니터링

```
tag:ChatDebug OR tag:ViewModel OR tag:MessageSyncPort
```

### 성능 모니터링

```
tag:Performance OR tag:Paging3-UI
```

## 📞 문제 해결

문제 발생 시:

1. **로그 확인**: 위 필터를 사용하여 상세 로그 확인
2. **DB 상태 검사**: DevMenu의 전체 DB 검사 실행
3. **동기화 재실행**: "동기화 + 비교" 기능으로 강제 동기화
4. **캐시 클리어**: 개발 메뉴의 "로컬 채팅 캐시 삭제 + 동기화" 실행

---

이 가이드를 통해 동기화 시스템이 올바르게 작동하고 Room DB에 데이터가 적절히 저장되는지 확인할 수 있습니다.