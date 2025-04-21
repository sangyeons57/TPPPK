// 경로: domain/repository/ProjectRepository.kt (기존 파일에 함수 추가)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.Project
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.ProjectInfo
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface ProjectRepository {
    // --- 기존 함수들 ---
    fun getProjectListStream(): Flow<List<Project>>
    suspend fun fetchProjectList(): Result<Unit>
    suspend fun isProjectNameAvailable(name: String): Result<Boolean>
    suspend fun joinProjectWithCode(codeOrLink: String): Result<String>
    suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo>
    suspend fun joinProjectWithToken(token: String): Result<String>
    suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project>

    // --- AddScheduleViewModel 용 함수 추가 ---
    /** 일정 추가 시 선택 가능한 프로젝트 목록 가져오기 (간략 정보) */
    suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> // 또는 ProjectSelection 모델
}