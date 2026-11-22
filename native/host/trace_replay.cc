#include "trace_replay.h"

#include <filesystem>
#include <mutex>
#include <set>

#include "third_party/tomlplusplus/include/toml++/toml.hpp"
#include "xenia/base/cvar.h"
#include "xenia/base/logging.h"
#include "xenia/gpu/trace_dump.h"
#include "xenia/gpu/vulkan/vulkan_graphics_system.h"

namespace ace360x::host {
namespace {

// Upstream's VulkanTraceDump (vulkan_trace_dump_main.cc) without RenderDoc.
class AceVulkanTraceDump : public xe::gpu::TraceDump {
 public:
  std::unique_ptr<xe::gpu::GraphicsSystem> CreateGraphicsSystem() override {
    return std::make_unique<xe::gpu::vulkan::VulkanGraphicsSystem>();
  }
  void BeginHostCapture() override {}
  void EndHostCapture() override {}
};

std::mutex g_replay_mutex;
std::set<std::string> g_overridden;

std::string SetCvar(const std::string& name, const std::string& toml_value) {
  if (!cvar::ConfigVars) return "no cvars registered";
  auto it = cvar::ConfigVars->find(name);
  if (it == cvar::ConfigVars->end()) return "unknown cvar " + name;
  toml::parse_result parsed = toml::parse(name + " = " + toml_value);
  if (!parsed) return "bad value for " + name;
  const toml::table& table = parsed.table();
  it->second->LoadConfigValue(table.get(name));
  g_overridden.insert(name);
  return "";
}

}  // namespace

std::string ReplayTrace(const std::string& trace_path, const std::string& png_path,
                        const std::vector<std::pair<std::string, std::string>>& cvars) {
  std::lock_guard<std::mutex> lock(g_replay_mutex);
  static bool logging_initialized = false;
  if (!logging_initialized) {
    xe::InitializeLogging("ace360x-replay");
    logging_initialized = true;
  }
  if (cvar::ConfigVars) {
    for (const std::string& name : g_overridden) {
      auto it = cvar::ConfigVars->find(name);
      if (it != cvar::ConfigVars->end()) it->second->ResetConfigValueToDefault();
    }
  }
  g_overridden.clear();
  for (const auto& [name, value] : cvars) {
    std::string error = SetCvar(name, value);
    if (!error.empty()) return error;
  }

  std::error_code ec;
  std::filesystem::remove(png_path, ec);
  // TraceDump writes <output path with .png extension>; it also writes a config
  // file to the working directory, so run from the output folder.
  std::filesystem::path out(png_path);
  std::filesystem::current_path(out.parent_path(), ec);
  XELOGI("Ace360X replay: {} -> {} ({} cvar overrides)", trace_path, png_path, cvars.size());
  AceVulkanTraceDump dump;
  dump.Main({"ace360x-replay", trace_path, png_path});
  if (!std::filesystem::exists(out, ec)) {
    return "replay produced no image (see logcat tag xenia)";
  }
  return "";
}

}  // namespace ace360x::host
