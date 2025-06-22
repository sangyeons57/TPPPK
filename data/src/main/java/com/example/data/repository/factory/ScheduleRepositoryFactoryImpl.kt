package com.example.data.repository.factory

import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.data.repository.base.ScheduleRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ScheduleRepository
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class ScheduleRepositoryFactoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
) : RepositoryFactory<ScheduleRepositoryFactoryContext, ScheduleRepository> {

    override fun create(input: ScheduleRepositoryFactoryContext): ScheduleRepository {
        return ScheduleRepositoryImpl(
            scheduleRemoteDataSource = scheduleRemoteDataSource,
            factoryContext = input,
        )
    }
}
