package com.example.data.remote.dto.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore의 'users' 컬렉션 데이터를 표현하는 DTO(Data Transfer Object) 클래스.
 * 원격 데이터베이스와 앱 내 데이터 교환을 위해 사용됩니다.
 */
data class UserDto(
    @DocumentId
    val userId: String = "", // Firebase Auth UID
    
    @PropertyName("email")
    val email: String = "",
    
    @PropertyName("name")
    val name: String = "", // Unique
    
    @PropertyName("profileImageUrl")
    val profileImageUrl: String? = null,
    
    @PropertyName("memo")
    val memo: String? = null, // User's personal memo/bio
    
    @PropertyName("status")
    val status: String = "OFFLINE", // UserStatus enum 대신 String 사용
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("fcmToken")
    val fcmToken: String? = null,
    
    @PropertyName("participatingProjectIds")
    val participatingProjectIds: List<String> = emptyList(),
    
    @PropertyName("accountStatus")
    val accountStatus: String = "ACTIVE", // AccountStatus enum 대신 String 사용
    
    @PropertyName("activeDmIds")
    val activeDmIds: List<String> = emptyList(),
    
    @PropertyName("isEmailVerified")
    val isEmailVerified: Boolean = false
) {
    // Firestore Data Class는 매개변수 없는 생성자를 필요로 할 수 있음 (특히 필드가 모두 기본값을 가질 때)
    // 모든 필드에 기본값이 있으므로 별도 constructor()는 없어도 될 수 있으나, 명시적으로 추가해도 무방.
    // constructor() : this()
} 