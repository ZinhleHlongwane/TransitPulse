#!/usr/bin/env bash
#
# Single-command runner for TransitPulse.
#
# Usage:
#   ./run.sh          # runs the final, full system (Iteration 4)
#   ./run.sh 1        # runs Iteration 1 only
#   ./run.sh 2        # runs Iteration 2 only
#   ./run.sh 3        # runs Iteration 3 only
#   ./run.sh 4        # runs Iteration 4 only (same as no argument)
#
# Uses Maven if it's on your PATH; otherwise falls back to plain javac/java
# automatically, so this works with or without Maven installed.

set -e

ITERATION="${1:-4}"

case "$ITERATION" in
  1) DIR="iteration-1-route-name-service"; MAIN="transitpulse.Iteration1Demo" ;;
  2) DIR="iteration-2-status-and-timetable-services"; MAIN="transitpulse.Iteration2Demo" ;;
  3) DIR="iteration-3-error-handling-and-panic-alerts"; MAIN="transitpulse.Iteration3Demo" ;;
  4) DIR="iteration-4-public-alerts"; MAIN="transitpulse.Iteration4Demo" ;;
  *)
    echo "Usage: ./run.sh [1|2|3|4]" >&2
    exit 1
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/$DIR"

echo "== Running $DIR ($MAIN) =="
echo

if command -v mvn >/dev/null 2>&1; then
  echo "(using Maven)"
  mvn -q compile exec:java -Dexec.mainClass="$MAIN"
else
  echo "(Maven not found on PATH — falling back to javac/java)"
  find src -name "*.java" > .sources.txt
  javac -d out @.sources.txt
  java -cp out "$MAIN"
  rm -f .sources.txt
fi
