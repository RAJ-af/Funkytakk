WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

# PROMPT 3 — Avatar frame sizing + profile header alignment

Read `docs/gemini-prompts/2026-11-20-frame-sizing-and-live-preview.md` first. If `FramedAvatar` and the frame calibration sheet from that file are NOT in the code yet, implement them as described there (Tasks A and C), then do the tasks below. If they exist, reuse them and do not create a second avatar component.

## What is wrong (from a screenshot of the own-profile screen, Floral Moon frame)
- The frame looks cut off / squeezed inside a circle and its size is a guess (`avatar_ratio 0.6` copied to all 3 asset frames, `calibrated = false`).
- The avatar does not start on the same left line as the text (name, username, stats, buttons) and the frame art is not given its own space.
- Debug and release look the same, so this is a layout/size problem, not a build problem.

## Rules
1. Only the PHOTO is clipped to a circle. The frame (asset or code-drawn) is NEVER clipped by `CircleShape` or the parent; the parent must not clip children.
2. All sizes are in dp and come from `config.asset` (`inner_ratio`, `offset_x`, `offset_y`, `overflow_ratio`, `z`). No hardcoded ratios or px anywhere. Do not "fix" the look by changing constants in code: the real numbers are saved with the staff Calibrate sheet (`admin_set_frame_calibration`).
3. Layout space = `FramedAvatar.outerSize(photoSize, frame)` (frame canvas plus overflow), so frame art never overlaps the name or the buttons.

## Profile header (own profile AND other users' profile, like language-exchange apps such as HelloTalk or Tandem)
- Cover image on top. The avatar overlaps the bottom edge of the cover by about 40% of its outer height.
- The LEFT edge of the avatar's outer box (frame art included) sits exactly on the 16 dp content margin line, the same line where "Raj", the @username, location, stats row, buttons and cards start. With no frame, the photo's left edge sits on that line.
- The name row starts directly under the avatar's outer box with an 8 dp gap. Nothing is cut at the left screen edge or by the cover.
- The country flag badge is anchored to the bottom-end of the PHOTO circle (not the frame canvas) and also never clipped.
- Photo size 88 dp by default; use one constant shared by the own profile, other-user profile and Edit Profile preview.
- Apply the same `FramedAvatar` to: Live Preview, store tiles, voice room seats, chat header, discover cards.

## Test and report
Check on Galaxy M02 (small screen) and Oppo A78. Test all 3 asset frames, one code-drawn frame (Neon Pulse) and the no-frame case. Finish with what you could not verify (frames are still uncalibrated until staff run Calibrate for each one).
