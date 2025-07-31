package com.example.domain_usecase.usecase.file

import com.example.domain_repository.base.FileRepository
import javax.inject.Inject

/**
 * Use case to check if a file exists in remote storage.
 */
class CheckFileExistenceUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(path: String): Boolean {
        return fileRepository.checkFileExists(path)
    }
}
