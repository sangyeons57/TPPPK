package com.example.domain.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.data_core.dao.OutboxDao
import com.example.data_model.local.OutboxEntity
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
import javax.inject.Inject

/**
 * 제네릭 로컬 변경사항 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 범용 동기화 로직
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
class SyncPendingLocalChangesToServerUseCase<T> @Inject constructor(
    private val outboxDao: OutboxDao
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<SyncResult, Exception> {
        return try {
            val collectionName = localRepository.collectionName

            // 1. 대기 중인 Outbox 항목들 조회
            val pendingEntries = outboxDao.getPendingOperationsByCollection(collectionName)

            if (pendingEntries.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList()
                    )
                )
            }

            // 2. 서버 Repository 설정
            remoteRepository.setCollection(CollectionPath.from(collectionName))

            // 3. 각 항목을 서버에 동기화
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            val processedEntries = mutableListOf<OutboxEntity>()

            pendingEntries.forEach { outboxEntry ->
                try {
                    val syncResult = when (outboxEntry.operation) {
                        "CREATE" -> handleCreate(outboxEntry, remoteRepository, localRepository)
                        "UPDATE" -> handleUpdate(outboxEntry, remoteRepository, localRepository)
                        "DELETE" -> handleDelete(outboxEntry, remoteRepository)
                        else -> CustomResult.Failure(Exception("Unknown operation: ${outboxEntry.operation}"))
                    }

                    when (syncResult) {
                        is CustomResult.Success -> {
                            successCount++
                            processedEntries.add(outboxEntry)
                        }

                        is CustomResult.Failure -> {
                            errorCount++
                            errors.add(syncResult.error)
                        }

                        else -> {
                            errorCount++
                            errors.add(Exception("Unexpected sync result state"))
                        }
                    }
                } catch (exception: Exception) {
                    errorCount++
                    errors.add(exception)
                }
            }

            // 4. 성공적으로 처리된 Outbox 항목들 삭제
            processedEntries.forEach { entry ->
                outboxDao.deleteOperation(entry.id)
            }

            CustomResult.Success(
                SyncResult(
                    totalCount = pendingEntries.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors
                )
            )

        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }

    private suspend fun handleCreate(
        outboxEntry: OutboxEntity,
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<Unit, Exception> {
        // 로컬에서 엔티티 조회 후 서버에 생성
        val entityResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (entityResult) {
            is CustomResult.Success -> {
                val entity = entityResult.data ?: return CustomResult.Failure(
                    Exception("Entity not found locally for CREATE operation")
                )
                remoteRepository.create(entity).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected create result state"))
                    }
                }
            }

            is CustomResult.Failure -> CustomResult.Failure(entityResult.error)
            else -> CustomResult.Failure(Exception("Entity not found locally for CREATE operation"))
        }
    }

    private suspend fun handleUpdate(
        outboxEntry: OutboxEntity,
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<Unit, Exception> {
        // 로컬에서 엔티티 조회 후 서버에 업데이트
        val entityResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (entityResult) {
            is CustomResult.Success -> {
                entityResult.data ?: return CustomResult.Failure(
                    Exception("Entity not found locally for UPDATE operation")
                )

                // TODO: 각 도메인별로 업데이트 데이터 구성이 다름
                // 이 부분은 별도의 Mapper나 Adapter 패턴으로 분리 필요
                val updateData = mapOf<String, Any?>() // Placeholder

                remoteRepository.update(DocumentId.from(outboxEntry.documentId), updateData)
                    .let { result ->
                        when (result) {
                            is CustomResult.Success -> CustomResult.Success(Unit)
                            is CustomResult.Failure -> CustomResult.Failure(result.error)
                            else -> CustomResult.Failure(Exception("Unexpected update result state"))
                        }
                    }
            }

            is CustomResult.Failure -> CustomResult.Failure(entityResult.error)
            else -> CustomResult.Failure(Exception("Entity not found locally for UPDATE operation"))
        }
    }

    private suspend fun handleDelete(
        outboxEntry: OutboxEntity,
        remoteRepository: DefaultRepository<T>
    ): CustomResult<Unit, Exception> {
        // 서버에서 엔티티 삭제
        return remoteRepository.delete(DocumentId.from(outboxEntry.documentId)).let { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unexpected delete result state"))
            }
        }
    }

    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>
    )
}