#!/bin/bash

# =================================================================
# Android Gradle Docker 실행 스크립트 (Linux/Mac)
# =================================================================
#
# 목적: Docker 컨테이너 내에서 Android 프로젝트를 빌드하는 간편한 스크립트
#
# 주요 기능:
# - Docker 이미지 자동 빌드 및 관리
# - Gradle 명령어를 Docker 컨테이너에서 실행
# - 컬러 로그 출력으로 가독성 향상
# - Docker 환경 정리 및 관리 기능
# - 컨테이너 쉘 접속 기능
#
# 작동 방식:
# 1. Docker 및 Docker Compose 설치 확인
# 2. 필요시 Docker 이미지 자동 빌드
# 3. Docker Compose를 통해 컨테이너 실행
# 4. 호스트 프로젝트를 컨테이너에 볼륨 마운트
# 5. Gradle 캐시를 영구 볼륨으로 유지
#
# 사용법:
#   ./run-gradle.sh [OPTIONS] [GRADLE_COMMAND]
#
# 예시:
#   ./run-gradle.sh build                 # 전체 프로젝트 빌드
#   ./run-gradle.sh compileDebugKotlin    # Kotlin 컴파일만
#   ./run-gradle.sh test                  # 테스트 실행
#   ./run-gradle.sh --build               # Docker 이미지 빌드
#   ./run-gradle.sh --shell               # 컨테이너 쉘 접속
#
# =================================================================

# 스크립트 실행 시 오류가 발생하면 즉시 종료
set -e

# 스크립트 설정 변수들
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"  # 스크립트가 위치한 디렉토리
PROJECT_NAME="android-gradle-slim"                          # Docker 이미지 이름
COMPOSE_FILE="docker-compose.gradle.yml"                   # Docker Compose 설정 파일

# 터미널 컬러 코드 정의 (로그 가독성 향상)
RED='\033[0;31m'        # 에러 메시지용
GREEN='\033[0;32m'      # 성공 메시지용  
YELLOW='\033[1;33m'     # 경고 메시지용
BLUE='\033[0;34m'       # 정보 메시지용
NC='\033[0m'            # 컬러 리셋 (No Color)

# 로그 출력 함수들 (컬러 지원)
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Docker 환경 요구사항 확인 함수
# - Docker가 설치되어 있는지 확인
# - Docker Compose가 설치되어 있는지 확인  
# - Docker 데몬이 실행 중인지 확인
check_requirements() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker가 설치되어 있지 않습니다."
        log_error "Docker Desktop을 설치하거나 Docker 패키지를 설치해주세요."
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose가 설치되어 있지 않습니다."
        log_error "Docker Desktop에 포함되어 있거나 별도로 설치해주세요."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker 데몬이 실행되고 있지 않습니다."
        log_error "Docker Desktop을 시작하거나 Docker 서비스를 시작해주세요."
        exit 1
    fi
}

# 도움말 출력
show_help() {
    cat << EOF
Android Gradle Docker 실행 스크립트

사용법:
  $0 [OPTIONS] [GRADLE_COMMAND]

OPTIONS:
  -h, --help     이 도움말 표시
  -b, --build    Docker 이미지 빌드
  -c, --clean    Docker 이미지 및 컨테이너 정리
  -s, --shell    컨테이너 쉘 접속

GRADLE_COMMANDS:
  build                    전체 프로젝트 빌드
  compileDebugKotlin      디버그 Kotlin 컴파일
  assembleDebug           디버그 APK 생성
  test                    테스트 실행
  lintDebug              린트 검사
  clean                   빌드 정리
  dependencies            종속성 확인

예시:
  $0 build                # 전체 빌드
  $0 compileDebugKotlin   # Kotlin 컴파일
  $0 --build              # Docker 이미지 빌드
  $0 --shell              # 컨테이너 쉘 접속
EOF
}

# Docker 이미지 빌드 함수
# - Dockerfile.gradle.slim을 사용하여 이미지 빌드
# - Docker Compose를 통해 빌드 진행
build_image() {
    log_info "Docker 이미지를 빌드합니다..."
    log_info "이 과정은 최대 5-10분 소요될 수 있습니다..."
    docker-compose -f "$COMPOSE_FILE" build
    log_success "Docker 이미지 빌드가 완료되었습니다."
}

