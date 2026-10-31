# ARCHITECTURE.md — LRgetBD System Architecture

## Overview
`LRgetBD` follows **Clean MVVM Architecture** with unidirectional data flow:

1. **UI Layer (`com.example.ui.*`)**:
   - Jetpack Compose + Material 3 Bangla-first screens (`MainActivity.kt`, `AreaSelectionSection.kt`, `QueueAndTelemetrySection.kt`, `DownloadConfirmationDialog.kt`).
   - Observes reactive `StateFlow` streams from `LRgetBDViewModel` using `collectAsStateWithLifecycle()`.
2. **Domain & Queue Engine (`com.example.storage.download.RealDownloadQueueEngine`)**:
   - Conservative 2-worker coroutine semaphore (`MAX_CONCURRENT_WORKERS = 2`) with respectful inter-batch rate limiting.
   - 10-step download & verification pipeline (`.part` staging → `%PDF-` signature check → HTML login page rejection → SHA-256 digest → duplicate check → atomic move to SAF/phone storage → Room audit log).
3. **Persistence & File Layer (`com.example.data.db.LRgetBDDatabase`, `com.example.storage.ArchiveStorageManager`)**:
   - Room SQLite tables (`download_jobs`, `audit_logs`) persist queue state across process restarts.
   - Android Storage Access Framework (`OpenDocumentTree`) writes validated PDFs, `manifest.json`, CSV/JSON reports, and Master ZIP packages to user-controlled phone memory.
