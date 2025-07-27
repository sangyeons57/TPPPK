package com.example.data.util

import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
// import timber.log.Timber - Data 모듈에서 제거됨
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 리소스 정리 타입
 */
enum class ResourceType {
    COMPRESSED_IMAGE,
    THUMBNAIL,
    TEMP_CACHE,
    UPLOAD_CHUNK
}

/**
 * 관리되는 리소스 정보
 */
data class ManagedResource(
    val file: File,
    val type: ResourceType,
    val createdAt: Long = System.currentTimeMillis(),
    val associatedUploadId: String? = null
)

/**
 * 업로드 과정에서 생성되는 임시 리소스를 안전하게 관리하는 클래스
 * 
 * 주요 기능:
 * - 임시 파일 추적 및 자동 정리
 * - 메모리 누수 방지
 * - 앱 종료시 리소스 정리
 * - 업로드별 리소스 그룹 관리
 */
@Singleton
class UploadResourceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val managedResources = ConcurrentHashMap<String, ManagedResource>()
    private val resourceLock = Mutex()
    
    // 리소스 생존 시간 (30분)
    private val resourceTtlMs = 30 * 60 * 1000L
    
    // 최대 관리할 리소스 수 (메모리 보호)
    private val maxResourceCount = 100

    /**
     * 관리되는 임시 파일을 생성합니다.
     */
    suspend fun createManagedTempFile(
        prefix: String,
        suffix: String = ".tmp",
        type: ResourceType = ResourceType.TEMP_CACHE,
        uploadId: String? = null
    ): CustomResult<File, Exception> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
            val resourceId = generateResourceId(tempFile)
            
            val managedResource = ManagedResource(
                file = tempFile,
                type = type,
                associatedUploadId = uploadId
            )
            
            resourceLock.withLock {
                // 최대 리소스 수 체크
                if (managedResources.size >= maxResourceCount) {
                    cleanupExpiredResources()
                }
                
                managedResources[resourceId] = managedResource
            }
            
            println("DEBUG: Created managed temp file: ${tempFile.absolutePath}")
            CustomResult.Success(tempFile)
            
        } catch (e: Exception) {
            println("ERROR: Failed to create managed temp file: ${e.message}")
            CustomResult.Failure(e)
        }
    }

    /**
     * 특정 파일을 관리 대상에 추가합니다.
     */
    suspend fun trackResource(
        file: File,
        type: ResourceType,
        uploadId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val resourceId = generateResourceId(file)
        val managedResource = ManagedResource(
            file = file,
            type = type,
            associatedUploadId = uploadId
        )
        
        resourceLock.withLock {
            managedResources[resourceId] = managedResource
        }
        
        println("DEBUG: Started tracking resource: ${file.absolutePath}")
        resourceId
    }

    /**
     * URI로부터 관리되는 임시 파일을 생성합니다.
     */
    suspend fun createManagedTempFileFromUri(
        sourceUri: Uri,
        prefix: String,
        type: ResourceType = ResourceType.TEMP_CACHE,
        uploadId: String? = null
    ): CustomResult<File, Exception> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile(prefix, null, context.cacheDir)
            
            // URI 내용을 임시 파일로 복사
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext CustomResult.Failure(
                Exception("Cannot open input stream for URI: $sourceUri")
            )
            
            val resourceId = generateResourceId(tempFile)
            val managedResource = ManagedResource(
                file = tempFile,
                type = type,
                associatedUploadId = uploadId
            )
            
            resourceLock.withLock {
                managedResources[resourceId] = managedResource
            }
            
            CustomResult.Success(tempFile)
            
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 특정 리소스를 정리합니다.
     */
    suspend fun cleanupResource(resourceId: String): Boolean = withContext(Dispatchers.IO) {
        resourceLock.withLock {
            val resource = managedResources.remove(resourceId)
            resource?.let { managedResource ->
                try {
                    if (managedResource.file.exists()) {
                        val deleted = managedResource.file.delete()
                        if (deleted) {
                            println("DEBUG: Cleaned up resource: ${managedResource.file.absolutePath}")
                        } else {
                            println("WARNING: Failed to delete file: ${managedResource.file.absolutePath}")
                        }
                        deleted
                    } else {
                        true // 파일이 이미 없으면 성공으로 간주
                    }
                } catch (e: Exception) {
                    println("ERROR: Error cleaning up resource: ${managedResource.file.absolutePath}: ${e.message}")
                    false
                }
            } ?: false
        }
    }

    /**
     * 파일로부터 리소스 ID를 찾아서 정리합니다.
     */
    suspend fun cleanupResourceByFile(file: File): Boolean {
        val resourceId = generateResourceId(file)
        return cleanupResource(resourceId)
    }

    /**
     * 특정 업로드와 관련된 모든 리소스를 정리합니다.
     */
    suspend fun cleanupUploadResources(uploadId: String): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        resourceLock.withLock {
            val resourcesToClean = managedResources.filterValues { resource ->
                resource.associatedUploadId == uploadId
            }
            
            resourcesToClean.forEach { (resourceId, resource) ->
                try {
                    if (resource.file.exists() && resource.file.delete()) {
                        managedResources.remove(resourceId)
                        cleanedCount++
                        println("DEBUG: Cleaned up upload resource: ${resource.file.absolutePath}")
                    }
                } catch (e: Exception) {
                    println("ERROR: Error cleaning up upload resource: ${resource.file.absolutePath}: ${e.message}")
                }
            }
        }
        
        println("DEBUG: Cleaned up $cleanedCount resources for upload: $uploadId")
        cleanedCount
    }

    /**
     * 만료된 리소스들을 정리합니다.
     */
    suspend fun cleanupExpiredResources(): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        val currentTime = System.currentTimeMillis()
        
        resourceLock.withLock {
            val expiredResources = managedResources.filterValues { resource ->
                (currentTime - resource.createdAt) > resourceTtlMs
            }
            
            expiredResources.forEach { (resourceId, resource) ->
                try {
                    if (resource.file.exists() && resource.file.delete()) {
                        managedResources.remove(resourceId)
                        cleanedCount++
                        println("DEBUG: Cleaned up expired resource: ${resource.file.absolutePath}")
                    }
                } catch (e: Exception) {
                    println("ERROR: Error cleaning up expired resource: ${resource.file.absolutePath}: ${e.message}")
                }
            }
        }
        
        if (cleanedCount > 0) {
            println("DEBUG: Cleaned up $cleanedCount expired resources")
        }
        cleanedCount
    }

    /**
     * 모든 관리되는 리소스를 정리합니다.
     * 앱 종료시나 메모리 부족시 사용됩니다.
     */
    suspend fun cleanupAllResources(): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        resourceLock.withLock {
            managedResources.values.forEach { resource ->
                try {
                    if (resource.file.exists() && resource.file.delete()) {
                        cleanedCount++
                        println("DEBUG: Cleaned up resource: ${resource.file.absolutePath}")
                    }
                } catch (e: Exception) {
                    println("ERROR: Error cleaning up resource: ${resource.file.absolutePath}: ${e.message}")
                }
            }
            managedResources.clear()
        }
        
        println("DEBUG: Cleaned up all $cleanedCount managed resources")
        cleanedCount
    }

    /**
     * 관리 중인 리소스 통계를 반환합니다.
     */
    suspend fun getResourceStats(): ResourceStats = withContext(Dispatchers.IO) {
        resourceLock.withLock {
            val typeCount = managedResources.values.groupingBy { it.type }.eachCount()
            val totalSize = managedResources.values.sumOf { resource ->
                try {
                    if (resource.file.exists()) resource.file.length() else 0L
                } catch (e: Exception) {
                    0L
                }
            }
            
            ResourceStats(
                totalCount = managedResources.size,
                typeCount = typeCount,
                totalSizeBytes = totalSize,
                oldestResourceAge = managedResources.values.minOfOrNull { 
                    System.currentTimeMillis() - it.createdAt 
                } ?: 0L
            )
        }
    }

    /**
     * 정기적인 정리 작업을 수행합니다.
     * 백그라운드에서 주기적으로 호출되어야 합니다.
     */
    suspend fun performMaintenanceCleanup() {
        try {
            val expiredCount = cleanupExpiredResources()
            val stats = getResourceStats()
            
            println("DEBUG: Maintenance cleanup completed: $expiredCount expired resources cleaned, ${stats.totalCount} remaining")
            
            // 너무 많은 리소스가 있으면 강제 정리
            if (stats.totalCount > maxResourceCount * 0.8) {
                println("WARNING: Too many resources (${stats.totalCount}), performing aggressive cleanup")
                cleanupOldestResources(maxResourceCount / 2)
            }
        } catch (e: Exception) {
            println("ERROR: Error during maintenance cleanup: ${e.message}")
        }
    }

    // === Private Helper Methods ===

    private fun generateResourceId(file: File): String {
        return "${file.name}_${file.absolutePath.hashCode()}"
    }

    private suspend fun cleanupOldestResources(count: Int): Int = withContext(Dispatchers.IO) {
        var cleanedCount = 0
        
        resourceLock.withLock {
            val sortedResources = managedResources.entries.sortedBy { (_, resource) ->
                resource.createdAt
            }
            
            sortedResources.take(count).forEach { (resourceId, resource) ->
                try {
                    if (resource.file.exists() && resource.file.delete()) {
                        managedResources.remove(resourceId)
                        cleanedCount++
                    }
                } catch (e: Exception) {
                    println("ERROR: Error cleaning up old resource: ${e.message}")
                }
            }
        }
        
        cleanedCount
    }
}

/**
 * 리소스 통계 정보
 */
data class ResourceStats(
    val totalCount: Int,
    val typeCount: Map<ResourceType, Int>,
    val totalSizeBytes: Long,
    val oldestResourceAge: Long
) {
    val totalSizeMB: Double get() = totalSizeBytes / (1024.0 * 1024.0)
}