# First on-device game launch — 2026-11-21

**Source:** diagnostics report shared by the user (`ace360x-diagnostics-20261121-170018.txt`),
build `3e1dc4b` (debug). Not committed: it contains the full logcat of the user's phone.

**Device:** Samsung SM-S948U1, Qualcomm SM8850, Adreno 840, Android 17 (API 37).
Vulkan driver: Qualcomm proprietary, version 0x8034A013, build date 07/01/26, API 1.4.295
(Xenia uses 1.3).

**Title:** Naruto: Rise of a Ninja (Europe), disc image from the user's SAF folder. Title ID
555307E5, media ID 12DF7A0D, `default.xex` module hash AA17DB7395C0A667.

## What the log shows (session 17:00:13 – 17:00:17)

| Time | Event |
|---|---|
| 13.93 | Xenia starts; memory, processor (a64 JIT), audio, graphics, HID, VFS, kernel initialise |
| 13.94 | Vulkan device "Adreno (TM) 840" selected; all required features present |
| 14.07 | Swapchain 3120×1440 created |
| 14.09–14.14 | Disc image opened through `content://` (patch 0005); `default.xex` decrypted and parsed (basic compression, 194 xam + 349 kernel imports) |
| 14.25 | Kernel launches the module; "Main XThread" runs guest PowerPC code on the a64 JIT |
| 14.28–14.80 | The game creates ~16 guest threads, reads files from the disc, opens AAudio (48 kHz, MMAP low-latency path) |
| 14.86 | Game configures the hardware scaler (16:9) — it is driving the GPU |
| 14.93 | GPU command processor starts (0 cached shaders) |
| 15.87 | User presses Back (1.6 s after the GPU started); quit dialog; Quit |
| 17.83 | Xenia's normal exit ("Cheap-skate exit!"), process exit status 0 |

No crash, fatal error or guest exception was logged. One guest file error
(`ResolvePath(ERRasser.log) failed`) is the game probing for a debug log; harmless on desktop
Xenia too. "Shader compilation failed" lines in the log are from Android's own UI renderer
(HWUI thread), not from Xenia.

## Conclusions

- **First evidence that the core runs on a phone:** a retail XEX was loaded from a SAF disc
  image and its code executed on the a64 JIT, with kernel, file system, audio and GPU set up.
- **Not shown:** whether any frame was drawn, whether the game reaches its menu, sound output,
  input. The session lasted 3.6 s. No compatibility status can be recorded yet.
- **Bug found and fixed:** a normal quit showed "The last game session ended unexpectedly".
  Upstream's native `OnDestroy` ends the process with `std::quick_exit`, so our clean-exit
  marker, written after `super.onDestroy()`, never ran. It is now written before.
- **Note:** on Android, upstream Xenia logs only to logcat; the report now says so instead of
  "(no xenia.log yet)".

## Next test requested from the user
Leave the game running for at least 60 seconds, note what the screen shows (black, logos,
menu) and whether there is sound, then quit and share diagnostics.

## Second run (21:09:27 – 21:12:51, build 51afa68, 3 min 24 s)

- No crash. Normal quit through the dialog (EXIT_SELF, status 0). Whether the "ended
  unexpectedly" banner still appeared isn't visible in the report; asked the user.
- The game got further: at +6.5 s it loaded its second module `AI2C.dll` (AI library) from the
  disc, created more threads and read many files until +19.6 s.
- Guest-side warnings, also common on desktop Xenia: unimplemented kernel export
  `IoDismountVolumeByFileHandle` (×2), `BaseHeap::Release failed because address is not a
  region start` (×6), `ResolvePath(Menu\) failed`.
- From +19.6 s the log is silent, except that at +79.6 s (exactly 60 s later) two threads
  are created and the log goes silent again until the user quit. A 60 s gap suggests the game
  waited on something with a timeout.
