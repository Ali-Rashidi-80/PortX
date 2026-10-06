# Contributing to PortX

Thank you for your interest in contributing to **PortX**! We welcome code contributions, architecture optimizations, documentation improvements, and bug reports.

---

## 🛠 Development Setup & Prerequisites

PortX is built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.

### Requirements
- **JDK:** OpenJDK 17 or 21 (Temurin / Corretto recommended).
- **Android SDK:** API Level 34+ (Build-Tools 34.0.0+).
- **Gradle:** Wrapper included (`./gradlew`).
- **OS Support:** Windows 10/11, macOS (Apple Silicon & Intel), or Ubuntu/Debian Linux.

---

## 🏗 Building Locally

### 1. Compile Shared KMP Logic
```bash
./gradlew :shared:compileKotlinJvm
```

### 2. Run Desktop App (Windows / Linux / macOS)
```bash
./gradlew :desktopApp:run
```

### 3. Package Desktop Installers
```bash
# Ubuntu / Debian (.deb)
./gradlew :desktopApp:packageDeb

# macOS (.dmg)
./gradlew :desktopApp:packageDmg

# Windows (.msi)
./gradlew.bat :desktopApp:packageReleaseMsi
```

### 4. Build Android Release APK
```bash
./gradlew :androidApp:assembleRelease
```

---

## 📐 Architecture Guidelines

1. **Strict UI Separation:**
   - The UI layer (`presentation/ui/`) is design-frozen to maintain the custom neon cyber-aesthetic. Do not alter shaders or layout components without prior maintainer approval.
2. **Memory Safety & Concurrency:**
   - All network socket dispatching must use bounded channels (`kotlinx.coroutines.channels.Channel`).
   - Concurrency limits must be explicitly capped between `10` and `2500` to prevent OS file descriptor exhaustion (`RLIMIT_NOFILE`).
3. **Android 14+ / 15+ Compliance:**
   - Foreground services must declare explicit `foregroundServiceType` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.

---

## 🔄 Pull Request Workflow

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Verify local builds, tests, and benchmarks:
   ```bash
   # Run all shared unit tests
   ./gradlew :shared:jvmTest

   # Run empirical 8-suite live benchmark suite
   ./gradlew :shared:jvmTest --tests "com.mrcoder20.portx.data.network.LiveSystemBenchmarkTest" --rerun-tasks

   # Verify desktop app compilation
   ./gradlew :desktopApp:compileKotlin
   ```
3. Commit with conventional commit messages:
   ```bash
   git commit -m "feat(engine): add adaptive RTT jitter mitigation"
   ```
4. Push to your fork and submit a PR to [`Ali-Rashidi-80/PortX`](https://github.com/Ali-Rashidi-80/PortX) (or upstream `mr-coder20/PortX`) branch `main`.

Thank you for building high-performance network tools with us!
