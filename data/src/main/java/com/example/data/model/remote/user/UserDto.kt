package com.example.data.model.remote.user

import com.google.firebase.Timestamp

/**
 * Firestore와 통신하기 위한 사용자 정보 DTO (Data Transfer Object) 입니다.
 */
data class UserDto(
    val id: String = "", // Firestore 문서 ID
    val email: String = "",
    val name: String = "",
    val profileImageUrl: String? = null,
    val joinedProjects: List<String> = emptyList(), // 참여한 프로젝트 ID 목록
    val createdAt: Timestamp = Timestamp.now() // 생성 타임스탬프 (Firestore 타입)
) 