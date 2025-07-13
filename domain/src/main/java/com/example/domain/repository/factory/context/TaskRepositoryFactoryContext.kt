package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

class TaskRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

}