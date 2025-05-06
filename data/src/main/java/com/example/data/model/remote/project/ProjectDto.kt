package com.example.data.model.remote.project

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * 프로젝트 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'projects' 컬렉션과 매핑됩니다.
 */
data class ProjectDto(
    @DocumentId
    val projectId: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("imageUrl")
    val imageUrl: String? = null,
    
    @PropertyName("categoryId")
    val categoryId: String = "",
    
    @PropertyName("ownerId")
    val ownerId: String = "",
    
    @PropertyName("memberIds")
    val memberIds: List<String> = emptyList(),
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now(),
    
    @PropertyName("isPublic")
    val isPublic: Boolean = true
) 