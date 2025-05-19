/*
package com.example.core_navigation.util

import com.example.core_navigation.core.NavigationManager
import com.example.core_navigation.destination.AcceptFriends
import com.example.core_navigation.destination.AddProject
import com.example.core_navigation.destination.AddSchedule
import com.example.core_navigation.destination.Calendar24Hour
import com.example.core_navigation.destination.ChangePassword
import com.example.core_navigation.destination.Chat
import com.example.core_navigation.destination.CreateCategory
import com.example.core_navigation.destination.CreateChannel
import com.example.core_navigation.destination.DevMenu
import com.example.core_navigation.destination.EditCategory
import com.example.core_navigation.destination.EditChannel
import com.example.core_navigation.destination.EditMember
import com.example.core_navigation.destination.EditProfile
import com.example.core_navigation.destination.EditRole
import com.example.core_navigation.destination.FindPassword
import com.example.core_navigation.destination.Friends
import com.example.core_navigation.destination.JoinProject
import com.example.core_navigation.destination.Login
import com.example.core_navigation.destination.Main
import com.example.core_navigation.destination.MemberList
import com.example.core_navigation.destination.ProjectSetting
import com.example.core_navigation.destination.RoleList
import com.example.core_navigation.destination.ScheduleDetail
import com.example.core_navigation.destination.Search
import com.example.core_navigation.destination.SetProjectName
import com.example.core_navigation.destination.SignUp
import com.example.core_navigation.destination.Splash
import com.example.core_navigation.destination.UserProfile

/**
 * 이전 네비게이션 호환성을 위한 확장 함수들
 * 
 * core_common.navigation에서 사용하던 함수들을 대체합니다.
 */

// 인증 관련 이동
fun NavigationManager.navigateToSplash() = navigateTo(Splash.route)
fun NavigationManager.navigateToLogin() = navigateTo(Login.route)
fun NavigationManager.navigateToSignUp() = navigateTo(SignUp.route)
fun NavigationManager.navigateToFindPassword() = navigateTo(FindPassword.route)
fun NavigationManager.navigateToMain() = navigateTo(Main.route)

// 프로필 관련 이동
fun NavigationManager.navigateToUserProfile(userId: String) = navigateTo(UserProfile.createRoute(userId))
fun NavigationManager.navigateToEditProfile() = navigateTo(EditProfile.route)
fun NavigationManager.navigateToChangePassword() = navigateTo(ChangePassword.route)

// 프로젝트 관련 이동
fun NavigationManager.navigateToAddProject() = navigateTo(AddProject.route)
fun NavigationManager.navigateToSetProjectName() = navigateTo(SetProjectName.route)
fun NavigationManager.navigateToJoinProject() = navigateTo(JoinProject.route)
fun NavigationManager.navigateToProjectSetting(projectId: String) = navigateTo(ProjectSetting.createRoute(projectId))

// 프로젝트 구조 관련 이동
fun NavigationManager.navigateToCreateCategory(projectId: String) = navigateTo(CreateCategory.createRoute(projectId))
fun NavigationManager.navigateToCreateChannel(projectId: String, categoryId: String) = 
    navigateTo(CreateChannel.createRoute(projectId, categoryId))
fun NavigationManager.navigateToEditCategory(projectId: String, categoryId: String) = 
    navigateTo(EditCategory.createRoute(projectId, categoryId))
fun NavigationManager.navigateToEditChannel(projectId: String, categoryId: String, channelId: String) = 
    navigateTo(EditChannel.createRoute(projectId, categoryId, channelId))

// 멤버 관련 이동
fun NavigationManager.navigateToMemberList(projectId: String) = navigateTo(MemberList.createRoute(projectId))
fun NavigationManager.navigateToEditMember(projectId: String, userId: String) = 
    navigateTo(EditMember.createRoute(projectId, userId))
fun NavigationManager.navigateToRoleList(projectId: String) = navigateTo(RoleList.createRoute(projectId))
fun NavigationManager.navigateToEditRole(projectId: String, roleId: String) = 
    navigateTo(EditRole.createEditRoute(projectId, roleId))
fun NavigationManager.navigateToAddRole(projectId: String) = navigateTo(EditRole.createAddRoute(projectId))

// 친구 관련 이동
fun NavigationManager.navigateToFriends() = navigateTo(Friends.route)
fun NavigationManager.navigateToAcceptFriends() = navigateTo(AcceptFriends.route)

// 채팅 관련 이동
fun NavigationManager.navigateToChat(channelId: String) = navigateTo(Chat.createRoute(channelId))
fun NavigationManager.navigateToChatWithMessage(channelId: String, messageId: String) = 
    navigateTo(Chat.createRouteWithMessage(channelId, messageId))

// 일정 관련 이동
fun NavigationManager.navigateToCalendar24Hour(year: Int, month: Int, day: Int) = 
    navigateTo(Calendar24Hour.createRoute(year, month, day))
fun NavigationManager.navigateToAddSchedule(year: Int, month: Int, day: Int) = 
    navigateTo(AddSchedule.createRoute(year, month, day))
