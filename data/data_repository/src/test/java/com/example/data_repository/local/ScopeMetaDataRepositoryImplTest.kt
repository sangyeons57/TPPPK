package com.example.data_repository.local

import com.example.core_common.result.CustomResult
import com.example.data_datasource.local.ScopeMetadataDataSource
import com.example.data_datasource.local.SyncStatistics
import com.example.domain.model.sync.ScopeMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ScopeMetaDataRepositoryImplTest {

    private lateinit var scopeMetadataDataSource: ScopeMetadataDataSource
    private lateinit var repository: ScopeMetaDataRepositoryImpl

    @Before
    fun setUp() {
        scopeMetadataDataSource = mockk()
        repository = ScopeMetaDataRepositoryImpl(scopeMetadataDataSource)
    }

    @Test
    fun `save should delegate to data source`() = runTest {
        // Given
        val scopeMetadata = ScopeMetadata.create("test_key", Instant.now())
        coEvery { scopeMetadataDataSource.save(scopeMetadata) } returns CustomResult.Success(Unit)

        // When
        val result = repository.save(scopeMetadata)

        // Then
        assertTrue(result is CustomResult.Success)
        coVerify { scopeMetadataDataSource.save(scopeMetadata) }
    }

    @Test
    fun `findByKey should delegate to data source`() = runTest {
        // Given
        val key = "test_key"
        val expected = ScopeMetadata.create(key, Instant.now())
        coEvery { scopeMetadataDataSource.getByKey(key) } returns CustomResult.Success(expected)

        // When
        val result = repository.findByKey(key)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expected, (result as CustomResult.Success).data)
        coVerify { scopeMetadataDataSource.getByKey(key) }
    }

    @Test
    fun `findSyncedAfter should convert Instant to epochMilli`() = runTest {
        // Given
        val afterTimestamp = Instant.now()
        val expected = listOf(ScopeMetadata.create("test", Instant.now()))
        coEvery {
            scopeMetadataDataSource.getSyncedAfter(afterTimestamp.toEpochMilli())
        } returns CustomResult.Success(expected)

        // When
        val result = repository.findSyncedAfter(afterTimestamp)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expected, (result as CustomResult.Success).data)
        coVerify { scopeMetadataDataSource.getSyncedAfter(afterTimestamp.toEpochMilli()) }
    }

    @Test
    fun `getSyncStatistics should convert data source statistics to domain model`() = runTest {
        // Given
        val dataSourceStats = SyncStatistics(
            totalCount = 5,
            totalSyncCount = 100L,
            avgSyncCount = 20.0,
            totalErrorCount = 2L,
            avgErrorCount = 0.4,
            lastSyncTimeMs = 1234567890L
        )
        coEvery { scopeMetadataDataSource.getSyncStatistics() } returns CustomResult.Success(
            dataSourceStats
        )

        // When
        val result = repository.getSyncStatistics()

        // Then
        assertTrue(result is CustomResult.Success)
        val domainStats = (result as CustomResult.Success).data
        assertEquals(5, domainStats.totalCount)
        assertEquals(100L, domainStats.totalSyncCount)
        assertEquals(20.0, domainStats.avgSyncCount, 0.001)
        assertEquals(2L, domainStats.totalErrorCount)
        assertEquals(0.4, domainStats.avgErrorCount, 0.001)
        assertEquals(Instant.ofEpochMilli(1234567890L), domainStats.lastSyncTime)
    }

    @Test
    fun `getSyncStatistics should handle null lastSyncTimeMs`() = runTest {
        // Given
        val dataSourceStats = SyncStatistics(
            totalCount = 0,
            totalSyncCount = 0L,
            avgSyncCount = 0.0,
            totalErrorCount = 0L,
            avgErrorCount = 0.0,
            lastSyncTimeMs = null
        )
        coEvery { scopeMetadataDataSource.getSyncStatistics() } returns CustomResult.Success(
            dataSourceStats
        )

        // When
        val result = repository.getSyncStatistics()

        // Then
        assertTrue(result is CustomResult.Success)
        val domainStats = (result as CustomResult.Success).data
        assertEquals(null, domainStats.lastSyncTime)
    }

    @Test
    fun `deleteOldMetadata should convert Instant to epochMilli`() = runTest {
        // Given
        val beforeTimestamp = Instant.now()
        val expectedCount = 3
        coEvery {
            scopeMetadataDataSource.deleteOldMetadata(beforeTimestamp.toEpochMilli())
        } returns CustomResult.Success(expectedCount)

        // When
        val result = repository.deleteOldMetadata(beforeTimestamp)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedCount, (result as CustomResult.Success).data)
        coVerify { scopeMetadataDataSource.deleteOldMetadata(beforeTimestamp.toEpochMilli()) }
    }

    @Test
    fun `findActiveSyncMetadata should convert Instant to epochMilli`() = runTest {
        // Given
        val afterTimestamp = Instant.now()
        val minSyncCount = 5L
        val expected = listOf(ScopeMetadata.create("test", Instant.now()))
        coEvery {
            scopeMetadataDataSource.getActiveSyncMetadata(
                minSyncCount,
                afterTimestamp.toEpochMilli()
            )
        } returns CustomResult.Success(expected)

        // When
        val result = repository.findActiveSyncMetadata(minSyncCount, afterTimestamp)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expected, (result as CustomResult.Success).data)
        coVerify {
            scopeMetadataDataSource.getActiveSyncMetadata(
                minSyncCount,
                afterTimestamp.toEpochMilli()
            )
        }
    }
}