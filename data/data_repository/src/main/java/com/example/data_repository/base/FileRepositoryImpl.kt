package com.example.data_repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.special.FileDataSource
import com.example.domain_repository.base.FileRepository
import javax.inject.Inject

/**
 * Implementation of [FileRepository].
 * This class delegates file operations to a [com.example.data_datasource.datasource.remote.special.FileDataSource].
 */
class FileRepositoryImpl @Inject constructor(
    private val fileDataSource: FileDataSource
) : FileRepository {

    override suspend fun uploadFile(storagePath: String, fileUri: Uri): CustomResult<String, Exception> {
        return fileDataSource.uploadFile(storagePath, fileUri)
    }

    override suspend fun deleteFile(storagePath: String): CustomResult<Unit, Exception> {
        return fileDataSource.deleteFile(storagePath)
    }

    override suspend fun getDownloadUrl(storagePath: String): CustomResult<String, Exception> {
        return fileDataSource.getDownloadUrl(storagePath)
    }

    override suspend fun downloadFileToUri(storagePath: String, localFileUri: Uri): CustomResult<Unit, Exception> {
        return fileDataSource.downloadFileToUri(storagePath, localFileUri)
    }

    override suspend fun checkFileExists(path: String): Boolean {
        return when (val result = fileDataSource.checkFileExists(path)) {
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> false
            else -> false
        }
    }
}
