package com.example.domain.model.base


import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.event.user.UserAccountActivatedEvent
import com.example.domain.event.user.UserAccountSuspendedEvent
import com.example.domain.event.user.UserAccountWithdrawnEvent
import com.example.domain.event.user.UserFcmTokenUpdatedEvent
import com.example.domain.event.user.UserMemoChangedEvent
import com.example.domain.event.user.UserNameChangedEvent
import com.example.domain.event.user.UserProfileUpdatedEvent
import com.example.domain.event.user.UserStatusChangedEvent
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.DocumentId
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
    initialEmail: UserEmail,
    initialName: UserName,
    initialConsentTimeStamp: Instant,
    initialMemo: UserMemo?,
    initialUserStatus: UserStatus,
    initialFcmToken: UserFcmToken?,
    initialAccountStatus: UserAccountStatus,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties
    val email: UserEmail = initialEmail
    val consentTimeStamp: Instant = initialConsentTimeStamp

    // Mutable properties
    var name: UserName = initialName
        private set
    var memo: UserMemo? = initialMemo
        private set
    var userStatus: UserStatus = initialUserStatus
        private set
    var fcmToken: UserFcmToken? = initialFcmToken
        private set
    var accountStatus: UserAccountStatus = initialAccountStatus
        private set

    init {
        setOriginalState()
    }

    /**
     * Returns and clears the accumulated domain events.
     */

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_EMAIL to email.value,
            KEY_NAME to name.value,
            KEY_CONSENT_TIMESTAMP to consentTimeStamp,
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
     * Name can be changed.
     * The operation is ignored if the account is withdrawn.
     *
     * @param newName The new name for the user.
     * @param newProfileImageUrl The new profile image URL (can be null).
     */
    @Deprecated("Use changeName() instead")
    fun updateProfile(newName: UserName) {
        if (isWithdrawn()) return

        this.name = newName
        pushDomainEvent(UserProfileUpdatedEvent(id.value))
    }

    /**
     * Changes only the user's name.
     */
    fun changeName(newName: UserName) {
        if (isWithdrawn()) return
        if (this.name == newName) return
        this.name = newName
        pushDomainEvent(UserNameChangedEvent(id.value))
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
        pushDomainEvent(UserFcmTokenUpdatedEvent(id.value))
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
        const val KEY_MEMO = "memo"
        const val KEY_USER_STATUS = "userStatus"
        const val KEY_FCM_TOKEN = "fcmToken"
        const val KEY_ACCOUNT_STATUS = "accountStatus"

        /**
         * Creates a new User instance for registration.
         *
         * @param id Unique identifier for the user.
         * @param email User's email address.
         * @param name User's display name.
         * @param consentTimeStamp Timestamp of user's consent.
         * @param initialFcmToken Optional initial FCM token.
         * @return A new User instance.
         */
        fun create(
            id: DocumentId,
            email: UserEmail,
            name: UserName,
            consentTimeStamp: Instant,
            memo: UserMemo? = null,
            initialFcmToken: UserFcmToken? = null
        ): User {
            val user = User(
                id = id,
                initialEmail = email,
                initialName = name,
                initialConsentTimeStamp = consentTimeStamp,
                initialMemo = memo,
                initialUserStatus = UserStatus.OFFLINE, // Default to offline
                initialFcmToken = initialFcmToken,
                initialAccountStatus = UserAccountStatus.ACTIVE,
                isNew = true,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant() // Default to active
            )
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
            memo: UserMemo?,
            userStatus: UserStatus,
            createdAt: Instant?,
            updatedAt: Instant?,
            fcmToken: UserFcmToken?,
            accountStatus: UserAccountStatus
        ): User {
            val user = User(
                id = id,
                initialEmail = email,
                initialName = name,
                initialConsentTimeStamp = consentTimeStamp,
                initialMemo = memo,
                initialUserStatus = userStatus,
                initialFcmToken = fcmToken,
                initialAccountStatus = accountStatus,
                isNew = false,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant()
            )
            return user
        }
    }
}