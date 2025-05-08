package com.example.data.model.remote.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Firestore 'users' 컬렉션 문서와 매핑되는 데이터 클래스
 * 사용자 프로필 정보를 담고 있습니다.
 */
data class UserDto(
    /**
     * 사용자 ID (Firestore 문서 내 필드, 문서 ID와 동일)
     */
    @DocumentId
    val id: String = "",
    
    val email: String = "",
    
    val name: String = "",
    
    @PropertyName("profile_image_url")
    val profileImageUrl: String? = null,
    
    @PropertyName("status_message")
    val statusMessage: String? = null,
    
    val memo: String? = null,
    
    val status: String = "OFFLINE",
    
    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("fcm_token")
    val fcmToken: String? = null,
    
    @PropertyName("participating_project_ids")
    val participatingProjectIds: List<String> = emptyList(),
    
    @PropertyName("account_status")
    val accountStatus: String = "ACTIVE",
    
    @PropertyName("active_dm_ids")
    val activeDmIds: List<String> = emptyList(),
    
    @PropertyName("is_email_verified")
    val isEmailVerified: Boolean = false
) 