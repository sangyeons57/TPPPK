@echo off
REM =================================================================
REM Android Gradle Docker 실행 스크립트 (Windows)
REM =================================================================
REM
REM 목적: Windows 환경에서 Docker 컨테이너 내 Android 프로젝트 빌드
REM
REM 주요 기능:
REM - Docker 이미지 자동 빌드 및 관리
REM - Gradle 명령어를 Docker 컨테이너에서 실행  
REM - Windows 컬러 콘솔 지원으로 가독성 향상
REM - Docker 환경 정리 및 관리 기능
REM - 컨테이너 쉘 접속 기능
REM
REM 작동 방식:
REM 1. Docker 및 Docker Compose 설치 확인
REM 2. 필요시 Docker 이미지 자동 빌드
REM 3. Docker Compose를 통해 컨테이너 실행
REM 4. 호스트 프로젝트를 컨테이너에 볼륨 마운트
REM 5. Gradle 캐시를 영구 볼륨으로 유지
REM
REM 사용법:
REM   run-gradle.bat [OPTIONS] [GRADLE_COMMAND]
REM
REM 예시:
REM   run-gradle.bat build                 # 전체 프로젝트 빌드
REM   run-gradle.bat compileDebugKotlin    # Kotlin 컴파일만
REM   run-gradle.bat test                  # 테스트 실행
REM   run-gradle.bat --build               # Docker 이미지 빌드
REM   run-gradle.bat --shell               # 컨테이너 쉘 접속
REM
REM =================================================================

REM 배치 파일 변수 확장 지연 활성화
setlocal enabledelayedexpansion

REM 스크립트 설정 변수들
set "PROJECT_NAME=android-gradle-slim"      REM Docker 이미지 이름
set "COMPOSE_FILE=docker-compose.gradle.yml" REM Docker Compose 설정 파일

REM Windows 터미널 컬러 코드 정의 (Windows 10/11 지원)
set "COLOR_INFO=[94m"        REM 정보 메시지용 (파란색)
set "COLOR_SUCCESS=[92m"     REM 성공 메시지용 (초록색)
set "COLOR_WARNING=[93m"     REM 경고 메시지용 (노란색)
set "COLOR_ERROR=[91m"       REM 에러 메시지용 (빨간색)
set "COLOR_RESET=[0m"        REM 컬러 리셋

REM 로그 출력 함수들 (Windows 컬러 지원)
:log_info
echo %COLOR_INFO%[INFO]%COLOR_RESET% %~1
goto :eof

:log_success
echo %COLOR_SUCCESS%[SUCCESS]%COLOR_RESET% %~1
goto :eof

:log_warning
echo %COLOR_WARNING%[WARNING]%COLOR_RESET% %~1
goto :eof

:log_error
echo %COLOR_ERROR%[ERROR]%COLOR_RESET% %~1
goto :eof

REM Docker 환경 요구사항 확인 함수
REM - Docker가 설치되어 있는지 확인
REM - Docker Compose가 설치되어 있는지 확인
REM - Docker 데몬이 실행 중인지 확인
:check_requirements
docker --version >nul 2>&1
if errorlevel 1 (
    call :log_error "Docker가 설치되어 있지 않습니다."
    call :log_error "Docker Desktop을 설치하거나 Docker 패키지를 설치해주세요."
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    call :log_error "Docker Compose가 설치되어 있지 않습니다."
    call :log_error "Docker Desktop에 포함되어 있거나 별도로 설치해주세요."
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    call :log_error "Docker 데몬이 실행되고 있지 않습니다."
    call :log_error "Docker Desktop을 시작해주세요."
    exit /b 1
)
goto :eof

REM 도움말 출력
:show_help
echo Android Gradle Docker 실행 스크립트
echo.
echo 사용법:
echo   %~n0 [OPTIONS] [GRADLE_COMMAND]
echo.
echo OPTIONS:
echo   -h, --help     이 도움말 표시
echo   -b, --build    Docker 이미지 빌드
echo   -c, --clean    Docker 이미지 및 컨테이너 정리
echo   -s, --shell    컨테이너 쉘 접속
echo.
echo GRADLE_COMMANDS:
echo   build                    전체 프로젝트 빌드
echo   compileDebugKotlin      디버그 Kotlin 컴파일
echo   assembleDebug           디버그 APK 생성
echo   test                    테스트 실행
echo   lintDebug              린트 검사
echo   clean                   빌드 정리
echo   dependencies            종속성 확인
echo.
echo 예시:
echo   %~n0 build                # 전체 빌드
echo   %~n0 compileDebugKotlin   # Kotlin 컴파일
echo   %~n0 --build              # Docker 이미지 빌드
echo   %~n0 --shell              # 컨테이너 쉘 접속
goto :eof

