package com.example.domain.model.collection

import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Friend
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.User

// 기본 모델들을 import 합니다. 실제 경로에 맞게 수정하세요.

data class UserCollection(
    val user: User,
    val friends: List<Friend>? = null,
    val dmWrappers: List<DMWrapper>? = null,
    @Deprecated("User's project participation is now primarily managed by ProjectsWrapperRepository. This field represents a legacy way of storing this data within the user document.")
    val projectsWrappers: List<ProjectsWrapper>? = null // 필드명은 ERD 기반 유지 또는 userProjects 등으로 변경 고려
)
