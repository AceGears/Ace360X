// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// Xenia hid::InputDriver backed by controller state pushed from Kotlin
// (EmulatorActivity → NativeBridge.setPadState). Up to 4 players.
#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>

namespace xe::ui {
class Window;
}
namespace xe::hid {
class InputDriver;
}

namespace ace360x::host {

constexpr int kMaxPads = 4;

struct PadState {
  bool connected = false;
  uint16_t buttons = 0;  // X_INPUT_GAMEPAD_* bits
  uint8_t left_trigger = 0;
  uint8_t right_trigger = 0;
  int16_t thumb_lx = 0, thumb_ly = 0, thumb_rx = 0, thumb_ry = 0;
};

// Thread-safe; called from the Android UI thread.
void SetPadState(int user_index, const PadState& state);
// Last rumble request from the game for a pad, 0..65535 per motor.
void GetPadVibration(int user_index, uint16_t* left, uint16_t* right);

}  // namespace ace360x::host

namespace xe::hid::android {
std::unique_ptr<xe::hid::InputDriver> Create(xe::ui::Window* window, size_t window_z_order);
}  // namespace xe::hid::android
