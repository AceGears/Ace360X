# CLAUDE.md — X360 Android Emulator (working name: "Ace360X")

This file is the authoritative development guide. It MUST match reality. If code and this file
disagree, fix one of them in the same change. Feasibility research: `docs/FEASIBILITY.md`.

---

## 1. Identity and objectives
- Android (ARM64) frontend + port of the **Xenia Canary** Xbox 360 emulation core (BSD-3).
- Our original work: Android integration, SAF storage, input, Oboe audio, performance/thermal
  management, library, overlay, theme engine, UX. Guest emulation correctness comes primarily
  from upstream Xenia; fixes we make are offered upstream.
- Priorities, in order: **honesty > correctness > stability > sustained performance > UX polish.**

## 2. Supported platforms
- Android 10+ (minSdk 29), targetSdk = latest stable. ABI: `arm64-v8a` only.
- Vulkan 1.1 required; capabilities detected at runtime, never assumed.
- Provisional floor: 8 GB RAM, Snapdragon 8 Gen 2 / Adreno 740 class. Everything else is
  "Untested" until measured (see `docs/hardware/`).

## 3. Architecture and boundaries
```
app/                Kotlin + Jetpack Compose UI (no emulation logic), package com.acegears.ace360x
  core/NativeBridge.kt  the ONLY Kotlin side of JNI
  ui/  library/  device/  diag/ (Diagnostics report + share)  settings/ (EmulatorSettings → cvars)
  emu/ (EmulatorActivity, GamepadInput/GamepadMapper, TouchControls)
                    (later: settings/ theme/ overlay/)
app/src/main/java/jp/xenia/emulator/  Kotlin port of upstream WindowedAppActivity/
                    WindowSurfaceView — package/class names fixed by upstream's JNI
native/             CMake root (built by Gradle for Android, standalone for host tests)
  host/             jni_bridge.cpp (our JNI), SAF fd I/O, Vulkan probe, format sniffer,
                    core self-test, android_input_driver (hid), aaudio_audio_system (apu);
                    later thermal/ADPF, perf counters. Upstream's own Android JNI
                    (windowed_app_context_android.cc) is also exported.
  tests/            host-side C++ unit tests + libFuzzer targets
  xenia-upstream/   git submodule (xenia-canary, pinned commit); upstream commits never edited —
                    our changes are patch files applied at CMake configure time
  patches/          NNNN-*.patch on top of upstream, each explained in patches/README.md
  cmake/            ApplyXeniaPatches.cmake, XeniaVersion.cmake
scripts/            fetch-xenia-deps.sh (shallow, Android-relevant submodules), CI helpers
app/src/test        JVM unit tests for pure Kotlin logic
.github/workflows/  CI: host tests + fuzz, Android unit tests, debug APK artifact
ci/debug.keystore   PUBLIC debug-only signing key (never for releases)
docs/               feasibility, architecture, ADRs, reports
```
- Core never includes Android/Compose headers outside `native/host`.
- UI never blocks on the emulation thread; communication is via JNI calls returning quickly
  plus a lock-free event queue (status, errors, perf samples).
- Gameplay renders to a `SurfaceView` owned by native Vulkan; Compose overlay sits above it.

## 4. Coding standards
- **C++20** (C++23 only where NDK clang supports it). `-Wall -Wextra -Werror` on our code;
  upstream warnings tracked, not silenced globally. RAII only; no raw `new`/`delete` in our code.
  Bounds-checked spans for all guest/file data. clang-format (Xenia style for patches).
- **Kotlin**: official style, ktlint/detekt, coroutines + Flow, no `GlobalScope`, no `!!` in prod.
- **JNI**: every entry point catches all C++ exceptions and converts to a Kotlin error;
  local refs released; no JNIEnv caching across threads; strings validated as UTF-8.
- **Vulkan**: validation layers on in debug builds; every feature gated by queried capability;
  pipeline cache persisted per driver version.
- **Shaders**: SPIR-V generated from upstream translator; our own UI shaders are AGSL/GLSL with
  a fallback path for devices lacking RuntimeShader (API < 33).

## 5. Build and dependencies
- Android Studio (latest stable), AGP + Gradle wrapper, NDK r29 `29.0.14206865` (pinned in `app/build.gradle.kts` and CI),
  CMake 3.22+, JDK 17.
