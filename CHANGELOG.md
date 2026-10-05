# Changelog

## [5.1.0] - 2026-10-05

### 🛡️ Enterprise Hardening & Resilience
- **Cooperative Cancellation:** Audited all coroutine worker loops (`PortScanner`, `ScannerController`, `ScannerService`, `ScanViewModel`, `ToolsViewModel`) to explicitly rethrow `CancellationException`, eliminating zombie workers and spurious cancellation errors.
- **Strict RFC 1123 Validation:** Tightened target parser in `ScanViewModel` to require all-numeric dotted strings to strictly validate as IPv4, preventing invalid inputs from passing as hostnames.
- **Concurrency & State Protection:** Added multi-click concurrency guards to `startScan()` and ensured pending network probes cancel immediately on `stopScan()`.
- **Filesystem Sanitization:** Stripped Windows illegal characters (`:`, `/`, `\`, `?`, `*`, `"`, `<`, `>`, `|`) from export filenames to guarantee report generation stability.
- **Timestamp Formatting:** Formatted report hour and minute outputs with standard two-digit zero-padding.

### 🌐 Bilingual Architecture & Community Standards
- **Production Documentation:** Added comprehensive English `README.md` and dedicated RTL Persian mirror (`README.fa.md`) with Mermaid concurrency pipelines and RFC citations.
- **Open Source Governance:** Added Apache 2.0 `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, and GitHub issue/PR templates.
- **Regional Market Localization:** Added `values-fa/strings.xml` for native app naming on Iranian app stores (Cafe Bazaar & Myket).

### ⚡ Desktop & Android Tuning
- **High-Throughput JVM Network Stack:** Configured desktop JVM flags for IPv4 stack priority, non-exclusive socket binding, and UTF-8 encoding.
- **Android 14+ FGS Compliance:** Enforced `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` in `ScannerService` with hardware acceleration enabled.
- **Automated QA:** Added `PresentationAdversarialTest` covering target validation, port bounds, and report generation.

---

## [5.0.0-ULTRA] - 2026-06-21

### 🚀 Massive Performance Upgrade (The "Ultra Engine")
- **Decoupled Scanning Engine**: Refactored the core logic to separate port discovery from service detection. Scanning speed is no longer limited by slow banner-grabbing responses.
- **Adaptive RTT Layer**: Introduced dynamic timeout calculation based on real-time network latency (RTT).
- **Neural-Inspired Timing**: Added `AdaptiveTiming` with "Turbo Mode" exploration for higher throughput on stable networks.
- **Bounded Channels**: Implemented backpressure using Kotlin Channels to ensure memory stability during full 65k port scans.

### 🎨 UI & Branding
- **Modern Iconography**: Integrated new professional high-tech icons across all platforms (Android, Windows, macOS, Linux).
- **Compose Resources Migration**: Migrated to the modern `org.jetbrains.compose.resources` library for centralized resource management.

### 🔧 Fixes & Refinement
- **DNS Caching**: Target hostnames are now resolved once per scan.
- **Accuracy Retries**: Added automatic retries for "filtered" ports in high-performance modes.
- **Warning Clean-up**: Resolved numerous compiler and deprecation warnings across the codebase.

---

## [4.0.0] - Previous Version
- Initial implementation of the KMP Port Scanner.
- Basic TCP/UDP scanning support.
- Initial Compose Multiplatform UI.
