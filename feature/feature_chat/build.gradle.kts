plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.feature_chat"
    compileSdk = 36

    defaultConfig {
        minSdk = 29

        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true // Compose 사용
    }
    kotlinOptions {
        //jvmTarget = libs.versions.jvmTarget.get()
    }
}
kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":core:core_common")) // 공통 유틸리티 사용
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui")) // 공통 유틸리티 사용
    implementation(project(":domain")) // Domain 모델, Repository 인터페이스 사용
    // ★ 중요: 현재 ChatViewModel이 Repository 구현체(ChatRepositoryImpl)가 제공하는
    //    ChatRepository 인터페이스를 직접 주입받으므로 :data 모듈 의존성이 필요합니다.
    //    (이상적으로는 ViewModel은 UseCase를 주입받고 UseCase가 Repository 인터페이스 사용)
    implementation(project(":data"))

    // --- 라이브러리 의존성 ---
    // Jetpack Compose UI
    implementation(platform(libs.androidx.compose.bom)) // BOM 버전은 프로젝트와 통일
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime) // 아이콘 사용

    // ViewModel & Lifecycle for Compose
    implementation(libs.androidx.compose.lifecycle.viewmodel)
    implementation(libs.androidx.compose.lifecycle.runtime)

    // Navigation Compose (Used by core_common now)
    // implementation(libs.androidx.compose.navigation)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    implementation(libs.androidx.core.ktx) // 버전 통일
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.compose.hilt.navigation) // ViewModel 주입

    // Coil (Image Loading)
    implementation(libs.androidx.compose.coil) // 버전 통일

    // WebSocket dependencies
    implementation(libs.okhttp.websocket)
    implementation(libs.kotlinx.serialization.json)

    // Paging dependencies
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 기타 ChatScreen, ChatViewModel에서 사용하는 라이브러리 (예: Activity Result API)
    implementation(libs.androidx.compose.activity)


    // 테스트 의존성
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
