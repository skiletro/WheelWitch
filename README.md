# Wheel Witch

Downloads/updates the Retro Rewind Mario Kart Wii Pack and launches Dolphin Emulator.

## Build

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Requirements

- Android 12+ (API 31)
- Dolphin Emulator installed (from https://dolphinemu.com)
- Mario Kart Wii ISO (NTSC-U)

## First Time Setup

1. Open the app, tap **Select Storage Folder** to choose where the pack files go
2. After installing the pack, select your Mario Kart Wii ISO when prompted
3. Tap **Launch Dolphin**

## Features

- Full pack install and incremental updates (same server as WheelWizard)
- Save data backup/restore via file picker
- Progress bars for download and extraction
- Material You dynamic colours
- Landscape-locked (matching Dolphin's orientation)
