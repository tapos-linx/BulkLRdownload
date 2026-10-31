# LRgetBD — Free-First Native Android Land Record Downloader

**LRgetBD** is a free-first, native Android application built with Kotlin, Jetpack Compose, Material 3, Room Database, and the Android Storage Access Framework (SAF) for organizing, downloading, validating, and archiving Bangladesh land records by administrative area (`Division → District → Upazila → Mouza`) and survey type (`CS`, `SA`, `RS`, `BS/BRS`).

## Key Features Implemented

1. **Cascading Area Selection (Cumilla & Brahmanbaria Priority)**:
   - Displays **Cumilla (কুমিল্লা — 17 Upazilas)** and **Brahmanbaria (ব্রাহ্মণবাড়িয়া — 9 Upazilas)** prominently at the top of the District selector alongside all Bangladesh divisions and districts.
   - Selecting any District automatically loads all of its related Upazilas.
   - Selecting any Upazila **automatically selects 100% of its Mouzas**, prepares the persistent download queue for the next process, and notifies the user with a confirmation dialog.
2. **Download Confirmation Notification & Dialog**:
   - Sends a system notification and displays an interactive confirmation dialog asking the user whether to start downloading immediately or choose a custom folder in phone memory first.
3. **Save Anywhere in Phone Memory (SAF Folder Picker)**:
   - Uses Android's `ActivityResultContracts.OpenDocumentTree()` (Storage Access Framework) so users can select any directory in internal storage or SD card.
   - Maintains a predictable archive layout:
     `LRgetBD/Bangladesh/Division/District/Upazila/Mouza/SurveyType/RecordType/validated-record.pdf`
4. **10-Step Validation & Persistent Queue Engine**:
   - Streams records to temporary `.part` files first.
   - Validates MIME type, `%PDF-` magic header bytes, and rejects HTML login/error pages or 0-byte responses.
   - Computes a full **SHA-256 checksum** for every file, detects duplicates, prevents filename overwrites, and records an immutable audit trail in SQLite (Room).
   - Supports **Start, Pause, Resume, Cancel, Retry Failed/Incomplete, Export Queue as JSON, Upload CSV Batch, and Save Master ZIP**.
5. **100% Free-First Architecture**:
   - Zero subscriptions, zero paid APIs, zero mandatory cloud backend, and zero embedded secrets.
