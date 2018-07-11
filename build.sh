#!/bin/bash

echo -ne "\n\ncompiling\n"
./gradlew clean build
if [[ $? -ne 0 ]]; then
    echo "compile failed"
    exit
fi

# sign and align release
#jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/keystore/indefatigable.keystore build/outputs/apk/familyphotoframe-release-unsigned.apk indefatigable
#$ANDROID_HOME/build-tools/20.0.0/zipalign -v 4 build/outputs/apk/familyphotoframe-release-unsigned.apk build/outputs/apk/familyphotoframe-release.apk

echo -ne "\n\ntransferring to device\n"
adb install -r build/outputs/apk/familyphotoframe-debug.apk
if [[ $? -ne 0 ]]; then
    echo -ne "transfer failed\n"
    exit
fi

echo -ne "transfer successful, opening app\n"
adb shell am start -n app.familyphotoframe/.MainActivity

# echo -ne "\n\nclearing log\n"
# adb logcat -c

# echo -ne "\n\nreading log\n"
# adb logcat *:E MainActivity:I
