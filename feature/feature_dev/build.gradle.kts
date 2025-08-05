plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.feature_dev"
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":domain:domain"))
    implementation(project(":usecase"))
    implementation(project(":domain:domain_repository"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui"))
    implementation(project(":core:websocket"))
    implementation(project(":data:data"))
    implementation(project(":data:data_repository"))
    // WebSocket 기능

    // Feature module dependencies
    implementation(project(":feature:feature_chat"))

    // Module-specific dependencies
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.activity)
}
