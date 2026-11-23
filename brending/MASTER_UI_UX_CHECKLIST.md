# pagomi. — Master UI/UX acceptance checklist

This is the mandatory release acceptance tracker for **pagomi. – PDF Scanner & Editor**. Source inventory: PR #2, commit `1ee82802136f433847971a7713830fda68be74d7`. The list includes all 24 registered tool IDs from Tools.kt. Additional screen states are tracked separately. No row below has verified pixel parity or current-head functional acceptance.

## Status definitions

- Needs implementation / Needs alignment / In progress: not accepted.
- Reference approved: exact image committed and approved for that screen/state.
- 1:1 verified: actual Android screenshot compared with its approved reference at matched content viewport.
- Function tested: relevant positive and negative functional tests passed on the same commit.
- Complete: reference approved, 1:1 verified, function tested, screenshot stored and evidence linked.

A general branding image is not a substitute for a screen-specific reference. Generated mockups are not actual screenshots.

## Main screens

| ID | Screen | Reference | Implementation | Reference match | Function tested | Actual screenshot |
|---|---|---|---|---|---|---|
| launch | Splash / launcher | splash.png | Needs implementation | Pending | Pending | Pending |
| intro-welcome | Onboarding — welcome | onboarding.png | In progress | Pending | Pending | Pending |
| intro-tools | Onboarding — pin tools | New reference required | Needs alignment | Pending | Pending | Pending |
| intro-appearance | Onboarding — appearance | New reference required | Needs alignment | Pending | Pending | Pending |
| consent | Consent / privacy acceptance | New reference required | In progress | Pending | Pending | Pending |
| home | Home dashboard | home.png | In progress | Pending | Pending | Pending |
| camera | Camera scanner | scanner.png | In progress | Pending | Pending | Pending |
| review | Review Scan | review-scan.png | Needs alignment | Pending | Pending | Pending |
| files | My Files | files.png | In progress | Pending | Pending | Pending |
| tools | PDF Tools hub | tools.png | In progress | Pending | Pending | Pending |
| viewer | PDF Viewer / Editor | pdf-editor.png | In progress | Pending | Pending | Pending |
| settings | Settings | settings.png | In progress | Pending | Pending | Pending |

## All registered tools

| ID | Screen | Reference | Implementation | Reference match | Function tested | Actual screenshot |
|---|---|---|---|---|---|---|
| scan | Scan Document | scanner.png | In progress | Pending | Pending | Pending |
| workflow | PDF Workflow Builder | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| merge | Merge PDFs | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| alternate | Alternate & Mix PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| compress | Compress PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| split | Split PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| splitText | Split PDF by Text | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| splitBookmarks | Split PDF by Bookmarks | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| splitHalf | Split PDF in Half | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| splitSize | Split PDF by Size | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| organize | Organize Pages | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| rotate | Rotate PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| flip | Flip PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| nup | Multiple Pages Per Sheet | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| crop | Crop & Resize | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| zip | PDF to ZIP | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| fillSign | Fill & Sign | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| watermark | Add Watermark | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| pageNumbers | Add Page Numbers | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| redact | Redact PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| ocr | OCR Scanned PDF | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| unlock | Remove Password | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| metadata | Edit PDF Details | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |
| password | Set Password | New tool-specific reference required | Existing functionality; visual review pending | Pending | Pending | Pending |

## State and component coverage

### Launch and onboarding

- [ ] Android launch and cold/warm startup
- [ ] Welcome, tool pin selection, light/system/dark selection
- [ ] Skip, Back, Continue, finish
- [ ] Privacy policy, terms, consent validation and repeat access

### Home and navigation

- [ ] Five tabs and selected states
- [ ] Scan, Import PDF and quick tools
- [ ] Recent files — empty, populated, search and no results
- [ ] Resume/discard unfinished tool
- [ ] Back navigation, keyboard and safe-area handling

### Scanner

- [ ] Camera permission granted, denied, permanently denied
- [ ] Automatic/manual capture, flash, camera switching
- [ ] Document, ID front/back, whiteboard and other implemented modes
- [ ] Detection guides, no detection, busy, captured, gallery import
- [ ] QR result, recent scans, page retake, review entry

### Review and scanned-page editing

- [ ] Page preview, thumbnails, select all, add/remove/reorder pages
- [ ] Crop and corner dragging, rotate, applicable page filters
- [ ] Brightness/contrast and apply-to-all
- [ ] Draw editor and available mark modes
- [ ] Rename, cancel/discard, save/update PDF, progress and error

### Files

