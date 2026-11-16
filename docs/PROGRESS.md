# Development progress

## 2026-11-16 — Phase 2 foundation

### Completed and verified
- Host C++ unit tests for the game-format sniffer pass (Linux, clang 18).
- GitHub Actions green: host tests, 120 s libFuzzer run (no crashes), JVM unit tests,
  debug APK builds (~26 MB) and contains lib/arm64-v8a/libace360x.so.

### Implemented but unverified (no device yet)
- Android project: Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1, Compose, NDK r29, CMake.
- Native library `libace360x.so`: JNI bridge, Vulkan capability probe, format sniffer over SAF file descriptors.
- Library screen: add/remove SAF folders (persisted), recursive scan, format identification. No launching.
- Device screen: provisional-minimum check, phone info, live thermal status/headroom, Vulkan features.
- About screen: status, legal, privacy, licences.
- GitHub Actions: host tests + 120 s fuzz; Android unit tests + debug APK artifact.

### Blocked
- APK builds in the development workspace (no SDK/NDK access) — handled by GitHub Actions.
- All on-device verification — needs the user's phone.

### Known gaps
- Xenia Canary core not yet included (Phase 3).
- Library results are not cached between launches; rescans on start.
- Single base theme only; theme engine is Phase 9.

### Next priorities
1. Confirm the APK installs/launches on the user's phone (closes Phase 2).
2. Phase 3: add Xenia Canary as a pinned submodule; CMake build of the core for arm64-v8a.
