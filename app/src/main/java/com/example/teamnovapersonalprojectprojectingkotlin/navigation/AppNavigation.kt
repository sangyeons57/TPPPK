package com.example.teamnovapersonalprojectprojectingkotlin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.ui.* // auth ui import
import com.example.teamnovapersonalprojectprojectingkotlin.feature_dev.ui.DevMenuScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.ui.AcceptFriendsScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.ui.FriendsScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.MainScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.ui.AddProjectScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.ui.JoinProjectScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.ui.SetProjectNameScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_members.ui.EditMemberScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_members.ui.MemberListScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_roles.ui.EditRoleScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_roles.ui.RoleListScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_setting.ui.ProjectSettingScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_structure.ui.* // project structure ui import
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.ui.* // schedule ui import
import com.example.teamnovapersonalprojectprojectingkotlin.feature_search.ui.SearchScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_settings.ui.ChangePasswordScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_settings.ui.EditProfileScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_chat.ui.ChatScreen // ChatScreen import

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
            DevMenuScreen(navController);
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
        composable(route = Login.route) { LoginScreen(navController); }
        composable(route = SignUp.route) { SignUpScreen(navController); }
        composable(route = FindPassword.route) { FindPasswordScreen(navController) }

        // 메인 화면 (Bottom Nav 포함)
        composable(route = Main.route) { MainScreen(navController) }

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
                onNavigateToEditCategory = { projectId, categoryId -> navController.navigate(EditCategory.createRoute(projectId, categoryId)) },
                onNavigateToCreateCategory = { projectId -> navController.navigate(CreateCategory.createRoute(projectId)) },
                onNavigateToEditChannel = { projectId, categoryId, channelId -> navController.navigate(EditChannel.createRoute(projectId, categoryId, channelId)) },
                onNavigateToCreateChannel = { projectId, categoryId -> navController.navigate(CreateChannel.createRoute(projectId, categoryId)) },
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
                modifier = TODO(),
                viewModel = TODO(),
                onChangeNameClick = TODO(),
                onChangeStatusClick = TODO()
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