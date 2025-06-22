package com.example.domain.repository.factory.context

import com.example.domain.model.base.Category
import com.example.domain.model.base.Member
import com.example.domain.model.base.Project
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.DefaultRepositoryFactoryContext

class MemberRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

    fun changeCollectionPath(projectId: String) : MemberRepositoryFactoryContext {
        this.collectionPath = CollectionPath("${Project.COLLECTION_NAME}/$projectId/${Member.COLLECTION_NAME}")
        return this
    }

}
