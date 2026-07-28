---
name: helm-ux
description: UX and visual design rules for Helm — a car head unit launcher. Use when designing screens, components, animations, or layouts for Helm. Covers driver distraction constraints (glance time, tap limits, task time), tap target minimums, typography hierarchy, contrast for automotive (sun glare + night driving), animation timing, information hierarchy, day/night mode, and established Helm patterns. Do NOT use generic mobile app UX conventions here — car HMI is a fundamentally different context.
---

# Helm UX Design

Car head unit UX is not mobile UX. The user is operating a vehicle.
Every design decision must pass one question first: **can a driver interact with this safely?**

---

## Driver Distraction Constraints (non-negotiable)

These are derived from NHTSA Visual-Manual guidelines and SAE J2364.

| Constraint | Limit | Rule for Helm |
|---|---|---|
| Single glance duration | ≤ 2 seconds | Primary info must be readable in one glance |
| Total eyes-off-road per task | ≤ 12 seconds | Any task must complete in ≤ 12 seconds of interaction |
| Manual interactions per task | ≤ 6 taps | Primary actions: 1–2 taps. Never more than 4 for any feature. |
| SAE J2364 total task time | ≤ 15 seconds | From first tap to task complete, including any animation |

### What this means in practice

- A song change: 1 tap (media controls always visible on Now Playing)
- Launching an app: 1 tap from Hotseat, 2 taps from AppGrid
- Changing volume: 1 tap + slider or hardware button — no modal dialogs
- Opening Settings: 1 tap, then 1 tap per setting — no nested submenus deeper than 2 levels

**If a task requires more than 4 taps, the design is wrong.** Restructure the information
hierarchy, not the taps.

---

## Touch Targets

Road vibration reduces touch precision. Targets must be larger than standard mobile.

| Element | Minimum size | Notes |
|---|---|---|
| Primary actions (play, home, back) | 72 × 72 dp | Frequently tapped with one hand |
| Secondary actions (settings icon, etc.) | 56 × 56 dp | |
| App icons in grid | 64 × 64 dp touch area | Visual icon can be smaller |
| Hotseat items | 72 × 72 dp | Always-visible row — must be easy to tap while driving |
| Minimum between adjacent targets | 8 dp gap | Prevents accidental taps on vibrating surface |

**Never place two critical-path targets adjacent without spacing.** A misplaced tap must
never trigger a destructive or hard-to-undo action.

---

## Typography

The display is read from the driver's seat at ~60 cm distance, often in a glance.

| Role | Minimum size | Weight | Notes |
|---|---|---|---|
| Primary glanceable info (speed, time) | 48 sp | Bold | Must be readable in ≤ 2 seconds |
| Media title | 20 sp | Medium | Truncate with ellipsis, never wrap |
| Secondary info (artist, subtitle) | 16 sp | Regular | 70% opacity of primary text color |
| Labels / captions | 14 sp | Regular | Never use below 14 sp for anything interactive |
| Metadata (bitrate, file format) | 12 sp | Regular | Non-interactive only |

- **One font family** throughout Helm — no font mixing across screens
- **Monospace** for speed readout (`tv_carspeed_text`) — variable-width digits shift layout
- **Never wrap** text on interactive elements — truncate instead. Wrapping forces a reading
  pause the driver cannot afford
- **Line height 1.3–1.4×** for any multi-line text block

### Existing Helm typography (established — do not change without reason)

| Element | Size | Location |
|---|---|---|
| Speed value | 60 sp | Speed badge pill |
| Speed unit (km/h) | 22 sp | Speed badge pill |

---

## Color and Contrast

The display operates in two extreme conditions: direct sunlight (washes out colors) and
night driving (glare from bright elements). Design for both simultaneously.

### Contrast minimums

| Context | Ratio | Standard |
|---|---|---|
| Primary text on background | 7:1 | WCAG AAA — car context requires enhanced contrast |
| Secondary text | 4.5:1 | WCAG AA |
| Icons and UI components | 3:1 | WCAG AA |
| Interactive state indicators | 4.5:1 | Must be distinguishable in sunlight |

### Automotive color rules

- **Night mode is the primary theme** for Helm — dark background reduces eye strain and
  glare. Day mode is a light variant, not the default.
- **Never use pure white (`#FFFFFF`) backgrounds** in night mode — use near-black with a
  slight warm or cool tint to reduce harshness
- **Avoid high-saturation colors for large surfaces** — they cause eye fatigue. Reserve
  full saturation for status indicators only (speed badge, alerts)
- **Day/Night switching is hardware-driven** — MCU code `0x0204` sends `arg1=1` for night,
  `arg1=0` for day. Helm must respond to this event, not rely on a manual toggle
- **Status colors must survive both lighting conditions** — test speed badge colors
  (neutral / primary / error) against both the day and night background
- **Shadow tint must match the surface** — never gray/black shadows on a colored surface

### Established Helm color semantics (do not change)

| State | Token | Speed threshold |
|---|---|---|
| Neutral | `MaterialTheme.colorScheme.onSurface` | < 50 km/h |
| Active | `MaterialTheme.colorScheme.primary` | 50 – 89 km/h |
| Warning | `MaterialTheme.colorScheme.error` | ≥ 90 km/h |

---

## Animation Timing

Animations in a car must be **fast and purposeful**. A driver cannot wait for a transition.
Long animations draw eyes off the road.

| Animation type | Duration | Easing |
|---|---|---|
| Screen transition (slide) | 250 – 320 ms | `FastOutSlowIn` |
| Icon launch pop | 150 – 200 ms | Spring: `NoBouncy + StiffnessHigh` |
| State change (color, opacity) | 100 – 150 ms | Linear or `FastOutLinearIn` |
| Boot splash fade | 700 ms fade-in, 1.6 s total | Acceptable — device is parked at boot |
| Feedback animation (button press) | ≤ 150 ms | Spring: `MediumBouncy` |

