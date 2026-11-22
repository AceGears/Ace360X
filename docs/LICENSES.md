# Licences of bundled code

Ace360X's own code is BSD 3-Clause (`LICENSE`). The native library also links code from the
Xenia Canary submodule and libraries it bundles. Licences below were read from each project's
licence file in `native/xenia-upstream/third_party/` at upstream commit `28bc50db`
(2026-11-16). Only libraries actually linked on Android are listed; that list will be confirmed
against the final `.so` symbols before any release.

| Component | Licence | Notes |
|---|---|---|
| Xenia Canary | BSD 3-Clause | © Ben Vanik and Xenia contributors |
| FFmpeg (XMA decoder subset) | LGPL 2.1+ | `ffmpeg-xenia/config.h`: `FFMPEG_LICENSE "LGPL version 2.1 or later"`; statically linked — compliance via full corresponding source being public and rebuildable (this repo + submodules) |
| libmspack | LGPL 2.1 | XEX LZX decompression; same compliance note as FFmpeg |
| fmt | MIT | |
| imgui | MIT | |
| VulkanMemoryAllocator | MIT | |
| Vulkan-Headers | Apache 2.0 or MIT | |
| Vulkan-ValidationLayers (`libVkLayer_khronos_validation.so`) | Apache 2.0 | **Debug APK only**, fetched in CI by `scripts/ci-fetch-validation-layer.sh` (pinned tag); not in release builds |
| glslang | BSD 3-Clause (+ others per file) | |
| xbyak_aarch64 | Apache 2.0 | ARM64 code emitter used by the JIT |
| capstone | BSD 3-Clause / LLVM licence | disassembler |
| xxhash | BSD 2-Clause | |
| zstd | BSD 3-Clause (dual-licensed with GPLv2; used under BSD) | |
| zlib-ng | zlib | |
| snappy | BSD 3-Clause | |
| pugixml, rapidjson, tomlplusplus, cxxopts, tabulate, date, disruptorplus, half, aes_128, zarchive, discord-rpc | MIT | discord-rpc is compiled but Discord presence is not used on Android |
| rapidcsv | BSD 3-Clause | |
| utfcpp | Boost 1.0 | |
| FidelityFX CAS / FSR | MIT | |
| crypto/sha256 | zlib | crypto/TinySHA1: permissive (ISC-style); rijndael-alg-fst: public domain |
| stb | public domain / MIT | |

## Resolved question

- **`third_party/dxbc` (DXBCChecksum, © AMD, "All rights reserved", no licence grant in the
  file).** Used only for Direct3D 12 shader containers. CI runs `llvm-nm` on every release-mode
  `libace360x.so`: **no dxbc symbols are linked on Android** (first checked 2026-11-16, run
  "CI: fix dxbc licence check"). The library is listed in CMake for link resolution only; the
  linker drops it. If that check ever warns, this must be revisited before a release.

Android app dependencies (AndroidX, Jetpack Compose, Kotlin coroutines): Apache 2.0.
