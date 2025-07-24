plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.core_common"
}

dependencies {

    // Navigation
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.hilt.navigation)
    
    // Additional Compose UI
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material.icons.core)
    

    // Firebase - 최소 필요한 의존성만 추가
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.storage.ktx)
    
    // WebSocket and Networking
    implementation(libs.okhttp.websocket)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Image Loading
    implementation(libs.androidx.compose.coil)
}