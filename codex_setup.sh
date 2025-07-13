#!/bin/bash

echo "==========Install openjdk-17==========="
apt-get update
apt-get install -y openjdk-17-jdk

echo "==========SET JAVA_HOME openjdk-17==========="
update-java-alternatives --list
update-java-alternatives --set java-1.17.0-openjdk-amd64
echo $JAVA_HOME
java -version

export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "==========Install Android SDK==========="
wget -O android-commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip


echo "==========Unpacking Android Studio=========="
unzip android-commandlinetools.zip -d /usr/lib/android-sdk

echo "==========Updating sdkmanager=========="
/usr/lib/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/usr/lib/android-sdk/ --update

echo "==========Installing Android SDK=========="
bash -c 'yes | /usr/lib/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/usr/lib/android-sdk/ "platforms;android-35"'

echo "==========Git Submodule Init=========="
git submodule update --init --recursive
