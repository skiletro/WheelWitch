# Contributing to Wheel Witch

## Prerequisites

- **JDK 17+** (required by Gradle 9.4.1)
- Android SDK (managed automatically via Gradle)
- (Optional) Android Studio for the emulator and layout previews

### Nix Devshell

If you have [Nix](https://nixos.org) and [direnv](https://direnv.net) installed, the `.envrc` will automatically load a development shell containing JDK 21 and the Android SDK. Otherwise, enter it manually with `nix develop`

### Justfile

A [`just`](https://github.com/casey/just) command runner is provided for common tasks:

```bash
just build        # assemble debug APK
just test         # run unit tests
just clean        # clean build outputs
just check        # build + test
```

## Contributing

1. Fork this repository
2. Create a feature branch (`git checkout -b fix-the-thing`)
3. Commit your changes and open a pull request

### Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(<scope>): <description>
```

Types: `feat:` (new user-facing), `fix:` (bug fix), `refactor:` (neither), `perf:`, `test:`, `docs:`, `chore:`, `build:`.

Scopes match the package layout. For example: `dolphin`, `pack`, `save`, `mii-maker`, `online`, `leaderboard`, `rooms`, `race-stats`, `onboarding`, `home`, `settings`, `quick-launch`, `theme`, `gamepad`, `ui`, `storage`, `i18n`, `viewmodel`.

Description is lowercase imperative, no trailing period.

### Build gates

`./gradlew assembleDebug testDebugUnitTest` must stay green. Pull requests run tests automatically in CI.

No formal CLA; if you contribute code, please add yourself to a credits section if we add one.

## Build

```bash
./gradlew assembleDebug                  # build APK
./gradlew assembleRelease                # release build (R8/ProGuard)
./gradlew testDebugUnitTest              # run unit tests
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

For a **signed release APK**, set these environment variables and run `./gradlew assembleRelease`:

```bash
export KEYSTORE_PATH=./release.keystore
export KEYSTORE_PASSWORD=your-store-pass
export KEY_ALIAS=wheelwitch
# KEY_PASSWORD is unified with KEYSTORE_PASSWORD (set it to the same value, or omit it)
```

The keystore is resolved relative to the project root. Defaults: PKCS12, RSA-4096,
SHA512withRSA, 10000-day validity. Run `scripts/setup-signing.sh` to generate one
interactively.

## Project structure

```
com.skiletro.wheelwitch
├── model/         (data types: SemVersion, PackStatus, SaveFileInfo, etc.)
├── data/          (storage: DolphinPaths, DolphinTree, DolphinConfig, SaveManager, RksysParser, GameTypeParser)
├── network/       (HTTP + JSON parsers: VersionFileParser, RoomStatusParser, etc.)
├── domain/        (business logic: RewindPackManager)
├── util/{io,net,mii,launcher,log,json,prefs}/  (utilities grouped by concern)
├── ui/{components,screens,theme}/
└── viewmodel/     (Android ViewModels per screen)
```

Strings go in `res/values/strings.xml`; Compose screens use `stringResource()`.
