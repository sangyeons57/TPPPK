// 경로: domain/model/Category.kt (ProjectSettingViewModel, ProjectStructure 관련 기반)
package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant // Import Instant

data class Category(
    @DocumentId
    val id: String,
    val projectId: String,
    val name: String,
    val order: Int, // 카테고리 순서
    val channels: List<Channel> = emptyList(), // 채널 목록 추가
    val createdAt: Instant? = null, // Add createdAt (nullable for existing data without it)
    val updatedAt: Instant? = null, // Add updatedAt (nullable for existing data without it)
    val createdBy: String? = null,   // Add createdBy
    val updatedBy: String? = null    // Add updatedBy
)