# Task: UseCase 컴파일 오류 해결

- [x] Step 1: `AuthRepository` 인터페이스 확인 (`isUserAuthenticated`, `isEmailVerified` 메서드 존재 여부 확인 및 필요한 경우 추가).
- [x] Step 2: `ProjectMemberRepository` 인터페이스 확인 (`deleteMember` 메서드 존재 여부 확인 및 필요한 경우 추가).
- [x] Step 3: `GetProjectMemberDetailsUseCase` 반환 타입 오류 수정 (`ProjectMemberRepository.getProjectMember` 반환 타입 확인 및 null 처리 추가).
- [x] Step 4: `GetProjectRolesUseCase`의 `ProjectRole` 참조 오류 수정 (import 문 확인 및 추가).
- [x] Step 5: `GetUserProfileUseCase`의 `userRepository.getUserProfile()` 호출 오류 수정 (파라미터 불일치 확인 및 Repository 인터페이스 시그니처 확인/수정).
- [x] Step 6: `UpdateUserStatusUseCase.kt`의 중복 선언 오류 수정 (클래스/인터페이스 구조 정리).
- [x] Step 7: UseCase 구현체들에서 임시 `delay` 호출 제거. 