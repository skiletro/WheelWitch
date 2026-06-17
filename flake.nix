{
  description = "Wheel Witch development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }: {
    devShells = nixpkgs.lib.genAttrs [ "x86_64-linux" "aarch64-darwin" ] (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "36" ];
          buildToolsVersions = [ "36.0.0" ];
          includeEmulator = false;
          includeSystemImages = false;
        };

        sdkPath = "${androidComposition.androidsdk}/libexec/android-sdk";
      in
      {
        default = pkgs.mkShell {
          packages = with pkgs; [
            jdk21
            androidComposition.androidsdk
          ];

          shellHook = ''
            export HOME_SDK="$HOME/.cache/wheelwitch/android-sdk"

            if [ ! -d "$HOME_SDK" ]; then
              echo "Setting up writable Android SDK at $HOME_SDK ..."
              mkdir -p "$HOME_SDK"
              cp -rL "${sdkPath}/." "$HOME_SDK/"
              chmod -R u+w "$HOME_SDK"
            fi

            export ANDROID_HOME="$HOME_SDK"
            export ANDROID_SDK_ROOT="$HOME_SDK"
            export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${sdkPath}/build-tools/36.0.0/aapt2"

            just --list
          '';
        };
      }
    );
  };
}
