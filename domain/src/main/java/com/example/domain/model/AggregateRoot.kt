package com.example.domain.model

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FieldValue
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2


abstract class AggregateRoot {
    /** Collects domain events raised by this aggregate until they are dispatched. */
    abstract val id: DocumentId
    abstract val isNew : Boolean
    
    /** Standard timestamp fields for all domain entities */
    companion object {
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
    }
    abstract val createdAt: Instant
    abstract val updatedAt: Instant
    private val originalState: Map<String, Any?> = this.getCurrentStateMap()

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    abstract fun getCurrentStateMap(): Map<String, Any?>

    fun pullDomainEvents(): List<DomainEvent> {
        val copy = _domainEvents.toList()
        _domainEvents.clear()
        return copy
    }
    fun clearDomainEvents() {
        _domainEvents.clear()
    }
    fun pushDomainEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }
    fun getChangedFields(): Map<String, Any?> {
        val newState = this.getCurrentStateMap()
        val changedFields = mutableMapOf<String, Any?>()
        if(isNew){
            return newState
        }
        originalState.forEach { (key, value) ->
            if (newState[key] != value) {
                changedFields[key] = newState[key]
            }
        }
        return changedFields
    }
}