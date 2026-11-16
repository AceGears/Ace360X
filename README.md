# Ace360X

An experimental Xbox 360 emulator for Android (ARM64), being built on top of
[Xenia Canary](https://github.com/xenia-canary/xenia-canary).

> **Status: early development — it cannot run games yet.**
> The current build scans your game folders, identifies Xbox 360 files (ISO / XEX / STFS)
> and reports your phone's Vulkan, RAM and thermal capabilities. The emulation core is
> the next phase. See [`docs/PROGRESS.md`](docs/PROGRESS.md).

## Getting a build

Every push to `main` builds a debug APK with GitHub Actions:
**Actions → latest "Build" run → Artifacts → `Ace360X-debug-…`**. Unzip it and install the APK
(you'll need to allow installs from your browser or file manager).
Debug builds share one public debug key, so each new build installs over the last.

To build locally: Android Studio (latest), JDK 17, then `./gradlew assembleDebug`.

## Requirements (provisional)

ARM64 phone, Android 10+, Vulkan 1.1, 8 GB RAM. Snapdragon 8 Gen 2 or newer is the expected
practical minimum once games run; nothing has been measured yet. See
[`docs/FEASIBILITY.md`](docs/FEASIBILITY.md).

## Legal

No Xbox 360 firmware, keys or games are included or downloaded. Use only backups of games you
own. Xbox and Xbox 360 are trademarks of Microsoft; this project is not affiliated with Microsoft.

## Project rules

[`CLAUDE.md`](CLAUDE.md) is the engineering guide, including the rule against fabricated
features, benchmarks or compatibility claims.

Licence: BSD 3-Clause.
