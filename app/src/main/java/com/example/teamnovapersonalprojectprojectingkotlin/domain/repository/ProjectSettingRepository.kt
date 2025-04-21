// 경로: domain/repository/ProjectSettingRepository.kt (신규 생성)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.ProjectStructure // 또는 Category, Channel 리스트
import kotlin.Result

interface ProjectSettingRepository {
    /** 프로젝트 구조(카테고리/채널 목록) 가져오기 */
    suspend fun getProjectStructure(projectId: String): Result<ProjectStructure> // 또는 Result<Pair<List<Category>, List<Channel>>>
    /** 카테고리 삭제 (내부 채널 처리 방식 필요) */
    suspend fun deleteCategory(projectId: String, categoryId: String): Result<Unit>
    /** 채널 삭제 */
    suspend fun deleteChannel(projectId: String, channelId: String): Result<Unit>
    /** 프로젝트 이름 변경 */
    suspend fun renameProject(projectId: String, newName: String): Result<Unit>
    /** 프로젝트 삭제 */
    suspend fun deleteProject(projectId: String): Result<Unit>
}