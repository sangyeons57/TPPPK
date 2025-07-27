package com.example.domain.repository.base

import com.example.domain.model.base.Category
import com.example.domain.repository.DefaultRepository

/**
 * Category Repository Interface
 * Generic CRUD operations for Category entities via DefaultRepository
 */
interface CategoryRepository : DefaultRepository<Category>
