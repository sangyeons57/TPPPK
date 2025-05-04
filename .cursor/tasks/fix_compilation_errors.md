# Task: Fix Kotlin Compilation Errors

이 파일은 Kotlin 컴파일 오류를 체계적으로 해결하기 위한 태스크 목록입니다.

## 1. Friend 관련 이슈 해결

- [x] 1.1: FriendRemoteDataSourceImpl 클래스 검사
  - [x] 1.1.1: 67번 라인 coroutine body 오류 확인 및 수정
  - [x] 1.1.2: Friend 모델 파라미터 불일치 오류 검사 (id, nickname 등)
  - [x] 1.1.3: Friend 생성자 파라미터 누락 문제 (userId, userName, status 등) 수정

- [x] 1.2: FriendRemoteDataSourceImpl의 expression body 함수 수정
  - [x] 1.2.1: 150, 158, 169번 라인 expression body 함수 블록 문법으로 변경
  - [x] 1.2.2: 250, 289번 라인 expression body 함수 블록 문법으로 변경

- [x] 1.3: FriendRepositoryImpl의 Friend 모델 불일치 수정
  - [x] 1.3.1: 85-87번 라인의 Friend 객체 생성 부분 수정

- [x] 1.4: FriendRemoteDataSourceImpl's suspension function errors
  - [x] 1.4.1: 63번 라인 suspension 함수 호출 문제 (coroutine body 내에서만 호출 가능) - 이미 수정됨 (launch 블록 내부에서 await 호출)

## 2. Invite 관련 이슈 해결

- [x] 2.1: InviteRemoteDataSourceImpl 클래스 검사
  - [x] 2.1.1: Collections, InviteFields, Status 등 누락된 import 추가
  - [x] 2.1.2: 누락된 필드 상수 검사 및 추가 필요 여부 확인

- [x] 2.2: InviteRemoteDataSourceImpl의 expression body 함수 수정
  - [x] 2.2.1: 64, 71, 108, 114, 120, 126번 라인 expression body 함수 블록 문법으로 변경
  - [x] 2.2.2: 145, 149, 162번 라인 expression body 함수 블록 문법으로 변경

- [x] 2.3: hashMapOf 관련 문제 해결
  - [x] 2.3.1: 83-87번 라인의 hashMapOf 사용 방식 오류 수정
  - [x] 2.3.2: 180-182번 라인의 hashMapOf 타입 추론 문제 수정

- [x] 2.4: 기타 참조 문제 해결
  - [x] 2.4.1: 169번 라인 limit 함수 참조 오류 수정
  - [x] 2.4.2: 170번 라인 await 호출 문제 수정
  - [x] 2.4.3: 172-173번 라인 not 연산자와 isEmpty 함수 호출 문제 수정
  - [x] 2.4.4: documents, id 등 참조 오류 수정
  - [x] 2.4.5: 243-244번 라인 LocalDateTime? 타입 불일치 문제 수정

- [x] 2.5: InviteRemoteDataSourceImpl 추가 이슈 해결
  - [x] 2.5.1: 255번 라인 'inviterName' 파라미터 없음 문제 해결 
  - [x] 2.5.2: 257번 라인 'projectName' 파라미터 없음 문제 해결
  - [x] 2.5.3: 258-259번 라인 LocalDateTime? 타입 불일치 문제 해결 

## 3. ProjectRole 관련 이슈 해결

- [x] 3.1: ProjectRoleRemoteDataSourceImpl 클래스 검사
  - [x] 3.1.1: 62, 114번 라인 projectId 파라미터 누락 문제 수정

- [x] 3.2: ProjectRoleRemoteDataSourceImpl의 expression body 함수 수정
  - [x] 3.2.1: 151, 164, 204, 211번 라인 expression body 함수 블록 문법으로 변경
  - [x] 3.2.2: 251, 263, 270, 303, 315, 322, 337번 라인 expression body 함수 블록 문법으로 변경

- [x] 3.3: ROLE_IDS 참조 문제 해결
  - [x] 3.3.1: 332번 라인 ROLE_IDS 참조 오류 수정
  - [x] 3.3.2: 333-334번 라인 파라미터 및 await 호출 문제 수정
  - [x] 3.3.3: 336번 라인 not 연산자와 isEmpty 함수 호출 문제 수정 

- [x] 3.4: ProjectRoleRemoteDataSourceImpl 추가 이슈 해결
  - [x] 3.4.1: 63번 라인 projectId 파라미터 누락 문제 수정
  - [x] 3.4.2: 115번 라인 projectId 파라미터 누락 문제 수정

## 4. Project 관련 이슈 해결

- [x] 4.1: ProjectRemoteDataSourceImpl 클래스 검사
  - [x] 4.1.1: 33번 라인 PARTICIPATING_PROJECT_IDS 참조 오류 수정
  - [x] 4.1.2: 34번 라인 name 파라미터 누락 문제 수정
  - [x] 4.1.3: 35-36번 라인 await 호출 및 documents 참조 문제 수정

## 5. 기타 이슈 해결

- [x] 5.1: ScheduleRepositoryImpl 수정
  - [x] 5.1.1: 39, 40, 60, 61번 라인 Timestamp 생성자 호출 문제 수정

- [x] 5.2: ProjectStructureRemoteDataSourceImpl 수정
  - [x] 5.2.1: 158번 라인 coroutine context 외부에서 suspension 함수 호출 문제 수정

