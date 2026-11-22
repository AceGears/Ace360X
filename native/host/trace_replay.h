// Replays a captured GPU frame (.xtr) on this device's Vulkan driver and writes
// the guest output as a PNG, using upstream's TraceDump. Used to bisect device
// graphics problems: the same capture is replayed with different GPU cvars.
#pragma once

#include <string>
#include <utility>
#include <vector>

namespace ace360x::host {

// Sets each cvar (TOML literal value, e.g. "false", "\"fbo\"", "2") for this
// replay, replays `trace_path` and writes `png_path`. Previously set overrides
// are reset to defaults first. Returns "" on success or an error message.
std::string ReplayTrace(const std::string& trace_path, const std::string& png_path,
                        const std::vector<std::pair<std::string, std::string>>& cvars);

}  // namespace ace360x::host
