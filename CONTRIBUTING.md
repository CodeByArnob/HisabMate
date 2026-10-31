# Contributing to HisabMate

Thank you for your interest in contributing to **HisabMate – Global Multi-Currency Personal Finance & Expense Manager**!

## Development Prerequisites

- **JDK**: Java 17 or Java 21
- **Android Studio**: Ladybug (2024.2.1) or newer
- **Android SDK**: API Level 36 (`compileSdk 36`, `minSdk 24`)
- **Kotlin & Jetpack Compose**: Configured via Gradle Version Catalog (`gradle/libs.versions.toml`)

## Getting Started

1. **Fork and Clone the Repository**
   ```bash
   git clone https://github.com/CodeByArnob/HisabMate.git
   cd HisabMate
   ```

2. **Environment Configuration**
   Copy `.env.example` to `.env` if needed for local properties:
   ```bash
   cp .env.example .env
   ```
   > **Security Note**: Never commit `.env`, `local.properties`, `.jks`, or `.keystore` files, or hardcode API keys/tokens in source code.

3. **Run Unit & Robolectric UI Tests**
   ```bash
   gradle :app:testDebugUnitTest
   ```

4. **Build Debug APK**
   ```bash
   gradle :app:assembleDebug
   ```

## Architectural Guidelines

- **Offline-First & Zero-Telemetry**: All user financial records (accounts, transactions, budgets, recurring bills, debts, savings goals, scheduled reminders, and custom FX rates) must persist locally in Room SQLite (`HisabDatabase`) with no external tracking.
- **Authentic ISO 4217 Multi-Currency Handling**: Always store monetary values in their original native currency alongside `currencyCode`, and use `CurrencyFormatter` + `CurrencyCatalog` for regional symbol formatting (`৳`, `$`, `€`, `£`, `₹`, `¥`, `ر.س`, `د.إ`, etc.) and base-currency aggregation.
- **Theme Tokens**: Use semantic tokens from `HisabTheme.colors` (`Color.kt` / `Theme.kt`) so both Light Mode (`#E6ECF5` soft slate canvas with `#FFFFFF` elevated cards) and Dark Mode (`#0A0D14` obsidian canvas) maintain WCAG AA/AAA readability.
- **Accessibility & Testability**: Include meaningful `contentDescription` on interactive icons and `Modifier.testTag(...)` on primary action controls.

## Pull Request Process

1. Create a feature or bugfix branch from `main` (`git checkout -b feat/your-feature` or `fix/issue-description`).
2. Ensure all existing and new Robolectric tests pass (`gradle :app:testDebugUnitTest`).
3. Verify light and dark mode contrast and responsive behavior on compact and wide screens.
4. Open a Pull Request with a clear summary of changes and screenshots for any UI updates.

## Code of Conduct

Be respectful, constructive, and collaborative. Security or privacy issues should be reported responsibly via GitHub Issues.
