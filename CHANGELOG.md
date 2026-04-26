# Changelog

## [1.0.2] - 2026-04-14

- Fix performance regression in GUI: progress callbacks now dispatch to the main thread via `scope.launch` instead of writing Compose state directly from IO threads
- Fix O(n²) string growth in output log — replaced string concatenation with `StringBuilder`
- Progress bar now updates at 1% intervals (adaptive to collection size) instead of fixed every 50 files
- Remove per-directory logging that ran sequentially before file processing began
- Reduce footer height in GUI

## [1.0.1] - 2026-04-05

- Scrollable output log panel in the GUI showing real-time sort progress
- Ko-fi funding button in the GUI and README (replaced GitHub Sponsors)
- Added `.github/FUNDING.yml` with Ko-fi support link

## [1.0.0] - 2026-02-26

- Kotlin 2.3.10, JDK 25, Gradle 9.3.1
- Compose Multiplatform GUI with Material 3
- Native directory picker via FileKit
- EXIF/metadata date sorting for JPEG, HEIC/HEIF, MP4, MOV, MPEG
- Additional format support: PNG, GIF, BMP, WebP, AVIF, CR2, NEF, RW2, and more
- Concurrent file processing via structured coroutines
- Duplicate handling with automatic renaming (_1, _2)
- Progress reporting in CLI and GUI
- Clean log output to sorter.log
- Dual JAR output (CLI + GUI)
