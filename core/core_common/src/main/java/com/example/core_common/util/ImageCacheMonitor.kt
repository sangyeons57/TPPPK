package com.example.core_common.util

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이미지 캐시 상태 모니터링을 위한 헬퍼 클래스
 * Coil ImageLoader의 메모리 및 디스크 캐시 상태를 추적하고 관리합니다.
 */
@OptIn(ExperimentalCoilApi::class)
@Singleton
class ImageCacheMonitor @Inject constructor(
    private val imageLoader: ImageLoader
) {

    /**
     * 현재 캐시 상태 정보를 반환합니다.
     */
    fun getCacheStats(): String {
        val memoryCache = imageLoader.memoryCache
        val diskCache = imageLoader.diskCache

        val memorySize = (memoryCache?.size as? Long) ?: 0L
        val memoryMaxSize = (memoryCache?.maxSize as? Long) ?: 0L
        val diskSize = (diskCache?.size as? Long) ?: 0L
        val diskMaxSize = (diskCache?.maxSize as? Long) ?: 0L

        return buildString {
            appendLine("=== 이미지 캐시 상태 ===")
            appendLine("메모리 캐시: ${formatBytes(memorySize)} / ${formatBytes(memoryMaxSize)}")
            appendLine("디스크 캐시: ${formatBytes(diskSize)} / ${formatBytes(diskMaxSize)}")
            appendLine(
                "메모리 사용률: ${
                    if (memoryMaxSize > 0) String.format(
                        "%.1f%%",
                        (memorySize.toDouble() / memoryMaxSize.toDouble()) * 100
                    ) else "N/A"
                }"
            )
            appendLine(
                "디스크 사용률: ${
                    if (diskMaxSize > 0) String.format(
                        "%.1f%%",
                        (diskSize.toDouble() / diskMaxSize.toDouble()) * 100
                    ) else "N/A"
                }"
            )
        }
    }

    /**
     * 바이트를 읽기 쉬운 형태로 포맷합니다.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * 메모리 캐시를 지웁니다.
     */
    fun clearMemoryCache() {
        imageLoader.memoryCache?.clear()
    }

    /**
     * 특정 URL의 캐시를 지웁니다.
     */
    suspend fun clearUrlFromCache(url: String) {
        // 메모리 캐시에서 제거
        imageLoader.memoryCache?.remove(MemoryCache.Key(url))
        // 디스크 캐시에서 제거
        imageLoader.diskCache?.remove(url)
    }
}