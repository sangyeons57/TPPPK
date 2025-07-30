package com.example.domain.model.sync

import com.example.domain.model.AggregateRoot

data class OutBoxPayload<T>(
    val data: T
) where T : AggregateRoot {
    companion object {
        fun <T> create(data: T): OutBoxPayload<T> where T : AggregateRoot {
            return OutBoxPayload(data)
        }
    }

    fun isEmpty(emptyChecker: (T) -> Boolean): Boolean = emptyChecker(data)

}