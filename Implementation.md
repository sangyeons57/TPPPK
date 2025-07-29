# 구현 하려는 것

https://developer.android.com/topic/architecture?hl=ko#common-principles
Single source of truth 라는 이키택쳐를 구현하려고해

항상 Local DB로 부터 데이터를 얻어사 작동하는 방식이지
에를 들어서 체팅을 전송한다고 생각하면

1. 로컬DB에 저장
2. Room 에 Observe 기능을 이용해 추가된 내용 UI 에 표시

웹소켓 서버

1. websocket으로 전송
2. firestore에 저장
3. ACK 반환
4. BroadCast 호출

ACK 획득

1. 로컬 DB에 전송 완료로 상태를 변경하고 시간 확정
2. Room에 Observe기능을 이용해 변경되 상태 UI에 표시

BroadCast 획득

1. 로컬 DB에 받을 데이터 저장
2. Room에서 Observe기능을 이용해 UI에 표시

Message말고

- update
- delete
  등에도 동일한 방식으로 동작

-------------------------------------------

# 로컬 DB

- Channels table : 사용자가 접근했던 체널들에 대한 데이터
- Chats table: 모든 채팅 체널에 존제하는 메시지 데이터
- Users table: 사용자 정보

# 동기화

Channels table에는 해당 체널의 마지막 update시간을 기록하여
동기화 할떄 하당 시간을 기준으로 새로 추가된 내용들을 동기화 시키면 된다.
이 동기화는  [# 데이터 싱크] 부분에서 볼 수있듯이

- 첫 입장시
- 과거 데이터 로드시
- 현제 화면 기준으로 미래 데이터 로드시 (너무 과거 내용으로 가면 메모리를 위해 제거하기떄문)
- 웹소켓을 통해 전송된 데이터 외 모든 데이터

등의 경우에 호출하면 된다.

Users는 자체적으로 lastUpdateAt이나 updateAt 을 firestore와 주기적으로 비교해서 업데이트 하면된다.

----------------------------------

# 데이터 싱크

- 첫 입장시
- 과거 데이터 로드시
- 현제 화면 기준으로 미래 데이터 로드시 (너무 과거 내용으로 가면 메모리를 위해 제거하기떄문)
- 웹소켓을 통해 전송된 데이터 외 모든 데이터

- websocket을 이용해 자동적으로