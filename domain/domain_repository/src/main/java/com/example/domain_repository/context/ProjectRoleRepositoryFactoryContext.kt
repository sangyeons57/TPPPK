package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class ProjectRoleRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

    fun changeCollectionPath(projectId: String) : ProjectRoleRepositoryFactoryContext {
        this.collectionPath = CollectionPath.projectRoles(projectId = projectId)
        return this
    }
}
