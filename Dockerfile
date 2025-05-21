# ====================================================================
# 1. Docker Engine 설치 (Jules 서버에 최초 1회만 실행)
# ====================================================================

echo "--- 1. Docker Engine 설치 시작 ---"

# 패키지 목록 업데이트 및 Docker 설치에 필요한 패키지 설치
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg

# Docker 공식 GPG 키 추가
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Docker APT 저장소 설정
echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker Engine 설치
sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# ====================================================================
# 2. 현재 사용자를 'docker' 그룹에 추가 (Jules 서버에 최초 1회만 실행)
# ====================================================================

echo "--- 2. Docker 권한 설정 시작 ---"

# 현재 사용자를 'docker' 그룹에 추가합니다.
# 이 변경사항은 현재 실행 중인 쉘에는 즉시 적용되지 않으므로,
# 이 스크립트 내의 'docker' 명령어는 'sudo'를 계속 사용합니다.
# 하지만 다음번 로그인부터는 'sudo' 없이 'docker' 명령을 사용할 수 있게 됩니다.
sudo usermod -aG docker ${USER}

echo ""
echo "!!! 참고: Docker 권한 변경사항이 적용되었습니다. !!!"
echo "다음번 로그인부터는 'sudo docker ...' 대신 'docker ...'로 명령어를 실행할 수 있습니다."
echo "현재 스크립트에서는 안정성을 위해 'sudo'를 사용하여 'docker' 명령어를 실행합니다."
echo ""

# ====================================================================
# 3. TPPPK 프로젝트 소스 코드 업데이트 및 Docker 빌드/실행 (매번 코드 변경 시 반복 실행)
# ====================================================================

echo "--- 3. TPPPK 프로젝트 빌드 및 실행 시작 ---"

# TPPPK 프로젝트 디렉토리로 이동 (GitHub 클론이 완료된 상태 가정)
# 만약 /app에 아직 프로젝트가 없다면, 'git clone https://github.com/sangyeons57/TPPPK /app' 먼저 실행
cd /app

# GitHub에서 최신 소스 코드 가져오기 (Dockerfile 포함)
git pull

# Docker 이미지 빌드
# 'sudo'를 사용하여 권한 문제를 방지합니다.
echo "--- Docker 이미지 빌드 중 ---"
sudo docker build -t tpppk-android-builder .

# Docker 컨테이너 실행
# 'sudo'를 사용하여 권한 문제를 방지합니다.
# Dockerfile의 CMD에 설정된 './gradlew build --stacktrace' 명령이 실행됩니다.
echo "--- Docker 컨테이너 실행 중 (Gradle 빌드 시작) ---"
sudo docker run tpppk-android-builder

echo "--- TPPPK 프로젝트 빌드 및 실행 완료 ---"```

---

### **Dockerfile (확인 및 수정용)**

이전에 `ENV ANDROID_HOME=/opt/android-sdk # 여기는 ENV 문법으로 '=' 이 필요합니다.` 라고 주석을 같은 라인에 붙여서 에러가 났던 부분을 수정했습니다.

```dockerfile
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
CMD ["./gradlew", "build", "--stacktrace"]