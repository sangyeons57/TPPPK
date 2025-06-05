package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.result.CustomResult

/**
 * Interface for handling file operations with a remote storage, like Firebase Storage.
 */
interface FileDataSource {
    /**
     * Uploads a file to the specified storage path.
     * @param storagePath The full path in Firebase Storage where the file should be uploaded.
     * @param fileUri The local URI of the file to upload.
     * @return A CustomResult containing the download URL of the uploaded file on success, or an Exception on failure.
     */
    suspend fun uploadFile(storagePath: String, fileUri: Uri): CustomResult<String, Exception>

    /**
     * Deletes a file from the specified storage path.
     * @param storagePath The full path in Firebase Storage of the file to delete.
     * @return A CustomResult indicating success (Unit) or an Exception on failure.
     */
    suspend fun deleteFile(storagePath: String): CustomResult<Unit, Exception>

    /**
     * Gets the download URL for a file at the specified storage path.
     * @param storagePath The full path in Firebase Storage of the file.
     * @return A CustomResult containing the download URL string on success, or an Exception on failure.
     */
    suspend fun getDownloadUrl(storagePath: String): CustomResult<String, Exception>

    /**
     * Downloads a file from the specified storage path to a local file URI.
     * @param storagePath The full path in Firebase Storage of the file to download.
     * @param localFileUri The local URI where the downloaded file should be saved.
     * @return A CustomResult indicating success (Unit) or an Exception on failure.
     */
    suspend fun downloadFileToUri(storagePath: String, localFileUri: Uri): CustomResult<Unit, Exception>
}
