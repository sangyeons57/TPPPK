package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.TaskRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.mapper.task.TaskMapper
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.TaskRepository
import com.google.firebase.firestore.FieldValue
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val taskMapper: TaskMapper,
) : DefaultRepositoryImpl(taskRemoteDataSource), TaskRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Task)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Task"))
        ensureCollection()
        return if (entity.isNew) {
            taskRemoteDataSource.create(taskMapper.domainToDto(entity))
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