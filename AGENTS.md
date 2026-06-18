# WheelWitch

An Android app that downloads/updates the Retro Rewind Mario Kart Wii Pack and launches Dolphin Emulator.

> Build instructions, commit conventions, and contribution guidelines live in [CONTRIBUTING.md](CONTRIBUTING.md). This file covers architecture, key decisions, constants, and testing — the things that don't change often.

## Build & Dev

See [CONTRIBUTING.md#build](CONTRIBUTING.md#build) for build commands and signing setup.

### Formatting (Spotless + ktfmt)

- `./gradlew spotlessApply` — auto-format all `.kt` and `.kts` files
- `./gradlew spotlessCheck` — verify formatting (for CI)
- No configuration to debate — ktfmt DEFAULT style is enforced

### Linting (Android Lint)

- `./gradlew lint` — runs Android Lint on the default variant
- `./gradlew check` — includes lint + unit tests
- Config in `app/lint.xml` (silences non-actionable checks)
- Keep lint clean before committing — no errors allowed

## Git & Commits

- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) — `<type>(<scope>): <description>`, lowercase imperative, no trailing period. See [CONTRIBUTING.md#commit-messages](CONTRIBUTING.md#commit-messages) for types and scopes.
- Stage only intended files with `git add <file>`; read `git status` before every commit; never use `git add -A` or `git commit -a` without verifying the diff.

## Architecture

- **Min SDK**: 31, **Target SDK**: 36, **Java 11**, **Compose + Material3** with dynamic color
- **No Google Play Services** — sideloaded APK only
- **Landscape-locked** fullscreen via `WindowCompat.getInsetsController`
- **Navigation**: flat overlay pattern via `AnimatedVisibility` — no NavHost
- **Gamepad focus isolation**: background screens removed from composition when overlays active
- **OkHttp 4.12.0** via `HttpClientProvider` singleton (15s / 60s read timeout variants)
- **SAF folder picker** for storage; direct `java.io.File` I/O with `DocumentFile` fallback
- **Dolphin launch**: `AutoStartFile` intent extra with kebab-case JSON; RR.json I/O in `DolphinLauncher`
- **i18n**: all strings in `res/values/strings.xml`; Compose uses `stringResource()`, VMs use `app.getString()`

## Package Structure

```
com.skiletro.wheelwitch
├── MainActivity.kt
├── model/               — data types (SemVersion, UpdateEntry, PackStatus, RoomStatus, etc.)
├── data/                — storage (PackStorage, SaveManager, RksysParser)
├── network/             — HTTP (VersionFileParser — version/delete files, leaderboard, rooms APIs)
├── domain/              — business logic (RewindPackManager, ChangelogParser)
├── util/                — utilities (DolphinLauncher, FileDownloader, HttpClientProvider, MiiFaceCache, MiiWadInstaller)
└── ui/
    ├── components/      — reusable composables (Constants, Format, StatusColors, ScreenHeader, FocusableSurface, MiiFace, ChangelogCard, Components, TopBar)
    ├── screens/         — screen composables
    └── theme/           — Color, Theme, StatusColors, Type
└── viewmodel/
    ├── PackUpdateViewModel   — install/update state machine + storage URI
    ├── SaveDataViewModel     — backup/restore, license parsing
    ├── MiiMakerViewModel     — WAD install (Mutex-guarded), launch, delete
    ├── OnlineViewModel       — rooms, leaderboard (Channel-based race-free), health, race stats
    ├── UiState               — sealed class for pack update flow
    └── SaveDataDelegate      — (in PackUpdateViewModel.kt) cross-VM notification interface
```

## Key Decisions

- **Screen nav**: `MainScreen` orchestrates `HomeScreen`/`SettingsScreen`/`OnboardingScreen`; `HomeScreen` orchestrates `OnlineMenuScreen`/`SaveInfoScreen`; `OnlineMenuScreen` uses `AnimatedContent` with `OnlineMenuPage` enum
- **PrimaryActionButton**: 56dp, filled primary, `titleMedium` semi-bold; **SecondaryActionButton**: 48dp, outlined
- **Leaderboard**: clickable rows with 5dp primary border on focus; one-shot `hasRequestedFocus` guard for pagination
- **File downloads**: `FileDownloader.downloadToFile()` with progress callback, HTTP validation, configurable client
- **Mii Maker WAD**: downloads zip from GameBanana, extracts `.wad`, launches via `ACTION_VIEW` + FileProvider
- **Update server**: `https://update.rwfc.net/`; version/deletion manifest files; fallback to full reinstall if < 3.2.6
- **Copy buffer**: 262144 bytes; **Parallel incremental downloads** via `async/await`

## Key Constants

| Thing | Value |
|-------|-------|
| Dolphin package | `org.dolphinemu.dolphinemu` |
| Dolphin activity | `org.dolphinemu.dolphinemu.ui.main.MainActivity` |
| Launch intent extra | `"AutoStartFile"` (path to RR.json) |
| Update server | `https://update.rwfc.net/` |
| Version file | `RetroRewind/RetroRewindVersion.txt` |
| Delete file | `RetroRewind/RetroRewindDelete.txt` |
| Full zip | `RetroRewind/zip/RetroRewind.zip` |
| Min reinstall version | `3.2.6` |
| Local version file | `<storage>/RetroRewind6/version.txt` |
| Save path (hardcoded `RMCP`) | `riivolution/save/RetroWFC/RMCP/rksys.dat` |

## Tests (~89 tests)

### Stack
JUnit 5, MockK 1.13.x, Truth 1.4.x, `org.json:json` test dep (Android stubs throw "not mocked")

### Running
```bash
./gradlew testDebugUnitTest                                         # all unit tests
./gradlew testDebugUnitTest --tests "com.skiletro.wheelwitch.model.SemVersionTest"  # single class
```

### Test files

| File | Tests | What it covers |
|---|---|---|
| `model/SemVersionTest.kt` | 25 | `parse()` valid/invalid, `compareTo()`, `toString()` |
| `model/RoomStatusTest.kt` | 7 | `parseRooms()` — full room, null race, edge cases |
| `util/DolphinLauncherTest.kt` | 9 | JSON generation, file I/O |
| `util/MiiFaceCacheTest.kt` | 5 | cache size, clear, init |
| `util/MiiWadInstallerTest.kt` | 8 | WAD validation, zip extraction, HACS variant |
| `data/RksysParserTest.kt` | 9 | RKPD binary parsing — slots, friend code golden |
| `network/VersionFileParserTest.kt` | 13 | update/deletion parsing, leaderboard, health, tracks, race stats |
| `domain/ChangelogParserTest.kt` | 7 | Jsoup table extraction, spoiler handling |
| `domain/RewindPackManagerTest.kt` | 6 | status checks, install/update flows |

### Testability notes
- Pure functions tested directly (no Android deps): SemVersion, parseRooms(), parseUpdatesText(), etc.
- `RksysParser` and `MiiFaceCache` use `java.util.Base64` for JVM testability
- Static `object` singletons mocked via `mockkObject()`; `PackStorage` is a class with constructor injection
- ViewModels require Android framework — their orchestrated logic tested at domain/util layer
