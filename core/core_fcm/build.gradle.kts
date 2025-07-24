plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.core_fcm"
}

dependencies {
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui"))
    
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.messaging.ktx)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    

    // Module-specific Compose
    implementation(libs.androidx.compose.material3)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
}

