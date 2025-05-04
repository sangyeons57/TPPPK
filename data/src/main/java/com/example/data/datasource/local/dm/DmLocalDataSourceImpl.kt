package com.example.data.datasource.local.dm

import com.example.core_common.util.DateTimeUtil
import com.example.data.db.dao.DmDao
import com.example.data.model.local.DmConversationEntity
import com.example.domain.model.DmConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * DM 관련 로컬 데이터 소스 구현
 * Room 데이터베이스를 사용하여 DM 대화 목록 관련 기능을 구현합니다.
 * @param dmDao DM 관련 Room DAO
 */
class DmLocalDataSourceImpl @Inject constructor(
    private val dmDao: DmDao
) : DmLocalDataSource {

    // 도메인 모델 -> 로컬 엔티티 변환 함수
    private fun DmConversation.toEntity(): DmConversationEntity = DmConversationEntity(
        id = this.channelId,
        otherUserId = this.partnerUserId,
        otherUserNickname = this.partnerUserName,
        otherUserProfileImageUrl = this.partnerProfileImageUrl,
        lastMessage = this.lastMessage,
        lastMessageTimestamp = this.lastMessageTimestamp,
        cachedAt = DateTimeUtil.now()
    )

    // 로컬 엔티티 -> 도메인 모델 변환 함수
    private fun DmConversationEntity.toDomain(): DmConversation = DmConversation(
        channelId = this.id,
        partnerUserId = this.otherUserId,
        partnerUserName = this.otherUserNickname,
        partnerProfileImageUrl = this.otherUserProfileImageUrl,
        lastMessage = this.lastMessage,
        lastMessageTimestamp = this.lastMessageTimestamp,
        unreadCount = 0
    )

    /**
     * DM 대화 목록 스트림을 가져옵니다.
     */
    override fun getDmConversationsStream(): Flow<List<DmConversation>> {
        return dmDao.getAllDmConversationsStream().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * DM 대화 목록을 로컬 데이터베이스에 저장합니다.
     */
    override suspend fun saveDmConversations(dmConversations: List<DmConversation>) {
        val entities = dmConversations.map { it.toEntity() }
        dmDao.insertOrUpdateDmConversations(entities)
    }

    /**
     * 특정 DM 대화 정보를 가져옵니다.
     */
    override suspend fun getDmConversationById(dmId: String): DmConversation? {
        return dmDao.getDmConversationById(dmId)?.toDomain()
    }

    /**
     * 특정 DM 대화 정보를 저장합니다.
     */
    override suspend fun saveDmConversation(dmConversation: DmConversation) {
        dmDao.insertOrUpdateDmConversation(dmConversation.toEntity())
    }

    /**
     * 특정 DM 대화 정보를 삭제합니다.
     */
    override suspend fun deleteDmConversation(dmId: String) {
        dmDao.deleteDmConversation(dmId)
    }

    /**
     * 모든 DM 대화 정보를 삭제합니다.
     */
    override suspend fun deleteAllDmConversations() {
        dmDao.deleteAllDmConversations()
    }

    /**
     * 마지막 메시지를 업데이트합니다.
     */
    override suspend fun updateLastMessage(dmId: String, message: String, timestamp: LocalDateTime) {
        val timestampMillis = DateTimeUtil.toEpochMillis(timestamp)
        dmDao.updateLastMessage(dmId, message, timestampMillis)
    }
} 