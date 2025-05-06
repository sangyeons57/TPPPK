// 경로: domain/repository/ProjectSettingRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.ProjectCategory
import kotlin.Result

/**
 * 프로젝트 설정 관련 데이터 작업을 위한 레포지토리 인터페이스 (도메인 계층)
 */
interface ProjectSettingRepository {
    /**
     * 특정 프로젝트의 이름과 구조(카테고리 및 채널 목록)를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return Result 객체. 성공 시 Pair(프로젝트 이름, 카테고리 목록) 반환.
     */
    suspend fun getProjectStructure(projectId: String): Result<Pair<String, List<ProjectCategory>>>

    /**
     * 카테고리를 삭제합니다.
     * @param categoryId 삭제할 카테고리 ID
     * @return Result 객체. 성공 시 Unit 반환.
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>

    /**
     * 채널을 삭제합니다.
     * @param channelId 삭제할 채널 ID
     * @return Result 객체. 성공 시 Unit 반환.
     */
    suspend fun deleteChannel(channelId: String): Result<Unit>

    /**
     * 프로젝트 이름을 변경합니다.
     * @param projectId 대상 프로젝트 ID
     * @param newName 새로운 프로젝트 이름
     * @return Result 객체. 성공 시 Unit 반환.
     */
    suspend fun renameProject(projectId: String, newName: String): Result<Unit>

    /**
     * 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트 ID
     * @return Result 객체. 성공 시 Unit 반환.
     */
    suspend fun deleteProject(projectId: String): Result<Unit>
    
    // 필요에 따라 카테고리/채널 생성, 수정 관련 함수 추가 가능
}