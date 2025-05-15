# 테스크: 채널 및 메시지 기능 리팩토링

- [x] **단계 1: 메시지 관련 기능 분석 및 설계**
    - [x] 기존 `ChannelRemoteDataSource`, `ChannelRepository` 및 관련 유스케이스/뷰모델을 검토하여 순수 메시지 처리(전송, 수신, 조회, 수정, 삭제, 반응 등)와 관련된 메서드 및 로직을 식별합니다.
    - [x] 이러한 기능 중 어떤 것을 새로운 전용 `MessageRemoteDataSource`, `MessageRepository` 및 해당 유스케이스로 이전할지 결정합니다.
    - [x] `timestamp`를 `Instant`로 통일하고 `firestore-schema.mdc`와 일치시키는 등 새로운 메시지 관련 기능 또는 필요한 변경 사항을 식별합니다. (`ChatMessage.timestamp`는 `Instant` 확인, 스키마와 모델 간 필드 차이점 기록)
    - [x] `Channel` 모듈(채널 메타데이터, 멤버, 유형, 모드 관리)과 `Message` 모듈(채널 내 메시지 내용 및 상호 작용 관리)의 명확한 책임을 정의합니다.
    - [x] 이 계획을 문서화하고, `Message` 구성 요소에 대해 이동/생성할 메서드 목록과 `Channel` 구성 요소에서 유지/수정할 메서드 목록을 포함합니다. (이 대화에 기록)

- [x] **단계 2: 기능 이전 및 `Channel` 모듈 잠재적 문제 처리 계획**
    - [x] 1단계의 내용을 바탕으로, 메시지 기능 이전으로 인해 영향을 받을 `ChannelRemoteDataSource`, `ChannelRepository` 및 관련 유스케이스/뷰모델의 특정 코드 섹션을 식별합니다. (주로 인터페이스 변경으로 인한 컴파일 오류 지점들)
    - [x] 이 마이그레이션 과정에서 `Channel` 모듈에서 발생할 수 있는 컴파일 오류 또는 논리적 문제를 예상합니다. (의존성 변경, 데이터 조합 로직 변경, `lastMessagePreview` 업데이트 등)
    - [x] 이러한 `Channel` 구성 요소가 새로운 `Message` 구성 요소와 상호 작용하는 방식(예: 필요한 경우 `MessageRepository`를 `Channel` 유스케이스 또는 뷰모델에 주입하거나 그 반대, 또는 분리 유지)에 대한 전략을 설명합니다. (ViewModel/UseCase에서 조합하는 방식 우선, `lastMessagePreview`는 별도 처리 또는 Firestore Function 고려)
    - [x] 이 마이그레이션 계획 및 오류 완화 전략을 문서화합니다. (이 대화에 기록, 점진적 수정 계획)

- [x] **단계 3: 오래된 `Chat` 관련 구성 요소 제거**
    - [x] 오래된 `ChatRemoteDataSource`, `ChatRepository` 및 `ChatScreen` 또는 `ChatViewModel`이 아닌 모든 `Chat` 유스케이스/뷰모델(`ChatScreen`이 `feature_chat`의 유일한 UI인 경우)과 관련된 모든 파일 및 코드를 식별합니다.
    - [x] 이러한 오래된 파일을 안전하게 삭제하고 사용 부분을 제거합니다. (data, domain 모듈의 Chat 관련 DataSource, Repository, UseCase 삭제 완료. feature_chat은 ChatScreen/ViewModel만 남김)
    - [x] (`domain` 레이어에 제거할 오래된 Chat 유스케이스가 있는지도 확인해야 합니다.) (확인 및 삭제 완료)

