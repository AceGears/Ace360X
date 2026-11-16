// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
#include "aaudio_audio_system.h"

#include <aaudio/AAudio.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cstring>
#include <mutex>
#include <queue>
#include <stack>
#include <thread>
#include <vector>

#include "xenia/apu/apu_flags.h"
#include "xenia/apu/audio_driver.h"
#include "xenia/apu/conversion.h"
#include "xenia/apu/nop_audio_driver.h"
#include "xenia/apu/pacing_audio_driver.h"
#include "xenia/base/assert.h"
#include "xenia/base/logging.h"
#include "xenia/base/threading.h"

namespace xe::apu::aaudio {
namespace {

class AAudioDriver final : public AudioDriver {
 public:
  AAudioDriver(xe::threading::Semaphore* semaphore, uint32_t frequency, uint32_t channels,
               bool need_format_conversion)
      : semaphore_(semaphore),
        frequency_(frequency),
        channels_(channels),
        need_format_conversion_(need_format_conversion) {
    channel_samples_ = channels_ == 6 ? 256 : (channels_ == 2 ? 768 : 0);
    frame_floats_ = channels_ * channel_samples_;
    // 5.1 input is always downmixed to stereo; other layouts are passed through.
    output_channels_ = need_format_conversion_ ? 2 : channels_;
    output_frame_.resize(static_cast<size_t>(output_channels_) * channel_samples_);
  }

  ~AAudioDriver() override { Shutdown(); }

  bool Initialize() override {
    if (!channel_samples_ || frame_floats_ > kFrameSamplesMax ||
        (need_format_conversion_ && channels_ != 6)) {
      XELOGE("AAudio: unsupported guest format ({} channels)", channels_);
      return false;
    }
    return OpenStream();
  }

  void Shutdown() override {
    shutting_down_.store(true);
    if (reopen_thread_.joinable()) reopen_thread_.join();
    CloseStream();
    std::lock_guard<std::mutex> lock(frames_mutex_);
    while (!queued_.empty()) queued_.pop();
    while (!unused_.empty()) unused_.pop();
    storage_.clear();
  }

  void SubmitFrame(float* samples) override {
    std::lock_guard<std::mutex> lock(frames_mutex_);
    float* frame;
    if (unused_.empty()) {
      storage_.emplace_back(std::make_unique<float[]>(frame_floats_));
      frame = storage_.back().get();
    } else {
      frame = unused_.top();
      unused_.pop();
    }
    std::memcpy(frame, samples, sizeof(float) * frame_floats_);
    queued_.push(frame);
  }

  void Pause() override {
    paused_.store(true);
    std::lock_guard<std::mutex> lock(stream_mutex_);
    if (stream_) AAudioStream_requestPause(stream_);
  }

  void Resume() override {
    paused_.store(false);
    std::lock_guard<std::mutex> lock(stream_mutex_);
    if (stream_) AAudioStream_requestStart(stream_);
  }

  void SetVolume(float volume) override { volume_.store(std::clamp(volume, 0.0f, 1.0f)); }

 private:
  bool OpenStream() {
    AAudioStreamBuilder* builder = nullptr;
    if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK) return false;
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setChannelCount(builder, static_cast<int32_t>(output_channels_));
    AAudioStreamBuilder_setSampleRate(builder, static_cast<int32_t>(frequency_));
    AAudioStreamBuilder_setUsage(builder, AAUDIO_USAGE_GAME);
    AAudioStreamBuilder_setContentType(builder, AAUDIO_CONTENT_TYPE_MUSIC);
    AAudioStreamBuilder_setDataCallback(builder, &AAudioDriver::DataCallback, this);
    AAudioStreamBuilder_setErrorCallback(builder, &AAudioDriver::ErrorCallback, this);

    AAudioStream* stream = nullptr;
    aaudio_result_t r = AAudioStreamBuilder_openStream(builder, &stream);
    AAudioStreamBuilder_delete(builder);
    if (r != AAUDIO_OK) {
      XELOGE("AAudio: openStream failed: {}", AAudio_convertResultToText(r));
      return false;
    }
    if (AAudioStream_getSampleRate(stream) != static_cast<int32_t>(frequency_) ||
        AAudioStream_getChannelCount(stream) != static_cast<int32_t>(output_channels_)) {
      // We don't resample; a shared-mode stream normally converts for us.
      XELOGW("AAudio: device format {} Hz / {} ch differs from requested {} Hz / {} ch",
             AAudioStream_getSampleRate(stream), AAudioStream_getChannelCount(stream), frequency_,
             output_channels_);
    }
    // Two bursts of buffering: low latency without constant underruns.
    AAudioStream_setBufferSizeInFrames(stream, AAudioStream_getFramesPerBurst(stream) * 2);
    {
      std::lock_guard<std::mutex> lock(stream_mutex_);
      stream_ = stream;
    }
    if (!paused_.load()) AAudioStream_requestStart(stream);
    XELOGI("AAudio: stream open, {} Hz, {} ch, burst {} frames", frequency_, output_channels_,
           AAudioStream_getFramesPerBurst(stream));
    return true;
  }

  void CloseStream() {
    AAudioStream* stream;
    {
      std::lock_guard<std::mutex> lock(stream_mutex_);
      stream = stream_;
      stream_ = nullptr;
    }
    if (stream) {
      AAudioStream_requestStop(stream);
      AAudioStream_close(stream);
    }
  }

