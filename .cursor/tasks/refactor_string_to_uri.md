# Task: String URI를 Uri 타입으로 리팩토링

- [x] Step 1: `domain/model/MediaImage.kt`의 `contentPath` 타입을 `String`에서 `Uri`로 변경하고 관련 import 추가.
- [x] Step 2: `data/model/mapper/MediaImageMapper.kt`에서 `MediaImage`(`Uri`)와 `MediaImageEntity`(`String`) 간 변환 로직 수정 (`Uri.parse`, `Uri.toString` 사용).
- [x] Step 3: `data/datasource/local/chat/ChatLocalDataSourceImpl.kt`에서 갤러리 이미지 로드 시 `Uri.toString()` 호출 제거 및 Mapper 사용 확인.
- [x] Step 4: `domain/usecase/chat/SendMessageUseCase.kt`의 `invoke` 파라미터를 `imageUris: List<Uri>`로 변경.
- [x] Step 5: `domain/repository/ChatRepository.kt`의 `sendMessage` 메서드 파라미터를 `imageUris: List<Uri>`로 변경.
- [x] Step 6: `data/repository/ChatRepositoryImpl.kt`의 `sendMessage` 메서드를 `List<Uri>` 받도록 수정하고, 내부에서 Uri 처리 로직 구현 또는 호출 (e.g., 이미지 업로드 후 URL 목록 생성하여 DTO에 저장).
- [x] Step 7: `feature/feature_chat/src/main/java/com/example/feature_chat/viewmodel/ChatViewModel.kt`의 `onSendMessageClick`에서 `sendMessageUseCase` 호출 시 `List<Uri>`를 그대로 전달하도록 수정. 