package com.example.data.repository

import com.example.domain.model.Project
import com.example.domain.model.ProjectInfo
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.Result

class ProjectRepositoryImpl @Inject constructor(
    // TODO: ProjectApiService, ProjectDao 등 주입
) : ProjectRepository {

    override fun getProjectListStream(): Flow<List<Project>> {
        println("ProjectRepositoryImpl: getProjectListStream called (returning empty flow)")
        return flowOf(emptyList())
    }

    override suspend fun fetchProjectList(): Result<Unit> {
        println("ProjectRepositoryImpl: fetchProjectList called (returning success)")
        return Result.success(Unit)
    }

    override suspend fun isProjectNameAvailable(name: String): Result<Boolean> {
        println("ProjectRepositoryImpl: isProjectNameAvailable called for '$name' (returning true)")
        return Result.success(true) // 임시로 항상 가능
    }

    override suspend fun joinProjectWithCode(codeOrLink: String): Result<String> {
        println("ProjectRepositoryImpl: joinProjectWithCode called with '$codeOrLink' (returning failure)")
        return Result.failure(NotImplementedError("구현 필요")) // 임시로 실패
        // return Result.success("joined_project_id") // 성공 시 프로젝트 ID 반환 가정
    }

    override suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo> {
        println("ProjectRepositoryImpl: getProjectInfoFromToken called with '$token' (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
        // return Result.success(ProjectInfo("임시 프로젝트", 10)) // 임시 성공 데이터
    }

    override suspend fun joinProjectWithToken(token: String): Result<String> {
        println("ProjectRepositoryImpl: joinProjectWithToken called with '$token' (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
        // return Result.success("joined_project_id") // 성공 시 프로젝트 ID 반환 가정
    }

    override suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project> {
        println("ProjectRepositoryImpl: createProject called with name '$name' (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
        // 임시 성공 데이터 예시:
        // val newProject = Project(id = "new_proj_${Random.nextInt()}", name = name, description = description, isPublic = isPublic, ownerId = "me")
        // return Result.success(newProject)
    }

    override suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> {
        println("ProjectRepositoryImpl: getAvailableProjectsForScheduling called  (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
    }
}