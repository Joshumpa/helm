## What

<!-- One sentence describing the change -->

## Why

<!-- Motivation — link the issue or OpenSpec change if applicable -->

## Track

- [ ] Track A (no root required)
- [ ] Track B (root required — stub compiles and runs without it)

## Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes
- [ ] Tested on emulator (Track A) or real device (Track B)
- [ ] No direct OEM / MCU API calls from feature modules — everything goes through `:sdk`
- [ ] Track B code compiles and shows stub UI without root

## Screenshots / Video

<!-- For UI changes, attach a screenshot or screen recording -->
