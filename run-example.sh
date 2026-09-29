#!/usr/bin/env sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/nonprofit-dashboard-example"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java -name '*.java' -print)
java -cp "$BUILD_DIR" org.example.nonprofit.NonprofitDashboardExample
