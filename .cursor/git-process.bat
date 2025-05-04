@echo off
setlocal

REM --- Argument Parsing and Variable Initialization ---
REM --title argument is removed
set "ARG_CONTENT_FILE="
set "BRANCH_TO_PUSH=" REM Default is empty (will attempt auto-detection)
set "REMOTE_NAME=origin" REM Default remote repository name
set "REMAINING_ARGS="

:ParseLoop
IF "%1"=="" GOTO EndParseLoop

REM --content option handling (Required)
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

REM --branch option handling (Optional)
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

REM --remote option handling (Optional)
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

REM Store any remaining unhandled arguments (for reference)
IF NOT "%REMAINING_ARGS%"=="" (
    set "REMAINING_ARGS=%REMAINING_ARGS% %~1"
) ELSE (
    set "REMAINING_ARGS=%~1"
)
SHIFT
GOTO ParseLoop

:EndParseLoop

REM --- Required Argument Validation ---
REM --title validation removed
IF "%ARG_CONTENT_FILE%"=="" (
    echo [ERROR] --content parameter is required.
    goto SyntaxError
)
IF NOT EXIST "%ARG_CONTENT_FILE%" (
    echo [ERROR] Content file not found: %ARG_CONTENT_FILE%
    goto EndScript
)

REM --- Script Variable Setup ---
REM Temporary file might still be needed (Git might prefer a file path)
set TEMP_COMMIT_MSG_FILE=%TEMP%\commit_msg_%RANDOM%.txt

echo [INFO] Starting Git workflow script...
echo [INFO] Commit Message File: %ARG_CONTENT_FILE%
echo [INFO] Remote Name: %REMOTE_NAME%
if defined BRANCH_TO_PUSH (
    echo [INFO] Target Branch: %BRANCH_TO_PUSH%
) else (
    echo [INFO] Target Branch: ^(Current branch will be detected^)
)
echo -------------------------------------
echo.

REM --- 1. Check Git Status (Informational) ---
echo [INFO] Checking current Git status...
git status
echo -------------------------------------
echo.

REM --- 2. Stage Changes ---
echo [INFO] Staging all changes ('git add .') ...
git add .
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Error occurred during 'git add .'^^! Aborting script
    goto EndScript
)
echo [SUCCESS] Changes staged successfully.
echo -------------------------------------
echo.

REM --- 3. Prepare Commit Message File (Copy to temporary file) ---
echo [INFO] Preparing commit message file... Copying content from %ARG_CONTENT_FILE%
copy "%ARG_CONTENT_FILE%" "%TEMP_COMMIT_MSG_FILE%" > nul

REM Check for file copy errors
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Error preparing commit message file^^! Aborting script
    goto ErrorCleanup
)
echo [SUCCESS] Commit message file prepared successfully.
echo -------------------------------------
echo.

REM --- 4. Execute Commit ---
echo [INFO] Executing 'git commit -F'...
git commit -F "%TEMP_COMMIT_MSG_FILE%"
set COMMIT_EXIT_CODE=%ERRORLEVEL%

REM Handle commit failure and cleanup
if %COMMIT_EXIT_CODE% neq 0 (
    echo [ERROR] Error occurred during 'git commit' ^(ERRORLEVEL = %%COMMIT_EXIT_CODE%%^)^^! Aborting script
    goto ErrorCleanup
)
echo [SUCCESS] Commit created successfully.
echo -------------------------------------
echo.

REM --- 5. Push to Remote Repository ---
echo [INFO] Executing 'git push'...

REM Determine the branch to push (try current branch if empty)
if "%BRANCH_TO_PUSH%"=="" (
    echo [INFO] Push branch name not specified via --branch. Detecting current branch...
    
    REM Try to detect the current branch name directly from Git config
    set "CURRENT_BRANCH=main"
    
    REM Check if we are in a git repository first
    git rev-parse --is-inside-work-tree >nul 2>&1
    if %ERRORLEVEL% neq 0 (
        echo [WARN] Not inside a Git repository, using default branch 'main'.
    ) else (
        REM Try multiple methods to get the branch name
        git symbolic-ref --short HEAD > "%TEMP%\branch.tmp" 2>nul
        if %ERRORLEVEL% equ 0 (
            set /p CURRENT_BRANCH=<"%TEMP%\branch.tmp"
            del "%TEMP%\branch.tmp" 2>nul
        ) else (
            REM Fallback to another method if symbolic-ref fails
            git branch --show-current > "%TEMP%\branch.tmp" 2>nul
            if %ERRORLEVEL% equ 0 (
                set /p CURRENT_BRANCH=<"%TEMP%\branch.tmp"
                del "%TEMP%\branch.tmp" 2>nul
            ) else (
                echo [WARN] Could not detect branch name, using default 'main'.
            )
        )
    )
    
    echo [INFO] Pushing to branch '%CURRENT_BRANCH%' on remote '%REMOTE_NAME%'.
    git push %REMOTE_NAME% %CURRENT_BRANCH%
    set PUSH_EXIT_CODE=%ERRORLEVEL%
) else (
    echo [INFO] Pushing to specified branch '%BRANCH_TO_PUSH%' on remote '%REMOTE_NAME%'.
    git push %REMOTE_NAME% %BRANCH_TO_PUSH%
    set PUSH_EXIT_CODE=%ERRORLEVEL%
)

REM Handle push result
if %PUSH_EXIT_CODE% neq 0 (
    echo [ERROR] Error occurred during 'git push' ^(ERRORLEVEL = %PUSH_EXIT_CODE%^)
    goto ErrorCleanup
)

echo [SUCCESS] Push to remote repository completed successfully.
echo -------------------------------------
echo.
echo [COMPLETE] Git workflow script finished successfully.
goto Cleanup

:SyntaxError
echo.
REM Usage instructions updated (no --title)
echo Usage: %~n0 --content "path\to\commit_message_file.txt" [--branch <branch_name>] [--remote <remote_name>]
echo   --content : (Required) Path to a text file containing the full commit message (title and body).
echo   --branch  : (Optional) The branch to push to. If omitted, attempts to use the current branch.
echo   --remote  : (Optional) The remote repository name to push to (default: origin).
goto EndScript

:ErrorCleanup
echo [WARN] Script completed with non-critical issues.
REM Clean up temporary files
:Cleanup
if exist "%TEMP_COMMIT_MSG_FILE%" (
    del "%TEMP_COMMIT_MSG_FILE%"
    echo [INFO] Temporary commit message file cleaned up.
)

:EndScript
endlocal
REM Pause before exiting to see the output (remove REM if needed)
REM pause