# Usecase Implementation Requirements

This document lists the necessary changes to other layers (Repository, DataSource, etc.) identified during the implementation of Usecases.

## AppContentRepository

- **Method**: `getPrivacyPolicyText()`
  - **Change**: This new method should return `com.example.domain.model.CustomResult<String>`.
  - **Reason**: Required by `WorkspacePrivacyPolicyTextUseCase` to fetch the privacy policy content.

## AuthRepository

- **Method**: `requestPasswordResetCode(email: String)`
  - **Change**: Modify the return type from `kotlin.Result<Unit>` to `com.example.domain.model.CustomResult<Unit>`.
  - **Reason**: To align with the `RequestPasswordResetUseCase` which now uses `CustomResult`.

- **Method**: `verifyPasswordResetCode(code: String)`
  - **Change**: Modify parameters from `(email: String, code: String)` to `(code: String)` and change return type from `kotlin.Result<Unit>` to `com.example.domain.model.CustomResult<String?>`.
  - **Reason**: To align with `VerifyPasswordResetCodeUseCase` which now takes only `code` and expects a `CustomResult<String?>` (token or identifier).

- **Method**: `confirmPasswordReset(codeOrToken: String, newPassword: String)`
  - **Change**: Add this new method. It should accept `codeOrToken: String` and `newPassword: String` as parameters and return `com.example.domain.model.CustomResult<Unit>`.
  - **Reason**: Required by the new `ConfirmPasswordResetWithCodeUseCase` to finalize password reset.

- **Method**: `login(email: String, password: String)`
  - **Change**: Modify the return type from `kotlin.Result<User?>` to `com.example.domain.model.CustomResult<com.example.domain.model.UserSession>`.
  - **Reason**: To align with the `AttemptLoginUseCase` which now returns `CustomResult<UserSession>` and handles login attempts.

- **Method**: `getCurrentUserSession()`
  - **Change**: This method should return `com.example.domain.model.CustomResult<com.example.domain.model.UserSession?>`. (This might be a new method or a modification of an existing `checkSession()` method).
  - **Reason**: Required by the `CheckInitialSessionUseCase` to determine if there's a valid user session at app start.

- **Method**: `observeAuthenticationState()`
  - **Change**: This method should return `kotlinx.coroutines.flow.Flow<com.example.domain.model.auth.AuthenticationState>`. (This is likely a new method).
  - **Reason**: Required by the `ObserveAuthenticationStateUseCase` to provide a stream of authentication state changes.

- **Method**: `checkIfEmailExists(email: String)`
  - **Change**: Add this new method. It should accept `email: String` as a parameter and return `com.example.domain.model.CustomResult<Boolean>` (true if email exists, false otherwise).
  - **Reason**: Required by `ValidateEmailForSignUpUseCase` to check if an email is already registered.

- **Method**: `registerUser(params: com.example.domain.model.auth.UserRegistrationParams)`
  - **Change**: Add this new method (or modify existing `signUp`). It should accept `UserRegistrationParams` and return `com.example.domain.model.CustomResult<com.example.domain.model.UserSession>`.
  - **Reason**: Required by `RegisterUserUseCase` to perform user registration and return a user session.

- **Method**: `sendEmailVerification()`
  - **Change**: Modify the return type from `kotlin.Result<Unit>` to `com.example.domain.model.CustomResult<Unit>`.
  - **Reason**: Required by `RequestEmailVerificationAfterSignUpUseCase` for consistent error handling.

- **Method**: `observeCurrentUserId(): kotlinx.coroutines.flow.Flow<com.example.domain.model.CustomResult<String?>>`
  - **Change**: Add this new method. It should observe the ID of the currently authenticated user.
  - **Reason**: Required by various use cases that need to react to user session changes or fetch user-specific data, e.g., `ObserveIncomingFriendRequestsUseCase`.

## UserRepository

- **Method**: `checkNicknameAvailability(nickname: String)`
  - **Change**: Modify the return type from `kotlin.Result<Boolean>` to `com.example.domain.model.CustomResult<Boolean>` (true if nickname is available, false if taken).
  - **Reason**: Required by `ValidateNicknameForSignUpUseCase` for consistent error handling and to check nickname availability.

- **Method**: `observeCurrentUser(): kotlinx.coroutines.flow.Flow<com.example.domain.model.CustomResult<com.example.domain.model.User?>>`
  - **Change**: Add this new method. It should observe the current authenticated user's profile data.
  - **Reason**: Required by `DetermineInitialDestinationUseCase` (indirectly, to get onboarding status) and other use cases needing live updates to user data (e.g., `ObserveMySettingsProfileUseCase`). The `User` object returned should include an `isOnboardingComplete: Boolean` field (see Shared Enums and Models section).

- **Method**: `observeFriends(currentUserId: String): kotlinx.coroutines.flow.Flow<com.example.domain.model.CustomResult<List<com.example.domain.model.friend.Friend>>>`
  - **Change**: Add this new method. It should observe the list of friends and their statuses for the given user ID.
  - **Reason**: Required by `ObserveIncomingFriendRequestsUseCase`, `ObserveFriendListUseCase`, and other friend-related use cases to get a live stream of friend data.

