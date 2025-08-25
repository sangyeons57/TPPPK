package com.example.data.db.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.db.AppDatabase
import com.example.data_datasource.database.migration.DatabaseMigrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room 데이터베이스 마이그레이션 테스트 클래스
 * 
 * 이 클래스는 데이터베이스 마이그레이션이 올바르게 작동하는지 검증하는 테스트를 포함합니다.
 * AndroidTest 환경에서 실행되어야 합니다.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB = "migration-test"
    
    // MigrationTestHelper는 테스트용 데이터베이스를 생성하고 마이그레이션을 실행하는 유틸리티입니다.
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )
    
    /**
     * 버전 6에서 7로의 마이그레이션 테스트
     * 
     * 이 테스트는 다음 과정을 수행합니다:
     * 1. 버전 6 데이터베이스를 생성하고 테스트 데이터 추가
     * 2. 마이그레이션 실행
     * 3. 마이그레이션 후 새 필드가 올바르게 추가되었는지 확인
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        // 버전 6 DB 생성
        helper.createDatabase(TEST_DB, 6).apply {
            // 테스트용 Invite 데이터 추가
            execSQL("""
                INSERT INTO invites (token, type, inviterId, projectId, expiresAt, createdAt, cachedAt) 
                VALUES ('test-token', 'project_invite', 'user1', 'project1', 1650000000000, 1640000000000, 1640000000000)
            """.trimIndent())
            close()
        }
        
        // 마이그레이션 실행
        helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            DatabaseMigrations.ALL_MIGRATIONS.find { it.startVersion == 6 && it.endVersion == 7 }!!
        ).use { db ->
            // 마이그레이션 검증 - 새 컬럼이 추가되었는지 확인
            val cursor = db.query("SELECT inviterName, projectName FROM invites WHERE token = 'test-token'")
            cursor.use {
                // 데이터가 존재하는지 확인
                assert(it.moveToFirst()) { "데이터가 존재하지 않습니다." }
                
                // 새 컬럼에 기본값이 올바르게 설정되었는지 확인
                val inviterNameIdx = it.getColumnIndex("inviterName")
                val projectNameIdx = it.getColumnIndex("projectName")
                
                assert(inviterNameIdx >= 0) { "inviterName 컬럼이 없습니다." }
                assert(projectNameIdx >= 0) { "projectName 컬럼이 없습니다." }
                
                val inviterName = it.getString(inviterNameIdx)
                val projectName = it.getString(projectNameIdx)
                
                assert(inviterName == "알 수 없음") { "inviterName 기본값이 올바르지 않습니다." }
                assert(projectName == "알 수 없음") { "projectName 기본값이 올바르지 않습니다." }
            }
        }
    }

    /**
     * 마이그레이션 체인 검증 테스트
     * DatabaseMigrations의 자동 검증 시스템을 테스트합니다.
     */
    @Test
    fun validateMigrationChain() {
        val validationError = DatabaseMigrations.validateMigrationChain()
        assert(validationError == null) { "마이그레이션 체인 검증 실패: $validationError" }
    }

    /**
     * 모든 마이그레이션이 등록되어 있는지 확인하는 테스트
     */
    @Test
    fun testAllMigrationsRegistered() {
        DatabaseMigrations.CURRENT_VERSION
        val allMigrations = DatabaseMigrations.ALL_MIGRATIONS

        // 현재 버전까지의 모든 주요 마이그레이션이 존재하는지 확인
        val expectedMigrations = listOf(
            "3->4", "6->7", "7->8", "8->9", "9->10"
        )

        val actualMigrations = allMigrations.map { "${it.startVersion}->${it.endVersion}" }

        expectedMigrations.forEach { expected ->
            assert(actualMigrations.contains(expected)) {
                "필수 마이그레이션 누락: $expected\n실제 등록된 마이그레이션: $actualMigrations"
            }
        }
    }

    /**
     * 최신 마이그레이션 테스트 (9->10)
     * tasks 테이블에 deletedAt 컬럼이 추가되는지 확인
     */
    @Test
    @Throws(IOException::class)
    fun migrate9To10() {
        // 버전 9 DB 생성 (tasks 테이블은 이미 존재)
        helper.createDatabase(TEST_DB, 9).apply {
            // 테스트용 Task 데이터 추가
            execSQL(
                """
                INSERT INTO tasks (id, channelId, taskType, status, content, `order`, checkedBy, checkedAt, createdAt, updatedAt) 
                VALUES ('task1', 'channel1', 'TODO', 'PENDING', 'Test task', 1, null, null, 1640000000000, 1640000000000)
            """.trimIndent()
            )
            close()
        }

        // 마이그레이션 실행 (9->10)
        val migration9To10 =
            DatabaseMigrations.ALL_MIGRATIONS.find { it.startVersion == 9 && it.endVersion == 10 }
        assert(migration9To10 != null) { "MIGRATION_9_10이 등록되지 않았습니다" }

        helper.runMigrationsAndValidate(TEST_DB, 10, true, migration9To10!!).use { db ->
            // deletedAt 컬럼이 추가되었는지 확인
            val cursor = db.query("SELECT id, deletedAt FROM tasks WHERE id = 'task1'")
            cursor.use {
                assert(it.moveToFirst()) { "테스트 데이터가 존재하지 않습니다" }

                val deletedAtIdx = it.getColumnIndex("deletedAt")
                assert(deletedAtIdx >= 0) { "deletedAt 컬럼이 추가되지 않았습니다" }

                // 기존 데이터의 deletedAt은 null이어야 함
                assert(it.isNull(deletedAtIdx)) { "기존 데이터의 deletedAt이 null이 아닙니다" }
            }
        }
    }

    /**
     * 전체 마이그레이션 경로 출력 테스트 (디버깅용)
     */
    @Test
    fun printMigrationPaths() {
        val paths = DatabaseMigrations.printMigrationPaths()
        println(paths)
        // 이 테스트는 정보 출력 목적이므로 항상 성공
        assert(true)
    }
} 