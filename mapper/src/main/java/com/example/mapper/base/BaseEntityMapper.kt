package com.example.mapper.base

/**
 * A generic base interface for mappers between Domain and Entity models.
 * @param D The Domain model type.
 * @param E The Entity model type.
 */
interface BaseEntityMapper<D, E> {

    /**
     * Maps an Entity to a Domain model.
     */
    fun toDomain(entity: E): D

    /**
     * Maps a Domain model to an Entity.
     */
    fun toEntity(domain: D): E
}