# Docker 환경 정리 함수
# - 생성된 컨테이너, 이미지, 볼륨 모두 제거
# - 완전한 초기화를 위한 기능
clean_docker() {
    log_info "Docker 이미지 및 컨테이너를 정리합니다..."
    log_warning "이 작업은 모든 캐시를 삭제하므로 다음 빌드에서 시간이 더 걸릴 수 있습니다."
    docker-compose -f "$COMPOSE_FILE" down --rmi all --volumes --remove-orphans
    log_success "정리가 완료되었습니다."
}

# 컨테이너 쉘 접속 함수
# - 디버깅이나 수동 작업을 위한 bash 쉘 제공
# - 컨테이너 내부 환경 탐색 가능
enter_shell() {
    log_info "컨테이너 쉘에 접속합니다..."
    log_info "종료하려면 'exit' 명령어를 입력하세요."
    docker-compose -f "$COMPOSE_FILE" run --rm android-gradle bash
}

# Gradle 명령 실행 함수 (핵심 기능)
# - Docker 컨테이너 내에서 Gradle 명령어 실행
# - 필요시 Docker 이미지 자동 빌드
# - 프로젝트 소스를 컨테이너에 볼륨 마운트
# - Gradle 캐시를 영구 볼륨으로 유지하여 빌드 속도 향상
run_gradle() {
    local gradle_cmd="$1"
    
    # 명령어가 없으면 도움말 표시
    if [ -z "$gradle_cmd" ]; then
        gradle_cmd="--help"
    fi
    
    log_info "Gradle 명령을 실행합니다: ./gradlew $gradle_cmd"
    
    # Docker 이미지 존재 여부 확인 및 자동 빌드
    if ! docker images | grep -q "$PROJECT_NAME"; then
        log_warning "Docker 이미지가 없습니다. 빌드를 시작합니다..."
        build_image
    fi
    
    # Docker Compose를 통해 Gradle 명령 실행
    # - 컨테이너 실행 후 자동 삭제 (--rm)
    # - 현재 프로젝트를 /workspace에 마운트
    # - Gradle 캐시를 영구 볼륨으로 유지
    # - --no-daemon: 컨테이너 환경에서 데몬 비활성화
    docker-compose -f "$COMPOSE_FILE" run --rm android-gradle ./gradlew "$gradle_cmd" --no-daemon
    
    # 실행 결과 확인 및 로그 출력
    if [ $? -eq 0 ]; then
        log_success "Gradle 명령이 성공적으로 완료되었습니다."
    else
        log_error "Gradle 명령 실행 중 오류가 발생했습니다."
        log_error "문제 해결을 위해 다음을 시도해보세요:"
        log_error "1. ./run-gradle.sh --clean (Docker 환경 초기화)"
        log_error "2. ./run-gradle.sh --shell (컨테이너 내부 확인)"
        exit 1
    fi
}

# 메인 함수 - 스크립트 진입점
# - 명령줄 인수 파싱 및 적절한 함수 호출
# - 사용자 요청에 따라 분기 처리
main() {
    # Docker 환경 요구사항 확인
    check_requirements
    
    # 명령줄 인수에 따른 분기 처리
    case "$1" in
        -h|--help)
            # 도움말 표시 후 종료
            show_help
            exit 0
            ;;
        -b|--build)
            # Docker 이미지 빌드만 수행
            build_image
            exit 0
            ;;
        -c|--clean)
            # Docker 환경 정리 (이미지, 컨테이너, 볼륨 삭제)
            clean_docker
            exit 0
            ;;
        -s|--shell)
            # 컨테이너 쉘 접속 (디버깅/수동 작업용)
            enter_shell
            exit 0
            ;;
        *)
            # 기본 동작: Gradle 명령어 실행
            # 모든 인수를 Gradle 명령어로 전달
            run_gradle "$*"
            ;;
    esac
}

# 스크립트 실행 시작점
# - 모든 명령줄 인수를 main 함수로 전달
main "$@"