# ====================================================================
# Dockerfile for TPPPK Android Project Build (with Kotlin 2.1.20+ compatibility)
# ====================================================================

# 1. 베이스 이미지 선택: OpenJDK 17을 포함한 경량 Debian 리눅스
FROM openjdk:17-jdk-slim-bullseye

# 2. 시스템 필수 도구 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# 3. Android SDK Command-line Tools 다운로드 및 설치
ARG SDK_TOOLS_VERSION=11076708
ARG SDK_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${SDK_TOOLS_VERSION}_latest.zip"
ENV ANDROID_HOME=/opt/android-sdk
# 참고: ENV 문법은 'KEY=VALUE' 형태이며, 주석은 별도 라인에 있어야 합니다.

# 이 부분이 특히 중요합니다. '=' 기호가 없어야 합니다.
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget ${SDK_TOOLS_URL} -O /tmp/commandlinetools.zip && \
    unzip -o /tmp/commandlinetools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/commandlinetools.zip

# 4. PATH 환경 변수 설정
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# 5. Android SDK 라이선스 동의 및 필수 구성 요소 설치
RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# 6. 프로젝트 파일 복사 및 Gradle Wrapper 설정
WORKDIR /app
COPY . /app
RUN chmod +x gradlew

# 7. 컨테이너 실행 시 기본 명령 설정
CMD ["sudo", "./gradlew", "build", "--stacktrace"]