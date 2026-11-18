#!/usr/bin/env bash
# Fetches the Xenia Canary submodule and the nested third-party submodules the
# Android build needs (shallow). Skips large/unneeded ones: DirectX shader
# compiler + headers (Windows/D3D12), SDL3 (not used on Android), libusb
# (Windows only), catch + cpplint (tests/lint), xbyak (x86-64 JIT).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UP="$ROOT/native/xenia-upstream"

git -C "$ROOT" submodule update --init --depth 1 native/xenia-upstream

SKIP_RE='^third_party/(DirectXShaderCompiler|DirectX-Headers|SDL3|libusb|catch|cpplint|xbyak)$'
# ACE_FETCH_DESKTOP=1: also fetch what a desktop Linux build needs (used by the
# ARM64 PPC instruction-test job in CI).
if [[ "${ACE_FETCH_DESKTOP:-0}" == "1" ]]; then
  SKIP_RE='^third_party/(DirectXShaderCompiler|DirectX-Headers|libusb|cpplint)$'
fi
mapfile -t paths < <(git -C "$UP" config -f .gitmodules --get-regexp '^submodule\..*\.path$' \
  | awk '{print $2}' | grep -Ev "$SKIP_RE")

echo "Fetching ${#paths[@]} upstream submodules (shallow)…"
git -C "$UP" submodule update --init --depth 1 --jobs 4 -- "${paths[@]}"
echo "Done."
