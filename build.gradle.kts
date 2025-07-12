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
}

// Configure Java toolchain for all subprojects
allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }
}

// Ensure all projects use JDK 17 toolchain
subprojects {
    afterEvaluate {
        extensions.findByType<JavaPluginExtension>()?.apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
}