@echo off
setlocal

REM --- 인수 파싱 및 변수 초기화 ---
REM --title 인수는 제거됨
set "ARG_CONTENT_FILE="
set "BRANCH_TO_PUSH=" REM 기본값은 비워둠 (자동 감지 시도)
set "REMOTE_NAME=origin" REM 기본 원격 저장소 이름
set "REMAINING_ARGS="

:ParseLoop
IF "%1"=="" GOTO EndParseLoop

REM --content 옵션 처리 (필수)
IF /I "%1"=="--content" (
    IF "%2"=="" (
        echo [ERROR] --content option requires a file path.
        goto SyntaxError
    )
    set "ARG_CONTENT_FILE=%~2"
    SHIFT
    SHIFT
    GOTO ParseLoop
)

REM --branch 옵션 처리 (선택 사항)
IF /I "%1"=="--branch" (
    IF "%2"=="" (
        echo [ERROR] --branch option requires a branch name.
        goto SyntaxError
    )
    set "BRANCH_TO_PUSH=%~2"
    SHIFT
    SHIFT
    GOTO ParseLoop
)

REM --remote 옵션 처리 (선택 사항)
IF /I "%1"=="--remote" (
    IF "%2"=="" (
        echo [ERROR] --remote option requires a remote name.
        goto SyntaxError
    )
    set "REMOTE_NAME=%~2"
    SHIFT
    SHIFT
    GOTO ParseLoop
)

REM 처리되지 않은 나머지 인수 (참고용)
IF NOT "%REMAINING_ARGS%"=="" (
    set "REMAINING_ARGS=%REMAINING_ARGS% %~1"
) ELSE (
    set "REMAINING_ARGS=%~1"
)
SHIFT
GOTO ParseLoop

:EndParseLoop

REM --- 필수 인수 검증 ---
REM --title 검증 제거
IF "%ARG_CONTENT_FILE%"=="" (
    echo [ERROR] --content parameter is required.
    goto SyntaxError
)
IF NOT EXIST "%ARG_CONTENT_FILE%" (
    echo [ERROR] Content file not found: %ARG_CONTENT_FILE%
    goto EndScript
)

REM --- 스크립트 변수 설정 ---
REM 임시 파일은 여전히 필요할 수 있음 (Git이 파일 경로를 선호할 수 있으므로)
set TEMP_COMMIT_MSG_FILE=%TEMP%\commit_msg_%RANDOM%.txt

echo [INFO] Starting Git workflow script...
echo [INFO] Commit Message File: %ARG_CONTENT_FILE%
echo [INFO] Remote Name: %REMOTE_NAME%
if defined BRANCH_TO_PUSH (
    echo [INFO] Target Branch: %BRANCH_TO_PUSH%
) else (
    echo [INFO] Target Branch: (Current branch will be detected)
)
echo -------------------------------------
echo.

REM --- 1. Git 상태 확인 (참고용) ---
echo [INFO] Checking current Git status...
git status
echo -------------------------------------
echo.

REM --- 2. 변경 사항 스테이징 ---
echo [INFO] Staging all changes ('git add .') ...
git add .
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Error occurred during 'git add .'! Aborting script.
    goto EndScript
)
echo [SUCCESS] Changes staged successfully.
echo -------------------------------------
echo.

REM --- 3. 커밋 메시지 파일 준비 (임시 파일로 복사) ---
echo [INFO] Preparing commit message file... Copying content from %ARG_CONTENT_FILE%
copy "%ARG_CONTENT_FILE%" "%TEMP_COMMIT_MSG_FILE%" > nul

REM 파일 복사 오류 확인
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Error preparing commit message file! Aborting script.
    goto ErrorCleanup
)
echo [SUCCESS] Commit message file prepared successfully.
echo -------------------------------------
echo.

REM --- 4. 커밋 실행 ---
echo [INFO] Executing 'git commit -F'...
git commit -F "%TEMP_COMMIT_MSG_FILE%"
set COMMIT_EXIT_CODE=%ERRORLEVEL%

REM 커밋 실패 시 오류 메시지 및 정리
if %COMMIT_EXIT_CODE% neq 0 (
    echo [ERROR] Error occurred during 'git commit' (ERRORLEVEL = %COMMIT_EXIT_CODE%)! Aborting script.
    goto ErrorCleanup
)
echo [SUCCESS] Commit created successfully.
echo -------------------------------------
echo.

REM --- 5. 원격 저장소로 푸시 ---
echo [INFO] Executing 'git push'...

REM 푸시할 브랜치 결정 (비어 있으면 현재 브랜치 사용 시도)
if "%BRANCH_TO_PUSH%"=="" (
    echo [INFO] Push branch name not specified via --branch. Detecting current branch...
    REM 'git branch --show-current'는 최신 Git 버전에서 작동합니다. 구 버전에서는 다른 방법 필요.
    for /f "tokens=*" %%a in ('git branch --show-current 2^>nul') do set CURRENT_BRANCH=%%a
    if defined CURRENT_BRANCH (
        echo [INFO] Pushing to current branch '%CURRENT_BRANCH%' on remote '%REMOTE_NAME%'.
        git push %REMOTE_NAME% %CURRENT_BRANCH%
        set PUSH_EXIT_CODE=%ERRORLEVEL%
    ) else (
        echo [ERROR] Could not automatically detect current branch. Use the --branch option to specify it.
        set PUSH_EXIT_CODE=1
    )
) else (
    echo [INFO] Pushing to specified branch '%BRANCH_TO_PUSH%' on remote '%REMOTE_NAME%'.
    git push %REMOTE_NAME% %BRANCH_TO_PUSH%
    set PUSH_EXIT_CODE=%ERRORLEVEL%
)

REM 푸시 실패 시 오류 메시지
if %PUSH_EXIT_CODE% neq 0 (
    echo [ERROR] Error occurred during 'git push' (ERRORLEVEL = %PUSH_EXIT_CODE%)!
    goto ErrorCleanup
)
echo [SUCCESS] Push to remote repository completed successfully.
echo -------------------------------------
echo.

echo [COMPLETE] Git workflow script finished successfully!
goto Cleanup

:SyntaxError
echo.
REM 사용법 안내에서 --title 제거
echo Usage: %~n0 --content "path\to\commit_message_file.txt" [--branch <branch_name>] [--remote <remote_name>]
echo   --content : (Required) Path to a text file containing the full commit message (title and body).
echo   --branch  : (Optional) The branch to push to. If omitted, attempts to use the current branch.
echo   --remote  : (Optional) The remote repository name to push to (default: origin).
goto EndScript

:ErrorCleanup
echo [WARN] Script aborted due to an error.
REM 오류 발생 시에도 임시 파일 정리 시도
:Cleanup
if exist "%TEMP_COMMIT_MSG_FILE%" (
    del "%TEMP_COMMIT_MSG_FILE%"
    echo [INFO] Temporary commit message file cleaned up.
)

:EndScript
endlocal
REM 스크립트 종료 전 잠시 대기 (결과 확인용, 필요 없으면 REM 처리)
REM pause