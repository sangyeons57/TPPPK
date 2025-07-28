package com.example.domain.usecase.sync.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalProjectRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.model.vo.CollectionPath
import java.time.Instant
import javax.inject.Inject

/**
 * 프로젝트 데이터의 서버-로컬 간 충돌을 해결하는 UseCase
 * 충돌 감지 및 해결 전략을 적용합니다.
 */
class ResolveProjectDataConflictUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<Project>,
    private val localRepository: LocalProjectRepository
) {
    
    suspend operator fun invoke(
        projectId: DocumentId,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.SERVER_WINS
    ): CustomResult<ConflictResolution, Exception> {
        return try {
            // 1. 로컬과 서버에서 데이터 조회
            val localResult = localRepository.getEntityById(projectId.value)
            val localProject = when (localResult) {
                is CustomResult.Success -> localResult.data
                is CustomResult.Failure -> null
                else -> null
            }
            
            remoteRepository.setCollection(CollectionPath.from("projects"))
            val remoteResult = remoteRepository.findById(projectId)
            val remoteProject = when (remoteResult) {
                is CustomResult.Success -> remoteResult.data
                is CustomResult.Failure -> null
                else -> null
            }
            
            // 2. 충돌 상황 분석
            val conflictType = when {
                localProject == null && remoteProject == null -> {
                    return CustomResult.Success(ConflictResolution.NoConflict("Both versions are null"))
                }
                localProject == null -> ConflictType.LOCAL_MISSING
                remoteProject == null -> ConflictType.REMOTE_MISSING
                localProject.updatedAt == remoteProject.updatedAt -> {
                    return CustomResult.Success(ConflictResolution.NoConflict("Versions are identical"))
                }
                localProject.updatedAt.isAfter(remoteProject.updatedAt) -> ConflictType.LOCAL_NEWER
                else -> ConflictType.REMOTE_NEWER
            }
            
            // 3. 해결 전략 적용
            val resolution = when (strategy) {
                ConflictResolutionStrategy.SERVER_WINS -> resolveServerWins(localProject, remoteProject, conflictType)
                ConflictResolutionStrategy.CLIENT_WINS -> resolveClientWins(localProject, remoteProject, conflictType)
                ConflictResolutionStrategy.LATEST_TIMESTAMP -> resolveLatestTimestamp(localProject, remoteProject, conflictType)
                ConflictResolutionStrategy.MANUAL -> ConflictResolution.RequiresManualResolution(
                    conflictType = conflictType,
                    localData = localProject,
                    remoteData = remoteProject
                )
            }
            
            CustomResult.Success(resolution)
            
        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }
    
    private suspend fun resolveServerWins(
        localProject: Project?,
        remoteProject: Project?,
        conflictType: ConflictType
    ): ConflictResolution {
        return when (conflictType) {
            ConflictType.LOCAL_MISSING -> ConflictResolution.NoConflict("Local missing, no action needed")
            ConflictType.REMOTE_MISSING -> {
                // 서버 우선이므로 로컬 데이터 삭제
                localProject?.let { localRepository.deleteEntity(it.id.value) }
                ConflictResolution.Resolved("Local data deleted to match server")
            }
            ConflictType.LOCAL_NEWER, ConflictType.REMOTE_NEWER -> {
                // 서버 우선이므로 서버 데이터로 로컬 업데이트
                remoteProject?.let { localRepository.saveEntity(it) }
                ConflictResolution.Resolved("Local data updated with server version")
            }
        }
    }
    
    private suspend fun resolveClientWins(
        localProject: Project?,
        remoteProject: Project?,
        conflictType: ConflictType
    ): ConflictResolution {
        return when (conflictType) {
            ConflictType.LOCAL_MISSING -> ConflictResolution.NoConflict("Local missing, no action needed")
            ConflictType.REMOTE_MISSING -> ConflictResolution.NoConflict("Remote missing, local data preserved")
            ConflictType.LOCAL_NEWER, ConflictType.REMOTE_NEWER -> {
                // 클라이언트 우선이므로 로컬 데이터를 서버에 업로드 (Outbox 사용)
                localProject?.let { 
                    // Outbox에 추가하여 나중에 서버로 동기화
                    localRepository.addToOutbox(it.id.value, "UPDATE", null)
                }
                ConflictResolution.Resolved("Local data queued for server update")
            }
        }
    }
    
    private suspend fun resolveLatestTimestamp(
        localProject: Project?,
        remoteProject: Project?,
        conflictType: ConflictType
    ): ConflictResolution {
        return when (conflictType) {
            ConflictType.LOCAL_MISSING -> ConflictResolution.NoConflict("Local missing, no action needed")
            ConflictType.REMOTE_MISSING -> ConflictResolution.NoConflict("Remote missing, local data preserved")
            ConflictType.LOCAL_NEWER -> {
                // 로컬이 더 최신이므로 서버에 업로드
                localProject?.let { 
                    localRepository.addToOutbox(it.id.value, "UPDATE", null)
                }
                ConflictResolution.Resolved("Newer local data queued for server update")
            }
            ConflictType.REMOTE_NEWER -> {
                // 서버가 더 최신이므로 로컬 업데이트
                remoteProject?.let { localRepository.saveEntity(it) }
                ConflictResolution.Resolved("Local data updated with newer server version")
            }
        }
    }
    
    enum class ConflictResolutionStrategy {
        SERVER_WINS,
        CLIENT_WINS,
        LATEST_TIMESTAMP,
        MANUAL
    }
    
    enum class ConflictType {
        LOCAL_MISSING,
        REMOTE_MISSING,
        LOCAL_NEWER,
        REMOTE_NEWER
    }
    
    sealed class ConflictResolution {
        data class NoConflict(val reason: String) : ConflictResolution()
        data class Resolved(val action: String) : ConflictResolution()
        data class RequiresManualResolution(
            val conflictType: ConflictType,
            val localData: Project?,
            val remoteData: Project?
        ) : ConflictResolution()
    }
}