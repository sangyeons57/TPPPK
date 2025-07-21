package com.example.domain.provider.file

import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.FileRepository
import com.example.domain.repository.factory.context.FileRepositoryFactoryContext
import com.example.domain.usecase.file.CheckFileExistenceUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUseCaseProvider @Inject constructor(
    private val fileRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<FileRepositoryFactoryContext, FileRepository>
) {

    fun create(): FileUseCases {
        val fileRepository = fileRepositoryFactory.create(FileRepositoryFactoryContext())
        
        return FileUseCasesData(
            checkFileExistenceUseCase = CheckFileExistenceUseCase(fileRepository)
        )
    }
}

interface FileUseCases {
    val checkFileExistenceUseCase: CheckFileExistenceUseCase
}

data class FileUseCasesData(
    override val checkFileExistenceUseCase: CheckFileExistenceUseCase
) : FileUseCases