fun NavigationManager.navigateToScheduleDetail(scheduleId: String) = 
    navigateTo(ScheduleDetail.createRoute(scheduleId))
fun NavigationManager.navigateToEditSchedule(scheduleId: String) =
    navigateTo(ScheduleDetail.createRoute(scheduleId))

// 검색 관련 이동
fun NavigationManager.navigateToSearch() = navigateTo(Search.route)

// 개발 메뉴
fun NavigationManager.navigateToDevMenu() = navigateTo(DevMenu.route)
*/ 

// 변경 후

package com.example.core_navigation.extension

import android.os.Bundle
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.core.NavigationManager
import com.example.core_navigation.destination.AppRoutes

/**
 * NavigationManager 및 AppNavigator의 확장 메서드들
 * 
 * 특정 화면으로 이동하는 편의 기능을 제공합니다.
 */

// 프로젝트 상세 화면 이동 관련 확장 함수
/**
 * 프로젝트 상세 화면으로 이동
 */
fun AppNavigator.navigateToProjectDetails(projectId: String) {
    // 프로젝트 상세 경로 생성
    val projectDetailRoute = AppRoutes.Project.detail(projectId)
    val projectDestination = NavDestination.fromRoute(projectDetailRoute)
    val command = NavigationCommand.NavigateToRoute(projectDestination, emptyMap())

    navigateToProjectDetails(projectId, command)
}

/**
 * NavigationCommand를 사용하여 프로젝트 상세 화면으로 이동
 */
fun AppNavigator.navigateToProjectDetails(projectId: String, command: NavigationCommand.NavigateToRoute) {
    when (this) {
        is NavigationManager -> {
            // 자식 NavController가 있는지 확인 (MainScreen 내부인지)
            activeChildNavController?.let { childNav ->
                println("NavigationManager: Child NavController 사용하여 프로젝트 상세로 이동: $projectId")
                try {
                    // Home 탭 내부의 프로젝트 상세로 이동 - 내부 경로는 다를 수 있음
                    val internalRoute = "project_detail/$projectId"
                    val internalDestination = NavDestination.fromRoute(internalRoute)
                    
                    childNav.navigate(internalDestination.route)
                    return // 성공적으로 이동했으므로 함수 종료
                } catch (e: Exception) {
                    println("NavigationManager: 자식 NavController로 이동 실패, 부모 NavController 사용: ${e.message}")
                    // 실패 시 최상위 NavController로 이동 (아래 로직)
                }
            }
            
            // 자식 NavController가 없거나 이동 실패 시 최상위 NavController로 이동
            println("NavigationManager: 최상위 NavController 사용하여 프로젝트 상세로 이동: $projectId")
            navigate(command)
        }
        else -> navigate(command)
    }
}

// 채팅 화면 이동 관련 확장 함수
/**
 * 채팅 화면으로 이동
 * AppRoutes의 chat 경로를 사용합니다.
 *
 * @param channelId 이동할 채널의 ID
 * @param messageId 스크롤할 메시지 ID (옵션)
 */
fun AppNavigator.navigateToChat(channelId: String, messageId: String? = null) {
    val route = AppRoutes.Chat.screen(channelId, messageId)
    val chatDestination = NavDestination.fromRoute(route)
    
    // 선택적으로 추가 인자들을 전달할 수 있음
    val args = mutableMapOf<String, Any>()
    messageId?.let { args["messageId"] = it }
    
    val command = NavigationCommand.NavigateToRoute(chatDestination, args)
    navigateToChat(channelId, messageId, command)
}

/**
 * NavigationCommand를 사용하여 채팅 화면으로 이동
 */
fun AppNavigator.navigateToChat(channelId: String, messageId: String?, command: NavigationCommand.NavigateToRoute) {
    navigate(command)
}

// 프로젝트 내부 네비게이션 관련 확장 함수
/**
 * 탭 내부에서 프로젝트 상세 화면으로 이동
 */
fun AppNavigator.navigateToProjectDetailsNested(projectId: String) {
    println("AppNavigator: navigateToProjectDetailsNested 호출 - 프로젝트 ID: $projectId")
    
    // 표준 네비게이션으로 처리
    val projectDestination = NavDestination.fromRoute("project_detail")
    val command = NavigationCommand.NavigateToRoute(
        destination = projectDestination,
        args = mapOf("projectId" to projectId)
    )
    
}


// 아래에 더 많은 확장 함수를 추가할 수 있음
// 기존 주석 처리된 확장 함수들을 필요에 따라 활성화하고 업데이트

