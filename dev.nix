{ pkgs ? import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/nixos-unstable.tar.gz") {} }:

let
  # Android SDK 구성
  # compileSdkVersion 35 (프리뷰 가능성)에 맞추어 설정
  android-sdk-components = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "35" ];
    buildToolsVersions = [ "35.0.0" ]; # 여전히 이 버전이 문제일 수 있음

    # cmdlineToolsVersion = "latest"; # 필요시
    # emulatorVersion = "latest";
    # systemImageVersions = [ "android-35;google_apis;x86_64" ];

    # extraSdkPackages = [ ];

    # includeNdk = true;
    # ndkVersion = "25.2.9519653";

    includeEmulator = false;
    includeSystemImages = false;
  };
in
pkgs.mkShell {
  # Nix 셸 환경에 포함될 패키지들
  nativeBuildInputs = [
    # 프로젝트 빌드 및 실행에 직접적으로 사용되는 도구들
    pkgs.git # 버전 관리 시스템
  ];

  buildInputs = [
    # Java Development Kit (JDK) - Java 17 사용
    pkgs.jdk17

    # Gradle - 버전 8.11.1
    # Nixpkgs에 'gradle_8_11_1'과 같은 정확한 버전의 패키지가 없을 수 있습니다.
    # 이 경우 'pkgs.gradle.override { version = "8.11.1"; }' 와 같은 방법을 사용하거나,
    # 'pkgs.gradle_8_11' (마이너 버전까지 일치) 또는 'pkgs.gradle' (최신 안정)을 사용 후 확인 필요.
    # 여기서는 일반적인 최신 gradle을 사용하도록 하고, 필요시 사용자가 버전을 고정할 수 있도록 주석 처리합니다.
    # (pkgs.gradle.override { version = "8.11.1"; } ) # 정확한 버전 명시가 더 안전합니다.
    # 또는, 해당 버전이 있다면 pkgs.gradle_8_11_1
    pkgs.gradle_8 # Nixpkgs에서 제공하는 최신 Gradle 8.x 버전을 사용합니다.
                  # 실제 빌드는 ./gradlew 가 gradle-wrapper.properties 에 명시된 8.11.1 버전을 사용합니다.

    # Android SDK (위에서 구성한 components 사용)
    android-sdk-components.androidsdk

    # 기타 개발/배포에 필요한 CLI 도구들
    pkgs.firebase-tools # Firebase CLI (최신 안정 버전)
  ];

  # 셸이 활성화될 때 실행될 스크립트
  shellHook = ''
    # Java 환경 변수 설정
    export JAVA_HOME="${pkgs.jdk17.home}" # .home 사용이 더 일반적일 수 있음

    # Android SDK 환경 변수 설정
    # android-sdk-components.androidsdk.home으로 SDK 경로를 가져올 수 있습니다.
    export ANDROID_SDK_ROOT="${android-sdk-components.androidsdk}/libexec/android-sdk"
    export ANDROID_HOME="$ANDROID_SDK_ROOT" # 일부 오래된 도구 호환성

    # gradlew 스크립트에 실행 권한 부여
    if [ -f ./gradlew ]; then
      chmod +x ./gradlew
    fi

    echo ""
    echo "################################################################"
    echo "#                                                              #"
    echo "#      Nix-shell for TeamnovaPersonalProjectProjectingKotlin      #"
    echo "#                                                              #"
    echo "################################################################"
    echo ""
    echo "Java version: $($JAVA_HOME/bin/java -version 2>&1 | grep version)"
    echo "Gradle version: $(gradle --version | grep Gradle || echo 'Gradle not found or version check failed')" # Gradle 버전 확인 개선
    echo "Android SDK Root: $ANDROID_SDK_ROOT"
    echo "Android SDK Platforms: $(ls $ANDROID_SDK_ROOT/platforms)" # 설치된 플랫폼 확인
    echo "Android SDK Build-Tools: $(ls $ANDROID_SDK_ROOT/build-tools)" # 설치된 빌드툴 확인
    echo ""
    echo "Available commands: ./gradlew, firebase, etc."
    echo ""
  '';
} 