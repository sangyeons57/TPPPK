@echo off
REM 채널 시스템 마이그레이션 배치 스크립트
REM 새로운 채널 구조로 기존 DM과 프로젝트 채널 데이터를 마이그레이션합니다.

echo 채널 시스템 마이그레이션 시작
echo 경고: 이 스크립트는 데이터베이스에 직접 영향을 미칩니다. 
echo 실행 전 반드시 데이터베이스 백업을 수행해주세요.
echo.

set /p CONFIRM=진행하려면 'Y'를 입력하세요: 

if /i not "%CONFIRM%"=="Y" (
  echo 마이그레이션이 취소되었습니다.
  exit /b 1
)

echo.
echo 마이그레이션 준비 중...

REM 앱 빌드가 완료되었는지 확인
echo 앱 빌드 상태 확인...
gradlew.bat :app:assembleDebug
if %ERRORLEVEL% neq 0 (
  echo 앱 빌드 실패! 마이그레이션을 중단합니다.
  exit /b 1
)

REM adb 명령으로 앱 실행 및 마이그레이션 트리거
echo 마이그레이션 실행 중...

REM Firebase Auth 로그인 상태 확인
adb shell am start -n com.example.teamnova.personal.projecting/com.example.feature_settings.ui.MigrationToolActivity
if %ERRORLEVEL% neq 0 (
  echo 마이그레이션 도구 실행 실패! 앱이 설치되었는지 확인하세요.
  exit /b 1
)

echo 마이그레이션 도구가 실행되었습니다.
echo 앱 화면에서 마이그레이션 작업을 진행해주세요.
echo.
echo 참고사항:
echo 1. DM 마이그레이션을 먼저 실행하세요.
echo 2. 각 프로젝트마다 프로젝트 ID를 입력하여 마이그레이션하세요.
echo 3. 마이그레이션이 완료된 후 결과를 기록해주세요.

echo.
echo 마이그레이션 스크립트 종료 