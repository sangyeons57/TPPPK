package com.example.data_core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.CategoriesEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CategoriesDao {

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoriesEntity?

    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun getCategoriesByProject(projectId: String): List<CategoriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoriesEntity>)

    @Update
    suspend fun updateCategory(category: CategoriesEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY `order` ASC")
    fun observeCategoriesByProject(projectId: String): Flow<List<CategoriesEntity>>

    @Query("SELECT * FROM categories WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getCategoriesUpdatedAfter(timestamp: Instant): List<CategoriesEntity>


    @Query("SELECT MAX(`order`) FROM categories WHERE projectId = :projectId")
    suspend fun getMaxOrderInProject(projectId: String): Int?

    @Query("SELECT COUNT(*) FROM categories WHERE projectId = :projectId")
    suspend fun getCategoryCountByProject(projectId: String): Int

    @Query("DELETE FROM categories WHERE projectId = :projectId")
    suspend fun deleteCategoriesByProject(projectId: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    suspend fun getAllCategories(): List<CategoriesEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun observeCategoryById(categoryId: String): Flow<CategoriesEntity?>

    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    fun observeAllCategories(): Flow<List<CategoriesEntity>>
}