plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.feature_chat"
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":domain:domain"))
    implementation(project(":usecase"))
    implementation(project(":domain:domain_repository"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui")) // 공통 유틸리티 사용
    implementation(project(":core:websocket"))
    implementation(project(":data:data"))
    // WebSocket 기능

    // Module-specific dependencies
    // Coil (Image Loading)
    implementation(libs.androidx.compose.coil) // 버전 통일

    // WebSocket dependencies
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Paging dependencies
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 기타 ChatScreen, ChatViewModel에서 사용하는 라이브러리 (예: Activity Result API)
    implementation(libs.androidx.compose.activity)

    // 테스트 의존성
    testImplementation(libs.mockk) // MockK 의존성 추가
    testImplementation(libs.kotlinx.coroutines.test) // 코루틴 테스트 의존성 추가
}