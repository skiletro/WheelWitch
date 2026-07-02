{ pkgs, ... }:

{
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk21;
  };

  android = {
    enable = true;
    platforms.version = [ "36.1" ];
    buildTools.version = [ "36.0.0" ];
    cmdLineTools.version = "11.0";
    android-studio = {
      enable = true;
      package = pkgs.android-studio;
    };
  };

  packages = with pkgs; [
    android-tools
  ];

  dotenv.enable = true;

  env.ORG_GRADLE_PROJECT_android_sdk_skipSdkInstall = "true";

  tasks = {
    "gradle:build:debug" = {
      description = "Assemble the debug APK";
      exec = "./gradlew assembleDebug";
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
          exec ./gradlew assembleRelease
        '';
    };
    "gradle:test" = {
      description = "Run unit tests (filter with: ./gradlew testDebugUnitTest --tests <name>)";
      exec = "./gradlew testDebugUnitTest";
    };
    "gradle:format" = {
      description = "Auto-format Kotlin with Spotless + ktfmt";
      exec = "./gradlew spotlessApply";
    };
    "gradle:lint" = {
      description = "Run Android Lint";
      exec = "./gradlew lint";
    };
    "gradle:clean" = {
      description = "Clean build outputs";
      exec = "./gradlew clean";
    };
    "gradle:check" = {
      description = "Build + run unit tests";
      after = [ "gradle:build:debug" ];
      exec = "./gradlew testDebugUnitTest";
    };
    "android:install" = {
      description = "Build the debug APK and install + launch on the connected adb device";
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
          echo "Building debug APK..."
          ./gradlew assembleDebug
          APK="app/build/outputs/apk/debug/app-debug.apk"
          [ -f "$APK" ] || { echo "APK not found at $APK" >&2; exit 1; }
          echo "Installing $APK on $(adb get-serialno)..."
          adb install -r "$APK"
          echo "Launching com.skiletro.wheelwitch/.MainActivity..."
          adb shell am start -n com.skiletro.wheelwitch/com.skiletro.wheelwitch.MainActivity
        '';
    };
  };

  enterShell = ''
    echo -e "\033[1;4mWheel Witch\033[0m"
    echo -e "  \033[2mjava:\033[0m         $(java -version 2>&1 | head -1)"
    echo -e "  \033[2mANDROID_HOME:\033[0m $ANDROID_HOME"
    echo
    echo -e "\033[1mTasks\033[0m"
    echo -e "  \033[36mdevenv tasks run gradle:build:debug\033[0m        \033[2m# debug APK\033[0m"
    echo -e "  \033[36mdevenv tasks run gradle:build:release\033[0m      \033[2m# signed release (after scripts/setup-signing.sh)\033[0m"
    echo -e "  \033[36mdevenv tasks run android:install\033[0m           \033[2m# build + install on device\033[0m"
  '';
}
