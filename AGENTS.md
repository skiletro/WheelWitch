# WheelWitch

An Android app that downloads/updates the Retro Rewind Mario Kart Wii Pack and launches Dolphin Emulator.

## Build & Dev

```bash
./gradlew assembleDebug                  # build APK
./gradlew assembleDebug --build-cache    # cached build
./gradlew testDebugUnitTest              # run all unit tests
./gradlew testDebugUnitTest --tests "com.skiletro.wheelwitch.model.SemVersionTest"  # single test class
./gradlew assembleRelease                # release build (R8/ProGuard)
```

## Git

- Commit after each logical feature change; use descriptive messages matching the existing conversational style (e.g. "add room status thingie").
- Stage only the intended files with `git add <file>` and commit with `git commit -m "<message>"`.
- Read `git status` before every commit; never use `git add -A` or `git commit -a` without first verifying the diff.

## Architecture

- **Min SDK**: 31 (Android 12)
- **Target SDK**: 36
- **Toolchain**: Java 11
- **UI**: Compose + Material3 with dynamic color (`dynamicColor = true` in `Theme.kt`)
- **Theme XML**: `Theme.Material3.DayNight.NoActionBar` (in `res/values/themes.xml`)
- **Screen orientation**: Landscape-locked (`android:screenOrientation="landscape"` in manifest)
- **Fullscreen**: `WindowCompat.getInsetsController(...).hide(systemBars())` — no `FLAG_FULLSCREEN`
- **No Google Play Services** — sideloaded APK only
- **Gradle daemon heap**: `-Xmx4096m` (in `gradle.properties`) — bumped from 2048m for AGP 9 + K2 compiler headroom

## Key Decisions & Conventions

- **SAF folder picker** for pack storage location; direct `java.io.File` path resolution for I/O with `DocumentFile` fallback for metadata
- **OkHttp 4.12.0** for HTTP (connection pooling, progress tracking); `HttpClientProvider` exposes both `client` (15s timeout) and `largeDownloadClient` (60s read timeout)
- **Dolphin launch**: uses `AutoStartFile` intent extra with kebab-case JSON fields; RR.json read/written via `DolphinLauncher` methods (`readIsoPathFromLaunchJson`/`writeLaunchJson`/`deleteLaunchJson`)
- **Screen navigation**: flat overlay pattern via `AnimatedVisibility` inside `Box` — no NavHost/routes; `MainScreen` orchestrates `HomeScreen`/`SettingsScreen`/`OnboardingScreen`, `HomeScreen` orchestrates `OnlineMenuScreen`/`SaveInfoScreen`; internal pages within `OnlineMenuScreen` use `AnimatedContent` with `OnlineMenuPage` enum
- **Gamepad focus isolation**: background screens are conditionally composed (not just visually hidden) when overlays are active — `if (!showSettings)` for `HomeScreen`, `if (!(showOnlineMenu || showSaveInfo))` for `Scaffold` — prevents d-pad focus leaking through overlays
- **Leaderboard rows**: use `.clickable { }` (not just `.focusable()`) for proper Compose focus engagement; 5dp `primary` border on focus
- **Leaderboard pagination**: one-shot `hasRequestedFocus` guard prevents focus-stealing when more data loads
- **File downloads**: consolidated in `FileDownloader.downloadToFile()` with optional progress callback, HTTP status validation, and configurable OkHttp client
- **Mii Maker (WAD install)**: downloads `https://filecache45.gamebanana.com/mods/mii_channel_symbols_-_hacs.zip` to app cache, extracts the `.wad` file, and launches Dolphin via `ACTION_VIEW` intent with FileProvider content URI
- **Update server**: `https://update.rwfc.net/` (same as WheelWizard); version file manifest, deletion list, fallback to full reinstall if version < 3.2.6
- **Pipelined extraction** removed (`LinkedBlockingQueue` pipe caused hang at 99%)
- **Copy buffer**: 262144 bytes
- **Parallel incremental downloads** via `async/await`
- **Save backup/restore** fully user-directed via file pickers (no automatic `.backups` folder)
- **i18n ready**: all user-facing strings live in `res/values/strings.xml`; Compose code uses `stringResource(R.string.xxx)`, ViewModels use `app.getString(...)`

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
│   ├── MiiWadOnboarding.kt — Sealed class for Mii WAD install step
│   ├── RoomStatus.kt    — Online rooms API + ServerConnectivity
│   ├── ServerHealth.kt  — parseHealthResponse()
│   ├── RaceStats.kt     — parseRaceStats()
│   ├── TimeTrialEntry.kt — parseTracks()
│   ├── LeaderboardEntry.kt — parseLeaderboardResponse()
│   ├── ChangelogEntry.kt
│   ├── SaveFileInfo.kt
│   └── LicenseInfo.kt
├── data/                — Storage and persistence
│   ├── PackStorage.kt   — SAF + direct file I/O, raw: id fallback for OTG
│   ├── SaveManager.kt   — Save file management
│   └── RksysParser.kt   — RKPD save data parser (uses java.util.Base64)
├── network/             — HTTP layer
│   └── VersionFileParser.kt — Version + delete file fetching, leaderboard API, rooms API; exposes `parseUpdatesText()`/`parseDeletionsText()` as package-visible top-level functions for testing
├── domain/              — Business logic
│   ├── RewindPackManager.kt — Install/update orchestration
│   └── ChangelogParser.kt   — Fetches + parses Tockdom wiki changelog
├── util/                — Utilities
│   ├── DolphinLauncher.kt — Launch JSON generation, intent launch, ISO prefs, RR.json I/O
│   ├── FileDownloader.kt  — Shared download-to-file with optional progress + client selection
│   ├── HttpClientProvider.kt — OkHttp singletons (client, largeDownloadClient)
│   ├── MiiFaceCache.kt   — Thread-safe LRU on-disk bitmap cache, java.util.Base64
│   └── MiiWadInstaller.kt — Mii Channel WAD download + extract (uses copyTo)
├── ui/                  — Presentation layer
│   ├── components/      — Reusable composables
│   │   ├── Constants.kt        — buttonShape, sectionShape, focusBorder modifier
│   │   ├── Format.kt           — formatBytes, cacheSize
│   │   ├── StatusColors.kt     — statusColors() reads from LocalStatusColors
│   │   ├── ScreenHeader.kt     — Standard header with title/back/refresh
│   │   ├── FocusableSurface.kt — Clickable + focusable Surface wrapper
│   │   ├── MiiFace.kt          — Shared Mii face composable
│   │   └── ChangelogCard.kt    — Renders a ChangelogEntry
│   ├── screens/         — Screen-level composables
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       ├── StatusColors.kt — LightStatusColors / DarkStatusColors / LocalStatusColors
│       └── Type.kt
└── viewmodel/
    ├── PackUpdateViewModel.kt — Pack install/update state machine + storage URI
    ├── SaveDataViewModel.kt   — Save backup/restore, license parsing
    ├── MiiMakerViewModel.kt   — Mii WAD install (Mutex-guarded), launch, delete
    ├── OnlineViewModel.kt     — Rooms, leaderboard (Channel-based race-free), health, race stats
    ├── UiState.kt             — Sealed class for pack update flow
    └── SaveDataDelegate.kt    — (in PackUpdateViewModel.kt) interface for cross-VM notifications
