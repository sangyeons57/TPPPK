# Navigation Component Migration Plan

## Overview
This document outlines the plan to move all navigation components from `core_common/navigation` to a dedicated `core_navigation` module.

## Files to Move
Move the following files from `core/core_common/src/main/java/com/example/core_common/navigation/` to `core/core_navigation/src/main/java/com/example/core_navigation/`:

1. `AppDestinations.kt`
2. `SavedStateHandleUtils.kt`
3. `NavigationManagerExtensions.kt`
4. `NavigationManager.kt`
5. `SentryNavigationTracker.kt`
6. `SentryNavigationHelper.kt`
7. `NavControllerExtensions.kt`
8. `NavigationManagerComposables.kt`
9. `NestedNavigationManagerFactory.kt`
10. `SavedStateNavigationResultHandler.kt`
11. `NavigationResultListener.kt`
12. `NavigationHandler.kt`
13. `NavigationCommand.kt`
14. `ComposeNavigationHandler.kt`
15. `README.md`

## Package Name Updates
Update package declarations from `com.example.core_common.navigation` to `com.example.core_navigation`.

## Import Updates
After moving files, update import statements across the codebase:
- Search for `import com.example.core_common.navigation.*`
- Replace with `import com.example.core_navigation.*`

## Module Dependencies
Modules that depend on navigation functionality need to update their dependencies:
1. Add `implementation(project(":core:core_navigation"))` in their `build.gradle.kts` files
2. Check if they can remove `implementation(project(":core:core_common"))` if only used for navigation
3. If other core_common functionality is used, keep the dependency

## Navigation Module
Move `NavigationModule.kt` from `core_common/di` to `core_navigation/di` and update its package.

## Testing
After migration:
1. Rebuild the project to identify any missed imports
2. Run unit tests related to navigation
3. Test basic navigation flows in the app

## Rollback Plan
If issues are encountered:
1. Revert all changes
2. Ensure the original navigation functionality in core_common is preserved
3. Reassess the migration approach 