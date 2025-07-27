package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.UsersEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Users collection
 * Provides CRUD operations for user data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface UsersDao {

    // === Basic CRUD Operations ===

    /**
     * Get user by ID
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UsersEntity?

    /**
     * Get user by email
     */
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UsersEntity?

    /**
     * Get all users
     */
    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllUsers(): List<UsersEntity>

    /**
     * Insert or update user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UsersEntity)

    /**
     * Insert or update multiple users
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UsersEntity>)

    /**
     * Update user
     */
    @Update
    suspend fun updateUser(user: UsersEntity)

    /**
     * Delete user by ID
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    // === Flow-based Real-time Observations ===

    /**
     * Observe user by ID
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: String): Flow<UsersEntity?>

    /**
     * Observe all users
     */
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun observeAllUsers(): Flow<List<UsersEntity>>

    /**
     * Observe users by account status
     */
    @Query("SELECT * FROM users WHERE accountStatus = :status ORDER BY name ASC")
    fun observeUsersByStatus(status: String): Flow<List<UsersEntity>>

    // === Incremental Sync Queries ===

    /**
     * Get users updated after specific timestamp (for incremental sync)
     * 🔑 Core sync method: fetches users changed since last sync
     */
    @Query("SELECT * FROM users WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getUsersUpdatedAfter(timestamp: Instant): List<UsersEntity>

    /**
     * Get latest update timestamp for sync tracking
     */
    @Query("SELECT MAX(updatedAt) FROM users")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Search and Filter Queries ===

    /**
     * Search users by name
     */
    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchTerm || '%' ORDER BY name ASC")
    suspend fun searchUsersByName(searchTerm: String): List<UsersEntity>

    /**
     * Get users by account status
     */
    @Query("SELECT * FROM users WHERE accountStatus = :status ORDER BY name ASC")
    suspend fun getUsersByAccountStatus(status: String): List<UsersEntity>

    /**
     * Get online users
     */
    @Query("SELECT * FROM users WHERE userStatus = 'ONLINE' ORDER BY name ASC")
    suspend fun getOnlineUsers(): List<UsersEntity>

    // === Utility Queries ===

    /**
     * Check if user exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)")
    suspend fun userExists(userId: String): Boolean

    /**
     * Get total user count
     */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    /**
     * Get user count by status
     */
    @Query("SELECT COUNT(*) FROM users WHERE accountStatus = :status")
    suspend fun getUserCountByStatus(status: String): Int

    // === Cleanup Operations ===

    /**
     * Delete all users (for cache reset)
     */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    /**
     * Delete users with specific account status
     */
    @Query("DELETE FROM users WHERE accountStatus = :status")
    suspend fun deleteUsersByStatus(status: String)
}