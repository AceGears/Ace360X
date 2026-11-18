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
#include "android_input_driver.h"
#include "core_selftest.h"
#include "saf_tree_provider.h"
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

// Controller state from Kotlin (EmulatorActivity). Values are already in
// XInput ranges; see GamepadMapper.kt.
extern "C" JNIEXPORT void JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeSetPadState(
    JNIEnv* /*env*/, jobject /*thiz*/, jint user, jboolean connected, jint buttons, jint lt, jint rt,
    jint lx, jint ly, jint rx, jint ry) {
#if ACE_WITH_XENIA
  ace360x::host::PadState s;
  s.connected = connected == JNI_TRUE;
  s.buttons = static_cast<uint16_t>(buttons & 0xFFFF);
  s.left_trigger = static_cast<uint8_t>(lt < 0 ? 0 : (lt > 255 ? 255 : lt));
  s.right_trigger = static_cast<uint8_t>(rt < 0 ? 0 : (rt > 255 ? 255 : rt));
  auto clamp16 = [](jint v) {
    return static_cast<int16_t>(v < -32768 ? -32768 : (v > 32767 ? 32767 : v));
  };
  s.thumb_lx = clamp16(lx);
  s.thumb_ly = clamp16(ly);
  s.thumb_rx = clamp16(rx);
  s.thumb_ry = clamp16(ry);
  ace360x::host::SetPadState(user, s);
#else
  (void)user; (void)connected; (void)buttons; (void)lt; (void)rt;
  (void)lx; (void)ly; (void)rx; (void)ry;
#endif
}

// Returns (left << 16) | right motor speed requested by the game.
extern "C" JNIEXPORT jint JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeGetPadVibration(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                  jint user) {
#if ACE_WITH_XENIA
  uint16_t left = 0, right = 0;
  ace360x::host::GetPadVibration(user, &left, &right);
  return static_cast<jint>((static_cast<uint32_t>(left) << 16) | right);
#else
  (void)user;
  return 0;
#endif
}

// Registers the acetree:// provider (Kotlin SafTree) with Xenia's filesystem.
extern "C" JNIEXPORT jboolean JNICALL
Java_com_acegears_ace360x_core_NativeBridge_nativeInstallSafTree(JNIEnv* env, jobject /*thiz*/,
                                                                 jclass saf_tree_class) {
#if ACE_WITH_XENIA
  try {
    return ace360x::host::InstallSafTreeProvider(env, saf_tree_class) ? JNI_TRUE : JNI_FALSE;
  } catch (...) {
    LOGE("InstallSafTreeProvider threw");
    return JNI_FALSE;
  }
#else
  (void)env;
  (void)saf_tree_class;
  return JNI_FALSE;
#endif
}
