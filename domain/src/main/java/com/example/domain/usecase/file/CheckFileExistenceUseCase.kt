package com.example.domain.usecase.file

import com.example.domain.repository.base.FileRepository
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
