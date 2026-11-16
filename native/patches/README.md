# Patches to Xenia Canary

Applied in filename order to `native/xenia-upstream` at CMake configure time by
`native/cmake/ApplyXeniaPatches.cmake` (idempotent). Upstream commits are never edited;
the submodule pointer pins the upstream version. Each patch should be small, explain
itself in this table, and be offered upstream where it makes sense.

| Patch | Why |
|---|---|
| 0001-cmake-android-platform-sources | `xe_platform_sources()` had no Android branch, so no platform files were compiled. Adds POSIX + `_android` files. Adds `XENIA_DISABLE_WERROR` so NDK-clang warnings are logged rather than fatal while the Android port is brought up. |
| 0002-cmake-no-sdl3-on-android | SDL3 isn't used on Android; skip building it and leave the `SDL3` interface target empty. |

To regenerate a patch: edit files in `native/xenia-upstream`, run
`git -C native/xenia-upstream diff -- <files> > native/patches/NNNN-name.patch`, then
`git -C native/xenia-upstream checkout -- .`.
