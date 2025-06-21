package com.example.domain.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.Repository

/**
 * Interface for handling file operations.
 * This repository abstracts the data source (e.g., Firebase Storage)
 * and provides a clean API for file-related use cases.
 */
interface FileRepository : Repository {

    /**
     * Uploads a file to a specified path.
     *
     * @param storagePath The full path in the storage where the file should be uploaded.
     *                    Use [com.example.core_common.constants.FirebaseStorageConstants] to build this path.
     * @param fileUri The local URI of the file to upload.
     * @return A [CustomResult] containing the download URL of the uploaded file on success, or an [Exception] on failure.
     */
    suspend fun uploadFile(storagePath: String, fileUri: Uri): CustomResult<String, Exception>

    /**
     * Deletes a file from a specified path.
     *
     * @param storagePath The full path in the storage of the file to delete.
     *                    Use [com.example.core_common.constants.FirebaseStorageConstants] to build this path.
     * @return A [CustomResult] indicating success (Unit) or an [Exception] on failure.
     */
    suspend fun deleteFile(storagePath: String): CustomResult<Unit, Exception>

    /**
     * Gets the download URL for a file at a specified path.
     *
     * @param storagePath The full path in the storage of the file.
     *                    Use [com.example.core_common.constants.FirebaseStorageConstants] to build this path.
     * @return A [CustomResult] containing the download URL string on success, or an [Exception] on failure.
     */
    suspend fun getDownloadUrl(storagePath: String): CustomResult<String, Exception>

    /**
     * Downloads a file from a specified path to a local file URI.
     *
     * @param storagePath The full path in the storage of the file to download.
     *                    Use [com.example.core_common.constants.FirebaseStorageConstants] to build this path.
     * @param localFileUri The local URI where the downloaded file should be saved.
     * @return A [CustomResult] indicating success (Unit) or an [Exception] on failure.
     */
    suspend fun downloadFileToUri(storagePath: String, localFileUri: Uri): CustomResult<Unit, Exception>
}
