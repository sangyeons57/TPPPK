package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.event.user.UserAccountSuspendedEvent
import com.example.domain.event.user.UserNameChangedEvent
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserName
import com.google.common.truth.Truth.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

class UserTest {

    private val testStartTime: Instant = Instant.parse("2023-01-01T10:00:00Z")
    private val laterTime: Instant = Instant.parse("2023-01-01T11:00:00Z")

    @Before
    fun setUp() {
        DateTimeUtil.setFixedNow(testStartTime)
    }

    @After
    fun tearDown() {
        DateTimeUtil.clearFixedNow()
    }

    private fun createTestUser(
        uid: String = "testUid",
        email: String = "test@example.com",
        name: String = "Initial Name",
        consentTimeStamp: Instant = testStartTime,
        profileImageUrl: String? = null,
        memo: String? = null,
        userStatus: UserStatus = UserStatus.OFFLINE,
        createdAt: Instant = testStartTime,
        updatedAt: Instant = testStartTime,
        fcmToken: String? = null,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE
    ): User {
        return User.fromDataSource(
            uid = uid,
            email = UserEmail(email),
            name = UserName(name),
            consentTimeStamp = consentTimeStamp,
            profileImageUrl = profileImageUrl,
            memo = memo?.let { UserMemo(it) },
            userStatus = userStatus,
            createdAt = createdAt,
            updatedAt = updatedAt,
            fcmToken = fcmToken,
            accountStatus = accountStatus
        )
    }

    @Test
    fun `changeName should update name and updatedAt, and publish event`() {
        val user = createTestUser(name = "Old Name")
        val newName = UserName("New Name")

        DateTimeUtil.setFixedNow(laterTime) // Simulate time passing for updatedAt
        user.changeName(newName)

        assertThat(user.name).isEqualTo(newName)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        val events = user.pullDomainEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(UserNameChangedEvent::class.java)
        assertThat((events.first() as UserNameChangedEvent).userId).isEqualTo(user.uid)
    }

    @Test
    fun `changeName should do nothing if name is the same`() {
        val originalName = "Same Name"
        val user = createTestUser(name = originalName)
        val sameName = UserName(originalName)

        DateTimeUtil.setFixedNow(laterTime)
        user.changeName(sameName)

        assertThat(user.name.value).isEqualTo(originalName)
        assertThat(user.updatedAt).isEqualTo(testStartTime) // Should not change
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `changeName should be ignored if account is withdrawn`() {
        val user = createTestUser(accountStatus = UserAccountStatus.WITHDRAWN)
        val newName = UserName("New Name Attempt")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeName(newName)

        assertThat(user.name.value).isEqualTo("Initial Name") // Should not change
        assertThat(user.updatedAt).isEqualTo(testStartTime) // Should not change
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `suspendAccount should change status to SUSPENDED and publish event when ACTIVE`() {
        val user = createTestUser(accountStatus = UserAccountStatus.ACTIVE)

        DateTimeUtil.setFixedNow(laterTime)
        user.suspendAccount()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.SUSPENDED)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        val events = user.pullDomainEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(UserAccountSuspendedEvent::class.java)
        assertThat((events.first() as UserAccountSuspendedEvent).userId).isEqualTo(user.uid)
    }

    @Test
    fun `suspendAccount should do nothing if account is already SUSPENDED`() {
        val user = createTestUser(accountStatus = UserAccountStatus.SUSPENDED)

        DateTimeUtil.setFixedNow(laterTime)
        user.suspendAccount()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.SUSPENDED)
        assertThat(user.updatedAt).isEqualTo(testStartTime) // Should not change
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `suspendAccount should be ignored if account is WITHDRAWN`() {
        val user = createTestUser(accountStatus = UserAccountStatus.WITHDRAWN)

        DateTimeUtil.setFixedNow(laterTime)
        user.suspendAccount()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.WITHDRAWN)
        assertThat(user.updatedAt).isEqualTo(testStartTime) // Should not change
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    // --- changeProfileImage Tests ---
    @Test
    fun `changeProfileImage should update image URL and updatedAt, and publish event`() {
        val user = createTestUser(profileImageUrl = "old_url")
        val newImageUrl = "new_url"

        DateTimeUtil.setFixedNow(laterTime)
        user.changeProfileImage(newImageUrl)

        assertThat(user.profileImageUrl).isEqualTo(newImageUrl)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        val events = user.pullDomainEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(com.example.domain.event.user.UserProfileImageChangedEvent::class.java)
    }

    @Test
    fun `changeProfileImage to null should remove image URL and publish event`() {
        val user = createTestUser(profileImageUrl = "old_url")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeProfileImage(null)

        assertThat(user.profileImageUrl).isNull()
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserProfileImageChangedEvent::class.java)
    }

