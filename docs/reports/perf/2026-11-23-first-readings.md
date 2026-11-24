# First on-device performance readings (spot values, not a sustained test)

- Device: Samsung SM-S948U1 (Snapdragon SM8850, Adreno 840), Android 17, Qualcomm driver 07/01/26
- Build: 77eb7c4 or b7976e6 (debug APK; native code built Release), internal resolution 1×,
  default settings, on-screen touch controls
- Source: the in-game readout (`emu/PerfStats.kt`: guest swaps over the last 600 frames;
  "1 % low" = FPS of the slowest 1 % of those frames), read off 13 tester screenshots.
  Durations per scene are unknown; **these are instantaneous readings, not averages over a
  timed run**, and no 15/30/60-minute test exists yet (CLAUDE.md §11).

| Title | Scene | Avg FPS | 1 % low | Frame-time stdev | Input → game |
|---|---|---|---|---|---|
| 55530825 | Intro logos (video) | 30.0 | 23.0–23.3 | 1.9–2.1 ms | — |
| 55530825 | Loading (black) | 28.9 | 5.0 | 32.5 ms | — |
| 55530825 | Title / main menu | 25.5–25.6 | 1.7 | 72.7 ms | 5 ms (max 8) |
| 55530825 | Dojo, shortly after loading | 31.2–33.0 | 2.1–2.5 | 70.6–72.6 ms | one reading of 1609 ms during a stall |
| 55530825 | Dojo, later | 59.3 | 40.9 | 1.4 ms | 5 ms (max 15) |
| 555307E5 | Intro (video) | 30.2 | 24.4 | 2.5 ms | — |
| 555307E5 | Title screen | 30.0 | 25.0 | 1.5 ms | 14 ms (max 26) |
| 555307E5 | Fight | 27.6–29.6 | 1.9 | 57.4–57.8 ms | 35 ms (max 50) |

## What the readings show
- **Input delivery inside the app is within budget** (§7: < 1 frame): 5–35 ms, i.e. the
  game reads the pad on its next frame. The "slight lag" reported earlier is therefore not
  in our input path. The one 1.6 s reading coincides with a stall (the game did not read
  input while it was stuck).
- **Stalls**: a 1 % low of 1.7–2.5 FPS means single frames of roughly 400–600 ms within the
  last 600 frames. They appear after loading and in new scenes and are absent once a scene
  has been running (dojo: 59.3 FPS, 1 % low 40.9). That pattern fits shader/pipeline
  compilation on first use; **not verified** (no log was captured with these readings).
- **Heat**: the tester reports the phone gets hot quickly; Android's "moderate" thermal
  status notice is visible in five screenshots. No temperature or headroom values exist yet.
- Graphics: 55530825 still shows black character parts and dark buildings (per-game
  "Missing graphics fix" was off in the tester's screenshots, so that test is still open).
  555307E5 renders correctly in these screenshots.

## Next
Build after 77eb7c4 records a per-session summary (duration, average FPS, stalls ≥ 100 ms,
input delivery, thermal status and headroom) into the diagnostics report, so sustained
numbers no longer depend on screenshots.
