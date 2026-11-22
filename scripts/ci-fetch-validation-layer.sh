#!/usr/bin/env bash
# Puts the Khronos Vulkan validation layer (Apache-2.0) into the DEBUG APK only, for
# on-device GPU debugging (CLAUDE.md §4: validation layers on in debug builds).
# Release builds never contain it. Pinned by release tag; on a missing asset the
# script lists what exists and exits non-zero (the CI step is continue-on-error).
set -euo pipefail
TAG="${VVL_TAG:-vulkan-sdk-1.4.335.0}"
REPO=KhronosGroup/Vulkan-ValidationLayers
DEST=app/src/debug/jniLibs/arm64-v8a
work=$(mktemp -d)
asset=$(gh api "repos/$REPO/releases/tags/$TAG" --jq '.assets[].name' | grep -i android | head -1 || true)
if [ -z "$asset" ]; then
  echo "No Android asset for $TAG. Recent releases:"
  gh api "repos/$REPO/releases?per_page=8" --jq '.[] | .tag_name + ": " + ([.assets[].name] | join(", "))'
  exit 1
fi
echo "Using $TAG / $asset"
gh release download "$TAG" --repo "$REPO" --pattern "$asset" --dir "$work"
unzip -q "$work/$asset" -d "$work/x"
so=$(find "$work/x" -path "*arm64-v8a*" -name "libVkLayer_khronos_validation.so" | head -1)
[ -n "$so" ] || { echo "arm64 layer not found in $asset"; find "$work/x" | head -40; exit 1; }
mkdir -p "$DEST"
cp "$so" "$DEST/"
ls -la "$DEST"
