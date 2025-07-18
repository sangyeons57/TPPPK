package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.TaskRemoteDataSource
import com.example.data.datasource.remote.TaskRemoteDataSourceImpl
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.TaskRepository
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext
import com.google.firebase.firestore.FieldValue
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    override val factoryContext: TaskRepositoryFactoryContext,
) : DefaultRepositoryImpl(taskRemoteDataSource, factoryContext), TaskRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Task)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Task"))
        ensureCollection()
        return if (entity.isNew) {
            taskRemoteDataSource.create(entity.toDto())
        } else {
            val changedFields = entity.getChangedFields().toMutableMap()
            
            // checkedAt이 서버 타임스탬프 마커인 경우 FieldValue.serverTimestamp()로 변환
            if (entity.isCheckedAtServerTimestamp()) {
                changedFields[Task.KEY_CHECKED_AT] = FieldValue.serverTimestamp()
            }
            
            taskRemoteDataSource.update(entity.id, changedFields)
        }
    }
}