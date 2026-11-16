// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// Queries the device's real Vulkan capabilities. Used by the Device screen
// and, later, to gate GPU emulation paths. Reports only what the driver says.
#pragma once

#include <string>

namespace ace360x::host {

// Returns a JSON object describing the first physical device, or
// {"error": "..."} if Vulkan is unavailable. Never throws.
std::string ProbeVulkanAsJson();

}  // namespace ace360x::host
