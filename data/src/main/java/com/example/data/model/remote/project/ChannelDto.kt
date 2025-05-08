package com.example.data.model.remote.project

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * 프로젝트 채널 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'projects/{projectId}/categories/{categoryId}/channels' 서브컬렉션과 매핑됩니다.
 */
data class ChannelDto(
    /**
     * 채널 ID (Firestore 문서 ID)
     */
    @DocumentId
    val channelId: String = "",
    
    /**
     * 채널 이름
     */
    @PropertyName("name")
    val name: String = "",
    
    /**
     * 채널 유형 (TEXT, VOICE 등)
     */
    @PropertyName("type")
    val type: String = "TEXT",
    
    /**
     * 채널 표시 순서
     */
    @PropertyName("order")
    val order: Int = 0,
    
    /**
     * 채널 생성 시간
     */
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * 채널 생성자 ID
     */
    @PropertyName("createdBy")
    val createdBy: String = "",
    
    /**
     * 채널 설명 (선택 사항)
     */
    @PropertyName("description")
    val description: String? = null,
    
    /**
     * 채널 접근 제한 (특정 역할만 접근 가능 등)
     */
    @PropertyName("restrictions")
    val restrictions: Map<String, Any>? = null
) 