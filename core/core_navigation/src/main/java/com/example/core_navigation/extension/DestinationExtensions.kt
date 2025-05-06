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