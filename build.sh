#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

if ! command -v java >/dev/null 2>&1; then
  echo '需要 JDK 17；请先安装并配置 JAVA_HOME。' >&2
  exit 1
fi

# The project ships its own Gradle 8.5 wrapper.
# First build may download the Gradle distribution to ~/.gradle/wrapper/dists.
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  for sdk in "$HOME/Android/Sdk" "$HOME/android-sdk" "/opt/android-sdk"; do
    if [[ -d "$sdk/platforms" ]]; then
      export ANDROID_HOME="$sdk"
      break
    fi
  done
fi

bash ./gradlew --no-daemon assembleDebug "$@"
echo "APK: $PWD/app/build/outputs/apk/debug/app-debug.apk"