- Still unknown: whether any frame reached the screen. Xenia doesn't log presented frames,
  so patch 0013 adds a "GPU activity" line (guest swaps and draw calls).

## Third run (21:32:30 – 21:32:36, build 8b25be4, 5 s)

- With patch 0013: **guest swap #1 at +1.0 s, 1280×720 front buffer, 36 draw calls**. The game
  does produce frames; the user saw a black screen (with the on-screen controls) in all runs.
  The session ended before the 5 s report interval, so the swap rate is still unknown.
- The "ended unexpectedly" banner still appeared. Cause from the log: on Quit, the Library
  resumes in the main process ~0.5 s before the game process reaches `onDestroy`, so it saw
  the session marker. Fixed by clearing the marker on Quit and ignoring it while `:emu` is
  alive.
- Patch 0014 logs presenter results so the next run shows whether frames reach the screen.

## Fourth run (22:53:17 – 22:57:47, build afa8382, about 4.5 min)

- **The game runs at a steady 30 guest frames per second** (150 swaps every 5 s, from +5 s
  to the end), 1280×720, about 5,500 draw calls per second after the menu loads. This is
  the guest frame rate measured by the command processor, not a host-display FPS.
- **Every presenter paint (30/s) returns `VK_SUBOPTIMAL_KHR`**, none fail, guest output is
  active. Yet the screen shows only black with the on-screen controls (user screenshot).
- Suboptimal on Android typically means the swapchain `preTransform` (identity here) differs
  from the surface's current rotation; Android should still compose such frames. So either
  the guest image itself is black, or the composed SurfaceView isn't visible.
