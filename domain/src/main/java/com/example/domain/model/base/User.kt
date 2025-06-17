package com.example.domain.model.base

import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.event.DomainEvent
import com.example.domain.event.user.UserAccountSuspendedEvent
import com.example.domain.event.user.UserAccountActivatedEvent
import com.example.domain.event.user.UserAccountWithdrawnEvent
import com.example.domain.event.user.UserStatusChangedEvent
import com.example.domain.event.user.UserProfileUpdatedEvent
import com.example.domain.event.user.UserMemoChangedEvent
import com.example.domain.event.user.UserFcmTokenUpdatedEvent
import com.example.domain.event.user.UserNameChangedEvent
import com.example.domain.event.user.UserProfileImageChangedEvent
import com.example.domain.event.user.UserCreatedEvent

import com.example.core_common.util.DateTimeUtil
import com.example.domain.event.AggregateRoot

import java.time.Instant

/**
 * Represents a User entity in the domain.
 * This class is responsible for managing its own state and enforcing business rules.
 */
class User private constructor(
    uid: DocumentId,
    email: UserEmail,
    name: UserName,
    consentTimeStamp: Instant,
    profileImageUrl: ImageUrl?,
    memo: UserMemo?,
    userStatus: UserStatus,
    createdAt: Instant,
    updatedAt: Instant,
    fcmToken: UserFcmToken?,
    accountStatus: UserAccountStatus
) : AggregateRoot {

    /** Collects domain events raised by this aggregate until they are dispatched. */
    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    // Immutable properties
    val uid: DocumentId = uid
    val email: UserEmail = email
    val consentTimeStamp: Instant = consentTimeStamp
    val createdAt: Instant = createdAt


    // Exposed mutable properties with restricted setters
    var name: UserName = name
        private set

    var profileImageUrl: ImageUrl? = profileImageUrl
        private set

    var memo: UserMemo? = memo
        private set

    var userStatus: UserStatus = userStatus
        private set

    var updatedAt: Instant = updatedAt
        private set

    var fcmToken: UserFcmToken? = fcmToken
        private set

    var accountStatus: UserAccountStatus = accountStatus
        private set

    /**
     * Returns and clears the accumulated domain events.
     */
    override fun pullDomainEvents(): List<DomainEvent> {
        val copy = _domainEvents.toList()
        _domainEvents.clear()
        return copy
    }

    override fun clearDomainEvents() {
        _domainEvents.clear()
    } 

    // Secondary constructor for convenience if some fields can be truly optional at creation
    // For now, we assume all parameters in the primary internal constructor are essential for a valid User object
    // or are set by factory methods.

    /**
     * Updates the user's profile information.
     * Name and profile image URL can be changed.
     * The operation is ignored if the account is withdrawn.
     *
     * @param newName The new name for the user.
     * @param newProfileImageUrl The new profile image URL (can be null).
     */
    @Deprecated("Use changeName() and/or changeProfileImage() instead")
    fun updateProfile(newName: UserName, newProfileImageUrl: ImageUrl?) {
        if (isWithdrawn()) return

        this.name = newName
        this.profileImageUrl = newProfileImageUrl
        this.updatedAt = Instant.now()
        _domainEvents.add(UserProfileUpdatedEvent(uid.value))
    }

    /**
     * Changes only the user's name.
     */
    fun changeName(newName: UserName) {
        if (isWithdrawn()) return
        if (this.name == newName) return
        this.name = newName
        this.updatedAt = Instant.now()
        _domainEvents.add(UserNameChangedEvent(uid.value))
    }

    /**
     * Changes only the user's profile image URL (nullable). Pass null to remove image.
     */
    fun changeProfileImage(newProfileImageUrl: ImageUrl?) {
        if (isWithdrawn()) return
        if (this.profileImageUrl == newProfileImageUrl) return
        this.profileImageUrl = newProfileImageUrl
        this.updatedAt = Instant.now()
        _domainEvents.add(UserProfileImageChangedEvent(uid.value))
    }

    /**
     * Changes the user's memo.
     * The operation is ignored if the account is withdrawn.
     *
     * @param newMemo The new memo for the user.
     */
    fun changeMemo(newMemo: UserMemo?) {
        if (isWithdrawn()) return

        this.memo = newMemo
        this.updatedAt = Instant.now()
        _domainEvents.add(UserMemoChangedEvent(uid.value))
    }

    /**
     * Updates the user's online status (e.g., OFFLINE, ONLINE).
     * The operation is ignored if the account is withdrawn.
     *
     * @param newStatus The new user status.
     */
    fun updateUserStatus(newStatus: UserStatus) {
        if (isWithdrawn()) return

        this.userStatus = newStatus
        this.updatedAt = Instant.now()
        _domainEvents.add(UserStatusChangedEvent(uid.value, newStatus))
    }

    /**
     * Updates the FCM token for push notifications.
     * The operation is ignored if the account is withdrawn.
     *
     * @param newToken The new FCM token.
     */
    fun updateFcmToken(newToken: UserFcmToken?) {
        if (isWithdrawn()) return
        if (this.fcmToken == newToken) return // no-op if unchanged

        this.fcmToken = newToken
        this.updatedAt = DateTimeUtil.nowInstant()
        _domainEvents.add(UserFcmTokenUpdatedEvent(uid.value))
    }

    /**
     * Removes the user's profile image (sets it to null).
     * Generates [UserProfileImageChangedEvent] if an image was present.
     */
    fun removeProfileImage() {
        if (isWithdrawn()) return
        if (profileImageUrl == null) return

        profileImageUrl = null
        updatedAt = DateTimeUtil.nowInstant()
        _domainEvents.add(UserProfileImageChangedEvent(uid.value))
    }

    /**
     * Suspends the user's account.
     * Account can only be suspended if it is currently ACTIVE.
     * The operation is ignored if the account is withdrawn.
     */
    fun suspendAccount() {
        if (isWithdrawn()) return
        // Allow suspension from any non-withdrawn state for now, as per user request.
        // Original stricter rule: if (this.accountStatus == UserAccountStatus.ACTIVE)
        this.accountStatus = UserAccountStatus.SUSPENDED
        this.updatedAt = Instant.now()
        _domainEvents.add(UserAccountSuspendedEvent(uid.value))
    }

    /**
     * Activates a previously suspended user's account.
     * Account can only be activated if it is currently SUSPENDED.
     * The operation is ignored if the account is withdrawn.
     */
    fun activateAccount() {
        if (isWithdrawn()) return
        // Allow activation from any non-withdrawn state for now, as per user request.
        // Original stricter rule: if (this.accountStatus == UserAccountStatus.SUSPENDED)
        this.accountStatus = UserAccountStatus.ACTIVE
        this.updatedAt = Instant.now()
        _domainEvents.add(UserAccountActivatedEvent(uid.value))
    }

    /**
     * Marks the user's account as withdrawn.
     * This is a final state and cannot be undone through this method.
     * The operation is ignored if the account is already withdrawn.
     */
    fun markAsWithdrawn() {
        if (isWithdrawn()) return

        this.accountStatus = UserAccountStatus.WITHDRAWN
        this.userStatus = UserStatus.OFFLINE // Ensure offline status on withdrawal
        this.fcmToken = null // Clear FCM token on withdrawal
        this.updatedAt = Instant.now()
        _domainEvents.add(UserAccountWithdrawnEvent(uid.value))
    }

    /**
     * Checks if the account is in a withdrawn state.
     */
    fun isWithdrawn(): Boolean = this.accountStatus == UserAccountStatus.WITHDRAWN

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        return uid == other.uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    companion object {
        /**
         * Creates a new User instance for registration.
         *
         * @param uid Unique identifier for the user.
         * @param email User's email address.
         * @param name User's display name.
         * @param consentTimeStamp Timestamp of user's consent.
         * @param profileImageUrl Optional initial profile image URL.
         * @param initialMemo Optional initial memo.
         * @param initialFcmToken Optional initial FCM token.
         * @return A new User instance.
         */
        fun registerNewUser(
            uid: DocumentId,
            email: UserEmail,
            name: UserName,
            consentTimeStamp: Instant,
            profileImageUrl: ImageUrl? = null,
            initialMemo: UserMemo? = null,
            initialFcmToken: UserFcmToken? = null
        ): User {
            val now = DateTimeUtil.nowInstant()
            val user = User(
                uid = uid,
                email = email,
                name = name,
                consentTimeStamp = consentTimeStamp,
                profileImageUrl = profileImageUrl,
                memo = initialMemo,
                userStatus = UserStatus.OFFLINE, // Default to offline
                createdAt = now,
                updatedAt = now,
                fcmToken = initialFcmToken,
                accountStatus = UserAccountStatus.ACTIVE // Default to active
            )
            user._domainEvents.add(UserCreatedEvent(uid.value))
            return user
        }

        /**
         * Reconstructs a User instance from a data source (e.g., database).
         * This method assumes the data is valid as it's coming from a trusted source.
         * Further validation or transformation can be added if necessary.
         */
        fun fromDataSource(
            uid: DocumentId,
            email: UserEmail,
            name: UserName,
            consentTimeStamp: Instant,
            profileImageUrl: ImageUrl?,
            memo: UserMemo?,
            userStatus: UserStatus,
            createdAt: Instant,
            updatedAt: Instant,
            fcmToken: UserFcmToken?,
            accountStatus: UserAccountStatus
        ): User {
            val user = User(
                uid, email, name, consentTimeStamp, profileImageUrl, memo,
                userStatus, createdAt, updatedAt, fcmToken, accountStatus
            )
            user._domainEvents.add(UserCreatedEvent(uid.value))
            return user
        }
    }
}