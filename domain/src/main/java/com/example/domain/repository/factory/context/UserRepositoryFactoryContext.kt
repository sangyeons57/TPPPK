package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

class UserRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {
}