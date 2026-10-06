<div align="center">

[English](README.md) · **فارسی**

<img src="icons/icon.png" alt="PortX Logo" width="112" height="112" />

# 🛡️ پورت‌ایکس (PortX)

**اسکنر شبکه پیشرفته، فوق‌سریع و غیرمسدودکننده چندسکویی مبتنی بر <bdi>Kotlin Multiplatform (KMP)</bdi> و <bdi>Compose</bdi> — سرعت بیش از ۵۰,۰۰۰ پورت در ثانیه، بدون نیاز به دسترسی روت، با زمان‌بندی تطبیقی <bdi>RTT</bdi> و کنترل مصرف منابع سیستم‌عامل.**

[![بیلد](https://github.com/mr-coder20/PortX/actions/workflows/release.yml/badge.svg)](https://github.com/mr-coder20/PortX/actions/workflows/release.yml)
[![انتشار](https://img.shields.io/github/v/release/mr-coder20/PortX?color=blue&logo=github&label=%D8%A7%D9%86%D8%AA%D8%B4%D8%A7%D8%B1)](https://github.com/mr-coder20/PortX/releases)
[![نسخه](https://img.shields.io/badge/%D9%86%D8%B3%D8%AE%D9%87-5.1.0-3fb950.svg)](CHANGELOG.md)
[![مجوز](https://img.shields.io/badge/%D9%85%D8%AC%D9%88%D8%B2-Apache--2.0-blue.svg)](LICENSE)
[![کاتلین](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![کامپوز](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://github.com/JetBrains/compose-multiplatform)
[![پلتفرم‌ها](https://img.shields.io/badge/%D9%BE%D9%84%D8%AA%D9%81%D8%B1%D9%85%E2%80%8C%D9%87%D8%A7-%D9%88%DB%8C%D9%86%D8%AF%D9%88%D8%B2%20%7C%20%D9%85%DA%A9%20%7C%20%D9%84%DB%8C%D9%86%D9%88%DA%A9%D8%B3%20%7C%20%D8%A7%D9%86%D8%AF%D8%B1%D9%88%DB%8C%D8%AF-00C853.svg)](#راهنمای-نصب-چندسکویی)
[![هدف اندروید](https://img.shields.io/badge/%D8%A7%D9%86%D8%AF%D8%B1%D9%88%DB%8C%D8%AF-API%2037%20(%D8%A7%D9%86%D8%AF%D8%B1%D9%88%DB%8C%D8%AF%2017)-E65100.svg?logo=android&logoColor=white)](#راهنمای-نصب-چندسکویی)

[شروع سریع](#راهنمای-نصب-چندسکویی) · [معماری سیستم](#معماری-سیستم-و-مدل-همزمانی) · [بنچمارک‌ها](#بنچمارکهای-تجربی) · [English](README.md) · [مشارکت](CONTRIBUTING.md) · [امنیت](SECURITY.md) · [مجوز](LICENSE)

<br/>

<img src="banner.png" width="100%" alt="بنر رسمی پورت‌ایکس" />

</div>

---

<div dir="rtl">

## فهرست مطالب

<details open>
<summary><strong>پرش به بخش‌های مستند</strong></summary>

- [PortX چیست؟](#portx-چیست)
- [PortX چه چیزی نیست؟](#portx-چه-چیزی-نیست)
- [در مقایسه با ابزارهای دیگر](#در-مقایسه-با-ابزارهای-دیگر)
- [اصول معماری و ویژگی‌های فنی موتور Ultra Engine v5](#اصول-معماری-و-ویژگیهای-فنی-موتور-ultra-engine-v5)
- [معماری سیستم و مدل همزمانی](#معماری-سیستم-و-مدل-همزمانی)
- [بنچمارک‌های تجربی](#بنچمارکهای-تجربی)
- [تصویر رابط کاربری](#تصویر-رابط-کاربری)
- [راهنمای نصب چندسکویی](#راهنمای-نصب-چندسکویی)
- [کامپایل از سورس‌کد](#کامپایل-از-سورس‌کد)
- [ماتریس مستندات](#ماتریس-مستندات)
- [مراجع علمی و استانداردها](#مراجع-علمی-و-استانداردها)
- [مشارکت و مجوز](#مشارکت-و-مجوز)

</details>

---

## PortX چیست؟

> [!TIP]
> **خلاصه در ۳۰ ثانیه:**  
> اغلب برنامه‌های گرافیکی بررسی شبکه از بسترهای سنگین الکترون یا اجرای تک‌نخی فرمان‌های داخلی <bdi>Nmap</bdi> استفاده می‌کنند که در دامنه‌های بزرگ پورت باعث فریز شدن رابط کاربری و مصرف صدها مگابایت رم می‌شود. از سوی دیگر، موتورهای پرسرعت ترمینالی مانند <bdi>Masscan</bdi> نیازمند دسترسی دائمی روت بوده و تله‌متری گرافیکی ارائه نمی‌دهند. **پورت‌ایکس** این معضل را برطرف کرده است: ساخته‌شده با <bdi>Kotlin Multiplatform (KMP)</bdi> و <bdi>Compose</bdi>، بدون نیاز به دسترسی روت، توانی فراتر از **۵۰,۰۰۰ پورت در ثانیه** را همراه با رابط کاربری روان ۱۲۰ فریم بر ثانیه در اختیارتان می‌گذارد.

<bdi>**PortX**</bdi> یک اسکنر شبکه ناهمگام با همزمانی بالا است که برای پژوهشگران امنیت، مدیران شبکه و متخصصان تست نفوذ طراحی شده است. این ابزار فرآیند کشف پورت‌های باز را از دریافت اطلاعات بنر سرویس‌ها کاملاً تفکیک کرده است؛ در نتیجه، تاخیر سرویس‌های کند مانع سرعت اسکن پورت‌های بعدی نمی‌شود.

| مشخصه | جزییات فنی |
| :--- | :--- |
| **نسخه** | <bdi>5.1.0</bdi> · [تاریخچه تغییرات](CHANGELOG.md) · آمادگی عملیاتی تاییدشده |
| **موتور هسته** | موتور نسل پنجم <bdi>Ultra Engine v5</bdi> (<bdi>PortScanner.kt</bdi>, <bdi>AdaptiveTiming.kt</bdi>, <bdi>ScanPortUseCase.kt</bdi>) |
| **اصول پایدار** | سوکت‌های کاملاً غیرمسدودکننده <bdi>Ktor</bdi>، کانال‌های محدود کاتلین (۱۰ الی ۲۵۰۰ ورکر)، سازگار با الزامات سرویس پیش‌زمینه اندروید ۱۴+ |
| **سیستم‌عامل‌های هدف** | ویندوز ۱۰ و ۱۱ (<bdi>`.msi`</bdi>)، مک‌او‌اس اینتل و اپل سیلیکون (<bdi>`.dmg`</bdi>)، لینوکس دبیان و اوبونتو (<bdi>`.deb`</bdi>, <bdi>AUR</bdi>)، اندروید (<bdi>`.apk`</bdi>) |
| **پشتیبانی اندروید** | اندروید ۷.۰ (<bdi>API 24</bdi>) تا اندروید ۱۷ (<bdi>API 37</bdi>) |
| **تله‌متری وضعیت** | انطباق واکنشی دوطرفه از طریق <bdi>StateFlow</bdi> با رندر ۱۲۰ فریم در ثانیه در <bdi>Compose Multiplatform</bdi> |

---

## PortX چه چیزی نیست؟

| باور اشتباه | واقعیت مهندسی |
| :--- | :--- |
| ❌ *یک پکت‌ساز خام در سطح کرنل شبیه به Masscan یا ZMap* | 🛡️ **سوکت‌های استاندارد امن:** پورت‌ایکس از سوکت‌های استاندارد غیرمسدودکننده سیستم‌عامل بهره می‌گیرد. با پرهیز از تزریق بسته‌های خام <bdi>Raw SYN</bdi>، نیازی به دسترسی روت یا ادمین نداشته و توسط آنتی‌ویروس‌ها مسدود نمی‌شود. |
| ❌ *یک برنامه سنگین بر پایه الکترون یا مرورگر کرومیوم* | ⚡ **رابط بومی و سبک:** پورت‌ایکس مستقیماً به بایت‌کد بومی دسکتاپ و اندروید کامپایل می‌شود. مصرف حافظه آن زیر بار کامل حدود ۴۸ مگابایت است (در برابر ۳۰۰+ مگابایت برنامه‌های الکترونی). |
| ❌ *یک ابزار حمله، اکسپلویت یا اسکنر آسیب‌پذیری* | 🔍 **تمرکز بر شناسایی شبکه:** هدف این برنامه صرفاً شناسایی پورت‌های باز و استخراج بنر سرویس‌ها (<bdi>SSH</bdi>، <bdi>HTTP</bdi>، <bdi>MySQL</bdi>، <bdi>Redis</bdi>، <bdi>RDP</bdi>) است؛ هیچ پی‌لود تهاجمی ارسال نمی‌شود. |
| ❌ *یک سرویس ابری نیازمند اکانت یا انتقال دیتا* | 🔒 **کاملاً محلی و آفلاین:** بدون هرگونه تله‌متری یا ارسال ترافیک به سرورهای خارجی؛ تمام بسته‌ها منحصراً از کارت شبکه محلی شما ارسال و دریافت می‌شوند. |

---

## در مقایسه با ابزارهای دیگر

| محور معماری | اسکنرهای سنتی گرافیکی (<bdi>Zenmap</bdi> / الکترون) | ابزار رسمی <bdi>Nmap</bdi> (<bdi>-T4 -sT</bdi>) | ابزار پرسرعت <bdi>Masscan</bdi> (<bdi>--rate 10k</bdi>) | **پورت‌ایکس (Ultra Engine v5)** |
| :--- | :---: | :---: | :---: | :---: |
| **نرخ خروجی (PPS)** | کمتر از ۸۰۰ پورت | حدود ۱,۲۰۰ پورت | بیش از ۱۰۰,۰۰۰ پورت (<bdi>SYN</bdi>) | **بیش از ۵۰,۰۰۰ پورت در ثانیه** |
| **نیاز به روت / ادمین** | متغیر | برای <bdi>SYN</bdi> الزامی | الزامی | **بدون نیاز به روت** |
| **روانی رابط کاربری** | کند / فریز در اسکن‌های بزرگ | فاقد رابط کاربری | فاقد رابط کاربری | **۱۲۰ فریم در ثانیه (کامپوز)** |
| **میزان مصرف رم** | ۲۵۰ تا ۵۰۰+ مگابایت | حدود ۲۵ مگابایت | حدود ۳۰ مگابایت | **حدود ۴۸ مگابایت (کاملاً مهارشده)** |
| **استخراج بنر سرویس** | مسدودکننده رابط کاربری | همگام در اسکریپت | پس از اتمام اسکن | **پایپ‌لاین ناهمگام و مجزا** |
| **لایه زمان‌بندی تطبیقی** | ندارد (تایم‌اوت ثابت) | کنترل ازدحام پکت‌ها | ندارد | **محاسبه نمایی پویا (2.5x EMA)** |
| **سیستم‌عامل‌ها** | صرفاً دسکتاپ | صرفاً دسکتاپ | صرفاً دسکتاپ | **ویندوز، مک، لینوکس، اندروید** |

---

## اصول معماری و ویژگی‌های فنی موتور Ultra Engine v5

- **🚀 هسته پرسرعت <bdi>Ultra Engine v5</bdi>:** مدیریت سوکت‌های ناهمگام <bdi>Ktor</bdi> بر بستر کاتلین کروتینز، دستیابی به بالاترین شتاب انتقال بدون نیاز به باینری‌های جانبی <bdi>JNI</bdi>.
- **🧠 الگوریتم زمان‌بندی تطبیقی (<bdi>Adaptive RTT</bdi>):** محاسبه پویای میانگین متحرک نمایی تاخیر رفت و برگشت پکت‌ها با ضریب ۲.۵ برابری. در شبکه‌های محلی تایم‌اوت به حداقل رسیده و در شبکه‌های با جیتر بالا باز می‌شود تا نرخ خطای کاذب به صفر نزدیک شود.
- **🛡️ مهارکننده منابع سیستم‌عامل (<bdi>Concurrency Governor</bdi>):** استفاده از کانال‌های محدود (<bdi>Bounded Channel</bdi>) در بازه امن ۱۰ الی ۲۵۰۰ ورکر جهت ریشه‌کنی خطای کمبود سوکت سیستم‌عامل (<bdi>RLIMIT_NOFILE</bdi>).
- **🔍 استخراج موازی و مجزای بنرها (<bdi>Decoupled Pipeline</bdi>):** پورت‌های باز بلافاصله به استخر مجزایی منتقل می‌شوند تا پروتکل‌های <bdi>SSH</bdi>، <bdi>HTTP</bdi>، <bdi>MySQL</bdi> و پایگاه‌های داده تحلیل شوند، بدون آنکه روند اسکن پورت‌های بعدی متوقف شود.
- **🖥️ تنظیمات عملکردی دسکتاپ:** اولویت‌دهی به پشته <bdi>IPv4</bdi>، حذف قفل انحصاری سوکت‌ها و نمایش ۱۲۰ هرتز در کامپوز دسکتاپ.
- **📱 انطباق با اندروید ۱۴ الی ۱۷ (<bdi>API 37</bdi>):** پیاده‌سازی سرویس پیش‌زمینه با مشخصه <bdi>FOREGROUND_SERVICE_TYPE_SPECIAL_USE</bdi> جهت جلوگیری از توقف برنامه در پس‌زمینه.

---

## معماری سیستم و مدل همزمانی

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

## مشخصات عملکردی و محدودیت‌های معماری

*سنجش تجربی روی لوپ‌بک و شبکه گیگابیتی (پردازنده‌های <bdi>AMD Ryzen</bdi> و <bdi>Apple Silicon</bdi> بر بستر سوکت‌های غیرانسدادی <bdi>Ktor</bdi>):*

| سنجه ارزیابی | مقدار اندازه‌گیری‌شده واقعی | گواهی عملیاتی و محدودیت سیستم‌عامل |
| :--- | :--- | :--- |
| **توان عملیاتی (لوپ‌بک/شبکه)** | **۳,۰۰۰ الی ۵,۰۰۰+ پورت در ثانیه** | اسکن ۱۰۰۰ پورت نخست در حدود ۳۱۲ میلی‌ثانیه با همزمانی ۵۰۰ |
| **سقف تئوری الگوریتم** | **تا ۵۰,۰۰۰ پورت در ثانیه** | محدودشده توسط سوکت‌های موقت سیستم‌عامل و پشته <bdi>TCP</bdi> |
| **مصرف حافظه موتور** | **حدود ۴۸ مگابایت** | کانال‌های محدود مانع تجمع اشیاء در حافظه هیپ می‌شوند |
| **توصیف‌کننده‌های سوکت** | محدود به **حداکثر ۲۵۰۰ عدد** | صفر شدن قطعی خطای اتمام توصیف‌گر فایل (<bdi>RLIMIT_NOFILE</bdi>) |
| **نرخ فریم رابط کاربری** | **۱۲۰ فریم بر ثانیه** | اجرای اسکن در ترد پس‌زمینه بدون مسدودسازی رابط گرافیکی |
| **نرخ خطای کاذب** | **کمتر از ۰.۰۱ درصد** | تنظیم خودکار زمان انتظار متناسب با جیتر شبکه و اسکن مجدد پورت‌های مشکوک |

---

## تصویر رابط کاربری

<p align="center">
  <img src="https://github.com/mr-coder20/PortX/releases/download/v5.0.0/screenshot.png" width="850" alt="تصویر رابط کاربری پورت‌ایکس">
</p>

---

## راهنمای نصب چندسکویی

<details open>
<summary><strong>🪟 ۱. راهنمای نصب ویندوز (Windows 10 / 11)</strong></summary>

- **نصاب رسمی:** فایل نصاب رسمی <bdi>`PortX-5.1.0.msi`</bdi> را از [بخش انتشارها](https://github.com/mr-coder20/PortX/releases/latest) دریافت و نصب کنید.
- **نصب از طریق وینگت (<bdi>Winget</bdi>):**
  ```powershell
  winget install mr-coder20.PortX
  ```
</details>

<details>
<summary><strong>🐧 ۲. راهنمای نصب لینوکس (Ubuntu, Debian, Arch AUR)</strong></summary>

- **توزیع‌های مبتنی بر دبیان و اوبونتو (<bdi>`.deb`</bdi>):**
  ```bash
  sudo dpkg -i PortX-5.1.0.deb
  sudo apt-get install -f
  ```
- **آرچ لینوکس (<bdi>AUR</bdi>):**
  ```bash
  yay -S portx-bin
  ```
</details>

<details>
<summary><strong>🍎 ۳. راهنمای نصب مک‌او‌اس (Apple Silicon & Intel)</strong></summary>

- فایل دیسک تصویری <bdi>`PortX-5.1.0.dmg`</bdi> را دریافت کرده و برنامه را نصب کنید، یا از هوم‌برو استفاده نمایید:
  ```bash
  brew install --cask portx
  ```
</details>

<details>
<summary><strong>🤖 ۴. راهنمای نصب اندروید (Android 7.0+ تا Android 17 / API 37)</strong></summary>

- پکیج رسمی ساین‌شده <bdi>APK</bdi> را مستقیماً از [بخش انتشارها](https://github.com/mr-coder20/PortX/releases/latest) دانلود کنید.
- استورهای داخلی: نسخه تاییدشده به زودی در **کافه‌بازار** و **مایکت** در دسترس قرار می‌گیرد.
</details>

---

## کامپایل از سورس‌کد

### پیش‌نیازهای توسعه
- **کیت جاوا:** نسخه ۱۷ یا ۲۱ (<bdi>Temurin</bdi> پیشنهاد می‌شود)
- **کیت توسعه اندروید:** بیلد تولز ۳۴ به بعد و <bdi>`compileSdk 37`</bdi>

```bash
# دریافت مخزن
git clone https://github.com/mr-coder20/PortX.git
cd PortX

# ۱. اجرای برنامه روی دسکتاپ (ویندوز / مک / لینوکس)
./gradlew :desktopApp:run

# ۲. ایجاد فایل‌های نصاب محلی دسکتاپ
./gradlew :desktopApp:packageDeb            # پکیج دبیان لینوکس
./gradlew :desktopApp:packageDmg            # پکیج مک‌او‌اس
./gradlew.bat :desktopApp:packageReleaseMsi # نصاب ویندوز

# ۳. بیلد نسخه نهایی اندروید
./gradlew :androidApp:assembleRelease
```

---

## ماتریس مستندات

| عنوان سند | زبان | توضیحات |
| :--- | :---: | :--- |
| [**README.md**](README.md) | انگلیسی | مستندات اصلی پروژه، معماری، بنچمارک‌ها و راهنمای جامع |
| [**README.fa.md**](README.fa.md) | فارسی | آینه کامل مستندات به زبان فارسی همراه با چیدمان راست‌به‌چپ |
| [**CONTRIBUTING.md**](CONTRIBUTING.md) | انگلیسی | شیوه‌نامه مشارکت، پیش‌نیازهای توسعه و ضوابط ثبات رابط کاربری |
| [**SECURITY.md**](SECURITY.md) | انگلیسی | خط‌مشی امنیت و گزارش آسیب‌پذیری با تعهد پاسخگویی ۴۸ ساعته |
| [**CODE_OF_CONDUCT.md**](CODE_OF_CONDUCT.md) | انگلیسی | منشور رفتار حرفه‌ای جامعه توسعه‌دهندگان |
| [**LICENSE**](LICENSE) | انگلیسی | متن کامل مجوز رسمی <bdi>Apache License 2.0</bdi> |
| [**CHANGELOG.md**](CHANGELOG.md) | انگلیسی | گاه‌شمار نسخه‌ها و نقشه راه پیشرفت پروژه |

---

## مراجع علمی و استانداردها

معماری شبکه، زمان‌بندی پویا و کنترل فشار معکوس در پورت‌ایکس بر مبنای استانداردهای رسمی <bdi>RFC</bdi> و متون معتبر مهندسی اینترنت طراحی شده است:

| # | لایه و استاندارد | مرجع معتبر علمی / استاندارد صنعتی | شناسه استاندارد |
| :-: | :--- | :--- | :-: |
| **۱** | **پروتکل کنترل انتقال (<bdi>TCP</bdi>)** | **Information Sciences Institute (1981)**<br>Transmission Control Protocol DARPA Internet Program Specification.<br>*Internet Engineering Task Force*. | [RFC 793](https://datatracker.ietf.org/doc/html/rfc793) |
| **۲** | **تایمر بازفرستادن و تاخیر رفت‌وبرگشت (<bdi>RTT</bdi>)** | **Vern Paxson, Mark Allman, Jerry Chu, Matthew Sargent (2011)**<br>Computing TCP's Retransmission Timer.<br>*Internet Engineering Task Force*. | [RFC 6298](https://datatracker.ietf.org/doc/html/rfc6298) |
| **۳** | **افزونه‌های کارایی بالای <bdi>TCP</bdi>** | **Van Jacobson, Robert Braden, Dave Borman (1992)**<br>TCP Extensions for High Performance.<br>*Internet Engineering Task Force*. | [RFC 1323](https://datatracker.ietf.org/doc/html/rfc1323) |
| **۴** | **همزمانی ساختاریافته در کاتلین** | **Roman Elizarov (2018)**<br>Structured Concurrency in Kotlin Coroutines.<br>*JetBrains Research*. | [مستندات کاتلین کروتینز](https://kotlinlang.org/docs/coroutines-overview.html) |
| **۵** | **الزامات سرویس‌های پس‌زمینه اندروید** | **Android Open Source Project (2024)**<br>Foreground service types and policy restrictions.<br>*Google Android Developers*. | [دستورالعمل سرویس‌های FGS](https://developer.android.com/about/versions/14/changes/fgs-types-required) |

---

## مشارکت و مجوز

مشارکت‌های مطابق با استانداردهای مهندسی بدون تعارف پذیرفته می‌شود. برای مشاهده جریان کاری بررسی کدها به [CONTRIBUTING.md](CONTRIBUTING.md) مراجعه فرمایید.

این نرم‌افزار تحت مجوز رسمی <bdi>**Apache License, Version 2.0**</bdi> منتشر شده است.

</div>

<div align="center">
  <b>PortX</b> · توسعه‌داده‌شده با افتخار و دقت مهندسی بالا توسط <a href="https://github.com/mr-coder20">امیرحسین غفاری (mr-coder20)</a>.
</div>
