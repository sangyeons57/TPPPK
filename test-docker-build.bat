@echo off
echo ========================================
echo Android Kotlin Docker Build Test
echo ========================================

echo Step 1: Building Docker image...
docker build -f Dockerfile.android -t android-kotlin-app .

if %ERRORLEVEL% EQU 0 (
    echo ✅ Docker image built successfully!
    echo.
    echo Step 2: Running container to test build...
    docker run --rm -v %cd%:/workspace android-kotlin-app ./gradlew clean compileDebugKotlin
    
    if %ERRORLEVEL% EQU 0 (
        echo ✅ Domain compilation successful!
    ) else (
        echo ❌ Domain compilation failed!
    )
) else (
    echo ❌ Docker image build failed!
)

echo.
echo Step 3: Checking Docker images...
docker images | findstr android-kotlin-app

pause