package com.example.domain.provider.file


import com.example.domain.repository.remote.FileRepository
import com.example.domain.usecase.file.CheckFileExistenceUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUseCaseProvider @Inject constructor(
    private val fileRepository: FileRepository
) {

    fun create(): FileUseCases {
        
        return FileUseCasesData(
            checkFileExistenceUseCase = CheckFileExistenceUseCase(fileRepository),
            fileRepository = fileRepository
        )
    }
}

interface FileUseCases {
    val checkFileExistenceUseCase: CheckFileExistenceUseCase
    val fileRepository: FileRepository
}

data class FileUseCasesData(
    override val checkFileExistenceUseCase: CheckFileExistenceUseCase,
    override val fileRepository: FileRepository
) : FileUseCases