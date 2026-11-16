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

## 2026-11-16 (later) — Phase 3: Xenia Canary core on Android

### Completed and verified (in CI, not on a device)
- Xenia Canary `canary_experimental@28bc50db` compiles **and links** for arm64-v8a with NDK r29:
  a64 JIT backend, PPC frontend, kernel/XAM HLE, VFS, Xenos GPU + Vulkan backend, APU with
  FFmpeg XMA decoder (NEON asm), UI/ImGui, patcher. `libace360x.so` ≈ 24 MB; APK ≈ 50 MB.
- 4 upstream patches (CMake Android branch, no SDL3, Android source bit-rot, shader script).
- Shaders compiled to SPIR-V at build time with the LunarG Vulkan SDK 1.4.363.1.

### Implemented but unverified (needs the user's phone)
- Core self-test on the Device screen: reserve 4.5 GB guest address space, commit/access guest
  pages, start the a64 JIT, translate + run a 3-instruction PowerPC function (expects 42).
- Xenia Android init (`InitializeAndroidAppFromMainThread`) at app start.

### Findings
- Xeo (libaerto/xeo) does not build a CPU backend on ARM64 and stubs FFmpeg; not used.
- `third_party/dxbc` (AMD, no licence grant) — CI now reports whether it is linked.
- Upstream warnings are not fatal on Android yet (`XENIA_DISABLE_WERROR`); to be reviewed.

### Next priorities
1. User installs the APK and runs the self-test; record results in docs/hardware/.
2. Phase 4: Vulkan surface from a SurfaceView, `Emulator` setup with the Vulkan graphics
   system, launch a homebrew XEX, first guest frame.

## 2026-11-16 (evening) — Game launch path, controller input, sound

### Implemented (CI-built, **not yet run on a device**)
- **Launching games**: tapping an ISO or single-file STFS package (CON/LIVE/PIRS) in the Library
  opens `EmulatorActivity` in a separate `:emu` process. It hosts upstream's own `EmulatorApp`
  ("xenia" windowed app: emulator thread, Vulkan graphics system, ImGui window/menus) on a
  native `SurfaceView`, via a Kotlin port of upstream's `WindowedAppActivity`.
- **SAF game files**: patch 0005 lets Xenia open `content://` documents through the whole
  launch path, so no storage permission is needed.
- **Controller input**: `native/host/android_input_driver.cc` (Xenia `InputDriver`, 4 players,
  rumble) + `GamepadMapper`/`GamepadInput` in Kotlin (patch 0006 registers `hid=android`).
- **Sound**: `native/host/aaudio_audio_system.cc` (AAudio, low-latency, 5.1→stereo, reopen on
  device change, silent fallback; patch 0007 registers `apu=aaudio`).
- Xenia log written to the app's files dir (`xenia/xenia.log`) for diagnosis.

### Known gaps
- No on-screen touch controls yet (physical controller needed to play).
- Loose XEX folders, multi-file GoD/SVOD and ZAR can't be launched from SAF yet.
- No in-game overlay of our own; upstream's ImGui menus are what you see.
- No log export UI yet.
- Nothing about game compatibility or speed is known; all titles remain **Untested**.

### Next priorities
1. User test: launch a game, report what happens (screenshot + phone model), and run the
   core self-test.
2. Fix whatever the first device run shows; add log export so failures can be diagnosed.
3. On-screen touch controls; XEX-folder launching via SAF trees.
