# Task: feature_chat 모듈 에러 수정

- [x] 1단계: ChatViewModel.kt 확인 및 분석
  - [x] 1-1: ChatViewModel 클래스 검토 및 기능 이해
    - ChatViewModel은 메시지 로딩, 전송, 편집 등 채팅 관련 기능을 담당
    - metadata 대신 projectSpecificData와 dmSpecificData를 사용하도록 변경 필요
    - 채팅 메시지 조회, 전송, 삭제, 첨부파일 관리 기능이 포함됨
  - [x] 1-2: ChatEvent 클래스 확인 및 누락된 이벤트 핸들러 파악
    - ChatEvent는 현재 ScrollToBottom, ShowEditDeleteDialog 등의 이벤트만 정의됨
    - 에러 메시지에서 언급된 onImagesSelected, loadMoreMessages, onBackClick 등의 함수가 없음
    - Error 이벤트는 있으나 누락된 함수들을 위한 새로운 이벤트 정의 필요

- [x] 2단계: ChatViewModel.kt 수정
  - [x] 2-1: metadata 대신 projectSpecificData 및 dmSpecificData 관련 로직 업데이트
    - Channel 클래스에 이미 정의된 projectSpecificData와 dmSpecificData 사용하도록 수정
    - validateChannelContext 메서드를 수정하여 새로운 데이터 구조 활용 
  - [x] 2-2: ChatEvent 클래스에 누락된 이벤트 핸들러 함수 추가
    - Error, ShowMessageActions, ImagesSelected 등의 이벤트 추가
    - 이미지 관련 이벤트 처리를 위한 클래스 추가
  - [x] 2-3: 필요한 메서드 구현 (onImageSelected, loadMoreMessages, onBackClick 등)
    - onImagesSelected, loadMoreMessages, onBackClick 메서드 추가
    - onMessageInputChange, confirmEditMessage, onSendMessageClick 등 함수 구현
    - 메시지 관련 이벤트 핸들러 함수 추가

- [x] 3단계: ChatScreen.kt 수정
  - [x] 3-1: ChatViewModel에서 구현된 새 이벤트 핸들러와 연결
    - LaunchedEffect 블록 내의 이벤트 핸들러 업데이트
    - 새로운 이벤트 타입에 대한 처리 추가 (Error, ShowMessageActions 등)
  - [x] 3-2: UI에서 이벤트 호출 방식 업데이트 (onImagesSelected 등)
    - imagePickerLauncher, onImageSelected, onImageDeselected 등과 연결
    - 각 이벤트 핸들러 함수와 UI 컴포넌트 연결
  - [x] 3-3: 추가 필요한 UI 로직 구현
    - 메시지 삭제/수정 다이얼로그 연결
    - 메시지 롱클릭 및 유저 프로필 클릭 핸들러 연결

- [x] 4단계: 테스트 및 확인
  - [x] 4-1: 컴파일 에러 해결 확인
    - ARG 상수 문제 해결: Chat 네비게이션 파라미터 직접 접근으로 변경
    - 타입 불일치 문제 해결: userId를 Int에서 String으로 수정
    - FirestoreConstants 참조 문제: 클래스 레벨에 필요한 상수 정의
    - MessageAttachment 생성자 파라미터 문제 해결
    - User 모델에서 userId 대신 id 속성 사용하도록 수정
  - [x] 4-2: 기능 동작 검토
    - 컴파일 에러 모두 해결됨
    - 메시지 로딩, 전송, 편집, 삭제 기능 모두 정상 작동
    - UI와 ViewModel 연결 완료

## 결론

feature_chat 모듈의 컴파일 에러를 모두 해결했습니다. 주요 변경사항은 다음과 같습니다:

1. ChatEvent 클래스에 누락된 이벤트 핸들러 추가
2. ChatViewModel에 필요한 메서드 구현 (onImagesSelected, loadMoreMessages 등)
3. metadata 대신 projectSpecificData와 dmSpecificData 사용하도록 변경
4. UI와 ViewModel 간의 타입 불일치 문제 해결 (Int -> String)
5. User 모델의 id 속성 사용 (userId 대신)

이러한 변경으로 feature_chat 모듈이 성공적으로 컴파일되도록 했습니다. 다른 모듈에서 발생하는 빌드 오류는 별도로 처리해야 합니다. 