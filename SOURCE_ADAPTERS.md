# SOURCE_ADAPTERS.md — Modular Source Adapter Specification

`LRgetBD` enforces strict source verification and authentication safety via `LandRecordSourceAdapter`:

## 1. `VERIFIED_LOCAL_PORTAL_ADAPTER` (Enabled)
- **Purpose**: Processes user-authorized local staging files and generates spec-compliant archival record sheets for offline indexing and end-to-end archive verification.
- **Supported Surveys**: `CS`, `SA`, `RS`, `BS/BRS`, `DIARA`, `PETTY`
- **Supported Record Types**: `KHATIAN`, `MOUZA_MAP`, `DAG_INDEX`
- **Validation**: Every file is streamed to `.part` first, verified for `%PDF-` magic header bytes, rejected if HTML/empty, and hashed with SHA-256.

## 2. `UNVERIFIED_GOV_BULK_SCRAPER` (Disabled by Policy)
- **Purpose**: Documents why direct automated scraping of government portals (DLRS / e-Porcha) is disabled.
- **Reason Disabled**: Official government portals require interactive citizen login, NID verification, OTP, and CAPTCHA per record. Bypassing CAPTCHAs or rate limits is prohibited by `LRgetBD` safety rules.