REM Docker 이미지 빌드 함수
REM - Dockerfile.gradle.slim을 사용하여 이미지 빌드
REM - Docker Compose를 통해 빌드 진행
:build_image
call :log_info "Docker 이미지를 빌드합니다..."
call :log_info "이 과정은 최대 5-10분 소요될 수 있습니다..."
docker-compose -f "%COMPOSE_FILE%" build
if errorlevel 1 (
    call :log_error "Docker 이미지 빌드에 실패했습니다."
    exit /b 1
)
call :log_success "Docker 이미지 빌드가 완료되었습니다."
goto :eof

REM Docker 환경 정리 함수
REM - 생성된 컨테이너, 이미지, 볼륨 모두 제거
REM - 완전한 초기화를 위한 기능
:clean_docker
call :log_info "Docker 이미지 및 컨테이너를 정리합니다..."
call :log_warning "이 작업은 모든 캐시를 삭제하므로 다음 빌드에서 시간이 더 걸릴 수 있습니다."
docker-compose -f "%COMPOSE_FILE%" down --rmi all --volumes --remove-orphans
if errorlevel 1 (
    call :log_error "Docker 정리에 실패했습니다."
    exit /b 1
)
call :log_success "정리가 완료되었습니다."
goto :eof

REM 컨테이너 쉘 접속 함수
REM - 디버깅이나 수동 작업을 위한 bash 쉘 제공
REM - 컨테이너 내부 환경 탐색 가능
:enter_shell
call :log_info "컨테이너 쉘에 접속합니다..."
call :log_info "종료하려면 'exit' 명령어를 입력하세요."
docker-compose -f "%COMPOSE_FILE%" run --rm android-gradle bash
goto :eof

REM Gradle 명령 실행 함수 (핵심 기능)
REM - Docker 컨테이너 내에서 Gradle 명령어 실행
REM - 필요시 Docker 이미지 자동 빌드
REM - 프로젝트 소스를 컨테이너에 볼륨 마운트
REM - Gradle 캐시를 영구 볼륨으로 유지하여 빌드 속도 향상
:run_gradle
set "gradle_cmd=%*"
REM 명령어가 없으면 도움말 표시
if "%gradle_cmd%"=="" set "gradle_cmd=--help"

call :log_info "Gradle 명령을 실행합니다: ./gradlew %gradle_cmd%"

REM Docker 이미지 존재 여부 확인 및 자동 빌드
docker images | findstr "%PROJECT_NAME%" >nul
if errorlevel 1 (
    call :log_warning "Docker 이미지가 없습니다. 빌드를 시작합니다..."
    call :build_image
    if errorlevel 1 exit /b 1
)

REM Docker Compose를 통해 Gradle 명령 실행
REM - 컨테이너 실행 후 자동 삭제 (--rm)
REM - 현재 프로젝트를 /workspace에 마운트
REM - Gradle 캐시를 영구 볼륨으로 유지
REM - --no-daemon: 컨테이너 환경에서 데몬 비활성화
docker-compose -f "%COMPOSE_FILE%" run --rm android-gradle ./gradlew %gradle_cmd% --no-daemon
if errorlevel 1 (
    call :log_error "Gradle 명령 실행 중 오류가 발생했습니다."
    call :log_error "문제 해결을 위해 다음을 시도해보세요:"
    call :log_error "1. run-gradle.bat --clean (Docker 환경 초기화)"
    call :log_error "2. run-gradle.bat --shell (컨테이너 내부 확인)"
    exit /b 1
)
call :log_success "Gradle 명령이 성공적으로 완료되었습니다."
goto :eof

REM 메인 함수 - 스크립트 진입점
REM - 명령줄 인수 파싱 및 적절한 함수 호출
REM - 사용자 요청에 따라 분기 처리
:main
REM Docker 환경 요구사항 확인
call :check_requirements
if errorlevel 1 exit /b 1

REM 명령줄 인수에 따른 분기 처리
if "%1"=="-h" goto :show_help
if "%1"=="--help" goto :show_help
if "%1"=="-b" goto :build_image
if "%1"=="--build" goto :build_image
if "%1"=="-c" goto :clean_docker
if "%1"=="--clean" goto :clean_docker
if "%1"=="-s" goto :enter_shell
if "%1"=="--shell" goto :enter_shell

REM 기본 동작: Gradle 명령어 실행
REM 모든 인수를 Gradle 명령어로 전달
call :run_gradle %*
goto :eof

REM 스크립트 실행 시작점
REM - 모든 명령줄 인수를 main 함수로 전달
call :main %*