package com.example.domain_repository

import com.example.domain_repository.context.RepositoryFactoryContext

interface RepositoryFactory <in Input, out Output>
        where Input : RepositoryFactoryContext,
              Output : Repository {
    fun create (input: Input): Output
}