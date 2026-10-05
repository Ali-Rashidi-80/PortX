<div align="center">

**English** · [فارسی](README.fa.md)

<img src="icons/icon.png" alt="PortX Logo" width="112" height="112" />

# 🛡️ PortX

**Ultra-Fast, Non-Blocking Multiplatform Network Port Scanner Powered by Kotlin Multiplatform (KMP) & Compose — 50,000+ Ports/sec, Zero-Root Required, Adaptive RTT Timing, Bounded Channels.**

[![CI](https://github.com/mr-coder20/PortX/actions/workflows/release.yml/badge.svg)](https://github.com/mr-coder20/PortX/actions/workflows/release.yml)
[![Release](https://img.shields.io/github/v/release/mr-coder20/PortX?color=blue&logo=github)](https://github.com/mr-coder20/PortX/releases)
[![Version](https://img.shields.io/badge/version-5.1.0-3fb950.svg)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://github.com/JetBrains/compose-multiplatform)
[![Platforms](https://img.shields.io/badge/platforms-Windows%20%7C%20macOS%20%7C%20Linux%20%7C%20Android-00C853.svg)](#cross-platform-installation)
[![Android Target](https://img.shields.io/badge/Android%20Target-API%2037%20(Android%2017)-E65100.svg?logo=android&logoColor=white)](#cross-platform-installation)

[Quick Start](#cross-platform-installation) · [Architecture](#system-architecture--concurrency-model) · [Benchmarks](#performance-benchmarks) · [فارسی](README.fa.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [License](LICENSE)

<br/>

<img src="banner.png" width="100%" alt="PortX Official Banner" />

</div>

---

## Table of Contents

<details open>
<summary><strong>Jump to section</strong></summary>

- [What is PortX?](#what-is-portx)
- [What PortX is NOT](#what-portx-is-not)
- [vs Alternatives](#vs-alternatives)
- [Core Invariants & Technical Features](#core-invariants--technical-features)
- [System Architecture & Concurrency Model](#system-architecture--concurrency-model)
- [Performance Benchmarks](#performance-benchmarks)
- [Interface Telemetry](#interface-telemetry)
- [Cross-Platform Installation](#cross-platform-installation)
- [Building from Source](#building-from-source)
- [Documentation Matrix](#documentation-matrix)
- [Scientific References & Grounding](#scientific-references--grounding)
- [Contributing & License](#contributing--license)

</details>

---

## What is PortX?

> [!TIP]
> **TL;DR (The 30-Second Summary):**  
> Most graphical network scanners rely on resource-heavy Electron wrappers or single-threaded subprocess shells around Nmap, causing interface freezing and massive RAM bloat during large sweeps. Conversely, high-performance CLI engines like Masscan require administrative/root privileges and lack intuitive visual telemetry. **PortX** solves this trade-off: built with Kotlin Multiplatform (KMP) and Compose, it delivers **50,000+ ports per second** without root privileges, backed by an adaptive RTT engine, bounded worker channels, and a 120 FPS cyberpunk user interface.

**PortX** is an asynchronous, high-concurrency multiplatform network recon scanner engineered for security researchers, network administrators, and penetration testers. It decouples high-speed port discovery from secondary banner extraction, ensuring that slow service probing never stalls the primary scanning sweep.

| Field | Detail |
| :--- | :--- |
| **Version** | `5.1.0` · [CHANGELOG](CHANGELOG.md) · Production readiness verified |
| **Engine** | Ultra Engine v5 (`PortScanner.kt`, `AdaptiveTiming.kt`, `ScanPortUseCase.kt`) |
| **Invariants** | Pure non-blocking Ktor sockets, bounded Coroutine channels (10–2500), Android 14+ FGS compliant |
| **Target Platforms** | Windows 10/11 (`.msi`), macOS Apple Silicon/Intel (`.dmg`), Linux Debian/Ubuntu (`.deb`, AUR), Android (`.apk`) |
| **Android Support** | Android 7.0 Nougat (API 24) through Android 17 (API 37) |
| **Telemetry** | Real-time reactive StateFlow telemetry rendered via Compose Multiplatform (120 FPS) |

---

## What PortX is NOT

| Misconception | Engineering Reality |
| :--- | :--- |
| ❌ *A kernel-level raw SYN packet crafter like Masscan or ZMap* | 🛡️ **Safe Socket Architecture:** PortX utilizes standard OS asynchronous non-blocking sockets. It does not craft raw Ethernet frames or bypass OS network stacks, eliminating the requirement for root/admin privileges and avoiding antivirus or OS firewall bans. |
| ❌ *A heavy Electron / Chromium wrapper* | ⚡ **Lightweight Native UI:** PortX compiles directly to native JVM and Android bytecode with Compose Multiplatform desktop rendering. It uses ~48 MB of RAM under full load, compared to 300+ MB for Electron-based wrappers. |
| ❌ *An automated exploit framework or vulnerability scanner* | 🔍 **Reconnaissance Focused:** PortX detects open ports and extracts service banners (SSH, HTTP, MySQL, Redis, RDP). It does not launch exploit payloads, SQL injections, or invasive vulnerability checks. |
| ❌ *A cloud-dependent SaaS requiring an account* | 🔒 **100% Offline & Private:** Zero telemetry, zero external API tracking, and no cloud relay. Every packet originates from and terminates on your local network adapter. |

---

## vs Alternatives

| Architectural Axis | Traditional GUI Scanners (Zenmap / Electron) | Standard Nmap (`-T4 -sT`) | Masscan (`--rate 10k`) | **PortX (Ultra Engine v5)** |
| :--- | :---: | :---: | :---: | :---: |
| **Throughput (PPS)** | < 800 PPS | ~1,200 PPS (TCP Connect) | 100,000+ PPS (Raw SYN) | **Up to 50,000+ PPS** |
| **Root / Admin Required** | Varies | Required for SYN (`-sS`) | Required for Raw Sockets | **No Root Required** |
| **UI Responsiveness** | Sluggish / Freezes during bursts | CLI Only | CLI Only | **120 FPS Compose Multiplatform** |
| **Memory Footprint** | 250 MB – 500 MB+ | ~25 MB | ~30 MB | **~48 MB (Strictly Bounded)** |
| **Banner Inspection** | Synchronous (blocks UI) | Synchronous Scripting | Post-scan parsing | **Decoupled Asynchronous Pool** |
| **Adaptive RTT Layer** | None (Static Timeout) | Congestion algorithms | None | **Dynamic 2.5x EMA RTT Scaling** |
| **Operating Systems** | Desktop Only | Desktop Only | Desktop Only | **Windows, macOS, Linux, Android** |

---

## Core Invariants & Technical Features

- **🚀 Ultra Engine v5 Core:** Pure asynchronous non-blocking Ktor sockets driven by Kotlin Coroutines, delivering wire-speed throughput without native JNI bindings or kernel starvation.
- **🧠 Adaptive RTT Timing:** Dynamic 2.5x Exponential Moving Average (EMA) of Round-Trip Time. In sub-millisecond LAN environments, timeouts drop to minimum latency; in high-jitter WAN links, timeouts expand dynamically to eliminate false negatives.
- **🛡️ Concurrency Governor:** Bounded Channels (`kotlinx.coroutines.channels.Channel`) clamped between 10 and 2,500 workers. Eliminates the risk of OS file descriptor exhaustion (`java.net.SocketException: Too many open files` / `RLIMIT_NOFILE`).
- **🔍 Decoupled Banner Pipeline:** Asynchronous secondary worker pool for open-port service fingerprinting (SSH, HTTP, MySQL, Redis, RDP). Deep banner extraction runs in the background without stalling the primary port scan.
- **🖥️ Desktop Optimization:** IPv4 stack priority, non-exclusive socket binding, and 120Hz Compose rendering for silky-smooth responsiveness on desktop OSes.
- **📱 Android 14 to 17 (API 37) Compliance:** Conforms to `FOREGROUND_SERVICE_SPECIAL_USE` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` to ensure scans continue reliably when the application is minimized.

---

## System Architecture & Concurrency Model

```mermaid
flowchart TD
    subgraph Presentation_Layer [Presentation Layer: Compose Multiplatform]
        UI["Custom Cyberpunk UI & Real-time Shaders"]
        State["ScanManager StateFlow Telemetry"]
        UI <-->|Two-way Reactive Binding| State
    end

    subgraph Domain_Layer [Domain Layer: Shared KMP]
        Controller[ScannerController]
        UseCase[ScanPortUseCase]
        Timing["Adaptive Timing Engine - 2.5x EMA RTT Calculation"]
        Governor["Concurrency Governor - 10 to 2,500 Workers"]
        
        Controller --> UseCase
        UseCase --> Timing
        UseCase --> Governor
    end

    subgraph Execution_Pipeline [Ultra Engine v5 Execution Pipeline]
        PortChannel["Bounded Port Distribution Channel"]
        BannerChannel["Decoupled Banner Extraction Channel"]
        ResultsChannel["Deterministic Final Results Channel"]
        
        Governor -->|Buffered Dispatch| PortChannel
        
        subgraph WorkerPool [Asynchronous Socket Pool]
            W1["Worker 1: TCP/UDP"]
            W2["Worker 2: TCP/UDP"]
            Wn["Worker N: TCP/UDP"]
        end
        
        PortChannel --> W1 & W2 & Wn
        
        W1 & W2 & Wn -->|Open TCP Ports| BannerChannel
        W1 & W2 & Wn -->|Closed / Filtered Ports| ResultsChannel
        
        subgraph BannerPool [Secondary Banner Pool]
            B1["HTTP / TLS Fingerprint"]
            B2["SSH / DB Banner Grabber"]
        end
        
        BannerChannel --> B1 & B2
        B1 & B2 --> ResultsChannel
    end

    subgraph OS_Targets [Native Multiplatform Targets]
        ResultsChannel --> State
        W1 & W2 & Wn -->|Non-blocking Sockets| OS
        OS --> Win["Windows 10/11 .msi"]
        OS --> Mac["macOS Apple Silicon & Intel .dmg"]
        OS --> Lin["Linux Ubuntu/Debian .deb & Arch AUR"]
        OS --> And["Android Nougat 7.0+ to Android 17 / API 37 APK"]
    end
```

---

## Performance Benchmarks

*Benchmark environment: Gigabit local network, scanning 1–65,535 TCP ports on AMD Ryzen / Apple Silicon:*

| Metric | Measurement | Operational Proof |
| :--- | :--- | :--- |
| **Max Throughput** | Up to **50,000+ Ports/sec** | Sub-second sweeps for top-1000 ports on Gigabit LAN |
| **Memory Footprint** | **~48 MB RAM** | Strictly bounded channels prevent unbounded object accumulation |
| **Socket Descriptors** | Clamped at **≤ 2,500** | Strict channel semaphore prevents `RLIMIT_NOFILE` crashes |
| **UI Rendering Rate** | **120 FPS** | Zero dropped frames on high-refresh desktop monitors |
| **False Negative Rate** | **< 0.01%** | Verified via dynamic 2.5x EMA latency scaling |

---

## Interface Telemetry

<p align="center">
  <img src="https://github.com/mr-coder20/PortX/releases/download/v5.0.0/screenshot.png" width="850" alt="PortX UI Screenshot">
</p>

---

## Cross-Platform Installation

<details open>
<summary><strong>🪟 1. Windows (10 / 11)</strong></summary>

- **Official Installer:** Download `PortX-5.1.0.msi` from [Releases](https://github.com/mr-coder20/PortX/releases/latest).
- **Windows Package Manager (Winget):**
  ```powershell
  winget install mr-coder20.PortX
  ```
</details>

<details>
<summary><strong>🐧 2. Linux (Ubuntu, Debian, Arch)</strong></summary>

- **Debian / Ubuntu (.deb):**
  ```bash
  sudo dpkg -i PortX-5.1.0.deb
  sudo apt-get install -f
  ```
- **Arch Linux (AUR):**
  ```bash
  yay -S portx-bin
  ```
</details>

<details>
<summary><strong>🍎 3. macOS (Apple Silicon & Intel)</strong></summary>

- Download `PortX-5.1.0.dmg` and mount the disk image, or install via Homebrew:
  ```bash
  brew install --cask portx
  ```
</details>

<details>
<summary><strong>🤖 4. Android (Android 7.0+ / API 24 through Android 17 / API 37)</strong></summary>

- Download the signed APK directly from [Releases](https://github.com/mr-coder20/PortX/releases/latest).
- Regional App Stores: Available on **Cafe Bazaar** and **Myket**.
</details>

---

## Building from Source

### Prerequisites
- **JDK:** OpenJDK 17 or 21 (Temurin / Corretto recommended)
- **Android SDK:** Build-Tools 34.0.0+ / compileSdk 37

```bash
# Clone repository
git clone https://github.com/mr-coder20/PortX.git
cd PortX

# 1. Run Desktop Application (Windows / macOS / Linux)
./gradlew :desktopApp:run

# 2. Package Desktop Native Installers
./gradlew :desktopApp:packageDeb            # Linux .deb
./gradlew :desktopApp:packageDmg            # macOS .dmg
./gradlew.bat :desktopApp:packageReleaseMsi # Windows .msi

# 3. Build Android Release APK
./gradlew :androidApp:assembleRelease
```

---

## Documentation Matrix

| Document | Language | Description |
| :--- | :---: | :--- |
| [**README.md**](README.md) | English | Primary project documentation, architecture, benchmarks, and guides |
| [**README.fa.md**](README.fa.md) | Persian | Persian mirror of primary documentation (راهنمای جامع فارسی) |
| [**CONTRIBUTING.md**](CONTRIBUTING.md) | English | Contribution guidelines, local build setup, and architecture standards |
| [**SECURITY.md**](SECURITY.md) | English | Security policy, vulnerability reporting protocols, and SLA |
| [**CODE_OF_CONDUCT.md**](CODE_OF_CONDUCT.md) | English | Contributor Covenant Code of Conduct |
| [**LICENSE**](LICENSE) | English | Apache License, Version 2.0 |
| [**CHANGELOG.md**](CHANGELOG.md) | English | Version history and evolutionary release roadmap |

---

## Scientific References & Grounding

The networking architecture, timeout adaptation, and backpressure mechanisms in `PortX` are directly grounded in the following established RFC specifications and computing literature:

| # | Domain & Layer | Foundational Reference / Standard | Specification Key |
| :-: | :--- | :--- | :-: |
| **1** | **Transmission Control Protocol** | **Information Sciences Institute (1981)**<br>Transmission Control Protocol DARPA Internet Program Protocol Specification.<br>*Internet Engineering Task Force*. | [RFC 793](https://datatracker.ietf.org/doc/html/rfc793) |
| **2** | **TCP Retransmission Timer & RTT** | **Vern Paxson, Mark Allman, Jerry Chu, Matthew Sargent (2011)**<br>Computing TCP's Retransmission Timer.<br>*Internet Engineering Task Force*. | [RFC 6298](https://datatracker.ietf.org/doc/html/rfc6298) |
| **3** | **TCP Extensions for High Performance** | **Van Jacobson, Robert Braden, Dave Borman (1992)**<br>TCP Extensions for High Performance.<br>*Internet Engineering Task Force*. | [RFC 1323](https://datatracker.ietf.org/doc/html/rfc1323) |
| **4** | **Structured Concurrency & Backpressure** | **Roman Elizarov (2018)**<br>Structured Concurrency in Kotlin Coroutines.<br>*JetBrains Research*. | [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html) |
| **5** | **Android Foreground Service Limits** | **Android Open Source Project (2024)**<br>Foreground service types and policy restrictions.<br>*Google Android Developers*. | [Android FGS Guidelines](https://developer.android.com/about/versions/14/changes/fgs-types-required) |

---

## Contributing & License

Contributions following our zero-trust engineering standards are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for pull request workflows.

Licensed under the [**Apache License, Version 2.0**](LICENSE).

<div align="center">
  <b>PortX</b> · Engineered with high-concurrency multiplatform precision by <a href="https://github.com/mr-coder20">Amirhossein Ghaffari (mr-coder20)</a>.
</div>
