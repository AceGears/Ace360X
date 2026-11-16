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

## Open question

- **`third_party/dxbc` (DXBCChecksum, © AMD, "All rights reserved", no licence grant in the
  file).** Used for Direct3D 12 shader containers. Before any release we must confirm whether
  any of it is linked into `libace360x.so` (`nm`/`readelf` on the release build) and, if so,
  either confirm its licence upstream or exclude it on Android.

Android app dependencies (AndroidX, Jetpack Compose, Kotlin coroutines): Apache 2.0.
