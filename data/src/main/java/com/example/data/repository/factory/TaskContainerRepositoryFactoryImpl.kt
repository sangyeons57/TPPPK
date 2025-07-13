package com.example.data.repository.factory

import com.example.data.datasource.remote.TaskContainerRemoteDataSource
import com.example.data.repository.base.TaskContainerRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.TaskContainerRepository
import com.example.domain.repository.factory.context.TaskContainerRepositoryFactoryContext
import javax.inject.Inject

class TaskContainerRepositoryFactoryImpl @Inject constructor(
    private val taskContainerRemoteDataSource: TaskContainerRemoteDataSource
) : RepositoryFactory<TaskContainerRepositoryFactoryContext, TaskContainerRepository> {

    override fun create(input: TaskContainerRepositoryFactoryContext): TaskContainerRepository {
        return TaskContainerRepositoryImpl(
            factoryContext = input,
            taskContainerRemoteDataSource = taskContainerRemoteDataSource
        )
    }
}