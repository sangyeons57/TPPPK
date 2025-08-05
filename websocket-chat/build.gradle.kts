plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))   // Jetty 12 최소 JDK
}

repositories {
    mavenCentral()
}

dependencies {
    // Jetty core + EE10 modules (WebSocket + Servlet)
    implementation("org.eclipse.jetty:jetty-server:12.0.23")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.0.23")
    implementation("org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jakarta-server:12.0.23")

    // Jetty Annotations - @ServerEndpoint 애노테이션 스캔을 위해 필요
    implementation("org.eclipse.jetty.ee10:jetty-ee10-annotations:12.0.23")

    // Compile-only Jakarta APIs (avoid runtime duplicates)
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")   // EE10 스펙
    compileOnly("jakarta.websocket:jakarta.websocket-api:2.1.1")

    // Firebase Admin SDK (현재 사용 가능한 버전)
    implementation("com.google.firebase:firebase-admin:9.5.0")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

tasks.test { 
    useJUnitPlatform() 
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest {
        attributes["Main-Class"] = "com.example.websocket.ChatWebSocketServer"
    }
}

tasks.jar { 
    enabled = false 
}

tasks.build { 
    dependsOn(tasks.shadowJar) 
}