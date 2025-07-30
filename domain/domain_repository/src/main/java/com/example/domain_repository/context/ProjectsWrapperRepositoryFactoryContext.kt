package com.example.domain_repository.context

import com.example.domain.model.vo.UserId
import com.example.domain.vo.CollectionPath

class ProjectsWrapperRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {
    fun changeCollectionPath(userId: UserId) : ProjectsWrapperRepositoryFactoryContext {
        this.collectionPath = CollectionPath.userProjectWrappers(userId = userId.value)
        return this
    }
}
