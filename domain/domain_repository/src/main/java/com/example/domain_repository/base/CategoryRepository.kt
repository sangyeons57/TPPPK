package com.example.domain_repository.base

import com.example.domain_repository.DefaultRepository

import com.example.domain.model.base.Category

/**
 * 프로젝트 내 카테고리 관련 데이터 처리를 위한 인터페이스입니다.
 * 카테고리는 특정 프로젝트에 종속됩니다.
 * 
 * DefaultRepository를 상속하므로 다음 메서드들을 자동으로 제공받습니다:
 * - observeAll(): Flow<CustomResult<List<Category>, Exception>>
 * - observe(id): Flow<CustomResult<Category, Exception>>
 * - save(entity): CustomResult<DocumentId, Exception>
 * - delete(id): CustomResult<Unit, Exception>
 * - setCollection(collectionPath): void
 */
interface CategoryRepository : DefaultRepository<Category>
