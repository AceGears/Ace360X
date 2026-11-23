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

## 2026-11-17 — Diagnostics and touch controls

### Implemented (CI-built, not yet run on a device)
- **Share diagnostics** (About screen, and a Library banner after an unexpected game exit):
  text report with build/device info, Android `ApplicationExitInfo` exit reasons (native
  crash, low memory…), Xenia log tail and this app's logcat. Shared only via the share sheet.
- **On-screen touch controls** on the game screen: two analog sticks, d-pad, A/B/X/Y,
  LB/RB, LT/RT (digital), LS/RS clicks, Back/Start, haptics; show/hide toggle; touches
  outside the controls still reach Xenia's ImGui menus; a physical controller replaces touch
  as player 1 automatically.

### Known gaps
- Touch layout isn't customisable yet (position/size/opacity); triggers are digital only.
- Still no on-device result for anything — the next step needs the user's phone.

## 2026-11-17 (later) — Settings, thermal notice, library features, fixes

### Implemented (CI-built, not yet run on a device)
- **Settings tab** — every option maps to a verified Xenia cvar or real app behaviour:
  graphics presets (Original / Enhanced / High quality / Custom), internal resolution 1–3×,
  FXAA, CAS/FSR, keep aspect ratio, mute, touch-control visibility/opacity/size, and an
  Advanced guest-VSync switch with a warning. Settings travel to the game process in the
  launch Intent.
- **Thermal notice** on the game screen from Android's thermal status (informational only).
- **Library**: cached results on start, search, sort (name / recently played / most played /
  size), favourites with filter, last-played and playtime per game.
- **Fixes from code review**: Back now asks before quitting (some controllers send Back from
  B, which used to end the game instantly); launching a second game while one is open now
  explains why the old one came back.

### Deliberately not implemented (and why)
- **Eco / Performance / Automatic profiles and frame caps**: in Xenia `vsync` and
  `framerate_limit` pace the guest's vertical blank, so capping them changes game speed. With
  no on-device measurements yet, any profile would be a guess (CLAUDE.md §7, §20).
- **Battery Saver / Balanced graphics presets**: they would be identical to Original today.

### Still missing
- Launching extracted game folders (loose XEX) and multi-file GoD/SVOD — needs a SAF-tree VFS
  device in native code.
- Touch layout editing, analog touch triggers, controller remapping, per-game settings,
  in-game overlay of our own, cover art, the 12-theme engine, ADPF performance hints.
- Any on-device result.

## 2026-11-17 (night) — Extracted games and Games on Demand

### Implemented (CI-built, not yet run on a device)
- Extracted games (folder with `default.xex`) and Games on Demand (STFS header + `.data`
  folder) launch from SAF folders through a new `acetree://` provider: upstream patches 0008
  (generic `ExternalTreeProvider` hook in `xe::filesystem`) and 0009 (SVOD check), native
  `saf_tree_provider.cc`, Kotlin `SafTree`. Design: docs/research/saf-tree-vfs.md.
- Library shows extracted games and GoD titles by folder name; all recognised formats are now
  launchable (rescan needed for entries cached before this build).

### Unverified / risks
- Mount speed of large extracted games over SAF (one query per directory) — unmeasured.
- ZAR archives still go through a plain file path in zarchive and aren't covered.

## 2026-11-18 — ARM64 CPU correctness

### Verified (host, real ARM64 CPU in CI — not a phone)
- Upstream's PPC instruction test suite on the a64 JIT: **170,448 / 170,448 pass**, up from
  147,535 (86.6 %). Every failure was floating point. Report with CI run IDs:
  `docs/reports/cpu/ppc-tests-2026-11-18.md`. The workflow is now a gate.
- Fixes, as upstream patches to offer back:
  - 0010: negated multiply-adds (`fnmadd`/`fnmsub`/`vnmsubfp`) returned un-negated results;
    wrong default-NaN sign; wrong FMA NaN priority; wrong `vmaxfp`/`vminfp` NaN choice.
  - 0011: bit-exact `fres`/`vrefp`/`vlogefp`/`vexptefp` via upstream's `ppc::Estimate*`.
  - 0012: `vmsum3fp128`/`vmsum4fp128` truncate like upstream x64's default path.
- The `vnmsubfp` sign bug alone hit 13,655 test cases. It is common in vector math, so it
  probably affected any title that uses it on ARM64.

### Not measured
- Speed cost of 0011 (a native call per estimate instruction). A NEON table version can come
  later, with before/after numbers.
