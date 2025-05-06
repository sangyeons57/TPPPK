# Task: AppNavigation.kt NavigationManager Implementation Errors Resolution

## Problem Analysis
The errors in AppNavigation.kt stem from inconsistent parameter patterns between the old callback-based navigation system and the new NavigationManager approach. Specifically:

1. Screen components expect callbacks like `onNavigateBack` but should now use `navigationManager`
2. Route parameters (projectId, channelId, etc.) need to be properly passed
3. NavHost composable definitions need updating to match the new pattern

## Detailed Steps

- [x] 1. Check the current structure of AppNavigation.kt
  - [x] 1.1: Examine the NavHost structure and composable routes definition
  - [x] 1.2: Identify the pattern of Screen component parameters currently used

  **Analysis Results:**
  - Current AppNavigation.kt uses a NavHost with many composable routes defined.
  - Each route's composable uses a Screen component with the following parameter patterns:
    - All screens have `navigationManager` parameter, which is correct
    - Simple screens: Only `navigationManager` (e.g., LoginScreen, FriendsScreen)
    - Screens with route parameters: Both route params and `navigationManager` (e.g., ProjectSettingScreen with projectId)
    - Complex screens: Multiple route params and `navigationManager` (e.g., EditChannelScreen with projectId, categoryId, channelId)
  - Most routes have proper parameter extraction from navigation arguments
  - **Main issue**: The compile errors show that many Screen components still expect callback parameters (onNavigateBack, onNavigateToLogin, etc.) instead of using NavigationManager directly

- [x] 2. Create a consistent screen parameter pattern
  - [x] 2.1: Define standard parameters for all screens (navigationManager plus any required route params)
  - [x] 2.2: Document the pattern to follow for all screens

  **Screen Parameter Standard:**
  - All screen components should use the following parameter pattern:
    1. Required parameters specific to the screen (projectId, channelId, etc.) should be listed first
    2. NavigationManager should always be the last parameter
    3. Modifier should be an optional parameter with a default value
    4. NO callback parameters for navigation (onNavigateBack, onNavigateToLogin, etc.)
  
  **Example patterns:**
  - Simple screen: `fun ExampleScreen(navigationManager: NavigationManager, modifier: Modifier = Modifier)`
  - With route params: `fun DetailScreen(itemId: String, navigationManager: NavigationManager, modifier: Modifier = Modifier)`
  - Complex screen: `fun EditScreen(projectId: String, categoryId: String, itemId: String, navigationManager: NavigationManager, modifier: Modifier = Modifier)`
  
  **Navigation Implementation:**
  - Inside each screen component, use NavigationManager extension functions directly:
    - `navigationManager.navigateBack()` instead of `onNavigateBack()`
    - `navigationManager.navigateToLogin()` instead of `onNavigateToLogin()`
    - `navigationManager.navigateToMain()` instead of `onNavigateToMain()`
  - For parameterized navigation, use appropriate extension functions:
    - `navigationManager.navigateToProjectSetting(projectId)` instead of `onNavigateToProjectSetting(projectId)`
    - `navigationManager.navigateToChat(channelId)` instead of `onNavigateToChat(channelId)`

- [x] 3. Update Login/Auth flow screens
  - [x] 3.1: Fix LoginScreen parameters and navigation
  - [x] 3.2: Fix SignUpScreen parameters and navigation
  - [x] 3.3: Fix FindPasswordScreen parameters and navigation

  **Auth Flow Analysis Results:**
  - All auth flow screens (LoginScreen, SignUpScreen, FindPasswordScreen, SplashScreen) are already correctly implemented
  - They use the NavigationManager parameter properly
  - They use the NavigationManager extension functions directly for navigation
  - Example: `navigationManager.navigateToLogin()` instead of `onNavigateToLogin()`
  - No changes needed for the auth flow screens

- [x] 4. Update Main screens
  - [x] 4.1: Fix MainScreen parameters and navigation
  - [x] 4.2: Fix ProfileScreen parameters and navigation

  **Main Screens Analysis Results:**
  - Both MainScreen and ProfileScreen are already correctly implemented
  - MainScreen uses the NavigationManager properly and even creates a nested navigation manager
  - ProfileScreen correctly uses NavigationManager for navigation events:
    - Example: `navigationManager.navigateToEditProfile()` instead of callback functions
  - No changes needed for the main screens

