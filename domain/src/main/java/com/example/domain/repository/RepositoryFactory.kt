package com.example.domain.repository

import com.example.domain.model.AggregateRoot

interface RepositoryFactory <in Input, out Output>
        where Input : RepositoryFactoryContext,
              Output : Repository {
    fun create (input: Input): Output
}