- [x] 5.3: ProjectStructureRemoteDataSourceImpl 추가 이슈
  - [x] 5.3.1: 146번 라인 'kotlinx' 참조 오류 수정
  - [x] 5.3.2: 159번 라인 suspension 함수 호출 문제 수정

- [x] 5.4: DmRepositoryImpl 수정
  - [x] 5.4.1: 57번 라인 'id' 참조 오류 수정
  - [x] 5.4.2: createDmChannel 메서드에 override 키워드 추가
  - [x] 5.4.3: deleteDmChannel 메서드에 override 키워드 추가

- [x] 5.5: TypeConverters 중복 클래스 문제 해결
  - [x] 5.5.1: data/src/main/java/com/example/data/db/AppTypeConverters.kt 파일 삭제

## 6. 최종 확인 및 테스트

- [x] 6.1: 모든 수정사항 검토
- [x] 6.2: 빌드 테스트 실행 (2023-04-26 실행 결과: 추가 오류 발견)
- [x] 6.3: 추가 발견된 오류 수정 
  - [x] 6.3.1: FriendRemoteDataSourceImpl 63번 라인 - suspend 함수 호출 수정
  - [x] 6.3.2: InviteRemoteDataSourceImpl 255-259번 라인 - 파라미터 및 타입 불일치 수정
  - [x] 6.3.3: ProjectRoleRemoteDataSourceImpl 63, 115번 라인 - projectId 파라미터 전달
  - [x] 6.3.4: ProjectStructureRemoteDataSourceImpl 146, 159번 라인 - kotlinx 참조 및 suspend 함수 수정
  - [x] 6.3.5: DmRepositoryImpl 57번 라인 - 'id' 참조 오류 수정
- [x] 6.4: 최종 빌드 테스트 실행

## 7. 추가 작업 필요 사항

데이터 모듈의 Kotlin 컴파일 오류는 해결되었지만, 전체 프로젝트 빌드 시 몇 가지 추가 문제가 있습니다:

- [ ] 7.1: Hilt DI 관련 오류 (빌드 테스트 결과 발견)
  - [ ] 7.1.1: FriendRemoteDataSource, FriendLocalDataSource, ProjectMemberRemoteDataSource, ProjectMemberLocalDataSource에 대한 @Provides 주석 추가 필요
  - [ ] 7.1.2: data/di/DataSourceModule.kt에 필요한 Provider 메소드 추가

- [ ] 7.2: 네트워크 권한 관련 문제
  - [ ] 7.2.1: NetworkConnectivityMonitorImpl.kt에서 ACCESS_NETWORK_STATE 권한 확인 코드 추가 필요

- [ ] 7.3: 테스트 코드 오류 
  - [ ] 7.3.1: feature_settings 모듈의 테스트 코드 오류 (TestUri 클래스 구현 문제)

## 8. Hilt DI 에러 수정 계획

### 8.1: DataSourceModule 검사 및 생성

- [ ] 8.1.1: `data/di` 디렉토리에서 기존 DataSourceModule.kt 파일 확인
- [ ] 8.1.2: 없는 경우 새로운 DataSourceModule.kt 파일 생성
- [ ] 8.1.3: 적절한 어노테이션 (`@Module`, `@InstallIn(SingletonComponent::class)`) 추가

### 8.2: FriendDataSource Provider 메소드 추가

- [ ] 8.2.1: FriendRemoteDataSource에 대한 @Provides 메소드 추가
```kotlin
@Provides
@Singleton
fun provideFriendRemoteDataSource(
    firestore: FirebaseFirestore,
    auth: FirebaseAuth
): FriendRemoteDataSource {
    return FriendRemoteDataSourceImpl(firestore, auth)
}
```

- [ ] 8.2.2: FriendLocalDataSource에 대한 @Provides 메소드 추가
```kotlin
@Provides
@Singleton
fun provideFriendLocalDataSource(
    database: AppDatabase
): FriendLocalDataSource {
    return FriendLocalDataSourceImpl(database.friendDao())
}
```

### 8.3: ProjectMemberDataSource Provider 메소드 추가

- [ ] 8.3.1: ProjectMemberRemoteDataSource에 대한 @Provides 메소드 추가
```kotlin
@Provides
@Singleton
fun provideProjectMemberRemoteDataSource(
    firestore: FirebaseFirestore,
    auth: FirebaseAuth
): ProjectMemberRemoteDataSource {
    return ProjectMemberRemoteDataSourceImpl(firestore, auth)
}
```

- [ ] 8.3.2: ProjectMemberLocalDataSource에 대한 @Provides 메소드 추가
```kotlin
@Provides
@Singleton
fun provideProjectMemberLocalDataSource(
    database: AppDatabase
): ProjectMemberLocalDataSource {
    return ProjectMemberLocalDataSourceImpl(database.projectMemberDao())
}
```

### 8.4: 빌드 테스트 및 검증

- [ ] 8.4.1: 변경사항 확인
- [ ] 8.4.2: 테스트 빌드 실행

### 8.5: 네트워크 권한 관련 문제 해결 (추가 이슈)

- [ ] 8.5.1: NetworkConnectivityMonitorImpl.kt 파일에서 ACCESS_NETWORK_STATE 권한 확인 코드 추가
- [ ] 8.5.2: AndroidManifest.xml 파일에 필요한 권한 추가 확인
```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 8.6: 테스트 코드 오류 수정 (추가 이슈)

- [ ] 8.6.1: feature_settings 모듈의 테스트 코드 오류 (TestUri 클래스 구현 문제) 확인 및 수정 