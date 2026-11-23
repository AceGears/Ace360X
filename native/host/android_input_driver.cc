// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
#include "android_input_driver.h"

#include <array>
#include <chrono>
#include <cstring>
#include <mutex>

#include "xenia/base/byte_order.h"
#include "xenia/hid/input.h"
#include "xenia/hid/input_driver.h"
#include "xenia/xbox.h"

namespace ace360x::host {
namespace {

struct Slot {
  PadState state;
  uint32_t packet_number = 0;
  uint16_t vibration_left = 0;
  uint16_t vibration_right = 0;
  // Time of the oldest input change the game hasn't read yet; 0 = none pending.
  uint64_t pending_change_us = 0;
};

uint64_t NowMicros() {
  return static_cast<uint64_t>(std::chrono::duration_cast<std::chrono::microseconds>(
                                   std::chrono::steady_clock::now().time_since_epoch())
                                   .count());
}

std::mutex g_mutex;
std::array<Slot, kMaxPads> g_slots;
InputStats g_input_stats;  // guarded by g_mutex

bool SameInput(const PadState& a, const PadState& b) {
  return a.connected == b.connected && a.buttons == b.buttons &&
         a.left_trigger == b.left_trigger && a.right_trigger == b.right_trigger &&
         a.thumb_lx == b.thumb_lx && a.thumb_ly == b.thumb_ly && a.thumb_rx == b.thumb_rx &&
         a.thumb_ry == b.thumb_ry;
}

}  // namespace

void SetPadState(int user_index, const PadState& state) {
  if (user_index < 0 || user_index >= kMaxPads) return;
  std::lock_guard<std::mutex> lock(g_mutex);
  Slot& slot = g_slots[user_index];
  // XInput semantics: the packet number changes only when the input changes.
  if (!SameInput(slot.state, state)) {
    ++slot.packet_number;
    if (!slot.pending_change_us) slot.pending_change_us = NowMicros();
  }
  slot.state = state;
  if (!state.connected) {
    slot.vibration_left = slot.vibration_right = 0;
  }
}

InputStats DrainInputStats() {
  std::lock_guard<std::mutex> lock(g_mutex);
  InputStats stats = g_input_stats;
  g_input_stats = InputStats();
  return stats;
}

void GetPadVibration(int user_index, uint16_t* left, uint16_t* right) {
  *left = *right = 0;
  if (user_index < 0 || user_index >= kMaxPads) return;
  std::lock_guard<std::mutex> lock(g_mutex);
  *left = g_slots[user_index].vibration_left;
  *right = g_slots[user_index].vibration_right;
}

}  // namespace ace360x::host

namespace xe::hid::android {
namespace {

using ace360x::host::g_input_stats;
using ace360x::host::g_mutex;
using ace360x::host::g_slots;
using ace360x::host::kMaxPads;

void FillGamepad(const ace360x::host::PadState& s, X_INPUT_GAMEPAD* out) {
  out->buttons = s.buttons;
  out->left_trigger = s.left_trigger;
  out->right_trigger = s.right_trigger;
  out->thumb_lx = s.thumb_lx;
  out->thumb_ly = s.thumb_ly;
  out->thumb_rx = s.thumb_rx;
  out->thumb_ry = s.thumb_ry;
}

class AndroidInputDriver final : public InputDriver {
 public:
  AndroidInputDriver(xe::ui::Window* window, size_t window_z_order)
      : InputDriver(window, window_z_order) {}

  X_STATUS Setup() override { return X_STATUS_SUCCESS; }

  X_RESULT GetCapabilities(uint32_t user_index, uint32_t /*flags*/,
                           X_INPUT_CAPABILITIES* out_caps) override {
    if (user_index >= kMaxPads || !out_caps) return X_ERROR_BAD_ARGUMENTS;
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_slots[user_index].state.connected) return X_ERROR_DEVICE_NOT_CONNECTED;
    std::memset(out_caps, 0, sizeof(*out_caps));
    out_caps->type = XINPUT_DEVTYPE_GAMEPAD;
    out_caps->sub_type = XINPUT_DEVSUBTYPE_GAMEPAD;
    out_caps->flags = 0;
    // Report full resolution for every input, like a standard controller.
    out_caps->gamepad.buttons = 0xF7FF;
    out_caps->gamepad.left_trigger = 0xFF;
    out_caps->gamepad.right_trigger = 0xFF;
    out_caps->gamepad.thumb_lx = int16_t(0xFFC0);
    out_caps->gamepad.thumb_ly = int16_t(0xFFC0);
    out_caps->gamepad.thumb_rx = int16_t(0xFFC0);
    out_caps->gamepad.thumb_ry = int16_t(0xFFC0);
    out_caps->vibration.left_motor_speed = 0xFFFF;
    out_caps->vibration.right_motor_speed = 0xFFFF;
    return X_ERROR_SUCCESS;
  }

  X_RESULT GetState(uint32_t user_index, X_INPUT_STATE* out_state) override {
    if (user_index >= kMaxPads) return X_ERROR_BAD_ARGUMENTS;
    std::lock_guard<std::mutex> lock(g_mutex);
    auto& slot = g_slots[user_index];
    if (!slot.state.connected) return X_ERROR_DEVICE_NOT_CONNECTED;
    ++g_input_stats.guest_polls;
    if (slot.pending_change_us) {
      uint64_t now_us = ace360x::host::NowMicros();
      uint64_t waited = now_us > slot.pending_change_us ? now_us - slot.pending_change_us : 0;
      ++g_input_stats.changes_delivered;
      g_input_stats.delivery_sum_us += waited;
      if (waited > g_input_stats.delivery_max_us) g_input_stats.delivery_max_us = waited;
      slot.pending_change_us = 0;
    }
    if (out_state) {
      out_state->packet_number = slot.packet_number;
      FillGamepad(slot.state, &out_state->gamepad);
    }
    return X_ERROR_SUCCESS;
  }

  X_RESULT SetState(uint32_t user_index, X_INPUT_VIBRATION* vibration) override {
    if (user_index >= kMaxPads) return X_ERROR_BAD_ARGUMENTS;
    std::lock_guard<std::mutex> lock(g_mutex);
    auto& slot = g_slots[user_index];
    if (!slot.state.connected) return X_ERROR_DEVICE_NOT_CONNECTED;
    if (vibration) {
      slot.vibration_left = vibration->left_motor_speed;
      slot.vibration_right = vibration->right_motor_speed;
    }
    return X_ERROR_SUCCESS;
  }

  X_RESULT GetKeystroke(uint32_t user_index, uint32_t /*flags*/,
                        X_INPUT_KEYSTROKE* /*out_keystroke*/) override {
    if (user_index >= kMaxPads && user_index != 0xFF) return X_ERROR_BAD_ARGUMENTS;
    // Keystroke events aren't generated yet; games fall back to GetState.
    return X_ERROR_EMPTY;
  }

  InputType GetInputType() const override { return InputType::Controller; }
};

}  // namespace

std::unique_ptr<InputDriver> Create(xe::ui::Window* window, size_t window_z_order) {
  return std::make_unique<AndroidInputDriver>(window, window_z_order);
}

}  // namespace xe::hid::android
