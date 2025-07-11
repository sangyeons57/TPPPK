package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

class ProjectRoleRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

    fun changeCollectionPath(projectId: String) : ProjectRoleRepositoryFactoryContext {
        this.collectionPath = CollectionPath.projectRoles(projectId = projectId)
        return this
    }
}
