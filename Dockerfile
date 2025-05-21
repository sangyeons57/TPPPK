# ====================================================================
# Dockerfile for TPPPK Android Project Build (with Kotlin 2.1.20+ compatibility)
# ====================================================================

# 1. 베이스 이미지 선택: OpenJDK 17을 포함한 경량 Debian 리눅스
#    이 이미지는 Gradle과 Java 기반 Android 빌드에 필요한 Java 런타임과 컴파일러를 제공합니다.
#    'bullseye'는 안정적인 Debian 11 기반을 의미합니다.
FROM openjdk:17-jdk-slim-bullseye

# 2. 시스템 필수 도구 설치
#    apt-get update: 패키지 목록을 최신화합니다.
#    apt-get install: 빌드에 필요한 유틸리티(wget, unzip, git)를 설치합니다.
#    --no-install-recommends: 불필요한 추천 패키지 설치를 방지하여 이미지 크기를 최소화합니다.
#    rm -rf /var/lib/apt/lists/*: APT 캐시를 삭제하여 최종 이미지 크기를 줄입니다.
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# 3. Android SDK Command-line Tools 설치
#    ARG: Docker 빌드 시에만 사용되는 빌드 인수(예: SDK 툴 버전).
#    ENV: 컨테이너 실행 시에도 사용되는 환경 변수(예: ANDROID_HOME 경로).
ARG SDK_TOOLS_VERSION=11076708
ARG SDK_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${SDK_TOOLS_VERSION}_latest.zip"
ENV ANDROID_HOME = /opt/android-sdk

#    SDK 설치 경로 생성 및 툴 다운로드, 압축 해제, 구조 변경.
#    unzip -o: 기존 파일 덮어쓰기.
#    mv: 압축 해제된 'cmdline-tools' 폴더를 표준 구조인 'latest'로 변경.
#    rm: 임시 다운로드 파일 삭제.
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget ${SDK_TOOLS_URL} -O /tmp/commandlinetools.zip && \
    unzip -o /tmp/commandlinetools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/commandlinetools.zip

# 4. PATH 환경 변수 설정
#    SDK 도구(sdkmanager, adb 등)를 컨테이너 내 어디서든 실행할 수 있도록 PATH에 추가합니다.
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# 5. Android SDK 라이선스 동의 및 필수 구성 요소 설치
#    yes | sdkmanager --licenses: 모든 Android SDK 라이선스 계약에 자동으로 동의합니다.
#    sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools":
#       - 'platforms;android-35': 프로젝트의 `compileSdk` 버전에 맞춰 Android API 레벨 35 플랫폼 설치.
#       - 'build-tools;35.0.0': 프로젝트의 `buildToolsVersion`에 맞춰 Build-Tools 35.0.0 설치.
#       - 'platform-tools': ADB(Android Debug Bridge) 등 핵심 플랫폼 도구 설치.
RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# 6. 프로젝트 파일 복사 및 Gradle Wrapper 설정
#    WORKDIR: 컨테이너 내에서 명령어가 실행될 기본 작업 디렉토리 설정.
#    COPY . /app: 로컬 PC의 현재 디렉토리(TPPPK 프로젝트 루트)의 모든 파일을 컨테이너의 /app으로 복사.
#    chmod +x gradlew: Gradle Wrapper 스크립트에 실행 권한을 부여합니다.
WORKDIR /app
COPY . /app
RUN chmod +x gradlew

# 7. 컨테이너 실행 시 기본 명령 설정
#    CMD: 컨테이너가 시작될 때 자동으로 실행될 명령입니다.
#    ./gradlew build --stacktrace: Android 프로젝트를 빌드하고 상세한 스택 트레이스를 출력합니다.
CMD ["./gradlew", "build", "--stacktrace"]

# Optional: 컨테이너 포트 노출 (애플리케이션이 특정 포트에서 실행될 경우)
# EXPOSE 8080