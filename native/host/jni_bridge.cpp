// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// The ONLY Kotlin <-> C++ crossing point. Every entry point catches all C++
// exceptions so nothing unwinds through JNI.
#include <android/log.h>
#include <jni.h>

#include <exception>
#include <string>

#include "fd_byte_source.h"
#include "format_sniffer.h"
#include "vulkan_probe.h"

#if ACE_WITH_XENIA
#include "core_selftest.h"
#endif

#define LOG_TAG "Ace360XNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

jstring ToJString(JNIEnv* env, const std::string& s) {
  // All strings we build are ASCII/UTF-8 JSON; NewStringUTF is safe for them.
  return env->NewStringUTF(s.c_str());
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeProbeVulkan(JNIEnv* env, jobject /*thiz*/) {
  try {
    return ToJString(env, ace360x::host::ProbeVulkanAsJson());
  } catch (const std::exception& e) {
    LOGE("ProbeVulkan threw: %s", e.what());
  } catch (...) {
    LOGE("ProbeVulkan threw unknown exception");
  }
  return ToJString(env, "{\"error\":\"internal error while probing Vulkan\"}");
}

// Returns the GameFormat enum value, or -1 if the fd could not be read.
extern "C" JNIEXPORT jint JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeSniffFormat(JNIEnv* /*env*/, jobject /*thiz*/,
                                                             jint fd) {
  try {
    ace360x::host::FdByteSource source(fd);
    if (source.Size() < 0) return -1;
    return static_cast<jint>(ace360x::host::SniffGameFormat(source).format);
  } catch (...) {
    LOGE("SniffFormat threw");
    return -1;
  }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeHasCore(JNIEnv* /*env*/, jobject /*thiz*/) {
#if ACE_WITH_XENIA
  return JNI_TRUE;
#else
  return JNI_FALSE;
#endif
}

// Must be called on the main thread (Xenia's Android init requirement).
extern "C" JNIEXPORT void JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeInitCore(JNIEnv* env, jobject /*thiz*/,
                                                           jint api_level, jobject context) {
#if ACE_WITH_XENIA
  try {
    ace360x::host::InitializeCore(env, api_level, context);
  } catch (...) {
    LOGE("InitializeCore threw");
  }
#else
  (void)env;
  (void)api_level;
  (void)context;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeRunCoreSelfTest(JNIEnv* env, jobject /*thiz*/) {
#if ACE_WITH_XENIA
  try {
    return ToJString(env, ace360x::host::RunCoreSelfTestJson());
  } catch (const std::exception& e) {
    LOGE("Core self-test threw: %s", e.what());
    std::string what = e.what();
    for (char& c : what) {
      if (c == '"' || c == '\\' || static_cast<unsigned char>(c) < 0x20) c = ' ';
    }
    return ToJString(env, "{\"error\":\"exception: " + what + "\"}");
  } catch (...) {
    LOGE("Core self-test threw unknown exception");
  }
  return ToJString(env, "{\"error\":\"unknown exception\"}");
#else
  return ToJString(env, "{\"error\":\"This build does not include the emulation core\"}");
#endif
}
