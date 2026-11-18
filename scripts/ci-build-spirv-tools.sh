#!/usr/bin/env bash
# Builds spirv-opt / spirv-dis from upstream Xenia's own SPIRV-Tools submodule
# (with the SPIRV-Headers commit pinned in its DEPS file) into an SDK-shaped
# folder, for hosts where the LunarG Vulkan SDK isn't available (Linux ARM64).
# glslangValidator comes from the distro (glslang-tools). Exports VULKAN_SDK.
# Usage: ci-build-spirv-tools.sh <prefix>
set -euo pipefail
PREFIX="$(realpath -m "$1")"
UP="$(cd "$(dirname "$0")/../native/xenia-upstream" && pwd)"
ST="$UP/third_party/SPIRV-Tools"
if [[ ! -x "$PREFIX/bin/spirv-opt" ]]; then
  rev=$(sed -n "s/.*'spirv_headers_revision': '\([0-9a-f]*\)'.*/\1/p" "$ST/DEPS")
  [[ -n "$rev" ]] || { echo "No spirv_headers_revision in DEPS"; exit 1; }
  WORK="$(mktemp -d)"
  git init -q "$WORK/headers"
  git -C "$WORK/headers" fetch -q --depth 1 https://github.com/KhronosGroup/SPIRV-Headers "$rev"
  git -C "$WORK/headers" checkout -q FETCH_HEAD
  cmake -S "$ST" -B "$WORK/build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DSPIRV-Headers_SOURCE_DIR="$WORK/headers" -DSPIRV_SKIP_TESTS=ON \
    -DSPIRV_WERROR=OFF -DCMAKE_INSTALL_PREFIX="$PREFIX" >/dev/null
  ninja -C "$WORK/build" spirv-opt spirv-dis >/dev/null
  mkdir -p "$PREFIX/bin"
  cp "$WORK/build/tools/spirv-opt" "$WORK/build/tools/spirv-dis" "$PREFIX/bin/"
fi
ln -sf "$(command -v glslangValidator)" "$PREFIX/bin/glslangValidator"
"$PREFIX/bin/spirv-opt" --version | head -1
if [[ -n "${GITHUB_ENV:-}" ]]; then echo "VULKAN_SDK=$PREFIX" >> "$GITHUB_ENV"; fi
