#!/bin/bash

echo "==========Install openjdk-17==========="
apt-get update
apt-get install -y openjdk-17-jdk

echo "==========SET JAVA_HOME openjdk-17==========="
update-java-alternatives --list
update-java-alternatives --set java-1.17.0-openjdk-amd64
echo $JAVA_HOME
java -version

echo "========== Persist enviroment variables in ~/.bashrc ==========="
grep -qxF 'export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64' ~/.bashrc || \
  echo 'export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64' >> ~/.bashrc
grep -qxF 'export PATH=$JAVA_HOME/bin:$PATH' ~/.bashrc || \
  echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
grep -qxF 'export ANDROID_SDK_ROOT=/usr/lib/android-sdk' ~/.bashrc || \
  echo 'export ANDROID_SDK_ROOT=/usr/lib/android-sdk' >> ~/.bashrc
grep -qxF 'export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH' ~/.bashrc || \
  echo 'export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH' >> ~/.bashrc

source ~/.bashrc

echo "==========Install Android SDK==========="
# Download latest tools
wget -q -O android-sdk-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip

# Unpack to standard SDK root
mkdir -p /usr/lib/android-sdk/cmdline-tools
unzip -q android-sdk-tools.zip -d /usr/lib/android-sdk/cmdline-tools

# Restructure into 'latest' folder
sudo mv /usr/lib/android-sdk/cmdline-tools/cmdline-tools /usr/lib/android-sdk/cmdline-tools/latest
rm android-sdk-tools.zip

echo "========== Update SDK Manager & Install Platform 35 ==========="
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --update
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platforms;android-35" "build-tools;35.0.0"

echo "========== Initialize Git Submodules ==========="
git submodule update --init --recursive

echo "========== Setup Complete =========="
java -version
sdkmanager --versionecho "==========Git Submodule Init=========="
git submodule update --init --recursive
