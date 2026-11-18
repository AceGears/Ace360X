#!/usr/bin/env bash
# Assembles Xenia's PPC instruction tests into bin/ (equivalent of upstream
# `xb gentests`, xenia-build.py GenTestsCommand).
# Usage: ci-gen-ppc-tests.sh <binutils-prefix>
set -euo pipefail
BU="$(realpath "$1")/bin"
UP="$(cd "$(dirname "$0")/../native/xenia-upstream" && pwd)"
SRC="$UP/src/xenia/cpu/ppc/testing"
OUT="$SRC/bin"
mkdir -p "$OUT"
count=0
for s in "$SRC"/instr_*.s "$SRC"/seq_*.s; do
  [[ -e "$s" ]] || continue
  name="$(basename "$s" .s)"
  "$BU/powerpc-none-elf-as" -a32 -be -mregnames -ma2 -maltivec -mvsx -mvmx128 -R -o"$OUT/$name.o" "$s"
  "$BU/powerpc-none-elf-objdump" --adjust-vma=0x100000 -Ma2 -Mvmx128 -D -EB "$OUT/$name.o" | tail -n +5 > "$OUT/$name.dis"
  "$BU/powerpc-none-elf-ld" -A powerpc:common32 -melf32ppc -EB -nostdlib --oformat=binary \
    -Ttext=0x80000000 -e0x80000000 -o"$OUT/$name.bin" "$OUT/$name.o"
  "$BU/powerpc-none-elf-nm" --numeric-sort "$OUT/$name.o" > "$OUT/$name.map"
  count=$((count + 1))
done
echo "Assembled $count test files into $OUT"
