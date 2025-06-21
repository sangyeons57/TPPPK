package com.example.data.repository.base

import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.repository.base.MediaRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [MediaRepository] that uses Firebase Storage for media operations.
 * 
 * Provides methods for uploading, deleting, and retrieving media files from Firebase Storage.
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val firebaseStorage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : MediaRepository {

    override suspend fun deleteFile(fileUrl: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val fileRef = firebaseStorage.getReferenceFromUrl(fileUrl)
            fileRef.delete().await()
            Unit
        }
    }

    override suspend fun getFileUrl(filePath: String): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val fileRef = getStorageReference(filePath)
            fileRef.downloadUrl.await().toString()
        }
    }
    
    /**
     * Uploads a file to Firebase Storage from a Uri.
     *
     * @param uri The Uri of the file to upload
     * @param storagePath The complete storage path including filename
     * @return A [CustomResult] containing the download URL on success, or an error
     */
    override suspend fun uploadFile(uri: Uri, storagePath: String): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val fileRef = getStorageReference(storagePath)
            
            // Get input stream from Uri
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
            
            inputStream.use { stream ->
                // Upload file from input stream
                val uploadTask = fileRef.putStream(stream).await()
                
                // Get download URL
                val downloadUrl = fileRef.downloadUrl.await()
                downloadUrl.toString()
            }
        }
    }
    
    /**
     * Gets a reference to a file in Firebase Storage.
     *
     * @param path The path to the file in the storage bucket.
     * @return A [StorageReference] to the file.
     */
    private fun getStorageReference(path: String): StorageReference {
        return firebaseStorage.reference.child(path)
    }
}
