package com.example.data.datasource.remote

import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.util.SafeImageCompressionUtil
import com.example.data.util.ResourceType
import com.example.data.util.UploadResourceManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
// import timber.log.Timber - Data 모듈에서 제거됨
import java.util.UUID
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
    private val resourceManager: UploadResourceManager
) : FileUploadDataSource {
    
    // 리소스 정리를 위한 코루틴 스코프
    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun uploadFileWithProgress(storagePath: String, fileUri: Uri): Flow<FileUploadResult> {
        return callbackFlow {
            val uploadId = UUID.randomUUID().toString()
            var uploadTask: UploadTask? = null
            
            try {
                val storageRef = storage.getReference(storagePath)
                uploadTask = storageRef.putFile(fileUri)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = FileUploadProgress(
                        bytesTransferred = taskSnapshot.bytesTransferred,
                        totalBytes = taskSnapshot.totalByteCount
                    )
                    trySend(FileUploadResult.Progress(progress))
                }

                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // 업로드 완료 후 다운로드 URL 가져오기
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        trySend(FileUploadResult.Success(downloadUri.toString()))
                        close()
                        // 성공시 관련 리소스 정리
                        cleanupScope.launch { resourceManager.cleanupUploadResources(uploadId) }
                    }.addOnFailureListener { exception ->
                        trySend(FileUploadResult.Failure(exception))
                        close(exception)
                        // 실패시에도 리소스 정리
                        cleanupScope.launch { resourceManager.cleanupUploadResources(uploadId) }
                    }
                }

                uploadTask.addOnFailureListener { exception ->
                    trySend(FileUploadResult.Failure(exception))
                    close(exception)
                    // 실패시 리소스 정리
                    cleanupScope.launch { resourceManager.cleanupUploadResources(uploadId) }
                }

                awaitClose {
                    // 업로드 취소시 안전한 정리
                    try {
                        uploadTask?.let { task ->
                            if (task.isInProgress) {
                                task.cancel()
                                println("DEBUG: Upload task cancelled for path: $storagePath")
                            }
                        }
                        // 취소시에도 리소스 정리
                        cleanupScope.launch { resourceManager.cleanupUploadResources(uploadId) }
                    } catch (e: Exception) {
                        println("ERROR: Error during upload cleanup: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                trySend(FileUploadResult.Failure(e))
                close(e)
                // 예외 발생시에도 리소스 정리
                cleanupScope.launch { resourceManager.cleanupUploadResources(uploadId) }
            }
        }
    }

    override suspend fun uploadCompressedImage(
        storagePath: String,
        fileUri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): CustomResult<String, Exception> {
        val uploadId = UUID.randomUUID().toString()
        
        return try {
            // 안전한 이미지 압축
            val compressionResult = SafeImageCompressionUtil.compressImageSafely(
                context = context,
                imageUri = fileUri,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                quality = quality
            )
            
            val compressedUri = when (compressionResult) {
                is CustomResult.Success -> compressionResult.data
                is CustomResult.Failure -> {
                    println("ERROR: Image compression failed: ${compressionResult.error.message}")
                    return CustomResult.Failure(Exception("이미지 압축에 실패했습니다: ${compressionResult.error.message}"))
                }
                is CustomResult.Initial -> {
                    return CustomResult.Failure(Exception("이미지 압축 초기 상태 오류"))
                }
                is CustomResult.Loading -> {
                    return CustomResult.Failure(Exception("이미지 압축 로딩 중 오류"))
                }
                is CustomResult.Progress -> {
                    return CustomResult.Failure(Exception("이미지 압축 진행 중 오류"))
                }
            }

            // 압축된 파일을 관리 대상으로 추가
            val compressedFile = compressedUri.path?.let { File(it) }
            compressedFile?.let { file ->
                resourceManager.trackResource(file, ResourceType.COMPRESSED_IMAGE, uploadId)
            }

            // 압축된 이미지 업로드
            val storageRef = storage.getReference(storagePath)
            val uploadTask = storageRef.putFile(compressedUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            
            // 성공시 리소스 정리
            resourceManager.cleanupUploadResources(uploadId)
            
            CustomResult.Success(downloadUrl)
        } catch (e: Exception) {
            println("ERROR: Compressed image upload failed: ${e.message}")
            // 실패시에도 리소스 정리
            resourceManager.cleanupUploadResources(uploadId)
            CustomResult.Failure(e)
        }
    }

    override suspend fun uploadThumbnail(
        storagePath: String,
        fileUri: Uri,
        thumbnailSize: Int
    ): CustomResult<String, Exception> {
        val uploadId = UUID.randomUUID().toString()
        
        return try {
            // 안전한 썸네일 생성
            val thumbnailResult = SafeImageCompressionUtil.createThumbnailSafely(
                context = context,
                imageUri = fileUri,
                size = thumbnailSize
            )
            
            val thumbnailUri = when (thumbnailResult) {
                is CustomResult.Success -> thumbnailResult.data
                is CustomResult.Failure -> {
                    println("ERROR: Thumbnail creation failed: ${thumbnailResult.error.message}")
                    return CustomResult.Failure(Exception("썸네일 생성에 실패했습니다: ${thumbnailResult.error.message}"))
                }
                is CustomResult.Initial -> {
                    return CustomResult.Failure(Exception("썸네일 생성 초기 상태 오류"))
                }
                is CustomResult.Loading -> {
                    return CustomResult.Failure(Exception("썸네일 생성 로딩 중 오류"))
                }
                is CustomResult.Progress -> {
                    return CustomResult.Failure(Exception("썸네일 생성 진행 중 오류"))
                }
            }

            // 썸네일 파일을 관리 대상으로 추가
            val thumbnailFile = thumbnailUri.path?.let { File(it) }
            thumbnailFile?.let { file ->
                resourceManager.trackResource(file, ResourceType.THUMBNAIL, uploadId)
            }

            // 썸네일 업로드
            val storageRef = storage.getReference(storagePath)
            val uploadTask = storageRef.putFile(thumbnailUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            
            // 성공시 리소스 정리
            resourceManager.cleanupUploadResources(uploadId)
            
            CustomResult.Success(downloadUrl)
        } catch (e: Exception) {
            println("ERROR: Thumbnail upload failed: ${e.message}")
            // 실패시에도 리소스 정리
            resourceManager.cleanupUploadResources(uploadId)
            CustomResult.Failure(e)
        }
    }
}