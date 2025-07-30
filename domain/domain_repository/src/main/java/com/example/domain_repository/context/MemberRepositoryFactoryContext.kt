package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class MemberRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

    fun changeCollectionPath(projectId: String) : MemberRepositoryFactoryContext {
        this.collectionPath = CollectionPath.projectMembers(projectId)
        return this
    }

}