- [x] 5. Update Project screens
  - [x] 5.1: Fix AddProjectScreen, SetProjectNameScreen, JoinProjectScreen parameters
    - [x] 5.1.1: Fix JoinProjectScreen to use NavigationManager
    - [x] 5.1.2: Fix AddProjectScreen to use NavigationManager (was already correctly implemented)
    - [x] 5.1.3: Fix SetProjectNameScreen to use NavigationManager
  - [x] 5.2: Fix ProjectSettingScreen parameters and navigation
  - [x] 5.3: Fix CreateCategory/Channel screens parameters and navigation
    - [x] 5.3.1: Fixed CreateChannelScreen to use NavigationManager
    - [x] 5.3.2: Fix CreateCategoryScreen to use NavigationManager
    - [x] 5.3.3: Fix EditChannelScreen to use NavigationManager
    - [x] 5.3.4: Fix EditCategoryScreen to use NavigationManager
  - [x] 5.4: Fix EditCategory/Channel screens parameters and navigation

  **Project Screens Analysis:**
  - AddProjectScreen was already correctly implemented with NavigationManager
  - SetProjectNameScreen has been updated to use NavigationManager:
    - Removed callback parameters (onNavigateBack, onNavigateNext)
    - Updated to use navigationManager.navigateBack() and navigationManager.navigateToSelectProjectType()
    - Updated TopAppBar back button to use NavigationManager
  - ProjectSettingScreen has been updated to use NavigationManager directly:
    - Changed signature to accept projectId and navigationManager
    - Updated LaunchedEffect to use NavigationManager extension functions
    - Updated direct navigation calls like TopAppBar back button
  - CreateChannelScreen has been updated to use NavigationManager:
    - Added missing projectId and categoryId parameters
    - Updated event handler to use navigationManager.navigateBack()
    - Updated TopAppBar back button to use NavigationManager
  - CreateCategoryScreen has been updated to use NavigationManager:
    - Replaced NavController with NavigationManager
    - Added required projectId parameter
    - Updated all navigation calls to use NavigationManager
  - EditChannelScreen has been updated to use NavigationManager:
    - Added projectId, categoryId, and channelId parameters
    - Updated event handlers to use NavigationManager
    - Updated TopAppBar back button and other navigation calls
  - EditCategoryScreen has been updated to use NavigationManager:
    - Added projectId and categoryId parameters
    - Updated all navigation calls to use NavigationManager.navigateBack()
    - Updated TopAppBar back button to use NavigationManager
  - JoinProjectScreen has been updated to use NavigationManager:
    - Removed callback parameters (onNavigateBack, onJoinSuccess)
    - Updated to use navigationManager.navigateBack() and navigationManager.navigateToProjectMain()
    - Updated TopAppBar back button to use NavigationManager
  - All project screens now use the NavigationManager properly

- [x] 6. Update Member/Role management screens
  - [x] 6.1: Fix MemberListScreen parameters and navigation
  - [x] 6.2: Fix EditMemberScreen parameters and navigation
  - [x] 6.3: Fix RoleListScreen parameters and navigation
  - [x] 6.4: Fix EditRoleScreen parameters and navigation

  **Member/Role Management Screens Analysis:**
  - MemberListScreen has been updated to use NavigationManager:
    - Added projectId parameter
    - Removed callback parameters (onNavigateBack, onNavigateToEditMember, onShowAddMemberDialog)
    - Updated to use navigationManager.navigateBack(), navigateToEditMember(), and navigateToAddMember()
    - Updated TopAppBar back button to use NavigationManager
  - EditMemberScreen has been updated to use NavigationManager:
    - Added projectId and userId parameters
    - Removed callback parameter (onNavigateBack)
    - Updated to use navigationManager.navigateBack() in all places
    - Updated TopAppBar back button to use NavigationManager
  - RoleListScreen has been updated to use NavigationManager:
    - Added projectId parameter
    - Removed callback parameters (onNavigateBack, onNavigateToAddRole, onNavigateToEditRole)
    - Updated to use navigationManager extension functions
    - Updated TopAppBar back button to use NavigationManager
  - EditRoleScreen has been updated to use NavigationManager:
    - Added projectId and roleId parameters
    - Removed callback parameters (onNavigateBack)
    - Updated to use navigationManager extension functions
    - Updated TopAppBar back button to use NavigationManager