**Hard limits:**
- No animation longer than 400 ms on any interactive element
- No looping or attention-drawing animations while driving is possible
- No animations that block input (driver must be able to tap through)
- Shared element transitions between screens: max 300 ms

### Established Helm transitions (do not change)

| Transition | Direction | Notes |
|---|---|---|
| Home → Now Playing | Vertical slide up | Music is "above" the home layer |
| Home → Settings | Horizontal slide right | Settings is "beside" the home layer |

---

## Information Hierarchy

What the driver needs to see and when.

### Always visible (zero taps)

- Current time
- Speed (when MCU data is available)
- Media playback status (playing / paused) — at minimum a visual indicator

### One tap away

- Full media controls (Now Playing)
- AppGrid for launching apps
- Quick settings (brightness, volume)

### Two taps away (maximum for any feature)

- Detailed settings
- Equalizer
- Theme selection

**Never put a frequently-used action two taps away.** If a user uses it daily, it belongs
on the Hotseat or as a one-tap action.

### Hotseat design rules

- Fixed row, always visible — never hidden or scrollable
- 4–5 items maximum (portrait width constraint + tap target size)
- Items are the user's most-used apps — configurable, not hardcoded
- No text labels under icons in the Hotseat — icons must be self-explanatory at 64 dp

### AppGrid design rules

- Maximum 3 columns (portrait display constraint — established Helm rule)
- Icon + label below — label max 1 line, truncated with ellipsis
- No pagination animations — instant grid appearance
- Scroll is vertical only

---

## Screen Layout Zones

```
┌─────────────────────┐
│   STATUS BAR        │  ← time, connectivity, MCU status — always present
├─────────────────────┤
│                     │
│   PRIMARY CONTENT   │  ← main action or information for this screen
│                     │
│                     │
│                     │
├─────────────────────┤
│   SECONDARY         │  ← supporting info or media strip
├─────────────────────┤
│   HOTSEAT           │  ← always-visible app shortcuts (bottom — thumb zone)
└─────────────────────┘
```

- **Primary content zone** holds one focused thing per screen — not a dashboard
- **Critical actions go in the bottom third** — thumb zone for a driver's seated position
- **Never place a destructive action in the thumb zone** without a confirmation step
- **Edge-to-edge is active** — always apply `.systemBarsPadding()` on the root `Column`

---

## Navigation Depth

| Level | Example | Max depth |
|---|---|---|
| L0 — always visible | Hotseat, status bar, speed | — |
| L1 — one tap | Now Playing, AppGrid, Settings root | 1 |
| L2 — two taps | Settings subscreen, EQ detail | 2 |
| L3+ — forbidden | Never require 3+ taps for any feature | — |

- **No modal dialogs with multiple options while the car is in motion** — if confirmation
  is needed, use a slide-up bottom sheet with a single large confirm button
- **Back navigation must always be one tap** — never trap the user in a screen
- **No onboarding flows** — Helm must be fully functional from first launch with no setup required

---

## Day / Night Mode

MCU code `0x0204` (decimal 516) signals the ambient light sensor state:
- `arg1 = 1` → night mode (low ambient light)
- `arg1 = 0` → day mode (bright ambient light)

Helm must respond to this event automatically. Design requirements:

- **Both themes must be designed simultaneously** — never design one and adapt the other
- Night: dark surfaces (`#121212` or warmer equivalent), reduced brightness on accent colors
- Day: light surfaces with higher contrast — not just "invert" of night
- **Media album art** must be legible in both themes — use a scrim or blur overlay if the
  art is too bright for night mode
- **Speed badge** color semantic (neutral / primary / error) must work in both themes

---

## Anti-patterns for Car HMI

These are acceptable on phones. They are not acceptable in Helm.

| Anti-pattern | Why it's wrong in a car |
|---|---|
| Text that wraps across 2+ lines on interactive elements | Forces reading pause — driver cannot look that long |
| Modal dialogs with multiple choices | Requires reading + decision under time pressure |
| Animations > 400 ms | Perceived as broken; driver cannot wait |
| Deep navigation (3+ levels) | Violates ≤ 4 taps rule |
| Swipe-only gestures for primary actions | Unreliable on vibrating surface; needs a tap fallback |
| Onboarding / first-run walkthroughs | Car may be in motion at first launch after an update |
| Auto-playing video | Draws visual attention away from road |
| Notifications that expand or animate unexpectedly | Startling — dangerous while driving |
| Touch targets < 56 dp | Missed taps on rough roads |
| Placing confirm + cancel buttons adjacent | One missed tap triggers wrong action |
| Requiring precise drag gestures (e.g. sliders < 200 dp wide) | Road vibration makes these unreliable |

---

## Helm-Specific Patterns (established — follow exactly)

### Speed badge
- Pill shape, always visible in status area
- Color animates based on speed threshold (neutral / primary / error)
- Font: 60 sp monospace for value + 22 sp for unit

### Button press feedback
- Shadow: `NoBouncy + StiffnessHigh` (immediate, tight)
- Scale: `MediumBouncy` to 0.96f then pop back
- Duration: ≤ 150 ms total

### Icon launch animation
- `Animatable` 1.0 → 0.78 → pop back
- Applied only to the tapped icon, not the whole grid
- Both AppGrid and Hotseat use identical animation

### Boot splash
- Wordmark fade-in: 700 ms
- Auto-dismiss: 1.6 s → fade to Home
- No user interaction required or allowed during boot
