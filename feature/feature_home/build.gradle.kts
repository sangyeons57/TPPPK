plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)      
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.feature_home"
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":domain:domain"))
    implementation(project(":usecase"))
    implementation(project(":domain:domain_repository"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui"))
    implementation(project(":data:data"))

    // --- Feature 모듈 의존성 ---
    implementation(project(":feature:feature_edit_category"))
    implementation(project(":feature:feature_edit_channel"))

    // Module-specific dependencies
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.activity)
}