    @Test
    fun `changeProfileImage should do nothing if URL is the same`() {
        val user = createTestUser(profileImageUrl = "same_url")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeProfileImage("same_url")

        assertThat(user.profileImageUrl).isEqualTo("same_url")
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `changeProfileImage should be ignored if account is withdrawn`() {
        val user = createTestUser(profileImageUrl = "old_url", accountStatus = UserAccountStatus.WITHDRAWN)

        DateTimeUtil.setFixedNow(laterTime)
        user.changeProfileImage("new_url_attempt")

        assertThat(user.profileImageUrl).isEqualTo("old_url")
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    // --- removeProfileImage Tests (delegates to changeProfileImage(null)) ---
    @Test
    fun `removeProfileImage should set image URL to null and publish event`() {
        val user = createTestUser(profileImageUrl = "some_url")

        DateTimeUtil.setFixedNow(laterTime)
        user.removeProfileImage()

        assertThat(user.profileImageUrl).isNull()
        assertThat(user.updatedAt).isEqualTo(laterTime)
        val events = user.pullDomainEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(com.example.domain.event.user.UserProfileImageChangedEvent::class.java)
    }

    // --- changeMemo Tests ---
    @Test
    fun `changeMemo should update memo and updatedAt, and publish event`() {
        val user = createTestUser(memo = "Old Memo")
        val newMemo = UserMemo("New Memo")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeMemo(newMemo)

        assertThat(user.memo).isEqualTo(newMemo)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserMemoChangedEvent::class.java)
    }

    @Test
    fun `changeMemo to null should update memo and publish event`() {
        val user = createTestUser(memo = "Old Memo")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeMemo(null)

        assertThat(user.memo).isNull()
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserMemoChangedEvent::class.java)
    }

