#!/usr/bin/env bash
# Retry only a dependency-resolution rate limit, never a failed device test.
set -euo pipefail
mkdir -p build/device-ci
for attempt in 1 2 3; do
  log="build/device-ci/gradle-attempt-${attempt}.log"
  echo "Device test attempt ${attempt}/3"
  if ./gradlew :app:connectedDebugAndroidTest -PsideBySide --no-daemon --stacktrace 2>&1 | tee "$log"; then
    exit 0
  else
    status=$?
  fi
  if (( attempt == 3 )) ||
     ! grep -Eq 'Could not resolve (all files|org\.)|Could not get resource|Could not download' "$log" ||
     ! grep -Eq 'Received status code 429|HTTP (status )?429' "$log" ||
     grep -Eq 'Running tests on devices|[0-9]+ tests completed, [0-9]+ failed|There were failing tests|AssertionError' "$log"; then
    exit "$status"
  fi
  delay=$((60 * attempt))
  echo "Dependency repository rate limited the build; retrying in ${delay}s."
  sleep "$delay"
done
