# Technical Feasibility Report — Xbox 360 Emulation on Android (ARM64)

Status: Phase 1 deliverable. Date: 2026-11-16. Author: Claude (lead architect role).
Every claim below is labelled **[Demonstrated]** (shown publicly by someone, with source),
**[Plausible]** (technically expected but not shown by us), or **[Unproven]**.
Nothing in this report was measured by this project; we have no device yet.

---

## 1. Executive summary

1. Xbox 360 emulation on Android is **possible but immature**. [Demonstrated] Several public
   projects (aX360e, X360 Mobile, XenDroid, xenia-android → Xeo) run some titles on high-end
   Snapdragon devices. All of them are ports of **Xenia** (Canary/Edge), not original emulators.
2. Writing a new Xbox 360 emulator from scratch is **not a responsible strategy**. Xenia
   represents ~12 years of reverse engineering (kernel HLE, Xenos GPU, XMA audio, STFS/GDFX
   formats). Re-deriving it would take a team many years before booting a commercial game.
3. **Recommended strategy:** build on upstream **Xenia Canary** (BSD 3-Clause) using a
   *patch-overlay + submodule* layout, contribute ARM64/Android fixes upstream where possible,
   and make our original contribution the **Android integration layer, performance/thermal
   system, library, input and UI** — the parts the brief emphasises and where existing ports are
   weakest.
4. Realistic near-term outcome: lighter / 2D / less CPU-bound titles on Snapdragon 8 Gen 2+
   class hardware. Demanding 3D titles at full speed remain **[Unproven]** on any phone.
5. A 60 FPS target is only meaningful per-title, after measurement. No global 60 FPS claim
   will be made.

## 2. Landscape (as of Nov 2026)

| Project | Base | Notes |
|---|---|---|
| Xenia Canary | upstream, BSD-3 | Windows/Linux. Experimental **A64 (ARM64) JIT backend** using the `oaknut` emitter; ARM64 codegen optimizations still landing in 2026. Vulkan backend now comparable to D3D12. |
| Xenia Edge | Canary fork | Merges canary_experimental; used as a reference by XenDroid. |
| aX360e | Xenia arm64-backend + Canary | Closed-ish freemium Play Store app (Dec 2025). Developer states most bugs come from Xenia's incomplete Linux ARM64 backend. Prior source-attribution controversy around the same dev's aPS3e. **Do not reuse code.** |
| X360 Mobile | Canary ARM + Edge | Public alpha June 2026; press noted security concerns. |
| XenDroid | Xenia Edge | States Snapdragon 8 Gen 2+/Adreno 740+ requirement; supports custom Turnip drivers. |
| xenia-android → **Xeo** | Canary submodule + `patches/` overlay | BSD-3, Gradle/CMake/NDK r26. Archived Aug 2026, continued as `libaerto/xeo`. Best public reference for project layout. |

Implication: the hard part (guest correctness) is shared upstream; differentiation comes from
Android engineering quality, honesty, and UX.

## 3. Subsystem feasibility

| Subsystem | Assessment |
|---|---|
| **CPU (Xenon, 3 cores × 2 HW threads, PPC64 + VMX128)** | Reuse Xenia's PPC frontend + HIR + A64 backend. [Demonstrated] boots games. Risks: VMX128 → NEON precision/NaN semantics, big-endian byte-swapping cost, 6 guest threads vs. phone big.LITTLE scheduling, W^X JIT memory on Android (needs dual-mapped memfd or `mprotect` toggling). |
| **GPU (Xenos, unified shaders, 10 MB eDRAM)** | Reuse Xenia's Vulkan backend + DXBC/SPIR-V shader translation. Risks: eDRAM emulation is bandwidth-heavy on tiler GPUs; Adreno/Mali driver bugs; missing features (e.g. `shaderStorageImageWriteWithoutFormat`, fragment shader interlock) force slower paths. Mali support **[Unproven]**. |
| **Memory** | Xenia reserves a large 4 GB+ guest address space with fixed host mappings. On Android this requires 64-bit VA and works within app limits [Demonstrated by ports]; low-RAM (<8 GB) devices risk LMK kills. |
| **Kernel/XAM HLE** | Platform-independent in Xenia; mostly portable. Thread priorities/affinity need Android mapping. |
| **Audio (XMA2)** | Xenia decodes XMA via FFmpeg; output to be replaced with **Oboe/AAudio**. [Plausible] |
| **File formats** | ISO (GDFX/XGD2/3), XEX2, STFS (LIVE/CON), "God games" folders via Xenia's VFS. Must be adapted from POSIX paths to **SAF file descriptors** (`/proc/self/fd`) — non-trivial; ports exist [Demonstrated]. |
| **Input** | Android `InputDevice`/`MotionEvent` → Xenia HID (XInput-like) — straightforward. |
| **Build** | Xenia uses premake; Android ports replace it with CMake. Third-party deps (FFmpeg, glslang, SPIRV-Tools, xxhash, fmt, etc.) need NDK builds. Medium effort, [Demonstrated]. |

