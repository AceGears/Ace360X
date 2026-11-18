// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
#include "saf_tree_provider.h"

#include <cstdint>
#include <mutex>
#include <optional>
#include <string>
#include <vector>

#include "xenia/base/filesystem.h"
#include "xenia/base/logging.h"
#include "xenia/base/main_android.h"

namespace ace360x::host {
namespace {

using xe::filesystem::FileInfo;

std::mutex g_install_mutex;
jclass g_class = nullptr;  // global ref
jmethodID g_stat = nullptr;
jmethodID g_list = nullptr;
jmethodID g_open = nullptr;

// Unix milliseconds → Windows FILETIME (100 ns ticks since 1601).
uint64_t MillisToFiletime(int64_t millis) {
  if (millis <= 0) return 0;
  return uint64_t(millis) * 10000ull + 116444736000000000ull;
}

// Entry format produced by SafTree.kt: "d|f\t<size>\t<mtimeMillis>\t<name>".
std::optional<FileInfo> ParseEntry(const std::string& line, const std::filesystem::path& parent) {
  size_t a = line.find('\t');
  if (a == std::string::npos) return std::nullopt;
  size_t b = line.find('\t', a + 1);
  if (b == std::string::npos) return std::nullopt;
  size_t c = line.find('\t', b + 1);
  if (c == std::string::npos) return std::nullopt;
  FileInfo info{};
  info.type = line.compare(0, a, "d") == 0 ? FileInfo::Type::kDirectory : FileInfo::Type::kFile;
  int64_t size = 0, mtime = 0;
  try {
    size = std::stoll(line.substr(a + 1, b - a - 1));
    mtime = std::stoll(line.substr(b + 1, c - b - 1));
  } catch (...) {
    return std::nullopt;
  }
  info.total_size = info.type == FileInfo::Type::kFile && size > 0 ? size_t(size) : 0;
  info.create_timestamp = info.access_timestamp = info.write_timestamp = MillisToFiletime(mtime);
  info.name = line.substr(c + 1);
  info.path = parent;
  return info;
}

// Returns a UTF-8 std::string from a jstring (handles nullptr).
std::string ToStd(JNIEnv* env, jstring s) {
  if (!s) return {};
  const char* chars = env->GetStringUTFChars(s, nullptr);
  std::string out = chars ? chars : "";
  if (chars) env->ReleaseStringUTFChars(s, chars);
  return out;
}

bool ClearException(JNIEnv* env) {
  if (!env->ExceptionCheck()) return false;
  env->ExceptionDescribe();
  env->ExceptionClear();
  return true;
}

std::optional<FileInfo> GetInfo(const std::filesystem::path& path) {
  JNIEnv* env = xe::GetAndroidThreadJniEnv();
  if (!env || !g_class) return std::nullopt;
  jstring jpath = env->NewStringUTF(path.string().c_str());
  auto result = static_cast<jstring>(env->CallStaticObjectMethod(g_class, g_stat, jpath));
  env->DeleteLocalRef(jpath);
  if (ClearException(env) || !result) return std::nullopt;
  std::string line = ToStd(env, result);
  env->DeleteLocalRef(result);
  return ParseEntry(line, path.parent_path());
}

std::vector<FileInfo> ListFiles(const std::filesystem::path& path) {
  std::vector<FileInfo> out;
  JNIEnv* env = xe::GetAndroidThreadJniEnv();
  if (!env || !g_class) return out;
  jstring jpath = env->NewStringUTF(path.string().c_str());
  auto arr = static_cast<jobjectArray>(env->CallStaticObjectMethod(g_class, g_list, jpath));
  env->DeleteLocalRef(jpath);
  if (ClearException(env) || !arr) return out;
  const jsize n = env->GetArrayLength(arr);
  out.reserve(size_t(n));
  for (jsize i = 0; i < n; ++i) {
    auto item = static_cast<jstring>(env->GetObjectArrayElement(arr, i));
    if (auto info = ParseEntry(ToStd(env, item), path)) out.push_back(std::move(*info));
    env->DeleteLocalRef(item);
  }
  env->DeleteLocalRef(arr);
  return out;
}

int OpenFd(const std::filesystem::path& path, bool write) {
  JNIEnv* env = xe::GetAndroidThreadJniEnv();
  if (!env || !g_class) return -1;
  jstring jpath = env->NewStringUTF(path.string().c_str());
  jint fd = env->CallStaticIntMethod(g_class, g_open, jpath, write ? JNI_TRUE : JNI_FALSE);
  env->DeleteLocalRef(jpath);
  if (ClearException(env)) return -1;
  return fd;
}

const xe::filesystem::ExternalTreeProvider kProvider = {
    "acetree",
    &GetInfo,
    &ListFiles,
    &OpenFd,
};

}  // namespace

bool InstallSafTreeProvider(JNIEnv* env, jclass saf_tree_class) {
  std::lock_guard<std::mutex> lock(g_install_mutex);
  if (g_class) return true;
  jmethodID stat = env->GetStaticMethodID(saf_tree_class, "stat", "(Ljava/lang/String;)Ljava/lang/String;");
  jmethodID list = env->GetStaticMethodID(saf_tree_class, "list", "(Ljava/lang/String;)[Ljava/lang/String;");
  jmethodID open = env->GetStaticMethodID(saf_tree_class, "openFd", "(Ljava/lang/String;Z)I");
  if (ClearException(env) || !stat || !list || !open) {
    XELOGE("SafTree: Kotlin methods not found; extracted games can't be opened");
    return false;
  }
  g_class = static_cast<jclass>(env->NewGlobalRef(saf_tree_class));
  g_stat = stat;
  g_list = list;
  g_open = open;
  xe::filesystem::SetExternalTreeProvider(&kProvider);
  return true;
}

}  // namespace ace360x::host
