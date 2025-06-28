package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.ProjectRemoteDataSource
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSource // ProjectMember 관리를 위해 필요
import com.example.data.model.remote.ProjectDTO
import com.example.core_common.constants.Constants
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.ProjectRepository

import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.InputStream
import javax.inject.Inject



class ProjectRepositoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource, // ProjectStructure 관리용
    private val memberRemoteDataSource: MemberRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext, // 멤버 관리용
) : DefaultRepositoryImpl(projectRemoteDataSource, factoryContext.collectionPath), ProjectRepository {


    /**
     * 프로젝트 구조를 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 정보
     */
    override suspend fun getProjectStructureStream(projectId: String): Flow<CustomResult<List<Category>, Exception>> {
        return categoryRemoteDataSource.observeCategories(projectId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val categories = result.data.map { it.toDomain() }
                    CustomResult.Success(categories)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in getProjectStructureStream"))
                }
            }
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Project)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Project"))

        return if (entity.id.isAssigned()) {
            projectRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            projectRemoteDataSource.create(entity.toDto())
        }
    }

    override suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Project)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Project"))
        return projectRemoteDataSource.create(entity.toDto())
    }
}
