// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// ByteSource over a POSIX file descriptor. On Android the fd comes from
// ContentResolver.openFileDescriptor() (Storage Access Framework), so the app
// never needs broad storage permissions. The fd is NOT owned or closed here.
#pragma once

#include <sys/stat.h>
#include <unistd.h>

#include <cerrno>

#include "format_sniffer.h"

namespace ace360x::host {

class FdByteSource final : public ByteSource {
 public:
  explicit FdByteSource(int fd) : fd_(fd) {
    struct stat st {};
    size_ = (fd_ >= 0 && fstat(fd_, &st) == 0) ? static_cast<int64_t>(st.st_size) : -1;
  }

  int64_t Size() const override { return size_; }

  bool ReadAt(uint64_t offset, std::span<std::byte> out) const override {
    if (fd_ < 0 || size_ < 0) return false;
    const uint64_t size = static_cast<uint64_t>(size_);
    if (offset > size || out.size() > size - offset) return false;
    size_t done = 0;
    while (done < out.size()) {
      ssize_t n = pread(fd_, out.data() + done, out.size() - done,
                        static_cast<off_t>(offset + done));
      if (n < 0 && errno == EINTR) continue;
      if (n <= 0) return false;
      done += static_cast<size_t>(n);
    }
    return true;
  }

 private:
  int fd_;
  int64_t size_;
};

}  // namespace ace360x::host
