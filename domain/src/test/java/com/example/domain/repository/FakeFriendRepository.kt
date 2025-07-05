package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Friend
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.FriendRepository
import com.example.domain.repository.factory.context.DefaultRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

class FakeFriendRepository : FriendRepository {

    private val friends = mutableMapOf<String, Friend>()
    private var shouldThrowError = false

    override val factoryContext: DefaultRepositoryFactoryContext
        get() = TODO("Not yet implemented")

    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }

    fun addFriend(friend: Friend) {
        friends[friend.id.value] = friend
    }

    override suspend fun findById(
        id: DocumentId,
        source: Source
    ): CustomResult<AggregateRoot, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find by id failed"))
        }
        return friends[id.value]?.let {
            CustomResult.Success(it)
        } ?: CustomResult.Failure(Exception("Friend not found"))
    }

    override suspend fun create(
        id: DocumentId,
        entity: AggregateRoot
    ): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Create failed"))
        }
        val friend = entity as Friend
        friends[id.value] = friend
        return CustomResult.Success(id)
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Save failed"))
        }
        val friend = entity as Friend
        friends[friend.id.value] = friend
        return CustomResult.Success(friend.id)
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Delete failed"))
        }
        return if (friends.remove(id.value) != null) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Friend not found"))
        }
    }

    override suspend fun findAll(source: Source): CustomResult<List<AggregateRoot>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find all failed"))
        }
        return CustomResult.Success(friends.values.toList())
    }

    override fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe failed")))
        }
        return flowOf(
            friends[id.value]?.let { CustomResult.Success(it) }
                ?: CustomResult.Failure(Exception("Friend not found"))
        )
    }

    override fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe all failed")))
        }
        return flowOf(CustomResult.Success(friends.values.toList()))
    }

    override suspend fun getUserFriends(userId: UserId): CustomResult<List<Friend>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Get user friends failed"))
        }
        val userFriends = friends.values.filter { friend ->
            (friend.requesterId == userId || friend.recipientId == userId) &&
                    friend.status == FriendStatus.ACCEPTED
        }
        return CustomResult.Success(userFriends)
    }

    override fun getUserFriendsStream(userId: UserId): Flow<CustomResult<List<Friend>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Get user friends stream failed")))
        }
        val userFriends = friends.values.filter { friend ->
            (friend.requesterId == userId || friend.recipientId == userId) &&
                    friend.status == FriendStatus.ACCEPTED
        }
        return flowOf(CustomResult.Success(userFriends))
    }

    override suspend fun getPendingFriendRequests(userId: UserId): CustomResult<List<Friend>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Get pending requests failed"))
        }
        val pendingRequests = friends.values.filter { friend ->
            friend.recipientId == userId && friend.status == FriendStatus.PENDING
        }
        return CustomResult.Success(pendingRequests)
    }

    override fun getPendingFriendRequestsStream(userId: UserId): Flow<CustomResult<List<Friend>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Get pending requests stream failed")))
        }
        val pendingRequests = friends.values.filter { friend ->
            friend.recipientId == userId && friend.status == FriendStatus.PENDING
        }
        return flowOf(CustomResult.Success(pendingRequests))
    }

    override suspend fun sendFriendRequest(
        requesterId: UserId,
        recipientId: UserId
    ): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Send friend request failed"))
        }
        
        // Check if friendship already exists
        val existingFriend = friends.values.find { friend ->
            (friend.requesterId == requesterId && friend.recipientId == recipientId) ||
            (friend.requesterId == recipientId && friend.recipientId == requesterId)
        }
        
        if (existingFriend != null) {
            return CustomResult.Failure(Exception("Friendship already exists"))
        }

        val friendId = DocumentId("friend_${requesterId.value}_${recipientId.value}")
        val friend = Friend.newRequest(
            id = friendId,
            name = Name(""),
            profileImageUrl = null,
            requestedAt = Instant.now()
        )
        friends[friendId.value] = friend
        return CustomResult.Success(friendId)
    }

    override suspend fun acceptFriendRequest(friendshipId: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Accept friend request failed"))
        }
        val friend = friends[friendshipId.value]
        return if (friend != null && friend.status == FriendStatus.PENDING) {
            val acceptedFriend = friend.copy(status = FriendStatus.ACCEPTED)
            friends[friendshipId.value] = acceptedFriend
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Friend request not found or already processed"))
        }
    }

    override suspend fun declineFriendRequest(friendshipId: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Decline friend request failed"))
        }
        val friend = friends[friendshipId.value]
        return if (friend != null && friend.status == FriendStatus.PENDING) {
            friends.remove(friendshipId.value)
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Friend request not found or already processed"))
        }
    }

    override suspend fun removeFriend(friendshipId: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Remove friend failed"))
        }
        return if (friends.remove(friendshipId.value) != null) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Friendship not found"))
        }
    }
}