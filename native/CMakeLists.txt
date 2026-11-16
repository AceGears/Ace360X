cmake_minimum_required(VERSION 3.22)
project(ace360x_native CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(ACE_WARNINGS -Wall -Wextra -Wpedantic -Werror)

# Platform-independent host code (compiled for Android and for host tests).
add_library(ace_host_common STATIC host/format_sniffer.cpp)
target_include_directories(ace_host_common PUBLIC host)
target_compile_options(ace_host_common PRIVATE ${ACE_WARNINGS})

if(ANDROID)
  # Shared library loaded by NativeBridge.kt
  add_library(ace360x SHARED host/jni_bridge.cpp host/vulkan_probe.cpp)
  target_compile_options(ace360x PRIVATE ${ACE_WARNINGS})
  target_link_libraries(ace360x PRIVATE ace_host_common vulkan log)
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
