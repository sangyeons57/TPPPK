package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.local.FriendEntity
import com.example.data.model.local.FriendRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * 친구 및 친구 요청 관련 데이터베이스 작업을 위한 DAO 인터페이스
 */
@Dao
interface FriendDao {
    // --- 친구 관련 쿼리 ---
    
    /**
     * 모든 친구 목록을 가져옵니다.
     * @return 친구 목록의 Flow
     */
    @Query("SELECT * FROM friends WHERE status = 'accepted' ORDER BY nickname ASC")
    fun getAllFriendsStream(): Flow<List<FriendEntity>>
    
    /**
     * 특정 상태의 친구 목록을 가져옵니다.
     * @param status 친구 관계 상태 (accepted, pending_sent, pending_received)
     * @return 지정된 상태의 친구 목록
     */
    @Query("SELECT * FROM friends WHERE status = :status ORDER BY nickname ASC")
    suspend fun getFriendsByStatus(status: String): List<FriendEntity>
    
    /**
     * 특정 ID의 친구 정보를 가져옵니다.
     * @param friendId 친구 ID
     * @return 친구 정보 또는 null
     */
    @Query("SELECT * FROM friends WHERE id = :friendId LIMIT 1")
    suspend fun getFriendById(friendId: String): FriendEntity?
    
    /**
     * 친구 정보를 추가하거나 업데이트합니다.
     * @param friend 추가/업데이트할 친구 정보
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFriend(friend: FriendEntity)
    
    /**
     * 친구 목록을 추가하거나 업데이트합니다.
     * @param friends 추가/업데이트할 친구 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFriends(friends: List<FriendEntity>)
    
    /**
     * 친구 정보를 삭제합니다.
     * @param friendId 삭제할 친구 ID
     * @return 삭제된 행 수
     */
    @Query("DELETE FROM friends WHERE id = :friendId")
    suspend fun deleteFriend(friendId: String): Int
    
    /**
     * 친구 상태를 업데이트합니다.
     * @param friendId 업데이트할 친구 ID
     * @param status 새로운 상태 (accepted, pending_sent, pending_received)
     * @return 업데이트된 행 수
     */
    @Query("UPDATE friends SET status = :status WHERE id = :friendId")
    suspend fun updateFriendStatus(friendId: String, status: String): Int
    
    /**
     * 모든 친구 데이터를 삭제합니다.
     */
    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()
    
    // --- 친구 요청 관련 쿼리 ---
    
    /**
     * 모든 친구 요청 목록을 가져옵니다.
     * @return 친구 요청 목록
     */
    @Query("SELECT * FROM friend_requests ORDER BY timestamp DESC")
    suspend fun getAllFriendRequests(): List<FriendRequestEntity>
    
    /**
     * 친구 요청을 추가하거나 업데이트합니다.
     * @param request 추가/업데이트할 요청 정보
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFriendRequest(request: FriendRequestEntity)
    
    /**
     * 친구 요청 목록을 추가하거나 업데이트합니다.
     * @param requests 추가/업데이트할 요청 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFriendRequests(requests: List<FriendRequestEntity>)
    
    /**
     * 친구 요청을 삭제합니다.
     * @param userId 삭제할 요청의 사용자 ID
     * @return 삭제된 행 수
     */
    @Query("DELETE FROM friend_requests WHERE userId = :userId")
    suspend fun deleteFriendRequest(userId: String): Int
    
    /**
     * 모든 친구 요청 데이터를 삭제합니다.
     */
    @Query("DELETE FROM friend_requests")
    suspend fun deleteAllFriendRequests()
    
    /**
     * 친구 요청이 수락되면 해당 요청을 삭제하고 친구 목록에 추가합니다.
     * @param request 수락된 요청
     * @param friend 추가될 친구 정보
     */
    @Transaction
    suspend fun acceptFriendRequest(request: FriendRequestEntity, friend: FriendEntity) {
        insertOrUpdateFriend(friend)
        deleteFriendRequest(request.userId)
    }
} 