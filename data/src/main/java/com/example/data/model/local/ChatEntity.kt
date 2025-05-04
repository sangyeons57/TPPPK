package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Room 데이터베이스의 Chat 대화방/채팅방 테이블을 나타내는 엔티티 클래스.
 *
 * @param id 채팅방 고유 ID (Primary Key)
 * @param projectId 이 채팅방이 속한 프로젝트의 ID
 * @param participantIds 채팅방 참여자들의 사용자 ID 목록
 * @param lastMessageSnippet 마지막 메시지 내용 미리보기
 * @param lastMessageTimestamp 마지막 메시지가 보내진 시간 (UTC)
 */
@Entity(tableName = "chats") // 테이블 이름 지정
data class ChatEntity(
    @PrimaryKey val id: String, // 채팅방 ID (e.g., projectId or custom UUID)
    val projectId: String, // 이 채팅방이 속한 프로젝트 ID (FK?)
    val participantIds: List<String> = emptyList(), // TypeConverter (List<String> <-> JSON)
    val lastMessageSnippet: String? = null, // 마지막 메시지 미리보기
    val lastMessageTimestamp: LocalDateTime? = null // TypeConverter (LocalDateTime <-> Long)
) 