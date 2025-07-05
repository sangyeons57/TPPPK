package com.example.domain.repository.factory.context

import com.example.domain.model.base.Category
import com.example.domain.model.base.Project
import com.example.domain.model.vo.CollectionPath

class CategoryRepositoryFactoryContext(
    override var collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

    fun changeCollectionPath(projectId: String) : CategoryRepositoryFactoryContext {
        this.collectionPath = CollectionPath("${Project.COLLECTION_NAME}/$projectId/${Category.COLLECTION_NAME}")
        return this
    }
}