- **Method**: `acceptFriendRequest(currentUserId: String, friendId: String): com.example.domain.model.CustomResult<Unit>`
  - **Change**: Add this new method. It should process the acceptance of a friend request.
  - **Reason**: Required by `ProcessFriendRequestAcceptanceUseCase` to accept an incoming friend request.

- **Method**: `denyFriendRequest(currentUserId: String, friendId: String): com.example.domain.model.CustomResult<Unit>`
  - **Change**: Add this new method. It should process the denial of a friend request.
  - **Reason**: Required by `ProcessFriendRequestDenialUseCase` to deny an incoming friend request.

- **Method**: `refreshFriendRequests(currentUserId: String): com.example.domain.model.CustomResult<Unit>`
  - **Change**: Add this new method. It should trigger a manual refresh of the friend requests list for the current user.
  - **Reason**: Required by `WorkspaceIncomingFriendRequestsUseCase` to allow manual refresh of friend requests.

- **Method**: `searchUsers(query: String, currentUserId: String): com.example.domain.model.CustomResult<List<com.example.domain.model.search.UserSearchResult>>`
  - **Change**: Add this new method. It should search for users based on a query and return their details along with their friendship status relative to the current user.
  - **Reason**: Required by `SearchUsersByQueryUseCase` to find users and display their relationship status.

## Shared Enums and Models

### Enums

- **Enum**: `com.example.domain.model.navigation.SplashDestination`
  - **Definition**:

    ```kotlin
    package com.example.domain.model.navigation // Or appropriate package

    enum class SplashDestination {
        ONBOARDING, // Navigate to the Onboarding flow.
        LOGIN,      // Navigate to the Login screen.
        MAIN_APP,   // Navigate to the Main application screen (e.g., Home).
        PENDING     // Intermediate state, possibly waiting for more data.
    }
    ```

  - **Reason**: Represents the possible navigation destinations from the Splash screen, determined by `DetermineInitialDestinationUseCase`.

- **Enum**: `com.example.domain.model.friend.FriendStatus` (New Enum)
  - **Definition**:

    ```kotlin
    package com.example.domain.model.friend // Or appropriate package

    enum class FriendStatus {
        PENDING_SENT,     // Friend request sent by the current user, awaiting response
        PENDING_RECEIVED, // Friend request received by the current user, awaiting action
        ACCEPTED,         // Friend request accepted, users are friends
        DECLINED_SENT,    // Current user declined a received request
        DECLINED_RECEIVED,// Other user declined current user's request
        BLOCKED_BY_ME,    // Current user blocked the other user
        BLOCKED_BY_OTHER, // Other user blocked the current user
        REMOVED           // Friendship was removed by either party
    }
    ```

  - **Reason**: Represents the various states of a friend relationship.

### Model Updates

- **Model**: `com.example.domain.model.User`
  - **Change**: Ensure this model includes a field: `val isOnboardingComplete: Boolean`.
  - **Reason**: Required by `DetermineInitialDestinationUseCase` to decide if the user needs to go through onboarding.

- **Model**: `com.example.domain.model.friend.Friend` (New Model)
  - **Definition**:

    ```kotlin
    package com.example.domain.model.friend // Or appropriate package

    import java.time.Instant

    data class Friend(
        val userId: String, // The ID of the friend
        val displayName: String? = null,
        val profileImageUrl: String? = null,
        val status: FriendStatus,
        val lastMessage: String? = null, // Optional: for display in friend lists
        val lastInteractionTime: Instant? = null, // Optional: for sorting or status indicators
        val createdAt: Instant = Instant.now()
    )
    ```

  - **Reason**: Represents a friend relationship, used in various friend-related use cases.

- **Model**: `com.example.domain.model.search.UserSearchResult` (New Model)
  - **Definition**:

    ```kotlin
    package com.example.domain.model.search // Or com.example.domain.model.friend

    import com.example.domain.model.User
    import com.example.domain.model.friend.FriendStatus

    data class UserSearchResult(
        val user: User,
        val friendshipStatus: FriendStatus? // Null if no existing relationship or not applicable
    )
    ```

  - **Reason**: Represents a user found in a search, including their friendship status relative to the searching user. Required by `SearchUsersByQueryUseCase`.

## StaticContentRepository

- **Method**: `getTermsOfServiceText()`
  - **Change**: This new method should return `com.example.domain.model.CustomResult<String>`.
  - **Reason**: Required by `WorkspaceTermsOfServiceTextUseCase` to fetch the terms of service content.

- **Method**: `getPrivacyPolicyText()`
  - **Change**: This new method should return `com.example.domain.model.CustomResult<String>`.
  - **Reason**: Required by `WorkspacePrivacyPolicyTextUseCase` to fetch the privacy policy content (replaces the need for the previously deleted `AppContentRepository`).
