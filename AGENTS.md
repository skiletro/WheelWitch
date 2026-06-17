# WheelWitch

An Android app that downloads/updates the Retro Rewind Mario Kart Wii Pack and launches Dolphin Emulator.

## Build & Dev

```bash
./gradlew assembleDebug     # build APK
./gradlew assembleDebug --build-cache  # cached build
```

No test framework — test sources deleted.

## Git

- Commit after each logical feature change; use descriptive messages matching the existing conversational style (e.g. "add room status thingie").
- Stage only the intended files with `git add <file>` and commit with `git commit -m "<message>"`.
- Read `git status` before every commit; never use `git add -A` or `git commit -a` without first verifying the diff.

## Architecture

- **Min SDK**: 31 (Android 12)
- **UI**: Compose + Material3 with dynamic color (`dynamicColor = true` in `Theme.kt`)
- **Theme XML**: `Theme.Material3.DayNight.NoActionBar` (in `res/values/themes.xml`)
- **Screen orientation**: Landscape-locked (`android:screenOrientation="landscape"` in manifest)
- **No edge-to-edge**: `enableEdgeToEdge()` was removed — system insets handled naturally by `NoActionBar` theme
- **No Google Play Services** — sideloaded APK only

## Key Decisions & Conventions

- **SAF folder picker** for pack storage location; direct `java.io.File` path resolution for I/O with `DocumentFile` fallback for metadata
- **OkHttp 4.12.0** for HTTP (connection pooling, progress tracking)
- **Dolphin launch**: uses `AutoStartFile` intent extra with kebab-case JSON fields; RR.json read/written via `DolphinLauncher` methods (`readIsoPathFromLaunchJson`/`writeLaunchJson`/`deleteLaunchJson`)
- **File downloads**: consolidated in `FileDownloader.downloadToFile()` with optional progress callback and HTTP status validation (used by both `RewindPackManager` and `MiiWadInstaller`)
- **Mii Maker (WAD install)**: downloads `https://filecache45.gamebanana.com/mods/mii_channel_symbols_-_hacs.zip` to app cache, extracts the `.wad` file, and launches Dolphin via `ACTION_VIEW` intent with FileProvider content URI
- **Update server**: `https://update.rwfc.net/` (same as WheelWizard); version file manifest, deletion list, fallback to full reinstall if version < 3.2.6
- **Pipelined extraction** removed (`LinkedBlockingQueue` pipe caused hang at 99%)
- **Copy buffer**: 262144 bytes
- **Parallel incremental downloads** via `async/await`
- **Save backup/restore** fully user-directed via file pickers (no automatic `.backups` folder)

## Package Structure

```
com.skiletro.wheelwitch
├── MainActivity.kt
├── model/               — Shared data types
│   ├── SemVersion.kt
│   ├── UpdateEntry.kt
│   ├── DeletionEntry.kt
│   ├── ServerInfo.kt
│   ├── PackStatus.kt
│   ├── ProgressInfo.kt
│   └── RoomStatus.kt    — Online rooms API + ServerConnectivity
├── data/                — Storage and persistence
│   ├── PackStorage.kt   — SAF + direct file I/O
│   ├── SaveManager.kt   — Save file management
│   └── RksysParser.kt   — RKPD save data parser
├── network/             — HTTP layer
│   └── VersionFileParser.kt — Version + delete file fetching, leaderboard API, rooms API
├── domain/              — Business logic
│   └── RewindPackManager.kt — Install/update orchestration
├── util/                — Utilities
│   ├── DolphinLauncher.kt — Launch JSON generation, intent launch, ISO prefs, RR.json I/O
│   └── FileDownloader.kt  — Shared download-to-file with optional progress
├── ui/                  — Presentation layer
│   ├── screens/
│   │   └── HomeScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── viewmodel/
    └── UpdateViewModel.kt
```

## ViewModel (`UpdateViewModel.kt`)

- **State machine**: `NoStorage → Checking → Downloading/Extracting/ApplyingUpdate → Ready/Error`
- `saveState` (StateFlow of `SaveState(hasSave)`) independently tracks save presence
- `successMessage` (StateFlow of `String?`) drives a brief auto-dismiss success banner (3s)
- `backupSave(Uri)` / `restoreSave(Uri)` / `deleteSave()` all call `refreshSaveState()` after completion
- `setStorageUri(Uri)` persists storage URI in shared prefs

