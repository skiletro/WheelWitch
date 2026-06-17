# WheelWitch

An Android app that downloads/updates the Retro Rewind Mario Kart Wii Pack and launches Dolphin Emulator.

## Build & Dev

```bash
./gradlew assembleDebug     # build APK
./gradlew assembleDebug --build-cache  # cached build
```

No test framework — test sources deleted.

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
- **Dolphin launch**: uses `AutoStartFile` intent extra with kebab-case JSON fields; RR.json generated at storage root
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
│   └── ProgressInfo.kt
├── data/                — Storage and persistence
│   ├── PackStorage.kt   — SAF + direct file I/O
│   └── SaveManager.kt   — Save file management
├── network/             — HTTP layer
│   └── VersionFileParser.kt — Version + delete file fetching
├── domain/              — Business logic
│   └── RewindPackManager.kt — Install/update orchestration
├── util/                — Utilities
│   └── DolphinLauncher.kt — JSON generation, intent launch, ISO prefs
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

- **State machine**: `NoStorage → Checking → Downloading/Extracting/ApplyingUpdate → Ready/ReadyToLaunch/Error`
- `saveState` (StateFlow of `SaveState(hasSave)`) independently tracks save presence
- `successMessage` (StateFlow of `String?`) drives a brief auto-dismiss success banner (3s)
- `backupSave(Uri)` / `restoreSave(Uri)` / `deleteSave()` all call `refreshSaveState()` after completion
- `setStorageUri(Uri)` persists storage URI in shared prefs

## UI (`HomeScreen.kt`)

- Single-screen app, no navigation
- Centered `Column` with scroll, cards for each state
- Ready/ReadyToLaunch states show main card + save card side-by-side in a `Row` (always landscape)
- Save card has Backup (disabled if no save) + Restore buttons, plus "Delete save data" TextButton with confirmation `AlertDialog`
- Primary action buttons (`Launch Dolphin`, `Download & Install`) are 56dp tall; secondary buttons 48dp

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

## State

All changes committed on `master`. No uncommitted work.
