# Task: Fix Compilation Errors in Data Layer

- [ ] Step 1: **Fix Core DTOs, Enums/Constants, and `CustomResult` Usage**
- [x] Address unresolved references for DTOs like `PermissionDTO` (in `PermissionRemoteDataSource.kt`)
- [x] Address unresolved references for DTOs like `Channel`, `ChatMessage` (in `ChannelMigrationTool.kt`).
- [x] Define or import constants like `Members` (in `MemberRemoteDataSourceImpl.kt`)
  - [x] Define or import constants like `Messages`, `collection` (in `MessageRemoteDataSourceImpl.kt`). Check `core_common` or define in the `data` module if specific.
  - [x] Define or import constants like `CHANNELS`, `MESSAGES` (in `ChannelLocator.kt`).
  - [x] Ensure `CustomResult` (from `core_common`) is consistently used. Investigate and fix `CustomResult<T, Exception>` vs `CustomResult<T, Throwable>` discrepancies (e.g., `MemberRemoteDataSourceImpl.kt:95:16`).

- [ ] Step 2: **Fix Interface Implementations (Signatures First)**
- [x] `MemberRemoteDataSourceImpl.kt:19:1`: Ensure `addMember` signature matches `MemberRemoteDataSource` interface.
- [x] `ProjectRemoteDataSourceImpl.kt:21:1`: Ensure `createProject` and `updateProjectDetails` signatures match `ProjectRemoteDataSource` interface.
- [x] `DMChannelRepositoryImpl.kt:11:1`: Ensure `getDmChannelById` signature matches `DMChannelRepository` interface.
- [x] `MessageAttachmentRepositoryImpl.kt:13:1`: Ensure `uploadAttachment` signature matches `MessageAttachmentRepository` interface.

- [x] Step 3: **Address `AuthRepositoryImpl.kt` Return Issues**
- [x] `AuthRepositoryImpl.kt:264:25`, `AuthRepositoryImpl.kt:268:25`: Fix `'return' is prohibited here`. (Should be fixed by refactoring `updateUserName`)
- [x] `AuthRepositoryImpl.kt:264:32`, `AuthRepositoryImpl.kt:268:32`: Fix `Return type mismatch`. (Should be fixed by refactoring `updateUserName`)
- [x] `AuthRepositoryImpl.kt:274:5`: Fix `Missing return statement`. (Should be fixed by refactoring `updateUserName`)

- [ ] Step 4: **Resolve `toDomain` / `toDto` and Mapper Issues**
- [x] `MemberRepositoryImpl.kt:6:31`, `MessageAttachmentRepositoryImpl.kt:6:31`: Ensure `mapper` instances are correctly injected or available.
- [ ] Globally check for unresolved `toDomain` and `toDto` methods (e.g., `CategoryRepositoryImpl.kt:59:17`, `DMWrapperRepositoryImpl.kt:19:34`, `MemberRepositoryImpl.kt:23:34`, `MessageAttachmentRepositoryImpl.kt:34:18`) and ensure they are correctly imported/defined.
- [x] `UserRepositoryImpl.kt`: Fix `toDomain` usage in `getUserStream` and `searchUsersByName`, addressing type inference and return type mismatches.

- [ ] Step 5: **Fix `MemberRemoteDataSourceImpl.kt` & `MessageRemoteDataSourceImpl.kt` Internals**
- `MemberRemoteDataSourceImpl.kt`: (All sub-items implicitly complete as the file has no errors in the new log)
  - [x] `71:30`: Infer type for parameter.
  - [x] `72:17`: Fix argument type mismatch for `mapOf`.
  - [x] `72:48`: Resolve `Members` (likely addressed in Step 1).
  - [x] `72:65`: Infer type for parameter.
- `MessageRemoteDataSourceImpl.kt`: (All sub-items addressed by previous fixes and current code structure)
  - [x] `29:61`, `29:105`: Resolve `collection`, `Messages` (Resolved: Uses `FirestoreConstants`).
  - [x] `43:9`: Fix argument type mismatch for `CustomResult<MessageDTO?, Exception>` (Resolved: `getMessage` fixed).
  - [x] `64:77`, `80:37`, `92:39`, `114:77`, `135:63`: Infer types for parameters (Resolved: Types are clear in current code).
  - [x] `65:20`: Resolve `id` (Resolved: `docRef.id` used correctly).
  - [x] `132:18`: Fix `Task.await()` call (Resolved: `await()` used correctly).
  - [x] `134:17`, `134:27`: Fix `!isEmpty()` usage (Resolved: `!snapshot.isEmpty` used correctly).
  - [x] `135:26`, `135:44`: Resolve `documents`, `reference` (Resolved: `snapshot.documents...reference` used correctly).

- [x] Step 6: **Fix `ProjectRemoteDataSourceImpl.kt` Override Issues**
    - [x] `45:5`, `60:5`: Correct `createProject` and `updateProjectDetails` `override` modifiers and ensure method signatures match the interface (Resolved: Verified current implementation is correct).

- [x] Step 7: **Fix `CategoryRepositoryImpl.kt` Issues**
- [x] `36:67`: Fixed argument type mismatch by extracting id, name, and order from Category.
- [x] `58:16`: Fixed return type mismatch for `getCategory` using proper handling of CustomResult.
- [x] `58:41`: Resolved `getCategory` with proper error handling.
- [x] `58:76`, `58:90`, `97:36`, `97:50`: Fixed type inference issues.
- [x] `81:41`: Replaced `updateCategoriesOrder` with direct category updates.
- [x] `96:67`: Replaced `getCategories` with `observeCategories`.
- [x] `99:26`: Changed `CustomResult.Error` to `CustomResult.Failure`.

