package com.example.domain.event

interface AggregateRoot {
    fun pullDomainEvents(): List<DomainEvent>
    fun clearDomainEvents() // Added this based on User.kt
}
