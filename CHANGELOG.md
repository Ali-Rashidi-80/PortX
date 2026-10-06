# Changelog

## [5.2.0] - 2026-10-06

### 🔬 Empirical Benchmarking & Performance Telemetry
- **8-Suite Live Benchmark Harness:** Implemented and executed automated real-time test suite (`LiveSystemBenchmarkTest.kt`) covering Concurrency Saturation (up to 2,500 workers), 10,000-port sustained sweep (8,143+ ports/sec with 17 MB RAM delta), Q-Learning adaptive timing, IPv4/IPv6 dual-stack efficiency, banner grabbing latency profiling, security posture evaluation (56,000+ ops/sec), multi-format report serialization (CSV, Markdown, JSON), and non-blocking UDP probe dispatch.
- **Official Benchmark Publication:** Authored and published [BENCHMARKS.md](BENCHMARKS.md) providing full hardware telemetry, scaling curves, and deterministic CLI reproducibility instructions.

### 🛡️ Core Engine, Security & Android Hardening
- **False-Positive Root Detection Elimination:** Replaced naive process output checks with strict exit code validation (`exitValue == 0`) and filtered out error messages like `which: no su in ...`, expanding binary search paths to Magisk/SuperSU.
- **Android 13+ Notification Safety:** Guarded all notification channel updates in `ScannerService` against `SecurityException` when `POST_NOTIFICATIONS` permission is revoked.
- **Exhaustive Private Network Classification:** Implemented `isTargetLocalOrPrivate` covering RFC 1122 (127.0.0.0/8), RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16), RFC 3927 (169.254.0.0/16), RFC 6598 (CGNAT 100.64.0.0/10), RFC 4193/4291 (IPv6 ULA/Link-local), and local domain suffixes (.local, .lan, .internal, .home.arpa).
- **Report Markdown Table Sanitization:** Sanitized extracted HTML `<title>` tags by replacing pipe `|` characters with `/` and clamping strings to 120 chars to eliminate Markdown table disruption.
- **Cloud & Industrial Threat Signatures:** Added port signatures for Docker Daemon (2375), Elasticsearch (9200), Kubernetes Kubelet (10250), Memcached (11211), Modbus/TCP (502), Siemens S7comm (102), and BACnet (47808) into `SecurityScoreUseCase` and `AnomalyDetectionUseCase`.
- **Database Schema Migration:** Added automated SQLite user version tracking (`PRAGMA user_version`) and forward-compatible column migrations in desktop runtime initialization.

### 📦 Desktop Packaging & CI/CD
- **Native Desktop Shortcuts:** Configured Compose desktop packaging to automatically generate desktop icons and start-menu shortcuts on Windows (`.msi`), Linux (`.deb`), and macOS (`.dmg`).
- **Comprehensive Unit Test Suites:** Added dedicated test suites for `DatabaseAdaptersTest.kt` (custom SQLite column adapters), `ScanManagerTest.kt` (flow state and UI event dispatcher), `SecurityHardenTest.kt` (encryption involution and client factory), `DeviceFingerprintUseCaseTest.kt` (heuristic device and OS classification across SCADA, Printers, Routers, NAS, Cloud, and DBs), `FirewallDetectionUseCaseTest.kt` (perimeter exposure evaluation), `ScanPortUseCaseTest.kt` (orchestration and automatic persistence), and `RemoteApiTest.kt` (threat intel and CVE lookup models).
- **Continuous Integration Workflow:** Created `.github/workflows/ci.yml` for automated multiplatform JVM testing, desktop compilation, and Android APK builds on pull requests and pushes.
- **Automated Dependency Auditing:** Added `.github/dependabot.yml` for automated weekly security vulnerability scans across Gradle dependencies and GitHub Actions.

---

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
