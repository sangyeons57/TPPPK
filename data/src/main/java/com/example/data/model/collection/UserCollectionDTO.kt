package com.example.data.model.collection

data class UserCollectionDTO(
    val user: UserDocumentDTO,
    val friends: List<FriendDocumentDTO>? = null,
    val dmWrappers: List<DMWrapperDocumentDTO>? = null,
    val projectsWrappers: List<ProjectsWrapperDocumentDTO>? = null
)
