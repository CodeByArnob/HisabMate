# HisabMate

**Offline-first personal finance and multi-currency wealth management application for Android.**

> 🚀 **Latest Release: v1.0.3** (Android 7.0–16, API 24–36)  
> 📥 **[Download Latest APK (v1.0.3)](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.apk)** • 📦 **[Download Latest AAB Bundle](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.aab)** • 📋 **[GitHub Release v1.0.3](https://github.com/CodeByArnob/HisabMate/releases/tag/v1.0.3)** • 🌐 **[Live Project Website](https://codebyarnob.github.io/HisabMate/)**

[![Release](https://img.shields.io/badge/Release-v1.0.3-10B981?style=flat-square&logo=github)](https://github.com/CodeByArnob/HisabMate/releases/tag/v1.0.3)
[![Android CI](https://img.shields.io/github/actions/workflow/status/CodeByArnob/HisabMate/android-ci.yml?branch=main&style=flat-square&logo=android&label=Android%20CI)](https://github.com/CodeByArnob/HisabMate/actions/workflows/android-ci.yml)
[![Platform](https://img.shields.io/badge/Android-API%2024%E2%80%9336-3DDC84?style=flat-square&logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Website](https://img.shields.io/badge/Website-GitHub%20Pages-0EA5E9?style=flat-square&logo=googlechrome)](https://codebyarnob.github.io/HisabMate/)

[Live Project Website](https://codebyarnob.github.io/HisabMate/) | [Download APK](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.apk) | [Download AAB](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.aab) | [Release Notes](https://github.com/CodeByArnob/HisabMate/releases/tag/v1.0.3) | [Architecture](#architecture) | [Security](#security-and-privacy)

---

![HisabMate Application Banner](docs/screenshots/hisabmate-banner.svg?v=1.0.3)

---

## Overview

HisabMate (`com.aistudio.hisabmate.fntkbd`) is an open-source, offline-first personal finance and expense management application engineered for Android using **Kotlin**, **Jetpack Compose (Material 3)**, and **Room SQLite v6**.

Designed for individuals, professionals, and households managing finances across multiple rails and regional boundaries, HisabMate brings together bank accounts, credit cards, mobile financial services (bKash, Nagad, Rocket), international multi-currency vaults (Wise, Revolut), multi-period budgets, debt ledgers, and recurring obligations into a single cohesive interface.

Every financial record is persisted in its native ISO 4217 currency with offline foreign exchange conversion, ensuring strict mathematical accuracy without requiring external cloud servers or telemetry.

---

## Release Downloads

Official release packages are compiled from verified source via Gradle `:app:assembleRelease` and `:app:bundleRelease`, verified with Android SDK `apksigner`, `aapt2`, and local Robolectric test suites.

| Package | Format | File Size | Target Architecture | Download |
| :--- | :---: | :---: | :--- | :--- |
| **HisabMate-v1.0.3-release.apk** | `.apk` | `11.1 MB` | Direct installation on Android 7.0 to 16 (`API 24–36`) | [Download APK](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.apk) |
| **HisabMate-v1.0.3-release.aab** | `.aab` | `10.8 MB` | Android App Bundle for Google Play / `bundletool` | [Download AAB](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.aab) |
| **SHA256SUMS.txt** | `.txt` | `190 B` | Release checksum verification manifest | [Download Checksums](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/SHA256SUMS.txt) |

### Cryptographic Checksums (v1.0.3)

- **APK SHA-256**: `e5caf443917c780350bf399aa885f81700b7138774508d547985aa09808a765f`
- **AAB SHA-256**: `84391328fc40ef5bf23d43e33b46a07b1a0b5b71d2c17d0b8ff5d33d20252fb4`

---

## Application Screenshots

The following screenshots are captured directly from the running Android application running on Android 16 with Jetpack Compose Material 3:

### Dashboard & Walkthrough

| Home Dashboard (Light) | Home Dashboard (Dark) | 10-Screen Motion Walkthrough |
| :---: | :---: | :---: |
| <img src="docs/screenshots/dashboard-light.png" alt="HisabMate Home Dashboard Light Mode" width="240" /> | <img src="docs/screenshots/dashboard-dark.png" alt="HisabMate Home Dashboard Dark Mode" width="240" /> | <img src="docs/screenshots/hisabmate-motion-walkthrough.gif" alt="HisabMate Motion Walkthrough Reel" width="240" /> |
| *Consolidated net liquidity and quick actions* | *Obsidian dark surface with high contrast* | *Animated walkthrough of primary flows* |

### Accounts, Analytics & FX

| Multi-Currency & Offline FX | Accounts & Wallets | Wallet Details & Trend |
| :---: | :---: | :---: |
| <img src="docs/screenshots/multi-currency-fx.png" alt="Multi-Currency and Offline FX Converter" width="240" /> | <img src="docs/screenshots/accounts-wallets.png" alt="Accounts and Wallets Ledger" width="240" /> | <img src="docs/screenshots/wallet-details-analytics.png" alt="Wallet Details and Daily Spend Trend" width="240" /> |
| *14-Currency offline conversion pipeline* | *Bank, Card, bKash, Nagad, and Wise vaults* | *Daily spend curve and transaction log* |

### Planning Hub & Financial Controls

| Budget Management | Cashflow Analysis | Savings Goals |
| :---: | :---: | :---: |
| <img src="docs/screenshots/budget-management.png" alt="Budget Management" width="240" /> | <img src="docs/screenshots/financial-reports.png" alt="Cashflow Analysis and Reports" width="240" /> | <img src="docs/screenshots/savings-goals.png" alt="Savings Goals" width="240" /> |
| *Category limits with 50%–100% threshold alerts* | *Monthly income vs. expense breakdown* | *Dedicated pots with deadline milestones* |

### Obligations, Alerts & System Settings

| Debt & Loan Ledger | Scheduled Reminders | Security & Backup |
| :---: | :---: | :---: |
| <img src="docs/screenshots/debt-management.png" alt="Debt and Loan Management" width="240" /> | <img src="docs/screenshots/bills-notifications.png" alt="Bills and Notifications" width="240" /> | <img src="docs/screenshots/settings-security.png" alt="Security and Backup Settings" width="240" /> |
| *Receivables (lent) and payables (borrowed)* | *AlarmManager bill and budget notifications* | *PIN lock, JSON backup, and CSV/PDF export* |

---

## Core Capabilities

### 1. Multi-Currency Engine with Offline FX
- **14 Built-in ISO 4217 Currencies**: Includes native symbols, regional precision (0 decimals for JPY, 2 for BDT/USD/EUR, 3 for KWD), and configurable South Asian (`1,25,000.00`) vs. International (`125,000.00`) digit grouping.
- **Native Persistence**: Accounts, transactions, budgets, bills, debts, and goals maintain their original currency identifier.
- **Offline Currency Conversion**: Instant bidirectional conversion with user-configurable custom exchange rates.

### 2. Account & Wallet Management
- Support for **Bank Accounts**, **Credit/Debit Cards**, **Mobile Financial Services (bKash, Nagad, Rocket)**, **Global Multi-Currency Vaults (Wise, Revolut)**, and **Cash Wallets**.
- Internal account transfers with automatic FX cross-rate conversion.

### 3. Planning & Budget Controls
- Multi-period budgets across **Daily**, **Weekly**, **Monthly**, **Yearly**, and **Custom** periods.
- Alert thresholds (50% to 100%) that deliver local notifications when approaching spending caps.
- Goal trackers with progress percentage indicators, target dates, and contribution histories.

### 4. Obligations & Debt Ledger
- Independent ledgers for **Money Lent (Receivables)** and **Money Borrowed (Payables)**.
- Partial settlement logging, remaining balance calculation, and due date reminders.

### 5. Document & Report Generation
- **Single-Page ISO A4 Executive PDF Statements**: Production-grade single-page ISO A4 (`595 × 842 pt`, `PAGE 1 OF 1`) bank-style financial statements with deep slate headers, 4-column executive KPI summary cards, side-by-side multi-currency wallet and category allocation tables, and a right-aligned transaction audit ledger.
- **Android Scoped Storage**: Directly exports to the system `Download/HisabMate/` directory using `MediaStore.Downloads` on Android 10+ (API 29+).
- **Secure Document Sharing**: Integrated Android `FileProvider` (`com.example.fileprovider`) for viewing and sharing without external storage permissions.
- **Data Portability**: Full JSON backup and restore with checksum verification, plus CSV spreadsheet export and import.

---

## Supported Currencies

| Code | Symbol | Currency Name | Region | Decimals | Grouping Style |
| :--- | :---: | :--- | :--- | :---: | :--- |
| **BDT** | `৳` | Bangladeshi Taka | Bangladesh | 2 | South Asian (`1,25,000.00`) & Standard |
| **USD** | `$` | US Dollar | United States / International | 2 | Standard (`125,000.00`) |
| **EUR** | `€` | Euro | European Union | 2 | Standard |
| **GBP** | `£` | British Pound | United Kingdom | 2 | Standard |
| **INR** | `₹` | Indian Rupee | India | 2 | South Asian & Standard |
| **JPY** | `¥` | Japanese Yen | Japan | 0 | Standard (`15,800`) |
| **CNY** | `¥` | Chinese Yuan Renminbi | China | 2 | Standard |
| **SAR** | `ر.স` | Saudi Riyal | Saudi Arabia | 2 | Standard |
| **AED** | `د.إ` | UAE Dirham | United Arab Emirates | 2 | Standard |
| **CAD** | `CA$` | Canadian Dollar | Canada | 2 | Standard |
| **AUD** | `A$` | Australian Dollar | Australia | 2 | Standard |
| **SGD** | `S$` | Singapore Dollar | Singapore | 2 | Standard |
| **MYR** | `RM` | Malaysian Ringgit | Malaysia | 2 | Standard |
| **KWD** | `د.ك` | Kuwaiti Dinar | Kuwait | 3 | Standard (`3` decimals) |

---

## Architecture

HisabMate follows clean architectural patterns with unidirectional data flow and reactive SQLite persistence:

```text
app/src/main/java/com/example
├── MainActivity.kt                      # Host Activity with Edge-to-Edge window insets
├── data
│   ├── local
│   │   ├── Entities.kt                  # 10 Room SQLite entities with query-indexed columns (v6)
│   │   ├── HisabDao.kt                  # Transactional DAO with reactive Kotlin Flow queries
│   │   └── HisabDatabase.kt             # Room Database definition and migration handlers (MIGRATION_5_6)
│   └── repository
│       └── HisabRepository.kt           # Repository layer, FX calculation, backup and CSV export/import
├── ui
│   ├── components
│   │   └── FintechComponents.kt         # Reusable Material 3 cards, progress bars, and navigation dock
│   ├── screens
│   │   ├── DashboardScreen.kt           # Home dashboard, quick actions, and wallet breakdowns
│   │   ├── AccountsAndMoreScreens.kt    # Account manager, data backup/restore, PDF export, settings
│   │   ├── PlanningHubScreen.kt         # Budgets, recurring payments, debt ledger, savings goals
│   │   ├── CurrencyManagementScreen.kt  # Currency selection and offline exchange rate editor
│   │   ├── SpendAnalysisScreen.kt       # Period spend trends and cashflow reports
│   │   ├── TransactionModals.kt         # Transaction creation sheets and split expense modals
│   │   ├── CalendarAndSecurityScreens.kt # Financial calendar heatmap and PIN lock screen
│   │   └── CinematicOnboardingScreen.kt  # 7-step guided setup experience
│   ├── theme
│   │   ├── Color.kt                     # Semantic light/dark surface, border, and brand palettes
│   │   ├── Theme.kt                     # HisabTheme and dynamic Material 3 color provider
│   │   └── Type.kt                      # Space Grotesk, Plus Jakarta Sans, and Inter typography
│   └── viewmodel
│       └── HisabViewModel.kt            # Coroutine-backed state aggregation (HisabUiState)
└── util
    ├── CurrencyFormatter.kt             # ISO 4217 formatting, South Asian digit grouping, FX conversion
    ├── HisabNotificationManager.kt      # Android AlarmManager scheduler and 4 notification channels
    ├── PdfExporter.kt                   # Scoped storage MediaStore PDF generation and FileProvider engine
    └── HapticFeedbackHelper.kt          # Haptic feedback utilities
```

### Technology Stack

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Local Persistence**: Room SQLite (Schema Version 6 with Indexed Queries)
- **Asynchronous Execution**: Kotlin Coroutines & Reactive `StateFlow`
- **Compiler Symbol Processing**: KSP
- **Target SDK**: Android 16 (API 36)
- **Minimum SDK**: Android 7.0 (API 24)

---

## Security and Privacy

1. **Zero Internet Permission**: The app manifest does not request `android.permission.INTERNET`. No network calls, telemetry, or analytics can be transmitted.
2. **On-Device Data Storage**: All transactions, accounts, and personal records remain exclusively on the local device inside private application sandboxes.
3. **Application Lock**: Optional PIN code protection with biometric support.
4. **Least-Privilege Permissions**:
   - `POST_NOTIFICATIONS`: Delivers local bill and budget reminders (runtime permission on Android 13+).
   - `RECEIVE_BOOT_COMPLETED`: Reschedules active local reminders upon device reboot.
   - `VIBRATE`: Provides haptic feedback during interactive workflows.

---

## Building from Source

### Prerequisites

- JDK 17 or JDK 21
- Android SDK with platform tools for API 24 through 36
- Gradle 8.x or 9.x (managed via system toolchain)

### Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/CodeByArnob/HisabMate.git
   cd HisabMate
   ```

2. **Initialize configuration**:
   ```bash
   cp .env.example .env
   ```

3. **Execute test suite**:
   ```bash
   gradle :app:testDebugUnitTest :app:lintDebug
   ```

4. **Compile debug package**:
   ```bash
   gradle :app:assembleDebug
   ```

5. **Compile signed release packages**:
   ```bash
   export KEYSTORE_PATH="/path/to/upload-keystore.jks"
   export STORE_PASSWORD="your-keystore-password"
   export KEY_PASSWORD="your-key-password"

   gradle :app:assembleRelease :app:bundleRelease
   ```

---

## Quality Assurance & Testing

Automated test verification is managed via **Robolectric** local JVM tests in `app/src/test/java/com/example/ExampleRobolectricTest.kt`. All 16 unit and UI tests validate:

- **Permanent Demo Deletion**: Verifies that deleting sample records clears demo markers while preserving real user accounts, and that app restart or database re-open never re-seeds sample data.
- **Room Migration**: Validates schema transition from version 5 to 6 (`MIGRATION_5_6`) with column additions and indexed queries.
- **Physical PDF Generation**: Validates generation of compliant, non-empty multi-page statement files with verified byte counts.
- **Multi-Currency Math**: Validates ISO 4217 conversions, formatting precision, and South Asian numbering separators.
- **Notification Scheduling**: Validates local channel creation and AlarmManager broadcast registrations.

---

## Release History

| Version | Date | Key Enhancements | Release Assets |
| :--- | :--- | :--- | :--- |
| **v1.0.3** | 2026-10-31 | Redesigned production Single-Page ISO A4 (`595 × 842 pt`) Executive PDF Financial Statement engine with KPI cards, dual-column wallet & category allocation tables, right-aligned audit ledger, and unified website/banner version sync. | [APK](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.apk) / [AAB](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/HisabMate-v1.0.3-release.aab) / [Checksums](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.3/SHA256SUMS.txt) |
| **v1.0.2** | 2026-10-31 | Light theme visual refinement with muted slate-blue background (#CFD8E6), rich card surfaces (#DFE6F0), dimensional Main Balance hero panel with subtle surface gradient, and elevated navigation dock. | [APK](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.2/HisabMate-v1.0.2-release.apk) / [AAB](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.2/HisabMate-v1.0.2-release.aab) / [Checksums](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.2/SHA256SUMS.txt) |
| **v1.0.1** | 2026-10-31 | Database query indexing for performance, permanent demo deletion with idempotent restart guards (Room v6), and physical PDF statement export with MediaStore Scoped Storage. | [APK](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.1/HisabMate-v1.0.1-release.apk) / [AAB](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.1/HisabMate-v1.0.1-release.aab) / [Checksums](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.1/SHA256SUMS.txt) |
| **v1.0.0** | 2026-10-31 | Initial production release featuring 14 ISO 4217 currencies, offline FX engine, multi-rail wallets, budgets, savings goals, debt ledger, and local notifications. | [APK](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.0/HisabMate-v1.0.0-release.apk) / [AAB](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.0/HisabMate-v1.0.0-release.aab) / [Checksums](https://github.com/CodeByArnob/HisabMate/releases/download/v1.0.0/SHA256SUMS.txt) |

---

## Contributing

Contributions are welcome. Please adhere to the following workflow:

1. Fork the repository and create a feature branch (`git checkout -b feature/improvement-name`).
2. Implement your changes adhering to Jetpack Compose Material 3 design guidelines and unidirectional data flows.
3. Ensure all tests pass (`gradle :app:testDebugUnitTest`).
4. Commit your changes with conventional commit messages (`feat:`, `fix:`, `docs:`, `perf:`).
5. Open a Pull Request against the `main` branch.

---

## License

This project is licensed under the **MIT License**. Refer to the [LICENSE](LICENSE) file for complete details.

Copyright © 2026 [CodeByArnob](https://github.com/CodeByArnob). All rights reserved.
