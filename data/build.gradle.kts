plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.data"
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
    kotlinOptions {
        //jvmTarget = libs.versions.jvmTarget.get()
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:core_common"))

    // Sentry 의존성 추가
    implementation(libs.sentry.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // OkHttp and Retrofit dependencies
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    
    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    
    // Mockito - Firebase 인증 및 콜백 테스트용
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
    
    // MockK 테스트 의존성 추가
    testImplementation(libs.mockk) // 예: libs.versions.toml에 mockk = "1.13.11" 추가 가정
    // testImplementation(libs.mockk.agent.jvm) // JVM 에이전트, 문제 발생 시 주석 처리 시도

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    // Task.await() 사용을 위한 의존성 추가
    implementation(libs.kotlinx.coroutines.play.services) // 버전은 libs.versions.toml 또는 직접 지정 (예: "1.7.3")

    // Also add the dependency for the Google Play services library and specify its version
    //implementation(libs.play.services.auth)
    //implementation(libs.play.services.base)

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    // Firebase BoM (Bill of Materials) - Firebase 라이브러리 버전 관리를 위한 BOM

    // 테스트 환경에서도 Timestamp 등을 사용하기 위해 추가
    testImplementation(libs.firebase.firestore.ktx)

    // 테스트 전용 의존성
    testImplementation(libs.kotlinx.coroutines.test) // 코루틴 테스트
    testImplementation(libs.androidx.arch.core.testing) // LiveData 테스트

    // Room Database 의존성 추가
    implementation(libs.androidx.room.runtime) // 또는 implementation "androidx.room:room-runtime:2.6.1"
    implementation(libs.androidx.room.ktx)      // 또는 implementation "androidx.room:room-ktx:2.6.1"
    ksp(libs.androidx.room.compiler)            // 또는 ksp "androidx.room:room-compiler:2.6.1"
    androidTestImplementation(libs.androidx.room.testing) // Room 테스트 의존성 추가
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}