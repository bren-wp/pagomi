Actual installed Android application screenshots only. Do not publish AI-generated mockups as real screenshots.

Capture each screen from the installed Android app using `python3 scripts/capture_android_screen.py <screen>` with ADB connected. The script verifies the foreground app and writes genuine PNGs. Review every capture before including it in the marketing README. No images are listed until captured and verified.

CI now captures actual first-run onboarding and Home, My Files, Tools and Settings from an isolated debug installation after instrumented tests. The `pagomi-actual-screen-captures` artifact contains PNGs and capture metadata. These are candidate screenshots, not proof of reference parity. Review before committing to this directory and linking in the marketing README. Scanner, Review, PDF editor, individual tools and remaining states still need real captures.
