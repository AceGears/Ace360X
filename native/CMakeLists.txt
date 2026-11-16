cmake_minimum_required(VERSION 3.22)
project(ace360x_native C CXX ASM)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(ACE_WARNINGS -Wall -Wextra -Wpedantic -Werror)

# Platform-independent host code (compiled for Android and for host tests).
add_library(ace_host_common STATIC host/format_sniffer.cpp)
target_include_directories(ace_host_common PUBLIC host)
set_target_properties(ace_host_common PROPERTIES CXX_EXTENSIONS OFF)
target_compile_options(ace_host_common PRIVATE ${ACE_WARNINGS})

if(ANDROID)
  # ---- Xenia Canary core (upstream CMake project + our patches) ----
  option(ACE_WITH_XENIA "Build and link the Xenia Canary core" ON)
  if(ACE_WITH_XENIA)
    include(cmake/ApplyXeniaPatches.cmake)
    include(cmake/XeniaVersion.cmake)
    set(XENIA_DISABLE_WERROR ON)  # see native/patches/README.md
    # Everything is linked into one .so: keep core symbols hidden (only JNIEXPORT
    # functions are exported). Required by FFmpeg's NEON assembly, which uses
    # PC-relative references to its C tables, and keeps the library smaller.
    set(CMAKE_C_VISIBILITY_PRESET hidden)
    set(CMAKE_CXX_VISIBILITY_PRESET hidden)
    set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)
    # Upstream adds -Wno-* flags for newer desktop clang that NDK clang lacks.
    string(APPEND CMAKE_C_FLAGS " -Wno-unknown-warning-option")
    string(APPEND CMAKE_CXX_FLAGS " -Wno-unknown-warning-option")
    add_subdirectory(xenia-upstream xenia EXCLUDE_FROM_ALL)
    set(ACE_XENIA_LIBS
      xenia-core xenia-cpu xenia-cpu-backend-a64 xenia-apu xenia-apu-nop
      xenia-gpu xenia-gpu-null xenia-gpu-vulkan xenia-hid xenia-hid-nop
      xenia-kernel xenia-vfs xenia-ui xenia-ui-vulkan xenia-patcher xenia-base
      # Third-party libraries, mirroring upstream's xenia-app link list
      # (upstream libraries don't declare all of these themselves).
      aes_128 capstone fmt dxbc imgui libavcodec libavutil mspack snappy xxhash
      glslang-spirv xbyak_aarch64 zstd zarchive zlib-ng pugixml
      # Needed by upstream's app layer (xenia_main / emulator_window).
      xenia-debug-ui xenia-hid-keyboard xenia-app-discord discord-rpc)

    # discord-rpc only gets its POSIX sources on desktop Linux upstream. Discord
    # presence is disabled on Android (EmulatorActivity passes discord=false),
    # but the code must still link.
    target_sources(discord-rpc PRIVATE
      ${CMAKE_CURRENT_SOURCE_DIR}/xenia-upstream/third_party/discord-rpc/src/connection_unix.cpp
      ${CMAKE_CURRENT_SOURCE_DIR}/xenia-upstream/third_party/discord-rpc/src/discord_register_linux.cpp)

    # Upstream's emulator app (EmulatorApp, EmulatorWindow, dialogs), compiled
    # with exactly the definitions/options upstream uses for its own targets:
    # some (e.g. IMGUI_USE_WCHAR32) change struct layouts and must match.
    set(_xe "${CMAKE_CURRENT_SOURCE_DIR}/xenia-upstream")
    get_directory_property(_xe_defs DIRECTORY xenia-upstream COMPILE_DEFINITIONS)
    get_directory_property(_xe_opts DIRECTORY xenia-upstream COMPILE_OPTIONS)
    add_library(ace_xenia_app OBJECT
      ${_xe}/src/xenia/app/xenia_main.cc
      ${_xe}/src/xenia/app/emulator_window.cc
      ${_xe}/src/xenia/app/console_settings_dialog.cc
      ${_xe}/src/xenia/app/content_list_dialog.cc
      ${_xe}/src/xenia/app/profile_dialogs.cc
      # Ours, but built with upstream's flags since it implements a Xenia interface.
      ${CMAKE_CURRENT_SOURCE_DIR}/host/android_input_driver.cc
      ${CMAKE_CURRENT_SOURCE_DIR}/host/aaudio_audio_system.cc)
    target_compile_definitions(ace_xenia_app PRIVATE ${_xe_defs}
      XBYAK_NO_OP_NAMES XBYAK_ENABLE_OMITTED_OPERAND)
    target_compile_options(ace_xenia_app PRIVATE ${_xe_opts})
    target_include_directories(ace_xenia_app PRIVATE
      ${_xe} ${_xe}/src ${_xe}/third_party ${CMAKE_BINARY_DIR}
      ${CMAKE_CURRENT_SOURCE_DIR}/host)
    target_link_libraries(ace_xenia_app PRIVATE ${ACE_XENIA_LIBS})
    list(APPEND ACE_XENIA_LIBS ace_xenia_app)

    # JNI functions defined by upstream (windowed_app_context_android.cc) for
    # jp.xenia.emulator.WindowedAppActivity live in a static archive and are
    # not referenced from C++, so force the linker to keep them.
    set(ACE_XENIA_JNI_SYMBOLS
      Java_jp_xenia_emulator_WindowedAppActivity_initializeWindowedAppOnCreate
      Java_jp_xenia_emulator_WindowedAppActivity_onDestroyNative
      Java_jp_xenia_emulator_WindowedAppActivity_onWindowSurfaceLayoutChange
      Java_jp_xenia_emulator_WindowedAppActivity_onWindowSurfaceMotionEvent
      Java_jp_xenia_emulator_WindowedAppActivity_onWindowSurfaceChanged
      Java_jp_xenia_emulator_WindowedAppActivity_paintWindow)
  endif()

  # Shared library loaded by NativeBridge.kt
  add_library(ace360x SHARED host/jni_bridge.cpp host/vulkan_probe.cpp)
  target_compile_options(ace360x PRIVATE ${ACE_WARNINGS})
  set_target_properties(ace360x PROPERTIES CXX_EXTENSIONS OFF)
  target_link_libraries(ace360x PRIVATE ace_host_common vulkan log android aaudio ${ACE_XENIA_LIBS})
  if(ACE_WITH_XENIA)
    foreach(_sym IN LISTS ACE_XENIA_JNI_SYMBOLS)
      target_link_options(ace360x PRIVATE "-Wl,-u,${_sym}")
    endforeach()
    target_sources(ace360x PRIVATE host/core_selftest.cpp)
    target_compile_definitions(ace360x PRIVATE ACE_WITH_XENIA=1)
    # Upstream headers as SYSTEM so our -Werror applies to our code only.
    target_include_directories(ace360x SYSTEM PRIVATE
      xenia-upstream xenia-upstream/src xenia-upstream/third_party
      xenia-upstream/third_party/fmt/include ${CMAKE_BINARY_DIR})
  endif()
  # 16 KB page-size compatibility (required for new Play submissions).
  target_link_options(ace360x PRIVATE "-Wl,-z,max-page-size=16384")
else()
  enable_testing()
  add_executable(format_sniffer_test tests/format_sniffer_test.cpp)
  target_link_libraries(format_sniffer_test PRIVATE ace_host_common)
  target_compile_options(format_sniffer_test PRIVATE ${ACE_WARNINGS})
  add_test(NAME format_sniffer_test COMMAND format_sniffer_test)

  option(ACE_BUILD_FUZZERS "Build libFuzzer targets (requires clang)" OFF)
  if(ACE_BUILD_FUZZERS)
    # Compiles the sources directly so sanitizer flags don't leak into other targets.
    add_executable(format_sniffer_fuzzer tests/format_sniffer_fuzzer.cpp host/format_sniffer.cpp)
    target_include_directories(format_sniffer_fuzzer PRIVATE host)
    target_compile_options(format_sniffer_fuzzer PRIVATE -fsanitize=fuzzer,address,undefined)
    target_link_options(format_sniffer_fuzzer PRIVATE -fsanitize=fuzzer,address,undefined)
  endif()
endif()
