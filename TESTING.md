# TESTING.md — Automated Verification Suite for LRgetBD

All tests run locally on the JVM using **JUnit 4** and **Robolectric (`@Config(sdk = [36])`)** without requiring a physical device or ADB.

## Run Command
```bash
gradle :app:testDebugUnitTest
```

## Covered Scenarios (`LRgetBDRobolectricTest.kt`)
1. Exact `LRgetBD` application branding check.
2. Priority District dropdown check for **Cumilla (17 Upazilas)** and **Brahmanbaria (9 Upazilas)**.
3. Cascading reset (`Division → District → Upazila`) and **100% Auto-Select All Mouzas** + Download Confirmation prompt upon Upazila selection.
4. Batch handling for **0, 1, 7, 8, and 220 (200+) Mouzas**.
5. PDF Validator check: rejects HTML login pages (`HTML_LOGIN_OR_ERROR_PAGE`) and empty files (`EMPTY_FILE`), while accepting valid `%PDF-1.4` files with 64-char SHA-256 checksums.
6. Duplicate detection (`DUPLICATE`) and collision-safe archiving.
7. Queue JSON export/import and CSV batch processing utility.
8. Process restart recovery (`DOWNLOADING` → `INCOMPLETE`) and `AUTH_REQUIRED` handling.
