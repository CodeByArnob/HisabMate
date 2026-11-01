package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index("isPrimary"),
        Index("currencyCode"),
        Index("isDemo")
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "BANK", "MFS", "CARD", "CASH"
    val balance: Double, // Stored in the account's native [currencyCode]
    val currencyCode: String = "BDT",
    val maskedNumber: String,
    val colorHex: Long,
    val iconKey: String,
    val isPrimary: Boolean = false,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "categories",
    indices = [
        Index("type"),
        Index("isDemo")
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "EXPENSE" or "INCOME"
    val monthlyBudget: Double, // Stored in [budgetCurrencyCode]
    val budgetCurrencyCode: String = "BDT",
    val budgetPeriod: String = "MONTHLY", // "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "CUSTOM"
    val alertThresholdPercent: Int = 80, // Alert at 50%-100%
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val colorHex: Long,
    val iconKey: String,
    val isQuickAction: Boolean = true,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("timestamp"),
        Index("accountId"),
        Index("categoryId"),
        Index("type"),
        Index("isFavorite"),
        Index("isDemo")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val merchant: String,
    val amount: Double, // Original recorded amount in [currencyCode]
    val currencyCode: String = "BDT",
    val exchangeRateSnapshotToBdt: Double = 1.0,
    val accountExchangeRateSnapshotToBdt: Double = 1.0,
    val toAccountExchangeRateSnapshotToBdt: Double = 1.0,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val categoryId: Int,
    val categoryName: String,
    val categoryColorHex: Long,
    val categoryIconKey: String,
    val accountId: Int,
    val accountLabel: String, // e.g., "Card •••• 1234", "Paid with bKash"
    val toAccountId: Int? = null,
    val timestamp: Long,
    val note: String = "",
    val isRecurring: Boolean = false,
    val receiptUri: String = "",
    val splitDetails: String = "",
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "recurring_payments",
    indices = [
        Index("nextDueDateMillis"),
        Index("categoryId"),
        Index("isDemo")
    ]
)
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val provider: String,
    val amount: Double,
    val currencyCode: String = "BDT",
    val transactionType: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val frequency: String, // "Daily", "Weekly", "Monthly", "Yearly"
    val nextDueDateMillis: Long,
    val categoryId: Int,
    val categoryName: String,
    val colorHex: Long,
    val iconKey: String,
    val autoPay: Boolean = true,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "debt_records",
    indices = [
        Index("dueDateMillis"),
        Index("type"),
        Index("isDemo")
    ]
)
data class DebtRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val relationOrNote: String,
    val totalAmount: Double,
    val settledAmount: Double,
    val currencyCode: String = "BDT",
    val type: String, // "LENT" (Receivable) or "BORROWED" (Payable)
    val dueDateMillis: Long,
    val phoneOrTag: String,
    val colorHex: Long,
    val repaymentHistory: String = "",
    val isDemo: Boolean = false
)

@Entity(
    tableName = "savings_goals",
    indices = [
        Index("deadlineMillis"),
        Index("isDemo")
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subtitle: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val currencyCode: String = "BDT",
    val deadlineMillis: Long,
    val colorHex: Long,
    val iconKey: String,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "app_notifications",
    indices = [
        Index("timestamp"),
        Index("isRead"),
        Index("isDemo")
    ]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val categoryTag: String, // "BUDGET", "BILL", "SECURITY", "INSIGHT", "DEBT", "GOAL", "REMINDER", "TRANSACTION"
    val timestamp: Long,
    val isRead: Boolean = false,
    val accentColorHex: Long,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "scheduled_reminders",
    indices = [
        Index("triggerTimeMillis"),
        Index("sourceType", "sourceId"),
        Index("notificationId"),
        Index("isDemo")
    ]
)
data class ScheduledReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val notificationId: Int, // Stable unique ID derived from sourceType + sourceId
    val sourceType: String, // "BILL", "BUDGET", "DEBT", "GOAL", "CUSTOM"
    val sourceId: Int,
    val title: String,
    val subtitle: String,
    val amount: Double, // Exact amount in [currencyCode]
    val currencyCode: String, // Original ISO 4217 currency code of the bill/budget/debt/goal
    val triggerTimeMillis: Long,
    val frequency: String = "ONCE", // "ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val leadTimeDays: Int = 1,
    val isEnabled: Boolean = true,
    val lastNotifiedMillis: Long = 0L,
    val accentColorHex: Long = 0xFF6D4AFFL,
    val isDemo: Boolean = false
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String,
    val rateToBdt: Double, // 1 unit of [currencyCode] = [rateToBdt] BDT
    val lastUpdatedMillis: Long,
    val sourceLabel: String = "Offline Verified Rate",
    val isCustomRate: Boolean = false
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val baseCurrencyCode: String = "BDT",
    val languageCode: String = "en",
    val regionCode: String = "BD",
    val hasCompletedOnboarding: Boolean = false,
    val useSouthAsianGrouping: Boolean = true,
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val isBalanceHidden: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    // Retained only for one-time legacy migration; never return this value to UI state.
    val appPinCode: String = "",
    val appPinHash: String = "",
    val appPinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val budgetAlertsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val debtRemindersEnabled: Boolean = true,
    val goalMilestonesEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val reminderLeadTimeDays: Int = 1,
    val hapticsEnabled: Boolean = true,
    val demoDataSeeded: Boolean = false,
    val demoDataDeleted: Boolean = false
)
