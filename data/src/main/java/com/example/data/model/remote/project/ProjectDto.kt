package com.example.data.model.remote.project

import com.google.firebase.Timestamp

/**
 * Firestore와 통신하기 위한 프로젝트 정보 DTO 입니다.
 */
data class ProjectDto(
    val id: String = "", // Firestore 문서 ID
    val name: String = "",
    val description: String = "",
    val ownerId: String = "", // 프로젝트 생성자 ID
    val participantIds: List<String> = emptyList(), // 참여자 ID 목록
    val createdAt: Timestamp = Timestamp.now(), // 생성 타임스탬프
    val lastUpdatedAt: Timestamp = Timestamp.now() // 마지막 업데이트 타임스탬프
) 