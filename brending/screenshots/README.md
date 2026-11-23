Actual installed Android application screenshots only. Do not publish AI-generated mockups as real screenshots.

Capture each screen from the installed Android app using `python3 scripts/capture_android_screen.py <screen>` with ADB connected. The script verifies the foreground app and writes genuine PNGs. Review every capture before including it in the marketing README. No images are listed until captured and verified.

CI now captures actual first-run onboarding and Home, My Files, Tools and Settings from an isolated debug installation after instrumented tests. The `pagomi-actual-screen-captures` artifact contains PNGs and capture metadata. These are candidate screenshots, not proof of reference parity. Review before committing to this directory and linking in the marketing README. Scanner, Review, PDF editor, individual tools and remaining states still need real captures.

## Published gallery

The real 0.1.7 screenshots are committed at `0.1.7/` and displayed in the root README. Source: successful device test run 45279327395; the metadata is preserved exactly. The screenshot gallery is not a visual parity approval. The expanded 0.1.8 capture includes a synthetic three-page PDF library, scanner, review, viewer and all 23 non-camera tool entry screens at 1080x2400 / 440 dpi. Additional tool options, populated editor states, dialogs, alternative modes and themes still need coverage. Archive only successful runs; never use generated concepts as screenshots.

## 0.1.8 gallery

31 authentic screenshots are stored under `0.1.8/`: eight main screens and 23 individual PDF tool entry screens. Source: successful device test run 45286276735. These are captured states, not proof of 1:1 visual parity; other dialog and advanced editor states still require capture.
