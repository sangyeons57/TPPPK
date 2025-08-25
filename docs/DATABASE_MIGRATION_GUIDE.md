# 데이터베이스 마이그레이션 가이드

## 📋 개요

이 문서는 Room 데이터베이스 마이그레이션을 안전하고 체계적으로 관리하기 위한 가이드입니다.
자동화된 `DatabaseMigrations` 시스템을 통해 마이그레이션 누락을 방지하고 검증을 자동화합니다.

## 🏗️ 마이그레이션 시스템 구조

```
data/
├── data_datasource/
│   └── database/
│       └── migration/
│           ├── DatabaseMigrations.kt        # 중앙 관리 시스템
│           ├── Migration3To4.kt
│           ├── Migration6To7.kt
│           ├── Migration7To8.kt
│           ├── Migration8To9.kt
│           └── Migration9To10.kt
└── data/
    ├── di/DatabaseModule.kt                 # 자동 적용
    └── src/androidTest/
        └── migration/MigrationTest.kt       # 자동 테스트
```

## 🔄 새로운 마이그레이션 추가 절차

### 1단계: 엔티티 수정

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatar: String? = null // 새 컬럼 추가
)
```

### 2단계: 데이터베이스 버전 업데이트

```kotlin
@Database(
    entities = [...],
version = 11, // 버전 증가
exportSchema = false
)
abstract class AppDatabase : RoomDatabase()
```

### 3단계: 마이그레이션 파일 생성

파일명: `Migration10To11.kt`

```kotlin
package com.example.data_datasource.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migration: Version 10 → 11
 * - Add avatar column to users table
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE users ADD COLUMN avatar TEXT")
    }
}
```

### 4단계: DatabaseMigrations에 등록

```kotlin
object DatabaseMigrations {
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_3_4,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11  // 새 마이그레이션 추가
    )

    const val CURRENT_VERSION = 11  // 버전 업데이트
}
```

### 5단계: 테스트 케이스 추가

`MigrationTest.kt`에 새 테스트 추가:

```kotlin
@Test
@Throws(IOException::class)
fun migrate10To11() {
    helper.createDatabase(TEST_DB, 10).apply {
        execSQL("INSERT INTO users (id, name, email) VALUES ('user1', 'Test', 'test@test.com')")
        close()
    }

    val migration =
        DatabaseMigrations.ALL_MIGRATIONS.find { it.startVersion == 10 && it.endVersion == 11 }!!
    helper.runMigrationsAndValidate(TEST_DB, 11, true, migration).use { db ->
        val cursor = db.query("SELECT avatar FROM users WHERE id = 'user1'")
        cursor.use {
            assert(it.moveToFirst())
            val avatarIdx = it.getColumnIndex("avatar")
            assert(avatarIdx >= 0) { "avatar 컬럼이 추가되지 않았습니다" }
        }
    }
}
```

## ✅ 자동 검증 시스템

### 런타임 검증 (DEBUG 모드)

앱 시작 시 자동으로 마이그레이션 체인을 검증합니다:

```kotlin
// 개발 모드에서만 실행
if (BuildConfig.DEBUG) {
    DatabaseMigrations.validateMigrationChain()?.let { error ->
        throw IllegalStateException("마이그레이션 검증 실패: $error")
    }
}
```

### 단위 테스트 검증

```bash
./gradlew :data:data:testDebugUnitTest
```

### 통합 테스트 검증

```bash
./gradlew :data:data:connectedDebugAndroidTest
```

## 🚨 일반적인 실수와 해결책

### ❌ 실수 1: 마이그레이션 등록 누락

```kotlin
// DatabaseMigrations에 추가하지 않음
val MIGRATION_10_11 = object : Migration(10, 11) { ... }
```

**해결**: 반드시 `DatabaseMigrations.ALL_MIGRATIONS`에 추가

### ❌ 실수 2: 버전 불일치

```kotlin
// AppDatabase.version = 11
// DatabaseMigrations.CURRENT_VERSION = 10  // 불일치!
```

**해결**: 두 값이 항상 같도록 유지

### ❌ 실수 3: 마이그레이션 체인 누락

```kotlin
// 10->11은 있지만 9->10이 없는 경우
val ALL_MIGRATIONS = arrayOf(MIGRATION_8_9, MIGRATION_10_11)
```

**해결**: 자동 검증 시스템이 빌드 시 오류 발생

### ❌ 실수 4: SQL 구문 오류

```kotlin
database.execSQL("ALTER TABLE user ADD COLUMN avatar TEXT") // 잘못된 테이블명
```

**해결**: 마이그레이션 테스트에서 발견됨

## 📈 마이그레이션 성능 최적화

### 대용량 데이터 처리

```kotlin
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            // 배치 처리로 성능 향상
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages(timestamp)")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
```

### 점진적 마이그레이션

```kotlin
// 데이터 백업
database.execSQL("CREATE TABLE users_backup AS SELECT * FROM users")

// 스키마 변경
database.execSQL("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'active'")

// 데이터 복원 (필요시)
database.execSQL("DROP TABLE users_backup")
```

## 🐛 디버깅 도구

### 마이그레이션 경로 확인

```kotlin
println(DatabaseMigrations.printMigrationPaths())
```

### 개별 마이그레이션 테스트

```bash
./gradlew :data:data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.data.db.migration.MigrationTest#migrate10To11
```

### 데이터베이스 스키마 확인

```kotlin
// 개발 중 스키마 덤프
database.query("SELECT sql FROM sqlite_master WHERE type='table'")
```

## 📚 참고 자료

- [Room Migration 공식 문서](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Migration Testing 가이드](https://developer.android.com/training/data-storage/room/testing-db-migrations)
- [SQLite ALTER TABLE 문서](https://www.sqlite.org/lang_altertable.html)

## 🚀 베스트 프랙티스

1. **항상 백워드 호환성 유지**: 기존 데이터를 보존하세요
2. **테스트 우선 개발**: 마이그레이션 전에 테스트부터 작성하세요
3. **점진적 배포**: 단계별로 마이그레이션을 배포하세요
4. **모니터링**: 프로덕션에서 마이그레이션 성공률을 모니터링하세요
5. **롤백 계획**: 마이그레이션 실패 시 롤백 전략을 준비하세요

---
*이 가이드는 DatabaseMigrations 시스템 v1.0을 기준으로 작성되었습니다.*