- Any of this on an Android device; whether any game boots.

### Still needs the user
- First on-device run: Device tab self-test, launch a game, share diagnostics.

## 2026-11-18 (evening) — Theme engine

### Implemented (CI-built and unit-tested, not yet seen on a device)
- 12 themes in `app/.../theme/`: each has its own palette, heading/body fonts (Android system
  fonts only, no new dependency), corner style (rounded/cut, sizes), card style (tonal,
  outlined, glass, glow), motion tokens, icon style, a Canvas background ("ambiance"), and
  component overrides (nav indicator, switch/slider colour roles, progress thickness, focus
  ring). Minimal Luxe deliberately has no background effect.
- Settings → Appearance: theme picker with previews in each theme's own colours, shapes and
  font; accent override (only accents that keep WCAG AA in that theme); background effect;
  reduce motion; corner style; reset. Stored as a diff over the base theme (DataStore
  `theme`, schema v1). The game's quit dialog follows the theme via the launch Intent.
- Motion: tab transitions and ambiance use the theme's tokens; with Android animations off
  (`ANIMATOR_DURATION_SCALE` = 0) or "Reduce motion", durations are 0 and the background is a
  static frame. No ambiance exists in the game process.
- `ThemeTest` (JVM, CI run 44395421531): all 12 themes and every offered accent pass WCAG AA
  (4.5:1) for body, secondary, primary and error text on every surface level, on the
  ambiance tint and on glass cards; focus rings ≥ 3:1; themes differ beyond colour.

### Not verified
- How the themes look and perform on a phone: no screenshots, no frame timing for animated
  backgrounds. Phase 9 needs visual review screenshots from a device.
- ktlint/detekt (CLAUDE.md §4) are still not set up in CI.

## 2026-11-18 (night) — Per-game settings, compatibility database

### Implemented (CI-built and unit-tested, not seen on a device)
- Per-game settings: settings button on each game row. Resolution, anti-aliasing,
  sharpening, picture shape, sound and guest VSync either follow the Settings tab or are
  fixed for that game. Stored as a diff (one small file per game, schema v1). Controller and
  touch options stay global. CI run 44436645746.
- `compat/compat.json` (empty: every title is Untested), `compat/README.md` with the entry
  format, and `scripts/validate-compat.py` run in CI: entries without evidence, with an
  unknown status or with extra fields (e.g. an FPS claim) fail the build.

### Not done
- The app does not read the compatibility DB: it needs the title ID, which we do not extract
  yet (would be a new parser for untrusted files, so it needs fuzzing first).
- Remaining gaps: touch layout editing, analog touch triggers, cover art, ADPF hints,
  in-game overlay, ZAR archives, ktlint/detekt.

## 2026-11-21 — First on-device game launch (user)

### Verified on a device (evidence: docs/reports/device/2026-11-21-first-launch.md)
- App installs and runs on a Samsung SM-S948U1 (SM8850, Adreno 840, Android 17): Library, SAF
  folder scan, theme. Phase 2 "app launches on device" is met.
- Naruto: Rise of a Ninja (disc image via SAF) loads; its XEX runs on the a64 JIT; kernel,
  VFS, AAudio and the Vulkan GPU initialise. No crash in a 3.6 s session. Frames, menu, sound
  and input not yet observed.

### Fixed from that report
- False "ended unexpectedly" banner after a normal quit (marker written after upstream's
  quick_exit). Section headings drawn black on themed screens. Out-of-date Library notice.
- Public `latest-debug` pre-release so the APK downloads without a GitHub account.

## Session 2026-11-22/23 — black picture on the Adreno 840 found and fixed

- Method: GPU frame captures replayed on Mesa lavapipe (reference) and on the phone, with
  diagnostic patches 0013–0020 halving the pipeline each round (presentation → readback →
  resolve → rasterization → pixel stage).
- Root cause (patch 0021): translated pixel shaders declared `gl_SampleMask` without writing
  it; the Qualcomm driver dropped every fragment. Fix: write full coverage at shader start.
- **Verified on device** (build 7dcdd66, screenshots from the tester): title screen and
  gameplay of 555307E5 render. Evidence: `compat/evidence/555307E5-2026-11-23.txt`.
- Not tested: frame rate/speed (no measurements exist), sustained runs, other titles, other
  devices. Controller/touch input during gameplay was not explicitly reported.
- Open: presenter stops painting after swapchain recreation (seen once); banner fix
  unconfirmed; diagnostic cvars from 0016–0020 remain in the build, off by default.