## 4. Licensing

- Xenia / Canary / Xeo: **BSD 3-Clause** — compatible; keep copyright notices and LICENSE in
  the APK's "Open-source licences" screen.
- Third-party: FFmpeg (LGPL — must be dynamically linked or LGPL-compliant build config),
  oaknut (MIT), Vulkan-Headers/VMA (MIT/Apache-2.0), glslang/SPIRV (BSD/Apache-2.0), Oboe (Apache-2.0).
- Custom GPU drivers (Turnip via libadrenotools, MIT/BSD) — legal but a **security risk**
  (loading user-supplied `.so`). Only as opt-in, clearly labelled Experimental feature.
- **Never** bundle Xbox 360 firmware, dashboard files, keys, or games. Xenia does not require
  a BIOS/firmware dump, which keeps the app distributable.

## 5. Hardware tiers (provisional — to be replaced by measured data)

| Tier | Example SoC | Expectation |
|---|---|---|
| A | Snapdragon 8 Elite / 8 Gen 3 | Primary target. Some titles playable [Demonstrated by other ports]. |
| B | Snapdragon 8 Gen 2 / Adreno 740 | Minimum suggested by XenDroid. |
| C | Older Snapdragon, Dimensity/Mali, Tensor | **Unsupported/Untested** until measured. |
| Floor | ARM64, Android 10+ (API 29), Vulkan 1.1, 8 GB RAM | Proposed minimum; to be validated. |

## 6. Environment constraints of *this* development session

- Linux x86_64 cloud workspace, 2 cores, 7 GB RAM.
- JDK, Gradle, CMake 3.28, Ninja, Clang present. **Android SDK/NDK cannot be downloaded**
  (dl.google.com blocked by network allowlist) → **no APK can be built here**.
- GitHub reachable → upstream source can be cloned and studied.
- **No physical Android device** → every performance, thermal, battery and compatibility
  claim stays **[Unverified]** until the user runs tests on hardware.

What *can* be done here: architecture, CLAUDE.md, Kotlin/Compose frontend code, native
glue code, host-side unit tests of pure C++ (e.g. parsers, settings, frame-pacing logic)
compiled with desktop Clang, and fuzz harnesses for file parsers.

## 7. Key risks

1. Upstream A64 backend immaturity → crashes/incorrect results beyond our control.
2. Driver fragmentation (Adreno versions, Mali) → per-driver workarounds need evidence.
3. Thermals: sustained CPU-bound JIT load will throttle within minutes on phones.
4. SAF performance for multi-GB ISOs (random reads via fd) — must benchmark.
5. Legal/reputational: competing ports have faced code-provenance accusations; we must keep
   provenance clean and attribution explicit.
6. Scope: the brief (12 themes, full motion system) is UI-heavy; it must not starve the core.

## 8. Recommended roadmap (vertical slice first)

1. **Slice 0** — Android project skeleton (Compose shell, JNI bridge, Vulkan surface) that
   builds in Android Studio. Verifiable by user.
2. **Slice 1** — Xenia Canary core built via CMake for `arm64-v8a`; boot a homebrew XEX
   (legal test content) to a rendered frame. Success = first frame + logs, not "playable".
3. **Slice 2** — SAF-backed game loading (ISO/XEX/STFS), error reporting, library scanning.
4. **Slice 3** — Input (controller + touch), Oboe audio, overlay, frame-time HUD.
5. **Slice 4** — Measured profiling, thermal subsystem (ADPF `PerformanceHintManager`,
   `PowerManager.getThermalHeadroom`), profiles.
6. **Slice 5** — Theme engine (12 themes), customization, motion system.
7. Ongoing — compatibility DB populated only from documented tests.

## 9. Sources

- Android Authority, "aX360e Xbox 360 emulator" (Dec 2025) and X360 Mobile coverage (Apr/Jun 2026)
- TecMundo, aX360e announcement (Dec 2025)
- github.com/xenia-android/xenia-android (archived Aug 2026) and github.com/libaerto/xeo
- xenia-project/xenia PR #2259 "[a64] Implement an ARM64 backend" (Wunkolo, oaknut)
- Prism News, Xenia Canary ARM64 optimizations (Mar 2026)
- AlternativeTo, XenDroid device requirements
- Emulation General Wiki, Xenia