    @Test
    fun `changeMemo should do nothing if memo is the same`() {
        val user = createTestUser(memo = "Same Memo")
        val sameMemo = UserMemo("Same Memo")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeMemo(sameMemo)

        assertThat(user.memo?.value).isEqualTo("Same Memo")
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `changeMemo should be ignored if account is withdrawn`() {
        val user = createTestUser(memo = "Old Memo", accountStatus = UserAccountStatus.WITHDRAWN)
        val newMemo = UserMemo("New Memo Attempt")

        DateTimeUtil.setFixedNow(laterTime)
        user.changeMemo(newMemo)

        assertThat(user.memo?.value).isEqualTo("Old Memo")
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    // --- updateUserStatus Tests ---
    @Test
    fun `updateUserStatus should update status and updatedAt, and publish event`() {
        val user = createTestUser(userStatus = UserStatus.OFFLINE)
        val newStatus = UserStatus.ONLINE

        DateTimeUtil.setFixedNow(laterTime)
        user.updateUserStatus(newStatus)

        assertThat(user.userStatus).isEqualTo(newStatus)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserStatusChangedEvent::class.java)
    }

    @Test
    fun `updateUserStatus should do nothing if status is the same`() {
        val user = createTestUser(userStatus = UserStatus.ONLINE)

        DateTimeUtil.setFixedNow(laterTime)
        user.updateUserStatus(UserStatus.ONLINE)

        assertThat(user.userStatus).isEqualTo(UserStatus.ONLINE)
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `updateUserStatus should be ignored if account is withdrawn`() {
        val user = createTestUser(userStatus = UserStatus.OFFLINE, accountStatus = UserAccountStatus.WITHDRAWN)

        DateTimeUtil.setFixedNow(laterTime)
        user.updateUserStatus(UserStatus.ONLINE)

        assertThat(user.userStatus).isEqualTo(UserStatus.OFFLINE)
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    // --- updateFcmToken Tests ---
    @Test
    fun `updateFcmToken should update token and updatedAt, and publish event`() {
        val user = createTestUser(fcmToken = "old_token")
        val newToken = "new_token"

        DateTimeUtil.setFixedNow(laterTime)
        user.updateFcmToken(newToken)

        assertThat(user.fcmToken).isEqualTo(newToken)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserFcmTokenUpdatedEvent::class.java)
    }

    @Test
    fun `updateFcmToken to null should update token and publish event`() {
        val user = createTestUser(fcmToken = "old_token")

        DateTimeUtil.setFixedNow(laterTime)
        user.updateFcmToken(null)

        assertThat(user.fcmToken).isNull()
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserFcmTokenUpdatedEvent::class.java)
    }

    @Test
    fun `updateFcmToken should do nothing if token is the same`() {
        val user = createTestUser(fcmToken = "same_token")

        DateTimeUtil.setFixedNow(laterTime)
        user.updateFcmToken("same_token")

        assertThat(user.fcmToken).isEqualTo("same_token")
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    @Test
    fun `updateFcmToken should be ignored if account is withdrawn`() {
        val user = createTestUser(fcmToken = "old_token", accountStatus = UserAccountStatus.WITHDRAWN)

        DateTimeUtil.setFixedNow(laterTime)
        user.updateFcmToken("new_token_attempt")

        assertThat(user.fcmToken).isEqualTo("old_token") // Should remain old_token, not nullified by withdrawn logic directly in this setter
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    // --- activateAccount Tests ---
    @Test
    fun `activateAccount should change status to ACTIVE and publish event when SUSPENDED`() {
        val user = createTestUser(accountStatus = UserAccountStatus.SUSPENDED)

        DateTimeUtil.setFixedNow(laterTime)
        user.activateAccount()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.ACTIVE)
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserAccountActivatedEvent::class.java)
    }

    @Test
    fun `activateAccount should change status to ACTIVE and publish event even when already ACTIVE`() {
        // As per relaxed rule: activating an active account is a no-op that still publishes an event.
        val user = createTestUser(accountStatus = UserAccountStatus.ACTIVE)

        DateTimeUtil.setFixedNow(laterTime)
        user.activateAccount()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.ACTIVE)
        assertThat(user.updatedAt).isEqualTo(laterTime) // updatedAt should still update
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserAccountActivatedEvent::class.java)
    }

    @Test
    fun `activateAccount should be ignored if account is WITHDRAWN`() {
        val user = createTestUser(accountStatus = UserAccountStatus.WITHDRAWN)

        DateTimeUtil.setFixedNow(laterTime)
        user.activateAccount()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.WITHDRAWN)
        assertThat(user.updatedAt).isEqualTo(testStartTime)
        assertThat(user.pullDomainEvents()).isEmpty()
    }

    // --- markAsWithdrawn Tests ---
    @Test
    fun `markAsWithdrawn should change status to WITHDRAWN, nullify token, set OFFLINE and publish event when ACTIVE`() {
        val user = createTestUser(accountStatus = UserAccountStatus.ACTIVE, userStatus = UserStatus.ONLINE, fcmToken = "active_token")

        DateTimeUtil.setFixedNow(laterTime)
        user.markAsWithdrawn()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.WITHDRAWN)
        assertThat(user.userStatus).isEqualTo(UserStatus.OFFLINE)
        assertThat(user.fcmToken).isNull()
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserAccountWithdrawnEvent::class.java)
    }

    @Test
    fun `markAsWithdrawn should change status to WITHDRAWN, nullify token, set OFFLINE and publish event when SUSPENDED`() {
        val user = createTestUser(accountStatus = UserAccountStatus.SUSPENDED, userStatus = UserStatus.ONLINE, fcmToken = "active_token")

        DateTimeUtil.setFixedNow(laterTime)
        user.markAsWithdrawn()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.WITHDRAWN)
        assertThat(user.userStatus).isEqualTo(UserStatus.OFFLINE)
        assertThat(user.fcmToken).isNull()
        assertThat(user.updatedAt).isEqualTo(laterTime)
        assertThat(user.pullDomainEvents().first()).isInstanceOf(com.example.domain.event.user.UserAccountWithdrawnEvent::class.java)
    }

    @Test
    fun `markAsWithdrawn should do nothing if account is already WITHDRAWN`() {
        val user = createTestUser(accountStatus = UserAccountStatus.WITHDRAWN, userStatus = UserStatus.OFFLINE, fcmToken = null)
        val originalUpdatedAt = user.updatedAt

        DateTimeUtil.setFixedNow(laterTime)
        user.markAsWithdrawn()

        assertThat(user.accountStatus).isEqualTo(UserAccountStatus.WITHDRAWN)
        assertThat(user.userStatus).isEqualTo(UserStatus.OFFLINE)
        assertThat(user.fcmToken).isNull()
        assertThat(user.updatedAt).isEqualTo(originalUpdatedAt) // Should not change if already withdrawn
        assertThat(user.pullDomainEvents()).isEmpty()
    }
}
