#!/usr/bin/env bash
# CI helper: installs the LunarG Vulkan SDK's host tools (glslangValidator,
# spirv-opt, spirv-dis) used to compile Xenia's SPIR-V shaders at build time,
# and exports VULKAN_SDK. Ubuntu's spirv-tools package is too old for the
# --canonicalize-ids pass upstream uses.
set -euo pipefail
VER="${VULKAN_SDK_VERSION:-}"
if [[ -z "$VER" ]]; then
  VER=$(curl -fsSL --retry 3 https://vulkan.lunarg.com/sdk/latest/linux.json | python3 -c 'import json,sys; print(json.load(sys.stdin)["linux"])')
fi
echo "Vulkan SDK version: $VER"
DEST="${RUNNER_TEMP:-/tmp}/vulkansdk"
if [[ ! -x "$DEST/$VER/x86_64/bin/spirv-opt" ]]; then
  mkdir -p "$DEST"
  url="https://sdk.lunarg.com/sdk/download/$VER/linux/vulkansdk-linux-x86_64-$VER.tar.xz"
  echo "Downloading Vulkan SDK $VER"
  curl -fsSL --retry 3 "$url" | tar -xJ -C "$DEST"
fi
export VULKAN_SDK="$DEST/$VER/x86_64"
"$VULKAN_SDK/bin/spirv-opt" --version
if [[ -n "${GITHUB_ENV:-}" ]]; then echo "VULKAN_SDK=$VULKAN_SDK" >> "$GITHUB_ENV"; fi
