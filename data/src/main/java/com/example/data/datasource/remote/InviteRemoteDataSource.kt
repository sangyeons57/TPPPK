
package com.example.data.datasource.remote

import com.example.data.model.remote.InviteDTO
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

interface InviteRemoteDataSource {

    /**
     * 특정 프로젝트의 모든 초대 링크 목록을 실시간으로 관찰합니다.
     * @param projectId 초대 목록을 가져올 프로젝트의 ID
     */
    fun observeInvites(projectId: String): Flow<List<InviteDTO>>

    /**
     * 프로젝트에 새로운 초대 링크를 생성합니다.
     * @param projectId 초대를 생성할 프로젝트의 ID
     * @param expirationDate 만료 날짜 (null이면 무기한)
     * @return 생성된 초대 정보 DTO를 포함한 Result 객체
     */
    suspend fun createInvite(projectId: String, expirationDate: Timestamp?): Result<InviteDTO>

    /**
     * 초대 링크의 상태를 변경합니다. (예: 비활성화)
     * @param projectId 대상 프로젝트의 ID
     * @param inviteId 상태를 변경할 초대의 ID
     * @param newStatus 새로운 상태
     */
    suspend fun updateInviteStatus(projectId: String, inviteId: String, newStatus: String): Result<Unit>

    /**
     * 프로젝트 ID와 초대 코드로 특정 초대 정보를 가져옵니다.
     * @param projectId 대상 프로젝트의 ID
     * @param inviteCode 조회할 초대 코드
     * @return 조회된 초대 정보 DTO를 포함한 Result 객체, 없으면 null
     */
    suspend fun getInviteByCode(projectId: String, inviteCode: String): Result<InviteDTO?>
}