// 기존 주석 처리된 확장 함수들을 필요에 따라 활성화하고 업데이트
/*
package com.example.core_navigation.util

import com.example.core_navigation.core.NavigationManager
import com.example.core_navigation.destination.AcceptFriends
import com.example.core_navigation.destination.AddProject
import com.example.core_navigation.destination.AddSchedule
import com.example.core_navigation.destination.Calendar24Hour
import com.example.core_navigation.destination.ChangePassword
import com.example.core_navigation.destination.Chat
import com.example.core_navigation.destination.CreateCategory
import com.example.core_navigation.destination.CreateChannel
import com.example.core_navigation.destination.DevMenu
import com.example.core_navigation.destination.EditCategory
import com.example.core_navigation.destination.EditChannel
import com.example.core_navigation.destination.EditMember
import com.example.core_navigation.destination.EditProfile
import com.example.core_navigation.destination.EditRole
import com.example.core_navigation.destination.FindPassword
import com.example.core_navigation.destination.Friends
import com.example.core_navigation.destination.JoinProject
import com.example.core_navigation.destination.Login
import com.example.core_navigation.destination.Main
import com.example.core_navigation.destination.MemberList
import com.example.core_navigation.destination.ProjectSetting
import com.example.core_navigation.destination.RoleList
import com.example.core_navigation.destination.ScheduleDetail
import com.example.core_navigation.destination.Search
import com.example.core_navigation.destination.SetProjectName
import com.example.core_navigation.destination.SignUp
import com.example.core_navigation.destination.Splash
import com.example.core_navigation.destination.UserProfile

/**
 * 이전 네비게이션 호환성을 위한 확장 함수들
 * 
 * core_common.navigation에서 사용하던 함수들을 대체합니다.
 */

// 인증 관련 이동
fun NavigationManager.navigateToSplash() = navigateTo(Splash.route)
fun NavigationManager.navigateToLogin() = navigateTo(Login.route)
fun NavigationManager.navigateToSignUp() = navigateTo(SignUp.route)
fun NavigationManager.navigateToFindPassword() = navigateTo(FindPassword.route)
fun NavigationManager.navigateToMain() = navigateTo(Main.route)

// 프로필 관련 이동
fun NavigationManager.navigateToUserProfile(userId: String) = navigateTo(UserProfile.createRoute(userId))
fun NavigationManager.navigateToEditProfile() = navigateTo(EditProfile.route)
fun NavigationManager.navigateToChangePassword() = navigateTo(ChangePassword.route)

// 프로젝트 관련 이동
fun NavigationManager.navigateToAddProject() = navigateTo(AddProject.route)
fun NavigationManager.navigateToSetProjectName() = navigateTo(SetProjectName.route)
fun NavigationManager.navigateToJoinProject() = navigateTo(JoinProject.route)
fun NavigationManager.navigateToProjectSetting(projectId: String) = navigateTo(ProjectSetting.createRoute(projectId))

// 프로젝트 구조 관련 이동
fun NavigationManager.navigateToCreateCategory(projectId: String) = navigateTo(CreateCategory.createRoute(projectId))
fun NavigationManager.navigateToCreateChannel(projectId: String, categoryId: String) = 
    navigateTo(CreateChannel.createRoute(projectId, categoryId))
fun NavigationManager.navigateToEditCategory(projectId: String, categoryId: String) = 
    navigateTo(EditCategory.createRoute(projectId, categoryId))
fun NavigationManager.navigateToEditChannel(projectId: String, categoryId: String, channelId: String) = 
    navigateTo(EditChannel.createRoute(projectId, categoryId, channelId))

// 멤버 관련 이동
fun NavigationManager.navigateToMemberList(projectId: String) = navigateTo(MemberList.createRoute(projectId))
fun NavigationManager.navigateToEditMember(projectId: String, userId: String) = 
    navigateTo(EditMember.createRoute(projectId, userId))
fun NavigationManager.navigateToRoleList(projectId: String) = navigateTo(RoleList.createRoute(projectId))
fun NavigationManager.navigateToEditRole(projectId: String, roleId: String) = 
    navigateTo(EditRole.createEditRoute(projectId, roleId))
fun NavigationManager.navigateToAddRole(projectId: String) = navigateTo(EditRole.createAddRoute(projectId))

// 친구 관련 이동
fun NavigationManager.navigateToFriends() = navigateTo(Friends.route)
fun NavigationManager.navigateToAcceptFriends() = navigateTo(AcceptFriends.route)

// 채팅 관련 이동
fun NavigationManager.navigateToChat(channelId: String) = navigateTo(Chat.createRoute(channelId))
fun NavigationManager.navigateToChatWithMessage(channelId: String, messageId: String) = 
    navigateTo(Chat.createRouteWithMessage(channelId, messageId))

// 일정 관련 이동
fun NavigationManager.navigateToCalendar24Hour(year: Int, month: Int, day: Int) = 
    navigateTo(Calendar24Hour.createRoute(year, month, day))
fun NavigationManager.navigateToAddSchedule(year: Int, month: Int, day: Int) = 
    navigateTo(AddSchedule.createRoute(year, month, day))
fun NavigationManager.navigateToScheduleDetail(scheduleId: String) = 
    navigateTo(ScheduleDetail.createRoute(scheduleId))
fun NavigationManager.navigateToEditSchedule(scheduleId: String) =
    navigateTo(ScheduleDetail.createRoute(scheduleId))

// 검색 관련 이동
fun NavigationManager.navigateToSearch() = navigateTo(Search.route)

// 개발 메뉴
fun NavigationManager.navigateToDevMenu() = navigateTo(DevMenu.route)
*/ 