plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Jetty Core
    implementation("org.eclipse.jetty:jetty-server:12.0.23")
    
    // Jetty EE10 (Jakarta EE 10) WebSocket Support
    implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.0.23")
    implementation("org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jakarta-server:12.0.23")
    
    // Jetty Annotations - @WebServlet 애노테이션 스캔을 위해 필요
    implementation("org.eclipse.jetty:jetty-annotations:12.0.23")
    
    // Jakarta WebSocket API
    implementation("jakarta.websocket:jakarta.websocket-api:2.2.0")
    
    // Jakarta Servlet API
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // Firebase Admin SDK
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