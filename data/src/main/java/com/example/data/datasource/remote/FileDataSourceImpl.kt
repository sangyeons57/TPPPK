package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementation of [FileDataSource] using Firebase Storage.
 * @param storage The FirebaseStorage instance.
 */
class FileDataSourceImpl @Inject constructor(
    private val storage: FirebaseStorage
) : FileDataSource {

    /**
     * Uploads a file to the specified storage path in Firebase Storage.
     * @param storagePath The full path in Firebase Storage where the file should be uploaded (e.g., "user_profile_images/userId/fileName.jpg").
     * @param fileUri The local URI of the file to upload.
     * @return A CustomResult containing the download URL of the uploaded file on success, or an Exception on failure.
     */
    override suspend fun uploadFile(storagePath: String, fileUri: Uri): CustomResult<String, Exception> {
        return try {
            val storageRef = storage.getReference(storagePath)
            val uploadTask = storageRef.putFile(fileUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            CustomResult.Success(downloadUrl)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * Deletes a file from the specified storage path in Firebase Storage.
     * @param storagePath The full path in Firebase Storage of the file to delete (e.g., "user_profile_images/userId/fileName.jpg").
     * @return A CustomResult indicating success (Unit) or an Exception on failure.
     */
    override suspend fun deleteFile(storagePath: String): CustomResult<Unit, Exception> {
        return try {
            val storageRef = storage.getReference(storagePath)
            storageRef.delete().await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * Gets the download URL for a file at the specified storage path in Firebase Storage.
     * @param storagePath The full path in Firebase Storage of the file.
     * @return A CustomResult containing the download URL string on success, or an Exception on failure.
     */
    override suspend fun getDownloadUrl(storagePath: String): CustomResult<String, Exception> {
        return try {
            val storageRef = storage.getReference(storagePath)
            val downloadUrl = storageRef.downloadUrl.await().toString()
            CustomResult.Success(downloadUrl)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * Downloads a file from the specified storage path in Firebase Storage to a local file URI.
     * @param storagePath The full path in Firebase Storage of the file to download.
     * @param localFileUri The local URI where the downloaded file should be saved.
     * @return A CustomResult indicating success (Unit) or an Exception on failure.
     */
    override suspend fun downloadFileToUri(storagePath: String, localFileUri: Uri): CustomResult<Unit, Exception> {
        return try {
            val storageRef = storage.getReference(storagePath)
            storageRef.getFile(localFileUri).await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
