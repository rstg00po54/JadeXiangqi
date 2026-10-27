#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -x ./gradlew ]]; then
  ./gradlew assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle assembleDebug
elif [[ -x ../GoEngine-java/gradlew ]]; then
  ../GoEngine-java/gradlew -p "$PWD" assembleDebug
elif [[ -x ../GoEngine-java/GoEngine-java/gradlew ]]; then
  ../GoEngine-java/GoEngine-java/gradlew -p "$PWD" assembleDebug
else
  echo '没有检测到 Gradle Wrapper 或 gradle，请用 Android Studio 打开工程构建。' >&2
  exit 1
fi
