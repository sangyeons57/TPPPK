plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.core_navigation"
    compileSdk = 36

    defaultConfig {
        minSdk = 29

        consumerProguardFiles("consumer-rules.pro")
        
        // 테스트 관련 설정
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        //jvmTarget = libs.versions.jvmTarget.get()
    }
    buildFeatures {
        compose = true
    }
    
    // 테스트 영역 설정
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}
dependencies {
    // Project modules
    implementation(project(":core:core_common"))
    implementation(project(":domain"))

    // Android core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom)) // BOM 버전은 프로젝트와 통일
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core) // 아이콘 사용
    implementation(libs.androidx.compose.material.icons.extended) // 아이콘 사용

    // Navigation
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.hilt.navigation)

    // Lifecycle
    implementation(libs.androidx.compose.lifecycle.viewmodel)
    implementation(libs.androidx.compose.lifecycle.runtime)
    
    // DI
    implementation(libs.androidx.core.ktx) // 버전 통일
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coil (Image Loading)
    implementation(libs.androidx.compose.coil) // 버전 통일

    
    // 코루틴
    implementation(libs.kotlinx.coroutines.android)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.espresso.core)
    
    // 코루틴 테스트
    testImplementation(libs.kotlinx.coroutines.test)

    // Mockito - Firebase 인증 및 콜백 테스트용
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
    
    // MockK 테스트 의존성 추가
    testImplementation(libs.mockk) // 예: libs.versions.toml에 mockk = "1.13.11" 추가 가정

    implementation(libs.kotlinx.serialization.json)
}