package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.data.model.local.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room 데이터베이스의 'users' 테이블에 접근하기 위한 DAO(Data Access Object) 인터페이스입니다.
 */
@Dao
interface UserDao {

    /**
     * 모든 사용자 목록을 Flow 형태로 가져옵니다.
     * @return 사용자 엔티티 리스트의 Flow.
     */
    @Query("SELECT * FROM users")
    fun getAllUsersStream(): Flow<List<UserEntity>>

    /**
     * 특정 ID를 가진 사용자 정보를 가져옵니다.
     * @param userId 가져올 사용자의 ID.
     * @return 해당 ID의 사용자 엔티티. 없으면 null.
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * 특정 ID를 가진 사용자 정보를 Flow 형태로 가져옵니다.
     * @param userId 가져올 사용자의 ID.
     * @return 해당 ID의 사용자 엔티티 Flow. 없으면 null을 방출하는 Flow.
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserStream(userId: String): Flow<UserEntity?>

    /**
     * 단일 사용자를 삽입하거나 이미 존재하면 업데이트합니다 (Upsert).
     * @param user 추가 또는 업데이트할 사용자 엔티티.
     */
    @Upsert
    suspend fun upsertUser(user: UserEntity)

    /**
     * 여러 사용자를 한 번에 삽입합니다. 충돌 시 무시합니다.
     * @param users 삽입할 사용자 엔티티 리스트.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)

    /**
     * 사용자 정보를 업데이트합니다.
     * @param user 업데이트할 사용자 엔티티.
     * @return 업데이트된 행의 수.
     */
    @Update
    suspend fun updateUser(user: UserEntity): Int

    /**
     * 특정 ID의 사용자를 삭제합니다.
     * @param userId 삭제할 사용자의 ID.
     * @return 삭제된 행의 수.
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String): Int

    /**
     * 사용자를 삭제합니다.
     * @param user 삭제할 사용자 엔티티.
     * @return 삭제된 행의 수.
     */
    @Delete
    suspend fun deleteUser(user: UserEntity): Int

    /**
     * 모든 사용자를 삭제합니다.
     */
    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
} 