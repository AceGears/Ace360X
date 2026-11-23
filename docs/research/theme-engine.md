# Theme engine — research notes

## Requirements (CLAUDE.md §9, §10)
- `ThemeSpec` = colour scheme + typography + shape + elevation + motion tokens + icon style +
  ambiance renderer + component overrides. 12 named themes; colour-only themes don't count.
- User customisation stored as a diff over a base theme (DataStore, versioned schema).
- WCAG AA text contrast for every theme, checked by an automated test.
- Motion from central tokens; `ANIMATOR_DURATION_SCALE = 0` and a user reduce-motion switch
  disable continuous animation. No decoration while a game runs.

## Sources
- WCAG 2.2, Success Criterion 1.4.3 Contrast (Minimum): 4.5:1 for normal text, 3:1 for large
  text and (1.4.11) for UI component boundaries. Relative luminance and contrast ratio
  formulas: W3C "relative luminance" definition (sRGB linearisation with the 0.04045
  threshold) — https://www.w3.org/TR/WCAG22/#dfn-relative-luminance
- Material 3 colour roles (`ColorScheme`): each `onX` colour is the text/icon colour used on
  `X`; cards default to `surfaceContainerHighest`, navigation bar to `surfaceContainer`.
  androidx.compose.material3 1.3 (Compose BOM 2024.12.01) `ColorScheme` constructor.
- System font families on Android: `Typeface.create("sans-serif-condensed" | "sans-serif-light"
  | "sans-serif-medium" | "sans-serif-black" | "serif" | "monospace", style)`; Compose wraps
  an `android.graphics.Typeface` with `FontFamily(typeface)`. No font files are bundled, so
  there is no licence or APK size impact.
- `Settings.Global.ANIMATOR_DURATION_SCALE` (0 = animations off in developer options /
  accessibility "Remove animations").

## Design decisions
- The token model (`theme/ThemeSpec.kt`) is plain Kotlin with ARGB `Long`s and enums so the
  contrast test runs on the JVM without Robolectric; Compose mapping is a separate file.
- "On" colours for derived containers and accents are chosen from the theme's text colour,
  background, black and white by highest contrast; hand-picked text colours are checked as is.
- Translucent ("glass") cards are checked after compositing over the background and over
  the ambiance's strongest colour, which is what the text actually sits on.
- Accent override: only accents that keep every checked pair at ≥ 4.5:1 for that theme are
  offered (`validAccentsFor`).
- Ambiance is drawn with `Canvas` behind non-game screens only; the game runs in the `:emu`
  process with its own activity, so no ambiance can run during gameplay. Animated ambiance
  uses one slow infinite transition and draws a static frame when motion is reduced.

Superseded 2026-11-23: the theme set and token model were replaced to match AceArcade; see `acearcade-design.md`.
