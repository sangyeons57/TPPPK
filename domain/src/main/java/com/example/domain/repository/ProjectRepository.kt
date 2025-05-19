// 경로: domain/repository/ProjectRepository.kt (기존 파일에 함수 추가)
package com.example.domain.repository

import com.example.domain.model.Project
import com.example.domain.model.ProjectInfo
import kotlinx.coroutines.flow.Flow
import kotlin.Result
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure

interface ProjectRepository {
    // --- 기존 함수들 ---
    suspend fun getProjectListStream(): Flow<List<Project>>
    suspend fun fetchProjectList(): Result<Unit>
    suspend fun isProjectNameAvailable(name: String): Result<Boolean>
    suspend fun joinProjectWithCode(codeOrLink: String): Result<String>
    suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo>
    suspend fun joinProjectWithToken(token: String): Result<String>
    suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project>

    // --- AddScheduleViewModel 용 함수 추가 ---
    /** 일정 추가 시 선택 가능한 프로젝트 목록 가져오기 (간략 정보) */
    suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> // 또는 ProjectSelection 모델

    // --- Project Structure Management --- (Phase 2.1 of dm_project_chat_revamp.md)

    /**
     * 프로젝트의 전체 구조(카테고리, 직속 채널) 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    fun getProjectStructureStream(projectId: String): Flow<ProjectStructure>

    /**
     * 프로젝트 내에 새 카테고리를 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 카테고리 이름
     * @return 생성된 카테고리 정보
     */
    suspend fun createCategory(projectId: String, name: String): Result<Category>

    /**
     * 프로젝트의 특정 카테고리 내에 새 채널을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param name 채널 이름
     * @param type 채널 타입
     * @return 생성된 채널 정보
     */
    suspend fun createCategoryChannel(projectId: String, categoryId: String, name: String, type: ChannelMode, order: Int): Result<Channel>

    /**
     * 프로젝트 내에 새 직속 채널을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 채널 이름
     * @param type 채널 타입
     * @return 생성된 직속 채널 정보
     */
    suspend fun createDirectChannel(projectId: String, name: String, mode: ChannelMode, order: Int): Result<Channel>
}