package com.example.data.model.remote.project

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * 프로젝트 카테고리 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'projects/{projectId}/categories' 서브컬렉션과 매핑됩니다.
 */
data class CategoryDto(
    /**
     * 카테고리 ID (Firestore 문서 ID)
     */
    @DocumentId
    val categoryId: String = "",
    
    /**
     * 카테고리 이름
     */
    @PropertyName("name")
    val name: String = "",
    
    /**
     * 카테고리 표시 순서
     */
    @PropertyName("order")
    val order: Int = 0,
    
    /**
     * 카테고리 생성 시간
     */
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * 카테고리 생성자 ID
     */
    @PropertyName("createdBy")
    val createdBy: String = ""
) 