# SECURITY.md — Security, Privacy & Evidence Traceability Policy

1. **Zero Secret Leakage**:
   - No API keys, passwords, OTPs, national ID numbers, or session cookies are embedded in source code, logged, or committed to version control.
2. **No CAPTCHA / OTP Bypass**:
   - `LRgetBD` never bypasses CAPTCHAs, OTP verification, paywalls, or government access controls. When authentication or interactive verification is required, jobs transition to `AUTH_REQUIRED` or `ACCESS_RESTRICTED`.
3. **Byte-for-Byte Document Integrity**:
   - Downloaded PDFs are validated against HTML login/error pages and verified via SHA-256 checksums before being moved from `.part` staging to final archive folders.
4. **Honest Verification Statuses**:
   - Uses explicit statuses (`VALIDATED`, `DOWNLOADED_UNVALIDATED`, `DUPLICATE`, `NOT_FOUND`, `FAILED`, `INCOMPLETE`, `AUTH_REQUIRED`, `ACCESS_RESTRICTED`, `SOURCE_UNVERIFIED`) and never labels a file as legally certified merely because its PDF downloaded.
