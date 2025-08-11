package com.example.core_common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
// ExifInterface 의존성 제거 - 간단한 압축만 사용
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 이미지 압축 유틸리티
 *
 * 기능:
 * - 이미지 크기 조정 (최대 해상도 제한)
 * - 품질 조정으로 파일 크기 압축
 * - 메모리 효율적인 처리
 */
object ImageCompressor {

    // 기본 설정값들
    private const val DEFAULT_MAX_WIDTH = 1920
    private const val DEFAULT_MAX_HEIGHT = 1920
    private const val DEFAULT_QUALITY = 80
    private const val MAX_FILE_SIZE_MB = 5 // 5MB 제한
    private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024

    /**
     * 이미지 압축 옵션
     */
    data class CompressionOptions(
        val maxWidth: Int = DEFAULT_MAX_WIDTH,
        val maxHeight: Int = DEFAULT_MAX_HEIGHT,
        val quality: Int = DEFAULT_QUALITY,
        val maxFileSizeBytes: Long = MAX_FILE_SIZE_BYTES.toLong(),
        val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    )

    /**
     * URI로부터 압축된 이미지 파일 생성
     *
     * @param context Android Context
     * @param imageUri 원본 이미지 URI
     * @param options 압축 옵션
     * @return 압축된 이미지 파일의 URI
     */
    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        options: CompressionOptions = CompressionOptions()
    ): Uri = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(
                "ImageCompressor",
                "🔄 [이미지압축] 이미지 압축 시작: quality=${options.quality}%, maxSize=${options.maxWidth}x${options.maxHeight}"
            )
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw IllegalArgumentException("Cannot open input stream for URI: $imageUri")

            // 1단계: 이미지 메타데이터 및 크기 정보 읽기
            val originalBitmap = decodeAndRotateImage(inputStream, options)
            inputStream.close()

            // 2단계: 크기 조정
            val resizedBitmap = resizeBitmap(originalBitmap, options)
            originalBitmap.recycle() // 메모리 해제

            // 3단계: 압축 및 파일 저장
            val compressedFile = compressAndSave(context, resizedBitmap, options)
            resizedBitmap.recycle() // 메모리 해제

            android.util.Log.d(
                "ImageCompressor",
                "✅ [이미지압축] 이미지 압축 완료: ${compressedFile.length() / 1024}KB"
            )
            Uri.fromFile(compressedFile)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "❌ [이미지압축] 이미지 압축 실패", e)
            throw IllegalStateException("이미지 압축 실패: ${e.message}", e)
        }
    }

    /**
     * 다중 이미지 병렬 압축
     */
    suspend fun compressImages(
        context: Context,
        imageUris: List<Uri>,
        options: CompressionOptions = CompressionOptions()
    ): List<Uri> = withContext(Dispatchers.IO) {
        imageUris.map { uri ->
            compressImage(context, uri, options)
        }
    }

    /**
     * 이미지 디코딩 (EXIF 회전 제외 - 간단한 압축만 사용)
     */
    private fun decodeAndRotateImage(
        inputStream: InputStream,
        options: CompressionOptions
    ): Bitmap {
        // 먼저 이미지 크기만 읽어서 샘플링 계산
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, boundsOptions)

        // 샘플링 비율 계산
        val sampleSize = calculateInSampleSize(
            boundsOptions.outWidth,
            boundsOptions.outHeight,
            options.maxWidth,
            options.maxHeight
        )

        // 실제 비트맵 디코딩
        inputStream.reset() // 스트림을 처음으로 되돌림
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // 메모리 절약
        }

        return BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            ?: throw IllegalStateException("비트맵 디코딩 실패")
    }


    /**
     * 적절한 샘플링 크기 계산
     */
    private fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var inSampleSize = 1

        if (originalHeight > maxHeight || originalWidth > maxWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            // 최대 크기보다 작아질 때까지 샘플링 크기 증가
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 비트맵 크기 조정
     */
    private fun resizeBitmap(bitmap: Bitmap, options: CompressionOptions): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= options.maxWidth && height <= options.maxHeight) {
            return bitmap
        }

        // 비율 유지하면서 크기 조정
        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            val newW = min(options.maxWidth, width)
            val newH = (newW / aspectRatio).toInt()
            newW to min(newH, options.maxHeight)
        } else {
            val newH = min(options.maxHeight, height)
            val newW = (newH * aspectRatio).toInt()
            min(newW, options.maxWidth) to newH
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 압축 및 파일 저장
     */
    private fun compressAndSave(
        context: Context,
        bitmap: Bitmap,
        options: CompressionOptions
    ): File {
        val tempDir = File(context.cacheDir, "compressed_images")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val extension = when (options.format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "jpg"
        }

        val tempFile = File(tempDir, "compressed_${System.currentTimeMillis()}.$extension")

        // 목표 파일 크기에 맞추기 위해 품질을 조정
        var quality = options.quality
        var outputStream: ByteArrayOutputStream

        do {
            outputStream = ByteArrayOutputStream()
            bitmap.compress(options.format, quality, outputStream)

            if (outputStream.size() <= options.maxFileSizeBytes || quality <= 10) {
                break
            }

            quality -= 10
        } while (quality > 0)

        // 최종 파일 저장
        FileOutputStream(tempFile).use { fileOut ->
            fileOut.write(outputStream.toByteArray())
        }

        outputStream.close()
        return tempFile
    }

    /**
     * 임시 압축 파일들 정리
     */
    fun clearCompressedCache(context: Context) {
        val tempDir = File(context.cacheDir, "compressed_images")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    /**
     * URI에서 파일 확장자를 추출합니다.
     *
     * @param context Android Context
     * @param uri 파일 URI
     * @return 파일 확장자 (예: "jpg", "png") 또는 null
     */
    fun getExtension(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    // ContentResolver를 통해 MIME 타입 확인
                    val mimeType = context.contentResolver.getType(uri)
                    when (mimeType) {
                        "image/jpeg" -> "jpg"
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        "image/gif" -> "gif"
                        else -> "jpg" // 기본값
                    }
                }

                "file" -> {
                    // 파일 경로에서 확장자 추출
                    val path = uri.path ?: return "jpg"
                    val lastDot = path.lastIndexOf('.')
                    if (lastDot != -1 && lastDot < path.length - 1) {
                        path.substring(lastDot + 1).lowercase()
                    } else {
                        "jpg"
                    }
                }

                else -> "jpg" // 기본값
            }
        } catch (e: Exception) {
            "jpg" // 오류 시 기본값
        }
    }
}