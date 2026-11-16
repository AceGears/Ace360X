// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// Pattern follows upstream's PPC test runner (cpu/ppc/testing/ppc_testing_main.cc).
#include "core_selftest.h"

#include <chrono>
#include <cstdio>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include "xenia/base/byte_order.h"
#include "xenia/base/main_android.h"
#include "xenia/cpu/backend/a64/a64_backend.h"
#include "xenia/cpu/processor.h"
#include "xenia/cpu/raw_module.h"
#include "xenia/cpu/thread_state.h"
#include "xenia/memory.h"

namespace ace360x::host {
namespace {

std::mutex g_mutex;
bool g_initialized = false;

struct Step {
  std::string name;
  bool ok = false;
  std::string detail;
  double ms = 0;
};

std::string Escape(const std::string& s) {
  std::string out;
  for (char c : s) {
    if (c == '"' || c == '\\') out += '\\';
    if (static_cast<unsigned char>(c) < 0x20) continue;
    out += c;
  }
  return out;
}

std::string ToJson(const std::vector<Step>& steps) {
  bool all_ok = !steps.empty();
  std::string j = "{\"steps\":[";
  for (size_t i = 0; i < steps.size(); ++i) {
    const Step& s = steps[i];
    all_ok = all_ok && s.ok;
    char ms[32];
    std::snprintf(ms, sizeof(ms), "%.2f", s.ms);
    if (i) j += ",";
    j += "{\"name\":\"" + Escape(s.name) + "\",\"ok\":" + (s.ok ? "true" : "false") +
         ",\"detail\":\"" + Escape(s.detail) + "\",\"ms\":" + ms + "}";
  }
  j += std::string("],\"ok\":") + (all_ok ? "true" : "false") + "}";
  return j;
}

class Timer {
 public:
  double ElapsedMs() const {
    return std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - start_)
        .count();
  }

 private:
  std::chrono::steady_clock::time_point start_ = std::chrono::steady_clock::now();
};

// Guest layout used by the test (inside the 0x80000000 XEX heap, 64 KB pages).
constexpr uint32_t kRegionBase = 0x81FE0000;  // PCR page + stack
constexpr uint32_t kRegionSize = 0x30000;
constexpr uint32_t kPcrAddress = 0x81FE0000;
constexpr uint32_t kStackTop = 0x81FFFF00;
constexpr uint32_t kCodeBase = 0x82000000;
constexpr uint32_t kCodeSize = 0x10000;

// int f(int a, int b) { return a + a + b; }  — PowerPC, big-endian.
constexpr uint32_t kTestCode[] = {
    0x7C631A14,  // add r3, r3, r3
    0x7C632214,  // add r3, r3, r4
    0x4E800020,  // blr
};

}  // namespace

void InitializeCore(JNIEnv* env, int api_level, jobject application_context) {
  std::lock_guard<std::mutex> lock(g_mutex);
  if (g_initialized) return;
  xe::InitializeAndroidAppFromMainThread(api_level, env, application_context, nullptr);
  g_initialized = true;
}

std::string RunCoreSelfTestJson() {
  std::lock_guard<std::mutex> lock(g_mutex);
  std::vector<Step> steps;
  if (!g_initialized) {
    steps.push_back({"Core initialised", false, "InitializeCore was not called", 0});
    return ToJson(steps);
  }

  // 1. Reserve the 4 GB guest address space (+512 MB physical views).
  auto memory = std::make_unique<xe::Memory>();
  {
    Timer t;
    Step s{"Reserve guest memory (4.5 GB address space)"};
    s.ok = memory->Initialize();
    s.ms = t.ElapsedMs();
    if (s.ok) {
      char buf[64];
      std::snprintf(buf, sizeof(buf), "Mapped at host address %p",
                    static_cast<void*>(memory->virtual_membase()));
      s.detail = buf;
    } else {
      s.detail = "Memory::Initialize failed (see logcat tag 'xenia')";
    }
    steps.push_back(s);
    if (!s.ok) return ToJson(steps);
  }

  // 2. Commit guest pages and check a write/read round trip.
  {
    Timer t;
    Step s{"Commit and access guest pages"};
    auto* heap = memory->LookupHeap(kRegionBase);
    bool ok = heap &&
              heap->AllocFixed(kRegionBase, kRegionSize, 0,
                               xe::kMemoryAllocationReserve | xe::kMemoryAllocationCommit,
                               xe::kMemoryProtectRead | xe::kMemoryProtectWrite) &&
              heap->AllocFixed(kCodeBase, kCodeSize, 0,
                               xe::kMemoryAllocationReserve | xe::kMemoryAllocationCommit,
                               xe::kMemoryProtectRead | xe::kMemoryProtectWrite);
    if (ok) {
      auto* p = memory->TranslateVirtual<uint8_t*>(kCodeBase);
      for (size_t i = 0; i < std::size(kTestCode); ++i) {
        xe::store_and_swap<uint32_t>(p + i * 4, kTestCode[i]);
      }
      ok = xe::load_and_swap<uint32_t>(p) == kTestCode[0] &&
           xe::load_and_swap<uint32_t>(p + 8) == kTestCode[2];
    }
    s.ok = ok;
    s.ms = t.ElapsedMs();
    s.detail = ok ? "Wrote and read back big-endian guest code" : "Guest heap allocation failed";
    steps.push_back(s);
    if (!ok) return ToJson(steps);
  }

  // 3. Start the PowerPC -> ARM64 JIT backend.
  auto processor = std::make_unique<xe::cpu::Processor>(memory.get(), nullptr);
  {
    Timer t;
    Step s{"Start ARM64 JIT backend (a64)"};
    s.ok = processor->Setup(std::make_unique<xe::cpu::backend::a64::A64Backend>());
    s.ms = t.ElapsedMs();
    s.detail = s.ok ? "Code cache and thunks created" : "Processor::Setup failed (see logcat)";
    steps.push_back(s);
    if (!s.ok) return ToJson(steps);
  }

  // 4. Translate and execute a small PowerPC function.
  {
    auto module = std::make_unique<xe::cpu::RawModule>(processor.get());
    module->set_name("ace360x-selftest");
    module->set_executable(true);
    module->SetAddressRange(kCodeBase, kCodeSize);
    processor->AddModule(std::move(module));

    auto thread_state =
        std::make_unique<xe::cpu::ThreadState>(processor.get(), 0x100, kStackTop, kPcrAddress);

    Timer t;
    Step s{"Translate and run PowerPC code"};
    uint64_t args[] = {20, 2};
    uint64_t result = processor->Execute(thread_state.get(), kCodeBase, args, 2);
    s.ms = t.ElapsedMs();
    s.ok = static_cast<uint32_t>(result) == 42;
    s.detail = "f(20, 2) = " + std::to_string(static_cast<int64_t>(result)) + ", expected 42";
    steps.push_back(s);
    thread_state.reset();
  }

  processor.reset();
  memory.reset();
  return ToJson(steps);
}

}  // namespace ace360x::host
