package com.example.data_datasource.database.migration

/**
 * 데이터베이스 마이그레이션 중앙 관리 클래스
 *
 * 이 클래스는 모든 Room 마이그레이션을 중앙 집중적으로 관리하여
 * 마이그레이션 누락을 방지하고 자동 검증을 제공합니다.
 *
 * 새로운 마이그레이션 추가 시:
 * 1. Migration{From}To{To}.kt 파일 생성
 * 2. 해당 마이그레이션을 이 클래스의 ALL_MIGRATIONS 배열에 추가
 * 3. MigrationTest에 테스트 케이스 추가
 */
object DatabaseMigrations {

    /**
     * 모든 데이터베이스 마이그레이션의 완전한 목록
     *
     * ⚠️ 중요: 새로운 마이그레이션 생성 시 반드시 이 배열에 추가해야 합니다.
     *
     * 순서는 from 버전 순으로 정렬되어야 합니다.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_3_4,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
        // 새로운 마이그레이션은 여기에 추가하세요
    )

    /**
     * 현재 데이터베이스 버전
     * AppDatabase의 version과 일치해야 합니다.
     */
    const val CURRENT_VERSION = 10

    /**
     * 마이그레이션 체인의 유효성을 검증합니다.
     *
     * @return 검증 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    fun validateMigrationChain(): String? {
        val migrations = ALL_MIGRATIONS.sortedBy { it.startVersion }

        // 중복된 마이그레이션 경로 확인
        val duplicates = migrations
            .groupBy { "${it.startVersion}-${it.endVersion}" }
            .filter { it.value.size > 1 }

        if (duplicates.isNotEmpty()) {
            return "중복된 마이그레이션 경로: ${duplicates.keys.joinToString()}"
        }

        // 마이그레이션 체인의 연속성 확인
        val versions =
            migrations.flatMap { listOf(it.startVersion, it.endVersion) }.distinct().sorted()
        val gaps = mutableListOf<String>()

        for (i in 0 until versions.size - 1) {
            val current = versions[i]
            val next = versions[i + 1]

            val hasPath = migrations.any {
                it.startVersion == current && it.endVersion == next
            }

            if (!hasPath && next - current == 1) {
                gaps.add("$current -> $next")
            }
        }

        if (gaps.isNotEmpty()) {
            return "누락된 마이그레이션: ${gaps.joinToString()}"
        }

        return null // 검증 성공
    }

    /**
     * 현재 버전까지의 모든 마이그레이션 경로를 출력합니다.
     * 디버깅 및 문서화 목적으로 사용됩니다.
     */
    fun printMigrationPaths(): String {
        return buildString {
            appendLine("=== 데이터베이스 마이그레이션 경로 ===")
            appendLine("현재 버전: $CURRENT_VERSION")
            appendLine()

            ALL_MIGRATIONS.sortedBy { it.startVersion }.forEach { migration ->
                appendLine("${migration.startVersion} → ${migration.endVersion}")
            }

            appendLine()
            val validationResult = validateMigrationChain()
            if (validationResult == null) {
                appendLine("✅ 마이그레이션 체인 검증 성공")
            } else {
                appendLine("❌ 마이그레이션 체인 검증 실패: $validationResult")
            }
        }
    }
}