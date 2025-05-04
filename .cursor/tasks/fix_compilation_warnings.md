# Task: Kotlin 컴파일 경고 수정

이 작업은 프로젝트 빌드 시 발생하는 Kotlin 컴파일 경고들을 해결하는 것을 목표로 합니다.

## 경고 목록 및 수정 계획

- [x] 1. `SentryNavigationTracker.kt`
  - [x] 1.1: `navBackStackEntry.arguments?.get(p0)`의 deprecated 경고 수정 (안전한 접근 방법 사용)

- [x] 2. `DmRemoteDataSourceImpl.kt`
  - [x] 2.1: 99번 라인 Unchecked cast 경고 수정 (타입 안전하게 캐스팅)
  - [x] 2.2: 209번 라인 Unchecked cast 경고 수정 (타입 안전하게 캐스팅)

- [x] 3. `InviteRemoteDataSourceImpl.kt`
  - [x] 3.1: 244번 라인 Condition is always 'true' 경고 수정 (불필요한 조건 제거 또는 로직 수정)

- [x] 4. `ProjectMemberRemoteDataSourceImpl.kt`
  - [x] 4.1: 56번 라인 Unchecked cast 경고 수정 (타입 안전하게 캐스팅)
  - [x] 4.2: 119번 라인 Unchecked cast 경고 수정 (타입 안전하게 캐스팅)

- [x] 5. `DatabaseModule.kt`
  - [x] 5.1: `fallbackToDestructiveMigration()` deprecated 경고 수정 (파라미터가 있는 오버로드 버전 사용)

- [x] 6. `ChannelListItem.kt`
  - [x] 6.1: `Icons.Filled.VolumeUp` deprecated 경고 수정 (`Icons.AutoMirrored.Filled.VolumeUp` 사용) 