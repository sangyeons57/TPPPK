package com.example.domain_usecase.usecase.file

import android.net.Uri
import android.content.Context
import com.example.core_common.result.CustomResult
import com.example.core_common.util.ImageCompressor
import com.example.domain_repository.base.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class AttachmentMeta(
    val index: Int,
    val url: String,
    val filename: String,
    val mime: String,
    val ext: String
)

data class UploadOptions(
    val maxWidth: Int = 1920,
    val maxHeight: Int = 1920,
    val quality: Int = 85,
    val maxFileSizeBytes: Long = 3L * 1024L * 1024L
)

interface UploadMessageAttachmentsUseCase {
    suspend operator fun invoke(
        channelId: String,
        messageId: String,
        imageUris: List<Uri>,
        options: UploadOptions = UploadOptions()
    ): CustomResult<List<Map<String, Any?>>, Exception>
}

class UploadMessageAttachmentsUseCaseImpl @Inject constructor(
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) : UploadMessageAttachmentsUseCase {

    override suspend fun invoke(
        channelId: String,
        messageId: String,
        imageUris: List<Uri>,
        options: UploadOptions
    ): CustomResult<List<Map<String, Any?>>, Exception> {
        return try {
            val result = imageUris.mapIndexedNotNull { index, uri ->
                val ext = (ImageCompressor.getExtension(context, uri) ?: "jpg").lowercase()
                val mime = if (ext == "jpg") "image/jpeg" else "image/$ext"

                val compressedUri = ImageCompressor.compressImage(
                    context = context,
                    imageUri = uri,
                    options = ImageCompressor.CompressionOptions(
                        maxWidth = options.maxWidth,
                        maxHeight = options.maxHeight,
                        quality = options.quality,
                        maxFileSizeBytes = options.maxFileSizeBytes
                    )
                )

                val fileName = "${messageId}_${index}_r0.${ext}"
                val storagePath = "channels/$channelId/messages/$messageId/attachments/$fileName"

                when (val upload = fileRepository.uploadFile(storagePath, compressedUri)) {
                    is CustomResult.Success -> {
                        mapOf(
                            "index" to index,
                            "url" to upload.data,
                            "filename" to (uri.lastPathSegment ?: "image.$ext"),
                            "mime" to mime,
                            "ext" to ext
                        )
                    }

                    else -> null
                }
            }
            CustomResult.Success(result)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}


