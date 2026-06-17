# Build debug APK
build:
    ./gradlew assembleDebug

# Build signed release APK (set KEYSTORE_* env vars)
build-release:
    ./gradlew assembleRelease

# Run all unit tests, or filter by test class, e.g. `just test com.skiletro.wheelwitch.model.SemVersionTest`
test CLASSES="":
    ./gradlew testDebugUnitTest {{if CLASSES == "" { "" } else { "--tests \"" + CLASSES + "\"" }}}

# Clean build outputs
clean:
    ./gradlew clean

# Build + test
check: build test
