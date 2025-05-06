# feature_settings 모듈 UseCase 리팩토링 요약

## 개요

feature_settings 모듈의 ViewModel들이 Repository를 직접 참조하는 방식에서 UseCase를 통해 접근하도록 리팩토링을 완료했습니다. 이 변경은 Clean Architecture 원칙을 준수하기 위한 것으로, 관심사 분리와 테스트 용이성을 향상시킵니다.

## 구현된 UseCase

다음 UseCases를 새로 구현했습니다:

1. `UpdateNicknameUseCase`: 사용자 닉네임 업데이트 기능
2. `UpdateProfileImageUseCase`: 사용자 프로필 이미지 업데이트 기능
3. `RemoveProfileImageUseCase`: 사용자 프로필 이미지 제거 기능
4. `ChangePasswordUseCase`: 사용자 비밀번호 변경 기능

## 수정된 ViewModel

다음 ViewModel들이 Repository 직접 참조에서 UseCase 사용으로 리팩토링되었습니다:

1. `ChangeNameViewModel`:
   - `userRepository.updateNickname()` → `updateNicknameUseCase()`

2. `EditProfileViewModel`:
   - `userRepository.getUserProfile()` → `getUserProfileUseCase()`
   - `userRepository.updateProfileImage()` → `updateProfileImageUseCase()`
   - `userRepository.removeProfileImage()` → `removeProfileImageUseCase()`

3. `ChangePasswordViewModel`:
   - 임시 구현 코드 → `changePasswordUseCase()`

## 아키텍처 문서 업데이트

프로젝트 아키텍처 문서(`.cursor/rules/architecture.mdc`)를 다음과 같이 업데이트했습니다:

1. ViewModel에서 Repository 직접 참조 금지 규칙 명시
2. UseCase 설계 규칙 상세화
3. 데이터 흐름 및 예외 처리 전략 관련 내용 개선

## 다음 작업

1. 단위 테스트 수정 및 실행
2. 새 UseCase에 대한 단위 테스트 작성
3. 다른 Feature 모듈의 ViewModel에서도 동일한 방식으로 리팩토링 진행 