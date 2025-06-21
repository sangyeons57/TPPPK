package com.example.data.repository.factory

import com.example.data.datasource.remote.FileDataSource
import com.example.data.repository.base.FileRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.FileRepository
import com.example.domain.repository.factory.context.FileRepositoryFactoryContext
import javax.inject.Inject

class FileRepositoryFactoryImpl @Inject constructor(
    private val fileDataSource: FileDataSource
) : RepositoryFactory<FileRepositoryFactoryContext, FileRepository> {

    override fun create(input: FileRepositoryFactoryContext): FileRepository {
        return FileRepositoryImpl(
            fileDataSource = fileDataSource
        )
    }
}
