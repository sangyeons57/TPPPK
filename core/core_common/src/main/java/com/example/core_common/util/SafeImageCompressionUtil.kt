package com.example.core_common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.core_common.result.CustomResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 이미지 압축 에러 타입
 */
sealed class ImageCompressionError : Exception() {
    data class FileNotFound(val uri: Uri) : ImageCompressionError() {
        override val message: String = "파일을 찾을 수 없습니다: $uri"
    }
    
    data class DecodingFailed(override val cause: Throwable) : ImageCompressionError() {
        override val message: String = "이미지 디코딩에 실패했습니다: ${cause.message}"
    }
    
    data class CompressionFailed(override val cause: Throwable) : ImageCompressionError() {
        override val message: String = "이미지 압축에 실패했습니다: ${cause.message}"
    }
    
    data class TempFileCreationFailed(override val cause: Throwable) : ImageCompressionError() {
        override val message: String = "임시 파일 생성에 실패했습니다: ${cause.message}"
    }
    
    data class ExifProcessingFailed(override val cause: Throwable) : ImageCompressionError() {
        override val message: String = "EXIF 정보 처리에 실패했습니다: ${cause.message}"
    }
}

/**
 * 메모리 안전한 이미지 압축 및 최적화 유틸리티 클래스
 */
object SafeImageCompressionUtil {

    /**
     * 이미지를 안전하게 압축하고 임시 파일로 저장합니다.
     * 모든 리소스는 자동으로 정리됩니다.
     */
    suspend fun compressImageSafely(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080,
        quality: Int = 80
    ): CustomResult<Uri, ImageCompressionError> = withContext(Dispatchers.IO) {
        
        var inputBitmap: Bitmap? = null
        var processedBitmap: Bitmap? = null
        var outputStream: FileOutputStream? = null
        var tempFile: File? = null
        
        try {
            // 1. 원본 이미지 옵션 확인
            val options = getBitmapOptions(context, imageUri)
                ?: return@withContext CustomResult.Failure(
                    ImageCompressionError.FileNotFound(imageUri)
                )

            // 2. 적절한 샘플링 크기 계산
            val sampleSize = calculateInSampleSize(options, maxWidth, maxHeight)

            // 3. 실제 이미지 디코딩 (샘플링 적용)
            inputBitmap = decodeBitmapSafely(context, imageUri, sampleSize)
                ?: return@withContext CustomResult.Failure(
                    ImageCompressionError.DecodingFailed(Exception("Bitmap decoding returned null"))
                )

            // 4. EXIF 정보 기반 회전 보정
            processedBitmap = correctImageOrientation(context, imageUri, inputBitmap)
            
            // 5. 추가 크기 조정 (필요시)
            if (processedBitmap.width > maxWidth || processedBitmap.height > maxHeight) {
                val resizedBitmap = resizeBitmap(processedBitmap, maxWidth, maxHeight)
                
                // 이전 bitmap과 다르면 정리
                if (resizedBitmap != processedBitmap && resizedBitmap != inputBitmap) {
                    processedBitmap.recycle()
                }
                processedBitmap = resizedBitmap
            }

            // 6. 임시 파일 생성 및 압축 저장
            tempFile = File.createTempFile("compressed_image_", ".jpg", context.cacheDir)
            outputStream = FileOutputStream(tempFile)
            
            val compressionResult = processedBitmap.compress(
                Bitmap.CompressFormat.JPEG, 
                quality, 
                outputStream
            )
            
            if (!compressionResult) {
                return@withContext CustomResult.Failure(
                    ImageCompressionError.CompressionFailed(Exception("Bitmap.compress returned false"))
                )
            }

            CustomResult.Success(Uri.fromFile(tempFile))

        } catch (e: SecurityException) {
            // 파일 접근 권한 없음
            CustomResult.Failure(ImageCompressionError.FileNotFound(imageUri))
        } catch (e: IOException) {
            // 파일 I/O 에러
            CustomResult.Failure(ImageCompressionError.TempFileCreationFailed(e))
        } catch (e: OutOfMemoryError) {
            // 메모리 부족
            CustomResult.Failure(ImageCompressionError.CompressionFailed(e))
        } catch (e: Exception) {
            // 기타 예외
            CustomResult.Failure(ImageCompressionError.CompressionFailed(e))
        } finally {
            // 리소스 정리 (finally 블록에서 보장)
            try {
                outputStream?.close()
            } catch (e: Exception) {
                // 스트림 닫기 실패는 무시
            }
            
            // Bitmap 정리
            inputBitmap?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            
            processedBitmap?.let { bitmap ->
                if (!bitmap.isRecycled && bitmap != inputBitmap) {
                    bitmap.recycle()
                }
            }
            
            // 실패시 임시 파일 정리
            if (tempFile?.exists() == true && !tempFile.canRead()) {
                try {
                    tempFile.delete()
                } catch (e: Exception) {
                    // 파일 삭제 실패는 무시 (GC에 의해 정리될 것)
                }
            }
        }
    }

