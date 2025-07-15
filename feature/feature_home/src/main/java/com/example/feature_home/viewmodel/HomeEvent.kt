package com.example.feature_home.viewmodel

import com.example.domain.model.vo.DocumentId

/** 홈 화면 이벤트 */
sealed class HomeEvent {
    object ShowAddProjectDialog : HomeEvent()
    object ShowAddFriendDialog : HomeEvent()
    data class ShowSnackbar(val message: String) : HomeEvent()
    data class ShowAddProjectElementDialog(val projectId: DocumentId) : HomeEvent()
    data class ProjectDeleted(val projectId: DocumentId, val projectName: String) : HomeEvent()
}
