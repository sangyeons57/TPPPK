package com.example.domain.model.collection

// 기본 모델들을 import 합니다. 실제 경로에 맞게 수정하세요.
import com.example.domain.model.User
import com.example.domain.model.Friend
import com.example.domain.model.DMWrapper
import com.example.domain.model.ProjectsWrapper // ERD 기반 이름 유지

data class UserCollection(
    val user: User,
    val friends: List<Friend>? = null,
    val dmWrappers: List<DMWrapper>? = null,
    val projectsWrappers: List<ProjectsWrapper>? = null // 필드명은 ERD 기반 유지 또는 userProjects 등으로 변경 고려
)
