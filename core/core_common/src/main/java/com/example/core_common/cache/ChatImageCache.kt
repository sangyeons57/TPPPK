package com.example.core_common.cache

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 이미지 전용 캐시 매니저
 * 프로필 이미지와는 별도로 채팅에서 주고받는 이미지들의 효율적인 캐싱을 담당
 */
@Singleton
class ChatImageCache @Inject constructor(
    private val context: Context,
    private val imageLoader: ImageLoader
) {

    companion object {
        private const val TAG = "ChatImageCache"
        private const val CACHE_DIR_NAME = "chat_images"
        private const val THUMBNAIL_DIR_NAME = "chat_thumbnails"
        private const val MAX_MEMORY_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
        private const val THUMBNAIL_SIZE = 200 // 200x200 썸네일
    }

    // 메모리 캐시 - 원본 이미지 URL
    private val memoryCache =
        LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE / (4 * THUMBNAIL_SIZE * THUMBNAIL_SIZE))

    // 썸네일 메모리 캐시
    private val thumbnailMemoryCache =
        LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE / (4 * THUMBNAIL_SIZE * THUMBNAIL_SIZE))

    // 로딩 상태 추적
    private val loadingUrls = mutableSetOf<String>()
    private val thumbnailLoadingUrls = mutableSetOf<String>()
    private val mutex = Mutex()

    // 디스크 캐시 디렉토리
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailCacheDir: File by lazy {
        File(context.cacheDir, THUMBNAIL_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 이미지를 미리 로드하여 캐시에 저장
     * 채팅방 진입 시 최근 이미지들을 미리 캐싱하는 용도
     */
    suspend fun preloadImages(imageUrls: List<String>) {
        if (imageUrls.isEmpty()) return

        Log.d(TAG, "🚀 이미지 프리로딩 시작: ${imageUrls.size}개")

        withContext(Dispatchers.IO) {
            imageUrls.chunked(5).forEach { chunk -> // 5개씩 배치 처리
                chunk.forEach { url ->
                    try {
                        preloadSingleImage(url)
                        generateThumbnail(url) // 썸네일도 함께 생성
                    } catch (e: Exception) {
                        Log.e(TAG, "이미지 프리로딩 실패: $url", e)
                    }
                }
            }
        }

        Log.d(TAG, "✅ 이미지 프리로딩 완료")
    }

    /**
     * 단일 이미지 프리로딩
     */
    private suspend fun preloadSingleImage(imageUrl: String) {
        if (isImageCached(imageUrl)) {
            Log.d(TAG, "이미지 이미 캐시됨: $imageUrl")
            return
        }

        mutex.withLock {
            if (loadingUrls.contains(imageUrl)) return
            loadingUrls.add(imageUrl)
        }

        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()

            val drawable = imageLoader.execute(request).drawable
            if (drawable != null) {
                Log.d(TAG, "✅ 이미지 프리로딩 성공: $imageUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 이미지 프리로딩 실패: $imageUrl", e)
        } finally {
            mutex.withLock {
                loadingUrls.remove(imageUrl)
            }
        }
    }

    /**
     * 썸네일 생성 및 캐싱
     */
    suspend fun generateThumbnail(imageUrl: String): String? {
        val thumbnailKey = getThumbnailKey(imageUrl)

        // 메모리 캐시 확인
        thumbnailMemoryCache.get(thumbnailKey)?.let { bitmap ->
            Log.d(TAG, "썸네일 메모리 캐시 히트: $imageUrl")
            return saveThumbnailToFile(thumbnailKey, bitmap)
        }

        // 디스크 캐시 확인
        val thumbnailFile = File(thumbnailCacheDir, "$thumbnailKey.webp")
        if (thumbnailFile.exists()) {
            Log.d(TAG, "썸네일 디스크 캐시 히트: $imageUrl")
            return thumbnailFile.absolutePath
        }

        // 썸네일 생성이 진행 중인지 확인
        mutex.withLock {
            if (thumbnailLoadingUrls.contains(imageUrl)) {
                // 로딩 완료까지 대기
                while (thumbnailLoadingUrls.contains(imageUrl)) {
                    mutex.unlock()
                    kotlinx.coroutines.delay(100)
                    mutex.lock()
                }
                return thumbnailFile.takeIf { it.exists() }?.absolutePath
            }
            thumbnailLoadingUrls.add(imageUrl)
        }

        return try {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .transformations(ThumbnailTransformation(THUMBNAIL_SIZE))
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()

                val result = imageLoader.execute(request)
                val drawable = result.drawable

                if (drawable != null) {
                    // drawable을 bitmap으로 변환
                    val bitmap = drawable.toBitmap(THUMBNAIL_SIZE, THUMBNAIL_SIZE)

                    // 메모리 캐시에 저장
                    thumbnailMemoryCache.put(thumbnailKey, bitmap)

                    // 디스크에 저장
                    val filePath = saveThumbnailToFile(thumbnailKey, bitmap)
                    Log.d(TAG, "✅ 썸네일 생성 완료: $imageUrl")
                    filePath
                } else {
                    Log.w(TAG, "⚠️ 썸네일 생성 실패: $imageUrl")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 썸네일 생성 중 오류: $imageUrl", e)
            null
        } finally {
            mutex.withLock {
                thumbnailLoadingUrls.remove(imageUrl)
            }
        }
    }

    /**
     * 썸네일을 파일로 저장
     */
    private fun saveThumbnailToFile(thumbnailKey: String, bitmap: Bitmap): String? {
        return try {
            val file = File(thumbnailCacheDir, "$thumbnailKey.webp")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "썸네일 파일 저장 실패", e)
            null
        }
    }

    /**
     * 이미지가 캐시되어 있는지 확인
     */
    suspend fun isImageCached(imageUrl: String): Boolean {
        // Coil의 캐시 상태 확인
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()

        // Coil 캐시 확인 로직 간소화
        val isMemoryCached = false // 실제 구현에서는 Coil 내부 캐시 API 활용 필요

        // 디스크 캐시는 Coil 내부에서 확인하기 어려우므로 메모리 캐시만 확인
        return isMemoryCached
    }

    /**
     * 특정 이미지 URL의 캐시를 클리어
     */
    suspend fun clearImageCache(imageUrl: String) {
        val thumbnailKey = getThumbnailKey(imageUrl)

        withContext(Dispatchers.IO) {
            // 메모리 캐시 클리어
            memoryCache.remove(imageUrl)
            thumbnailMemoryCache.remove(thumbnailKey)

            // 디스크 캐시 클리어
            File(thumbnailCacheDir, "$thumbnailKey.webp").delete()

            Log.d(TAG, "이미지 캐시 클리어: $imageUrl")
        }
    }

    /**
     * 오래된 캐시 정리 (7일 이상된 파일)
     */
    suspend fun cleanupOldCache() {
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7일
            var deletedCount = 0

            // 썸네일 캐시 정리
            thumbnailCacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            Log.d(TAG, "캐시 정리 완료: ${deletedCount}개 파일 삭제")
        }
    }

    /**
     * 캐시 사용량 정보 조회
     */
    fun getCacheInfo(): ChatImageCacheInfo {
        val memorySize = memoryCache.size()
        val thumbnailMemorySize = thumbnailMemoryCache.size()

        val diskCount = thumbnailCacheDir.listFiles()?.size ?: 0
        val diskSize = thumbnailCacheDir.listFiles()?.sumOf { it.length() } ?: 0L

        return ChatImageCacheInfo(
            memoryEntries = memorySize,
            thumbnailMemoryEntries = thumbnailMemorySize,
            diskEntries = diskCount,
            diskSizeBytes = diskSize,
            loadingCount = loadingUrls.size + thumbnailLoadingUrls.size
        )
    }

    /**
     * URL을 파일명으로 사용 가능한 해시로 변환
     */
    private fun getThumbnailKey(imageUrl: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hashBytes = digest.digest(imageUrl.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            imageUrl.hashCode().toString()
        }
    }
}

/**
 * 썸네일 생성을 위한 Coil Transform
 */
class ThumbnailTransformation(private val size: Int) : Transformation {

    override val cacheKey: String = "thumbnail_${size}x${size}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return Bitmap.createScaledBitmap(input, this.size, this.size, true)
    }
}

/**
 * 채팅 이미지 캐시 정보
 */
data class ChatImageCacheInfo(
    val memoryEntries: Int,
    val thumbnailMemoryEntries: Int,
    val diskEntries: Int,
    val diskSizeBytes: Long,
    val loadingCount: Int
) {
    val diskSizeMB: String = "%.1f MB".format(diskSizeBytes / 1024.0 / 1024.0)

    override fun toString(): String {
        return buildString {
            appendLine("=== Chat Image Cache Info ===")
            appendLine("Memory Entries: $memoryEntries")
            appendLine("Thumbnail Memory Entries: $thumbnailMemoryEntries")
            appendLine("Disk Entries: $diskEntries")
            appendLine("Disk Size: $diskSizeMB")
            appendLine("Currently Loading: $loadingCount")
        }
    }
}