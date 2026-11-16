// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
// Xenia apu::AudioSystem that outputs through Android AAudio.
// Behaviour mirrors upstream's SDL backend (xenia/apu/sdl/sdl_audio_driver.cc):
// guest frames are queued, converted (5.1 big-endian sequential → stereo
// little-endian interleaved) in the device callback, and the guest's audio
// semaphore is released once per frame actually played, which paces the game.
#pragma once

#include <memory>
#include <string>

#include "xenia/apu/audio_system.h"

namespace xe::apu::aaudio {

class AAudioAudioSystem : public AudioSystem {
 public:
  explicit AAudioAudioSystem(cpu::Processor* processor);
  ~AAudioAudioSystem() override;

  static bool IsAvailable() { return true; }
  static std::unique_ptr<AudioSystem> Create(cpu::Processor* processor);

  std::string name() const override { return "AAudio"; }

  X_STATUS CreateDriver(size_t index, xe::threading::Semaphore* semaphore,
                        AudioDriver** out_driver) override;
  AudioDriver* CreateDriver(xe::threading::Semaphore* semaphore, uint32_t frequency,
                            uint32_t channels, bool need_format_conversion) override;
};

}  // namespace xe::apu::aaudio
