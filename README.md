# Dsremo Gallery

A privacy-focused photo and video gallery for Android — forked from [Fossify Gallery](https://github.com/FossifyOrg/Gallery) with reworked UX and new features.

No ads. No cloud sync. No unnecessary permissions.

## What's different from upstream

**New features**

- Private folders — biometric or PIN lock, per-folder
- Smart albums — auto-grouped (recent, favorites, videos, screenshots)
- Duplicate detection using perceptual hashing
- Bulk EXIF location stripping
- Photo compression tool
- Storage reclaim advisor — trash / cache / unused HEIC breakdown
- Trash auto-cleanup with configurable retention
- HEIC support advisor
- Settings-wide search

**UX rework**

- Settings restructured into 8 top-level categories, one level deep
- ~15 obvious-default toggles removed (loop-videos, deep-zoom-on-double-tap, show-highest-quality, etc.)
- Select-all lives inside the action-bar title as a `☐ / ☑` glyph — no separate icon
- Back button inside a folder returns to Directories view (was missing upstream)
- Rotate is two direct icons (left / right), not a submenu
- Video-player loop is a session button in the player, not a global setting
- "Show hidden folders" reveals only folders you hid — not `.nomedia` folders from other apps
- Photo-viewer chrome auto-hide slowed to 6s (was 500ms)

## Build

```bash
./gradlew assembleFossRelease
```

The APK lands in `app/build/outputs/apk/foss/release/`.

To sign the release build, put a `keystore.properties` at the repo root:

```
storeFile=/absolute/path/to/your.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

Omit it and the build proceeds unsigned.

## License

[GPL-3.0](LICENSE) — same as upstream.

Based on [Fossify Gallery](https://github.com/FossifyOrg/Gallery) (GPL-3.0). Modified 2026 by [dsremo](https://github.com/dsremo).
