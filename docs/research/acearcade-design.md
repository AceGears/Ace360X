# AceArcade design in Ace360X

Status: **all three stages implemented (unverified on device)**.

## Source and licence
- Reference: the owner's AceArcade app (repository AceGears/AceCore, branch `overhaul-0.4`,
  commit 0faffa1; README title "AceArcade"). It is GPL-3.0-or-later.
- Owner's decision (2026-11-23): **rebuild fresh** — Ace360X gets new code that looks and
  moves the same; no AceArcade source is copied, so Ace360X's licence does not change.
  Design facts taken over: theme names, colour values, backdrop layouts, and the numeric
  look parameters (opacities, radii, ranges and defaults of the theme editor).
- Owner's decision: Ace360X uses **AceArcade's theme set only**; the previous 12 Ace360X
  themes and their animated backgrounds are removed.

## What AceArcade's design consists of (as read from its design system)
1. **Themes**: five "glass" themes (Aqua Glass, Violet Pulse, Jade Glass, Sandstone, Plasma)
   with a backdrop — a vertical gradient plus soft radial light orbs — and translucent cards;
   twelve solid themes led by "AceArcade Steel" (neon red on brushed steel, the default).
   Every theme has a dark and a light version; mode is Dark, Light or Follow system.
   Also "Dynamic Material You" and a full "Custom" colour editor.
2. **Surfaces**: cards are bevelled plates (chrome highlight top-left) on solid themes and
   frosted glass (bright top rim, glow-tinted bottom edge) over a backdrop; a blurred neon
   glow behind highlighted elements; a neon header wash; a dual-tone neon edge.
3. **Type and shape**: system sans-serif, heavy weights with tight tracking on headings,
   wide tracking on labels; one corner radius (default 16 dp) scaled ×0.35/0.6/1/1.4/1.8.
4. **User adjustments**: glow 0–100 (45), gradient 0–100 (60), border 0–4 dp (1), corners
   0–32 dp (16), text size 80–150 %, animation speed 25–300 %, glass on/off, reduce motion,
   surface opacity, motion style (liquid/standard), delete effect.
5. **Motion**: press-scale and press-lift springs; a "liquid" selection indicator whose
   leading edge springs fast and trailing edge follows; a shake-then-dissolve delete effect.
6. **Structure**: home with a category dock and several library layouts (grid, compact,
   large, list, carousel), "Continue playing", a game page, grouped settings screens.

## Stage 1: themes and surfaces
`app/.../theme/`: `ThemeSpec.kt`, `Themes.kt` (pure Kotlin, JVM-tested), `AceTheme.kt`,
`Ambiance.kt` (backdrop), `ThemedComponents.kt`, `AppearanceSection.kt`, `ThemeRepository.kt`.
All 17 preset themes in dark and light, plus **Dynamic Material You** (Android 12+; the
system's colours are fed through the same palette builder) and **Custom** (seven colours
entered as hex codes). Adjustments: mode, glass, glow, gradient, corners, border, text
size, animation speed, reduce motion, liquid motion, delete effect.
Not taken over: AceArcade's "surface opacity" slider (glass on/off covers it here) and its
derived dark/light counterpart of a Custom theme (a Custom theme here has one look).

### Deliberate differences from AceArcade
- **Accessibility first** (CLAUDE.md §10, WCAG AA is tested): colours used as *text*
  (primary, secondary, muted text, error) are moved toward the text colour until they reach
  4.5:1 on every surface, including orb-lit backdrop and glass cards. The brand colour is
  unchanged where it is decoration (glow, edges, gradients). Example: Steel's primary text
  red is #E96872 instead of #E3202B; the glow stays #FF2A2A.
- For the same reason the backdrop orbs are dimmed where needed (dark Aqua and Violet 0.45,
  Sandstone 0.25, Plasma 0.30 instead of 0.55), so accents need at most a 30 % shift.
- Custom and Dynamic colours are "sanitised": background and card colour are pushed darker
  or lighter just far enough for text to be readable. A unit test builds themes from 3,000
  random colour sets and requires every one to pass.
- No logo image: the header shows the name as a wordmark in the heading type. No cover art
  (the app has no INTERNET permission, CLAUDE.md §12): covers are generated plates with the
  game's initials in the theme's glow tones.

## Stage 2: structure
- `ui/AppRoot.kt`: screens pushed on a back stack with a fade + short slide (1/6 of the
  width), as AceArcade's navigation does; the bottom tab bar is gone.
- Home (`library/LibraryScreen.kt`): neon header (wordmark, search, layout/sort, settings),
  "Continue playing", games as a cover grid / large covers / list, bottom dock
  (`HomeDock.kt`). Tap starts a game, holding opens a sheet (Play, Game page, Settings for
  this game, favourite).
- AceArcade's dock holds user-made categories. Ace360X has no categories, so the dock holds
  the three filters that exist: All games, Favourites, Recent.
- Game page (`library/GameDetailsScreen.kt`), Settings home with one screen per section
  (`settings/SettingsHome.kt`); Device and About moved under Settings; game folders have
  their own screen (`library/FoldersScreen.kt`).
- Not taken over because the features don't exist in Ace360X: categories and their manager,
  carousel layout, multi-select, cover editing, cheats, import flow, engine settings.

## Stage 3: motion
`theme/Motion.kt`: press-scale and press-lift, the liquid dock indicator, the delete
effect (used when a game folder is removed), tab-change slide, all scaled by the animation
speed and disabled by reduce-motion. Nothing animates continuously.

### Note on closeness
The motion effects and surface treatments reproduce AceArcade's behaviour and numeric
parameters (spring stiffness, durations, opacities, particle counts) so that they feel the
same. The code was written for Ace360X, but an effect with the same phases and numbers is
necessarily structured alike. Both projects have the same owner, who can license their own
design to themselves; if a strictly independent implementation is ever needed (for example
before accepting outside contributions under a different licence), `Motion.kt` and the edge
brushes in `ThemedComponents.kt` are the places to revisit.

## Status
Implemented, compiled and unit-tested by CI. **Not yet seen on a device.**
