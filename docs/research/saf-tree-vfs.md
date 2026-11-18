# Research: launching extracted games and Games on Demand from SAF folders

Date: 2026-11-17. Upstream: xenia-canary `28bc50db`.

## Problem
- Extracted games (`default.xex` + data files) launch via `Emulator::LaunchPath` →
  `MountPath` → `CreateVfsDevice`, which mounts `path.parent_path()` as a
  `vfs::HostPathDevice` (src/xenia/emulator.cc). `HostPathDevice` walks a real directory.
- Games on Demand (SVOD) are an STFS header file plus a sibling `<name>.data/` folder of
  `Data0000…` fragments (`SvodContainerDevice::LoadHostFiles`).
- Android gives us Storage Access Framework *tree* URIs, not POSIX directories, and we keep
  only a persisted read grant (no storage permission). Re-opening SAF files by path through
  `/proc/self/fd` is not reliable on scoped storage.

## What HostPathDevice actually needs
All of its directory/file access goes through a handful of `xe::filesystem` functions:
`GetInfo`, `ListFiles`, `FileHandle::OpenExisting`, `OpenFile`, `MappedMemory::Open` — plus
`std::filesystem::exists` in `Initialize()` and a `canonical()` call used only for symlink-loop
detection (it fails harmlessly for non-POSIX paths). SVOD adds one `std::filesystem::exists`.

## Decision
Patch 0008 adds a generic `ExternalTreeProvider` hook (scheme + get_info/list_files/open_fd)
to `xe::filesystem` and routes those functions to it for matching paths; patch 0009 fixes the
SVOD existence check. No Android-specific code in the patches.

Ace360X registers the `acetree://` scheme (`native/host/saf_tree_provider.cc`), implemented
in Kotlin (`emu/SafTree.kt`) with `DocumentsContract` child queries and
`ContentResolver.openFileDescriptor`, caching document IDs from listings. Native emulator
threads reach Kotlin via `xe::GetAndroidThreadJniEnv()` with class/method IDs cached on the
main thread (app classes aren't visible to `FindClass` from native threads).

Path form: `acetree://<Uri.encode(treeUri)>/<relative/path>` — the encoded tree URI has no
`/`, so `std::filesystem::path` operations (`/`, `parent_path`, `filename`) behave.

## Risks / unverified
- Performance: `HostPathDevice` lists the whole game tree at mount; one SAF query per
  directory. Unmeasured; large games with thousands of folders may mount slowly.
- Writes: game folders are mounted read-only (`allow_game_relative_writes` default), matching
  our read-only grant.
- Not run on a device yet.
