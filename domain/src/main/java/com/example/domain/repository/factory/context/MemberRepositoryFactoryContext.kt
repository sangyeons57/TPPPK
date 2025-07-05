package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

class MemberRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

    fun changeCollectionPath(projectId: String) : MemberRepositoryFactoryContext {
        this.collectionPath = CollectionPath.projectMembers(projectId)
        return this
    }

}
