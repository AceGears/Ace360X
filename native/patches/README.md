# Patches to Xenia Canary

Applied in filename order to `native/xenia-upstream` at CMake configure time by
`native/cmake/ApplyXeniaPatches.cmake` (idempotent). Upstream commits are never edited;
the submodule pointer pins the upstream version. Each patch should be small, explain
itself in this table, and be offered upstream where it makes sense.

| Patch | Why |
|---|---|
| 0001-cmake-android-platform-sources | `xe_platform_sources()` had no Android branch, so no platform files were compiled. Adds POSIX + `_android` files. Adds `XENIA_DISABLE_WERROR` so NDK-clang warnings are logged rather than fatal while the Android port is brought up. |
| 0002-cmake-no-sdl3-on-android | SDL3 isn't used on Android; skip building it and leave the `SDL3` interface target empty. |
| 0003-android-source-fixes | Bit-rot in Android code paths found by the first NDK build: `window_android` uses the new `OnActualSizeUpdate(…, WindowResizeAction, …)` signature; `imgui_drawer` gates fontconfig/GTK/embedded-font code to desktop Linux; `mutex.cc` casts `pthread_t` portably (it's `long` on bionic); `threading_posix` skips robust mutexes (not in bionic — process-death recovery isn't needed inside one app) and fixes const-correctness of the Android pre-API-26 thread name helper; removes a stub `SetAttributes` in `filesystem_android` that duplicated (and was worse than) the POSIX one. |
| 0004-shader-script-error-output | `compile_shader_spirv.py` crashed with a `TypeError` while printing tool errors (bytes vs str), hiding the real message. Tooling-only. |
| 0005-android-content-uri-game-launch | Lets a game be launched from a Storage Access Framework `content://` URI (ISO, single-file STFS/CON/LIVE/PIRS packages) so the app needs no storage permission. Extends upstream's existing `IsAndroidContentUri`/`OpenForAndroidContentUri` support to `OpenFile`, `MappedMemory::Open`, file-type detection, `RunTitle` and package size checks via new `PathExistsAnyScheme`/`FileSizeAnyScheme`/`IsNonHostPath` helpers. Not covered: extracted XEX folders, multi-file SVOD/GoD, ZAR. |

To regenerate a patch (each is relative to the tree with all earlier patches applied): edit files in `native/xenia-upstream`, run
`git -C native/xenia-upstream diff -- <files> > native/patches/NNNN-name.patch`, then
`git -C native/xenia-upstream checkout -- .`.
