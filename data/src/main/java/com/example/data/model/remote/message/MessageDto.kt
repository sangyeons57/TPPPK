package com.example.data.model.remote.message

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * 채널 메시지 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'projects/{projectId}/categories/{categoryId}/channels/{channelId}/messages' 서브컬렉션과 매핑됩니다.
 */
data class MessageDto(
    /**
     * 메시지 ID (Firestore 문서 ID)
     */
    @DocumentId
    val messageId: String = "",
    
    /**
     * 메시지 송신자 ID
     */
    @PropertyName("senderId")
    val senderId: String = "",
    
    /**
     * 메시지 내용
     */
    @PropertyName("text")
    val text: String = "",
    
    /**
     * 메시지 전송 시간 (서버 타임스탬프)
     */
    @ServerTimestamp
    @PropertyName("timestamp")
    val timestamp: Timestamp? = null,
    
    /**
     * 메시지 수정 여부
     */
    @PropertyName("isEdited")
    val isEdited: Boolean = false,
    
    /**
     * 메시지 수정 시간
     */
    @PropertyName("editedAt")
    val editedAt: Timestamp? = null,
    
    /**
     * 첨부 파일 URL 목록
     */
    @PropertyName("attachments")
    val attachments: List<String>? = null,
    
    /**
     * 멘션된 사용자 ID 목록
     */
    @PropertyName("mentions")
    val mentions: List<String>? = null,
    
    /**
     * 리액션 정보 (이모지: 사용자 ID 목록)
     */
    @PropertyName("reactions")
    val reactions: Map<String, List<String>>? = null
) 