package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.DefaultRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeUserRepository : UserRepository {

    private val users = mutableMapOf<String, User>()
    private var shouldThrowError = false

    override val factoryContext: DefaultRepositoryFactoryContext
        get() = TODO("Not yet implemented")

    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }

    fun addUser(user: User) {
        users[user.id.value] = user
    }

    override suspend fun findById(
        id: DocumentId,
        source: Source
    ): CustomResult<AggregateRoot, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find by id failed"))
        }
        return users[id.value]?.let {
            CustomResult.Success(it)
        } ?: CustomResult.Failure(Exception("User not found"))
    }

    override fun observeByName(name: UserName): Flow<CustomResult<User, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe by name failed")))
        }
        val user = users.values.find { it.name == name }
        return flowOf(
            user?.let { CustomResult.Success(it) }
                ?: CustomResult.Failure(Exception("User not found"))
        )
    }

    override fun observeAllByName(
        name: String,
        limit: Int
    ): Flow<CustomResult<List<User>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe all by name failed")))
        }
        val filteredUsers =
            users.values.filter { it.name.value.contains(name, ignoreCase = true) }.take(limit)
        return flowOf(CustomResult.Success(filteredUsers))
    }

    override fun observeByEmail(email: String): Flow<CustomResult<User, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe by email failed")))
        }
        val user = users.values.find { it.email.value == email }
        return flowOf(
            user?.let { CustomResult.Success(it) }
                ?: CustomResult.Failure(Exception("User not found"))
        )
    }

    override suspend fun create(
        id: DocumentId,
        entity: AggregateRoot
    ): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Create failed"))
        }
        val user = entity as User
        users[id.value] = user
        return CustomResult.Success(id)
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Delete failed"))
        }
        return if (users.remove(id.value) != null) {
            CustomResult.Success(Unit)
        } else {
            return CustomResult.Failure(Exception("User not found"))
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Save failed"))
        }
        val user = entity as? User ?: return CustomResult.Failure(Exception("Entity is not User"))
        if (!user.id.isAssigned()) {
            return CustomResult.Failure(Exception("User ID not assigned"))
        }
        users[user.id.value] = user
        return CustomResult.Success(user.id)
    }

    override suspend fun findAll(source: Source): CustomResult<List<AggregateRoot>, Exception> {
        TODO("Not yet implemented")
    }

    override fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>> {
        TODO("Not yet implemented")
    }

    override fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>> {
        TODO("Not yet implemented")
    }
}
