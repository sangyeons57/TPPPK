package com.example.domain.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 제네릭 데이터 충돌 해결 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 충돌 해결 로직
 *
 * 충돌 해결 전략: "서버 우선" (Server Wins)
 * - 서버의 최신 데이터를 로컬에 덮어씀
 * - 추후 설정을 통해 다른 전략으로 변경 가능
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
class ResolveDataConflictUseCase<T> @Inject constructor() where T : AggregateRoot {
    suspend operator fun invoke(
        collectionPath: CollectionPath,
        entityId: String,
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<ConflictResolution<T>, Exception> {
        return try {
            remoteRepository.setCollection(collectionPath)

            // 1. 로컬과 서버에서 엔티티 조회
            val localResult = localRepository.getEntityById(entityId)
            val serverResult = remoteRepository.findById(DocumentId.from(entityId))

            val localEntity = when (localResult) {
                is CustomResult.Success -> localResult.data
                is CustomResult.Failure -> return CustomResult.Failure(localResult.error)
                else -> null
            }

            val serverEntity = when (serverResult) {
                is CustomResult.Success -> serverResult.data
                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> null
            }

            // 2. 충돌 상황 분석
            val conflictType = when {
                localEntity == null && serverEntity == null -> ConflictType.BOTH_DELETED
                localEntity == null && serverEntity != null -> ConflictType.LOCAL_DELETED_SERVER_EXISTS
                localEntity != null && serverEntity == null -> ConflictType.LOCAL_EXISTS_SERVER_DELETED
                localEntity != null && serverEntity != null -> {
                    val localUpdatedAt = getEntityUpdatedAt(localEntity)
                    val serverUpdatedAt = getEntityUpdatedAt(serverEntity)

                    when {
                        localUpdatedAt == null || serverUpdatedAt == null -> ConflictType.TIMESTAMP_MISSING
                        localUpdatedAt.isAfter(serverUpdatedAt) -> ConflictType.LOCAL_NEWER
                        serverUpdatedAt.isAfter(localUpdatedAt) -> ConflictType.SERVER_NEWER
                        else -> ConflictType.SAME_TIMESTAMP
                    }
                }

                else -> ConflictType.UNKNOWN
            }

            // 3. 충돌 해결 전략 적용 (서버 우선)
            val resolution = when (conflictType) {
                ConflictType.BOTH_DELETED -> {
                    // 양쪽 모두 삭제됨 - 추가 작업 불필요
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.NO_ACTION,
                        appliedEntity = null,
                        reason = "Both entities are deleted"
                    )
                }

                ConflictType.LOCAL_DELETED_SERVER_EXISTS -> {
                    // 로컬 삭제, 서버 존재 - 서버 데이터로 복원
                    localRepository.saveEntity(serverEntity!!)
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.RESTORE_FROM_SERVER,
                        appliedEntity = serverEntity,
                        reason = "Server entity restored to local"
                    )
                }

                ConflictType.LOCAL_EXISTS_SERVER_DELETED -> {
                    // 로컬 존재, 서버 삭제 - 서버 우선으로 로컬 삭제
                    localRepository.deleteEntity(entityId)
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.DELETE_LOCAL,
                        appliedEntity = null,
                        reason = "Server deletion applied to local"
                    )
                }

                ConflictType.SERVER_NEWER, ConflictType.SAME_TIMESTAMP -> {
                    // 서버가 더 최신이거나 타임스탬프 동일 - 서버 데이터 적용
                    localRepository.saveEntity(serverEntity!!)
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.UPDATE_FROM_SERVER,
                        appliedEntity = serverEntity,
                        reason = "Server data is newer or same timestamp"
                    )
                }

                ConflictType.LOCAL_NEWER -> {
                    // 로컬이 더 최신 - 서버 우선 전략이지만 예외적으로 로컬 유지
                    // 추후 설정에 따라 서버에 업로드하는 로직 추가 가능
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.KEEP_LOCAL,
                        appliedEntity = localEntity,
                        reason = "Local data is newer - kept local version"
                    )
                }

                ConflictType.TIMESTAMP_MISSING -> {
                    // 타임스탬프 누락 - 서버 우선으로 적용
                    localRepository.saveEntity(serverEntity!!)
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.UPDATE_FROM_SERVER,
                        appliedEntity = serverEntity,
                        reason = "Timestamp missing - applied server data"
                    )
                }

                else -> {
                    ConflictResolution<T>(
                        conflictType = conflictType,
                        action = ResolutionAction.NO_ACTION,
                        appliedEntity = null,
                        reason = "Unknown conflict type"
                    )
                }
            }

            CustomResult.Success(resolution)

        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }

    /**
     * 리플렉션을 사용하여 엔티티의 updatedAt 필드 접근
     */
    private fun getEntityUpdatedAt(entity: T): Instant? {
        return try {
            val field = entity::class.java.getDeclaredField("updatedAt")
            field.isAccessible = true
            field.get(entity) as? Instant
        } catch (e: Exception) {
            null
        }
    }

    enum class ConflictType {
        BOTH_DELETED,
        LOCAL_DELETED_SERVER_EXISTS,
        LOCAL_EXISTS_SERVER_DELETED,
        LOCAL_NEWER,
        SERVER_NEWER,
        SAME_TIMESTAMP,
        TIMESTAMP_MISSING,
        UNKNOWN
    }

    enum class ResolutionAction {
        NO_ACTION,
        RESTORE_FROM_SERVER,
        DELETE_LOCAL,
        UPDATE_FROM_SERVER,
        KEEP_LOCAL,
        UPLOAD_TO_SERVER
    }

    data class ConflictResolution<T>(
        val conflictType: ConflictType,
        val action: ResolutionAction,
        val appliedEntity: T?,
        val reason: String
    ) where T : AggregateRoot
}