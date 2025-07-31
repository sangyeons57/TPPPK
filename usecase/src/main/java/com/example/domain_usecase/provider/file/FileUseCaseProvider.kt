package com.example.domain_usecase.provider.file

import com.example.domain_repository.base.FileRepository
import com.example.domain_usecase.usecase.file.CheckFileExistenceUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUseCaseProvider @Inject constructor(
    private val fileRepository: FileRepository
) {

    fun create(): FileUseCases {
        return FileUseCasesData(
            checkFileExistenceUseCase = CheckFileExistenceUseCase(this.fileRepository)
        )
    }
}

interface FileUseCases {
    val checkFileExistenceUseCase: CheckFileExistenceUseCase
}

data class FileUseCasesData(
    override val checkFileExistenceUseCase: CheckFileExistenceUseCase
) : FileUseCases