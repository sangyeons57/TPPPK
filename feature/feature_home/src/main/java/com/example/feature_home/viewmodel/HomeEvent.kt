package com.example.feature_home.viewmodel

import com.example.domain.model.vo.DocumentId

/** 홈 화면 이벤트 */
sealed class HomeEvent {
    data class NavigateToProjectSettings(val projectId: DocumentId?) : HomeEvent()
    data class NavigateToDmChat(val dmId: DocumentId) : HomeEvent()
    data class NavigateToChannel(val projectId: DocumentId, val channelId: String) : HomeEvent()
    data class NavigateToTaskList(val projectId: DocumentId, val channelId: DocumentId) : HomeEvent()
    object ShowAddProjectDialog : HomeEvent()
    object ShowAddFriendDialog : HomeEvent()
    object NavigateToAddProject : HomeEvent()
    data class ShowSnackbar(val message: String) : HomeEvent()
    data class ShowAddProjectElementDialog(val projectId: DocumentId) : HomeEvent()
    data class NavigateToReorderCategory(val projectId: String) : HomeEvent()
    data class NavigateToReorderChannel(val projectId: String, val categoryId: String) : HomeEvent()
    data class ProjectDeleted(val projectId: DocumentId, val projectName: String) : HomeEvent()
}
