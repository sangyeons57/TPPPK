package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.TaskContainerRemoteDataSource
import com.example.data.datasource.remote.TaskContainerRemoteDataSourceImpl
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.TaskContainer
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.TaskContainerRepository
import com.example.domain.repository.factory.context.TaskContainerRepositoryFactoryContext
import javax.inject.Inject

class TaskContainerRepositoryImpl @Inject constructor(
    private val taskContainerRemoteDataSource: TaskContainerRemoteDataSource,
    override val factoryContext: TaskContainerRepositoryFactoryContext,
) : DefaultRepositoryImpl(taskContainerRemoteDataSource, factoryContext), TaskContainerRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is TaskContainer)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type TaskContainer"))
        ensureCollection()
        return if (entity.isNew) {
            (taskContainerRemoteDataSource as TaskContainerRemoteDataSourceImpl)
                .createContainer(entity.toDto())
        } else {
            (taskContainerRemoteDataSource as TaskContainerRemoteDataSourceImpl)
                .updateContainer(entity.getChangedFields())
        }
    }
}