// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// "acetree://" external-tree provider for Xenia's filesystem layer (patch 0008),
// backed by Android Storage Access Framework folders through the Kotlin object
// com.acegears.ace360x.emu.SafTree. Lets extracted games (a folder containing
// default.xex) and Games on Demand titles mount without storage permissions.
#pragma once

#include <jni.h>

namespace ace360x::host {

// Call on a Java thread (it caches the class and method IDs, because native
// emulator threads can't look up app classes). Returns false if the Kotlin
// methods are missing. Safe to call more than once.
bool InstallSafTreeProvider(JNIEnv* env, jclass saf_tree_class);

}  // namespace ace360x::host
