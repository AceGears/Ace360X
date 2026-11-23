# Performance measurement (in-game readout)

Status: **Implemented (unverified on device)**.

## What is measured
- **Frames**: guest swaps — each `PM4_XE_SWAP` the game issues
  (`xenia-upstream/src/xenia/gpu/pm4_command_processor_implement.h`,
  `ExecutePacketType3_XE_SWAP`). Patch 0023 records the interval between consecutive swaps
  with `std::chrono::steady_clock` in a 256-entry ring (`xenia/gpu/ace_frame_stats.h`).
  Host presents (which may repeat a guest frame) are not counted, so nothing interpolated or
  duplicated can inflate the number (CLAUDE.md §20.3).
- **Input delivery**: time from a controller/touch state change arriving in
  `native/host/android_input_driver.cc` (`SetPadState`) to the game's next `XInputGetState`
  read of that pad (`AndroidInputDriver::GetState`). This is the "input → guest delivery"
  budget of §7. It does **not** include the touch screen's own latency, the frames the game
  takes to react, or display latency, so it is a lower bound on what a player feels.

## Derived numbers (`emu/PerfStats.kt`, JVM-tested)
Over the last 600 frames: average FPS, FPS of the slowest 1 % of frames, frame-time standard
deviation. Per 1 s sample: average and maximum input delivery time.

## Not measured yet
- Emulation speed %: with guest VSync on, upstream paces the guest's vertical blank from a
  host timer, so guest time does not visibly slow down; a title's target frame rate is not
  known to the emulator. Reporting a percentage would be a guess.
- Choreographer/display-side pacing and end-to-end latency (needs external measurement).
