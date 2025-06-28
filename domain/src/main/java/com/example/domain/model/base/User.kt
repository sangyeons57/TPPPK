package com.example.domain.model.base


import com.example.core_common.util.DateTimeUtil
import com.example.domain.event.AggregateRoot
import com.example.domain.event.user.UserAccountActivatedEvent
import com.example.domain.event.user.UserAccountSuspendedEvent
import com.example.domain.event.user.UserAccountWithdrawnEvent
import com.example.domain.event.user.UserCreatedEvent
import com.example.domain.event.user.UserFcmTokenUpdatedEvent
import com.example.domain.event.user.UserMemoChangedEvent
import com.example.domain.event.user.UserNameChangedEvent
import com.example.domain.event.user.UserProfileImageChangedEvent
import com.example.domain.event.user.UserProfileUpdatedEvent
import com.example.domain.event.user.UserStatusChangedEvent
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserName
import java.time.Instant

/**
 * Represents a User entity in the domain.
 * This class is responsible for managing its own state and enforcing business rules.
 */
class User private constructor(
    initialId: DocumentId,
    initialEmail: UserEmail,
    initialName: UserName,
    initialConsentTimeStamp: Instant,
    initialProfileImageUrl: ImageUrl?,
    initialMemo: UserMemo?,
    initialUserStatus: UserStatus,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    initialFcmToken: UserFcmToken?,
    initialAccountStatus: UserAccountStatus,
    override val isNew: Boolean,
) : AggregateRoot() {


    // Immutable properties
    override val id: DocumentId = initialId
    val email: UserEmail = initialEmail
    val consentTimeStamp: Instant = initialConsentTimeStamp
    val createdAt: Instant = initialCreatedAt


    // Exposed mutable properties with restricted setters
    var name: UserName = initialName
        private set

    var profileImageUrl: ImageUrl? = initialProfileImageUrl
        private set

    var memo: UserMemo? = initialMemo
        private set

    var userStatus: UserStatus = initialUserStatus
        private set

    var updatedAt: Instant = initialUpdatedAt
        private set

    var fcmToken: UserFcmToken? = initialFcmToken
        private set

    var accountStatus: UserAccountStatus = initialAccountStatus
        private set

    /**
     * Returns and clears the accumulated domain events.
     */

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_EMAIL to email.value,
            KEY_NAME to name.value,
            KEY_CONSENT_TIMESTAMP to consentTimeStamp,
            KEY_PROFILE_IMAGE_URL to profileImageUrl?.value,
            KEY_MEMO to memo?.value,
            KEY_USER_STATUS to userStatus,
            KEY_CREATED_AT to createdAt,
            KEY_UPDATED_AT to updatedAt,
            KEY_FCM_TOKEN to fcmToken?.value,
            KEY_ACCOUNT_STATUS to accountStatus
        )
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
        pushDomainEvent(UserProfileUpdatedEvent(id.value))
    }

    /**
     * Changes only the user's name.
     */
    fun changeName(newName: UserName) {
        if (isWithdrawn()) return
        if (this.name == newName) return
        this.name = newName
        this.updatedAt = Instant.now()
        pushDomainEvent(UserNameChangedEvent(id.value))
    }

    /**
     * Changes only the user's profile image URL (nullable). Pass null to remove image.
     */
    fun changeProfileImage(newProfileImageUrl: ImageUrl?) {
        if (isWithdrawn()) return
        if (this.profileImageUrl == newProfileImageUrl) return
        this.profileImageUrl = newProfileImageUrl
        this.updatedAt = Instant.now()
        pushDomainEvent(UserProfileImageChangedEvent(id.value))
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
        this.updatedAt = DateTimeUtil.nowInstant()
        pushDomainEvent(UserMemoChangedEvent(id.value))
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
        pushDomainEvent(UserStatusChangedEvent(id.value, newStatus))
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
        pushDomainEvent(UserFcmTokenUpdatedEvent(id.value))
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
        pushDomainEvent(UserProfileImageChangedEvent(id.value))
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
        pushDomainEvent(UserAccountSuspendedEvent(id.value))
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
        pushDomainEvent(UserAccountActivatedEvent(id.value))
    }

    /**
     * Reactivates a withdrawn user account.
     * This is specifically intended for accounts whose current state is [UserAccountStatus.WITHDRAWN].
     * If the account is not withdrawn, the operation is ignored.
     */
    fun reactivateAccount() {
        if (!isWithdrawn()) return // Only proceed when the account is WITHDRAWN

        this.accountStatus = UserAccountStatus.ACTIVE
        this.updatedAt = Instant.now()
        pushDomainEvent(UserAccountActivatedEvent(id.value))
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
        pushDomainEvent(UserAccountWithdrawnEvent(id.value))
    }

    /**
     * Checks if the account is in a withdrawn state.
     */
    fun isWithdrawn(): Boolean = this.accountStatus == UserAccountStatus.WITHDRAWN

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        const val COLLECTION_NAME = "users"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_CONSENT_TIMESTAMP = "consentTimeStamp"
        const val KEY_PROFILE_IMAGE_URL = "profileImageUrl"
        const val KEY_MEMO = "memo"
        const val KEY_USER_STATUS = "userStatus"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_FCM_TOKEN = "fcmToken"
        const val KEY_ACCOUNT_STATUS = "accountStatus"

        /**
         * Creates a new User instance for registration.
         *
         * @param id Unique identifier for the user.
         * @param email User's email address.
         * @param name User's display name.
         * @param consentTimeStamp Timestamp of user's consent.
         * @param profileImageUrl Optional initial profile image URL.
         * @param initialFcmToken Optional initial FCM token.
         * @return A new User instance.
         */
        fun create(
            id: DocumentId,
            email: UserEmail,
            name: UserName,
            consentTimeStamp: Instant,
            profileImageUrl: ImageUrl? = null,
            memo: UserMemo? = null,
            initialFcmToken: UserFcmToken? = null
        ): User {
            val now = DateTimeUtil.nowInstant()
            val user = User(
                initialId = DocumentId.EMPTY,
                initialEmail = email,
                initialName = name,
                initialConsentTimeStamp = consentTimeStamp,
                initialProfileImageUrl = profileImageUrl,
                initialMemo = memo,
                initialUserStatus = UserStatus.OFFLINE, // Default to offline
                initialCreatedAt = now,
                initialUpdatedAt = now,
                initialFcmToken = initialFcmToken,
                initialAccountStatus = UserAccountStatus.ACTIVE,
                isNew = true // Default to active
            )
            user.pushDomainEvent(UserCreatedEvent(id.value))
            return user
        }

        /**
         * Reconstructs a User instance from a data source (e.g., database).
         * This method assumes the data is valid as it's coming from a trusted source.
         * Further validation or transformation can be added if necessary.
         */
        fun fromDataSource(
            id: DocumentId,
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
                initialId= id,
                initialEmail= email,
                initialName= name,
                initialConsentTimeStamp = consentTimeStamp,
                initialProfileImageUrl = profileImageUrl,
                initialMemo = memo,
                initialUserStatus = userStatus,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                initialFcmToken = fcmToken,
                initialAccountStatus = accountStatus,
                isNew = false
            )
            user.pushDomainEvent(UserCreatedEvent(id.value))
            return user
        }
    }
}