// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// libFuzzer target: clang++ -fsanitize=fuzzer,address,undefined
#include <cstddef>
#include <cstdint>
#include <span>

#include "format_sniffer.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  ace360x::host::MemoryByteSource src(
      std::span<const std::byte>(reinterpret_cast<const std::byte*>(data), size));
  (void)ace360x::host::SniffGameFormat(src);
  return 0;
}