  // Device callback: fill num_frames (host sample frames) from guest frames.
  static aaudio_data_callback_result_t DataCallback(AAudioStream* /*stream*/, void* user,
                                                    void* audio_data, int32_t num_frames) {
    static_cast<AAudioDriver*>(user)->Fill(static_cast<float*>(audio_data), num_frames);
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
  }

  void Fill(float* out, int32_t num_frames) {
    const uint32_t ch = output_channels_;
    const float gain = cvars::mute ? 0.0f : volume_.load();
    int32_t written = 0;
    while (written < num_frames) {
      if (read_pos_ >= channel_samples_) {
        // Need the next guest frame.
        float* frame = nullptr;
        {
          std::lock_guard<std::mutex> lock(frames_mutex_);
          if (!queued_.empty()) {
            frame = queued_.front();
            queued_.pop();
          }
        }
        if (!frame) {
          // Underrun: silence for the rest; don't release (nothing was played).
          std::memset(out + size_t(written) * ch, 0, sizeof(float) * size_t(num_frames - written) * ch);
          return;
        }
        if (need_format_conversion_) {
          conversion::sequential_6_BE_to_interleaved_2_LE(output_frame_.data(), frame,
                                                          channel_samples_);
        } else {
          std::memcpy(output_frame_.data(), frame, sizeof(float) * frame_floats_);
        }
        {
          std::lock_guard<std::mutex> lock(frames_mutex_);
          unused_.push(frame);
        }
        read_pos_ = 0;
        // One guest frame consumed: let the guest produce another.
        semaphore_->Release(1, nullptr);
      }
      const int32_t take =
          std::min<int32_t>(num_frames - written, int32_t(channel_samples_ - read_pos_));
      const float* src = output_frame_.data() + size_t(read_pos_) * ch;
      float* dst = out + size_t(written) * ch;
      for (int32_t i = 0; i < take * int32_t(ch); ++i) dst[i] = src[i] * gain;
      written += take;
      read_pos_ += uint32_t(take);
    }
  }

  // Called e.g. when headphones are unplugged or Bluetooth changes. AAudio
  // requires the stream to be reopened from another thread.
  static void ErrorCallback(AAudioStream* /*stream*/, void* user, aaudio_result_t error) {
    auto* self = static_cast<AAudioDriver*>(user);
    if (error != AAUDIO_ERROR_DISCONNECTED || self->shutting_down_.load()) return;
    if (self->reopening_.exchange(true)) return;
    if (self->reopen_thread_.joinable()) self->reopen_thread_.detach();
    self->reopen_thread_ = std::thread([self] {
      self->CloseStream();
      if (!self->shutting_down_.load() && !self->OpenStream()) {
        XELOGE("AAudio: could not reopen the stream after a device change");
      }
      self->reopening_.store(false);
    });
  }

  xe::threading::Semaphore* semaphore_;
  uint32_t frequency_;
  uint32_t channels_;
  bool need_format_conversion_;
  uint32_t channel_samples_ = 0;
  uint32_t frame_floats_ = 0;
  uint32_t output_channels_ = 2;

  std::mutex stream_mutex_;
  AAudioStream* stream_ = nullptr;

  std::mutex frames_mutex_;
  std::vector<std::unique_ptr<float[]>> storage_;
  std::queue<float*> queued_;
  std::stack<float*> unused_;

  // Callback-thread only.
  std::vector<float> output_frame_;
  uint32_t read_pos_ = UINT32_MAX;

  std::atomic<float> volume_{1.0f};
  std::atomic<bool> paused_{false};
  std::atomic<bool> shutting_down_{false};
  std::atomic<bool> reopening_{false};
  std::thread reopen_thread_;
};

}  // namespace

AAudioAudioSystem::AAudioAudioSystem(cpu::Processor* processor) : AudioSystem(processor) {}
AAudioAudioSystem::~AAudioAudioSystem() = default;

std::unique_ptr<AudioSystem> AAudioAudioSystem::Create(cpu::Processor* processor) {
  return std::make_unique<AAudioAudioSystem>(processor);
}

X_STATUS AAudioAudioSystem::CreateDriver(size_t /*index*/, xe::threading::Semaphore* semaphore,
                                         AudioDriver** out_driver) {
  assert_not_null(out_driver);
  auto driver = std::make_unique<AAudioDriver>(semaphore, AudioDriver::kFrameFrequencyDefault,
                                               AudioDriver::kFrameChannelsDefault, true);
  if (!driver->Initialize()) {
    // No usable output: keep the game running silently, as the nop system does.
    XELOGW("AAudio: falling back to silent output");
    driver->Shutdown();
    *out_driver = new nop::NopAudioDriver(semaphore);
    return X_STATUS_SUCCESS;
  }
  *out_driver = driver.release();
  return X_STATUS_SUCCESS;
}

AudioDriver* AAudioAudioSystem::CreateDriver(xe::threading::Semaphore* semaphore,
                                             uint32_t frequency, uint32_t channels,
                                             bool need_format_conversion) {
  auto driver = std::make_unique<AAudioDriver>(semaphore, frequency, channels,
                                               need_format_conversion);
  if (!driver->Initialize()) {
    driver->Shutdown();
    if (!channels || channels > AudioDriver::kFrameSamplesMax) return nullptr;
    return new PacingAudioDriver(semaphore, frequency, AudioDriver::kFrameSamplesMax / channels);
  }
  return driver.release();
}

}  // namespace xe::apu::aaudio
