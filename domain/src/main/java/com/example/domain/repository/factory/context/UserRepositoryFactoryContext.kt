package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.RepositoryFactoryContext

class UserRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {
}