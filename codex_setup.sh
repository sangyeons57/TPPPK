#!/bin/bash
set -euo pipefail

echo "==== Install JDK 17 & tools ===="
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates ca-certificates-java wget unzip openjdk-17-jdk

# known issue: ensure java keystore dir exists, then finalize dpkg config
mkdir -p /etc/ssl/certs/java || true
dpkg --configure -a || true

echo "==== Configure Java ===="
# Use path-based alternative to avoid name differences
update-java-alternatives --list || true
update-java-alternatives --set /usr/lib/jvm/java-17-openjdk-amd64 || true

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_SDK_ROOT=/usr/lib/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

# persist (append only if missing)
grep -qxF 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' ~/.bashrc || \
  echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
grep -qxF 'export PATH=$JAVA_HOME/bin:$PATH' ~/.bashrc || \
  echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
grep -qxF 'export ANDROID_SDK_ROOT=/usr/lib/android-sdk' ~/.bashrc || \
  echo 'export ANDROID_SDK_ROOT=/usr/lib/android-sdk' >> ~/.bashrc
grep -qxF 'export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH' ~/.bashrc || \
  echo 'export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH' >> ~/.bashrc

echo "==== Install Android cmdline-tools ===="
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
# NOTE: this versioned URL is brittle; prefer parameterized or keep it updated.
wget -q -O /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools"
mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
rm /tmp/cmdline-tools.zip

echo "==== Accept licenses & install SDKs ===="
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --update
# add platform-tools (빈번히 필요)
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "platforms;android-35" "build-tools;35.0.0" || \
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platforms;android-34" "build-tools;34.0.0"

echo "==== Git submodules ===="
git submodule update --init --recursive

echo "==== Versions ===="
java -version
sdkmanager --version