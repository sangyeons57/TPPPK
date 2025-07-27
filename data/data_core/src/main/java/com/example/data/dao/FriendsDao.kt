package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.FriendsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Friends collection
 * Provides CRUD operations for friend data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface FriendsDao {

    // === Basic CRUD Operations ===

    @Query("SELECT * FROM friends WHERE id = :friendId")
    suspend fun getFriendById(friendId: String): FriendsEntity?

    @Query("SELECT * FROM friends ORDER BY name ASC")
    suspend fun getAllFriends(): List<FriendsEntity>

    @Query("SELECT * FROM friends WHERE status = :status ORDER BY name ASC")
    suspend fun getFriendsByStatus(status: String): List<FriendsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendsEntity>)

    @Update
    suspend fun updateFriend(friend: FriendsEntity)

    @Query("DELETE FROM friends WHERE id = :friendId")
    suspend fun deleteFriend(friendId: String)

    // === Flow-based Real-time Observations ===

    @Query("SELECT * FROM friends WHERE id = :friendId")
    fun observeFriendById(friendId: String): Flow<FriendsEntity?>

    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun observeAllFriends(): Flow<List<FriendsEntity>>

    @Query("SELECT * FROM friends WHERE status = :status ORDER BY name ASC")
    fun observeFriendsByStatus(status: String): Flow<List<FriendsEntity>>

    @Query("SELECT * FROM friends WHERE status = 'ACCEPTED' ORDER BY name ASC")
    fun observeAcceptedFriends(): Flow<List<FriendsEntity>>

    @Query("SELECT * FROM friends WHERE status = 'PENDING' ORDER BY requestedAt DESC")
    fun observePendingFriendRequests(): Flow<List<FriendsEntity>>

    // === Incremental Sync Queries ===

    @Query("SELECT * FROM friends WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getFriendsUpdatedAfter(timestamp: Instant): List<FriendsEntity>

    @Query("SELECT MAX(updatedAt) FROM friends")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Friend Status Management ===

    @Query("UPDATE friends SET status = :newStatus WHERE id = :friendId")
    suspend fun updateFriendStatus(friendId: String, newStatus: String)

    @Query("UPDATE friends SET status = :newStatus, acceptedAt = :acceptedAt WHERE id = :friendId")
    suspend fun acceptFriendRequest(friendId: String, newStatus: String, acceptedAt: Instant)

    // === Search and Filter Queries ===

    @Query("SELECT * FROM friends WHERE name LIKE '%' || :searchTerm || '%' ORDER BY name ASC")
    suspend fun searchFriendsByName(searchTerm: String): List<FriendsEntity>

    @Query("SELECT * FROM friends WHERE status = 'ACCEPTED' ORDER BY name ASC")
    suspend fun getAcceptedFriends(): List<FriendsEntity>

    @Query("SELECT * FROM friends WHERE status = 'PENDING' ORDER BY requestedAt DESC")
    suspend fun getPendingFriendRequests(): List<FriendsEntity>

    @Query("SELECT * FROM friends WHERE status = 'REQUESTED' ORDER BY requestedAt DESC")
    suspend fun getSentFriendRequests(): List<FriendsEntity>

    @Query("SELECT * FROM friends WHERE status = 'BLOCKED' ORDER BY updatedAt DESC")
    suspend fun getBlockedFriends(): List<FriendsEntity>

    // === Utility Queries ===

    @Query("SELECT EXISTS(SELECT 1 FROM friends WHERE id = :friendId)")
    suspend fun friendExists(friendId: String): Boolean

    @Query("SELECT COUNT(*) FROM friends")
    suspend fun getFriendCount(): Int

    @Query("SELECT COUNT(*) FROM friends WHERE status = :status")
    suspend fun getFriendCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM friends WHERE status = 'ACCEPTED'")
    suspend fun getAcceptedFriendCount(): Int

    @Query("SELECT COUNT(*) FROM friends WHERE status = 'PENDING'")
    suspend fun getPendingRequestCount(): Int

    // === Cleanup Operations ===

    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()

    @Query("DELETE FROM friends WHERE status = :status")
    suspend fun deleteFriendsByStatus(status: String)
}