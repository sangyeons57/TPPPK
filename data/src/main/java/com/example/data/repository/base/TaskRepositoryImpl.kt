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
            (taskRemoteDataSource as TaskRemoteDataSourceImpl)
                .createTask(entity.toDto())
        } else {
            (taskRemoteDataSource as TaskRemoteDataSourceImpl)
                .updateTask(entity.id.value, entity.getChangedFields())
        }
    }
}