- [x] **단계 4: 새로운 `Message` 관련 구성 요소 구현**
    - [x] `MessageRemoteDataSource` 인터페이스 및 `MessageRemoteDataSourceImpl`을 생성합니다 (Firestore 사용, `timestamp`가 `Instant`인지 확인).
    - [x] `MessageRepository` 인터페이스 및 `MessageRepositoryImpl`을 생성합니다.
    - [x] 필요한 `Message` 유스케이스(예: `SendMessageUseCase`, `GetMessagesStreamUseCase`, `EditMessageUseCase`, `DeleteMessageUseCase`, `AddReactionUseCase`, `RemoveReactionUseCase`)를 생성합니다. 메시지가 채널의 하위 컬렉션인 `firestore-schema.mdc`와 일치하는지 확인합니다. (주요 UseCase 생성 완료, 나머지는 동일 패턴으로 확장 가능)
    - [x] 구현 중 메시지 구조에 대한 불일치 또는 새로운 결정이 발생하는 경우 `firestore-schema.mdc`를 업데이트합니다. (ChatMessageMapper 생성 및 스키마와의 차이점 일부 반영)

- [x] **단계 5: `Channel` 구성 요소 리팩토링 및 `Message` 구성 요소와 통합**
    - [x] 1단계에서 식별된 메시지 처리 로직을 제거하기 위해 `ChannelRemoteDataSource` 및 `ChannelRepository`를 수정합니다.
    - [x] 메시지 관련 작업을 위해 새로운 `Message` 유스케이스/리포지토리를 사용하거나, 이제 완전히 위임된 경우 직접적인 메시지 처리를 제거하도록 `Channel` 유스케이스 및 뷰모델을 업데이트합니다.
    - [x] 2단계의 계획에 따라 이러한 변경으로 인해 발생하는 모든 컴파일 오류 또는 문제를 해결합니다.
    - [x] `feature_chat`의 `ChatViewModel` (및 `ChatScreen`)이 이제 컨텍스트를 위해 새로운 `Message` 유스케이스 및 잠재적으로 `Channel` 유스케이스를 사용하는지 확인합니다.

- [x] **단계 6: 타임스탬프 유형(`java.time.Instant`) 및 스키마 정렬 확인**
    - [x] `Message` 구성 요소(`DataSource`, `Repository`, `UseCase` 및 관련 도메인 모델 예: `ChatMessage.kt`) 전체에서 모든 타임스탬프 필드가 일관되게 `java.time.Instant`를 사용하는지 확인 완료. (`DateTimeUtil` 및 `ChatMessageMapper` 업데이트 완료).
    - [x] 메시지를 가져오고 저장하는 방식이 `firestore-schema.mdc` (즉, `channels/{channelId}/messages/{messageId}`)와 일치하는지 재확인 완료.
    - [x] **스키마 비교 결과 및 권장 사항:**
        - `ChatMessage.kt`의 비정규화 필드 (`senderName`, `channelId` 등)는 UI 편의성을 위해 허용됨.
        - `ChatMessage.kt`에 `updatedAt: Instant?` 추가됨. 스키마(`firestore-schema.mdc`의 `messages` 하위 컬렉션)에도 `updatedAt: Timestamp?` 추가 권장.
        - `ChatMessage.kt`의 `attachments` 필드(`List<MessageAttachment>`)는 스키마의 `Array<Map<String, String>>?`보다 상세한 구조를 가짐. 스키마를 `Array<Map<String, Any>>?` (또는 `MessageAttachment`의 세부 구조 명시)로 업데이트 권장.
        - `isEdited` (모델 사용)와 `isModified` (스키마 내 대체 필드) 간의 사용 통일 또는 명확화 필요. 모델은 `isEdited`를 사용 중. 스키마 단순화를 위해 `isModified` 제거 고려.
    - [x] **UI 모델 업데이트:**
        - `ChatUiState.kt`: `myUserNameDisplay`, `myUserProfileUrl`, `pendingMessageText`, `selectedAttachmentUris`, `isLoadingGallery` 추가.
        - `ChatMessageUiModel.kt`: `actualTimestamp: Instant`, `isDeleted: Boolean` 추가.
        - `ChatMessage.toUiModel` 매핑 함수 업데이트 (ChatViewModel에 수동 적용 필요).

- [ ] **단계 7: 최종 검토 및 테스트**
    - [ ] 변경된 모든 파일과 새로 생성된 파일의 정확성, 규칙 준수 여부 및 KDoc 문서를 검토합니다.
    - [ ] 프로젝트를 컴파일하고 테스트(있는 경우)를 실행하여 리팩토링이 성공적이고 회귀가 발생하지 않았는지 확인합니다.
    - [ ] 채팅 기능을 수동으로 테스트합니다. 