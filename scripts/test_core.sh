#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test
javac -encoding UTF-8 -d build/test app/src/main/java/com/badukai/xiangqi/XiangqiGame.java app/src/main/java/com/badukai/xiangqi/SimpleAi.java app/src/main/java/com/badukai/xiangqi/PuzzleBook.java tests/CoreTest.java tests/PuzzleTest.java
java -cp build/test CoreTest
java -cp build/test PuzzleTest
