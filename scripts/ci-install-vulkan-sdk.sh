#!/usr/bin/env bash
# CI helper: installs the LunarG Vulkan SDK's host tools (glslangValidator,
# spirv-opt, spirv-dis) used to compile Xenia's SPIR-V shaders at build time,
# and exports VULKAN_SDK. Ubuntu's spirv-tools package is too old for the
# --canonicalize-ids pass upstream uses.
set -euo pipefail
VER="${VULKAN_SDK_VERSION:-1.4.363.1}"  # pinned; first resolved 2026-11-16
if [[ -z "$VER" ]]; then
  VER=$(curl -fsSL --retry 3 https://vulkan.lunarg.com/sdk/latest/linux.json | python3 -c 'import json,sys; print(json.load(sys.stdin)["linux"])')
fi
echo "Vulkan SDK version: $VER"
DEST="${RUNNER_TEMP:-/tmp}/vulkansdk"
if [[ ! -x "$DEST/$VER/x86_64/bin/spirv-opt" ]]; then
  mkdir -p "$DEST"
  url="https://sdk.lunarg.com/sdk/download/$VER/linux/vulkansdk-linux-x86_64-$VER.tar.xz"
  echo "Downloading Vulkan SDK $VER"
  tmp="$DEST/sdk.tar.xz"
  curl -fSL --retry 5 --retry-all-errors --retry-delay 5 -C - -o "$tmp" "$url"
  tar -xJf "$tmp" -C "$DEST"
  rm -f "$tmp"
fi
export VULKAN_SDK="$DEST/$VER/x86_64"
"$VULKAN_SDK/bin/spirv-opt" --version
if [[ -n "${GITHUB_ENV:-}" ]]; then echo "VULKAN_SDK=$VULKAN_SDK" >> "$GITHUB_ENV"; fi
