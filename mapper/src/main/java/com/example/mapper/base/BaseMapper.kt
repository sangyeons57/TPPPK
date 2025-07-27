package com.example.mapper.base

/**
 * A generic base interface for mappers.
 * @param D The Domain model type.
 * @param E The DTO (Data Transfer Object) or Entity type.
 */
interface BaseMapper<D, E> {

    /**
     * Maps a DTO to a Domain model.
     */
    fun fromDto(dto: E): D

    /**
     * Maps a Domain model to a DTO.
     */
    fun toDto(domain: D): E

    /**
     * Maps a Domain model to a Map for database operations.
     */
    fun domainToMap(domain: D): Map<String, Any?>

    /**
     * Maps a DTO to a Map for database operations.
     */
    fun dataToMap(data: E): Map<String, Any?>
}
