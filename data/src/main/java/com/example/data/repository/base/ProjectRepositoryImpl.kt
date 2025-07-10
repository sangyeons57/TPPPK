package com.example.data.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.ProjectRemoteDataSource
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSource // ProjectMember 관리를 위해 필요
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.model.remote.ProjectDTO
import com.example.core_common.constants.Constants
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import com.example.domain.repository.base.ProjectRepository

import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.InputStream
import javax.inject.Inject



class ProjectRepositoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: ProjectRepositoryFactoryContext,
) : DefaultRepositoryImpl(projectRemoteDataSource, factoryContext), ProjectRepository {


    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Project)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Project"))
        ensureCollection()
        return if (entity.isNew) {
            projectRemoteDataSource.create(entity.toDto())
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

    override suspend fun generateInviteLink(
        projectId: DocumentId,
        expiresInHours: Int,
        maxUses: Int?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.generateInviteLink(projectId.value, expiresInHours, maxUses)
    }

    override suspend fun validateInviteCode(inviteCode: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.validateInviteCode(inviteCode)
    }

    override suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.joinProjectWithInvite(inviteCode)
    }

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