- Patch 0015 colours the letterbox bars dark blue in debug builds to tell these apart.
- Install note: Google Play Protect blocked the update ("hasn't seen an app from this
  developer"); expected for a debug build signed with a public key.

## Fifth run (23:46:52 – 23:47:20, build 9d63b62, 28 s) — letterbox test

- **The dark blue letterbox bars are visible** (user screenshot), the 16:9 area between them
  is black. So the presenter's images reach the display; Android composition, the SurfaceView
  and the swapchain work. **The guest image itself is black.**
- The game again swaps 30 frames/s at 1280×720 with up to ~5,500 draws/s.
- Next suspect: the GPU emulation path on the Adreno proprietary driver (render-target
  emulation, EDRAM resolve or texture loading producing an empty front buffer). No Xenia
  warning or shader error is logged.
- Second issue: after the swapchain was recreated at +9 s (surface change), no further
  presenter paint was logged for the remaining 19 s, while the guest kept swapping. Guest
  output may stop refreshing after a surface change on Android; to investigate separately.

## GPU frame captures replayed on a reference driver (2026-11-22)

- The user captured guest frames 450 and 900 on the phone (build d11d3ad), shared as text.
- Replayed here with unmodified upstream `xenia-gpu-vulkan-trace-dump` (x86-64 Linux build
  of the pinned commit) on Mesa lavapipe (software Vulkan 1.4): **frame 450 shows the Bink
  Video logo, frame 900 shows the game's title screen ("PRESS START BUTTON")**, both correct.
  The images and traces are not committed (they contain the game's copyrighted content).
- Conclusion: CPU emulation, kernel, file I/O and the guest's GPU command stream are
  correct on the phone; the game reaches its title screen. The black image comes from the
  Vulkan GPU backend on the Adreno 840 proprietary driver. The user also reports hearing
  the game's sound.
- Next: "Replay on this phone" (Device tab) replays the same capture on the phone with
  several GPU cvar variants and saves each image, to find which GPU path fails.

## On-phone replay (2026-11-22, build 87581ea)

- The user's capture of frame 900 (title screen) replayed **on the phone** with six cvar
  variants: **all black**, except variant 3 (`gamma_render_target_as_unorm16=false`) which
  shows a few small blue fragments at the top-left. The same trace renders correctly on
  lavapipe. So the failure reproduces offline, in about 1.5 s per replay, without the game.
- None of the tried cvars fixes it. The variant 7 result was missing from the results file
  (a race between the worker thread and UI list; fixed).
- No Adreno driver errors are logged in the emulator process ("Pipeline create failed" lines
  are from the main app process's UI renderer).
- Next: replay with the Khronos validation layer (debug APK only) to find invalid Vulkan
  usage that the Adreno driver handles differently from Mesa.

## Validation run and next bisect step (2026-11-22, build df77831)

- On-phone replay with `VK_LAYER_KHRONOS_validation` loaded (confirmed in logcat): **no
  validation errors** for the frame (only two shader-cache file warnings). All 8 variants
  black. So the black image is not explained by invalid Vulkan usage that the layer detects.
- Patch 0016 adds two diagnostics, checked on lavapipe with the same capture: front-buffer
  readback statistics (74 % of sampled bytes non-zero on lavapipe) and a magenta test
  pattern. On the phone they will show whether pixels are lost before the resolve (render
  targets / EDRAM emulation) or after it (texture load / gamma pass / guest output).

## Bisect step 1 result (2026-11-22, build 3b47952)

- Magenta test pattern appears in the on-phone capture: the gamma pass target, guest output
  image and capture path work on the Adreno 840.
- Front-buffer readback on the phone: **0 of 60,433 sampled bytes non-zero** (lavapipe:
  44,576). So nothing visible reaches guest memory through resolve + readback: the loss is
  in rendering to host render targets, the resolve, or shared memory, not in the final pass.
- Patch 0017 lets the replay disable groups of optional driver features to find which
  fallback path, if any, produces pixels on the phone.

## Bisect step 2 result (2026-11-22, build 98bfdf9)

- All nine feature-group variants (no geometry shaders, no tessellation, no stencil export /
  sample locations / conditional rendering, no depth clamp / clip-cull distances, no float
  controls, no view swizzle / scalar layouts, no sparse memory, 8-bit gamma targets): readback
  **0 of 60,433** on the phone every time. Not caused by one of these optional features.
- Patch 0018: magenta painted into the render target before each resolve, and permissive draw
  pipelines, to separate "draws produce nothing" from "resolve/readback loses it".

## Bisect step 3 result (2026-11-22, build 77ef5fa)

- Variants 2 and 4 (magenta painted into the render target before each resolve): fully
  magenta image on the phone, readback **44,821 of 60,433** bytes non-zero, average 189 —
  identical to lavapipe. Resolve log lines identical too (7 resolves, last to 1F303000,
  length 3,768,320, format 6).
- Variants 1 and 3 (default; permissive pipelines): 0 of 60,433.
- So host render targets, resolve, shared memory, readback, front-buffer texture load, gamma
  pass and capture work on the Adreno 840. The guest's draw calls leave no pixels, and that
  is not due to culling or depth/stencil rejection.
- Open: vertex stage / rasterization (vertex fetch from the shared-memory buffer, system
  constants, viewport/scissor) versus pixel stage (translated pixel shaders, texture
  sampling, alpha test, blending, write masks).
- Patch 0019: constant-magenta pixel shader substitute and blend-off pipelines to split
  those two. Lavapipe reference values are in `TraceReplayActivity.VARIANTS`. Not yet run on
  the phone.

## Bisect step 4 result (2026-11-23, build 2a461ed)

- Constant-magenta pixel shader substitute: **29,881 of 60,433** on the phone, and 44,821
  with blending off + permissive pipelines — both identical to lavapipe. Default and
  blend-off alone: 0.
- So vertex shaders, vertex fetch, viewport/scissor and rasterization of guest geometry
  work on the Adreno 840. The loss is inside the translated pixel shaders (or their inputs:
  interpolators, textures, constants), and blending/write masks alone do not explain it.
- Patch 0020 splits the pixel stage (output override after guest code, no discard, white
  textures). Not yet run on the phone.
