<div align="center">
  <img src=".github/assets/logo/wizard-hat.svg" alt="Wheel Witch" width="120">
</div>

<h1 align="center">Wheel Witch</h1>

<br>

<div align="center">
  <a href="https://github.com/skiletro/WheelWitch/actions/workflows/build.yml"><img src="https://github.com/skiletro/WheelWitch/actions/workflows/build.yml/badge.svg" alt="Build"></a>&nbsp;&nbsp;
  <img src="https://img.shields.io/badge/license-GPL--3.0-blue.svg" alt="License: GPL-3.0">&nbsp;&nbsp;
  <img src="https://img.shields.io/badge/min%20SDK-31-green.svg" alt="Min SDK 31">&nbsp;&nbsp;
  <img src="https://img.shields.io/badge/Kotlin-1.9-purple.svg" alt="Kotlin">&nbsp;&nbsp;
  <img src="https://img.shields.io/badge/Jetpack%20Compose-2026.02-blue.svg" alt="Compose">
</div>

<br>

<div align="center">
  <a href="https://github.com/skiletro/WheelWitch/releases/tag/latest"><img src="./.github/assets/badges/github.webp" alt="Latest Build" width="220"></a>&nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.skiletro.wheelwitch%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fskiletro%2FWheelWitch%22%2C%22author%22%3A%22skiletro%22%2C%22name%22%3A%22WheelWitch%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22wheelwitch-.%2A%5C%5C%5C%5C.apk%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%28%5C%5C%5C%5Cd%2B%5C%5C%5C%5C.%5C%5C%5C%5Cd%2B%5C%5C%5C%5C.%5C%5C%5C%5Cd%2B%29%5C%22%2C%5C%22releaseTitleAsVersion%5C%22%3Atrue%7D%22%7D"><img src="./.github/assets/badges/obtainium.webp" alt="Add to Obtainium" width="220"></a>
</div>

## Screenshots

| Home | Online Menu | Online Rooms |
|------|-------------|--------------|
| ![Home](.github/assets/screenshots/home.webp) | ![Online Menu](.github/assets/screenshots/online_menu.webp) | ![Online Rooms](.github/assets/screenshots/online_rooms.webp) |

| Quick Launch | Licenses | Settings |
|--------------|----------|----------|
| ![Quick Launch](.github/assets/screenshots/quick_launch.webp) | ![Licenses](.github/assets/screenshots/licenses.webp) | ![Settings](.github/assets/screenshots/settings.webp) |

| Race Stats | | |
|------------|---|---|
| ![Race Stats](.github/assets/screenshots/race_stats.webp) | | |

## About

Wheel Witch is an Android companion app for [Retro Rewind](https://wiki.tockdom.com/wiki/Retro_Rewind), a custom Mario Kart Wii distribution.
It downloads and incrementally updates the pack from the RWFC server, then launches Dolphin with the pack pre-loaded.

## Features

- One-tap full install + incremental updates
- Save data (license) backup/restore via file picker
- Live in-app leaderboard, online rooms, server health, race stats
- Home-screen quick launch shortcut
- On-device Mii Channel WAD installer from GameBanana
- Multiple themes including Material You dynamic colour, with dark, light, and system modes

> [!NOTE]
> Save backup/restore currently only supports the **PAL** version of Mario Kart Wii (`RMCP`). - see [TODO](##todo)

## Download

The latest signed release APK is built automatically on every push to `master` and published as a [pre-release](https://github.com/skiletro/WheelWitch/releases/tag/latest) with auto-generated changelog. You can also install it via [Obtainium](https://github.com/ImranR98/Obtainium) by importing the config from the button above.

To build from source or contribute, see [CONTRIBUTING.md](CONTRIBUTING.md#build).

## Requirements

- Android 12+ (API 31)
- [Dolphin Emulator](https://dolphinemu.com) installed
- Mario Kart Wii ISO

## First Time Setup

1. Open the app, tap **Select Storage Folder** to choose where the pack files go
2. After installing the pack, select your Mario Kart Wii ISO when prompted
3. Tap **Launch Dolphin**

For returning users, the gear icon opens Settings, and the **Quick Launch** section lets you pin a home-screen shortcut that skips onboarding entirely.

## Building & Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build instructions, signing setup, and contribution guidelines.

## TODO

- [ ] Back up `wc24scr.vff` - either the file itself or the entire `RMCP` folder
- [ ] Generalise save backup/restore to support other `RMCx` save types (not just `RMCP` - see `SaveManager.kt:8`)
- [ ] Disable license button if no licenses are created, or they cannot be found.
- [ ] Fix Wheel Witch logo speed changing depending on device
- [ ] Add animation to downloading and extracting bar (smooth rather than solid steps)
- [ ] Add logcat debugging information throughout program
- [ ] Add button to export logging so end users can report bugs easier

## Credits

- **[Retro Rewind](https://wiki.tockdom.com/wiki/Retro_Rewind)** and the **Wheel Wizard** team for the pack format and update server
- **[Dolphin Emulator](https://dolphinemu.org)** for the runtime
- **[Tockdom wiki](https://wiki.tockdom.com)** for the changelog source
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)**, **[Material 3](https://m3.material.io)**, and **[OkHttp](https://square.github.io/okhttp/)** for the building blocks
- **[Obtainium](https://github.com/ImranR98/Obtainium)** for making sideloaded auto-updates painless

Nintendo owns Mario Kart Wii. This project is unofficial and not affiliated with Nintendo.

> [!IMPORTANT]
> Parts of this codebase were written with assistance from [MiniMax M3](https://minimax.io) and [GLM-4.7](https://z.ai) as a way to get more familiar with AI tooling.
> It was especially useful for writing test cases and porting save-related logic from the original Wheel Wizard project.
> I'm always open to conversation about AI in development and I believe it would have been disingenuous to omit the fact that it was used; I see these tools as a way to help write code, not a replacement for understanding what you're building.
> They should be used with care and in moderation.
>
> Small disclaimer over. Please don't hate me. :(
