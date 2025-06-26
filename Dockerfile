# 최적화된 Android 빌드 Dockerfile
FROM openjdk:17-jdk-slim

# 환경 변수 설정
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# 필요한 패키지 설치 (최소한으로)
RUN apt-get update && \
    apt-get install -y wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Android SDK 설치 (캐시 최적화)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip -q commandlinetools-linux-*_latest.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm commandlinetools-linux-*_latest.zip

# SDK 구성 요소 설치 (필수만)
RUN yes | sdkmanager --licenses >/dev/null 2>&1 && \
    sdkmanager \
        "platform-tools" \
        "platforms;android-36" \
        "build-tools;35.0.0" \
        >/dev/null 2>&1

# 작업 디렉토리 설정
WORKDIR /workspace

# Gradle wrapper만 먼저 복사하여 종속성 캐싱
COPY gradlew gradle.properties ./
COPY gradle/ gradle/

# Gradle wrapper 실행 권한 부여
RUN chmod +x ./gradlew

# 종속성 미리 다운로드 (캐시 최적화)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
RUN echo "sdk.dir=${ANDROID_HOME}" > local.properties
RUN ./gradlew dependencies --no-daemon >/dev/null 2>&1 || true

# 소스 코드 복사
COPY . .

# local.properties 재생성 (Docker 내부용)
RUN echo "sdk.dir=${ANDROID_HOME}" > local.properties

# 기본 명령어
CMD ["./gradlew", "compileDebugKotlin"]