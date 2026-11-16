// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// Host-side unit tests (run on Linux in CI). Minimal harness, no dependencies.
#include <cstdio>
#include <cstring>
#include <string_view>
#include <vector>

#include "format_sniffer.h"

using ace360x::host::GameFormat;
using ace360x::host::MemoryByteSource;
using ace360x::host::SniffGameFormat;

static int g_failures = 0;
#define EXPECT(cond)                                                   \
  do {                                                                 \
    if (!(cond)) {                                                     \
      std::fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); \
      ++g_failures;                                                    \
    }                                                                  \
  } while (0)

static std::vector<std::byte> Buffer(size_t size) { return std::vector<std::byte>(size); }

static void Put(std::vector<std::byte>& buf, size_t offset, std::string_view text) {
  std::memcpy(buf.data() + offset, text.data(), text.size());
}

static GameFormat Sniff(const std::vector<std::byte>& buf) {
  MemoryByteSource src(buf);
  return SniffGameFormat(src).format;
}

int main() {
  // Empty and tiny inputs must not crash and must be Unknown.
  EXPECT(Sniff({}) == GameFormat::kUnknown);
  { auto b = Buffer(3); Put(b, 0, "XEX"); EXPECT(Sniff(b) == GameFormat::kUnknown); }

  // Header magics.
  { auto b = Buffer(16); Put(b, 0, "XEX2"); EXPECT(Sniff(b) == GameFormat::kXex2); }
  { auto b = Buffer(16); Put(b, 0, "CON "); EXPECT(Sniff(b) == GameFormat::kStfsCon); }
  { auto b = Buffer(16); Put(b, 0, "LIVE"); EXPECT(Sniff(b) == GameFormat::kStfsLive); }
  { auto b = Buffer(16); Put(b, 0, "PIRS"); EXPECT(Sniff(b) == GameFormat::kStfsPirs); }
  { auto b = Buffer(16); Put(b, 0, "XEX1"); EXPECT(Sniff(b) == GameFormat::kUnknown); }

  // Disc image at partition 0 and at a non-zero partition.
  {
    auto b = Buffer(0x10000 + 64);
    Put(b, 0x10000, "MICROSOFT*XBOX*MEDIA");
    MemoryByteSource src(b);
    auto r = SniffGameFormat(src);
    EXPECT(r.format == GameFormat::kDiscImage);
    EXPECT(r.partition_offset == 0);
  }
  {
    const size_t at = 0x02080000 + 0x10000;
    auto b = Buffer(at + 64);
    Put(b, at, "MICROSOFT*XBOX*MEDIA");
    MemoryByteSource src(b);
    auto r = SniffGameFormat(src);
    EXPECT(r.format == GameFormat::kDiscImage);
    EXPECT(r.partition_offset == 0x02080000);
  }

  // Magic truncated by end of file must not match or over-read.
  {
    auto b = Buffer(0x10000 + 10);
    Put(b, 0x10000, "MICROSOFT*");
    EXPECT(Sniff(b) == GameFormat::kUnknown);
  }

  // Out-of-range reads are rejected, including offset overflow.
  {
    auto b = Buffer(8);
    MemoryByteSource src(b);
    std::byte out[4];
    EXPECT(!src.ReadAt(6, out));
    EXPECT(!src.ReadAt(UINT64_MAX - 1, out));
    EXPECT(src.ReadAt(4, out));
  }

  if (g_failures == 0) std::puts("format_sniffer_test: all tests passed");
  return g_failures == 0 ? 0 : 1;
}
