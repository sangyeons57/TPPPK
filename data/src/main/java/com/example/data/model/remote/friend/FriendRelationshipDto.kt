package com.example.data.model.remote.friend

import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.FriendRelationship // Assuming domain model exists
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp // Keep for timestamp DTO field
import java.time.Instant

/**
 * Firestore 'users/{userId}/friends/{friendId}' 문서와 매핑되는 데이터 클래스
 * 친구 관계 정보를 담고 있습니다.
 * The document ID for this DTO is the ID of the friend.
 */
data class FriendRelationshipDto(
    @DocumentId
    var friendId: String = "", // Represents the ID of the friend user

    @PropertyName(FirestoreConstants.FriendFields.STATUS)
    var status: String? = null, // e.g., "pending_sent", "pending_received", "accepted"

    @PropertyName(FirestoreConstants.FriendFields.TIMESTAMP)
    @ServerTimestamp // This annotation ensures Firestore sets the timestamp on the server
    var timestamp: Timestamp? = null, // Relationship initiated/updated time

    @PropertyName(FirestoreConstants.FriendFields.ACCEPTED_AT)
    var acceptedAt: Timestamp? = null // Time when the friend request was accepted
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.FriendFields.STATUS to status,
            FirestoreConstants.FriendFields.TIMESTAMP to timestamp, // Often set by @ServerTimestamp, but can be included
            FirestoreConstants.FriendFields.ACCEPTED_AT to acceptedAt
        ).filterValues { it != null }
    }

    /**
     * Converts this DTO to a basic domain model.
     * The userId (owner of this friends subcollection) must be supplied externally.
     */
    fun toBasicDomainModel(ownerUserId: String): FriendRelationship {
        // Assumes FriendRelationship domain model exists.
        // Status will be string here, converted to Enum by mapper extension.
        return FriendRelationship(
            ownerUserId = ownerUserId, // The user who this friend list belongs to
            friendUserId = this.friendId, // The ID of the friend
            status = this.status ?: "", // Default to empty string if null, mapper handles enum
            timestamp = Instant.EPOCH, // Placeholder
            acceptedAt = null // Placeholder for Instant
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, documentId: String): FriendRelationshipDto {
            return FriendRelationshipDto(
                friendId = documentId,
                status = map[FirestoreConstants.FriendFields.STATUS] as? String,
                timestamp = map[FirestoreConstants.FriendFields.TIMESTAMP] as? Timestamp,
                acceptedAt = map[FirestoreConstants.FriendFields.ACCEPTED_AT] as? Timestamp
            )
        }

        /**
         * Converts a basic domain model to this DTO.
         * ownerUserId from domain model is not part of this DTO as it's implicit from collection path.
         */
        fun fromBasicDomainModel(domain: FriendRelationship): FriendRelationshipDto {
            // Assumes FriendRelationship domain model exists.
            // Status will be converted from Enum to String here.
            return FriendRelationshipDto(
                friendId = domain.friendUserId,
                status = domain.status, // Assuming domain.status is String or domain.status.name if Enum
                timestamp = null, // Placeholder, mapper or @ServerTimestamp handles
                acceptedAt = null // Placeholder, mapper handles
            )
        }
    }
} 