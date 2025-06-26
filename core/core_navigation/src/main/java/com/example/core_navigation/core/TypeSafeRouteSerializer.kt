package com.example.core_navigation.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TypeSafeRouteSerializer : JsonContentPolymorphicSerializer<TypeSafeRoute>(TypeSafeRoute::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TypeSafeRoute> {
        val typeName = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (typeName) {
            SplashRoute::class.simpleName -> SplashRoute.serializer()
            LoginRoute::class.simpleName -> LoginRoute.serializer()
            SignUpRoute::class.simpleName -> SignUpRoute.serializer()
            FindPasswordRoute::class.simpleName -> FindPasswordRoute.serializer()
            TermsOfServiceRoute::class.simpleName -> TermsOfServiceRoute.serializer()
            PrivacyPolicyRoute::class.simpleName -> PrivacyPolicyRoute.serializer()
            MainContainerRoute::class.simpleName -> MainContainerRoute.serializer()
            HomeRoute::class.simpleName -> HomeRoute.serializer()
            CalendarRoute::class.simpleName -> CalendarRoute.serializer()
            ProfileRoute::class.simpleName -> ProfileRoute.serializer()
            AddProjectRoute::class.simpleName -> AddProjectRoute.serializer()
            JoinProjectRoute::class.simpleName -> JoinProjectRoute.serializer()
            SetProjectNameRoute::class.simpleName -> SetProjectNameRoute.serializer()
            SelectProjectTypeRoute::class.simpleName -> SelectProjectTypeRoute.serializer()
            ProjectDetailRoute::class.simpleName -> ProjectDetailRoute.serializer()
            ProjectSettingsRoute::class.simpleName -> ProjectSettingsRoute.serializer()
            CreateCategoryRoute::class.simpleName -> CreateCategoryRoute.serializer()
            EditCategoryRoute::class.simpleName -> EditCategoryRoute.serializer()
            CreateChannelRoute::class.simpleName -> CreateChannelRoute.serializer()
            EditChannelRoute::class.simpleName -> EditChannelRoute.serializer()
            MemberListRoute::class.simpleName -> MemberListRoute.serializer()
            EditMemberRoute::class.simpleName -> EditMemberRoute.serializer()
            RoleListRoute::class.simpleName -> RoleListRoute.serializer()
            EditRoleRoute::class.simpleName -> EditRoleRoute.serializer()
            AddRoleRoute::class.simpleName -> AddRoleRoute.serializer()
            ChatRoute::class.simpleName -> ChatRoute.serializer()
            Calendar24HourRoute::class.simpleName -> Calendar24HourRoute.serializer()
            AddScheduleRoute::class.simpleName -> AddScheduleRoute.serializer()
            ScheduleDetailRoute::class.simpleName -> ScheduleDetailRoute.serializer()
            EditScheduleRoute::class.simpleName -> EditScheduleRoute.serializer()
            UserProfileRoute::class.simpleName -> UserProfileRoute.serializer()
            EditMyProfileRoute::class.simpleName -> EditMyProfileRoute.serializer()
            AppSettingsRoute::class.simpleName -> AppSettingsRoute.serializer()
            ChangePasswordRoute::class.simpleName -> ChangePasswordRoute.serializer()
            FriendsListRoute::class.simpleName -> FriendsListRoute.serializer()
            AcceptFriendsRoute::class.simpleName -> AcceptFriendsRoute.serializer()
            GlobalSearchRoute::class.simpleName -> GlobalSearchRoute.serializer()
            MessageDetailRoute::class.simpleName -> MessageDetailRoute.serializer()
            DevMenuRoute::class.simpleName -> DevMenuRoute.serializer()
            FCMTestRoute::class.simpleName -> FCMTestRoute.serializer()
            else -> throw IllegalArgumentException("Unknown TypeSafeRoute type: $typeName")
        }
    }
} 