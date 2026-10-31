<div align="center">

<img src="docs/screenshots/hisabmate-banner.svg" alt="HisabMate – Global Multi-Currency Personal Finance & Expense Manager Banner" width="100%" />

<br />

# HisabMate – Smart Multi-Currency Personal Finance Manager

**A privacy-first, offline-native international personal finance, budgeting, multi-currency wallet, debt ledger, and wealth analytics application for Android.**

[![Version](https://img.shields.io/badge/Release-v1.0.0-7C5CFF?style=for-the-badge&logo=github)](https://github.com/CodeByArnob/HisabMate/releases/tag/v1.0.0)
[![Android CI](https://img.shields.io/github/actions/workflow/status/CodeByArnob/HisabMate/android-ci.yml?branch=main&style=for-the-badge&logo=android&label=Android%20CI)](https://github.com/CodeByArnob/HisabMate/actions/workflows/android-ci.yml)
[![Website](https://img.shields.io/badge/GitHub%20Pages-Live%20Website-10B981?style=for-the-badge&logo=googlechrome)](https://codebyarnob.github.io/HisabMate/)
[![Platform](https://img.shields.io/badge/Android-API%2024%E2%80%9336-3DDC84?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

[**Live Project Website**](https://codebyarnob.github.io/HisabMate/) • [**Latest Release (`v1.0.0`)**](https://github.com/CodeByArnob/HisabMate/releases/tag/v1.0.0) • [**Screenshot Gallery**](#-application-screenshots) • [**Architecture**](#-architecture--engineering) • [**Build & Test**](#-installation--build-instructions)

</div>

---

## Overview

**HisabMate** (`com.aistudio.hisabmate.fntkbd`) is a modern Android personal finance application engineered with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room SQLite**. Designed for global professionals, freelancers, travelers, and households managing money across multiple currencies and payment rails (bank cards, mobile financial services like **bKash**, **Nagad**, **Rocket**, international **Wise / Revolut** accounts, and cash wallets), HisabMate combines institutional-grade financial clarity with **100% offline-first privacy**.

Every monetary record is stored in its **authentic native ISO 4217 currency** alongside a snapshot exchange rate, while dashboard net worth, category budgets, and cashflow reports dynamically consolidate into your chosen base currency.

---

## Application Screenshots

All screenshots below are rendered directly from the HisabMate Jetpack Compose UI using realistic sample data.

### Light Mode vs. Dark Mode Comparison

| **Home Dashboard (Light Mode)** | **Home Dashboard (Dark Mode)** |
| :---: | :---: |
| <img src="docs/screenshots/dashboard-light.png" alt="HisabMate Home Dashboard Light Mode" width="380" /> | <img src="docs/screenshots/dashboard-dark.png" alt="HisabMate Home Dashboard Dark Mode" width="380" /> |
| *Soft Slate-Cool canvas (`#E6ECF5`) with elevated `#FFFFFF` cards and high-contrast ink typography* | *Obsidian surface hierarchy (`#0A0D14` / `#131824`) calibrated for zero eye strain* |

### Core Financial Flows & Modules

| **Multi-Currency & Offline FX Converter** | **Accounts, Cards & MFS Wallets** | **Category Wallet & Daily Spend Curve** |
| :---: | :---: | :---: |
| <img src="docs/screenshots/multi-currency-fx.png" alt="Multi-Currency & Offline FX Converter" width="260" /> | <img src="docs/screenshots/accounts-wallets.png" alt="Accounts and Wallets" width="260" /> | <img src="docs/screenshots/wallet-details-analytics.png" alt="Wallet Details and Daily Spend Curve" width="260" /> |

| **Budget Management & Thresholds** | **Spend Analysis & Cashflow Reports** | **Savings Goals & Milestone Pots** |
| :---: | :---: | :---: |
| <img src="docs/screenshots/budget-management.png" alt="Budget Management" width="260" /> | <img src="docs/screenshots/financial-reports.png" alt="Spend Analysis and Financial Reports" width="260" /> | <img src="docs/screenshots/savings-goals.png" alt="Savings Goals" width="260" /> |

| **Debt & Loan Ledger** | **Scheduled Reminders & Alerts** | **Security, Backup & Settings** |
| :---: | :---: | :---: |
| <img src="docs/screenshots/debt-management.png" alt="Debt and Loan Management" width="260" /> | <img src="docs/screenshots/bills-notifications.png" alt="Bills and Notifications" width="260" /> | <img src="docs/screenshots/settings-security.png" alt="Settings and Security" width="260" /> |

| **Interactive 7-Step Onboarding Experience** |
| :---: |
| <img src="docs/screenshots/onboarding-overview.png" alt="Interactive 7-Step Onboarding" width="320" /> |

---

## Key Features

### 1. Authentic Multi-Currency Architecture & Offline FX Engine
- **14 Built-in ISO 4217 Currencies**: Native symbols, regional decimal precision (`0` decimals for `JPY`, `2` for `BDT`/`USD`/`EUR`/`GBP`, `3` for `KWD`), and optional South Asian (`1,25,000`) vs. International (`125,000`) digit grouping.
- **Per-Entity Native Currency Persistence**: Accounts, transactions, budgets, recurring bills, savings goals, debt records, and scheduled notifications each preserve their own native `currencyCode`.
- **Interactive Offline FX Converter & Custom Rates**: Convert between any supported currency pair offline and override exchange rates manually in the **Currencies & FX Rates** manager.

### 2. Multi-Rail Account & Wallet Management
- Track **Bank Accounts**, **Credit/Debit Cards**, **Mobile Financial Services (bKash, Nagad, Rocket)**, **Global Multi-Currency Accounts (Wise, Revolut)**, and **Cash Wallets**.
- Instant inter-account transfers with automatic cross-currency conversion when moving funds between accounts of different currencies.

### 3. Smart Transactions, Split Bills & Receipt OCR Simulation
- Record **Expenses**, **Income**, and **Transfers** with merchant metadata, notes, favorite templates, and split-expense breakdowns.
- Interactive **Wallet Details** view with daily spend line charts, month-over-month navigation, and quick category filtering.

### 4. Multi-Period Budgets & Proactive Threshold Alerts
- Configure category budgets across **Daily**, **Weekly**, **Monthly**, **Yearly**, or **Custom** date ranges in any ISO currency.
- Customizable alert thresholds (`50%`–`100%`) that automatically trigger local notifications when spending approaches or exceeds limits.

### 5. Planning Hub: Recurring Bills, Debt Ledger & Savings Goals
- **Recurring Payments**: Track subscriptions, rent, utilities, and recurring income with one-tap bill settlement.
- **Debt & Loan Ledger**: Separate tracking for **Money Lent (Receivables)** and **Money Borrowed (Payables)** with partial repayment history and progress bars.
- **Savings Goals**: Dedicated visual pots with target deadlines, milestone tracking (`25%`, `50%`, `75%`, `100%`), and instant contributions.

### 6. Currency-Aware Local Notifications & Scheduled Reminders
- Built on Android `NotificationCompat` and `AlarmManager` with 4 dedicated notification channels (`Bill Reminders`, `Budget Threshold Alerts`, `Debt & Loan Reminders`, `Savings Goal Milestones`).
- Automatically reschedules active reminders across device reboots and timezone changes via `ReminderBootAndTimeReceiver`.

### 7. Security, Offline Privacy & Data Portability
- **100% Offline-First**: Zero cloud lock-in, zero third-party analytics SDKs, and zero telemetry.
- **App Lock**: Optional 4-digit PIN lock overlay and biometric unlock flow.
- **Full Data Ownership**: Export and restore complete **JSON Backups**, export/import **CSV Spreadsheets**, and generate **PDF Financial Statements**.

---

## Supported Currencies (ISO 4217)

| Code | Symbol | Currency Name | Region | Decimals | Grouping Support |
| :--- | :---: | :--- | :--- | :---: | :--- |
| **BDT** | `৳` | Bangladeshi Taka | Bangladesh | 2 | South Asian (`1,25,000.00`) & Standard |
| **USD** | `$` | US Dollar | United States & Global | 2 | International (`125,000.00`) |
| **EUR** | `€` | Euro | European Union | 2 | International |
| **GBP** | `£` | British Pound Sterling | United Kingdom | 2 | International |
| **INR** | `₹` | Indian Rupee | India | 2 | South Asian & Standard |
| **JPY** | `¥` | Japanese Yen | Japan | 0 | International (`¥15,800`) |
| **CNY** | `¥` | Chinese Yuan Renminbi | China | 2 | International |
| **SAR** | `ر.س` | Saudi Riyal | Saudi Arabia | 2 | International |
| **AED** | `د.إ` | UAE Dirham | United Arab Emirates | 2 | International |
| **CAD** | `CA$` | Canadian Dollar | Canada | 2 | International |
| **AUD** | `A$` | Australian Dollar | Australia | 2 | International |
| **SGD** | `S$` | Singapore Dollar | Singapore | 2 | International |
| **MYR** | `RM` | Malaysian Ringgit | Malaysia | 2 | International |
| **KWD** | `د.ك` | Kuwaiti Dinar | Kuwait | 3 | International (`3` decimals) |

---

## Architecture & Engineering

HisabMate follows **Clean MVVM Architecture** with unidirectional data flow (`StateFlow`) and reactive Room SQLite persistence:

```text
com.example
├── MainActivity.kt                  # Edge-to-edge Activity host & root navigation state machine
├── data
│   ├── local
│   │   ├── Entities.kt              # 10 Room SQLite entities (Accounts, Transactions, Budgets, FX, etc.)
│   │   ├── HisabDao.kt              # Reactive Flow queries & transactional DAO operations
│   │   └── HisabDatabase.kt         # Room Database (v5) + realistic multi-currency seed generator
│   └── repository
│       └── HisabRepository.kt       # Single source of truth, FX conversion, JSON/CSV backup & restore
├── ui
│   ├── components
│   │   └── FintechComponents.kt     # Reusable M3 cards, interactive Canvas charts, dock & badges
│   ├── screens
│   │   ├── DashboardScreen.kt       # Balanced home dashboard, quick actions & wallet details
│   │   ├── AccountsAndMoreScreens.kt# Accounts, global search, notifications & settings/security
│   │   ├── PlanningHubScreen.kt     # Budgets, recurring bills, debt ledger & savings goals
│   │   ├── CurrencyManagementScreen.kt # Offline FX converter & ISO 4217 rate manager
│   │   ├── SpendAnalysisScreen.kt   # Multi-period analytics & cashflow bar charts
│   │   ├── TransactionModals.kt     # Add/edit transaction sheet, split bills & receipt OCR presets
│   │   ├── CalendarAndSecurityScreens.kt # Monthly calendar heatmap & PIN lock screen
│   │   └── CinematicOnboardingScreen.kt  # Interactive 7-step fintech onboarding flow
│   ├── theme
│   │   ├── Color.kt                 # Semantic Light/Dark surface, ink, border & category palettes
│   │   ├── Theme.kt                 # HisabTheme tokens & Material 3 ColorScheme provider
│   │   └── Type.kt                  # Space Grotesk, Plus Jakarta Sans & Inter typography hierarchy
│   └── viewmodel
│       └── HisabViewModel.kt        # Reactive UI state aggregation & multi-currency math
└── util
    ├── CurrencyFormatter.kt         # ISO 4217 catalog, regional formatting & cross-rate math
    ├── HisabNotificationManager.kt  # AlarmManager scheduler, notification channels & receivers
    └── HapticFeedbackHelper.kt      # Tactile feedback helper
```

---

## Tech Stack

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose + Material Design 3 (`MaterialTheme` + custom `HisabTheme` semantic tokens)
- **Local Persistence**: AndroidX Room SQLite (`2.7.x` with KSP compiler)
- **Asynchronous Concurrency**: Kotlin Coroutines & `StateFlow` (`collectAsStateWithLifecycle`)
- **Typography**: Locally bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `Inter`)
- **Testing**: JUnit 4, AndroidX Compose UI Test, and **Robolectric (`@GraphicsMode(NATIVE)`)**

---

## Android Permissions Explained

HisabMate adheres strictly to the principle of least privilege and operates 100% offline:

| Permission | Required? | Purpose |
| :--- | :---: | :--- |
| `android.permission.POST_NOTIFICATIONS` | Optional (Runtime on Android 13+) | Delivers local bill due alerts, budget threshold warnings, debt reminders, and savings milestone notifications. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal (Install-time) | Allows `ReminderBootAndTimeReceiver` to reschedule active local `AlarmManager` reminders after a device reboot. |
| `android.permission.VIBRATE` | Normal (Install-time) | Provides subtle haptic feedback on financial actions and notification alerts when enabled in Settings. |
| `android.permission.CAMERA` | Optional (`android:required="false"`) | Used only when capturing a physical receipt photo to attach to a transaction. |

> **Note**: HisabMate does **not** request `INTERNET` or broad external storage permissions. Photo attachments use the zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).

---

## Installation & Build Instructions

### Option 1: Clone & Build Locally

1. **Clone the repository**
   ```bash
   git clone https://github.com/CodeByArnob/HisabMate.git
   cd HisabMate
   ```

2. **Prepare environment configuration**
   ```bash
   cp .env.example .env
   ```

3. **Build the Debug APK**
   ```bash
   gradle :app:assembleDebug
   ```
   The compiled debug APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Building a Signed Production Release (`APK` / `AAB`)

Production release signing in `app/build.gradle.kts` reads credentials from environment variables so signing keys are never committed to source control:

```bash
export KEYSTORE_PATH="/path/to/my-upload-key.jks"
export STORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"

gradle :app:assembleRelease :app:bundleRelease
```

To enable automated signed release artifacts in **GitHub Actions** (`.github/workflows/release.yml`), add the following repository secrets under **Settings → Secrets and variables → Actions**:
- `UPLOAD_KEYSTORE_BASE64`: Base64-encoded upload keystore (`base64 -w 0 my-upload-key.jks`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password (for alias `upload`)

---

## Running Automated Tests

HisabMate includes a comprehensive **Robolectric JVM & Compose UI test suite** (`app/src/test/java/com/example/ExampleRobolectricTest.kt`) that verifies:
- Multi-currency ISO 4217 symbol formatting and cross-currency conversion accuracy
- Room SQLite persistence for accounts, budgets, recurring bills, and currency-aware scheduled reminders
- JSON full backup/restore and CSV export round-trip integrity
- Notification channel initialization
- Responsive Compose UI layout verification and native screenshot rendering

Run all tests with:
```bash
gradle :app:testDebugUnitTest
```

---

## Project Structure

```text
HisabMate/
├── .github/workflows/
│   ├── android-ci.yml          # Continuous Integration: Gradle build + Robolectric test suite
│   ├── website-pages.yml       # Automated GitHub Pages deployment from /website
│   └── release.yml             # Tag-triggered release verification & signed APK/AAB packaging
├── app/
│   ├── build.gradle.kts        # App module Gradle config (com.aistudio.hisabmate.fntkbd, v1.0.0)
│   ├── proguard-rules.pro      # R8 / ProGuard keep rules for Room & BroadcastReceivers
│   └── src/
│       ├── main/               # AndroidManifest.xml, Kotlin sources, fonts, adaptive launcher icon
│       └── test/               # Robolectric unit, Compose UI & native screenshot tests
├── docs/screenshots/           # Genuine PNG screenshots & SVG project banner
├── website/                    # Responsive static showcase website deployed to GitHub Pages
├── CONTRIBUTING.md             # Contributor workflow & engineering guidelines
├── LICENSE                     # MIT License
└── README.md                   # Project documentation
```

---

## Contributing & Issue Reporting

Contributions, bug reports, and feature requests are welcome!
- Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a Pull Request.
- Report bugs or request enhancements via [GitHub Issues](https://github.com/CodeByArnob/HisabMate/issues).

---

## License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for full license text.

Copyright © 2026 [CodeByArnob (Gameofbtc)](https://github.com/CodeByArnob).
