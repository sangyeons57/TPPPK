plugins {
    alias(libs.plugins.android.library)
    
    alias(libs.plugins.ksp)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.feature_schedule"
    compileSdk = 35

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
        jvmTarget = libs.versions.jvmTarget.get()
    }
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":core:core_common")) // 공통 유틸리티 사용
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui")) // 공통 유틸리티 사용
    implementation(project(":core:core_logging")) // Sentry 유틸리티 사용
    implementation(project(":domain")) // Domain 모델, Repository 인터페이스 사용
    // ★ 중요: 현재 ChatViewModel이 Repository 구현체(ChatRepositoryImpl)가 제공하는
    //    ChatRepository 인터페이스를 직접 주입받으므로 :data 모듈 의존성이 필요합니다.
    //    (이상적으로는 ViewModel은 UseCase를 주입받고 UseCase가 Repository 인터페이스 사용)
    implementation(project(":data"))

    // --- 라이브러리 의존성 ---
    // Jetpack Compose UI
    implementation(platform(libs.androidx.compose.bom)) // BOM 버전은 프로젝트와 통일
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.core) // 아이콘 사용
    implementation(libs.material.icons.extended) // 아이콘 사용

    // ViewModel & Lifecycle for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation Compose (NavHostController 등을 직접 사용하진 않지만, 화면 구성에 필요할 수 있음)
    // implementation(libs.androidx.navigation.compose) // Commented out as we're using core_common's navigation now

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    implementation(libs.androidx.core.ktx) // 버전 통일
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose) // ViewModel 주입

    // Coil (Image Loading)
    implementation(libs.coil.compose) // 버전 통일

    // 기타 ChatScreen, ChatViewModel에서 사용하는 라이브러리 (예: Activity Result API)
    implementation(libs.androidx.activity.compose)

    // Sentry 직접 사용 (core_logging 의존성을 통해 간접적으로 사용되지만 직접 참조를 위해)
    implementation(libs.sentry.android)

    // 테스트 의존성
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
