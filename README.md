# PicSort

**Your photos. Your computer. Nothing leaves.**

PicSort organizes your photos and videos into clean, date-based folders using EXIF and file metadata. It runs entirely on your machine — no cloud uploads, no accounts, no AI scanning your family photos, no data leaving your computer. Ever.

---

## Why PicSort?

Every major photo organizer wants your pictures in their cloud. Google Photos scans them for AI training. Apple locks them in iCloud. Amazon wants them on their servers.

PicSort doesn't want your photos. It just sorts them.

- **Fully offline** — no internet connection needed
- **No accounts** — no sign-up, no login, no tracking
- **No AI** — no facial recognition, no content analysis, no "smart" features that phone home
- **Open source (MIT)** — inspect every line, fork it, improve it

If you care about digital privacy, your photo library shouldn't live on someone else's server.

---

## What It Does

Point PicSort at a folder of unsorted photos and videos. It reads the metadata timestamps and copies everything into a clean structure. Each run gets its own timestamped folder inside the target, so runs never mix:

```
target/
└── 2026-06-15-143012/
    ├── pictures/
    │   ├── 2024/
    │   │   ├── 2024-01-15/
    │   │   │   ├── IMG_0042.jpg
    │   │   │   └── IMG_0043.heic
    │   │   └── 2024-03-22/
    │   │       └── vacation.jpg
    │   ├── 2025/
    │   │   └── 2025-06-01/
    │   │       └── birthday.heic
    │   └── wrong_date/
    │       └── 1980/
    │           └── 1980-01-01/
    │               └── IMG_0001.jpg
    ├── video/
    │   └── 2024/
    │       └── 2024-07-04/
    │           └── fireworks.mp4
    ├── other/
    │   └── screenshots/
    │       └── screenshot.png
    └── sorter.log
```

- Reads EXIF data (photos) and QuickTime/MP4 metadata (videos) for accurate dates
- Your originals are never moved or modified — everything is copied
- Handles duplicates automatically with `_1`, `_2` suffixes
- Dates before 1990 (usually a camera with an unset clock) go to `wrong_date/` for review
- Files without date metadata, and the formats listed below as `other/`, are copied to `other/`, keeping their original subfolder
- Files of unrecognized types are not copied — they're listed in the log so you can handle them
- Processes files concurrently for speed
- Logs everything to `sorter.log` in the run folder

---

## Supported Formats

| Category | Formats | Date Sorting |
|----------|---------|:------------:|
| Photos | JPEG, HEIC, HEIF | ✅ Via EXIF |
| Video | MP4, MOV, MPEG | ✅ Via metadata |
| Raw | CR2, NEF, RW2 | 📁 Copied to `other/` |
| Other images | PNG, GIF, BMP, WebP, AVIF | 📁 Copied to `other/` |
| Other video | AVI, WMV, FLV, 3GP, M2TS, M4V, MTS | 📁 Copied to `other/` |

---

## Getting Started

### Requirements

- **JDK 25** or later

### Build

```bash
./gradlew build
```

Produces two JARs in `build/libs/`:
- `picsort-cli-1.1.0.jar` — command-line interface
- `picsort-gui-1.1.0.jar` — desktop GUI

### GUI

```bash
java -jar build/libs/picsort-gui-1.1.0.jar
```

Pick your source and target folders, hit sort, and watch the live output log as each folder is processed.

![PicSort GUI](docs/screenshot.png)

### CLI

```bash
java -jar build/libs/picsort-cli-1.1.0.jar /path/to/photos /path/to/sorted
```

---

## Tech Stack

- **Kotlin** with coroutines for concurrent file processing
- **Compose Multiplatform** (Material 3) for the desktop GUI
- **metadata-extractor** for EXIF/video metadata parsing
- **FileKit** for native OS file dialogs

---

## Contributing

Contributions welcome. Open an issue first for anything non-trivial.

---

## Support the Project

PicSort is free and open source. If it saved you some time, consider buying me a coffee.

<a href="https://ko-fi.com/ngusev"><img src="docs/kofi.webp" alt="Buy me a coffee" width="180"></a>

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

You're free to use, modify, and distribute PicSort — including in commercial or closed-source projects. Just keep the copyright notice.
