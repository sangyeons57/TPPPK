package com.example.domain.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.Repository

/**
 * Repository interface for handling media-related operations such as uploading and deleting files.
 */
interface MediaRepository : Repository {
    
    /**
     * Uploads a file to the media storage from a Uri.
     *
     * @param uri The Uri of the file to upload.
     * @param storagePath The complete storage path including filename where the file should be stored.
     * @return A [CustomResult] containing the download URL of the uploaded file on success, or an error.
     */
    suspend fun uploadFile(
        uri: Uri,
        storagePath: String
    ): CustomResult<String, Exception>
    
    /**
     * Deletes a file from the media storage.
     *
     * @param fileUrl The URL of the file to delete.
     * @return A [CustomResult] indicating success or failure.
     */
    suspend fun deleteFile(fileUrl: String): CustomResult<Unit, Exception>
    
    /**
     * Gets a download URL for a file.
     *
     * @param filePath The path of the file in the storage.
     * @return A [CustomResult] containing the download URL on success, or an error.
     */
    suspend fun getFileUrl(filePath: String): CustomResult<String, Exception>
}