- [ ] Saved/Browse/Drive tabs; folders and subfolders
- [ ] Search, filter, empty states, file/folder menus
- [ ] New folder, rename, move, delete and confirmations
- [ ] Select PDF via device picker and AppFilesPicker
- [ ] Export dialog, share/open, PDF/Word export where implemented
- [ ] Drive sync states, network errors, local-only files

### Individual tools and PDF editors

- [ ] File picking, multi-file order, tool options, validation
- [ ] OrganizeEditor, PdfCropEditor, WatermarkEditor
- [ ] FillSignEditor, SignaturePad, RedactEditor, DetailsEditor
- [ ] WorkflowEditor — steps, options, order and results
- [ ] PasswordDialog, unsupported/corrupted input, processing, cancel
- [ ] Results, naming, save, share, export and repeated operations

### Viewer

- [ ] Document loading, opening, unsupported viewer fallback
- [ ] PasswordPrompt — incorrect/correct password and cancel
- [ ] Search and zero/multiple matches, previous/next match
- [ ] Page jump, thumbnails, selection handles and copy
- [ ] Share, open externally, zoom and scroll

### Settings and legal

- [ ] ThemePicker and all light/dark/system previews
- [ ] Storage counts and delete confirmation
- [ ] Searchable scanning toggle
- [ ] DriveBackup connection/disconnection and sync errors
- [ ] Privacy, terms, About, help/feedback and app version

### Shared components

- [ ] Header, navigation, group labels, icon/round buttons
- [ ] Primary, secondary, accent, danger, disabled and busy buttons
- [ ] ChipRow, filters, toggles, input fields and search
- [ ] AppDialog, ConfirmDialog, PromptDialog, FullSheet, MenuRow
- [ ] Spinner, empty/error/success states, focus and selected states

## Acceptance for every screen/state

- [ ] Original approved design reference committed under brending/references; record filename and hash.
- [ ] Compare content at matching viewport, density, font scale, theme, locale, fixture data and state.
- [ ] Match layout, text sizes/weights, spacing, colors, gradients, icon geometry, borders, radius, shadows and navigation.
- [ ] Match reference content rather than drawing an iOS frame or duplicating operating-system status/navigation bars.
- [ ] Generate difference image and record pixel difference; require exact match in comparable stable regions. Explicitly document any unavoidable dynamic/system exclusions. Do not silently mask content differences.
- [ ] Check 360, 393, 412 dp widths, landscape where supported, light/dark/system and font scaling. No clipping, overlap or off-screen inaccessible controls.
- [ ] Test TalkBack, focus order, touch targets, contrast, keyboard, system back and safe areas.
- [ ] Test normal, empty, loading, error, disabled, selected, permission-denied and offline states where applicable.
- [ ] Verify real actions, persistence, PDF outputs, cancellation, and data protection. No decorative nonworking controls.
- [ ] Capture genuine screenshots from installed Android build under brending/screenshots. Use only synthetic demo documents, not personal user files.
- [ ] Record commit SHA, emulator/device, viewport, density, API, theme, fixture, reference hash, screenshot path, diff result and test links.
- [ ] Add verified actual screenshots to the public marketing README. Keep implementation documentation inside brending.

## Release gate

- [ ] All references imported, including missing screen-specific variants.
- [ ] Every screen and state accepted using the checks above.
- [ ] Current HEAD Android CI, Lint, unit tests, device tests and 16 KB native alignment pass.
- [ ] Debug APK and debug AAB built and published as a GitHub prerelease with checksums and matching source tag; no production signing key required for debug.
- [ ] Google Play production review completed separately, including privacy, Data Safety, permissions, target SDK, native compatibility and signed release bundle.

Do not claim 1:1 completion or Play-ready status merely because CI passes. Never disable checks to report success.

## Evidence record template

| Screen/state | Commit | Reference/hash | Device/API/viewport/theme | Screenshot | Diff | Test | Accepted |
|---|---|---|---|---|---|---|---|
| Example (not evidence) | — | — | — | — | — | — | No |

## Fixed-screen requirement (0.1.9)

- [ ] No free vertical swipe on app screens, tool forms, settings, onboarding, pickers, lists or modal content. Overflow remains accessible via explicit Previous/Next controls, with no unreachable actions.
- [ ] No visible developer labels (including `Local`, `Debug`, internal paths, raw exception messages or stack traces).
- [ ] Verify small and large screens, font scaling, keyboard, TalkBack and every page boundary; PDF content navigation and zoom must stay usable.
- [ ] Recapture and compare against approved references before claiming pixel-perfect acceptance.
