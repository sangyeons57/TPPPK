package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.ProjectsWrapperDTO // Added import
import com.example.data.model.remote.DMWrapperDTO
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {

    /**
     * 특정 userId를 가진 사용자의 정보를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     */
    fun observeUser(userId: String): Flow<CustomResult<UserDTO, Exception>>


    /**
     * 주어진 이름(닉네임)과 정확히 일치하는 사용자 정보를 실시간 스트림으로 반환합니다.
     */
    fun findByNameStream(name: String): Flow<CustomResult<UserDTO, Exception>>

    /**
     * 주어진 이름(닉네임)을 포함하는 사용자 목록을 실시간 스트림으로 반환합니다.
     */
    fun findAllByNameStream(name: String, limit: Int = 10): Flow<CustomResult<List<UserDTO>, Exception>>


    /**
     * 서버에서만 단일 사용자 정보를 가져옵니다. Firestore 캐시를 무시합니다.
     * @param userId 조회할 사용자 ID
     */
    suspend fun fetchUserByIdServer(userId: String): CustomResult<UserDTO, Exception>

    /**
     * 특정 userId를 가진 사용자의 프로젝트 요약 정보(ProjectsWrapper)를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     * @return ProjectsWrapperDTO 목록을 담은 Flow
     */
    /**
     * 정확한 이름으로 단일 사용자를 실시간으로 가져옵니다.
     * Firestore에서 'name' 필드가 정확히 일치하는 사용자를 찾습니다.
     *
     * @param name 정확히 일치하는 사용자 이름
     * @return UserDTO를 담은 Flow, 실패 시 Exception (사용자를 찾지 못하거나 파싱 오류 발생 시 Failure)
     */
    fun getUserByExactNameStream(name: String): Flow<CustomResult<UserDTO, Exception>>

    /**
     * 특정 userId를 가진 사용자의 DM 요약 정보(DMWrapper)를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     * @return DMWrapperDTO 목록을 담은 Flow
     */
    fun getDmWrappersStream(userId: String): Flow<CustomResult<List<DMWrapperDTO>, Exception>>

    /**
     * Updates (merges) the given user document with the data in [userDto].
     * Implementations are free to use `set(merge=true)` or patch specific fields.
     */
    suspend fun updateUser(userDto: UserDTO): CustomResult<Unit, Exception>

    /**
     * Deletes the user document.
     */
    suspend fun deleteUser(uid: String): CustomResult<Unit, Exception>
}
