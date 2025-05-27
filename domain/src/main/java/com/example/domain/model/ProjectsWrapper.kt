package com.example.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * 프로젝트 목록 아이템 정보를 나타내는 도메인 모델입니다.
 * Firestore의 ProjectsWrapperDTO와 1:1 매핑됩니다.
 */
data class ProjectsWrapper(
    /**
     * 프로젝트의 ID (Firestore Document ID)
     */
    @DocumentId
    val projectId: String,

    /**
     * 프로젝트의 이름입니다.
     */
    val projectName: String,

    /**
     * 프로젝트의 대표 이미지 URL입니다. (선택 사항)
     */
    val projectImageUrl: String?
) 