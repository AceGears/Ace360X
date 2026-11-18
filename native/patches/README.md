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
| 0006-android-input-driver-registration | Registers Ace360X's Android controller driver (`native/host/android_input_driver.cc`) as the `android` HID option in `EmulatorApp::CreateInputDrivers`, so `hid=any` picks it up on Android. |
| 0007-android-aaudio-registration | Registers Ace360X's AAudio backend (`native/host/aaudio_audio_system.cc`) as the `aaudio` APU option ahead of `nop`, so `apu=any` gives sound on Android. |
| 0008-external-tree-provider | Adds a generic `ExternalTreeProvider` hook to `xe::filesystem` (scheme-prefixed virtual directory trees). `GetInfo`, `ListFiles`, `FileHandle::OpenExisting`, `OpenFile`, `MappedMemory::Open`, the `*AnyScheme` helpers and `HostPathDevice::Initialize` route matching paths to it. Ace360X registers an `acetree://` provider backed by Android SAF folders, so extracted games (folder + `default.xex`) mount through upstream's unchanged `HostPathDevice`. |
| 0009-svod-external-tree | Games on Demand (SVOD): the `.data` fragment-folder existence check uses `PathExistsAnyScheme`; listing and mapping fragments already go through the hooked helpers. |

To regenerate a patch (each is relative to the tree with all earlier patches applied): edit files in `native/xenia-upstream`, run
`git -C native/xenia-upstream diff -- <files> > native/patches/NNNN-name.patch`, then
`git -C native/xenia-upstream checkout -- .`.
