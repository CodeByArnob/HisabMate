package com.example.data.repository

import com.example.data.local.AccountEntity
import com.example.data.local.AppNotificationEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DebtRecordEntity
import com.example.data.local.ExchangeRateEntity
import com.example.data.local.HisabDao
import com.example.data.local.HisabDatabase
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.ScheduledReminderEntity
import com.example.data.local.SeedDataGenerator
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesEntity
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabHaptics
import com.example.util.HisabNotificationManager
import com.example.util.PinHasher
import androidx.room.withTransaction
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class HisabRepository(private val database: HisabDatabase) {
    private val dao = database.hisabDao()

    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val recurringPayments: Flow<List<RecurringPaymentEntity>> = dao.getAllRecurringPayments()
    val debtRecords: Flow<List<DebtRecordEntity>> = dao.getAllDebtRecords()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val notifications: Flow<List<AppNotificationEntity>> = dao.getAllNotifications()
    val scheduledReminders: Flow<List<ScheduledReminderEntity>> = dao.getAllScheduledReminders()
    val exchangeRates: Flow<List<ExchangeRateEntity>> = dao.getAllExchangeRates()
    val userPreferences: Flow<UserPreferencesEntity?> = dao.getUserPreferences()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val existingRates = dao.getExchangeRatesList()
        if (existingRates.size < CurrencyCatalog.supportedCurrencies.size) {
            val existingCodes = existingRates.map { it.currencyCode.uppercase() }.toSet()
            val missing = SeedDataGenerator.initialExchangeRates().filter { it.currencyCode.uppercase() !in existingCodes }
            if (missing.isNotEmpty()) {
                dao.insertExchangeRates(missing)
            }
        }
        var currentPrefs = dao.getUserPreferencesOnce()
        if (currentPrefs != null && currentPrefs.appPinCode.isNotBlank() && currentPrefs.appPinHash.isBlank()) {
            val hashedPin = PinHasher.create(currentPrefs.appPinCode)
            currentPrefs = currentPrefs.copy(appPinCode = "", appPinHash = hashedPin.value, appPinSalt = hashedPin.salt)
            dao.insertUserPreferences(currentPrefs)
        }
        if (currentPrefs == null) {
            val defaultPrefs = UserPreferencesEntity(
                id = 1,
                baseCurrencyCode = "BDT",
                languageCode = "en",
                regionCode = "BD",
                hasCompletedOnboarding = false,
                useSouthAsianGrouping = true,
                themeMode = "LIGHT",
                isBalanceHidden = false,
                demoDataSeeded = true,
                demoDataDeleted = false
            )
            dao.insertUserPreferences(defaultPrefs)
            HisabHaptics.isHapticsEnabled = defaultPrefs.hapticsEnabled
            if (dao.getAccountCount() == 0) {
                seedAllDefaultData()
            }
        } else {
            HisabHaptics.isHapticsEnabled = currentPrefs.hapticsEnabled
            // CRITICAL IDEMPOTENCY GUARD:
            // If demo data was explicitly deleted or already seeded, NEVER re-seed on restart/launch!
            if (!currentPrefs.demoDataDeleted && !currentPrefs.demoDataSeeded) {
                if (dao.getAccountCount() == 0) {
                    seedAllDefaultData()
                    dao.insertUserPreferences(currentPrefs.copy(demoDataSeeded = true))
                }
            }
        }
    }

    suspend fun deleteDemoData() = withContext(Dispatchers.IO) {
        dao.deleteAllDemoData()

        // If no user accounts remain, guarantee at least one primary user account
        if (dao.getAccountCount() == 0) {
            dao.insertAccount(
                AccountEntity(
                    name = "Primary Cash Wallet",
                    type = "CASH",
                    balance = 0.0,
                    currencyCode = "BDT",
                    maskedNumber = "Cash",
                    colorHex = 0xFF059669L,
                    iconKey = "CASH",
                    isPrimary = true,
                    isDemo = false
                )
            )
        }

        // If no user categories remain, guarantee essential categories
        val cats = dao.getAllCategoriesOnce()
        if (cats.isEmpty()) {
            val now = System.currentTimeMillis()
            dao.insertCategory(
                CategoryEntity(
                    name = "General Expense",
                    type = "EXPENSE",
                    monthlyBudget = 15000.0,
                    budgetCurrencyCode = "BDT",
                    colorHex = 0xFF6D4AFFL,
                    iconKey = "GROCERIES",
                    startDateMillis = now,
                    endDateMillis = now + 30L * 24L * 60L * 60L * 1000L,
                    isQuickAction = true,
                    isDemo = false
                )
            )
        }

        val currentPrefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        dao.insertUserPreferences(
            currentPrefs.copy(
                demoDataDeleted = true,
                demoDataSeeded = false
            )
        )
    }

    suspend fun resetToDefaultSampleData() = withContext(Dispatchers.IO) {
        dao.clearAllTransactions()
        dao.clearAllAccounts()
        dao.clearAllCategories()
        dao.clearAllRecurring()
        dao.clearAllDebts()
        dao.clearAllGoals()
        dao.clearAllNotifications()
        dao.clearAllScheduledReminders()
        dao.clearAllExchangeRates()
        dao.insertExchangeRates(SeedDataGenerator.initialExchangeRates())
        seedAllDefaultData()

        val currentPrefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        dao.insertUserPreferences(
            currentPrefs.copy(
                demoDataDeleted = false,
                demoDataSeeded = true
            )
        )
    }

    private suspend fun seedAllDefaultData() {
        dao.insertAccounts(SeedDataGenerator.initialAccounts())
        dao.insertCategories(SeedDataGenerator.initialCategories())
        val rates = getRateMap()
        val accountRates = dao.getAllAccountsOnce().associate { it.id to rateFor(it.currencyCode, rates) }
        val seededTransactions = SeedDataGenerator.initialTransactions().map { tx ->
            tx.copy(
                accountExchangeRateSnapshotToBdt = accountRates[tx.accountId] ?: 1.0,
                toAccountExchangeRateSnapshotToBdt = tx.toAccountId?.let { accountRates[it] } ?: 1.0
            )
        }
        dao.insertTransactions(seededTransactions)
        dao.insertRecurringPayments(SeedDataGenerator.initialRecurringPayments())
        dao.insertDebtRecords(SeedDataGenerator.initialDebts())
        dao.insertSavingsGoals(SeedDataGenerator.initialGoals())
        dao.insertScheduledReminders(SeedDataGenerator.initialScheduledReminders())
        dao.insertNotifications(SeedDataGenerator.initialNotifications())
    }

    private suspend fun getRateMap(): Map<String, Double> {
        val list = dao.getExchangeRatesList()
        if (list.isEmpty()) {
            return CurrencyCatalog.supportedCurrencies.associate { it.code to it.defaultRateToBdt }
        }
        return list.associate { it.currencyCode to it.rateToBdt }
    }

    suspend fun updateUserPreferences(transform: (UserPreferencesEntity) -> UserPreferencesEntity) {
        val current = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val updated = transform(current)
        HisabHaptics.isHapticsEnabled = updated.hapticsEnabled
        dao.insertUserPreferences(updated)
    }

    suspend fun configureAppLock(enabled: Boolean, pinCode: String, biometric: Boolean) {
        val current = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        if (enabled) {
            val hash = PinHasher.create(pinCode)
            dao.insertUserPreferences(current.copy(
                isAppLockEnabled = true, appPinCode = "", appPinHash = hash.value,
                appPinSalt = hash.salt, biometricEnabled = false
            ))
        } else {
            dao.insertUserPreferences(current.copy(
                isAppLockEnabled = false, appPinCode = "", appPinHash = "",
                appPinSalt = "", biometricEnabled = false
            ))
        }
    }

    suspend fun verifyAppPin(pinCode: String): Boolean = withContext(Dispatchers.IO) {
        if (pinCode.length != 4 || !pinCode.all(Char::isDigit)) return@withContext false
        val prefs = dao.getUserPreferencesOnce() ?: return@withContext false
        if (prefs.appPinHash.isNotBlank() && prefs.appPinSalt.isNotBlank()) {
            return@withContext PinHasher.verify(pinCode, prefs.appPinSalt, prefs.appPinHash)
        }
        if (prefs.appPinCode.isNotBlank() && prefs.appPinCode == pinCode) {
            val hash = PinHasher.create(pinCode)
            dao.insertUserPreferences(prefs.copy(appPinCode = "", appPinHash = hash.value, appPinSalt = hash.salt))
            return@withContext true
        }
        false
    }

    suspend fun updateExchangeRate(
        currencyCode: String,
        newRateToBdt: Double,
        sourceLabel: String = "Manual Custom Rate",
        isCustom: Boolean = true
    ) {
        if (currencyCode.equals("BDT", ignoreCase = true)) return
        dao.insertExchangeRate(
            ExchangeRateEntity(
                currencyCode = currencyCode.uppercase(),
                rateToBdt = newRateToBdt.coerceAtLeast(0.0001),
                lastUpdatedMillis = System.currentTimeMillis(),
                sourceLabel = sourceLabel,
                isCustomRate = isCustom
            )
        )
    }

    suspend fun resetExchangeRatesToDefaults() {
        val now = System.currentTimeMillis()
        val defaults = CurrencyCatalog.supportedCurrencies.map { meta ->
            ExchangeRateEntity(
                currencyCode = meta.code,
                rateToBdt = meta.defaultRateToBdt,
                lastUpdatedMillis = now,
                sourceLabel = "Offline Verified Rate",
                isCustomRate = false
            )
        }
        dao.insertExchangeRates(defaults)
    }

    suspend fun addTransaction(
        title: String, merchant: String, amount: Double, currencyCode: String, type: String,
        category: CategoryEntity, account: AccountEntity, toAccount: AccountEntity? = null,
        timestamp: Long = System.currentTimeMillis(), note: String = "", isRecurring: Boolean = false,
        receiptUri: String = "", splitDetails: String = "", isFavorite: Boolean = false
    ) {
        validateTransaction(amount, currencyCode, type, account.id, toAccount?.id)
        val source = dao.getAccountById(account.id) ?: error("Selected source account no longer exists")
        val target = if (type == "TRANSFER") dao.getAccountById(toAccount!!.id)
            ?: error("Selected destination account no longer exists") else null
        val rates = getRateMap()
        val label = when {
            target != null -> "${source.name} → ${target.name}"
            source.type == "MFS" -> "Paid with ${source.name.substringBefore(" ")}"
            else -> source.maskedNumber
        }
        val tx = TransactionEntity(
            title = title.trim(), merchant = merchant.trim().ifEmpty { category.name },
            amount = amount, currencyCode = currencyCode.uppercase(),
            exchangeRateSnapshotToBdt = rateFor(currencyCode, rates),
            accountExchangeRateSnapshotToBdt = rateFor(source.currencyCode, rates),
            toAccountExchangeRateSnapshotToBdt = target?.let { rateFor(it.currencyCode, rates) } ?: 1.0,
            type = type, categoryId = category.id,
            categoryName = if (type == "TRANSFER") "Transfer" else category.name,
            categoryColorHex = if (type == "TRANSFER") 0xFF1D9BF0L else category.colorHex,
            categoryIconKey = if (type == "TRANSFER") "TRANSFER" else category.iconKey,
            accountId = source.id, accountLabel = label, toAccountId = target?.id,
            timestamp = timestamp, note = note.trim(), isRecurring = isRecurring,
            receiptUri = receiptUri.trim(), splitDetails = splitDetails.trim(), isFavorite = isFavorite
        )
        database.withTransaction {
            dao.insertTransaction(tx)
            applyAccountDeltaForTransaction(tx, reverse = false)
            if (type == "EXPENSE" && category.monthlyBudget > 0.0) checkAndRecordBudgetThresholdAlert(category, rates, timestamp)
        }
    }

    private fun validateTransaction(amount: Double, currencyCode: String, type: String, sourceId: Int, targetId: Int?) {
        require(amount.isFinite() && amount > 0.0) { "Transaction amount must be greater than zero" }
        require(CurrencyCatalog.isSupported(currencyCode)) { "Unsupported currency: $currencyCode" }
        require(type in setOf("EXPENSE", "INCOME", "TRANSFER")) { "Invalid transaction type" }
        if (type == "TRANSFER") {
            require(targetId != null && targetId != sourceId) { "Select a different destination account" }
        } else require(targetId == null) { "Only transfers can have a destination account" }
    }

    private fun rateFor(currency: String, rates: Map<String, Double>): Double {
        require(CurrencyCatalog.isSupported(currency)) { "Unsupported currency: $currency" }
        val value = rates[currency.uppercase()] ?: CurrencyCatalog.getMetadata(currency).defaultRateToBdt
        require(value.isFinite() && value > 0.0) { "Invalid exchange rate for $currency" }
        return value
    }

    private fun budgetBounds(category: CategoryEntity, timestamp: Long): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (category.budgetPeriod.equals("CUSTOM", true)) {
            start.timeInMillis = category.startDateMillis.takeIf { it > 0L } ?: timestamp
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            val end = Calendar.getInstance().apply {
                timeInMillis = category.endDateMillis.takeIf { it >= start.timeInMillis } ?: timestamp
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            return start.timeInMillis to end.timeInMillis
        }
        when (category.budgetPeriod.uppercase()) {
            "DAILY" -> Unit
            "WEEKLY" -> start.add(Calendar.DAY_OF_MONTH, -((start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7))
            "YEARLY" -> start.set(Calendar.DAY_OF_YEAR, 1)
            else -> start.set(Calendar.DAY_OF_MONTH, 1)
        }
        val end = (start.clone() as Calendar)
        when (category.budgetPeriod.uppercase()) {
            "DAILY" -> end.add(Calendar.DAY_OF_MONTH, 1)
            "WEEKLY" -> end.add(Calendar.DAY_OF_MONTH, 7)
            "YEARLY" -> end.add(Calendar.YEAR, 1)
            else -> end.add(Calendar.MONTH, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    private suspend fun checkAndRecordBudgetThresholdAlert(category: CategoryEntity, rates: Map<String, Double>, timestamp: Long) {
        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        if (!prefs.budgetAlertsEnabled || category.monthlyBudget <= 0.0) return
        val (start, end) = budgetBounds(category, timestamp)
        val currency = category.budgetCurrencyCode.uppercase()
        val spent = dao.getAllTransactionsOnce().asSequence().filter {
            it.type == "EXPENSE" && it.categoryId == category.id && it.timestamp >= start && it.timestamp < end
        }.sumOf { CurrencyFormatter.convert(it.amount, it.currencyCode, currency, rates) }
        val threshold = category.alertThresholdPercent.coerceIn(50, 100)
        val percent = (spent / category.monthlyBudget * 100.0).toInt()
        if (percent < threshold) return
        val title = "${category.name} Budget Alert (${threshold}%)"
        if (dao.countBudgetNotifications(title, start, end) > 0) return
        val spentText = CurrencyFormatter.formatCurrency(spent, currency, prefs.useSouthAsianGrouping, includeCodeSuffix = true)
        val limitText = CurrencyFormatter.formatCurrency(category.monthlyBudget, currency, prefs.useSouthAsianGrouping, includeCodeSuffix = true)
        dao.insertNotification(AppNotificationEntity(
            title = title,
            message = "Spent $spentText of your $limitText ${category.budgetPeriod.lowercase()} limit ($percent%).",
            categoryTag = "BUDGET", timestamp = System.currentTimeMillis(), isRead = false, accentColorHex = category.colorHex
        ))
    }

    suspend fun editTransaction(
        oldTx: TransactionEntity, title: String, merchant: String, amount: Double, currencyCode: String,
        type: String, category: CategoryEntity, account: AccountEntity, toAccount: AccountEntity? = null,
        note: String = "", isRecurring: Boolean = false, receiptUri: String = "", splitDetails: String = "",
        isFavorite: Boolean = false
    ) {
        validateTransaction(amount, currencyCode, type, account.id, toAccount?.id)
        val source = dao.getAccountById(account.id) ?: error("Selected source account no longer exists")
        val target = if (type == "TRANSFER") dao.getAccountById(toAccount!!.id)
            ?: error("Selected destination account no longer exists") else null
        val rates = getRateMap()
        val updated = oldTx.copy(
            title = title.trim(), merchant = merchant.trim().ifEmpty { category.name }, amount = amount,
            currencyCode = currencyCode.uppercase(), exchangeRateSnapshotToBdt = rateFor(currencyCode, rates),
            accountExchangeRateSnapshotToBdt = rateFor(source.currencyCode, rates),
            toAccountExchangeRateSnapshotToBdt = target?.let { rateFor(it.currencyCode, rates) } ?: 1.0,
            type = type, categoryId = category.id,
            categoryName = if (type == "TRANSFER") "Transfer" else category.name,
            categoryColorHex = if (type == "TRANSFER") 0xFF1D9BF0L else category.colorHex,
            categoryIconKey = if (type == "TRANSFER") "TRANSFER" else category.iconKey,
            accountId = source.id,
            accountLabel = when {
                target != null -> "${source.name} → ${target.name}"
                source.type == "MFS" -> "Paid with ${source.name.substringBefore(" ")}"
                else -> source.maskedNumber
            },
            toAccountId = target?.id, note = note.trim(), isRecurring = isRecurring,
            receiptUri = receiptUri.trim(), splitDetails = splitDetails.trim(), isFavorite = isFavorite
        )
        database.withTransaction {
            applyAccountDeltaForTransaction(oldTx, reverse = true)
            dao.updateTransaction(updated)
            applyAccountDeltaForTransaction(updated, reverse = false)
            if (type == "EXPENSE" && category.monthlyBudget > 0.0) checkAndRecordBudgetThresholdAlert(category, rates, updated.timestamp)
        }
    }

    suspend fun toggleTransactionFavorite(tx: TransactionEntity): TransactionEntity {
        val updated = tx.copy(isFavorite = !tx.isFavorite)
        dao.updateTransaction(updated)
        return updated
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            applyAccountDeltaForTransaction(transaction, reverse = true)
            dao.deleteTransactionById(transaction.id)
        }
    }

    private suspend fun applyAccountDeltaForTransaction(tx: TransactionEntity, reverse: Boolean) {
        val source = dao.getAccountById(tx.accountId) ?: return
        val sourceDelta = if (tx.currencyCode.equals(source.currencyCode, true)) tx.amount else
            CurrencyFormatter.convertWithSnapshots(tx.amount, tx.exchangeRateSnapshotToBdt, tx.accountExchangeRateSnapshotToBdt, source.currencyCode)
        val sourceBalance = when (tx.type) {
            "EXPENSE", "TRANSFER" -> if (reverse) CurrencyFormatter.addMoney(source.balance, sourceDelta, source.currencyCode)
                else CurrencyFormatter.subtractMoney(source.balance, sourceDelta, source.currencyCode)
            "INCOME" -> if (reverse) CurrencyFormatter.subtractMoney(source.balance, sourceDelta, source.currencyCode)
                else CurrencyFormatter.addMoney(source.balance, sourceDelta, source.currencyCode)
            else -> source.balance
        }
        dao.updateAccount(source.copy(balance = sourceBalance))
        if (tx.type == "TRANSFER") {
            val targetId = tx.toAccountId ?: return
            val target = dao.getAccountById(targetId) ?: return
            val delta = if (tx.currencyCode.equals(target.currencyCode, true)) tx.amount else
                CurrencyFormatter.convertWithSnapshots(tx.amount, tx.exchangeRateSnapshotToBdt, tx.toAccountExchangeRateSnapshotToBdt, target.currencyCode)
            val balance = if (reverse) CurrencyFormatter.subtractMoney(target.balance, delta, target.currencyCode)
                else CurrencyFormatter.addMoney(target.balance, delta, target.currencyCode)
            dao.updateAccount(target.copy(balance = balance))
        }
    }

    suspend fun updateCategoryTransactionCategory(transaction: TransactionEntity, newCategory: CategoryEntity) {
        dao.updateTransaction(
            transaction.copy(
                categoryId = newCategory.id,
                categoryName = newCategory.name,
                categoryColorHex = newCategory.colorHex,
                categoryIconKey = newCategory.iconKey
            )
        )
    }

    suspend fun updateCategoryBudget(
        category: CategoryEntity,
        newBudget: Double,
        budgetCurrencyCode: String,
        budgetPeriod: String = category.budgetPeriod,
        alertThresholdPercent: Int = category.alertThresholdPercent,
        startDateMillis: Long = category.startDateMillis,
        endDateMillis: Long = category.endDateMillis
    ): ScheduledReminderEntity {
        val updatedCat = category.copy(
            monthlyBudget = newBudget,
            budgetCurrencyCode = budgetCurrencyCode.uppercase(),
            budgetPeriod = budgetPeriod.uppercase(),
            alertThresholdPercent = alertThresholdPercent.coerceIn(50, 100),
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis
        )
        dao.updateCategory(updatedCat)

        val notifId = HisabNotificationManager.computeStableNotificationId("BUDGET", category.id)
        val existing = dao.getScheduledReminderBySource("BUDGET", category.id)
        val nextTrigger = if (endDateMillis > System.currentTimeMillis()) {
            endDateMillis
        } else {
            System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
        }
        val reminder = ScheduledReminderEntity(
            id = existing?.id ?: 0,
            notificationId = notifId,
            sourceType = "BUDGET",
            sourceId = category.id,
            title = "${category.name} Budget (${alertThresholdPercent}% Alert)",
            subtitle = "${budgetPeriod.uppercase()} spending cap in ${budgetCurrencyCode.uppercase()}",
            amount = newBudget,
            currencyCode = budgetCurrencyCode.uppercase(),
            triggerTimeMillis = nextTrigger,
            frequency = budgetPeriod.uppercase(),
            leadTimeDays = 0,
            isEnabled = newBudget > 0,
            accentColorHex = category.colorHex
        )
        dao.insertScheduledReminder(reminder)
        return reminder
    }

    suspend fun addCategory(
        name: String,
        type: String,
        monthlyBudget: Double,
        budgetCurrencyCode: String,
        colorHex: Long,
        iconKey: String,
        budgetPeriod: String = "MONTHLY",
        alertThresholdPercent: Int = 80
    ) {
        val now = System.currentTimeMillis()
        val id = dao.insertCategory(
            CategoryEntity(
                name = name.trim(),
                type = type,
                monthlyBudget = monthlyBudget,
                budgetCurrencyCode = budgetCurrencyCode.uppercase(),
                budgetPeriod = budgetPeriod.uppercase(),
                alertThresholdPercent = alertThresholdPercent.coerceIn(50, 100),
                startDateMillis = now,
                endDateMillis = now + 30L * 24L * 60L * 60L * 1000L,
                colorHex = colorHex,
                iconKey = iconKey,
                isQuickAction = true
            )
        ).toInt()

        if (type == "EXPENSE" && monthlyBudget > 0) {
            dao.insertScheduledReminder(
                ScheduledReminderEntity(
                    notificationId = HisabNotificationManager.computeStableNotificationId("BUDGET", id),
                    sourceType = "BUDGET",
                    sourceId = id,
                    title = "${name.trim()} Budget (${alertThresholdPercent}% Alert)",
                    subtitle = "${budgetPeriod.uppercase()} spending cap in ${budgetCurrencyCode.uppercase()}",
                    amount = monthlyBudget,
                    currencyCode = budgetCurrencyCode.uppercase(),
                    triggerTimeMillis = now + 7L * 24L * 60L * 60L * 1000L,
                    frequency = budgetPeriod.uppercase(),
                    leadTimeDays = 0,
                    isEnabled = true,
                    accentColorHex = colorHex
                )
            )
        }
    }

    suspend fun addAccount(
        name: String,
        type: String,
        balance: Double,
        currencyCode: String,
        maskedNumber: String,
        colorHex: Long
    ) {
        dao.insertAccount(
            AccountEntity(
                name = name.trim(),
                type = type,
                balance = balance,
                currencyCode = currencyCode.uppercase(),
                maskedNumber = maskedNumber.trim(),
                colorHex = colorHex,
                iconKey = type,
                isPrimary = false
            )
        )
    }

    suspend fun deleteAccount(accountId: Int) {
        dao.deleteAccountById(accountId)
    }

    suspend fun payRecurringBillNow(
        bill: RecurringPaymentEntity,
        primaryAccount: AccountEntity,
        category: CategoryEntity?
    ): ScheduledReminderEntity {
        val cat = category ?: CategoryEntity(
            id = bill.categoryId,
            name = bill.categoryName,
            type = bill.transactionType,
            monthlyBudget = 20000.0,
            budgetCurrencyCode = bill.currencyCode,
            colorHex = bill.colorHex,
            iconKey = bill.iconKey
        )
        addTransaction(
            title = bill.title,
            merchant = bill.provider,
            amount = bill.amount,
            currencyCode = bill.currencyCode,
            type = bill.transactionType,
            category = cat,
            account = primaryAccount,
            note = "Recurring ${bill.frequency} ${bill.transactionType.lowercase()}",
            isRecurring = true
        )
        val advanceDays = when (bill.frequency.uppercase()) {
            "DAILY" -> 1L
            "WEEKLY" -> 7L
            "YEARLY" -> 365L
            else -> 30L
        }
        val nextDue = bill.nextDueDateMillis + advanceDays * 24L * 60L * 60L * 1000L
        dao.updateRecurringPayment(
            bill.copy(nextDueDateMillis = nextDue)
        )

        val notifId = HisabNotificationManager.computeStableNotificationId("BILL", bill.id)
        val existing = dao.getScheduledReminderBySource("BILL", bill.id)
        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val updatedReminder = ScheduledReminderEntity(
            id = existing?.id ?: 0,
            notificationId = notifId,
            sourceType = "BILL",
            sourceId = bill.id,
            title = bill.title,
            subtitle = "${bill.provider} • ${bill.frequency} ${bill.transactionType.lowercase()}",
            amount = bill.amount,
            currencyCode = bill.currencyCode.uppercase(),
            triggerTimeMillis = nextDue,
            frequency = bill.frequency.uppercase(),
            leadTimeDays = prefs.reminderLeadTimeDays,
            isEnabled = true,
            accentColorHex = bill.colorHex
        )
        dao.insertScheduledReminder(updatedReminder)
        return updatedReminder
    }

    suspend fun addRecurringPayment(
        title: String,
        provider: String,
        amount: Double,
        currencyCode: String,
        transactionType: String = "EXPENSE",
        frequency: String,
        daysUntilDue: Int,
        category: CategoryEntity,
        autoPay: Boolean
    ): ScheduledReminderEntity {
        val dueMillis = System.currentTimeMillis() + daysUntilDue * 24L * 60L * 60L * 1000L
        val id = dao.insertRecurringPayment(
            RecurringPaymentEntity(
                title = title.trim(),
                provider = provider.trim(),
                amount = amount,
                currencyCode = currencyCode.uppercase(),
                transactionType = transactionType,
                frequency = frequency,
                nextDueDateMillis = dueMillis,
                categoryId = category.id,
                categoryName = category.name,
                colorHex = category.colorHex,
                iconKey = category.iconKey,
                autoPay = autoPay
            )
        ).toInt()

        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val reminder = ScheduledReminderEntity(
            notificationId = HisabNotificationManager.computeStableNotificationId("BILL", id),
            sourceType = "BILL",
            sourceId = id,
            title = title.trim(),
            subtitle = "${provider.trim()} • $frequency ${transactionType.lowercase()}",
            amount = amount,
            currencyCode = currencyCode.uppercase(),
            triggerTimeMillis = dueMillis,
            frequency = frequency.uppercase(),
            leadTimeDays = prefs.reminderLeadTimeDays,
            isEnabled = true,
            accentColorHex = category.colorHex
        )
        dao.insertScheduledReminder(reminder)
        return reminder
    }

    suspend fun deleteRecurringPayment(id: Int): Int {
        dao.deleteRecurringById(id)
        dao.deleteScheduledReminderBySource("BILL", id)
        return HisabNotificationManager.computeStableNotificationId("BILL", id)
    }

    suspend fun settleDebtAmount(
        record: DebtRecordEntity,
        paymentAmountInRecordCurrency: Double,
        primaryAccount: AccountEntity?
    ): ScheduledReminderEntity? {
        val ratesMap = getRateMap()
        val newSettled = (record.settledAmount + paymentAmountInRecordCurrency).coerceAtMost(record.totalAmount)
        val remaining = (record.totalAmount - newSettled).coerceAtLeast(0.0)
        val dateStr = CurrencyFormatter.formatDateShort(System.currentTimeMillis())
        val formattedPay = CurrencyFormatter.formatCurrency(
            amount = paymentAmountInRecordCurrency,
            currencyCode = record.currencyCode,
            useSouthAsianGrouping = true
        )
        val newEntry = "$dateStr: ${if (record.type == "LENT") "+" else "-"}$formattedPay via ${primaryAccount?.name ?: "Wallet"}"
        val updatedHistory = if (record.repaymentHistory.isBlank()) newEntry else "${record.repaymentHistory}\n$newEntry"

        dao.updateDebtRecord(
            record.copy(
                settledAmount = newSettled,
                repaymentHistory = updatedHistory
            )
        )
        if (primaryAccount != null) {
            val deltaInAccountCurrency = CurrencyFormatter.convert(
                amount = paymentAmountInRecordCurrency,
                fromCurrency = record.currencyCode,
                toCurrency = primaryAccount.currencyCode,
                ratesToBdt = ratesMap
            )
            val updatedBalance = if (record.type == "LENT") {
                primaryAccount.balance + deltaInAccountCurrency
            } else {
                (primaryAccount.balance - deltaInAccountCurrency).coerceAtLeast(0.0)
            }
            dao.updateAccount(primaryAccount.copy(balance = updatedBalance))
        }

        val notifId = HisabNotificationManager.computeStableNotificationId("DEBT", record.id)
        return if (remaining <= 0.01) {
            dao.deleteScheduledReminderBySource("DEBT", record.id)
            null
        } else {
            val existing = dao.getScheduledReminderBySource("DEBT", record.id)
            val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
            val updatedReminder = ScheduledReminderEntity(
                id = existing?.id ?: 0,
                notificationId = notifId,
                sourceType = "DEBT",
                sourceId = record.id,
                title = "${if (record.type == "LENT") "Receivable Due" else "Payable Due"}: ${record.personName}",
                subtitle = record.relationOrNote,
                amount = remaining,
                currencyCode = record.currencyCode.uppercase(),
                triggerTimeMillis = record.dueDateMillis,
                frequency = "ONCE",
                leadTimeDays = prefs.reminderLeadTimeDays,
                isEnabled = true,
                accentColorHex = record.colorHex
            )
            dao.insertScheduledReminder(updatedReminder)
            updatedReminder
        }
    }

    suspend fun addDebtRecord(
        personName: String,
        relationOrNote: String,
        totalAmount: Double,
        currencyCode: String,
        type: String,
        daysUntilDue: Int,
        phoneOrTag: String
    ): ScheduledReminderEntity {
        val dueMillis = System.currentTimeMillis() + daysUntilDue * 24L * 60L * 60L * 1000L
        val color = if (type == "LENT") 0xFF0EA568L else 0xFFFF6E1AL
        val id = dao.insertDebtRecord(
            DebtRecordEntity(
                personName = personName.trim(),
                relationOrNote = relationOrNote.trim(),
                totalAmount = totalAmount,
                settledAmount = 0.0,
                currencyCode = currencyCode.uppercase(),
                type = type,
                dueDateMillis = dueMillis,
                phoneOrTag = phoneOrTag.trim(),
                colorHex = color,
                repaymentHistory = ""
            )
        ).toInt()

        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val reminder = ScheduledReminderEntity(
            notificationId = HisabNotificationManager.computeStableNotificationId("DEBT", id),
            sourceType = "DEBT",
            sourceId = id,
            title = "${if (type == "LENT") "Receivable Due" else "Payable Due"}: ${personName.trim()}",
            subtitle = relationOrNote.trim(),
            amount = totalAmount,
            currencyCode = currencyCode.uppercase(),
            triggerTimeMillis = dueMillis,
            frequency = "ONCE",
            leadTimeDays = prefs.reminderLeadTimeDays,
            isEnabled = true,
            accentColorHex = color
        )
        dao.insertScheduledReminder(reminder)
        return reminder
    }

    suspend fun deleteDebtRecord(id: Int): Int {
        dao.deleteDebtById(id)
        dao.deleteScheduledReminderBySource("DEBT", id)
        return HisabNotificationManager.computeStableNotificationId("DEBT", id)
    }

    suspend fun contributeToGoal(
        goal: SavingsGoalEntity,
        amountInGoalCurrency: Double,
        fromAccount: AccountEntity?
    ) {
        val ratesMap = getRateMap()
        val oldPct = if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100).toInt() else 0
        val newSaved = (goal.savedAmount + amountInGoalCurrency).coerceAtMost(goal.targetAmount * 1.5)
        val newPct = if (goal.targetAmount > 0) ((newSaved / goal.targetAmount) * 100).toInt() else 0

        dao.updateSavingsGoal(goal.copy(savedAmount = newSaved))
        if (fromAccount != null) {
            val deltaInAccCurrency = CurrencyFormatter.convert(
                amount = amountInGoalCurrency,
                fromCurrency = goal.currencyCode,
                toCurrency = fromAccount.currencyCode,
                ratesToBdt = ratesMap
            )
            dao.updateAccount(
                fromAccount.copy(balance = (fromAccount.balance - deltaInAccCurrency).coerceAtLeast(0.0))
            )
        }

        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        if (prefs.goalMilestonesEnabled && ((oldPct < 50 && newPct >= 50) || (oldPct < 80 && newPct >= 80) || (oldPct < 100 && newPct >= 100))) {
            val savedFormatted = CurrencyFormatter.formatCurrency(
                amount = newSaved,
                currencyCode = goal.currencyCode,
                useSouthAsianGrouping = prefs.useSouthAsianGrouping,
                includeCodeSuffix = true
            )
            val targetFormatted = CurrencyFormatter.formatCurrency(
                amount = goal.targetAmount,
                currencyCode = goal.currencyCode,
                useSouthAsianGrouping = prefs.useSouthAsianGrouping,
                includeCodeSuffix = true
            )
            dao.insertNotification(
                AppNotificationEntity(
                    title = if (newPct >= 100) "Goal Achieved: ${goal.title}!" else "${goal.title} Reached $newPct%!",
                    message = "You have saved $savedFormatted toward your $targetFormatted goal.",
                    categoryTag = "GOAL",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    accentColorHex = goal.colorHex
                )
            )
        }
    }

    suspend fun addSavingsGoal(
        title: String,
        subtitle: String,
        targetAmount: Double,
        initialSaved: Double,
        currencyCode: String,
        daysUntilDeadline: Int,
        colorHex: Long,
        iconKey: String
    ): ScheduledReminderEntity {
        val deadline = System.currentTimeMillis() + daysUntilDeadline * 24L * 60L * 60L * 1000L
        val id = dao.insertSavingsGoal(
            SavingsGoalEntity(
                title = title.trim(),
                subtitle = subtitle.trim(),
                targetAmount = targetAmount,
                savedAmount = initialSaved,
                currencyCode = currencyCode.uppercase(),
                deadlineMillis = deadline,
                colorHex = colorHex,
                iconKey = iconKey
            )
        ).toInt()

        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val reminder = ScheduledReminderEntity(
            notificationId = HisabNotificationManager.computeStableNotificationId("GOAL", id),
            sourceType = "GOAL",
            sourceId = id,
            title = title.trim(),
            subtitle = subtitle.trim(),
            amount = targetAmount,
            currencyCode = currencyCode.uppercase(),
            triggerTimeMillis = deadline,
            frequency = "MONTHLY",
            leadTimeDays = prefs.reminderLeadTimeDays,
            isEnabled = true,
            accentColorHex = colorHex
        )
        dao.insertScheduledReminder(reminder)
        return reminder
    }

    suspend fun deleteSavingsGoal(id: Int): Int {
        dao.deleteGoalById(id)
        dao.deleteScheduledReminderBySource("GOAL", id)
        return HisabNotificationManager.computeStableNotificationId("GOAL", id)
    }

    suspend fun addCustomScheduledReminder(
        title: String,
        subtitle: String,
        amount: Double,
        currencyCode: String,
        daysUntilTrigger: Int,
        frequency: String = "ONCE"
    ): ScheduledReminderEntity {
        val triggerTime = System.currentTimeMillis() + daysUntilTrigger.coerceAtLeast(0) * 24L * 60L * 60L * 1000L
        val tempSourceId = (System.currentTimeMillis() % 9000).toInt() + 100
        val notifId = HisabNotificationManager.computeStableNotificationId("CUSTOM", tempSourceId)
        val reminder = ScheduledReminderEntity(
            notificationId = notifId,
            sourceType = "CUSTOM",
            sourceId = tempSourceId,
            title = title.trim(),
            subtitle = subtitle.trim().ifEmpty { "Custom Financial Reminder" },
            amount = amount,
            currencyCode = currencyCode.uppercase(),
            triggerTimeMillis = triggerTime,
            frequency = frequency.uppercase(),
            leadTimeDays = 0,
            isEnabled = true,
            accentColorHex = 0xFF6D4AFFL
        )
        val insertedId = dao.insertScheduledReminder(reminder).toInt()
        return reminder.copy(id = insertedId)
    }

    suspend fun toggleScheduledReminder(reminder: ScheduledReminderEntity): ScheduledReminderEntity {
        val updated = reminder.copy(isEnabled = !reminder.isEnabled)
        dao.updateScheduledReminder(updated)
        return updated
    }

    suspend fun deleteScheduledReminder(reminder: ScheduledReminderEntity) {
        dao.deleteScheduledReminderById(reminder.id)
    }

    suspend fun insertAppNotification(notification: AppNotificationEntity) {
        dao.insertNotification(notification)
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsRead()
    }

    suspend fun dismissNotification(id: Int) {
        dao.deleteNotificationById(id)
    }

    // =========================================================================
    // Backup, Restore, CSV Export & CSV Import
    // =========================================================================

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 6)
        root.put("exportedAt", System.currentTimeMillis())

        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val prefsObj = JSONObject().apply {
            put("baseCurrencyCode", prefs.baseCurrencyCode)
            put("useSouthAsianGrouping", prefs.useSouthAsianGrouping)
            put("themeMode", prefs.themeMode)
        }
        root.put("preferences", prefsObj)

        val accountsArr = JSONArray()
        dao.getAllAccountsOnce().forEach { acc ->
            accountsArr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("type", acc.type)
                    put("balance", acc.balance)
                    put("currencyCode", acc.currencyCode)
                    put("maskedNumber", acc.maskedNumber)
                    put("colorHex", acc.colorHex)
                    put("iconKey", acc.iconKey)
                    put("isPrimary", acc.isPrimary)
                    put("isDemo", acc.isDemo)
                }
            )
        }
        root.put("accounts", accountsArr)

        val txsArr = JSONArray()
        dao.getAllTransactionsOnce().forEach { tx ->
            txsArr.put(
                JSONObject().apply {
                    put("title", tx.title)
                    put("merchant", tx.merchant)
                    put("amount", tx.amount)
                    put("currencyCode", tx.currencyCode)
                    put("exchangeRateSnapshotToBdt", tx.exchangeRateSnapshotToBdt)
                    put("type", tx.type)
                    put("categoryId", tx.categoryId)
                    put("categoryName", tx.categoryName)
                    put("categoryColorHex", tx.categoryColorHex)
                    put("categoryIconKey", tx.categoryIconKey)
                    put("accountId", tx.accountId)
                    put("accountLabel", tx.accountLabel)
                    put("timestamp", tx.timestamp)
                    put("note", tx.note)
                    put("isRecurring", tx.isRecurring)
                    put("receiptUri", tx.receiptUri)
                    put("splitDetails", tx.splitDetails)
                    put("isFavorite", tx.isFavorite)
                    put("isDemo", tx.isDemo)
                }
            )
        }
        root.put("transactions", txsArr)
        root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val txsArr = root.optJSONArray("transactions") ?: return@withContext false
            val importedTxs = mutableListOf<TransactionEntity>()
            for (i in 0 until txsArr.length()) {
                val obj = txsArr.getJSONObject(i)
                importedTxs.add(
                    TransactionEntity(
                        title = obj.optString("title", "Restored Entry"),
                        merchant = obj.optString("merchant", "General"),
                        amount = obj.optDouble("amount", 0.0),
                        currencyCode = obj.optString("currencyCode", "BDT"),
                        exchangeRateSnapshotToBdt = obj.optDouble("exchangeRateSnapshotToBdt", 1.0),
                        type = obj.optString("type", "EXPENSE"),
                        categoryId = obj.optInt("categoryId", 1),
                        categoryName = obj.optString("categoryName", "Groceries"),
                        categoryColorHex = obj.optLong("categoryColorHex", 0xFF7C5CFFL),
                        categoryIconKey = obj.optString("categoryIconKey", "GROCERIES"),
                        accountId = obj.optInt("accountId", 1),
                        accountLabel = obj.optString("accountLabel", "Card •••• 4821"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", ""),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        receiptUri = obj.optString("receiptUri", ""),
                        splitDetails = obj.optString("splitDetails", ""),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
            if (importedTxs.isNotEmpty()) {
                dao.clearAllTransactions()
                dao.insertTransactions(importedTxs)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportTransactionsCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Merchant,Type,OriginalAmount,CurrencyCode,Category,Account,Note,SplitDetails,Receipt")
        val list = dao.getAllTransactionsOnce()
        for (tx in list) {
            val dateStr = CurrencyFormatter.formatDateShort(tx.timestamp).replace(",", "")
            val safeTitle = tx.title.replace(",", " ")
            val safeMerchant = tx.merchant.replace(",", " ")
            val safeNote = tx.note.replace(",", " ")
            val safeSplit = tx.splitDetails.replace(",", " ")
            val safeReceipt = tx.receiptUri.replace(",", " ")
            sb.appendLine("$dateStr,$safeTitle,$safeMerchant,${tx.type},${tx.amount},${tx.currencyCode},${tx.categoryName},${tx.accountLabel},$safeNote,$safeSplit,$safeReceipt")
        }
        sb.toString()
    }

    suspend fun importTransactionsCsv(csvContent: String): Int = withContext(Dispatchers.IO) {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return@withContext 0
        val accounts = dao.getAllAccountsOnce()
        val categories = dao.getAllCategoriesOnce()
        val defaultAcc = accounts.firstOrNull() ?: return@withContext 0
        val defaultCat = categories.firstOrNull() ?: return@withContext 0

        var importedCount = 0
        val startIndex = if (lines.first().lowercase().startsWith("date,")) 1 else 0
        for (i in startIndex until lines.size) {
            val parts = lines[i].split(",")
            if (parts.size >= 6) {
                val title = parts[1].trim().ifEmpty { "Imported Entry" }
                val merchant = parts[2].trim().ifEmpty { title }
                val type = parts[3].trim().uppercase().takeIf { it in listOf("EXPENSE", "INCOME", "TRANSFER") } ?: "EXPENSE"
                val amount = parts[4].trim().toDoubleOrNull() ?: continue
                val currencyCode = parts[5].trim().uppercase().ifEmpty { "BDT" }
                val catName = parts.getOrNull(6)?.trim() ?: defaultCat.name
                val matchedCat = categories.find { it.name.equals(catName, ignoreCase = true) } ?: defaultCat
                val note = parts.getOrNull(8)?.trim() ?: "Imported via CSV"

                addTransaction(
                    title = title,
                    merchant = merchant,
                    amount = amount,
                    currencyCode = currencyCode,
                    type = type,
                    category = matchedCat,
                    account = defaultAcc,
                    note = note
                )
                importedCount++
            }
        }
        importedCount
    }
}
