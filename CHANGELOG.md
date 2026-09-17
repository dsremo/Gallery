# Changelog

All notable changes are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-09-17

Initial rebranded release. Forked from Fossify Gallery 1.13.1 (GPL-3.0).

### Added

- Private folders (biometric or PIN lock, per folder)
- Smart albums — auto-grouped Recent, Favorites, Videos, Screenshots
- Duplicate detection using SHA-256 plus a perceptual-hash pass for near-duplicates
- Bulk EXIF location stripping
- Photo compression tool
- Storage reclaim advisor — trash, cache, and unused HEIC breakdown with one-tap cleanup
- Trash auto-cleanup with configurable retention
- HEIC support advisor
- Settings-wide search
- GPL section 5(a) modification notice on the About screen

### Changed

- Settings restructured into 8 top-level categories, one level deep
- Select-all lives in the action-bar title as a `☐ / ☑` glyph — no separate icon
- Rotate is two direct action-bar icons (left / right) rather than a submenu
- Video-player loop is a session-only button in the player, not a global setting
- "Show hidden folders" reveals only folders the user hid; `.nomedia` folders from other apps gated behind Settings → Advanced → Show system folders
- Back button inside a folder returns to Directories view
- Photo-viewer chrome auto-hide slowed from 500ms to 6s
- Package renamed: `org.fossify.gallery` → `com.dsremo.gallery`
- Version reset to 1.0.0 / code 1
- Icon replaced with a sun + two-mountain landscape vector

### Removed

- Obvious-default toggles that no well-known gallery exposes as user preferences: global loop-videos, show-highest-quality, deep-zoom-on-double-tap, scroll-horizontally, show-notch, show-bottom-actions, enable-pull-to-refresh, show-ripple-animation, move-deleted-files-to-recycle-bin, show-all-folders-content, grid filename toggle
- Redundant magnifier icon in the main-grid toolbar (search bar below already covers this)
- Fossify branding, upstream social links, upstream donate targets
- Private cloud/SSO integration (stays local; not part of the public source)
