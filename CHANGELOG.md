# Changelog

## [5.3.0] - 2026-10-09

### 🔀 Stealth Mode & Firewall/WAF Evasion
- **Fisher-Yates Port Randomization:** Implemented randomized port distribution in `PortScanner.kt` to prevent sequential port access patterns that trigger stateful firewall, IPS, and IDS heuristic rate-limiting rules.
- **WAF Evasion HTTP Prober:** Dynamically rotates modern browser User-Agents (Chrome 122, Firefox 123, Safari 17.3, Edge 122) and realistic HTTP headers (`Accept`, `Accept-Language`, `Sec-Ch-Ua`, `Connection: close`) combined with 1–3ms jitter delays during HTTP service banner extraction.
- **4,000-Worker Full-Range Scaling:** Scaled the concurrency ceiling from 2,500 to 4,000 workers specifically during Full Range (1–65,535) sweeps, reducing full-port discovery duration while maintaining zero socket exhaustion.

### 🌐 Network Intelligence Suite (DNSSEC, DoH & CIDR)
- **DNS-over-HTTPS (DoH) & DNSSEC:** Added native encrypted DoH resolution via Cloudflare and Google endpoints, parsing DNSSEC Authenticated Data (`AD`) and Checking Disabled (`CD`) validation flags alongside EDNS support.
- **Advanced Dig DNS Resolver:** Integrated multi-type DNS querying supporting `A`, `AAAA`, `MX`, `TXT`, `NS`, `CNAME`, `SOA`, `PTR`, `SRV`, and `CAA` record types.
- **Resilient TCP Ping:** Added TCP socket connection latency checks as a resilient fallback when ICMP echo packets are blocked by firewalls or restricted non-root environments.
- **Subnet & CIDR Calculator:** Integrated network subnetting calculator resolving network address, broadcast address, netmask, wildcard mask, and usable host count for arbitrary CIDR prefixes.

### 🔔 Multiplatform Rich Notifications
- **Android Foreground Service (`ScannerService`):**
  - Configured official app launcher icon (`R.mipmap.ic_launcher`) for active and completed scan notifications.
  - Bound direct-focus `PendingIntent` (`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`) to launch and focus `MainActivity` upon notification tap.
  - Implemented `BigTextStyle` expandable completion notification detailing target address, device heuristic, discovered open ports, security score, and posture grade.
- **Desktop SystemTray Notifications:** Added native desktop system tray notifications in `ScannerController.jvm.kt` providing completion telemetry and posture rating.

### 🎨 UI/UX Refinement & Attack Surface Reduction
- **Minimalist 3-Button Language Switcher:** Streamlined language selection in `SettingsScreen` to a compact horizontal row of `[EN]`, `[FA]`, and `[RU]` toggle buttons.
- **Attack Surface Minimization:** Removed internal system architecture diagnostics card from settings to prevent environmental fingerprinting.
- **Search Bar Decluttering:** Simplified search input on Dashboard and Tools screens, removing distracting quick-target chips in favor of focused, direct input.
- **Responsive Export Grid:** Redesigned report export actions into an adaptive 2x2 grid on mobile viewports with single-line ellipsis truncation, eliminating UI overflow on narrow screens.

### 🔬 Empirical Benchmarking & Comprehensive Test Harness
- **8-Suite Live Benchmark Harness:** Automated real-time test suite (`LiveSystemBenchmarkTest.kt`) covering Concurrency Saturation (up to 2,500 workers), 10,000-port sustained sweep (8,143+ ports/sec with 17 MB RAM delta), Q-Learning adaptive timing, IPv4/IPv6 dual-stack efficiency, banner grabbing latency profiling, security posture evaluation (56,000+ ops/sec), multi-format report serialization (CSV, Markdown, JSON), and non-blocking UDP probe dispatch.
- **Adversarial QA Suite:** Added `AdversarialQAVerificationTest.kt` verifying Android notification intents, BigTextStyle fields, 4,000-worker ceiling, stealth shuffling, DNSSEC DoH resolution, and responsive UI layouts.
- **Official Benchmark Publication:** Published [BENCHMARKS.md](BENCHMARKS.md) providing full hardware telemetry, scaling curves, and deterministic CLI reproducibility instructions.

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
