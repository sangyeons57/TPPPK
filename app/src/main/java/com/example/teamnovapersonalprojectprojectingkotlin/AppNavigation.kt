package com.example.teamnovapersonalprojectprojectingkotlin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.feature_auth.ui.FindPasswordScreen
import com.example.feature_auth.ui.LoginScreen
import com.example.feature_auth.ui.SignUpScreen
import com.example.feature_auth.ui.SplashScreen
import com.example.feature_chat.ui.ChatScreen // ChatScreen import
import com.example.feature_dev.DevMenuScreen
import com.example.feature_friends.ui.AcceptFriendsScreen
import com.example.feature_friends.ui.FriendsScreen
import com.example.feature_main.MainScreen
import com.example.feature_project.members.ui.EditMemberScreen
import com.example.feature_project.members.ui.MemberListScreen
import com.example.feature_project.roles.ui.EditRoleScreen
import com.example.feature_project.roles.ui.RoleListScreen
import com.example.feature_project.setting.ui.ProjectSettingScreen
import com.example.feature_project.structure.ui.CreateCategoryScreen
import com.example.feature_project.structure.ui.CreateChannelScreen
import com.example.feature_project.structure.ui.EditCategoryScreen
import com.example.feature_project.structure.ui.EditChannelScreen
import com.example.feature_project.ui.AddProjectScreen
import com.example.feature_project.ui.JoinProjectScreen
import com.example.feature_project.ui.SetProjectNameScreen
import com.example.feature_schedule.ui.AddScheduleScreen
import com.example.feature_schedule.ui.Calendar24HourScreen
import com.example.feature_schedule.ui.ScheduleDetailScreen
import com.example.feature_search.ui.SearchScreen
import com.example.feature_settings.ui.ChangePasswordScreen
import com.example.feature_settings.ui.EditProfileScreen
import com.example.navigation.AcceptFriends
import com.example.navigation.AddProject
import com.example.navigation.AddSchedule
import com.example.navigation.Calendar24Hour
import com.example.navigation.ChangePassword
import com.example.navigation.Chat
import com.example.navigation.CreateCategory
import com.example.navigation.CreateChannel
import com.example.navigation.DevMenu
import com.example.navigation.EditCategory
import com.example.navigation.EditChannel
import com.example.navigation.EditMember
import com.example.navigation.EditProfile
import com.example.navigation.EditRole
import com.example.navigation.FindPassword
import com.example.navigation.Friends
import com.example.navigation.JoinProject
import com.example.navigation.Login
import com.example.navigation.Main
import com.example.navigation.MemberList
import com.example.navigation.ProjectSetting
import com.example.navigation.RoleList
import com.example.navigation.ScheduleDetail
import com.example.navigation.Search
import com.example.navigation.SetProjectName
import com.example.navigation.SignUp
import com.example.navigation.Splash

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    //startDestination: String = Splash.route // 실제 앱 시작점은 Splash
    startDestination: String = DevMenu.route // 개발/테스트 시 DevMenu로 시작
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 개발 메뉴
        composable(route = DevMenu.route) {
            // DevMenuScreen 호출 시 모든 네비게이션 람다 구현하여 전달
            DevMenuScreen(
                onNavigateBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                onNavigateToSplash = { navController.navigate(Splash.route) },
                onNavigateToLogin = { navController.navigate(Login.route) },
                onNavigateToSignUp = { navController.navigate(SignUp.route) },
                onNavigateToFindPassword = { navController.navigate(FindPassword.route) },
                onNavigateToMain = { navController.navigate(Main.route) },
                onNavigateToAddProject = { navController.navigate(AddProject.route) },
                onNavigateToSetProjectName = { navController.navigate(SetProjectName.route) },
                onNavigateToJoinProject = { navController.navigate(JoinProject.route) },
                onNavigateToProjectSetting = { projectId -> navController.navigate(ProjectSetting.createRoute(projectId)) },
                onNavigateToCreateCategory = { projectId -> navController.navigate(CreateCategory.createRoute(projectId)) },
                onNavigateToCreateChannel = { projectId, categoryId -> navController.navigate(
                    CreateChannel.createRoute(projectId, categoryId)) },
                onNavigateToEditCategory = { projectId, categoryId -> navController.navigate(
                    EditCategory.createRoute(projectId, categoryId)) },
                onNavigateToEditChannel = { projectId, categoryId, channelId -> navController.navigate(
                    EditChannel.createRoute(projectId, categoryId, channelId)) },
                onNavigateToMemberList = { projectId -> navController.navigate(MemberList.createRoute(projectId)) },
                onNavigateToEditMember = { projectId, userId -> navController.navigate(EditMember.createRoute(projectId, userId)) },
                onNavigateToRoleList = { projectId -> navController.navigate(RoleList.createRoute(projectId)) },
                onNavigateToAddRole = { projectId -> navController.navigate(EditRole.createAddRoute(projectId)) },
                onNavigateToEditRole = { projectId, roleId -> navController.navigate(EditRole.createEditRoute(projectId, roleId)) },
                onNavigateToFriends = { navController.navigate(Friends.route) },
                onNavigateToAcceptFriends = { navController.navigate(AcceptFriends.route) },
                onNavigateToEditProfile = { navController.navigate(EditProfile.route) },
                onNavigateToChangePassword = { navController.navigate(ChangePassword.route) },
                onNavigateToChat = { channelId -> navController.navigate(Chat.createRoute(channelId)) },
                onNavigateToCalendar24Hour = { year, month, day -> navController.navigate(
                    Calendar24Hour.createRoute(year, month, day)) },
                onNavigateToAddSchedule = { year, month, day -> navController.navigate(AddSchedule.createRoute(year, month, day)) },
                onNavigateToScheduleDetail = { scheduleId -> navController.navigate(ScheduleDetail.createRoute(scheduleId)) },
                onNavigateToSearch = { navController.navigate(Search.route) }
            )
        }

        // 스플래시 화면
        composable(route = Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Login.route) {
                        popUpTo(Splash.route) { inclusive = true } // 스플래시 화면 제거
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Main.route) {
                        popUpTo(Splash.route) { inclusive = true } // 스플래시 화면 제거
                    }
                }
            )
        }

        // 인증 관련 화면
        composable(route = Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(SignUp.route)
                },
                onNavigateToFindPassword ={
                    navController.navigate(FindPassword.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Main.route){
                        popUpTo(Login.route) { inclusive = true }
                    }
                },
            )
        }
        composable(route = SignUp.route) { SignUpScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLogin = {
                // 회원가입 성공 후 로그인 화면으로 이동 (기존 스택 지우기)
                navController.navigate(Login.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable(route = FindPassword.route) { FindPasswordScreen(navController) }

        // ★ MainScreen 호출 부분 ★
        composable(route = Main.route) {
            MainScreen( // MainScreen 호출 및 외부 네비게이션 람다 전달
                onNavigateToAddProject = { navController.navigate(AddProject.route) },
                onNavigateToFriends = { navController.navigate(Friends.route) },
                onNavigateToSettings = { /* navController.navigate(Settings.route) */ },
                onLogout = {
                    navController.navigate(Login.route) {
                        popUpTo(Main.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToScheduleDetail = { scheduleId ->
                    navController.navigate(ScheduleDetail.createRoute(scheduleId))
                },
                onNavigateToAddSchedule = { year, month, day ->
                    navController.navigate(AddSchedule.createRoute(year, month, day))
                },
                onNavigateToCalendar24Hour = { year, month, day ->
                    navController.navigate(Calendar24Hour.createRoute(year, month, day))
                },
                // ... MainScreen이 필요로 하는 다른 외부 네비게이션 람다 ...
            )
        }


        // 프로젝트 관련 화면
        composable(route = AddProject.route) { AddProjectScreen(navController) }
        composable(route = SetProjectName.route) {
            SetProjectNameScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { /* TODO: 다음 프로젝트 생성 단계로 이동 */
                    navController.navigate(Main.route) { // 임시: 메인으로 이동
                        popUpTo(AddProject.route) { inclusive = true }
                        popUpTo(SetProjectName.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = JoinProject.route) {
            JoinProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onJoinSuccess = { projectId ->
                    // TODO: 성공 시 해당 프로젝트 화면 또는 메인으로 이동
                    navController.navigate(Main.route) { // 임시: 메인으로 이동
                        popUpTo(JoinProject.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = ProjectSetting.routeWithArgs, arguments = ProjectSetting.arguments) {
            ProjectSettingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditCategory = { projectId, categoryId -> navController.navigate(
                    EditCategory.createRoute(projectId, categoryId)) },
                onNavigateToCreateCategory = { projectId -> navController.navigate(CreateCategory.createRoute(projectId)) },
                onNavigateToEditChannel = { projectId, categoryId, channelId -> navController.navigate(
                    EditChannel.createRoute(projectId, categoryId, channelId)) },
                onNavigateToCreateChannel = { projectId, categoryId -> navController.navigate(
                    CreateChannel.createRoute(projectId, categoryId)) },
                onNavigateToMemberList = { projectId -> navController.navigate(MemberList.createRoute(projectId)) },
                onNavigateToRoleList = { projectId -> navController.navigate(RoleList.createRoute(projectId)) }
            )
        }

        // 프로젝트 구조 관리 화면
        composable(route = CreateCategory.routeWithArgs, arguments = CreateCategory.arguments) {
            CreateCategoryScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = TODO(),
                modifier = TODO(),
                viewModel = TODO()
            )
        }
        composable(route = CreateChannel.routeWithArgs, arguments = CreateChannel.arguments) {
            CreateChannelScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = EditCategory.routeWithArgs, arguments = EditCategory.arguments) {
            EditCategoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = EditChannel.routeWithArgs, arguments = EditChannel.arguments) {
            EditChannelScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 멤버 및 역할 관리 화면
        composable(route = MemberList.routeWithArgs, arguments = MemberList.arguments) {
            MemberListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditMember = { projectId, userId ->
                    navController.navigate(
                        EditMember.createRoute(
                            projectId,
                            userId
                        )
                    )
                },
                modifier = TODO(),
                viewModel = TODO(),
                onShowAddMemberDialog = TODO()
            )
        }
        composable(route = EditMember.routeWithArgs, arguments = EditMember.arguments) {
            EditMemberScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = RoleList.routeWithArgs, arguments = RoleList.arguments) {
            RoleListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddRole = { projectId -> navController.navigate(EditRole.createAddRoute(projectId)) },
                onNavigateToEditRole = { projectId, roleId -> navController.navigate(EditRole.createEditRoute(projectId, roleId)) }
            )
        }
        composable(route = EditRole.routeWithArgs, arguments = EditRole.arguments) {
            EditRoleScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 친구 관련 화면
        composable(route = Friends.route) {
            println("test")
            FriendsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAcceptFriends = { navController.navigate(AcceptFriends.route) },
                onNavigateToChat = { channelId -> navController.navigate(Chat.createRoute(channelId)) },
                onShowAddFriendDialog = { /* TODO: 다이얼로그 표시 로직 */ }
            )
        }
        composable(route = AcceptFriends.route) {
            AcceptFriendsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 설정 관련 화면
        composable(route = EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onChangeNameClick = TODO(),
                onChangeStatusClick = TODO(),
            )
        }
        composable(route = ChangePassword.route) {
            ChangePasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 채팅 화면
        composable(route = Chat.routeWithArgs, arguments = Chat.arguments) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                //onNavigateToUserProfile = { /* TODO: 사용자 프로필 화면 또는 다이얼로그 */ },
                //onNavigateToMessage = { _, _ -> /* TODO: 메시지 위치로 이동 등 */ }
            )
        }

        // 캘린더/스케줄 화면
        composable(route = Calendar24Hour.routeWithArgs, arguments = Calendar24Hour.arguments) {
            Calendar24HourScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddSchedule = { year, month, day -> navController.navigate(AddSchedule.createRoute(year, month, day)) },
                onNavigateToScheduleDetail = { scheduleId -> navController.navigate(ScheduleDetail.createRoute(scheduleId)) }
            )
        }
        composable(route = AddSchedule.routeWithArgs, arguments = AddSchedule.arguments) {
            AddScheduleScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = ScheduleDetail.routeWithArgs, arguments = ScheduleDetail.arguments) {
            ScheduleDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditSchedule = { /* TODO: 일정 수정 화면으로 이동 */ }
            )
        }

        // 검색 화면
        composable(route = Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { /* TODO: 사용자 프로필 */ },
                onNavigateToChat = { _, channelId ->
                    navController.navigate(
                        Chat.createRoute(
                            channelId.toString()
                        )
                    )
                },
                modifier = TODO(),
                viewModel = TODO(),
            )
        }
    }
}