- [x] Step 8: **Fix `DMWrapperRepositoryImpl.kt` Issues**
- [x] `17:42`, `25:42`: Fixed by implementing proper methods using `observeDmWrappers` with proper error handling.
- [x] `17:70`, `17:76`, `18:20`, `18:34`, `19:25`, `19:29`, `25:74`, `25:80`, `26:20`, `26:34`: Infer types and fix type mismatches.

- [x] Step 9: **Fix `FriendRepositoryImpl.kt` Issues**
- [x] `31:54`: Fix `Too many arguments for 'observeFriends'`.
- [x] `33:20`, `50:20`, `69:20`, `79:20`: Fix `One type argument expected` for `CustomResult.Success`/`Failure`.
- [x] `34:21`, `35:51`, `35:57`, `45:38`, `70:21`, `71:52`, `71:58`, `74:38`: Infer types.
- [x] `35:46`, `37:49`, `38:50`, `39:61`, `40:70`, `41:51`, `42:50`, `51:49`, `71:47`, `72:57`, `80:49`: Resolve DTO properties (`data`, `id`, `name`, `profileImageUrl`, `status`, `requestedAt`, `acceptedAt`, `error`).
- [x] `67:39`: Resolve `observeFriendRequests`.
- [x] `99:89`, `202:81`: Fix `No parameter with name 'limit'`.
- [x] `121:57`: Resolve `sendFriendRequest`.
- [x] `145:49`, `166:49`: Resolve `updateFriendStatus`.
- [x] `187:36`: Resolve `removeFriend`.
- [x] `201:16`: Fix return type mismatch `CustomResult<List<User>, Exception>` vs `CustomResult<List<UserDTO>, Exception>`.

- [x] Step 10: **Fix `MemberRepositoryImpl.kt` Issues**
- [x] `20:62`, `20:79`, `28:77`, `28:94`, `36:78`, `46:123`, `51:94`, `56:94`: Correct return type mismatches and type arguments for `Flow<CustomResult<...>>`.
- [x] `21:39`, `29:39`: Resolve `getProjectMembersStream`, `getProjectMemberStream`.
- [x] `21:74`, `21:80`, `22:20`, `22:34`, `23:25`, `23:29`, `29:81`, `29:87`, `30:20`, `30:34`: Infer types.
- [x] `48:39`: Resolve `addMemberToProject`.
- [x] `53:39`: Resolve `updateProjectMember`.
- [x] `57:39`: Resolve `removeProjectMember`.

- [x] Step 11: **Fix `MessageAttachmentRepositoryImpl.kt` Issues**
- [x] `8:38`, `25:86`: Resolve `MessageAttachmentType` (fixed import path to use `domain.model.enum` instead of `_new.enum`).
- [x] `19:83`, `25:122`, `38:78`: Correct type arguments for `CustomResult` (added proper Unit error type).
- [x] `20:50`: Resolve `getAttachmentsForMessage` (implemented using Flow from RemoteDataSource).
- [x] `20:86`, `20:100`, `33:93`, `33:107`: Infer types (explicitly specified types in lambdas).
- [x] `21:27`: Resolve `it` (fixed lambda variable references).
- [x] `25:5`: Correct `uploadAttachment` override (implemented with proper signature and logic).
- [x] `33:50`: Resolve `uploadAttachment` (implemented using addAttachment from data source).
- [x] `39:50`: Resolve `deleteAttachment` (implemented using removeAttachment from data source).

- [x] Step 12: **Fix `MessageRepositoryImpl.kt` Issues**
- [x] `27:40`: Resolve `getMessagesStream` (implemented using observeMessages).
- [x] `30:75`, `30:89`, `31:75`, `31:89`, `50:75`, `50:89`: Infer types or fix `CustomResult` (all CustomResult types fixed).
- [x] `33:43`: Resolve `domainMessages` reference (proper mapping from DTOs to domain models).
- [x] `43:75`, `43:89`, `44:85`, `44:99`: Infer types or fix `CustomResult` (explicitly specified types).
- [x] `44:15`: Resolve method reference to `messageRemoteDataSource.getPastMessages` (implemented using observeMessages with filtering).
- [x] `48:43`: Resolve `domainMessages` reference (proper mapping implemented). map/flatMap.
- [x] `31:53`, `83:86`: Resolve `data`.
- [x] `45:58`: Resolve `getPastMessages`.
- [x] `72:82`: Resolve `uploadAttachment` (check if this is `MessageAttachmentRepository.uploadAttachment` or a local one).
- [x] `87:33` to `90:33`: Fix `No parameter with name...` when constructing `MessageAttachment`.

- [x] Step 13: **Final Sweep for Type Inference and Minor Unresolved References**
- [x] Go through any remaining "Cannot infer type for this parameter" errors and provide explicit types.
- [x] Fix any remaining compiler errors in the repository implementation classes.
  - Fixed ScheduleRepositoryImpl's getSchedulesForMonth method to use CustomResult.Failure instead of CustomResult.Error
  - Fixed incorrect remoteDataSource reference to scheduleRemoteDataSource
  - Updated ReactionRepositoryImpl to use CustomResult.Failure pattern instead of throwing exceptions
- [x] Run final build after all issues are addressed.
