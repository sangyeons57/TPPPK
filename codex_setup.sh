#!/bin/bash

# Codex Setup Script for Android Kotlin project
# This script installs required dependencies and performs initial build steps.
# It is intended for Ubuntu-based environments. Modify as needed for others.

set -e

# Update package lists
sudo apt-get update

# Install JDK 17 and basic tools
sudo apt-get install -y openjdk-17-jdk wget unzip git curl

# Install Node.js 22 and npm
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs

# Install Firebase CLI and Sentry CLI
sudo npm install -g firebase-tools @sentry/cli

# Optional: install Docker (required for docker-based builds)
sudo apt-get install -y docker.io docker-compose

# Install Android command line tools and required SDK packages
ANDROID_SDK_ROOT="$HOME/android-sdk"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools" && cd "$ANDROID_SDK_ROOT/cmdline-tools"
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
rm commandlinetools-linux-11076708_latest.zip

export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

yes | sdkmanager --licenses
sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

cd "$OLDPWD"

# Ensure gradlew is executable
chmod +x ./gradlew

# Pre-download Gradle dependencies
./gradlew dependencies --no-daemon

# Install functions/ node dependencies
cd functions
npm install
cd ..

# Initial build and tests
./gradlew clean build --no-daemon
./gradlew test --no-daemon

