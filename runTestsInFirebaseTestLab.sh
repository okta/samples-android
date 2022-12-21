#!/usr/bin/env bash

set -e # Fail on error.

gcloud firebase test android run --no-auto-google-login --type instrumentation --app browser-sign-in/build/outputs/apk/debug/browser-sign-in-debug.apk --test browser-sign-in/build/outputs/apk/androidTest/debug/browser-sign-in-debug-androidTest.apk --device model=oriole,version=32,locale=en_US,orientation=portrait --timeout 5m --no-performance-metrics --use-orchestrator --environment-variables clearPackageData=true & PID_BROWSER_SIGN_IN=$!
gcloud firebase test android run --no-auto-google-login --type instrumentation --app totp/build/outputs/apk/debug/totp-debug.apk --test totp/build/outputs/apk/androidTest/debug/totp-debug-androidTest.apk --device model=oriole,version=32,locale=en_US,orientation=portrait --timeout 5m --no-performance-metrics --use-orchestrator --environment-variables clearPackageData=true & PID_TOTP=$!

wait $PID_BROWSER_SIGN_IN
wait $PID_TOTP