    /**
     * 썸네일을 안전하게 생성합니다.
     */
    suspend fun createThumbnailSafely(
        context: Context,
        imageUri: Uri,
        size: Int = 200
    ): CustomResult<Uri, ImageCompressionError> = withContext(Dispatchers.IO) {
        
        var inputBitmap: Bitmap? = null
        var processedBitmap: Bitmap? = null
        var squareBitmap: Bitmap? = null
        var outputStream: FileOutputStream? = null
        var tempFile: File? = null
        
        try {
            // 원본 이미지 크기 확인
            val options = getBitmapOptions(context, imageUri)
                ?: return@withContext CustomResult.Failure(
                    ImageCompressionError.FileNotFound(imageUri)
                )

            // 썸네일용 샘플 크기 계산
            val sampleSize = calculateInSampleSize(options, size, size)

            // 이미지 디코딩
            inputBitmap = decodeBitmapSafely(context, imageUri, sampleSize)
                ?: return@withContext CustomResult.Failure(
                    ImageCompressionError.DecodingFailed(Exception("Thumbnail decoding failed"))
                )

            // EXIF 기반 회전 보정
            processedBitmap = correctImageOrientation(context, imageUri, inputBitmap)

            // 정사각형 썸네일 생성
            squareBitmap = createSquareThumbnailSafely(processedBitmap, size)

            // 임시 파일로 저장
            tempFile = File.createTempFile("thumbnail_", ".jpg", context.cacheDir)
            outputStream = FileOutputStream(tempFile)
            
            val compressionResult = squareBitmap.compress(
                Bitmap.CompressFormat.JPEG, 
                75, 
                outputStream
            )
            
            if (!compressionResult) {
                return@withContext CustomResult.Failure(
                    ImageCompressionError.CompressionFailed(Exception("Thumbnail compression failed"))
                )
            }

            CustomResult.Success(Uri.fromFile(tempFile))

        } catch (e: Exception) {
            CustomResult.Failure(ImageCompressionError.CompressionFailed(e))
        } finally {
            // 안전한 리소스 정리
            try {
                outputStream?.close()
            } catch (e: Exception) {
                // 스트림 닫기 실패는 무시
            }
            
            // Bitmap들을 안전하게 정리
            listOf(inputBitmap, processedBitmap, squareBitmap).forEach { bitmap ->
                bitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }
    }

    /**
     * 압축된 이미지의 예상 파일 크기를 계산합니다.
     */
    suspend fun estimateCompressedSizeSafely(
        context: Context,
        imageUri: Uri,
        quality: Int = 80
    ): CustomResult<Long, ImageCompressionError> = withContext(Dispatchers.IO) {
        try {
            val options = getBitmapOptions(context, imageUri)
                ?: return@withContext CustomResult.Failure(
                    ImageCompressionError.FileNotFound(imageUri)
                )

            // 픽셀 수에 기반한 대략적인 크기 추정
            val pixelCount = options.outWidth * options.outHeight
            val bytesPerPixel = when (quality) {
                in 90..100 -> 2.5
                in 70..89 -> 1.5
                in 50..69 -> 1.0
                in 30..49 -> 0.7
                else -> 0.5
            }

            val estimatedSize = (pixelCount * bytesPerPixel).toLong()
            CustomResult.Success(estimatedSize)

        } catch (e: Exception) {
            CustomResult.Failure(ImageCompressionError.CompressionFailed(e))
        }
    }

    // === Private Helper Methods ===

    private fun getBitmapOptions(context: Context, imageUri: Uri): BitmapFactory.Options? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                options
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBitmapSafely(
        context: Context, 
        imageUri: Uri, 
        sampleSize: Int
    ): Bitmap? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inJustDecodeBounds = false
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && 
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
    }

    private fun createSquareThumbnailSafely(bitmap: Bitmap, size: Int): Bitmap {
        val dimension = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - dimension) / 2
        val y = (bitmap.height - dimension) / 2

        val squareBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)
        
        return if (dimension != size) {
            Bitmap.createScaledBitmap(squareBitmap, size, size, true).also {
                // 원본 squareBitmap과 다르면 정리
                if (it != squareBitmap) {
                    squareBitmap.recycle()
                }
            }
        } else {
            squareBitmap
        }
    }

    private fun correctImageOrientation(
        context: Context, 
        imageUri: Uri, 
        bitmap: Bitmap
    ): Bitmap {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return bitmap
                }

                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: IOException) {
            // EXIF 처리 실패시 원본 반환
            bitmap
        }
    }
}