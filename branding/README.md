# pagomi. — Brand system

The product brand is always written **pagomi.** (lowercase, including the final period). The public application name is **pagomi. – PDF Scanner & Editor**. Product language is English.

## Design tokens
- Primary accent: `#6459D8` — refined violet
- Dark ink: `#192B36`
- Paper: `#F6F7F4`
- Soft mint: `#CDF2DE`
- Accent tint: `#E8E5FF`

The source of truth for the upstream import is [brand.json](brand.json). The resulting Android application's runtime color tokens are in `app/src/main/java/com/brendigo/pagomi/Theme.kt` (`Brand`, `LightPalette`, `DarkPalette` and `ScanUi`). When updating styling after import, edit runtime tokens there and keep `brand.json` in sync. Text display name lives in `app/src/main/res/values/themes.xml` and the product identity constants live in `Theme.kt`.

The launcher artwork is a custom scalable Android vector at `branding/assets/pagomi_launcher.xml`, imported as `app/src/main/res/drawable/pagomi_launcher.xml`. It is original artwork; do not restore upstream icons.

## Reproduce the source import

Run `python3 scripts/import_upstream.py` from a clean working tree; this fetches the fixed upstream commit (`6a48d60ccbbaca4dd18840b9d85f00d44b51f311`) and reconstructs the Kotlin source tree. Never point it at unreviewed upstream HEAD. The import **replaces the app directory**, so commit deliberate UI and feature changes before rerunning or reconcile them manually.

The initial import runs automatically through `.github/workflows/bootstrap.yml` on the import branch. Android builds must use Java 17 and Android SDK 36 with extension 19.

## Signing

Use a **new pagomi. upload key** stored outside the repository. Configure these local Gradle properties:
```properties
PAGOMI_UPLOAD_STORE_FILE=/private/path/pagomi-upload.jks
PAGOMI_UPLOAD_STORE_PASSWORD=...
PAGOMI_UPLOAD_KEY_ALIAS=...
PAGOMI_UPLOAD_KEY_PASSWORD=...
```
Neither signing keys nor plaintext signing properties belong in GitHub. CI builds may be unsigned unless secrets are configured; do not call unsigned artifacts Play-ready.

## Quality gates before launch

Verify camera permission and multi-page scan, OCR languages, PDF operations, viewer fallback on older Android versions, data safety, storage policy, Drive OAuth (must be reconfigured for the new application ID), accessibility, responsive layouts and dark mode. No math solver is shipped yet.

## Upstream attribution

This work derives from [sammyZi/pdf-tools](https://github.com/sammyZi/pdf-tools), under MIT. Keep the upstream copyright in `LICENSE` and third-party notices in `THIRD_PARTY_NOTICES.md`.
