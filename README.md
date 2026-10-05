# LR Mass Downloader (BulkLRdownload)

[![Build & Release Android APK](https://github.com/tapos-linx/BulkLRdownload/actions/workflows/build-apk.yml/badge.svg)](https://github.com/tapos-linx/BulkLRdownload/actions/workflows/build-apk.yml)
[![Latest Release](https://img.shields.io/github/v/release/tapos-linx/BulkLRdownload?color=gold&label=Direct%20APK%20Download)](https://github.com/tapos-linx/BulkLRdownload/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-090d16?logo=android&logoColor=d4af37)](#)
[![Theme](https://img.shields.io/badge/UI-Dark%20Luxury%20Gold%20%26%20Obsidian-2a200a?color=d4af37)](#)

A high-performance offline land record organizer, mass downloader, and master archive utility for Bangladesh land records (**CS, SA, RS, BRS**). Specially optimized for **Cumilla** (17 Upazilas) and **Brahmanbaria** (9 Upazilas) with zero missing Mouzas, managed concurrency queueing, and structured master ZIP packaging for Android phone storage.

---

## 📱 Direct Android APK Download

Get the ready-to-install Android APK directly on your phone:

- **[⬇️ Download Latest APK (v1.0 Releases)](https://github.com/tapos-linx/BulkLRdownload/releases/latest)**
- Every commit pushed to the `main` branch automatically builds a signed/debug `.apk` and attaches it to GitHub Releases via our CI/CD pipeline.

---

## 🌟 Key Features

### 1. Zero Missing Mouzas & Cascading Selection
- **Cumilla (17 Upazilas)**: Adarsha Sadar, Sadar Dakshin, Barura, Brahmanpara, Burichang, Chandina, Chauddagram, Daudkandi, Debidwar, Homna, Laksam, Lalmai, Meghna, Monohargonj, Muradnagar, Nangalkot, Titas (all 61 authentic mouzas included).
- **Brahmanbaria (9 Upazilas)**: Brahmanbaria Sadar, Ashuganj, Nasirnagar, Nabinagar, Sarail, Kasba, Akhaura, Bancharampur, Bijoynagar.
- **100% Automatic Selection**: Selecting any Upazila immediately selects all associated Mouzas by default.
- **Record Types**: Multi-select support for **CS**, **SA**, **RS**, and **BRS** survey series.

### 2. Concurrency-Controlled Queue Engine
- **Anti-Throttling & Anti-Crash**: Restricts simultaneous downloads to **maximum 2 worker threads** to prevent HTTP 429 errors and device freezes.
- **Fault-Tolerant Auto-Retry**: Automatically retries dropped downloads up to 2 times without losing progress.
- **Interactive Controls**: Pause, Resume, and Clear Queue directly from the HUD.

### 3. Nested Phone Storage Folder Structuring (Master ZIP)
- Bundles completed records directly into a clean, hierarchical directory tree inside a ZIP archive:
  ```text
  Master_LR_Records/
  └── [District_Name]/
        └── [Upazila_Name]/
              └── [SurveyType]_[Mouza_Name]_(JL_[JLNo]).pdf
  ```
- Target phone storage path: `/Internal Storage/LR_Records/[District]/[Upazila]/`
- One-click **"Save Master ZIP to Phone"** trigger saving as `LR_Records_${district}_${upazila}.zip`.

### 4. Dark Luxury Gold & Obsidian UI
- **Color Palette**: Deep Obsidian Midnight (`#090d16`), Elevated Panels (`#141b2c`), and Metallic Warm Gold (`#d4af37` / `#f59e0b`).
- **Thumb-Friendly Touch Targets**: Generous 56px (`h-14`) menu dropdowns and buttons with high-contrast text for outdoor visibility.

---

## 🛠️ Local Development & Build

### Prerequisites
- **Node.js**: v20 or higher
- **JDK**: Java 17 (Temurin recommended)
- **Android SDK**: Build-tools 34+

### Setup Commands
```bash
# Clone the repository
git clone https://github.com/tapos-linx/BulkLRdownload.git
cd BulkLRdownload

# Install dependencies
npm install

# Run web development server
npm run dev

# Build web distribution assets
npm run build

# Synchronize with Capacitor Android
npx cap sync android

# Build Debug APK locally
cd android
./gradlew assembleDebug
```
The output APK will be generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 🚀 Automated GitHub Actions CI/CD Pipeline

The workflow defined in `.github/workflows/build-apk.yml` handles end-to-end continuous integration and deployment:
1. **Runner**: `ubuntu-latest`
2. **Environment**: Java 17 + Node.js 20 + Android SDK
3. **Execution**:
   - `npm run build`
   - `npx cap sync android`
   - `chmod +x android/gradlew`
   - `./gradlew assembleDebug --stacktrace`
4. **Publishing**:
   - Publishes `LR-Mass-Downloader.apk` to **GitHub Actions Artifacts**
   - Automatically releases a tagged GitHub Release (`v1.0.${{ github.run_number }}`) with the `.apk` attached.

---

## 📄 License
MIT License. Built for Land Record organization and offline preservation.
