package com.example.teamnovapersonalprojectprojectingkotlin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.core_navigation.routes.AppRoutes
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Screen Composable imports will be needed here from all feature modules
// Example:
// import com.example.feature_auth.ui.SplashScreen
// import com.example.feature_main.MainScreen
// ... etc.

@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(navController, navigationHandler) {
        navigationHandler.navigationCommands
            .onEach { command ->
                println("Processing NavigationCommand: $command")
                when (command) {
                    is NavigationCommand.NavigateToRoute -> {
                        try {
                            navController.navigate(command.route, command.navOptions)
                        } catch (e: IllegalArgumentException) {
                            println("!!! Navigation Error: Failed to navigate to route: ${command.route}. ${e.message}")
                        }
                    }
                    is NavigationCommand.NavigateUp -> {
                        val success = navController.navigateUp()
                        println("NavigateUp processed. Success: $success")
                    }
                    is NavigationCommand.NavigateBack -> {
                        println("NavigateBack command received in AppNavigationGraph. Typically handled by NavigationManager.")
                    }
                    is NavigationCommand.NavigateToTab -> {
                        navController.navigate(command.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = command.saveState
                            }
                            launchSingleTop = true
                            restoreState = command.restoreState
                        }
                    }
                    is NavigationCommand.NavigateClearingBackStack -> {
                        navController.navigate(command.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                    is NavigationCommand.NavigateToNestedGraph -> {
                        println("NavigateToNestedGraph: Navigating to parent ${command.parentRoute}, child ${command.childRoute} - needs review")
                        navController.navigate(command.parentRoute) 
                    }
                    is NavigationCommand.NavigateWithArguments -> {
                        println("NavigateWithArguments: Route: ${command.route}, Args: ${command.args} - needs review/implementation if used this way")
                        navController.navigate(command.route, command.navOptions)
                    }
                }
            }
            .launchIn(this)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        navigation(
            route = AppRoutes.Auth.Graph.path,
            startDestination = AppRoutes.Auth.Graph.suggestedStartPath
        ) {
            composable(AppRoutes.Auth.Splash.path) { com.example.feature_auth.ui.SplashScreen(navigationHandler) }
            composable(AppRoutes.Auth.Login.path) { com.example.feature_auth.ui.LoginScreen(navigationHandler) }
            composable(AppRoutes.Auth.SignUp.path) { com.example.feature_auth.ui.SignUpScreen(navigationHandler) }
            composable(AppRoutes.Auth.FindPassword.path) { com.example.feature_auth.ui.FindPasswordScreen(navigationHandler) }
        }

        composable(AppRoutes.MainScreens.ROOT) {
            com.example.feature_main.MainScreen(navController = navController, navigationManager = navigationHandler)
        }

        navigation(
            route = AppRoutes.MainScreens.Home.GRAPH_ROOT,
            startDestination = AppRoutes.MainScreens.Home.ROOT_CONTENT
        ) {
            composable(AppRoutes.MainScreens.Home.ROOT_CONTENT) {
                androidx.compose.material3.Text("Home Content Placeholder")
            }
            composable(AppRoutes.Project.ADD) { com.example.feature_project.ui.AddProjectScreen(navigationHandler) }
            composable(AppRoutes.Project.SET_NAME) { com.example.feature_project.ui.SetProjectNameScreen(navigationHandler) }
            composable(AppRoutes.Project.JOIN) { com.example.feature_project.ui.JoinProjectScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.detailRoute(),
                arguments = AppRoutes.Project.detailArguments
            ) {
                com.example.feature_project.ui.ProjectDetailScreen(navigationHandler)
            }
            composable(
                route = AppRoutes.Project.settingsRoute(),
                arguments = AppRoutes.Project.settingsArguments
            ) {
                com.example.feature_project.setting.ui.ProjectSettingScreen(navigationHandler)
            }
            composable(
                route = AppRoutes.Project.createCategoryRoute(),
                arguments = AppRoutes.Project.createCategoryArguments
            ) { com.example.feature_project.structure.ui.CreateCategoryScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.createChannelRoute(),
                arguments = AppRoutes.Project.createChannelArguments
            ) { com.example.feature_project.structure.ui.CreateChannelScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.editCategoryRoute(),
                arguments = AppRoutes.Project.editCategoryArguments
            ) { com.example.feature_project.structure.ui.EditCategoryScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.editChannelRoute(),
                arguments = AppRoutes.Project.editChannelArguments
            ) { com.example.feature_project.structure.ui.EditChannelScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.memberListRoute(),
                arguments = AppRoutes.Project.memberListArguments
            ) { com.example.feature_project.members.ui.MemberListScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.editMemberRoute(),
                arguments = AppRoutes.Project.editMemberArguments
            ) { com.example.feature_project.members.ui.EditMemberScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.roleListRoute(),
                arguments = AppRoutes.Project.roleListArguments
            ) { com.example.feature_project.roles.ui.RoleListScreen(navigationHandler) }
            composable(
                route = AppRoutes.Project.editRoleRoute(),
                arguments = AppRoutes.Project.editRoleArguments
            ) { com.example.feature_project.roles.ui.EditRoleScreen(navigationHandler) }
            composable(
                route = AppRoutes.Chat.channelRoute(),
                arguments = AppRoutes.Chat.channelArguments
            ) { com.example.feature_chat.ui.ChatScreen(navigationHandler) }
        }

        navigation(
            route = AppRoutes.MainScreens.Calendar.GRAPH_ROOT,
            startDestination = AppRoutes.MainScreens.Calendar.ROOT_CONTENT
        ) {
            composable(AppRoutes.MainScreens.Calendar.ROOT_CONTENT) {
                com.example.feature_main.ui.calendar.CalendarScreen(navigationManager = navigationHandler)
            }
            composable(
                route = AppRoutes.MainScreens.Calendar.calendar24HourRoute(),
                arguments = AppRoutes.MainScreens.Calendar.calendar24HourArguments
            ) { com.example.feature_schedule.ui.Calendar24HourScreen(navigationHandler) }
            composable(
                route = AppRoutes.MainScreens.Calendar.addScheduleRoute(),
                arguments = AppRoutes.MainScreens.Calendar.addScheduleArguments
            ) { com.example.feature_schedule.ui.AddScheduleScreen(navigationHandler) }
            composable(
                route = AppRoutes.MainScreens.Calendar.scheduleDetailRoute(),
                arguments = AppRoutes.MainScreens.Calendar.scheduleDetailArguments
            ) { com.example.feature_schedule.ui.ScheduleDetailScreen(navigationHandler) }
            composable(
                route = AppRoutes.MainScreens.Calendar.editScheduleRoute(),
                arguments = AppRoutes.MainScreens.Calendar.editScheduleArguments
            ) { com.example.feature_schedule.ui.EditScheduleScreen(navigationHandler) }
        }

        navigation(
            route = AppRoutes.MainScreens.Profile.GRAPH_ROOT,
            startDestination = AppRoutes.MainScreens.Profile.ROOT_CONTENT
        ) {
            composable(AppRoutes.MainScreens.Profile.ROOT_CONTENT) {
                com.example.feature_main.ui.ProfileScreen(navigationManager = navigationHandler)
            }
            composable(
                route = AppRoutes.User.profileRoute(),
                arguments = AppRoutes.User.profileArguments
            ) {
                com.example.feature_user.ui.UserProfileScreen(navigationHandler)
            }
            composable(AppRoutes.Settings.EDIT_MY_PROFILE) { 
                com.example.feature_settings.ui.EditProfileScreen(navigationHandler) 
            }
            composable(AppRoutes.Settings.CHANGE_MY_PASSWORD) { 
                com.example.feature_settings.ui.ChangePasswordScreen(navigationHandler) 
            }
            composable(AppRoutes.Friends.LIST) { com.example.feature_friends.ui.FriendsScreen(navigationHandler) }
            composable(AppRoutes.Friends.ACCEPT_REQUESTS) { com.example.feature_friends.ui.AcceptFriendsScreen(navigationHandler) }
        }

        composable(AppRoutes.Dev.MENU) { com.example.feature_dev.ui.DevMenuScreen(navigationHandler) } 
        composable(AppRoutes.Search.GLOBAL) { com.example.feature_search.ui.SearchScreen(navigationHandler) }
        
        composable(AppRoutes.Settings.APP_SETTINGS) {
            androidx.compose.material3.Text("App Settings Placeholder")
        }
    }
} 