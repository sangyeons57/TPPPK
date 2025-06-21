package com.example.domain.repository

import com.example.domain.event.AggregateRoot

interface RepositoryFactory <in Input, out Output>
        where Input : RepositoryFactoryContext,
              Output : Repository {
    fun create (input: Input): Output
}