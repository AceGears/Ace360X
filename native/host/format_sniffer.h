// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
//
// Identifies the container format of a user-selected file from its header bytes.
// Inputs are UNTRUSTED: every read is bounds-checked through the ByteSource
// interface, and no field from the file is used as a size or offset without
// validation. This only *identifies* a format; it does not claim the emulator
// can run the content.
#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <string_view>

namespace ace360x::host {

enum class GameFormat : int {
  kUnknown = 0,
  kXex2 = 1,        // Xbox 360 executable (default.xex)
  kDiscImage = 2,   // GDFX / XDVDFS disc image (.iso)
  kStfsCon = 3,     // STFS package signed by console ("CON ")
  kStfsLive = 4,    // STFS package signed by Xbox LIVE ("LIVE")
  kStfsPirs = 5,    // STFS package signed by Microsoft ("PIRS")
};

std::string_view GameFormatName(GameFormat format);

// Abstract random-access reader so the sniffer can run over a file descriptor
// (Android SAF), an in-memory buffer (tests), or a fuzzer input.
class ByteSource {
 public:
  virtual ~ByteSource() = default;
  // Total size in bytes, or a negative value if unknown.
  virtual int64_t Size() const = 0;
  // Reads exactly out.size() bytes at offset. Returns false on short read,
  // I/O error, or out-of-range request. Must never read outside the source.
  virtual bool ReadAt(uint64_t offset, std::span<std::byte> out) const = 0;
};

class MemoryByteSource final : public ByteSource {
 public:
  explicit MemoryByteSource(std::span<const std::byte> data) : data_(data) {}
  int64_t Size() const override { return static_cast<int64_t>(data_.size()); }
  bool ReadAt(uint64_t offset, std::span<std::byte> out) const override;

 private:
  std::span<const std::byte> data_;
};

// Disc images: offsets of the game partition that Xenia also probes, plus the
// GDFX magic which lives 32 sectors (32 * 2048 bytes) into the partition.
inline constexpr uint64_t kGdfxMagicSectorOffset = 32ull * 2048ull;
inline constexpr uint64_t kDiscPartitionOffsets[] = {
    0x00000000ull, 0x0000FB20ull, 0x00020600ull, 0x02080000ull, 0x0FD90000ull};

struct SniffResult {
  GameFormat format = GameFormat::kUnknown;
  uint64_t partition_offset = 0;  // only meaningful for kDiscImage
};

SniffResult SniffGameFormat(const ByteSource& source);

}  // namespace ace360x::host
