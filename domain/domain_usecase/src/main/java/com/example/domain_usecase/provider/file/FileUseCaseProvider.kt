package com.example.domain_usecase.provider.file

import com.example.domain_repository.RepositoryFactory
import com.example.domain_repository.base.FileRepository
import com.example.domain_repository.context.FileRepositoryFactoryContext
import com.example.domain_usecase.usecase.file.CheckFileExistenceUseCase
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