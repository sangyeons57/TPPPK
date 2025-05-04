# Room 데이터베이스 마이그레이션 가이드

이 문서는 프로젝트에서 Room 데이터베이스 마이그레이션을 관리하는 방법을 설명합니다.

## 마이그레이션 개요

데이터베이스 마이그레이션은 앱 업데이트 시 기존 사용자 데이터를 유지하면서 데이터베이스 스키마를 변경하는 프로세스입니다. 마이그레이션 없이 스키마를 변경하면 앱이 충돌하거나 사용자 데이터가 손실될 수 있습니다.

## 마이그레이션 모듈 구조

프로젝트의 마이그레이션 시스템은 다음과 같이 구성되어 있습니다:

1. **AppDatabaseMigrations 클래스**
   - 위치: `com.example.data.db.migration.AppDatabaseMigrations`
   - 역할: 모든 마이그레이션 정의를 중앙 집중화하여 관리

2. **DatabaseModule 구성**
   - 위치: `com.example.data.di.DatabaseModule`
   - 역할: 마이그레이션 적용 및 설정 관리

3. **테스트 프레임워크**
   - 위치: `com.example.data.db.migration.MigrationTest` (androidTest 소스 세트)
   - 역할: 마이그레이션이 올바르게 작동하는지 검증

## 새 마이그레이션 추가 방법

1. **버전 번호 증가**
   - `AppDatabase` 클래스의 `version` 값을 증가시킵니다.
   - 변경 사항을 주석으로 문서화합니다.

2. **마이그레이션 정의**
   - `AppDatabaseMigrations` 클래스에 새 마이그레이션 객체를 추가합니다:
   ```kotlin
   val MIGRATION_X_Y = object : Migration(X, Y) {
       override fun migrate(database: SupportSQLiteDatabase) {
           // 필요한 SQL 명령 작성
       }
   }
   ```
   - `ALL_MIGRATIONS` 배열에 새 마이그레이션을 추가합니다.

3. **테스트 작성**
   - `MigrationTest` 클래스에 새 마이그레이션을 검증하는 테스트 메서드를 추가합니다.
   - 테스트는 이전 버전 DB를 생성하고, 마이그레이션을 적용한 후, 결과를 검증해야 합니다.

## 마이그레이션 작성 팁

1. **ALTER TABLE 주의사항**
   - SQLite는 ALTER TABLE 명령어로 할 수 있는 작업이 제한적입니다.
   - 복잡한 변경(예: 컬럼 제거, 컬럼 이름 변경)은 임시 테이블 생성 및 데이터 이동이 필요합니다.

2. **기본값 설정**
   - 새 NOT NULL 컬럼을 추가할 때는 반드시 DEFAULT 값을 지정해야 합니다.

3. **트랜잭션 사용**
   - 여러 단계가 필요한 복잡한 마이그레이션은 트랜잭션으로 감싸는 것이 좋습니다.

## 개발 vs. 프로덕션 설정

현재 구성에서는:
- 개발 환경: 마이그레이션 실패 시 fallback 전략(데이터 삭제 후 재생성)
- 프로덕션 환경: 마이그레이션 적용 (BuildConfig.DEBUG를 사용하여 구분 가능) 