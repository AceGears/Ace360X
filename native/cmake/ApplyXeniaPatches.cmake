# Applies native/patches/*.patch to the xenia-upstream submodule working tree.
# Idempotent: a patch that is already applied (reverse-applies cleanly) is
# skipped. Any patch that neither applies nor is already applied stops the
# configure step with a clear message, so upstream bumps fail loudly.

find_package(Git REQUIRED)
set(_up "${CMAKE_CURRENT_LIST_DIR}/../xenia-upstream")
file(GLOB _patches "${CMAKE_CURRENT_LIST_DIR}/../patches/*.patch")
list(SORT _patches)

if(NOT EXISTS "${_up}/CMakeLists.txt")
  message(FATAL_ERROR
    "xenia-upstream is missing. Run scripts/fetch-xenia-deps.sh first.")
endif()

foreach(_p IN LISTS _patches)
  get_filename_component(_name "${_p}" NAME)
  execute_process(
    COMMAND "${GIT_EXECUTABLE}" apply --reverse --check "${_p}"
    WORKING_DIRECTORY "${_up}" RESULT_VARIABLE _already OUTPUT_QUIET ERROR_QUIET)
  if(_already EQUAL 0)
    message(STATUS "Xenia patch already applied: ${_name}")
    continue()
  endif()
  execute_process(
    COMMAND "${GIT_EXECUTABLE}" apply --whitespace=nowarn "${_p}"
    WORKING_DIRECTORY "${_up}" RESULT_VARIABLE _rc ERROR_VARIABLE _err)
  if(NOT _rc EQUAL 0)
    message(FATAL_ERROR "Xenia patch failed to apply: ${_name}\n${_err}")
  endif()
  message(STATUS "Xenia patch applied: ${_name}")
endforeach()
