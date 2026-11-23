# AceArcade design in Ace360X

Status: **stage 1 implemented (unverified on device)**; stages 2–3 not started.

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

## Stage 1 (done): themes and surfaces
`app/.../theme/`: `ThemeSpec.kt`, `Themes.kt` (pure Kotlin, JVM-tested), `AceTheme.kt`,
`Ambiance.kt` (backdrop), `ThemedComponents.kt`, `AppearanceSection.kt`, `ThemeRepository.kt`.
Items 1–4 above except: Dynamic Material You, Custom colours, surface opacity, and the
motion-style/delete-effect switches (they belong to stage 3 effects that don't exist yet;
CLAUDE.md §9: no dead controls).

### Deliberate differences from AceArcade
- **Accessibility first** (CLAUDE.md §10, WCAG AA is tested): colours used as *text*
  (primary, secondary, muted text, error) are moved toward the text colour until they reach
  4.5:1 on every surface, including orb-lit backdrop and glass cards. The brand colour is
  unchanged where it is decoration (glow, edges, gradients). Example: Steel's primary text
  red is #E96872 instead of #E3202B; the glow stays #FF2A2A.
- For the same reason the backdrop orbs are dimmed where needed (dark Aqua and Violet 0.45,
  Sandstone 0.25, Plasma 0.30 instead of 0.55), so accents need at most a 30 % shift.

## Stage 2 (not started): structure
Home/library layouts, category dock, game page, settings grouping.

## Stage 3 (not started): motion
Liquid indicator, press-lift, delete effect, and their switches.