- `./gradlew assembleDebug` must work from a clean clone after `scripts/fetch-xenia-deps.sh`.
  Host tools: Python 3 and Vulkan SDK (`glslangValidator`, `spirv-opt`; CI pins SDK 1.4.363.1).
- The Xenia core is built via upstream's own CMake (`add_subdirectory`, `EXCLUDE_FROM_ALL`) with
  hidden symbol visibility; only JNI functions are exported from `libace360x.so`.
- All third-party versions pinned; licences listed in `docs/LICENSES.md` and shown in-app.
- No dependency added without: licence check, size impact, and reason in the PR/commit.

## 6. Research requirements
- Before implementing a subsystem, add/extend a note in `docs/research/<subsystem>.md` with
  sources (upstream code paths, public docs). Cite; don't copy non-BSD-compatible code.
- **Do not use code from aX360e, X360 Mobile or other projects with unclear licensing.**

## 7. Performance budgets (targets, not claims)
- UI: 16.6 ms frames on target tier; no jank > 2 frames during navigation.
- In-game: input → guest delivery < 1 frame; overlay open < 100 ms.
- Emulator frame pacing measured with `Choreographer` + native timestamps; report avg FPS,
  1 % low, frame-time stdev, emulation speed %.
- Every optimisation PR includes before/after numbers, device, build, title, and duration.
  No numbers → not merged.

## 8. CPU and GPU emulation standards
- Changes to the A64 backend need instruction-level tests vs. reference values (Xenia's
  `cpu-ppc-tests` style) and must pass on host before device.
- W^X compliance: JIT uses dual-mapped memory (RW + RX views); no RWX pages.
- GPU: correctness first; any "hack" option is per-game, off by default, labelled Experimental.
- Driver workarounds require: driver version, device, repro, and a link to the evidence.

## 9. UI/UX principles
- Compose + Material 3 foundations, customised by our theme engine. Touch targets ≥ 48 dp.
- Portrait + landscape for all non-game screens; gameplay landscape (portrait optional).
- Motion from central tokens (`MotionTokens`); respect `ANIMATOR_DURATION_SCALE` = 0 and
  reduced-motion settings. No continuous decoration while a game is running.
- Every control maps to real behaviour. Unimplemented = not shown (or disabled + "Coming later"
  only in debug builds).

## 10. Theme engine
- `ThemeSpec` = colour scheme + typography + shape + elevation + motion tokens + icon style +
  ambiance renderer + component overrides (cards, nav, dialogs, toggles, sliders, progress, focus).
- 12 themes (Obsidian Elite, Neon Velocity, Crimson Reactor, Arctic Glass, Aurora Spectrum,
  Titanium Forge, Emerald Matrix, Solar Flare, Midnight Cosmos, Retro Console, Minimal Luxe,
  Liquid Chrome). A theme that only changes colours does not count as done.
- User customisations stored as a diff over a base theme (DataStore, versioned schema).
- Every theme must pass WCAG AA contrast for text (automated test).

## 11. Battery and thermal
- Use ADPF: `PerformanceHintManager` for the emulation thread group, `PowerManager`
  thermal status + `getThermalHeadroom`. Sample at ≤ 1 Hz.
- Response ladder: disable ambiance → lower optional effects/resolution scale → notify user.
  Recover gradually with hysteresis.
- Never bypass thermal protections, force clocks, or require root.
- Sustained tests: 15/30/60 min, logged to `docs/reports/perf/`.

## 12. Security and privacy
- Permissions: none beyond what SAF/vibration/internet-free operation need. No INTERNET
  permission unless a user-facing feature requires it (e.g. cover downloads, opt-in).
- No ads, no tracking, no telemetry without explicit opt-in.
- All game files, metadata and patches are **untrusted input**: bounds checks, size limits,
  fuzzing (libFuzzer) for ISO/XEX/STFS/patch parsers.
- No bundled firmware, keys or games. Custom GPU driver loading (Turnip) only as opt-in
  Experimental with a clear warning; never downloaded automatically.
- Crash logs stay on device; user chooses to export.

## 13. Compatibility testing and regression
- Compatibility DB (`compat/compat.json`): title ID, status, device, driver, build hash, date,
  tester, notes, evidence (log/screenshot). Statuses: Playable, Playable with issues, In-game,
  Intro only, Boots but does not progress, Does not boot, Untested.
- Default for every title: **Untested**. Status changes require evidence.
- Regression suite = a fixed list of tested titles per tier, re-run before release.

