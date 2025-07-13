package com.example.data.repository.factory

import com.example.data.datasource.remote.TaskRemoteDataSource
import com.example.data.repository.base.TaskRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.TaskRepository
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext
import javax.inject.Inject

class TaskRepositoryFactoryImpl @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource
) : RepositoryFactory<TaskRepositoryFactoryContext, TaskRepository> {

    override fun create(input: TaskRepositoryFactoryContext): TaskRepository {
        return TaskRepositoryImpl(
            factoryContext = input,
            taskRemoteDataSource = taskRemoteDataSource
        )
    }
}