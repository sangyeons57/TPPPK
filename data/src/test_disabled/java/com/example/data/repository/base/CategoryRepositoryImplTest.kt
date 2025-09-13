package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.domain.model.base.Category
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.every
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepositoryImplTest {

    private lateinit var remoteDataSource: CategoryRemoteDataSource
    private lateinit var repository: CategoryRepositoryImpl
    private lateinit var context: CategoryRepositoryFactoryContext

    @Before
    fun setUp() {
        remoteDataSource = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        context = CategoryRepositoryFactoryContext(CollectionPath("projects/test/categories"))
        repository = CategoryRepositoryImpl(remoteDataSource, context)
    }

    @Test
    fun `save new category delegates to create`() = runTest {
        val category = Category.create(
            name = CategoryName("Test"),
            order = CategoryOrder.of(1.0),
            createdBy = OwnerId("owner")
        )
        val expectedId = DocumentId("123")
        coEvery { remoteDataSource.create(category.toDto()) } returns CustomResult.Success(expectedId)

        val result = repository.save(category)

        coVerify { remoteDataSource.setCollection(context.collectionPath) }
        coVerify { remoteDataSource.create(category.toDto()) }
        assertEquals(expectedId, (result as CustomResult.Success).data)
    }

    @Test
    fun `save existing category delegates to update`() = runTest {
        val category = Category.fromDataSource(
            id = DocumentId("1"),
            name = CategoryName("Existing"),
            order = CategoryOrder.of(2.0),
            createdBy = OwnerId("owner"),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            isCategory = IsCategoryFlag.TRUE
        )
        category.changeName(CategoryName("Updated"))
        val changedFields = category.getChangedFields()
        coEvery { remoteDataSource.update(category.id, changedFields) } returns CustomResult.Success(category.id)

        val result = repository.save(category)

        coVerify { remoteDataSource.setCollection(context.collectionPath) }
        coVerify { remoteDataSource.update(category.id, changedFields) }
        assertEquals(category.id, (result as CustomResult.Success).data)
    }
}