## 14. Phases and acceptance criteria
| Phase | Done when |
|---|---|
| 1 Research | FEASIBILITY.md reviewed by user ✅ (draft written) |
| 2 Foundation | Clean clone builds debug APK in Android Studio; app launches on device |
| 3 Core | Upstream core cross-compiles for arm64-v8a; host CPU tests pass; homebrew XEX boots |
| 4 GFX/Audio | First guest frame via Vulkan on device; Oboe output without underruns in 10-min test |
| 5 Loading | ISO/XEX/STFS via SAF; malformed files rejected with message; fuzzers run 1 h clean |
| 6 Input | Controller + touch verified on ≥ 1 device; remap persists |
| 7 Perf | Profiles backed by measurements; 15/30/60-min reports exist |
| 8 UI | All screens navigable, no dead controls, a11y scan clean |
| 9 Themes | 12 themes pass contrast tests + visual review screenshots |
| 10–12 | Compat reports, security review doc, signed release build, limitations doc |

## 15. Known limitations and risks
- Upstream ARM64 backend is incomplete → many titles will not boot or will crash.
- Demanding 3D titles are not expected to reach full speed on current phones.
- Mali/PowerVR/non-Snapdragon support unknown. Low-RAM devices may be killed by LMK.
- The Claude workspace cannot download the Android SDK/NDK, so APKs are built only by
  GitHub Actions (repo AceGears/Ace360X). There is no device here; all device-dependent
  results require the user.

## 16. Debugging and crash investigation
- Native crashes: symbolised tombstones via `ndk-stack`; keep unstripped `.so` per build.
- Logs: ring-buffer log in native, exported from About & Diagnostics.
- Guest issues: reproduce on desktop Xenia Canary first. If it also fails there, it's upstream.
- Use systematic debugging: reproduce → isolate → hypothesise → verify → fix → regression test.

## 17. Agent responsibilities
Lead Architect (integration, this file), CPU, GPU, Android Systems, Performance, UI/UX,
Motion, Audio, Security, QA/Compat. Each agent works within its directory boundary, documents
interface changes in `docs/adr/`, and **no agent marks another's work verified without evidence**.

## 18. Documentation and release
- `docs/PROGRESS.md` updated at the end of every work session (template below).
- Release: signed AAB/APK, licences screen, CHANGELOG, known-issues list, compat snapshot.

## 19. Status vocabulary (used everywhere: code comments, UI, docs)
- **Complete** — implemented, compiled, tested, verified on a device (evidence linked).
- **Implemented (unverified)** — compiles and host tests pass; not yet verified on device.
- **Incomplete** — partially implemented; hidden from release UI.
- **Experimental** — works in some cases; labelled in UI; off by default.
- **Unsupported** — not planned or not possible; stated explicitly.

## 20. Integrity rules (non-negotiable)
1. No fabricated functionality, benchmarks, compatibility entries, or screenshots.
2. No placeholder implementations presented as working; stubs must `TODO()`/log loudly and
   be absent from release UI.
3. No FPS or "60 FPS" claims without a measured report. Frame generation/interpolation is never
   reported as emulation FPS.
4. If something couldn't be tested, say so in PROGRESS.md and the commit message.
5. Don't mark a phase complete in this file until its acceptance criteria are met with evidence.

---

## Current status (2026-11-16) — details in `docs/PROGRESS.md`
- Complete: —
- Implemented (unverified on device): Phase 2 app; Phase 3 in progress — Xenia Canary core
  (incl. a64 JIT, Vulkan GPU, kernel, VFS, FFmpeg XMA) compiles for arm64-v8a in CI with 4
  patches; on-device core self-test (guest memory + JIT executes PowerPC code).
- Verified: host C++ unit tests + fuzzing (format sniffer); CI compile of the full core.
- Implemented (unverified on device): game launch from Library (EmulatorActivity in `:emu`
  process hosting upstream EmulatorApp on a SurfaceView), SAF content:// launch (patch 0005),
  Android controller input (0006), AAudio sound (0007).
- Implemented (unverified on device): diagnostics sharing (About + unexpected-exit banner),
  on-screen touch controls.
- Implemented (unverified on device): Settings tab (graphics presets, resolution scale, FXAA,
  CAS/FSR, aspect, mute, touch options, guest VSync), thermal notice, library cache/search/
  sort/favourites/playtime, quit confirmation.
- Next: first on-device game launch by the user and their diagnostics report; fix what it shows;
  SAF-tree VFS device for extracted games.
