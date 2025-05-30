package com.example.data.model.collection

import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.DMWrapperDTO
import com.example.data.model.remote.FriendDTO
import com.example.data.model.remote.ProjectsWrapperDTO

data class UserCollectionDTO(
    val user: UserDTO,
    val friends: List<FriendDTO>? = null,
    val dmWrappers: List<DMWrapperDTO>? = null,
    val projectsWrappers: List<ProjectsWrapperDTO>? = null
)
