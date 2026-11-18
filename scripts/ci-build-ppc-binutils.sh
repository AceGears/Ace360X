#!/usr/bin/env bash
# Builds powerpc-none-elf binutils 2.24 with Xenia's vmx128 patch (needed to
# assemble the PPC instruction tests). Same tarball, patch and configure flags
# as native/xenia-upstream/third_party/binutils/build.sh, but builds only gas,
# ld and binutils, and skips the docs (2.24's texinfo breaks with modern makeinfo).
# Usage: ci-build-ppc-binutils.sh <install-prefix>
set -euo pipefail
PREFIX="$(realpath -m "$1")"
UP="$(cd "$(dirname "$0")/../native/xenia-upstream" && pwd)"
VER=2.24
if [[ -x "$PREFIX/bin/powerpc-none-elf-as" ]]; then
  echo "binutils already built at $PREFIX"; exit 0
fi
WORK="$(mktemp -d)"
cd "$WORK"
curl -fsSL --retry 5 --retry-all-errors -o binutils.tar.gz \
  "https://mirrors.kernel.org/sourceware/binutils/releases/binutils-$VER.tar.gz"
tar xzf binutils.tar.gz
cd "binutils-$VER"
patch -p0 < "$UP/third_party/binutils/binutils-$VER-vmx128.patch"
./configure --disable-debug --disable-dependency-tracking --disable-werror \
  --enable-interwork --enable-multilib --target=powerpc-none-elf \
  --with-gnu-ld --with-gnu-as --prefix="$PREFIX" \
  CFLAGS="-O2 -fcommon -Wno-error -std=gnu89" >/dev/null
make -j"$(nproc)" MAKEINFO=true all-gas all-ld all-binutils >/dev/null
make MAKEINFO=true install-gas install-ld install-binutils >/dev/null
"$PREFIX/bin/powerpc-none-elf-as" --version | head -1
