// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.sentry) apply false
    alias(libs.plugins.kotlin.compose) apply false
    
    // Toolchain resolver for automatic JDK provisioning
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Configure JDK 17 for all subprojects (centralized configuration)
subprojects {
    afterEvaluate {
        // Java toolchain configuration
        extensions.findByType<JavaPluginExtension>()?.apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        
        // Kotlin compile options (applies to all Kotlin-enabled modules)
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        
        // Android compile options (applies to Android modules)
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}