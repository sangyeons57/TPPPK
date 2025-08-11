package com.example.data_datasource.remote.special

// import timber.log.Timber - Data 모듈에서 제거됨
import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 파일 업로드 진행률 데이터
 */
data class FileUploadProgress(
    val bytesTransferred: Long,
    val totalBytes: Long,
    val progress: Float = if (totalBytes > 0) bytesTransferred.toFloat() / totalBytes.toFloat() else 0f
)

/**
 * 파일 업로드 결과
 */
sealed class FileUploadResult {
    data class Progress(val progress: FileUploadProgress) : FileUploadResult()
    data class Success(val downloadUrl: String) : FileUploadResult()
    data class Failure(val exception: Exception) : FileUploadResult()
}

/**
 * 진행률 추적이 가능한 파일 업로드를 위한 DataSource
 */
interface FileUploadDataSource {
    /**
     * 진행률 추적과 함께 파일을 업로드합니다.
     * @param storagePath Firebase Storage 경로
     * @param fileUri 업로드할 파일의 로컬 URI
     * @return 업로드 진행률과 결과를 스트리밍하는 Flow
     */
    fun uploadFileWithProgress(storagePath: String, fileUri: Uri): Flow<FileUploadResult>
    
    /**
     * 이미지 파일을 압축하여 업로드합니다.
     * @param storagePath Firebase Storage 경로
     * @param fileUri 업로드할 이미지 파일의 로컬 URI
     * @param maxWidth 최대 너비 (픽셀)
     * @param maxHeight 최대 높이 (픽셀)
     * @param quality JPEG 품질 (0-100)
     * @return 업로드 진행률과 결과를 스트리밍하는 Flow
     */
    suspend fun uploadCompressedImage(
        storagePath: String, 
        fileUri: Uri, 
        maxWidth: Int = 1920, 
        maxHeight: Int = 1080, 
        quality: Int = 80
    ): CustomResult<String, Exception>
    
    /**
     * 썸네일을 생성하여 업로드합니다.
     * @param storagePath Firebase Storage 경로
     * @param fileUri 원본 파일 URI
     * @param thumbnailSize 썸네일 크기 (픽셀)
     * @return 썸네일 다운로드 URL
     */
    suspend fun uploadThumbnail(
        storagePath: String,
        fileUri: Uri,
        thumbnailSize: Int = 200
    ): CustomResult<String, Exception>
}

/**
 * FileUploadDataSource의 Firebase Storage 구현체
 */
class FileUploadDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
) : FileUploadDataSource {
    
    // 리소스 정리를 위한 코루틴 스코프
    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun uploadFileWithProgress(storagePath: String, fileUri: Uri): Flow<FileUploadResult> =
        callbackFlow {
            try {
                val storageRef = storage.reference.child(storagePath)
                val uploadTask = storageRef.putFile(fileUri)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = FileUploadProgress(
                        bytesTransferred = taskSnapshot.bytesTransferred,
                        totalBytes = taskSnapshot.totalByteCount
                    )
                    trySend(FileUploadResult.Progress(progress))
                }

                uploadTask.addOnSuccessListener { _ ->
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        trySend(FileUploadResult.Success(downloadUri.toString()))
                        close()
                    }.addOnFailureListener { exception ->
                        trySend(FileUploadResult.Failure(exception))
                        close(exception)
                    }
                }.addOnFailureListener { exception ->
                    trySend(FileUploadResult.Failure(exception))
                    close(exception)
                }

            } catch (e: Exception) {
                trySend(FileUploadResult.Failure(e))
                close(e)
            }

            awaitClose {
                // 업로드 작업 정리
            }
        }

    override suspend fun uploadCompressedImage(
        storagePath: String,
        fileUri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): CustomResult<String, Exception> {
        return try {
            // 이미지 압축
            val compressionOptions =
                com.example.core_common.util.ImageCompressor.CompressionOptions(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    quality = quality,
                    maxFileSizeBytes = 3 * 1024 * 1024 // 3MB 제한
                )

            val compressedUri = com.example.core_common.util.ImageCompressor.compressImage(
                context = context,
                imageUri = fileUri,
                options = compressionOptions
            )

            // Firebase Storage에 업로드
            val storageRef = storage.reference.child(storagePath)
            val uploadTask = storageRef.putFile(compressedUri)

            val taskSnapshot = uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await()

            CustomResult.Success(downloadUrl.toString())

        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun uploadThumbnail(
        storagePath: String,
        fileUri: Uri,
        thumbnailSize: Int
    ): CustomResult<String, Exception> {
        return try {
            // 썸네일용 압축 옵션 (작은 크기, 높은 압축률)
            val compressionOptions =
                com.example.core_common.util.ImageCompressor.CompressionOptions(
                    maxWidth = thumbnailSize,
                    maxHeight = thumbnailSize,
                    quality = 60, // 썸네일은 품질보다 크기 우선
                    maxFileSizeBytes = 500 * 1024 // 500KB 제한
                )

            val compressedUri = com.example.core_common.util.ImageCompressor.compressImage(
                context = context,
                imageUri = fileUri,
                options = compressionOptions
            )

            // Firebase Storage에 업로드
            val thumbnailPath = storagePath.replace("/", "/thumbnails/")
            val storageRef = storage.reference.child(thumbnailPath)
            val uploadTask = storageRef.putFile(compressedUri)

            val taskSnapshot = uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await()

            CustomResult.Success(downloadUrl.toString())

        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}