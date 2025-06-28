package com.example.domain.event

import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Marker interface for all domain events.
 */
interface DomainEvent {
    /** Timestamp when the event occurred (UTC). */
    val occurredOn: Instant
}

/**
 * Handles a specific type of [DomainEvent].
 */
fun interface EventHandler<E : DomainEvent> {
    fun handle(event: E)
}

/**
 * Publisher abstraction for domain events. Kept as an interface so that
 * infrastructure layers (e.g., adapters, messaging) can swap the dispatcher
 * without touching application code.
 */
interface DomainEventPublisher {
    fun publish(event: DomainEvent)
    fun publish(aggregate: AggregateRoot)
    fun publishAll(events: List<DomainEvent>)
}


/**
 * Global in-memory event bus.
 *
 * 1. Handlers are stored in a map keyed by the concrete event **class**.
 * 2. Registration is done at application startup (e.g., in a Hilt module).
 * 3. `publishAll` is a convenience for aggregates that return a list via `pullDomainEvents()`.
 *
 * NOTE: This implementation keeps everything in-memory and synchronous.
 * Replace with a message queue or coroutine channel if you need async / out-of-process dispatch.
 */

object EventDispatcher : DomainEventPublisher {


    private val handlers: MutableMap<Class<out DomainEvent>, MutableList<EventHandler<*>>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun publish(event: DomainEvent) {
        handlers[event.javaClass]?.forEach { (it as EventHandler<DomainEvent>).handle(event) }
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }

    /**
     * Publishes all accumulated domain events from an [AggregateRoot].
     * The aggregate's internal event list will be cleared after publishing.
     *
     * @param aggregate The [AggregateRoot] from which to pull and publish events.
     */
    override fun publish(aggregate: AggregateRoot) {
        val events = aggregate.pullDomainEvents()
        publishAll(events)
        aggregate.clearDomainEvents()
    }

    fun <T : DomainEvent> register(eventType: Class<T>, handler: EventHandler<T>) {
        handlers.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }
}

