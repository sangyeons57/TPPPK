plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.example.feature_main"
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":domain"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui"))
    implementation(project(":data:data_core"))

    // Feature module dependencies
    implementation(project(":feature:feature_calendar"))
    implementation(project(":feature:feature_home"))
    implementation(project(":feature:feature_profile"))

    // Module-specific dependencies
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.activity)
}