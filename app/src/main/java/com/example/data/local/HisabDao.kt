package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HisabDao {

    // Accounts
    @Query("SELECT * FROM accounts ORDER BY isPrimary DESC, id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isPrimary DESC, id ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Int): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: Int): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Int)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Int)

    // Recurring Payments
    @Query("SELECT * FROM recurring_payments ORDER BY nextDueDateMillis ASC")
    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments ORDER BY nextDueDateMillis ASC")
    suspend fun getAllRecurringPaymentsOnce(): List<RecurringPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringPayments(items: List<RecurringPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringPayment(item: RecurringPaymentEntity): Long

    @Update
    suspend fun updateRecurringPayment(item: RecurringPaymentEntity)

    @Query("DELETE FROM recurring_payments WHERE id = :id")
    suspend fun deleteRecurringById(id: Int)

    // Debts & Lending
    @Query("SELECT * FROM debt_records ORDER BY dueDateMillis ASC")
    fun getAllDebtRecords(): Flow<List<DebtRecordEntity>>

    @Query("SELECT * FROM debt_records ORDER BY dueDateMillis ASC")
    suspend fun getAllDebtRecordsOnce(): List<DebtRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecords(items: List<DebtRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecord(item: DebtRecordEntity): Long

    @Update
    suspend fun updateDebtRecord(item: DebtRecordEntity)

    @Query("DELETE FROM debt_records WHERE id = :id")
    suspend fun deleteDebtById(id: Int)

    // Savings Goals
    @Query("SELECT * FROM savings_goals ORDER BY deadlineMillis ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY deadlineMillis ASC")
    suspend fun getAllSavingsGoalsOnce(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(items: List<SavingsGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(item: SavingsGoalEntity): Long

    @Update
    suspend fun updateSavingsGoal(item: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    // Notifications
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotificationEntity>>

    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsOnce(): List<AppNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(items: List<AppNotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(item: AppNotificationEntity): Long

    @Query("""
        SELECT COUNT(*) FROM app_notifications
        WHERE categoryTag = 'BUDGET' AND title = :title
          AND timestamp >= :startMillis AND timestamp < :endMillis
    """)
    suspend fun countBudgetNotifications(title: String, startMillis: Long, endMillis: Long): Int

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)

    // Scheduled Local Reminders
    @Query("SELECT * FROM scheduled_reminders ORDER BY triggerTimeMillis ASC")
    fun getAllScheduledReminders(): Flow<List<ScheduledReminderEntity>>

    @Query("SELECT * FROM scheduled_reminders ORDER BY triggerTimeMillis ASC")
    suspend fun getAllScheduledRemindersOnce(): List<ScheduledReminderEntity>

    @Query("SELECT * FROM scheduled_reminders WHERE notificationId = :notificationId LIMIT 1")
    suspend fun getScheduledReminderByNotificationId(notificationId: Int): ScheduledReminderEntity?

    @Query("SELECT * FROM scheduled_reminders WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun getScheduledReminderBySource(sourceType: String, sourceId: Int): ScheduledReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledReminders(items: List<ScheduledReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledReminder(item: ScheduledReminderEntity): Long

    @Update
    suspend fun updateScheduledReminder(item: ScheduledReminderEntity)

    @Query("DELETE FROM scheduled_reminders WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun deleteScheduledReminderBySource(sourceType: String, sourceId: Int)

    @Query("DELETE FROM scheduled_reminders WHERE id = :id")
    suspend fun deleteScheduledReminderById(id: Int)

    @Query("DELETE FROM scheduled_reminders")
    suspend fun clearAllScheduledReminders()

    // Exchange Rates
    @Query("SELECT * FROM exchange_rates ORDER BY currencyCode ASC")
    fun getAllExchangeRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates")
    suspend fun getExchangeRatesList(): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(items: List<ExchangeRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(item: ExchangeRateEntity)

    // User Preferences
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesOnce(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(prefs: UserPreferencesEntity)

    // Full Reset Support
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM recurring_payments")
    suspend fun clearAllRecurring()

    @Query("DELETE FROM debt_records")
    suspend fun clearAllDebts()

    @Query("DELETE FROM savings_goals")
    suspend fun clearAllGoals()

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllExchangeRates()

    // Targeted Demo Data Management
    @Query("DELETE FROM transactions WHERE isDemo = 1")
    suspend fun deleteDemoTransactions()

    @Query("DELETE FROM accounts WHERE isDemo = 1")
    suspend fun deleteDemoAccounts()

    @Query("DELETE FROM categories WHERE isDemo = 1")
    suspend fun deleteDemoCategories()

    @Query("DELETE FROM recurring_payments WHERE isDemo = 1")
    suspend fun deleteDemoRecurring()

    @Query("DELETE FROM debt_records WHERE isDemo = 1")
    suspend fun deleteDemoDebts()

    @Query("DELETE FROM savings_goals WHERE isDemo = 1")
    suspend fun deleteDemoGoals()

    @Query("DELETE FROM scheduled_reminders WHERE isDemo = 1")
    suspend fun deleteDemoScheduledReminders()

    @Query("DELETE FROM app_notifications WHERE isDemo = 1")
    suspend fun deleteDemoNotifications()

    @Query("SELECT COUNT(*) FROM transactions WHERE isDemo = 1")
    suspend fun getDemoTransactionCount(): Int

    @Query("SELECT COUNT(*) FROM accounts WHERE isDemo = 1")
    suspend fun getDemoAccountCount(): Int

    @androidx.room.Transaction
    suspend fun deleteAllDemoData() {
        deleteDemoTransactions()
        deleteDemoAccounts()
        deleteDemoCategories()
        deleteDemoRecurring()
        deleteDemoDebts()
        deleteDemoGoals()
        deleteDemoScheduledReminders()
        deleteDemoNotifications()
    }
}

