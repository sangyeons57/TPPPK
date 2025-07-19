plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms)

    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)

    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.teamnovapersonalprojectprojectingkotlin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.teamnovapersonalprojectprojectingkotlin"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // sourceCompatibility and targetCompatibility now set globally in root build.gradle.kts
        isCoreLibraryDesugaringEnabled = true
    }
    // kotlinOptions.jvmTarget now set globally in root build.gradle.kts
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {


    // Firebase App Check
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    // Task.await() 사용을 위한 의존성 추가
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(project(":app-api")) // 버전은 libs.versions.toml 또는 직접 지정 (예: "1.7.3")

    // app_api 모듈 추가 - app에서 구현을 제공할 API를 정의

    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(project(":core:core_ui"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_fcm"))


    // ★ 네비게이션 그래프에서 직접 호출하는 모든 Feature 모듈 의존성 추가
    implementation(project(":feature:feature_main"))
    implementation(project(":feature:feature_splash"))
    implementation(project(":feature:feature_project_detail"))
    implementation(project(":feature:feature_project_setting"))
    implementation(project(":feature:feature_friends"))
    implementation(project(":feature:feature_settings"))
    implementation(project(":feature:feature_chat"))
    implementation(project(":feature:feature_tasks"))
    implementation(project(":feature:feature_add_schedule"))
    implementation(project(":feature:feature_search"))
    implementation(project(":feature:feature_profile"))
    implementation(project(":feature:feature_dev")) // DevMenuScreen 호출 시
    implementation(project(":feature:feature_find_password"))
    
    // 새로 추가된 feature 모듈들
    implementation(project(":feature:feature_login"))
    implementation(project(":feature:feature_signup"))
    implementation(project(":feature:feature_home"))
    implementation(project(":feature:feature_calendar"))
    implementation(project(":feature:feature_calendar_24hour"))
    implementation(project(":feature:feature_add_project"))
    implementation(project(":feature:feature_join_project"))
    implementation(project(":feature:feature_member_list"))
    implementation(project(":feature:feature_edit_category"))
    implementation(project(":feature:feature_edit_channel"))
    implementation(project(":feature:feature_edit_member"))
    implementation(project(":feature:feature_edit_profile"))
    implementation(project(":feature:feature_edit_role"))
    implementation(project(":feature:feature_edit_schedule"))
    implementation(project(":feature:feature_role_list"))
    implementation(project(":feature:feature_schedule_detail"))
    implementation(project(":feature:feature_privacy_policy"))
    implementation(project(":feature:feature_terms_of_service"))
    implementation(project(":feature:feature_change_password"))
    implementation(project(":feature:feature_accept_friend"))
    implementation(project(":feature:feature_add_role"))

    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    // Hilt Core
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.hilt.navigation)

    implementation(libs.coil) // View 기반 UI
    implementation(libs.androidx.compose.coil) // Jetpack Compose용

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.navigation) // Navigation Compose
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.runtime)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ViewModel
    implementation(libs.androidx.compose.lifecycle.viewmodel)
    implementation(libs.androidx.compose.lifecycle.runtime)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Coroutines for Google Play Services
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Play Services
    implementation(libs.gms.play.services.base)

    implementation(libs.androidx.compose.navigation) // 예시 버전, 최신 버전 확인하세요

    implementation(libs.androidx.compose.material.icons.extended)


    // Retrofit (HTTP 클라이언트)
    implementation(libs.retrofit)

    // OkHttp (HTTP 및 WebSocket 클라이언트)
    implementation(libs.okhttp)

    // Gson (JSON 직렬화/역직렬화)
    implementation(libs.gson)

    // Retrofit과 Gson 통합을 위한 Converter
    implementation(libs.converter.gson)



    //Room 추가
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)


    
    // LeakCanary for memory leak detection (debug only)
    debugImplementation(libs.leakcanary.android)
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

