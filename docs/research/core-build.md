# Research: building the Xenia Canary core for Android

Date: 2026-11-16. Upstream inspected: `xenia-canary/xenia-canary` branch `canary_experimental`,
commit `28bc50db0923e5fe2a7770e1be80da5fb7b78605`.

## Upstream state
- Upstream has **moved from premake to CMake** (`CMakeLists.txt`, `cmake/XeniaHelpers.cmake`).
  Supported CMake platforms: Windows, Linux (x86-64 and AArch64), macOS. **No Android branch.**
- ARM64 CPU backend exists: `src/xenia/cpu/backend/a64/` (built when `XE_TARGET_AARCH64`),
  selected at runtime by `cvars::cpu == "a64"` (or automatically on ARM64) in `emulator.cc`.
- Android platform sources still exist from earlier upstream work (Triang3l):
  `base/{main,filesystem,system,cvar,console_app_main}_android.cc`,
  `ui/{window,windowed_app_context,windowed_app_main,surface,file_picker}_android.cc`,
  `ui/vulkan/functions/instance_khr_android_surface.inc`. They were last built with the old
  premake/ndk-build flow (`android/android_studio_project`, NDK 25), so bit-rot is expected.
- `xe_platform_sources()` only adds back `_win` or `_posix/_linux/_gtk/_x11` files, so on
  Android it would compile **no** platform files → needs an Android branch.
- SPIR-V shaders are compiled at build time by `tools/build/compile_shader_spirv.py` using host
  `glslangValidator`, `spirv-opt`, `spirv-dis` (Vulkan SDK or PATH). Output goes into
  `src/xenia/{gpu,ui}/shaders/bytecode/vulkan_spirv/` in the source tree.
- FFmpeg (XMA audio decode) is built from source with a Xenia config overlay (`ffmpeg-xenia`).
  LGPL-2.1: linked statically into our .so; compliance via full source availability of the
  app (documented in docs/LICENSES.md when added).
- SDL3 is built from the submodule when not found via pkg-config. Not wanted on Android.
- `xe_target_defaults()` adds `-Werror` for Clang.

## Xeo (libaerto/xeo, BSD-3) — evaluated, not adopted
- Hand-written CMake source list over a pinned upstream commit, plus overwritten upstream files.
- On ARM64 it compiles **no CPU JIT backend** and its emulator.cc falls back to `NullBackend`,
  i.e. guest code cannot execute. FFmpeg is replaced by `ffmpeg_stubs.c` (no XMA audio).
- Conclusion: its "production-ready" description is not supported by its source. We do not
  copy its code. Useful only as confirmation of which upstream Android files exist.

## Decision (ADR-0001)
Use upstream's own CMake project via `add_subdirectory(xenia-upstream EXCLUDE_FROM_ALL)` and
link only the libraries we need into `libace360x.so`. Android support is added by small,
reviewed patch files in `native/patches/`, applied idempotently with `git apply` at CMake
configure time. Upstream commits are never edited; the submodule pointer pins the version.

Nested upstream submodules are fetched shallowly by `scripts/fetch-xenia-deps.sh`, skipping
those not needed on Android (DirectXShaderCompiler, DirectX-Headers, SDL3, libusb, catch,
cpplint, xbyak).

## Open risks
- Bit-rotted Android sources may need several patches.
- `-Werror` under NDK clang: Android builds use `XENIA_WERROR=OFF` initially; warnings are
  logged and tracked rather than silenced (CLAUDE.md §4).
- Debug (unoptimised) native builds will be far too slow for real emulation; the APK will
  build the core in Release mode even in the debug app variant.
