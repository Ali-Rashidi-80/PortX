<div align="center">

**English** · [فارسی](README.fa.md)

<img src="icons/icon.png" alt="PortX Logo" width="112" height="112" />

# 🛡️ PortX

**Ultra-Fast, Non-Blocking Multiplatform Network Port Scanner Powered by Kotlin Multiplatform (KMP) & Compose — 10,000+ Ports/sec (Empirical) / Up to 50,000 (Theoretical Peak), Zero-Root Required, Stealth Port Shuffling, WAF/Firewall Evasion, Dynamic 4,000-Worker Pool & DNSSEC DoH Suite.**

[![CI](https://github.com/Ali-Rashidi-80/PortX/actions/workflows/ci.yml/badge.svg)](https://github.com/Ali-Rashidi-80/PortX/actions/workflows/ci.yml)
[![Release](https://img.shields.io/badge/release-v5.3.0-blue.svg?logo=github)](https://github.com/Ali-Rashidi-80/PortX/releases)
[![Version](https://img.shields.io/badge/version-5.3.0-3fb950.svg)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://github.com/JetBrains/compose-multiplatform)
[![Platforms](https://img.shields.io/badge/platforms-Windows%20%7C%20macOS%20%7C%20Linux%20%7C%20Android-00C853.svg)](#cross-platform-installation)
[![Android Target](https://img.shields.io/badge/Android%20Target-API%2037%20(Android%2017)-E65100.svg?logo=android&logoColor=white)](#cross-platform-installation)

[Quick Start](#cross-platform-installation) · [Architecture](#system-architecture--concurrency-model) · [Benchmarks](#performance-benchmarks) · [Network Tools](#built-in-network-intelligence-suite) · [Stealth & Evasion](#stealth-mode--firewallwaf-evasion) · [فارسی](README.fa.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [License](LICENSE)

<br/>

<img src="banner.png" width="100%" alt="PortX Official Banner" />

<br/><br/>

<img src="icons/demo.svg" width="100%" alt="PortX 120 FPS Radar and Real-Time Telemetry Demo" />

</div>

---

## Table of Contents

<details open>
<summary><strong>Jump to section</strong></summary>

- [What is PortX?](#what-is-portx)
- [What PortX is NOT](#what-portx-is-not)
- [vs Alternatives (Nmap, Masscan, RustScan, Naabu)](#vs-alternatives-architectural-advantages)
- [Core Invariants & Technical Features](#core-invariants--technical-features)
- [System Architecture & Concurrency Model](#system-architecture--concurrency-model)
- [Stealth Mode & Firewall/WAF Evasion](#stealth-mode--firewallwaf-evasion)
- [Built-in Network Intelligence Suite](#built-in-network-intelligence-suite)
- [System Notifications & Telemetry](#system-notifications--telemetry)
- [Performance Benchmarks](#performance-benchmarks)
- [Sample Telemetry Output (JSON & Markdown)](#sample-telemetry-output-json--markdown-export)
- [Declarative Service Signatures](#declarative-service-signatures-extensible-engine)
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
> Most graphical network scanners rely on resource-heavy Electron wrappers or single-threaded subprocess shells around Nmap, causing interface freezing and massive RAM bloat during large sweeps. Conversely, high-performance CLI engines like Masscan require administrative/root privileges and lack intuitive visual telemetry. **PortX** solves this trade-off: built with Kotlin Multiplatform (KMP) and Compose, it delivers **10,000+ ports per second empirically** (scalable up to 50,000 theoretical peak) without root privileges, backed by an adaptive RTT engine, bounded worker channels (scaled up to 4,000 on full sweeps), Fisher-Yates stealth randomization, modern WAF evasion, DNSSEC-validated DoH, and a 120 FPS cyberpunk user interface.

**PortX** is an asynchronous, high-concurrency multiplatform network recon scanner engineered for security researchers, network administrators, and penetration testers. It decouples high-speed port discovery from secondary banner extraction, ensuring that slow service probing never stalls the primary scanning sweep.

| Field | Detail |
| :--- | :--- |
| **Version** | `5.3.0` · [CHANGELOG](CHANGELOG.md) · Production readiness verified |
| **Engine** | Ultra Engine v5 (`PortScanner.kt`, `ScanPortUseCase.kt`, `ScannerController.kt`) |
| **Invariants** | Pure non-blocking Ktor sockets, bounded Coroutine channels (10–4,000), Android 14+ FGS compliant |
| **Stealth & Evasion** | Fisher-Yates port randomization, randomized browser User-Agents & headers, jitter delays |
| **Network Tools** | DNSSEC DoH (Cloudflare/Google), multi-record Dig, Resilient TCP Ping, Subnet/CIDR calculator |
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

## vs Alternatives: Architectural Advantages

PortX was engineered specifically to solve the fundamental trade-offs between low-level raw packet crafters (which require kernel root access and lack reactive UIs) and heavy graphical wrappers (which stall under load and consume hundreds of megabytes of RAM).

| Architectural Dimension | Traditional GUI (Zenmap / Electron) | Standard Nmap (`-sS` / `-sT`) | Masscan (`--rate 10k`) | RustScan (`-a <target>`) | Naabu (`-rate 1000`) | **PortX (Ultra Engine v5)** |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Throughput (PPS)** | < 800 PPS | ~1,200 PPS (`-sT`) | 100,000+ PPS | Subprocess-gated | ~1,000 – 5,000 PPS | **8,000 – 10,200+ PPS (Empirical)** |
| **Root / Admin Privilege** | Varies | Required for SYN (`-sS`) | **Mandatory** (`SOCK_RAW`) | Required for fast SYN | Required for SYN | **Zero-Root Required (Pure Sockets)** |
| **UI Telemetry & FPS** | Stutters (< 30 FPS) | CLI Only | CLI Only | CLI Only | CLI Only | **120 FPS Compose Multiplatform HUD** |
| **Memory Footprint** | 250 MB – 500 MB+ | ~25 MB | ~30 MB | ~20 MB + Nmap Heap | ~35 MB | **~48 MB Strictly Bounded Heap** |
| **Banner Fingerprinting** | Synchronous (blocks UI) | Sequential NSE Scripts | Post-scan external | Spawns external Nmap | Basic / Passive probes | **Decoupled Asynchronous Coroutine Pool** |
| **Adaptive RTT Layer** | None (Static Timeout) | Congestion algorithms | None (Blind drops) | None | Static rate-limiting | **Dynamic 2.5x EMA Latency Scaling** |
| **Stealth & Evasion** | None | Fragment / Decoy (`-D`) | Rate randomization | None | Rate limits | **Fisher-Yates Shuffle + Browser WAF Evasion** |
| **Custom Signatures** | Static port lists | Monolithic Lua scripts | None | Relies on Nmap NSE | YAML templates | **Declarative Signatures (Nuclei-style)** |
| **Network Tools Suite** | None | Separate CLI utils | None | None | None | **Built-in DNSSEC DoH, Dig, CIDR, TCP Ping** |
| **Cross-Platform Target** | Desktop Only | Desktop Only | Desktop Only | Desktop Only | Desktop Only | **Windows, macOS, Linux, Android (API 24–37)** |

### Why PortX Outperforms Other Recon Engines

1. **Zero-Root Multiplatform Mobility (vs Nmap `-sS` & Masscan):**  
   Both Masscan and Nmap’s SYN stealth mode (`-sS`) rely on raw packet injection (`SOCK_RAW`), requiring `sudo`, `root`, or `CAP_NET_RAW` on Linux and WinPcap/Npcap on Windows. On unrooted Android devices and enterprise corporate environments, kernel security policies (`SELinux untrusted_app`) strictly prohibit raw sockets. PortX achieves **10,000+ ports per second** using pure asynchronous non-blocking OS sockets via Ktor Coroutines, allowing unprivileged, audit-safe execution on any workstation or phone.

2. **Decoupled Asynchronous Banner Pipeline (vs RustScan & Sequential Nmap):**  
   RustScan accelerates initial port discovery but relies on spawning an external Nmap subprocess (`nmap -sV`) to extract banners, introducing heavy process creation overhead, IPC serialization latency, and inability to run inside sandboxed mobile OSes. PortX natively embeds a dual-stage reactive pipeline: as soon as a port connects, it is streamed to a decoupled background `BannerPool` without impeding the primary sweep rate.

3. **Stealth Port Randomization & Anti-Firewall Evasion (vs Sequential Scanners):**  
   Sequential scans (`1..65535`) trigger stateful firewall tripwires and threshold-based IDS/IPS rules immediately. PortX applies non-cryptographic, high-speed Fisher-Yates port shuffling to distribute probe destinations across the entire port space. Concurrently, HTTP service fingerprinting rotates modern browser User-Agents and realistic headers with jitter delays, preventing Web Application Firewalls (WAFs) from detecting automated scanner signatures.

4. **Adaptive RTT vs Blind Fixed Timeouts (vs Masscan & Naabu):**  
   Masscan fires packets blindly at fixed rates, resulting in massive packet drop and false negatives over lossy Wi-Fi or high-jitter VPN/WAN networks. Naabu relies on fixed timeouts. PortX continuously computes the **Exponential Moving Average (EMA) of Round-Trip Time with a 2.5x safety multiplier**. In low-latency gigabit LANs, timeouts drop to ~20ms; over intercontinental WANs, timeouts expand dynamically, ensuring zero false negatives without manual rate tuning.

5. **Dynamic 4,000-Worker Scaling (vs Static Throttling):**  
   During standard selective scans, workers are clamped between 10 and 2,500 to guarantee resource stability. When initiating a Full Range scan (1–65,535 ports), PortX dynamically scales the worker pool to 4,000 concurrent non-blocking coroutines, finishing full-spectrum sweeps in record time without hitting OS descriptor exhaustion (`RLIMIT_NOFILE`).

6. **Silky 120 FPS Native HUD (vs Heavy Electron & Frozen GUIs):**  
   Zenmap and Electron-based wrappers suffer from UI thread blocking when receiving thousands of port events per second. PortX leverages Compose Multiplatform’s reactive state pipeline (`StateFlow`), rendering silky 120 FPS radar animations and real-time telemetry while consuming under 50 MB of RAM.

---

## Core Invariants & Technical Features

- **🚀 Ultra Engine v5 Core:** Pure asynchronous non-blocking Ktor sockets driven by Kotlin Coroutines, delivering wire-speed throughput without native JNI bindings or kernel starvation.
- **🔀 Stealth Mode & Port Randomization:** High-speed Fisher-Yates port shuffling disperses port connection order across the scan space, evading sequential threshold IDS/IPS rules.
- **🛡️ WAF & Firewall Evasion Prober:** Rotates modern desktop browser User-Agents (Chrome 122, Firefox 123, Safari 17.3, Edge 122) and standard HTTP headers (`Accept`, `Accept-Language`, `Sec-Ch-Ua`, `Connection: close`) with jitter delays (1–3ms) during banner extraction.
- **⚡ Dynamic Concurrency Governor:** Bounded Channels (`kotlinx.coroutines.channels.Channel`) clamped between 10 and 2,500 workers for selective scans and dynamically expanded to **4,000 workers** during Full 65k sweeps.
- **🧠 Adaptive RTT Timing:** Dynamic 2.5x Exponential Moving Average (EMA) of Round-Trip Time. In sub-millisecond LAN environments, timeouts drop to minimum latency; in high-jitter WAN links, timeouts expand dynamically to eliminate false negatives.
- **🔍 Decoupled Banner Pipeline:** Asynchronous secondary worker pool for open-port service fingerprinting (SSH, HTTP, MySQL, Redis, RDP). Deep banner extraction runs in the background without stalling the primary port scan.
- **🌐 Built-in Network Intelligence Suite:** Integrated DNS-over-HTTPS (DoH) with DNSSEC validation, multi-type Dig queries, non-root TCP Ping latency testing, and IPv4/IPv6 Subnet & CIDR calculations.
- **🔔 Multiplatform Rich Notifications:** Android Foreground Service with official app launcher icon (`R.mipmap.ic_launcher`), direct focus `PendingIntent`, and expandable `BigTextStyle` completion cards; native SystemTray alerts on Desktop.
- **🎨 Minimalist, Attack-Surface Hardened HUD:** Minimalist 3-button language switcher (`[EN]`, `[FA]`, `[RU]`), responsive 2x2 report export grid on mobile viewports, decluttered search input, and elimination of sensitive system architecture diagnostics.
- **📱 Android 14 to 17 (API 37) Compliance:** Conforms to `FOREGROUND_SERVICE_SPECIAL_USE` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` to ensure scans continue reliably when the application is minimized.

---

## System Architecture & Concurrency Model

```mermaid
flowchart TD
    subgraph Presentation_Layer [Presentation Layer: Compose Multiplatform]
        UI["Custom Cyberpunk HUD & Real-time Shaders (120 FPS)"]
        LangToggle["Minimalist 3-Button Language Selector: [EN] [FA] [RU]"]
        ExportGrid["Responsive Adaptive 2x2 Export Grid"]
        State["ScanManager StateFlow Telemetry Pipeline"]
        UI <-->|Two-way Reactive Binding| State
        LangToggle --> UI
        ExportGrid --> UI
    end

    subgraph Domain_Layer [Domain Layer: Shared KMP Engine]
        Controller[ScannerController]
        UseCase[ScanPortUseCase]
        Timing["Adaptive Timing Engine - 2.5x EMA RTT Calculation"]
        Governor["Dynamic Concurrency Governor - 10 to 4,000 Workers"]
        NetSuite["Network Intelligence Suite: DoH / DNSSEC / Dig / CIDR / TCP Ping"]
        
        Controller --> UseCase
        UseCase --> Timing
        UseCase --> Governor
        Controller --> NetSuite
    end

    subgraph Execution_Pipeline [Ultra Engine v5 Execution Pipeline]
        StealthRandomizer["Fisher-Yates Port Shuffling (Stealth Mode)"]
        PortChannel["Bounded Port Distribution Channel"]
        BannerChannel["Decoupled Banner Extraction Channel"]
        ResultsChannel["Deterministic Final Results Channel"]
        
        Governor --> StealthRandomizer
        StealthRandomizer -->|Buffered Dispatch| PortChannel
        
        subgraph WorkerPool [Asynchronous Socket Pool - Up to 4k Workers]
            W1["Worker 1: Non-blocking Socket"]
            W2["Worker 2: Non-blocking Socket"]
            Wn["Worker N: Non-blocking Socket"]
        end
        
        PortChannel --> W1 & W2 & Wn
        
        W1 & W2 & Wn -->|Open TCP Ports| BannerChannel
        W1 & W2 & Wn -->|Closed / Filtered Ports| ResultsChannel
        
        subgraph BannerPool [Secondary Banner Pool & WAF Evasion]
            B1["WAF Evasion HTTP Prober: Rotating Browser UAs & Jitter"]
            B2["TLS / SSH / Redis / DB Fingerprint Engine"]
        end
        
        BannerChannel --> B1 & B2
        B1 & B2 --> ResultsChannel
    end

    subgraph Notification_Bridge [Multiplatform Notification Bridge]
        ResultsChannel --> State
        State --> NotifRouter{Platform Router}
        NotifRouter -->|Android| FGS["ScannerService: ic_launcher + PendingIntent + BigTextStyle"]
        NotifRouter -->|Desktop| Tray["SystemTray Completion Telemetry Alert"]
    end

    subgraph OS_Targets [Native Multiplatform Targets]
        W1 & W2 & Wn -->|Standard Non-blocking Sockets| OS
        OS --> Win["Windows 10/11 .msi"]
        OS --> Mac["macOS Apple Silicon & Intel .dmg"]
        OS --> Lin["Linux Ubuntu/Debian .deb & Arch AUR"]
        OS --> And["Android Nougat 7.0+ to Android 17 / API 37 APK"]
    end
```

---

## Stealth Mode & Firewall/WAF Evasion

PortX is engineered to operate in environments protected by intrusion detection systems (IDS), firewalls, and application proxies without relying on destructive packet fragmentation:

```
[Target Port Space] ───────► [Fisher-Yates Shuffle] ───────► [Dispersed Non-Linear Sweep]
                                                                     │
[HTTP Open Port]    ───────► [Browser UA & Headers Rotation] ────────┼───► [WAF Bypass]
                                                                     │
                             [1-3ms Micro-Jitter Delay]      ────────┘
```

1. **Fisher-Yates Port Shuffling:** Instead of scanning sequential ports (`80, 81, 82...`), PortX applies a high-performance Fisher-Yates array permutation (`ports.shuffled()`). Port probes hit destination sockets in pseudo-random order, preventing threshold-based detection rules from flagging contiguous sweep signatures.
2. **Dynamic Browser User-Agent Rotation:** When querying HTTP/HTTPS ports (80, 443, 8080, 8443, etc.), the prober rotates modern desktop User-Agents across Google Chrome, Mozilla Firefox, Apple Safari, and Microsoft Edge, bypassing standard bot-blocking rules.
3. **Realistic HTTP Header Headers:** In addition to User-Agent rotation, probe requests include authentic headers:
   - `Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8`
   - `Accept-Language: en-US,en;q=0.9`
   - `Sec-Ch-Ua: "Chromium";v="122", "Not(A:Brand";v="24"`
   - `Connection: close`
4. **Adaptive Micro-Jitter Delays:** When stealth mode is engaged, inter-probe dispatch introduces micro-delays (1–3ms) to defeat rate-based anomaly filters.

---

## Built-in Network Intelligence Suite

PortX includes a suite of integrated network diagnostics accessible via the **Tools** tab:

| Tool | Protocol / RFC | Capabilities | Use Case |
| :--- | :--- | :--- | :--- |
| **DNS-over-HTTPS (DoH)** | [RFC 8484](https://datatracker.ietf.org/doc/html/rfc8484) | Encrypted queries via Cloudflare & Google DoH; JSON & wireformat | Bypass ISP DNS poisoning, inspect DoH resolution latency |
| **DNSSEC Validator** | [RFC 4035](https://datatracker.ietf.org/doc/html/rfc4035) | Parses Authenticated Data (`AD`) & Checking Disabled (`CD`) flags | Verify cryptographic integrity of domain zone records |
| **Dig DNS Query Engine** | [RFC 1035](https://datatracker.ietf.org/doc/html/rfc1035) | Queries `A`, `AAAA`, `MX`, `TXT`, `NS`, `CNAME`, `SOA`, `PTR`, `SRV`, `CAA` | Rapid zone inspection and mail/domain record auditing |
| **Resilient TCP Ping** | TCP Handshake | Connects to standard ports with high-resolution RTT latency timer | Latency testing when firewalls drop ICMP echo packets |
| **Subnet & CIDR Calculator** | [RFC 4632](https://datatracker.ietf.org/doc/html/rfc4632) | Calculates Network, Broadcast, Netmask, Wildcard, Usable Hosts | Planning scan boundaries and IP allocation ranges |
| **GeoIP & ASN Lookup** | HTTP / BGP Routing | External IP discovery, country code, ISP, Autonomous System Number | Auditing egress network routes and cloud datacenter origins |

---

## System Notifications & Telemetry

PortX keeps you informed of scan progress and completion through native OS integration:

### 📱 Android Foreground Service
- **Official App Icon:** Displays `R.mipmap.ic_launcher` in the Android status bar and notification drawer during active sweeps.
- **Direct App Focus:** Built with `PendingIntent` (`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`) so that tapping the notification instantly restores and focuses `MainActivity`.
- **Rich `BigTextStyle` Results:** Upon scan completion, an expandable notification displays:
  - Target Host & Resolved IP
  - Device Classification & Heuristic OS
  - Discovered Open Ports Count & Port List
  - Security Posture Score & Letter Grade (A–F)

### 🖥️ Desktop SystemTray Alerts
- Native system tray balloon/toast alerts on Windows, macOS, and Linux summarizing scan elapsed time, total open ports, and security posture rating.

---

## Performance Benchmarks

*Empirical Measurements on Local Loopback & Gigabit LAN (AMD Ryzen / Intel Core i3-12100 / Apple Silicon, Ktor Non-Blocking IO):*

| Metric | Measured Baseline | Operational Architecture Constraint |
| :--- | :--- | :--- |
| **Throughput (LAN/Loopback)** | **8,000 – 10,200+ Ports/sec** | Sub-second sweep of top 1,000 ports (10,000 ports in ~1.15s on Concurrency 1,000) |
| **Full 65k Scan Scaled Throughput** | Up to **12,500+ Ports/sec** | Dynamic 4,000-worker concurrency pool for Full 1–65535 sweeps |
| **Theoretical Peak** | Up to **50,000 Ports/sec** | Hard-bounded by OS socket buffer and ephemeral port allocation limits |
| **Memory Footprint** | **~17 – 26 MB Heap Delta** | Strictly bounded Kotlin Channel buffers prevent unbounded heap accumulation |
| **Socket Descriptors** | Clamped at **≤ 4,000** | Semaphore ceiling eliminates OS descriptor exhaustion (`RLIMIT_NOFILE`) |
| **UI Rendering Rate** | **120 FPS** | Non-blocking coroutine dispatchers keep Compose desktop main thread fluid |
| **False Negative Rate** | **< 0.01%** | Verified via dynamic 2.5x EMA latency scaling and retry pass on filtered ports |

> 📊 **Full Benchmark Telemetry:** See [BENCHMARKS.md](BENCHMARKS.md) for full hardware telemetry, scaling curves, and GC memory profiling across all 8 benchmark suites.  
> 🔬 **Reproduce Locally:** Run `./gradlew :shared:jvmTest --tests "com.mrcoder20.portx.data.network.LiveSystemBenchmarkTest" --rerun-tasks`

### Sample Telemetry Output (JSON & Markdown Export)

PortX provides structured multi-format serialization (JSON, Markdown Table, CSV) with deep banner extraction, heuristic OS fingerprinting, and automated vulnerability risk scoring:

#### 1. Machine-Readable JSON Export (`--export json` / API Output)

```json
{
  "target": "192.168.1.1",
  "hostname": "gateway.local",
  "ports_scanned": 1000,
  "elapsed_ms": 112,
  "throughput_pps": 8928,
  "stealth_mode": true,
  "device_classification": {
    "device_type": "Embedded Gateway / Linux Router",
    "heuristic_os": "Linux 6.6.x (OpenWrt / Alpine)",
    "confidence": 0.94
  },
  "open_ports": [
    {
      "port": 22,
      "protocol": "TCP",
      "state": "OPEN",
      "service": "ssh",
      "version": "OpenSSH 9.6p1",
      "banner": "SSH-2.0-OpenSSH_9.6p1 Ubuntu-3ubuntu13",
      "latency_ms": 2.1,
      "cve_risk_score": 0.0,
      "risk_level": "LOW"
    },
    {
      "port": 80,
      "protocol": "TCP",
      "state": "OPEN",
      "service": "http",
      "version": "nginx/1.24.0",
      "title": "Router Admin Gateway",
      "banner": "HTTP/1.1 200 OK\r\nServer: nginx/1.24.0",
      "latency_ms": 1.8,
      "cve_risk_score": 3.2,
      "risk_level": "MEDIUM"
    },
    {
      "port": 443,
      "protocol": "TCP",
      "state": "OPEN",
      "service": "https",
      "version": "TLS 1.3 / OpenSSL 3.1.4",
      "banner": "HTTP/1.1 200 OK\r\nServer: nginx/1.24.0\r\nStrict-Transport-Security: max-age=31536000",
      "latency_ms": 2.4,
      "cve_risk_score": 0.0,
      "risk_level": "LOW"
    },
    {
      "port": 502,
      "protocol": "TCP",
      "state": "OPEN",
      "service": "modbus",
      "version": "Modbus/TCP Industrial Node",
      "banner": "Schneider Electric Modbus/TCP Gateway 0x01",
      "latency_ms": 3.5,
      "cve_risk_score": 8.5,
      "risk_level": "CRITICAL"
    }
  ],
  "security_posture_score": 45,
  "posture_assessment": "EXPOSED_INDUSTRIAL_CONTROL_ENDPOINT"
}
```

#### 2. Human-Readable Clean Markdown Output (`--export markdown` / Clipboard)

| Target | `192.168.1.1` (`gateway.local`) |
| :--- | :--- |
| **Identified OS** | **Linux 6.6.x (OpenWrt / Alpine)** · Confidence `94%` |
| **Scan Summary** | `1,000` ports scanned in `112 ms` · `8,928 PPS` · `0` descriptor faults |
| **Posture Rating** | **45/100** · Critical Risk (Exposed ICS/SCADA Endpoint) |

| Port | Protocol | State | Service | Identified Banner & Product | Latency | Vulnerability Risk |
| :---: | :---: | :---: | :---: | :--- | :---: | :---: |
| `22` | TCP | `OPEN` | `ssh` | OpenSSH 9.6p1 (`Ubuntu-3ubuntu13`) | 2.1 ms | `LOW (0.0)` |
| `80` | TCP | `OPEN` | `http` | nginx/1.24.0 (Title: *Router Admin Gateway*) | 1.8 ms | `MED (3.2)` |
| `443` | TCP | `OPEN` | `https` | TLS 1.3 / OpenSSL 3.1.4 (HSTS Enabled) | 2.4 ms | `LOW (0.0)` |
| `502` | TCP | `OPEN` | `modbus` | Schneider Electric Modbus/TCP Gateway 0x01 | 3.5 ms | `CRIT (8.5)` |

---

### Declarative Service Signatures (Extensible Engine)

Traditional scanners rely on complex, single-threaded Lua scripting (Nmap NSE) which causes runtime execution overhead and maintenance friction. PortX adopts modern **Declarative Service Signatures** inspired by Nuclei templates:

- **Zero-Code Fingerprinting:** Add new services, ICS protocols, or proprietary cloud daemons via declarative YAML or JSON without recompiling the scanner.
- **Multi-Vector Matching:** Match services across port ranges, probe payload hex bytes, raw banner substrings, and high-performance regular expressions.
- **Automated Version & Risk Extraction:** Capture dynamic version tokens and assign automated severity ratings (`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).

```yaml
id: scada-modbus-controller
name: Modbus/TCP Industrial Node
protocol: TCP
default_ports: [502, 802]
match_substrings:
  - "modbus"
  - "schneider"
match_regexes:
  - "Modbus/TCP.*Gateway"
category: INDUSTRIAL
risk_severity: CRITICAL
```

All signatures are managed via `DeclarativeSignatureRegistry` with zero-allocation evaluation in the background banner extraction channel.

---

## Interface Telemetry

<p align="center">
  <img src="https://github.com/mr-coder20/PortX/releases/download/v5.0.0/screenshot.png" width="850" alt="PortX UI Screenshot">
</p>

---

## Cross-Platform Installation

<details open>
<summary><strong>🪟 1. Windows (10 / 11)</strong></summary>

- **Official Installer:** Download `PortX-5.3.0.msi` from [Releases](https://github.com/Ali-Rashidi-80/PortX/releases/latest).
- **Windows Package Manager (Winget):**
  ```powershell
  winget install Ali-Rashidi-80.PortX
  ```
</details>

<details>
<summary><strong>🐧 2. Linux (Ubuntu, Debian, Arch)</strong></summary>

- **Debian / Ubuntu (.deb):**
  ```bash
  sudo dpkg -i PortX-5.3.0.deb
  sudo apt-get install -f
  ```
- **Arch Linux (AUR):**
  ```bash
  yay -S portx-bin
  ```
</details>

<details>
<summary><strong>🍎 3. macOS (Apple Silicon & Intel)</strong></summary>

- Download `PortX-5.3.0.dmg` and mount the disk image, or install via Homebrew:
  ```bash
  brew install --cask portx
  ```
</details>

<details>
<summary><strong>🤖 4. Android (Android 7.0+ / API 24 through Android 17 / API 37)</strong></summary>

- Download the signed APK directly from [Releases](https://github.com/Ali-Rashidi-80/PortX/releases/latest).
- Regional App Stores: Available on **Cafe Bazaar** and **Myket**.
</details>

---

## Building from Source

### Prerequisites
- **JDK:** OpenJDK 17 or 21 (Temurin / Corretto recommended)
- **Android SDK:** Build-Tools 34.0.0+ / compileSdk 37

```bash
# Clone repository
git clone https://github.com/Ali-Rashidi-80/PortX.git
cd PortX

# 1. Run Shared Test Suite
./gradlew :shared:compileKotlinJvm :shared:jvmTest

# 2. Run Desktop Application (Windows / macOS / Linux)
./gradlew :desktopApp:run

# 3. Package Desktop Native Installers
./gradlew :desktopApp:packageDeb            # Linux .deb
./gradlew :desktopApp:packageDmg            # macOS .dmg
./gradlew.bat :desktopApp:packageReleaseMsi # Windows .msi

# 4. Build Android Release APK
./gradlew :androidApp:assembleRelease
```

---

## Documentation Matrix

| Document | Language | Description |
| :--- | :---: | :--- |
| [**README.md**](README.md) | English | Primary project documentation, architecture, benchmarks, and guides |
| [**README.fa.md**](README.fa.md) | Persian | Persian mirror of primary documentation (راهنمای جامع فارسی) |
| [**BENCHMARKS.md**](BENCHMARKS.md) | English | Detailed empirical benchmark methodology, telemetry, and GC analysis |
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
| **4** | **DNS Queries over HTTPS (DoH)** | **Paul Hoffman, Patrick McManus (2018)**<br>DNS Queries over HTTPS (DoH).<br>*Internet Engineering Task Force*. | [RFC 8484](https://datatracker.ietf.org/doc/html/rfc8484) |
| **5** | **DNS Security Extensions (DNSSEC)** | **Roy Arends, Rob Austein, Matt Larson, Dan Massey, Scott Rose (2005)**<br>DNS Security Introduction and Requirements.<br>*Internet Engineering Task Force*. | [RFC 4035](https://datatracker.ietf.org/doc/html/rfc4035) |
| **6** | **Classless Inter-domain Routing (CIDR)** | **Vince Fuller, Tony Li (2006)**<br>Classless Inter-domain Routing (CIDR): The Internet Address Assignment and Aggregation Plan.<br>*Internet Engineering Task Force*. | [RFC 4632](https://datatracker.ietf.org/doc/html/rfc4632) |
| **7** | **Structured Concurrency & Backpressure** | **Roman Elizarov (2018)**<br>Structured Concurrency in Kotlin Coroutines.<br>*JetBrains Research*. | [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html) |
| **8** | **Android Foreground Service Limits** | **Android Open Source Project (2024)**<br>Foreground service types and policy restrictions.<br>*Google Android Developers*. | [Android FGS Guidelines](https://developer.android.com/about/versions/14/changes/fgs-types-required) |

---

## Contributing & License

Contributions following our zero-trust engineering standards are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for pull request workflows.

Licensed under the [**Apache License, Version 2.0**](LICENSE).

<div align="center">
  <b>PortX</b> · Maintained & Hardened by <a href="https://github.com/Ali-Rashidi-80/PortX">Ali-Rashidi-80/PortX</a> · Upstream Engine: <a href="https://github.com/mr-coder20/PortX">mr-coder20/PortX</a>.
</div>
