package com.example.data_repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data_datasource.remote.ProjectRemoteDataSource
import com.example.data_datasource.remote.special.FunctionsRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.ProjectRepository
import com.example.mapper.project.ProjectMapper
import javax.inject.Inject


class ProjectRepositoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    private val projectMapper: ProjectMapper,
) : DefaultRepositoryImpl(projectRemoteDataSource), ProjectRepository {


    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Project)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Project"))
        ensureCollection()
        return if (entity.isNew) {
            projectRemoteDataSource.create(projectMapper.domainToDto(entity))
        } else {
            projectRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

    override suspend fun uploadProfileImage(projectId: DocumentId, uri: Uri): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.uploadProjectProfileImage(projectId, uri)
    }

    override suspend fun removeProfileImage(projectId: DocumentId): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.removeProjectProfileImage(projectId)
    }

    // Invite-related operations removed; handled by ProjectInvitationRepository

    override suspend fun deleteProject(projectId: DocumentId): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.deleteProject(projectId.value)
    }

    override suspend fun leaveProject(projectId: DocumentId): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.leaveProject(projectId.value)
    }

    override suspend fun transferOwnership(projectId: DocumentId, newOwnerId: String): CustomResult<Unit, Exception> {
        return resultTry {
            // 프로젝트 소유자 필드만 업데이트 (멤버 역할은 UseCase에서 처리)
            val projectDoc = projectRemoteDataSource.findById(projectId)
            if (projectDoc == null) {
                throw Exception("프로젝트를 찾을 수 없습니다.")
            }
            
            val updateData = mapOf("ownerId" to newOwnerId)
            projectRemoteDataSource.update(projectId, updateData)
        }
    }

}
