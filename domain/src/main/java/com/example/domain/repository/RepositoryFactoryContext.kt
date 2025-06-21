package com.example.domain.repository

import com.example.domain.event.AggregateRoot
import com.example.domain.model.vo.CollectionPath

interface RepositoryFactoryContext {
}

interface DefaultRepositoryFactoryContext: RepositoryFactoryContext {
    val collectionPath: CollectionPath
}