## UI (`HomeScreen.kt`, `SettingsScreen.kt`)

- Two screens: `HomeScreen` + `SettingsScreen`, toggled via `showSettings` state in `MainScreen`
- **Games launcher layout**: full-bleed dark background (`surface`), title top-left, content centered in remaining space
- **No floating cards** — content sections use `Surface` with `surfaceVariant` color and rounded corners (20dp), zero elevation
- Save data management (Backup/Restore/Delete) moved to `SettingsScreen`
- Gear button (`\u2699`) in home TopBar opens settings; back arrow (`ArrowBack`) returns home
- `SettingsScreen` collects `saveState` from viewmodel, handles delete confirmation dialog inline
- **PrimaryActionButton** (`Launch Dolphin`, `Download & Install`, `Select Storage`, `Try Again`) — 56dp tall, filled `primary` color, `titleMedium` semi-bold
- **SecondaryActionButton** (`Check Again`, `Mii Maker`) — 48dp tall, outlined; shown side-by-side in a `Row` with `weight(1f)`
- Success messages shown as inline `Surface` banner rather than Card

## Key Constants

| Thing | Value |
|-------|-------|
| App name (display) | `"Wheel Witch"` |
| Pack name | `"Retro Rewind Pack"` |
| Dolphin package | `org.dolphinemu.dolphinemu` |
| Dolphin activity | `org.dolphinemu.dolphinemu.ui.main.MainActivity` |
| Launch intent extra | `"AutoStartFile"` (value: absolute path to RR.json) |
| Update server | `https://update.rwfc.net/` |
| Version file | `RetroRewind/RetroRewindVersion.txt` |
| Delete file | `RetroRewind/RetroRewindDelete.txt` |
| Full zip | `RetroRewind/zip/RetroRewind.zip` |
| Min reinstall version | `3.2.6` |
| Local version file | `<storage>/RetroRewind6/version.txt` |
| Save path (hardcoded `RMCP`) | `riivolution/save/RetroWFC/RMCP/rksys.dat` |

## Completed Improvements

- **Shared OkHttpClient** via `HttpClientProvider` singleton — replaces 4 separate instances
- **Consolidated state flows** — `RoomsState` / `SaveInfoState` sealed classes replaced 5+ individual flows each
- **R8 minification** enabled for release builds
- **`produceState` replaced with `LaunchedEffect`** in MiiFace/MiiPlayerCard for proper cancellation
- **VR multiplier badge** on launch buttons — fetches from `rwfc.net` and shows `2x` etc.
- **Gamepad focus borders** use `primary` theme color (follows dynamic or custom theme)
- **Custom red theme `onPrimary`** explicitly set for proper text contrast
- **Sparkle animation** trimmed to 3 visible positions (avoiding wizard hat overlap)
- **`@Immutable` annotations** added to `UiState`, `SaveState`, `MiiMakerState`, `RoomsState`, `SaveInfoState`, `PackStatus`, `ServerConnectivity`
- **RksysParser magic offsets** named constants (MII_NAME_OFFSET, VR_OFFSET, etc.)
- **RR.json I/O moved** from ViewModel into `DolphinLauncher` methods
- **Shared `FileDownloader` utility** extracted from duplicate private functions in `RewindPackManager` and `MiiWadInstaller`; MiiWadInstaller now gets proper HTTP status checking for free
- **Leaderboard race condition fixed** — `refreshSaveFileInfo()` uses `async`/`awaitAll` for atomic state update instead of concurrent lost writes; inner fetches are children of `saveInfoJob` so cancellation is effective

## State

All changes committed on `master`. No uncommitted work.

## Large Items Evaluation

### 1. Leaderboard race condition — LOW complexity (~30 min)
Collect all `async` results with `awaitAll` before a single atomic state write. Inner fetches become children of `saveInfoJob` for proper cancellation. Readability impact: moderate (eliminates subtle concurrency trap, makes intent self-documenting).

### 2. Shared download-to-file utility — LOW complexity (~30 min)
Consolidate duplicate private `downloadToFile` functions from `RewindPackManager` and `MiiWadInstaller` into `FileDownloader`. Readability impact: high (MiiWadInstaller call site drops from 25 lines to 1; MiiWadInstaller gains HTTP error checking for free).
