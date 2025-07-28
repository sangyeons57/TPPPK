plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.core_ui"
}
dependencies {

    // Core modules
    implementation(project(":domain"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))

    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.activity)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.graphics)

    // Coil for image loading
    implementation(libs.androidx.compose.coil)


    
    // Compose 애니메이션 라이브러리 (animateItemPlacement 등을 위한 라이브러리)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation)


}