# Google Play release verification — pagomi.

**Status: pending production validation.** A debug APK or unsigned AAB artifact is not a publishable release.

- [ ] CI passes APK compilation, JVM unit tests, Android Lint, 16 KB native ELF alignment checks and AAB bundle generation at the final release commit.
- [ ] Full instrumentation coverage of all PDF tools, OCR, scanning, signing, encrypting, redacting, importing and exporting; test large and corrupted files.
- [ ] Device tests: Android API 24, 30, 34, 36; small and large screens; light and dark themes; TalkBack and font scaling.
- [ ] Play permission review: do not request MANAGE_EXTERNAL_STORAGE; use Android Storage Access Framework.
- [ ] Reconfigure Google OAuth credentials for com.brendigo.pagomi and the new signing certificate. If not configured, do not advertise Drive sync as working.
- [ ] Review Google Play Data Safety declarations and publish a privacy policy at a stable public URL. Confirm the Android 12+ backup exclusion rules are effective on tested devices.
- [ ] Generate new independent upload signing key, store outside source control, build signed AAB and validate with bundletool.
- [ ] Create real screenshots of pagomi. on devices, 512×512 launcher artwork, 1024×500 feature graphic, full listing and accessibility checks.
- [ ] Supply native debug symbols when available for the OpenCV library and review targetSdk 36 / minSdk 24.
- [ ] Verify merged source retains the original MIT license and third-party notices.
- [ ] Complete Play Console content rating, app access, testing and staged rollout requirements.

Internal Gradle root name is `pagomi`; the public brand is always `pagomi.` and the Play title is `pagomi. – PDF Scanner & Editor`.