- [x] 7. Update Friend/Chat screens
  - [x] 7.1: Fix FriendsScreen parameters and navigation
  - [x] 7.2: Fix AcceptFriendsScreen parameters and navigation
  - [x] 7.3: Fix ChatScreen parameters and navigation

  **Friend/Chat Screens Analysis:**
  - FriendsScreen has been updated to use NavigationManager:
    - Removed callback parameters (onNavigateBack, onNavigateToAcceptFriends, onNavigateToChat, onShowAddFriendDialog)
    - Updated to use navigationManager extension functions
    - Updated TopAppBar back button to use NavigationManager
  - AcceptFriendsScreen has been updated to use NavigationManager:
    - Removed callback parameter (onNavigateBack)
    - Updated to use navigationManager.navigateBack()
    - Updated TopAppBar back button to use NavigationManager
  - ChatScreen was already correctly implemented with NavigationManager

- [x] 8. Update Schedule screens
  - [x] 8.1: Fix Calendar24HourScreen parameters and navigation
  - [x] 8.2: Fix AddScheduleScreen parameters and navigation
  - [x] 8.3: Fix ScheduleDetailScreen parameters and navigation

  **Schedule Screens Analysis:**
  - Calendar24HourScreen was already correctly implemented with NavigationManager
  - AddScheduleScreen was already using NavigationManager for navigation
  - ScheduleDetailScreen has been updated to use NavigationManager:
    - Added scheduleId parameter
    - Removed callback parameters (onNavigateBack, onNavigateToEditSchedule)
    - Updated to use navigationManager extension functions
    - Updated TopAppBar back button to use NavigationManager

- [x] 9. Update Search screen
  - [x] 9.1: Fix SearchScreen parameters and navigation

  **Search Screen Analysis:**
  - SearchScreen was already correctly implemented with NavigationManager

- [x] 10. Final verification
  - [x] 10.1: Run a full build and check for any remaining navigation errors
  - [x] 10.2: Test navigation flow through the app

## Detailed Solution Approach

The root cause of the errors is a mismatch between:
1. How AppNavigation.kt is calling screen components (with NavigationManager)
2. How the screen components are defined (with callback functions)

For example, AppNavigation.kt attempts to call:
```kotlin
ProjectSettingScreen(
  projectId = projectId,
  navigationManager = navigationManager
)
```

But the ProjectSettingScreen is defined with multiple callback parameters:
```kotlin
fun ProjectSettingScreen(
  modifier: Modifier = Modifier,
  viewModel: ProjectSettingViewModel = hiltViewModel(),
  onNavigateBack: () -> Unit,
  onNavigateToEditCategory: (String, String) -> Unit,
  // ...more callbacks
)
```

### Fix template for each screen:

1. **Update screen signature**:
   ```kotlin
   // BEFORE - with callbacks
   fun ExampleScreen(
     // ...other params
     onNavigateBack: () -> Unit,
     onNavigateToDetail: (String) -> Unit
   )
   
   // AFTER - with NavigationManager
   fun ExampleScreen(
     // ...required route params
     navigationManager: NavigationManager,
     modifier: Modifier = Modifier
   )
   ```

2. **Update event handlers in LaunchedEffect**:
   ```kotlin
   // BEFORE - with callbacks
   LaunchedEffect(Unit) {
     viewModel.eventFlow.collectLatest { event ->
       when (event) {
         is ExampleEvent.NavigateBack -> onNavigateBack()
         is ExampleEvent.NavigateToDetail -> onNavigateToDetail(event.itemId)
       }
     }
   }
   
   // AFTER - with NavigationManager
   LaunchedEffect(Unit) {
     viewModel.eventFlow.collectLatest { event ->
       when (event) {
         is ExampleEvent.NavigateBack -> navigationManager.navigateBack()
         is ExampleEvent.NavigateToDetail -> navigationManager.navigateToDetail(event.itemId)
       }
     }
   }
   ```

3. **Update direct navigation calls**:
   ```kotlin
   // BEFORE - with callbacks
   IconButton(onClick = onNavigateBack) { /*...*/ }
   
   // AFTER - with NavigationManager
   IconButton(onClick = { navigationManager.navigateBack() }) { /*...*/ }
   ```

### Documentation

When implementing each screen, refer to NavigationManagerExtensions.kt for the available extension functions to handle specific navigation operations such as:
- `navigateBack()`
- `navigateToProjectSetting(projectId)`
- `navigateToEditCategory(projectId, categoryId)`
- etc.

This ensures type-safe navigation with proper parameters according to the route definition. 