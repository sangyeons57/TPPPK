package com.example.data.model

import com.example.domain.event.AggregateRoot

interface DTO {
    fun toDomain() : AggregateRoot
}