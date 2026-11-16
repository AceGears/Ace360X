// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// On-device checks that the Xenia core's foundations work on this phone:
// guest address-space reservation and the PowerPC -> ARM64 JIT.
// This proves nothing about game compatibility; it only reports what it ran.
#pragma once

#include <jni.h>

#include <string>

namespace ace360x::host {

// Must be called once on the Android main thread before anything else touches
// the core. Safe to call more than once.
void InitializeCore(JNIEnv* env, int api_level, jobject application_context);

// Runs the self-test and returns a JSON object:
// {"steps":[{"name":..., "ok":bool, "detail":..., "ms":number}], "ok":bool}
std::string RunCoreSelfTestJson();

}  // namespace ace360x::host
