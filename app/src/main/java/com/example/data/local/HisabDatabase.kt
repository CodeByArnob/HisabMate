package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.util.CurrencyCatalog

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringPaymentEntity::class,
        DebtRecordEntity::class,
        SavingsGoalEntity::class,
        AppNotificationEntity::class,
        ScheduledReminderEntity::class,
        ExchangeRateEntity::class,
        UserPreferencesEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun hisabDao(): HisabDao

    companion object {
        @Volatile
        private var INSTANCE: HisabDatabase? = null

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recurring_payments ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debt_records ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scheduled_reminders ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_notifications ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN demoDataSeeded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN demoDataDeleted INTEGER NOT NULL DEFAULT 0")

                // Indices for performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isPrimary ON accounts(isPrimary)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_currencyCode ON accounts(currencyCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isDemo ON accounts(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_type ON categories(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_isDemo ON categories(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isFavorite ON transactions(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isDemo ON transactions(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_payments_nextDueDateMillis ON recurring_payments(nextDueDateMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_payments_categoryId ON recurring_payments(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_payments_isDemo ON recurring_payments(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_records_dueDateMillis ON debt_records(dueDateMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_records_type ON debt_records(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_records_isDemo ON debt_records(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_savings_goals_deadlineMillis ON savings_goals(deadlineMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_savings_goals_isDemo ON savings_goals(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_reminders_triggerTimeMillis ON scheduled_reminders(triggerTimeMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_reminders_sourceType_sourceId ON scheduled_reminders(sourceType, sourceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_reminders_notificationId ON scheduled_reminders(notificationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_reminders_isDemo ON scheduled_reminders(isDemo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_timestamp ON app_notifications(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_isRead ON app_notifications(isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_isDemo ON app_notifications(isDemo)")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountExchangeRateSnapshotToBdt REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN toAccountExchangeRateSnapshotToBdt REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN appPinHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN appPinSalt TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    UPDATE transactions SET accountExchangeRateSnapshotToBdt = COALESCE(
                      (SELECT rateToBdt FROM exchange_rates WHERE UPPER(currencyCode)=UPPER(
                       (SELECT currencyCode FROM accounts WHERE id=transactions.accountId)) LIMIT 1), 1.0)
                """.trimIndent())
                db.execSQL("""
                    UPDATE transactions SET toAccountExchangeRateSnapshotToBdt = COALESCE(
                      (SELECT rateToBdt FROM exchange_rates WHERE UPPER(currencyCode)=UPPER(
                       (SELECT currencyCode FROM accounts WHERE id=transactions.toAccountId)) LIMIT 1), 1.0)
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): HisabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HisabDatabase::class.java,
                    "hisabmate_finance_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object SeedDataGenerator {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun initialExchangeRates(): List<ExchangeRateEntity> {
        val now = System.currentTimeMillis()
        return CurrencyCatalog.supportedCurrencies.map { meta ->
            ExchangeRateEntity(
                currencyCode = meta.code,
                rateToBdt = meta.defaultRateToBdt,
                lastUpdatedMillis = now - 25L * 60L * 1000L,
                sourceLabel = "Offline Verified Rate",
                isCustomRate = false
            )
        }
    }

    fun initialAccounts(): List<AccountEntity> = listOf(
        AccountEntity(
            id = 1,
            name = "BRAC Bank Visa Signature",
            type = "BANK",
            balance = 142500.0,
            currencyCode = "BDT",
            maskedNumber = "Card •••• 4821",
            colorHex = 0xFF6D4AFFL,
            iconKey = "BANK",
            isPrimary = true
        ),
        AccountEntity(
            id = 2,
            name = "bKash Personal Wallet",
            type = "MFS",
            balance = 28450.50,
            currencyCode = "BDT",
            maskedNumber = "bKash •• 9012",
            colorHex = 0xFFE2136EL,
            iconKey = "MFS",
            isPrimary = false
        ),
        AccountEntity(
            id = 3,
            name = "City Bank Amex Gold",
            type = "CARD",
            balance = 54200.0,
            currencyCode = "BDT",
            maskedNumber = "Card •••• 1234",
            colorHex = 0xFF0084E9L,
            iconKey = "CARD",
            isPrimary = false
        ),
        AccountEntity(
            id = 4,
            name = "Wise Global USD Account",
            type = "BANK",
            balance = 385.00, // Stored in USD
            currencyCode = "USD",
            maskedNumber = "USD •••• 7740",
            colorHex = 0xFF059669L,
            iconKey = "BANK",
            isPrimary = false
        ),
        AccountEntity(
            id = 5,
            name = "Nagad & Cash Wallet",
            type = "CASH",
            balance = 23500.30,
            currencyCode = "BDT",
            maskedNumber = "Cash & Nagad",
            colorHex = 0xFFF26419L,
            iconKey = "CASH",
            isPrimary = false
        )
    ).map { it.copy(isDemo = true) }

    fun initialCategories(): List<CategoryEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CategoryEntity(
                id = 1,
                name = "Groceries",
                type = "EXPENSE",
                monthlyBudget = 32000.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 80,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFF6D4AFFL, // Royal Violet
                iconKey = "GROCERIES",
                isQuickAction = true
            ),
            CategoryEntity(
                id = 2,
                name = "Transport",
                type = "EXPENSE",
                monthlyBudget = 16000.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 80,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFF0084E9L, // Cobalt Blue
                iconKey = "TRANSPORT",
                isQuickAction = true
            ),
            CategoryEntity(
                id = 3,
                name = "Entertainment",
                type = "EXPENSE",
                monthlyBudget = 15000.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 85,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFF059669L, // Emerald Green
                iconKey = "ENTERTAINMENT",
                isQuickAction = true
            ),
            CategoryEntity(
                id = 4,
                name = "Rent & Utilities",
                type = "EXPENSE",
                monthlyBudget = 42000.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 90,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFFF26419L, // Vibrant Tangerine
                iconKey = "RENT",
                isQuickAction = true
            ),
            CategoryEntity(
                id = 5,
                name = "Dining & Cafe",
                type = "EXPENSE",
                monthlyBudget = 12000.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 80,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFFDB2777L, // Crimson Rose
                iconKey = "DINING",
                isQuickAction = false
            ),
            CategoryEntity(
                id = 6,
                name = "Healthcare",
                type = "EXPENSE",
                monthlyBudget = 10000.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 80,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFF0891B2L, // Deep Cyan
                iconKey = "HEALTH",
                isQuickAction = false
            ),
            CategoryEntity(
                id = 7,
                name = "Salary & Payroll",
                type = "INCOME",
                monthlyBudget = 0.0,
                budgetCurrencyCode = "BDT",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 100,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFF059669L,
                iconKey = "SALARY",
                isQuickAction = false
            ),
            CategoryEntity(
                id = 8,
                name = "Freelance & FX",
                type = "INCOME",
                monthlyBudget = 0.0,
                budgetCurrencyCode = "USD",
                budgetPeriod = "MONTHLY",
                alertThresholdPercent = 100,
                startDateMillis = now - 14 * DAY_MS,
                endDateMillis = now + 16 * DAY_MS,
                colorHex = 0xFF6D4AFFL,
                iconKey = "FREELANCE",
                isQuickAction = false
            )
        ).map { it.copy(isDemo = true) }
    }

    fun initialTransactions(): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            // Groceries
            TransactionEntity(
                id = 1,
                title = "Shwapno Superstore",
                merchant = "Shwapno Gulshan-2",
                amount = 4850.50,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Groceries",
                categoryColorHex = 0xFF6D4AFFL,
                categoryIconKey = "GROCERIES",
                accountId = 3,
                accountLabel = "Card •••• 1234",
                timestamp = now - 2L * 3600_000L,
                note = "Weekly fresh produce, olive oil & dairy",
                isFavorite = true
            ),
            TransactionEntity(
                id = 2,
                title = "Fresh Artisan Bakery",
                merchant = "Cooper's Bakery Banani",
                amount = 1420.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Groceries",
                categoryColorHex = 0xFF6D4AFFL,
                categoryIconKey = "GROCERIES",
                accountId = 1,
                accountLabel = "Paid with Visa",
                timestamp = now - 1L * DAY_MS,
                note = "Sourdough loaves & breakfast pastries"
            ),
            TransactionEntity(
                id = 3,
                title = "Chaldal Express Delivery",
                merchant = "Chaldal Grocery",
                amount = 6480.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Groceries",
                categoryColorHex = 0xFF6D4AFFL,
                categoryIconKey = "GROCERIES",
                accountId = 2,
                accountLabel = "Paid with bKash",
                timestamp = now - 2L * DAY_MS,
                note = "Monthly pantry staples, basmati rice & spices",
                splitDetails = "Groceries: 4800 | Household: 1680",
                isFavorite = true
            ),
            TransactionEntity(
                id = 4,
                title = "Unimart Gourmet Market",
                merchant = "Unimart Dhanmondi",
                amount = 5920.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Groceries",
                categoryColorHex = 0xFF6D4AFFL,
                categoryIconKey = "GROCERIES",
                accountId = 3,
                accountLabel = "Card •••• 1234",
                timestamp = now - 5L * DAY_MS,
                note = "Imported cheese, coffee beans & snacks"
            ),
            TransactionEntity(
                id = 5,
                title = "Agora Organic Produce",
                merchant = "Agora Uttara",
                amount = 6184.80,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 1,
                categoryName = "Groceries",
                categoryColorHex = 0xFF6D4AFFL,
                categoryIconKey = "GROCERIES",
                accountId = 1,
                accountLabel = "Paid with Visa",
                timestamp = now - 7L * DAY_MS,
                note = "Seafood & organic vegetables"
            ),

            // Transport
            TransactionEntity(
                id = 6,
                title = "Pathao Car Plus",
                merchant = "Pathao Ride",
                amount = 680.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 2,
                categoryName = "Transport",
                categoryColorHex = 0xFF0084E9L,
                categoryIconKey = "TRANSPORT",
                accountId = 2,
                accountLabel = "Paid with bKash",
                timestamp = now - 1L * DAY_MS - 4L * 3600_000L,
                note = "Airport drop-off via Elevated Expressway",
                isFavorite = true
            ),
            TransactionEntity(
                id = 7,
                title = "Padma Oil Filling Station",
                merchant = "Trust Filling Station",
                amount = 4850.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 2,
                categoryName = "Transport",
                categoryColorHex = 0xFF0084E9L,
                categoryIconKey = "TRANSPORT",
                accountId = 3,
                accountLabel = "Card •••• 1234",
                timestamp = now - 4L * DAY_MS,
                note = "Full tank Octane 95"
            ),
            TransactionEntity(
                id = 8,
                title = "Dhaka Metro MRT Pass",
                merchant = "MRT Line-6 Rapid Pass",
                amount = 1800.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 2,
                categoryName = "Transport",
                categoryColorHex = 0xFF0084E9L,
                categoryIconKey = "TRANSPORT",
                accountId = 5,
                accountLabel = "Paid with Nagad",
                timestamp = now - 6L * DAY_MS,
                note = "Monthly MRT commuter pass recharge"
            ),
            TransactionEntity(
                id = 9,
                title = "Uber Intercity Trip",
                merchant = "Uber Premier",
                amount = 3590.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 2,
                categoryName = "Transport",
                categoryColorHex = 0xFF0084E9L,
                categoryIconKey = "TRANSPORT",
                accountId = 1,
                accountLabel = "Paid with Visa",
                timestamp = now - 9L * DAY_MS,
                note = "Client meeting commute"
            ),

            // Entertainment (includes foreign USD subscription)
            TransactionEntity(
                id = 10,
                title = "Netflix & Spotify Duo",
                merchant = "Digital Subscriptions",
                amount = 18.99, // USD transaction
                currencyCode = "USD",
                exchangeRateSnapshotToBdt = 120.50,
                type = "EXPENSE",
                categoryId = 3,
                categoryName = "Entertainment",
                categoryColorHex = 0xFF059669L,
                categoryIconKey = "ENTERTAINMENT",
                accountId = 4,
                accountLabel = "USD •••• 7740",
                timestamp = now - 3L * DAY_MS,
                note = "4K UHD Family + Spotify Premium (Charged in USD)",
                isRecurring = true
            ),
            TransactionEntity(
                id = 11,
                title = "Star Cineplex IMAX",
                merchant = "Bashundhara City IMAX",
                amount = 3400.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 3,
                categoryName = "Entertainment",
                categoryColorHex = 0xFF059669L,
                categoryIconKey = "ENTERTAINMENT",
                accountId = 2,
                accountLabel = "Paid with bKash",
                timestamp = now - 5L * DAY_MS,
                note = "Weekend movie night & gourmet popcorn"
            ),
            TransactionEntity(
                id = 12,
                title = "PlayStation Plus Deluxe",
                merchant = "Sony Interactive",
                amount = 44.00, // USD transaction
                currencyCode = "USD",
                exchangeRateSnapshotToBdt = 120.50,
                type = "EXPENSE",
                categoryId = 3,
                categoryName = "Entertainment",
                categoryColorHex = 0xFF059669L,
                categoryIconKey = "ENTERTAINMENT",
                accountId = 4,
                accountLabel = "USD •••• 7740",
                timestamp = now - 10L * DAY_MS,
                note = "Annual gaming membership renewal"
            ),

            // Rent & Utilities
            TransactionEntity(
                id = 13,
                title = "Apartment Rent & Service",
                merchant = "Gulshan Residence",
                amount = 28500.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 4,
                categoryName = "Rent & Utilities",
                categoryColorHex = 0xFFF26419L,
                categoryIconKey = "RENT",
                accountId = 1,
                accountLabel = "BRAC Bank EFT",
                timestamp = now - 3L * DAY_MS,
                note = "Monthly apartment lease + building service charge",
                isRecurring = true
            ),
            TransactionEntity(
                id = 14,
                title = "DESCO Smart Prepaid Bill",
                merchant = "DESCO Electricity",
                amount = 4850.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 4,
                categoryName = "Rent & Utilities",
                categoryColorHex = 0xFFF26419L,
                categoryIconKey = "RENT",
                accountId = 2,
                accountLabel = "Paid with bKash",
                timestamp = now - 6L * DAY_MS,
                note = "Meter #4401928310 recharge",
                isRecurring = true
            ),
            TransactionEntity(
                id = 15,
                title = "Amber IT 100Mbps Fiber",
                merchant = "Amber IT Broadband",
                amount = 2500.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 4,
                categoryName = "Rent & Utilities",
                categoryColorHex = 0xFFF26419L,
                categoryIconKey = "RENT",
                accountId = 2,
                accountLabel = "Paid with bKash",
                timestamp = now - 8L * DAY_MS,
                note = "Home office fiber optic internet",
                isRecurring = true
            ),

            // Dining & Healthcare
            TransactionEntity(
                id = 16,
                title = "North End Coffee Roasters",
                merchant = "North End Shahjadpur",
                amount = 1650.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 5,
                categoryName = "Dining & Cafe",
                categoryColorHex = 0xFFDB2777L,
                categoryIconKey = "DINING",
                accountId = 3,
                accountLabel = "Card •••• 1234",
                timestamp = now - 2L * DAY_MS,
                note = "Cappuccino & grilled panini"
            ),
            TransactionEntity(
                id = 17,
                title = "Lazz Pharma Wellness",
                merchant = "Lazz Pharma Kalabagan",
                amount = 2250.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "EXPENSE",
                categoryId = 6,
                categoryName = "Healthcare",
                categoryColorHex = 0xFF0891B2L,
                categoryIconKey = "HEALTH",
                accountId = 1,
                accountLabel = "Paid with Visa",
                timestamp = now - 6L * DAY_MS,
                note = "Vitamins & seasonal prescriptions"
            ),

            // Income Entries (BDT Salary + USD International Client Payout)
            TransactionEntity(
                id = 18,
                title = "Senior Product Lead Salary",
                merchant = "TechVantage Global Ltd.",
                amount = 165000.00,
                currencyCode = "BDT",
                exchangeRateSnapshotToBdt = 1.0,
                type = "INCOME",
                categoryId = 7,
                categoryName = "Salary & Payroll",
                categoryColorHex = 0xFF059669L,
                categoryIconKey = "SALARY",
                accountId = 1,
                accountLabel = "BRAC Bank Visa",
                timestamp = now - 4L * DAY_MS,
                note = "Monthly payroll direct deposit",
                isRecurring = true
            ),
            TransactionEntity(
                id = 19,
                title = "Fintech UI Design Retainer",
                merchant = "Stripe / Wise USD Payout",
                amount = 240.00, // Recorded in native USD
                currencyCode = "USD",
                exchangeRateSnapshotToBdt = 120.50,
                type = "INCOME",
                categoryId = 8,
                categoryName = "Freelance & FX",
                categoryColorHex = 0xFF6D4AFFL,
                categoryIconKey = "FREELANCE",
                accountId = 4,
                accountLabel = "Wise USD •••• 7740",
                timestamp = now - 2L * DAY_MS,
                note = "Design system architecture consulting ($240.00 USD)"
            )
        ).map { it.copy(isDemo = true) }
    }

    fun initialRecurringPayments(): List<RecurringPaymentEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            RecurringPaymentEntity(
                id = 1,
                title = "Grameenphone Postpaid Plan",
                provider = "Grameenphone Star",
                amount = 1600.0,
                currencyCode = "BDT",
                transactionType = "EXPENSE",
                frequency = "Monthly",
                nextDueDateMillis = now + 2L * DAY_MS,
                categoryId = 4,
                categoryName = "Rent & Utilities",
                colorHex = 0xFFF26419L,
                iconKey = "RENT",
                autoPay = true
            ),
            RecurringPaymentEntity(
                id = 2,
                title = "Adobe Creative Cloud & Figma",
                provider = "Figma Inc. (USD Billing)",
                amount = 24.0,
                currencyCode = "USD",
                transactionType = "EXPENSE",
                frequency = "Monthly",
                nextDueDateMillis = now + 5L * DAY_MS,
                categoryId = 3,
                categoryName = "Entertainment",
                colorHex = 0xFF059669L,
                iconKey = "ENTERTAINMENT",
                autoPay = true
            ),
            RecurringPaymentEntity(
                id = 3,
                title = "DPS Monthly Savings Installment",
                provider = "BRAC Bank Tara DPS",
                amount = 15000.0,
                currencyCode = "BDT",
                transactionType = "EXPENSE",
                frequency = "Monthly",
                nextDueDateMillis = now + 9L * DAY_MS,
                categoryId = 4,
                categoryName = "Rent & Utilities",
                colorHex = 0xFF6D4AFFL,
                iconKey = "BANK",
                autoPay = true
            ),
            RecurringPaymentEntity(
                id = 4,
                title = "WASA & Titas Gas Utility",
                provider = "Dhaka WASA / bKash PayBill",
                amount = 2450.0,
                currencyCode = "BDT",
                transactionType = "EXPENSE",
                frequency = "Monthly",
                nextDueDateMillis = now + 12L * DAY_MS,
                categoryId = 4,
                categoryName = "Rent & Utilities",
                colorHex = 0xFF0084E9L,
                iconKey = "RENT",
                autoPay = false
            )
        ).map { it.copy(isDemo = true) }
    }

    fun initialDebts(): List<DebtRecordEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            DebtRecordEntity(
                id = 1,
                personName = "Tanvir Ahmed",
                relationOrNote = "Cox's Bazar flight & resort split",
                totalAmount = 25000.0,
                settledAmount = 10000.0,
                currencyCode = "BDT",
                type = "LENT",
                dueDateMillis = now + 4L * DAY_MS,
                phoneOrTag = "bKash • 01711-•••482",
                colorHex = 0xFF059669L,
                repaymentHistory = "Oct 10: +৳10,000 via bKash Personal"
            ),
            DebtRecordEntity(
                id = 2,
                personName = "Nadia Karim",
                relationOrNote = "Shared studio monitor import",
                totalAmount = 180.0,
                settledAmount = 60.0,
                currencyCode = "USD",
                type = "LENT",
                dueDateMillis = now + 10L * DAY_MS,
                phoneOrTag = "Wise USD Transfer",
                colorHex = 0xFF6D4AFFL,
                repaymentHistory = "Oct 12: +$60.00 USD via Wise"
            ),
            DebtRecordEntity(
                id = 3,
                personName = "Rafiqul Islam",
                relationOrNote = "Short-term conference booking",
                totalAmount = 12000.0,
                settledAmount = 4000.0,
                currencyCode = "BDT",
                type = "BORROWED",
                dueDateMillis = now + 6L * DAY_MS,
                phoneOrTag = "City Bank NPSB",
                colorHex = 0xFFF26419L,
                repaymentHistory = "Oct 11: -৳4,000 via City Bank Amex"
            )
        ).map { it.copy(isDemo = true) }
    }

    fun initialGoals(): List<SavingsGoalEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            SavingsGoalEntity(
                id = 1,
                title = "6-Month Emergency Reserve",
                subtitle = "High-yield FDR & liquid cash buffer",
                targetAmount = 500000.0,
                savedAmount = 345000.0,
                currencyCode = "BDT",
                deadlineMillis = now + 120L * DAY_MS,
                colorHex = 0xFF059669L,
                iconKey = "SHIELD"
            ),
            SavingsGoalEntity(
                id = 2,
                title = "MacBook Pro M4 Max",
                subtitle = "Workstation upgrade fund (USD)",
                targetAmount = 2600.0,
                savedAmount = 1950.0,
                currencyCode = "USD",
                deadlineMillis = now + 45L * DAY_MS,
                colorHex = 0xFF6D4AFFL,
                iconKey = "LAPTOP"
            ),
            SavingsGoalEntity(
                id = 3,
                title = "Family Umrah & Makkah Fund",
                subtitle = "Dedicated Saudi Riyal travel pot",
                targetAmount = 12000.0,
                savedAmount = 8500.0,
                currencyCode = "SAR",
                deadlineMillis = now + 90L * DAY_MS,
                colorHex = 0xFF0084E9L,
                iconKey = "FLIGHT"
            )
        ).map { it.copy(isDemo = true) }
    }

    fun initialScheduledReminders(): List<ScheduledReminderEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ScheduledReminderEntity(
                id = 1,
                notificationId = 1001,
                sourceType = "BILL",
                sourceId = 1,
                title = "Grameenphone Postpaid Plan",
                subtitle = "Grameenphone Star • Monthly Bill",
                amount = 1600.0,
                currencyCode = "BDT",
                triggerTimeMillis = now + 2L * DAY_MS,
                frequency = "MONTHLY",
                leadTimeDays = 1,
                isEnabled = true,
                accentColorHex = 0xFFF26419L
            ),
            ScheduledReminderEntity(
                id = 2,
                notificationId = 1002,
                sourceType = "BILL",
                sourceId = 2,
                title = "Adobe Creative Cloud & Figma",
                subtitle = "Figma Inc. (USD Billing)",
                amount = 24.0,
                currencyCode = "USD",
                triggerTimeMillis = now + 5L * DAY_MS,
                frequency = "MONTHLY",
                leadTimeDays = 1,
                isEnabled = true,
                accentColorHex = 0xFF059669L
            ),
            ScheduledReminderEntity(
                id = 3,
                notificationId = 2001,
                sourceType = "DEBT",
                sourceId = 1,
                title = "Receivable Due: Tanvir Ahmed",
                subtitle = "Cox's Bazar flight & resort split",
                amount = 15000.0,
                currencyCode = "BDT",
                triggerTimeMillis = now + 4L * DAY_MS,
                frequency = "ONCE",
                leadTimeDays = 1,
                isEnabled = true,
                accentColorHex = 0xFF059669L
            ),
            ScheduledReminderEntity(
                id = 4,
                notificationId = 2002,
                sourceType = "DEBT",
                sourceId = 2,
                title = "Receivable Due: Nadia Karim",
                subtitle = "Shared studio monitor import",
                amount = 120.0,
                currencyCode = "USD",
                triggerTimeMillis = now + 10L * DAY_MS,
                frequency = "ONCE",
                leadTimeDays = 1,
                isEnabled = true,
                accentColorHex = 0xFF6D4AFFL
            ),
            ScheduledReminderEntity(
                id = 5,
                notificationId = 3001,
                sourceType = "BUDGET",
                sourceId = 1,
                title = "Groceries Budget Alert (80%)",
                subtitle = "Monthly spending threshold monitor",
                amount = 32000.0,
                currencyCode = "BDT",
                triggerTimeMillis = now + 7L * DAY_MS,
                frequency = "MONTHLY",
                leadTimeDays = 0,
                isEnabled = true,
                accentColorHex = 0xFF6D4AFFL
            )
        ).map { it.copy(isDemo = true) }
    }

    fun initialNotifications(): List<AppNotificationEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AppNotificationEntity(
                id = 1,
                title = "Groceries Wallet at 78% of Budget",
                message = "You've spent ৳24,855.30 BDT of your ৳32,000.00 BDT monthly Groceries budget.",
                categoryTag = "BUDGET",
                timestamp = now - 35L * 60_000L,
                isRead = false,
                accentColorHex = 0xFF6D4AFFL
            ),
            AppNotificationEntity(
                id = 2,
                title = "Upcoming Bill: Grameenphone Postpaid",
                message = "Your ৳1,600.00 BDT bill is scheduled for auto-pay in 2 days via bKash.",
                categoryTag = "BILL",
                timestamp = now - 5L * 3600_000L,
                isRead = false,
                accentColorHex = 0xFFF26419L
            ),
            AppNotificationEntity(
                id = 3,
                title = "USD Retainer Credited (+$240.00 USD)",
                message = "Your Wise USD account received $240.00 USD (≈ ৳28,920.00 BDT at 1 USD = ৳120.50).",
                categoryTag = "INSIGHT",
                timestamp = now - 1L * DAY_MS,
                isRead = true,
                accentColorHex = 0xFF059669L
            )
        ).map { it.copy(isDemo = true) }
    }
}
