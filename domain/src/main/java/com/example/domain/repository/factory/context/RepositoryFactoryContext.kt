package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

interface RepositoryFactoryContext {
}

/**
 * collectionPath 는 자동으로 DefaultDatasource에서 Collection경로로 설정됨
 */
interface DefaultRepositoryFactoryContext: RepositoryFactoryContext {
    val collectionPath: CollectionPath
}
