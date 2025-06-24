@echo off
echo ========================================
echo Quick Android Docker Test
echo ========================================

echo Building slim Docker image...
docker build -f Dockerfile.android.slim -t android-kotlin-slim .

if %ERRORLEVEL% EQU 0 (
    echo ✅ Slim Docker image built!
    echo.
    echo Testing domain compilation...
    docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon
) else (
    echo ❌ Docker build failed!
)

pause