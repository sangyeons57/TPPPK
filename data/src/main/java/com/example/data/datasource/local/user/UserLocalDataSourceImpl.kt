package com.example.data.datasource.local.user

import com.example.data.db.dao.UserDao // Room DAO 위치 가정
import com.example.data.model.local.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserLocalDataSource 인터페이스의 Room 데이터베이스 구현체입니다.
 */
@Singleton
class UserLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao // Room DAO 주입
) : UserLocalDataSource {

    // --- UserLocalDataSource 인터페이스 함수 구현 --- 

    /*
    override fun getCurrentUserStream(): Flow<UserEntity?> {
        // Room DAO를 사용하여 현재 사용자 스트림 조회 로직 구현
        // 예: return userDao.getCurrentUserStream()
        throw NotImplementedError("getCurrentUserStream not implemented yet")
    }
    */

    /*
    override suspend fun saveUser(user: UserEntity) {
        // Room DAO를 사용하여 사용자 정보 저장 로직 구현
        // 예: userDao.insertUser(user)
        throw NotImplementedError("saveUser not implemented yet")
    }
    */

    /*
    override suspend fun clearAllUsers() {
        // Room DAO를 사용하여 모든 사용자 정보 삭제 로직 구현
        // 예: userDao.deleteAllUsers()
        throw NotImplementedError("clearAllUsers not implemented yet")
    }
    */

    /**
     * 특정 사용자의 정보를 Flow 형태로 가져옵니다.
     * @param userId 가져올 사용자의 ID.
     * @return UserEntity의 Flow. 사용자가 없으면 null을 방출하는 Flow.
     */
    override fun getUserStream(userId: String): Flow<UserEntity?> {
        return userDao.getUserStream(userId)
    }

    /**
     * 사용자 정보를 삽입하거나 업데이트합니다 (Upsert).
     * @param user 추가 또는 업데이트할 사용자 엔티티.
     */
    override suspend fun upsertUser(user: UserEntity) {
        userDao.upsertUser(user)
    }

    /**
     * 특정 사용자의 정보를 삭제합니다.
     * @param userId 삭제할 사용자의 ID.
     */
    override suspend fun deleteUser(userId: String) {
        userDao.deleteUserById(userId)
    }

    // ... 다른 함수들의 실제 구현 추가 ...
} 