# Patches to Xenia Canary

Applied in filename order to `native/xenia-upstream` at CMake configure time by
`native/cmake/ApplyXeniaPatches.cmake` (idempotent). Upstream commits are never edited;
the submodule pointer pins the upstream version. Each patch should be small, explain
itself in this table, and be offered upstream where it makes sense.

| Patch | Why |
|---|---|
| 0001-cmake-android-platform-sources | `xe_platform_sources()` had no Android branch, so no platform files were compiled. Adds POSIX + `_android` files. Adds `XENIA_DISABLE_WERROR` so NDK-clang warnings are logged rather than fatal while the Android port is brought up. |
| 0002-cmake-no-sdl3-on-android | SDL3 isn't used on Android; skip building it and leave the `SDL3` interface target empty. |
| 0003-android-source-fixes | Bit-rot in Android code paths found by the first NDK build: `window_android` uses the new `OnActualSizeUpdate(…, WindowResizeAction, …)` signature; `imgui_drawer` gates fontconfig/GTK/embedded-font code to desktop Linux; `mutex.cc` casts `pthread_t` portably (it's `long` on bionic); `threading_posix` skips robust mutexes (not in bionic — process-death recovery isn't needed inside one app) and fixes const-correctness of the Android pre-API-26 thread name helper. |

To regenerate a patch: edit files in `native/xenia-upstream`, run
`git -C native/xenia-upstream diff -- <files> > native/patches/NNNN-name.patch`, then
`git -C native/xenia-upstream checkout -- .`.
