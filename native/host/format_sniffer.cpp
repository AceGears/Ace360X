// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
#include "format_sniffer.h"

#include <array>
#include <cstring>
#include <limits>

namespace ace360x::host {
namespace {

constexpr std::array<char, 4> kXex2Magic = {'X', 'E', 'X', '2'};
constexpr std::array<char, 4> kConMagic = {'C', 'O', 'N', ' '};
constexpr std::array<char, 4> kLiveMagic = {'L', 'I', 'V', 'E'};
constexpr std::array<char, 4> kPirsMagic = {'P', 'I', 'R', 'S'};
constexpr std::string_view kGdfxMagic = "MICROSOFT*XBOX*MEDIA";

template <size_t N>
bool Matches(std::span<const std::byte> bytes, const std::array<char, N>& magic) {
  return bytes.size() >= N && std::memcmp(bytes.data(), magic.data(), N) == 0;
}

}  // namespace

std::string_view GameFormatName(GameFormat format) {
  switch (format) {
    case GameFormat::kXex2: return "XEX2 executable";
    case GameFormat::kDiscImage: return "Disc image (GDFX)";
    case GameFormat::kStfsCon: return "STFS package (CON)";
    case GameFormat::kStfsLive: return "STFS package (LIVE)";
    case GameFormat::kStfsPirs: return "STFS package (PIRS)";
    case GameFormat::kUnknown: break;
  }
  return "Unknown";
}

bool MemoryByteSource::ReadAt(uint64_t offset, std::span<std::byte> out) const {
  const uint64_t size = data_.size();
  if (offset > size || out.size() > size - offset) {
    return false;  // overflow-safe range check
  }
  if (!out.empty()) {
    std::memcpy(out.data(), data_.data() + offset, out.size());
  }
  return true;
}

SniffResult SniffGameFormat(const ByteSource& source) {
  SniffResult result;

  std::array<std::byte, 4> head{};
  if (source.ReadAt(0, head)) {
    if (Matches(head, kXex2Magic)) return {GameFormat::kXex2, 0};
    if (Matches(head, kConMagic)) return {GameFormat::kStfsCon, 0};
    if (Matches(head, kLiveMagic)) return {GameFormat::kStfsLive, 0};
    if (Matches(head, kPirsMagic)) return {GameFormat::kStfsPirs, 0};
  }

  std::array<std::byte, kGdfxMagic.size()> magic{};
  for (uint64_t partition : kDiscPartitionOffsets) {
    // Both values are small compile-time constants; no overflow possible.
    const uint64_t offset = partition + kGdfxMagicSectorOffset;
    if (!source.ReadAt(offset, magic)) continue;
    if (std::memcmp(magic.data(), kGdfxMagic.data(), kGdfxMagic.size()) == 0) {
      return {GameFormat::kDiscImage, partition};
    }
  }
  return result;
}

}  // namespace ace360x::host
