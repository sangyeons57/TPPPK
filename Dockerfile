# syntax=docker/dockerfile:1
FROM openjdk:17-slim

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_HOME=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

RUN apt-get update && apt-get install -y wget unzip ca-certificates && \
    mkdir -p /opt/android-sdk/cmdline-tools && \
    wget -q -O /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools && \
    mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip && \
    yes | sdkmanager --licenses && \
    yes | sdkmanager --update && \
    yes | sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

WORKDIR /workspace
CMD ["bash", "-lc", "./gradlew --help"]