```

## ViewModels

The original monolithic `UpdateViewModel` was split into three focused VMs:

- **`PackUpdateViewModel`** owns the install/update state machine and the storage URI.
  - State machine: `NoStorage → Checking → Downloading/Extracting/ApplyingUpdate → Ready/Error`
  - `successMessage` (StateFlow of `String?`) drives a brief auto-dismiss success banner (3s)
  - Methods: `setStorageUri(uri)`, `checkStatus()`, `downloadOrUpdate(status)`, `launchDolphin()`, `setGameIsoPath(path)`, `clearIsoPath()`, `dismissSuccess()`, `clearError()`
  - Exposes `currentStorage` via companion object for `SaveDataViewModel` to read
- **`SaveDataViewModel`** owns the save file state, backup/restore, per-slot info, and active license.
  - Implements `SaveDataDelegate` to receive `onPackStatusChanged` notifications from `PackUpdateViewModel`
  - State: `saveState` (hasSave), `saveInfoState` (sealed), `selectedSlotIndex`, `activeLicenseInfo`
  - Methods: `backupSave(uri)`, `restoreSave(uri)`, `deleteSave()`, `selectSlot(index)`, `refreshSaveState()`, `refreshActiveLicense()`, `refreshSaveFileInfo()`
- **`MiiMakerViewModel`** owns the Mii Channel WAD install/launch/delete.
  - State: `miiMakerState`, `isInstallingWad`, `miiMakerError`
  - `installMiiMakerWad()` is guarded by a `Mutex` so concurrent user taps cannot trigger parallel installs
  - Methods: `installMiiMakerWad()`, `launchMiiMaker()`, `deleteWad()`, `refreshMiiMakerState()`
- **`OnlineViewModel`** owns rooms, leaderboard, health, race stats, tracks
  - Leaderboard fetches are driven by a `Channel<Unit>(CONFLATED)` so the TOCTOU race on `isLeaderboardLoading` is impossible

## UI

- Two top-level screens: `HomeScreen` + `SettingsScreen`, toggled via `showSettings` state in `MainScreen`
- **Games launcher layout**: full-bleed dark background (`surface`), title top-left, content centered in remaining space
- **No floating cards** — content sections use `Surface` with `surfaceVariant` color and rounded corners (20dp), zero elevation
- Save data management (Backup/Restore/Delete) in `SettingsScreen`
- Gear button (`\u2699`) in home TopBar opens settings; back arrow (`ArrowBack`) returns home
- **PrimaryActionButton** (`Launch Dolphin`, `Download & Install`, `Select Storage`, `Try Again`) — 56dp tall, filled `primary` color, `titleMedium` semi-bold
- **SecondaryActionButton** (`Check Again`, `Mii Maker`) — 48dp tall, outlined; shown side-by-side in a `Row` with `weight(1f)`
- Success messages shown as inline `Surface` banner rather than Card
- Standard `ScreenHeader` composable used by all sub-screens (back / title / refresh / trailing slot)
- Status colors read from `LocalStatusColors` composition local (light/dark aware)

## Components (`ui/components/`)

- **`buttonShape` / `sectionShape`**: shared `RoundedCornerShape` values (14dp and 20dp)
- **`focusBorder` modifier**: draws a 3dp `primary` border when focused
- **`formatBytes(Long)`**: formats bytes as "N B" / "N KB" / "M.NN MB"
- **`cacheSize(File)`**: sum of file lengths in a directory
- **`statusColors()`**: composable accessor for the active `StatusColors` from `LocalStatusColors`
- **`ScreenHeader`**: back / title / refresh header with focus-isolated icon buttons
- **`FocusableSurface`**: focusable + optionally clickable `Surface` with focus border
- **`MiiFace`**: shared Mii face composable (PNG decode + network fetch + cache + spinner)
- **`ChangelogCard`**: renders a `ChangelogEntry` as a card

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
- **Large download client** with 60s read timeout (separate from 15s metadata client)
- **Consolidated state flows** — `RoomsState` / `SaveInfoState` sealed classes replaced 5+ individual flows each
- **R8 minification** enabled for release builds
- **VM split**: 525-line `UpdateViewModel` broken into `PackUpdateViewModel` + `SaveDataViewModel` + `MiiMakerViewModel`
- **Leaderboard race fixed** via `Channel<Unit>(CONFLATED)` consumer (no more TOCTOU on `isLeaderboardLoading`)
- **MiiFace cache**: thread-safe with `@Synchronized`, periodic eviction (every 20 puts), `java.util.Base64`
- **Thread-safe MiiMaker install**: `Mutex.withLock` prevents double-tap races
- **WindowCompat fullscreen** instead of deprecated `FLAG_FULLSCREEN`
- **`produceState` replaced with `LaunchedEffect`** in MiiFace/MiiPlayerCard for proper cancellation
- **VR multiplier badge** on launch buttons — fetches from `rwfc.net` and shows `2x` etc.
- **Gamepad focus borders** use `primary` theme color (follows dynamic or custom theme)
- **Custom red theme `onPrimary`** explicitly set for proper text contrast
- **Sparkle animation** trimmed to 3 visible positions with `infiniteTransition` (no `withFrameNanos`)
- **`@Immutable` annotations** added to `UiState`, `SaveState`, `MiiMakerState`, `RoomsState`, `SaveInfoState`, `PackStatus`, `ServerConnectivity`
- **RksysParser magic offsets** named constants (MII_NAME_OFFSET, VR_OFFSET, etc.)
- **RR.json I/O moved** from ViewModel into `DolphinLauncher` methods
- **Shared `FileDownloader` utility** extracted from duplicate private functions
- **Leaderboard race condition fixed** — `refreshSaveFileInfo()` uses `async`/`awaitAll` for atomic state update
- **i18n ready**: all strings in `res/values/strings.xml` (250+ keys across 12 prefix groups)
- **StatusColors composition local**: theme-aware status colors via `LocalStatusColors`
- **Dead section files removed** (`AboutSection`, `CacheSection`, `MiiMakerSection`, `StorageSection`, `ThemeSection`, `SaveDataSection`, `IsoSection` — 620 lines)
- **Cloud backup rules**: `backup_rules.xml` and `data_extraction_rules.xml` include `settings.xml`, `wheelwitch.xml`, `changelog_cache.xml`, `race_stats_cache.xml`
- **Shared `ui/components/` package** holds 7 reusable composables, eliminating ~200 lines of duplication
- **OTG `raw:` SAF id** fallback in `PackStorage.resolveContentUriToPath`

## Gamepad Focus Navigation

The app uses several patterns to support gamepad/d-pad navigation:

- **Overlay focus isolation**: Background screens are removed from composition (not just visually hidden) when overlays are active. `MainScreen` composes `HomeScreen` only when `!showSettings`; `HomeScreen` composes `Scaffold` only when `!(showOnlineMenu || showSaveInfo)`. This prevents d-pad focus from leaking through overlays.
- **Leaderboard rows**: Each row uses `.clickable { }` + `.focusable()` + `.onFocusChanged {}` for proper Compose focus engagement. When focused, a 5dp `primary` border is drawn via `Modifier.border()`. A one-shot `hasRequestedFocus` guard in `LaunchedEffect(entries)` prevents focus from being stolen back to the container on pagination.
- **Header controls**: Back and Refresh `IconButton`s on leaderboard/rooms screens use `.focusable()` + `.onFocusChanged {}` + `.focusBorder()` so gamepad users can navigate to them. The shared `ScreenHeader` composable does this automatically.
- **Focus state hoisting**: `var focused by remember { ... }` declarations live at the top of their containing `@Composable` function — never inside an `IconButton` lambda. The original bug was the var being declared inside a lambda, which loses the state on recomposition.

## Tests

### Test stack
- **JUnit 5 (Jupiter)** — test framework
- **MockK 1.13.x** — Kotlin mocking (`mockkObject()` for static object singletons)
- **Truth 1.4.x** — readable assertions
- **`org.json:json`** test dependency — real `org.json` implementation for tests (Android stubs throw "not mocked")

### Running
```bash
./gradlew testDebugUnitTest                                         # all unit tests
./gradlew testDebugUnitTest --tests "com.skiletro.wheelwitch.model.SemVersionTest"  # single class
```

### Test files (~89 tests)

| File | Tests | What it covers |
|---|---|---|
| `model/SemVersionTest.kt` | 25 | `parse()` valid/invalid, `compareTo()` ordering, `toString()` |
| `model/RoomStatusTest.kt` | 7 | `parseRooms()` — full room, null race, empty array, missing fields |
| `util/DolphinLauncherTest.kt` | 9 | `generateLaunchJson()` JSON structure, `readIsoPathFromLaunchJson()` file I/O |
| `util/MiiFaceCacheTest.kt` | 5 | `cacheSize`, `clear`, `initWith` |
| `util/MiiWadInstallerTest.kt` | 8 | `isValidWad` magic validation, `extractWad` zip extraction, HACS variant selection, missing file |
| `data/RksysParserTest.kt` | 9 | Binary RKPD parsing — single/mixed/multiple slots, short data, friend code (PID 0x12345678 golden) |
| `network/VersionFileParserTest.kt` | 13 | `parseUpdatesText()` / `parseDeletionsText()` + `parseLeaderboardResponse`, `parseHealthResponse`, `parseTracks`, `parseRaceStats` |
| `domain/ChangelogParserTest.kt` | 7 | Jsoup `Document` parsing — table extraction, reversed order, spoilers-wrapped tables, fallback text |
| `domain/RewindPackManagerTest.kt` | 6 | `checkStatus()` variants, `freshInstall()` flow, `incrementalUpdate()` ordering |

### Testability notes
- Pure functions (no Android deps) are tested directly: `SemVersion`, `parseRooms()`, `generateLaunchJson()`, `parseUpdatesText()`, `parseDeletionsText()`, all `parse*Response()` functions
- `RksysParser` uses `java.util.Base64` (not `android.util.Base64`) so it's testable on JVM
- `MiiFaceCache` uses `java.util.Base64` (not `android.util.Base64`) so it's testable on JVM
- `MiiWadInstaller.isValidWad` and `extractWadForTest` are public for testing
- Static `object` singletons (`VersionFileParser`, `FileDownloader`) are mocked via `mockkObject()` — no interface extraction needed for testing
- `PackStorage` is already a class with constructor injection; mocked via `mockk()`
- `RewindPackManager` is a stateful `object` singleton — `initCacheDir()` must be called in each test that touches the cache
- ViewModels (`PackUpdateViewModel`, etc.) are not unit-tested in this suite — they require Android framework (`Application`, `SharedPreferences`). The pure logic they orchestrate is tested at the `domain` and `util` layer.
