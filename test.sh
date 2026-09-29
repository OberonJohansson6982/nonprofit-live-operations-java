#!/usr/bin/env sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/nonprofit-dashboard-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java' -print)
java -ea -cp "$BUILD_DIR" org.example.nonprofit.OperationalSnapshotTest
