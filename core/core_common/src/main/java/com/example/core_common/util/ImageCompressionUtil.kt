package com.example.core_common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * 이미지 압축 및 최적화 유틸리티 클래스
 */
object ImageCompressionUtil {

    /**
     * 이미지를 압축하고 임시 파일로 저장합니다.
     * @param context 컨텍스트
     * @param imageUri 원본 이미지 URI
     * @param maxWidth 최대 너비
     * @param maxHeight 최대 높이
     * @param quality JPEG 품질 (0-100)
     * @return 압축된 이미지 파일의 URI
     */
    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080,
        quality: Int = 80
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null

            // 원본 이미지의 옵션을 먼저 읽어옴 (메모리 효율성을 위해)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 적절한 샘플링 크기 계산
            val sampleSize = calculateInSampleSize(options, maxWidth, maxHeight)

            // 실제 이미지 디코딩
            val actualInputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null

            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            
            var bitmap = BitmapFactory.decodeStream(actualInputStream, null, decodingOptions)
                ?: return@withContext null
            actualInputStream.close()

            // EXIF 정보를 기반으로 이미지 회전 보정
            bitmap = correctImageOrientation(context, imageUri, bitmap)

            // 추가 크기 조정 (필요시)
            if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                bitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
            }

            // 압축된 이미지를 임시 파일로 저장
            val tempFile = File.createTempFile("compressed_image_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.close()
            bitmap.recycle()

            Uri.fromFile(tempFile)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 썸네일 이미지를 생성합니다.
     * @param context 컨텍스트
     * @param imageUri 원본 이미지 URI
     * @param size 썸네일 크기 (정사각형)
     * @return 썸네일 이미지 파일의 URI
     */
    suspend fun createThumbnail(
        context: Context,
        imageUri: Uri,
        size: Int = 200
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null

            // 원본 이미지 크기 확인
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 썸네일용 샘플 크기 계산
            val sampleSize = calculateInSampleSize(options, size, size)

            // 실제 이미지 디코딩
            val actualInputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null

            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            
            var bitmap = BitmapFactory.decodeStream(actualInputStream, null, decodingOptions)
                ?: return@withContext null
            actualInputStream.close()

            // EXIF 정보를 기반으로 이미지 회전 보정
            bitmap = correctImageOrientation(context, imageUri, bitmap)

            // 정사각형 썸네일 생성
            bitmap = createSquareThumbnail(bitmap, size)

            // 썸네일을 임시 파일로 저장
            val tempFile = File.createTempFile("thumbnail_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.close()
            bitmap.recycle()

            Uri.fromFile(tempFile)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 적절한 샘플링 크기를 계산합니다.
     */
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

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 비트맵 크기를 조정합니다.
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
        if (resizedBitmap != bitmap) {
            bitmap.recycle()
        }

        return resizedBitmap
    }

    /**
     * 정사각형 썸네일을 생성합니다.
     */
    private fun createSquareThumbnail(bitmap: Bitmap, size: Int): Bitmap {
        val dimension = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - dimension) / 2
        val y = (bitmap.height - dimension) / 2

        val squareBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)
        
        val scaledBitmap = if (dimension != size) {
            Bitmap.createScaledBitmap(squareBitmap, size, size, true)
        } else {
            squareBitmap
        }

        if (squareBitmap != bitmap && squareBitmap != scaledBitmap) {
            squareBitmap.recycle()
        }
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }

        return scaledBitmap
    }

    /**
     * EXIF 정보를 기반으로 이미지 회전을 보정합니다.
     */
    private fun correctImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return bitmap

            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }

            return rotatedBitmap

        } catch (e: IOException) {
            e.printStackTrace()
            return bitmap
        }
    }

    /**
     * 이미지 파일 크기를 예상합니다.
     * @param context 컨텍스트
     * @param imageUri 이미지 URI
     * @param quality JPEG 품질
     * @return 예상 파일 크기 (바이트)
     */
    suspend fun estimateCompressedSize(
        context: Context,
        imageUri: Uri,
        quality: Int = 80
    ): Long = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext 0L

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 픽셀 수에 기반한 대략적인 크기 추정
            val pixelCount = options.outWidth * options.outHeight
            val bytesPerPixel = when (quality) {
                in 90..100 -> 2.5
                in 70..89 -> 1.5
                in 50..69 -> 1.0
                in 30..49 -> 0.7
                else -> 0.5
            }

            (pixelCount * bytesPerPixel).toLong()

        } catch (e: Exception) {
            0L
        }
    }
}