# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ANR 방지 및 메모리 최적화를 위한 규칙
-keep class com.example.teamnovapersonalprojectprojectingkotlin.MyApp { *; }
-keep class com.example.websocket.** { *; }
-keep class com.example.domain.** { *; }

# Firebase 관련 클래스 보존
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# WebSocket 관련 클래스 보존
-keep class okhttp3.** { *; }
-keep class kotlinx.coroutines.** { *; }

# 메모리 최적화를 위한 불필요한 로그 제거 (Release 빌드에서)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}