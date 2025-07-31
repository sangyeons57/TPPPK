package com.example.domain_usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import com.example.domain.model.sync.OutBoxPayload
import com.example.domain.model.sync.ScopeMetadata
import com.example.domain.util.RetryManager
import com.example.domain.util.RetryResult
import com.example.domain_repository.local.OutBoxRepository
import com.example.domain_repository.local.ScopeMetaDataRepository
import com.example.domain_repository.local.SyncStatisticsData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SyncManagerImplTest {

    private lateinit var outBoxRepository: OutBoxRepository
    private lateinit var scopeMetaDataRepository: ScopeMetaDataRepository
    private lateinit var retryManager: RetryManager
    private lateinit var syncManager: SyncManagerImpl

    @Before
    fun setUp() {
        outBoxRepository = mockk()
        scopeMetaDataRepository = mockk()
        retryManager = mockk()
        syncManager = SyncManagerImpl(outBoxRepository, scopeMetaDataRepository, retryManager)
    }

    @Test
    fun `processOutBoxQueue should return empty result when no pending operations`() = runTest {
        // Given
        coEvery { outBoxRepository.getPendingOperations(10) } returns CustomResult.Success(emptyList())

        // When
        val result = syncManager.processOutBoxQueue(10)

        // Then
        assertTrue(result is CustomResult.Success)
        val syncResult = (result as CustomResult.Success).data
        assertEquals(0, syncResult.totalProcessed)
        assertEquals(0, syncResult.successCount)
        assertEquals(0, syncResult.failureCount)
        assertEquals(0, syncResult.skippedCount)
        assertTrue(syncResult.errors.isEmpty())
    }

    @Test
    fun `updateSyncScope should create new metadata when not exists`() = runTest {
        // Given
        val scopeKey = "test_scope"
        val cursor = Instant.now()
        coEvery { scopeMetaDataRepository.findByKey(scopeKey) } returns CustomResult.Success(null)
        coEvery { scopeMetaDataRepository.save(any()) } returns CustomResult.Success(Unit)
        coEvery {
            retryManager.executeWithCustomRetry(
                any(),
                any(),
                any(),
                any<suspend (Int) -> CustomResult<Unit, Exception>>()
            )
        } answers {
            val operation = arg<suspend (Int) -> CustomResult<Unit, Exception>>(3)
            RetryResult.Success(operation(0), 1)
        }

        // When
        val result = syncManager.updateSyncScope(scopeKey, cursor)

        // Then
        assertTrue(result is CustomResult.Success)
        coVerify { scopeMetaDataRepository.findByKey(scopeKey) }
        coVerify { scopeMetaDataRepository.save(any()) }
    }

    @Test
    fun `updateSyncScope should update existing metadata`() = runTest {
        // Given
        val scopeKey = "test_scope"
        val cursor = Instant.now()
        val existingMetadata = mockk<ScopeMetadata>(relaxed = true)
        coEvery { scopeMetaDataRepository.findByKey(scopeKey) } returns CustomResult.Success(
            existingMetadata
        )
        coEvery { scopeMetaDataRepository.save(existingMetadata) } returns CustomResult.Success(Unit)
        coEvery {
            retryManager.executeWithCustomRetry(
                any(),
                any(),
                any(),
                any<suspend (Int) -> CustomResult<Unit, Exception>>()
            )
        } answers {
            val operation = arg<suspend (Int) -> CustomResult<Unit, Exception>>(3)
            RetryResult.Success(operation(0), 1)
        }

        // When
        val result = syncManager.updateSyncScope(scopeKey, cursor)

        // Then
        assertTrue(result is CustomResult.Success)
        coVerify { existingMetadata.updateCursor(cursor) }
        coVerify { scopeMetaDataRepository.save(existingMetadata) }
    }

    @Test
    fun `getSyncStatus should delegate to repository`() = runTest {
        // Given
        val scopeKey = "test_scope"
        val expected = ScopeMetadata.create(scopeKey, Instant.now())
        coEvery { scopeMetaDataRepository.findByKey(scopeKey) } returns CustomResult.Success(
            expected
        )

        // When
        val result = syncManager.getSyncStatus(scopeKey)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expected, (result as CustomResult.Success).data)
        coVerify { scopeMetaDataRepository.findByKey(scopeKey) }
    }

    @Test
    fun `resetFailedOperations should delegate to outBoxRepository`() = runTest {
        // Given
        val expectedCount = 5
        coEvery { outBoxRepository.resetRetryableFailedOperations() } returns CustomResult.Success(
            expectedCount
        )

        // When
        val result = syncManager.resetFailedOperations()

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedCount, (result as CustomResult.Success).data)
        coVerify { outBoxRepository.resetRetryableFailedOperations() }
    }

    @Test
    fun `recordSyncError should create metadata if not exists and record error`() = runTest {
        // Given
        val scopeKey = "test_scope"
        val errorMessage = "Test error"
        coEvery { scopeMetaDataRepository.findByKey(scopeKey) } returns CustomResult.Success(null)
        coEvery { scopeMetaDataRepository.save(any()) } returns CustomResult.Success(Unit)
        coEvery {
            retryManager.executeWithCustomRetry(
                any(),
                any(),
                any(),
                any<suspend (Int) -> CustomResult<Unit, Exception>>()
            )
        } answers {
            val operation = arg<suspend (Int) -> CustomResult<Unit, Exception>>(3)
            RetryResult.Success(operation(0), 1)
        }

        // When
        val result = syncManager.recordSyncError(scopeKey, errorMessage)

        // Then
        assertTrue(result is CustomResult.Success)
        coVerify { scopeMetaDataRepository.findByKey(scopeKey) }
        coVerify { scopeMetaDataRepository.save(any()) }
    }

    @Test
    fun `getSyncStatusSummary should aggregate data from multiple sources`() = runTest {
        // Given
        val pendingCount = 5
        val failedCount = 2
        val completedCount = 10
        val syncStats = SyncStatisticsData(
            totalCount = 3,
            totalSyncCount = 100L,
            avgSyncCount = 33.33,
            totalErrorCount = 2L,
            avgErrorCount = 0.67,
            lastSyncTime = Instant.now()
        )
        val errorScopes = listOf(ScopeMetadata.create("error_scope", Instant.now()))

        coEvery { outBoxRepository.getCountByStatus(OutBoxStatus.PENDING) } returns CustomResult.Success(
            pendingCount
        )
        coEvery { outBoxRepository.getCountByStatus(OutBoxStatus.FAILED) } returns CustomResult.Success(
            failedCount
        )
        coEvery { outBoxRepository.getCountByStatus(OutBoxStatus.COMPLETED) } returns CustomResult.Success(
            completedCount
        )
        coEvery { scopeMetaDataRepository.getSyncStatistics() } returns CustomResult.Success(
            syncStats
        )
        coEvery { scopeMetaDataRepository.findErrorMetadata() } returns CustomResult.Success(
            errorScopes
        )
        coEvery {
            retryManager.executeWithCustomRetry(
                any(),
                any(),
                any(),
                any<suspend (Int) -> CustomResult<SyncStatusSummary, Exception>>()
            )
        } answers {
            val operation = arg<suspend (Int) -> CustomResult<SyncStatusSummary, Exception>>(3)
            RetryResult.Success(operation(0), 1)
        }

        // When
        val result = syncManager.getSyncStatusSummary()

        // Then
        assertTrue(result is CustomResult.Success)
        val summary = (result as CustomResult.Success).data
        assertEquals(pendingCount, summary.pendingOperationsCount)
        assertEquals(failedCount, summary.failedOperationsCount)
        assertEquals(completedCount, summary.completedOperationsCount)
        assertEquals(3, summary.activeScopesCount)
        assertEquals(1, summary.errorScopesCount)
        assertEquals(100L, summary.totalSyncOperations)
        assertEquals(syncStats.lastSyncTime, summary.lastSyncAt)
    }

    @Test
    fun `getErrorScopes should delegate to repository`() = runTest {
        // Given
        val minErrorCount = 3L
        val expected = listOf(ScopeMetadata.create("error_scope", Instant.now()))
        coEvery { scopeMetaDataRepository.findErrorMetadata(minErrorCount) } returns CustomResult.Success(
            expected
        )

        // When
        val result = syncManager.getErrorScopes(minErrorCount)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expected, (result as CustomResult.Success).data)
        coVerify { scopeMetaDataRepository.findErrorMetadata(minErrorCount) }
    }

    @Test
    fun `cleanupCompletedOperations should delegate to outBoxRepository`() = runTest {
        // Given
        val expectedCount = 10
        coEvery { outBoxRepository.deleteCompleted() } returns CustomResult.Success(expectedCount)

        // When
        val result = syncManager.cleanupCompletedOperations()

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedCount, (result as CustomResult.Success).data)
        coVerify { outBoxRepository.deleteCompleted() }
    }

    @Test
    fun `cleanupExpiredOperations should delegate to outBoxRepository`() = runTest {
        // Given
        val timeoutMs = 60000L
        val expectedCount = 3
        coEvery { outBoxRepository.deleteExpiredOperations(timeoutMs) } returns CustomResult.Success(
            expectedCount
        )

        // When
        val result = syncManager.cleanupExpiredOperations(timeoutMs)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedCount, (result as CustomResult.Success).data)
        coVerify { outBoxRepository.deleteExpiredOperations(timeoutMs) }
    }

    @Test
    fun `cleanupOldMetadata should delegate to scopeMetaDataRepository`() = runTest {
        // Given
        val beforeTimestamp = Instant.now()
        val expectedCount = 2
        coEvery { scopeMetaDataRepository.deleteOldMetadata(beforeTimestamp) } returns CustomResult.Success(
            expectedCount
        )

        // When
        val result = syncManager.cleanupOldMetadata(beforeTimestamp)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedCount, (result as CustomResult.Success).data)
        coVerify { scopeMetaDataRepository.deleteOldMetadata(beforeTimestamp) }
    }
}