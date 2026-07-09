{ pkgs, ... }:

let
  srcFiles = [
    "app/src/**/*.kt"
    "app/src/**/*.java"
    "app/src/**/*.xml"
  ];
  buildFiles = srcFiles ++ [
    "*.gradle.kts"
    "settings.gradle.kts"
    "gradle.properties"
    "gradle/**/*.kts"
  ];
in
{
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk21;
  };

  dotenv.enable = true;

  android = {
    enable = true;
    platforms.version = [ "36.1" ];
    buildTools.version = [ "36.0.0" ];
    cmdLineTools.version = "11.0";
  };

  packages = with pkgs; [
    android-tools
  ];


  env.ORG_GRADLE_PROJECT_android_sdk_skipSdkInstall = "true";

  tasks = {
    "gradle:build:debug" = {
      description = "Assemble the debug APK";
      exec = ''
        ./gradlew assembleDebug
        echo '{"apkPath":"app/build/outputs/apk/debug/app-debug.apk"}' > "$DEVENV_TASK_OUTPUT_FILE"
      '';
      execIfModified = buildFiles;
    };
    "gradle:build:release" = {
      description = "Assemble the signed release APK (needs release.keystore + .env — run scripts/setup-signing.sh first)";
      exec =
        # sh
        ''
          if [ ! -f release.keystore ] || [ ! -f .env ]; then
            echo "No signing config found. Run: scripts/setup-signing.sh" >&2
            exit 1
          fi
          ./gradlew assembleRelease
          echo '{"apkPath":"app/build/outputs/apk/release/app-release.apk"}' > "$DEVENV_TASK_OUTPUT_FILE"
        '';
      execIfModified = buildFiles ++ [ "release.keystore" ".env" ];
    };
    "gradle:test" = {
      description = "Run unit tests (filter with: ./gradlew testDebugUnitTest --tests <name>)";
      exec = ''
        ./gradlew testDebugUnitTest
        echo '{"reportPath":"app/build/reports/tests/testDebugUnitTest/index.html"}' > "$DEVENV_TASK_OUTPUT_FILE"
      '';
      execIfModified = srcFiles ++ [ "*.gradle.kts" "settings.gradle.kts" ];
    };
    "gradle:format" = {
      description = "Auto-format Kotlin with Spotless + ktfmt";
      exec = "./gradlew spotlessApply";
      execIfModified = [ "app/src/**/*.kt" ];
    };
    "gradle:lint" = {
      description = "Run Android Lint";
      exec = ''
        ./gradlew lint
        echo '{"reportPath":"app/build/reports/lint-results-debug.html"}' > "$DEVENV_TASK_OUTPUT_FILE"
      '';
      execIfModified = buildFiles ++ [ "app/lint.xml" ];
    };
    "gradle:clean" = {
      description = "Clean build outputs";
      exec = "./gradlew clean";
    };
    "gradle:check" = {
      description = "Build + run unit tests";
      after = [ "gradle:build:debug" ];
      exec = ''
        ./gradlew testDebugUnitTest
        echo '{"reportPath":"app/build/reports/tests/testDebugUnitTest/index.html"}' > "$DEVENV_TASK_OUTPUT_FILE"
      '';
      execIfModified = srcFiles ++ [ "*.gradle.kts" "settings.gradle.kts" ];
    };
    "android:install" = {
      description = "Build the debug APK and install + launch on the connected adb device";
      after = [ "gradle:build:debug" ];
      exec =
        # sh
        ''
          set -euo pipefail
          if ! command -v adb >/dev/null 2>&1; then
            echo "adb not found - run this inside the devenv shell (devenv shell)" >&2
            exit 1
          fi
          if ! adb get-state >/dev/null 2>&1; then
            echo "No adb device connected. Check with: adb devices" >&2
            exit 1
          fi
          APK="app/build/outputs/apk/debug/app-debug.apk"
          [ -f "$APK" ] || { echo "APK not found at $APK — run gradle:build:debug first" >&2; exit 1; }
          DEVICE=$(adb get-serialno)
          echo "Installing $APK on $DEVICE..."
           adb install -r -d "$APK"
          echo "Launching com.skiletro.wheelwitch/.MainActivity..."
          adb shell am start -n com.skiletro.wheelwitch.debug/com.skiletro.wheelwitch.MainActivity
          echo "{\"device\":\"$DEVICE\",\"apkInstalled\":true}" > "$DEVENV_TASK_OUTPUT_FILE"
        '';
    };
  };

  enterShell = ''
    echo -e ""
    echo -e "\033[1mWheel Witch\033[0m"
    echo -e "  \033[2mjava:\033[0m         $(java -version 2>&1 | head -1)"
    echo -e "  \033[2mANDROID_HOME:\033[0m $ANDROID_HOME"
    echo
    echo -e "\033[1mTasks\033[0m"
    echo -e "  \033[36mdevenv tasks run gradle:build:debug\033[0m        \033[2m# debug APK\033[0m"
    echo -e "  \033[36mdevenv tasks run gradle:build:release\033[0m      \033[2m# signed release (after scripts/setup-signing.sh)\033[0m"
    echo -e "  \033[36mdevenv tasks run android:install\033[0m           \033[2m# build + install on device\033[0m"
  '';
}
