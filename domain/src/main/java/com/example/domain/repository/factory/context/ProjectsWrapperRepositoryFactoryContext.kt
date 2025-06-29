package com.example.domain.repository.factory.context

import com.example.domain.model.base.Member
import com.example.domain.model.base.Project
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.UserId
import com.example.domain.repository.DefaultRepositoryFactoryContext

class ProjectsWrapperRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {
    fun changeCollectionPath(userId: UserId) : ProjectsWrapperRepositoryFactoryContext {
        this.collectionPath = CollectionPath.userProjectWrappers(userId = userId.value)
        return this
    }
}
