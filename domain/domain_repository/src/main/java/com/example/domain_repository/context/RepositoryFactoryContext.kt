package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

interface RepositoryFactoryContext

/**
 * collectionPath 는 자동으로 DefaultDatasource에서 Collection경로로 설정됨
 */
interface DefaultRepositoryFactoryContext: RepositoryFactoryContext {
    val collectionPath: CollectionPath
}
