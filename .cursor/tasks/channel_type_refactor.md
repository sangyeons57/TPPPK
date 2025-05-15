# Task: Channel Type 및 Channel Mode 리팩토링

- [x] `FirestoreConstants.kt` 업데이트: `ChannelFields.TYPE`을 `ChannelFields.CHANNEL_TYPE`으로 변경하고, `ChannelFields.CHANNEL_MODE` 추가. `ChannelTypeValues` 및 `ChannelModeValues`를 스키마에 맞게 업데이트합니다.
- [x] `Channel` 데이터 모델 업데이트: `type` 필드의 타입을 `ChannelType` 열거형으로 변경하고 `channelMode` 필드 (String, nullable)를 추가합니다. (`domain/src/main/java/com/example/domain/model/Channel.kt`)
- [x] `ChannelMapper.kt` 업데이트: `Channel` 모델 변경 사항을 매퍼 함수에 반영합니다. (`data/src/main/java/com/example/data/model/mapper/ChannelMapper.kt`)
- [ ] `ChannelRepositoryImpl.kt` 업데이트: Firestore 필드명 변경 및 모델 변경 사항을 레포지토리 구현에 반영합니다. (`data/src/main/java/com/example/data/repository/ChannelRepositoryImpl.kt`)
- [ ] `ProjectStructureRemoteDataSourceImpl.kt` 업데이트: Firestore 필드명 변경 및 모델 변경 사항을 데이터 소스 구현에 반영합니다. (`data/src/main/java/com/example/data/datasource/remote/projectstructure/ProjectStructureRemoteDataSourceImpl.kt`)
- [ ] `ChannelRemoteDataSourceImpl.kt` 업데이트: Firestore 필드명 변경 및 모델 변경 사항을 데이터 소스 구현에 반영합니다. (`data/src/main/java/com/example/data/datasource/remote/channel/ChannelRemoteDataSourceImpl.kt`)
- [ ] `feature_project` 모듈의 UI 코드 수정:
    - `ProjectSettingScreen.kt`: `Channel.type` 대신 `Channel.channelMode` 사용 및 `FirestoreConstants.ChannelModeValues` 참조로 변경합니다.
    - `ChannelListItem.kt`: `Channel.type`의 `when` 구문을 `ChannelType` 열거형에 맞게 수정하고, 아이콘 표시 로직에서 `Channel.channelMode`를 사용합니다.
    - `CreateChannelScreen.kt`: `Channel.channelMode` 및 `ChannelModeValues`를 사용하도록 수정합니다.
    - `EditChannelScreen.kt`: `Channel.channelMode` 및 `ChannelModeValues`를 사용하도록 수정합니다.
    - `ExpandableCategoryItem.kt`: `Channel.channelMode` 및 `ChannelModeValues`를 사용하도록 수정합니다.
- [ ] `feature_project` 모듈의 ViewModel 코드 수정:
    - `CreateChannelViewModel.kt`: `channelMode`를 사용하도록 수정합니다.
    - `EditChannelViewModel.kt`: `channelMode`를 사용하도록 수정합니다.
- [ ] Channel 메타데이터 접근 유틸리티 구현: `Channel` 모델 내에 확장 함수 또는 헬퍼 클래스를 추가하여 `metadata` 맵에 안전하게 접근하고 값을 가져오거나 설정하는 기능을 제공합니다.
- [ ] Channel 메타데이터 접근 유틸리티 적용: 변경된 `Channel` 모델과 유틸리티를 사용하는 모든 코드(Repository, DataSource, ViewModel 등)를 업데이트합니다. 