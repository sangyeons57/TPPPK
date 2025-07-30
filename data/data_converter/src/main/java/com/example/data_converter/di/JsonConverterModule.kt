package com.example.data_converter.di

import com.example.data_converter.CategoryJsonConverter
import com.example.data_converter.JsonConverter
import com.example.data_converter.MessageJsonConverter
import com.example.data_converter.ProjectInvitationJsonConverter
import com.example.data_converter.ProjectJsonConverter
import com.example.data_converter.ScheduleJsonConverter
import com.example.data_converter.TaskJsonConverter
import com.example.data_converter.UserJsonConverter
import com.example.domain.model.base.Category
import com.example.domain.model.base.Message
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.Task
import com.example.domain.model.base.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * JsonConverter들을 DI 컨테이너에 제공하는 모듈
 * Clean Architecture 원칙에 따라 Data 계층에서만 JSON 변환을 담당
 */
@Module
@InstallIn(SingletonComponent::class)
object JsonConverterModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }

    @Provides
    @Singleton
    fun provideMessageJsonConverter(gson: Gson): JsonConverter<Message> {
        return MessageJsonConverter(gson)
    }

    @Provides
    @Singleton
    fun provideUserJsonConverter(gson: Gson): JsonConverter<User> {
        return UserJsonConverter(gson)
    }

    @Provides
    @Singleton
    fun provideProjectJsonConverter(gson: Gson): JsonConverter<Project> {
        return ProjectJsonConverter(gson)
    }

    @Provides
    @Singleton
    fun provideCategoryJsonConverter(gson: Gson): JsonConverter<Category> {
        return CategoryJsonConverter(gson)
    }

    @Provides
    @Singleton
    fun provideTaskJsonConverter(gson: Gson): JsonConverter<Task> {
        return TaskJsonConverter(gson)
    }

    @Provides
    @Singleton
    fun provideScheduleJsonConverter(gson: Gson): JsonConverter<Schedule> {
        return ScheduleJsonConverter(gson)
    }

    @Provides
    @Singleton
    fun provideProjectInvitationJsonConverter(gson: Gson): JsonConverter<ProjectInvitation> {
        return ProjectInvitationJsonConverter(gson)
    }

    /**
     * JsonConverter Map을 제공하여 OutBoxRepository에서 제네릭하게 사용
     * 새로운 JsonConverter 추가 시 이 맵에도 추가해야 함
     */
    @Provides
    @Singleton
    fun provideJsonConverterMap(
        messageJsonConverter: JsonConverter<Message>,
        userJsonConverter: JsonConverter<User>,
        projectJsonConverter: JsonConverter<Project>,
        categoryJsonConverter: JsonConverter<Category>,
        taskJsonConverter: JsonConverter<Task>,
        scheduleJsonConverter: JsonConverter<Schedule>,
        projectInvitationJsonConverter: JsonConverter<ProjectInvitation>
    ): Map<Class<*>, @JvmSuppressWildcards JsonConverter<*>> {
        return mapOf(
            Message::class.java to messageJsonConverter,
            User::class.java to userJsonConverter,
            Project::class.java to projectJsonConverter,
            Category::class.java to categoryJsonConverter,
            Task::class.java to taskJsonConverter,
            Schedule::class.java to scheduleJsonConverter,
            ProjectInvitation::class.java to projectInvitationJsonConverter
            // TODO: 나머지 9개 JsonConverter들이 구현되면 계속 추가
        )
    }
}