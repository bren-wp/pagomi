# pagomi. — design system

![Pagomi icon](assets/icon.svg)

**pagomi. – PDF Scanner & Editor** · Scan. Edit. Create.

## Reference identity

Ink #192B36, Violet #6459D8, Lavender #E8E5FF, Mint #CDF2DE, Paper #F6F7F4. Reference typography: Sora (display), Inter (body); existing Poppins remains until fonts and layout are validated. The SVG and Android vector are editable implementations of the scanner motif, not pixel-identical copies.

## Reference PNG package

Original generated references are supplied separately as a ZIP with the paths below; binary transfer to the repository is still pending.

| File under references/ | Screen |
| --- | --- |
| brand-board.png | Brand board |
| splash.png | Launch |
| onboarding.png | Introduction |
| home.png | Dashboard |
| scanner.png | Camera |
| review-scan.png | Crop and enhancement |
| files.png | Library |
| tools.png | PDF tools |
| pdf-editor.png | Editor |
| settings.png | Preferences |

## Acceptance

Match the visual references using native Compose, preserving all actual functionality. Android owns the status bar and navigation insets; do not draw iOS phone chrome. Do not hardcode the sample files, fictional account or unimplemented Premium features. Validate each screen at 360, 393 and 412 dp, dark/light mode and enlarged font scale. Capture actual installed app screens under screenshots/; compare against references before marking 1:1 complete. Do not use concept art as product screenshots.
