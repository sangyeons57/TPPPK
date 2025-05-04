# Task: Room 데이터베이스 마이그레이션 모듈화

이 작업은 Room 데이터베이스 마이그레이션 로직을 모듈화하여 다른 기능과 분리하고, 체계적으로 관리할 수 있도록 하는 것을 목표로 합니다.

## 구현 단계

- [x] 1. AppDatabaseMigrations 클래스 생성
  - [x] 1.1: `data/src/main/java/com/example/data/db/migration` 디렉토리 생성
  - [x] 1.2: `AppDatabaseMigrations.kt` 파일 생성 및 기본 구조 구현
  - [x] 1.3: MIGRATION_6_7 구현 (InviteEntity에 inviterName, projectName 필드 추가)

- [x] 2. 스키마 내보내기 설정
  - [x] 2.1: AppDatabase 클래스의 `exportSchema` 옵션 변경
  - [x] 2.2: build.gradle 파일에 스키마 내보내기 위치 설정

- [x] 3. DatabaseModule 수정
  - [x] 3.1: provideAppDatabase 메서드 수정하여 마이그레이션 적용
  - [x] 3.2: 개발/프로덕션 환경에 따른 마이그레이션 전략 설정

- [x] 4. 마이그레이션 테스트 구현 (선택적)
  - [x] 4.1: MigrationTest 클래스 생성
  - [x] 4.2: migrate6To7 테스트 메서드 구현

- [x] 5. 문서화 및 코드 정리
  - [x] 5.1: 마이그레이션 시스템 사용법 주석 추가
  - [x] 5.2: README 또는 개발 문서 업데이트 