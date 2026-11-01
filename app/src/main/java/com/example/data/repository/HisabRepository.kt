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
        fun array(block: JSONArray.() -> Unit) = JSONArray().apply(block)
        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val root = JSONObject().apply {
            put("version", 7)
            put("exportedAt", System.currentTimeMillis())
            put("preferences", JSONObject().apply {
                put("baseCurrencyCode", prefs.baseCurrencyCode)
                put("useSouthAsianGrouping", prefs.useSouthAsianGrouping)
                put("themeMode", prefs.themeMode)
                put("languageCode", prefs.languageCode)
                put("regionCode", prefs.regionCode)
                put("hasCompletedOnboarding", prefs.hasCompletedOnboarding)
                put("isBalanceHidden", prefs.isBalanceHidden)
                put("budgetAlertsEnabled", prefs.budgetAlertsEnabled)
                put("billRemindersEnabled", prefs.billRemindersEnabled)
                put("debtRemindersEnabled", prefs.debtRemindersEnabled)
                put("goalMilestonesEnabled", prefs.goalMilestonesEnabled)
                put("soundEnabled", prefs.soundEnabled)
                put("vibrationEnabled", prefs.vibrationEnabled)
                put("quietHoursEnabled", prefs.quietHoursEnabled)
                put("quietHoursStartHour", prefs.quietHoursStartHour)
                put("quietHoursEndHour", prefs.quietHoursEndHour)
                put("reminderLeadTimeDays", prefs.reminderLeadTimeDays)
                put("hapticsEnabled", prefs.hapticsEnabled)
            })
        }
        root.put("accounts", array {
            dao.getAllAccountsOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("name", x.name); put("type", x.type); put("balance", x.balance)
                put("currencyCode", x.currencyCode); put("maskedNumber", x.maskedNumber)
                put("colorHex", x.colorHex); put("iconKey", x.iconKey); put("isPrimary", x.isPrimary); put("isDemo", x.isDemo)
            }) }
        })
        root.put("categories", array {
            dao.getAllCategoriesOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("name", x.name); put("type", x.type); put("monthlyBudget", x.monthlyBudget)
                put("budgetCurrencyCode", x.budgetCurrencyCode); put("budgetPeriod", x.budgetPeriod)
                put("alertThresholdPercent", x.alertThresholdPercent); put("startDateMillis", x.startDateMillis)
                put("endDateMillis", x.endDateMillis); put("colorHex", x.colorHex); put("iconKey", x.iconKey)
                put("isQuickAction", x.isQuickAction); put("isDemo", x.isDemo)
            }) }
        })
        root.put("transactions", array {
            dao.getAllTransactionsOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("title", x.title); put("merchant", x.merchant); put("amount", x.amount)
                put("currencyCode", x.currencyCode); put("exchangeRateSnapshotToBdt", x.exchangeRateSnapshotToBdt)
                put("accountExchangeRateSnapshotToBdt", x.accountExchangeRateSnapshotToBdt)
                put("toAccountExchangeRateSnapshotToBdt", x.toAccountExchangeRateSnapshotToBdt)
                put("type", x.type); put("categoryId", x.categoryId); put("categoryName", x.categoryName)
                put("categoryColorHex", x.categoryColorHex); put("categoryIconKey", x.categoryIconKey)
                put("accountId", x.accountId); put("accountLabel", x.accountLabel)
                if (x.toAccountId != null) put("toAccountId", x.toAccountId)
                put("timestamp", x.timestamp); put("note", x.note); put("isRecurring", x.isRecurring)
                put("receiptUri", x.receiptUri); put("splitDetails", x.splitDetails)
                put("isFavorite", x.isFavorite); put("isDemo", x.isDemo)
            }) }
        })
        root.put("recurringPayments", array {
            dao.getAllRecurringPaymentsOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("title", x.title); put("provider", x.provider); put("amount", x.amount)
                put("currencyCode", x.currencyCode); put("transactionType", x.transactionType); put("frequency", x.frequency)
                put("nextDueDateMillis", x.nextDueDateMillis); put("categoryId", x.categoryId); put("categoryName", x.categoryName)
                put("colorHex", x.colorHex); put("iconKey", x.iconKey); put("autoPay", x.autoPay); put("isDemo", x.isDemo)
            }) }
        })
        root.put("debts", array {
            dao.getAllDebtRecordsOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("personName", x.personName); put("relationOrNote", x.relationOrNote)
                put("totalAmount", x.totalAmount); put("settledAmount", x.settledAmount); put("currencyCode", x.currencyCode)
                put("type", x.type); put("dueDateMillis", x.dueDateMillis); put("phoneOrTag", x.phoneOrTag)
                put("colorHex", x.colorHex); put("repaymentHistory", x.repaymentHistory); put("isDemo", x.isDemo)
            }) }
        })
        root.put("goals", array {
            dao.getAllSavingsGoalsOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("title", x.title); put("subtitle", x.subtitle)
                put("targetAmount", x.targetAmount); put("savedAmount", x.savedAmount); put("currencyCode", x.currencyCode)
                put("deadlineMillis", x.deadlineMillis); put("colorHex", x.colorHex); put("iconKey", x.iconKey); put("isDemo", x.isDemo)
            }) }
        })
        root.put("notifications", array {
            dao.getAllNotificationsOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("title", x.title); put("message", x.message); put("categoryTag", x.categoryTag)
                put("timestamp", x.timestamp); put("isRead", x.isRead); put("accentColorHex", x.accentColorHex); put("isDemo", x.isDemo)
            }) }
        })
        root.put("scheduledReminders", array {
            dao.getAllScheduledRemindersOnce().forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("notificationId", x.notificationId); put("sourceType", x.sourceType)
                put("sourceId", x.sourceId); put("title", x.title); put("subtitle", x.subtitle); put("amount", x.amount)
                put("currencyCode", x.currencyCode); put("triggerTimeMillis", x.triggerTimeMillis)
                put("frequency", x.frequency); put("leadTimeDays", x.leadTimeDays); put("isEnabled", x.isEnabled)
                put("lastNotifiedMillis", x.lastNotifiedMillis); put("accentColorHex", x.accentColorHex); put("isDemo", x.isDemo)
            }) }
        })
        root.put("exchangeRates", array {
            dao.getExchangeRatesList().forEach { x -> put(JSONObject().apply {
                put("currencyCode", x.currencyCode); put("rateToBdt", x.rateToBdt)
                put("lastUpdatedMillis", x.lastUpdatedMillis); put("sourceLabel", x.sourceLabel); put("isCustomRate", x.isCustomRate)
            }) }
        })
        root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 0)
            if (version !in 6..7) return@withContext false
            val accountsJson = root.optJSONArray("accounts") ?: return@withContext false
            val txJson = root.optJSONArray("transactions") ?: return@withContext false
            val accounts = mutableListOf<AccountEntity>()
            for (i in 0 until accountsJson.length()) {
                val o = accountsJson.getJSONObject(i)
                val currency = o.optString("currencyCode", "BDT").uppercase()
                val balance = o.optDouble("balance", Double.NaN)
                if (!CurrencyCatalog.isSupported(currency) || !balance.isFinite()) return@withContext false
                accounts += AccountEntity(
                    id = o.optInt("id", 0), name = o.optString("name"), type = o.optString("type", "CASH"),
                    balance = balance, currencyCode = currency, maskedNumber = o.optString("maskedNumber", ""),
                    colorHex = o.optLong("colorHex", 0xFF059669L), iconKey = o.optString("iconKey", "CASH"),
                    isPrimary = o.optBoolean("isPrimary"), isDemo = o.optBoolean("isDemo")
                )
            }
            if (accounts.isEmpty() || accounts.map { it.id }.toSet().size != accounts.size) return@withContext false
            val accountById = accounts.associateBy { it.id }
            val currentRates = getRateMap()
            val transactions = mutableListOf<TransactionEntity>()
            for (i in 0 until txJson.length()) {
                val o = txJson.getJSONObject(i)
                val currency = o.optString("currencyCode", "BDT").uppercase()
                val type = o.optString("type", "EXPENSE").uppercase()
                val amount = o.optDouble("amount", Double.NaN)
                val accountId = o.optInt("accountId", -1)
                if (!CurrencyCatalog.isSupported(currency) || !amount.isFinite() || amount <= 0.0) return@withContext false
                if (accountId !in accountById || type !in setOf("EXPENSE", "INCOME", "TRANSFER")) return@withContext false
                val targetId = if (o.has("toAccountId")) o.optInt("toAccountId") else null
                if (type == "TRANSFER" && (version < 7 || targetId == null || targetId !in accountById || targetId == accountId)) return@withContext false
                val sourceCurrency = accountById.getValue(accountId).currencyCode
                val targetCurrency = targetId?.let { accountById.getValue(it).currencyCode }
                val txRate = o.optDouble("exchangeRateSnapshotToBdt", rateFor(currency, currentRates))
                val sourceRate = o.optDouble("accountExchangeRateSnapshotToBdt", rateFor(sourceCurrency, currentRates))
                val targetRate = targetCurrency?.let { o.optDouble("toAccountExchangeRateSnapshotToBdt", rateFor(it, currentRates)) } ?: 1.0
                if (listOf(txRate, sourceRate, targetRate).any { !it.isFinite() || it <= 0.0 }) return@withContext false
                transactions += TransactionEntity(
                    id = if (version >= 7) o.optInt("id", 0) else 0,
                    title = o.optString("title", "Restored Entry"), merchant = o.optString("merchant", "General"),
                    amount = amount, currencyCode = currency, exchangeRateSnapshotToBdt = txRate,
                    accountExchangeRateSnapshotToBdt = sourceRate, toAccountExchangeRateSnapshotToBdt = targetRate,
                    type = type, categoryId = o.optInt("categoryId", 1), categoryName = o.optString("categoryName", "General"),
                    categoryColorHex = o.optLong("categoryColorHex", 0xFF7C5CFFL), categoryIconKey = o.optString("categoryIconKey", "GROCERIES"),
                    accountId = accountId, accountLabel = o.optString("accountLabel", "Restored account"), toAccountId = targetId,
                    timestamp = o.optLong("timestamp", System.currentTimeMillis()), note = o.optString("note", ""),
                    isRecurring = o.optBoolean("isRecurring"), receiptUri = o.optString("receiptUri", ""),
                    splitDetails = o.optString("splitDetails", ""), isFavorite = o.optBoolean("isFavorite"), isDemo = o.optBoolean("isDemo")
                )
            }

            if (version == 6) {
                // Old backups omit destination IDs for transfers. Those are rejected above;
                // other v6 backups restore account balances + transactions without deleting other modules.
                database.withTransaction {
                    dao.clearAllTransactions()
                    dao.clearAllAccounts()
                    dao.insertAccounts(accounts)
                    dao.insertTransactions(transactions)
                }
                return@withContext true
            }

            val categoriesJson = root.optJSONArray("categories") ?: return@withContext false
            val recurringJson = root.optJSONArray("recurringPayments") ?: return@withContext false
            val debtsJson = root.optJSONArray("debts") ?: return@withContext false
            val goalsJson = root.optJSONArray("goals") ?: return@withContext false
            val notificationsJson = root.optJSONArray("notifications") ?: return@withContext false
            val remindersJson = root.optJSONArray("scheduledReminders") ?: return@withContext false
            val ratesJson = root.optJSONArray("exchangeRates") ?: return@withContext false

            val categories = (0 until categoriesJson.length()).map { i ->
                val o = categoriesJson.getJSONObject(i)
                val currency = o.optString("budgetCurrencyCode", "BDT").uppercase()
                if (!CurrencyCatalog.isSupported(currency)) return@withContext false
                CategoryEntity(
                    id=o.optInt("id",0), name=o.optString("name"), type=o.optString("type","EXPENSE"),
                    monthlyBudget=o.optDouble("monthlyBudget",0.0), budgetCurrencyCode=currency,
                    budgetPeriod=o.optString("budgetPeriod","MONTHLY"), alertThresholdPercent=o.optInt("alertThresholdPercent",80).coerceIn(50,100),
                    startDateMillis=o.optLong("startDateMillis"), endDateMillis=o.optLong("endDateMillis"),
                    colorHex=o.optLong("colorHex",0xFF6D4AFFL), iconKey=o.optString("iconKey","GROCERIES"),
                    isQuickAction=o.optBoolean("isQuickAction",true), isDemo=o.optBoolean("isDemo")
                )
            }
            val recurring = (0 until recurringJson.length()).map { i ->
                val o=recurringJson.getJSONObject(i); val c=o.optString("currencyCode","BDT").uppercase()
                val amount=o.optDouble("amount",Double.NaN)
                if (!CurrencyCatalog.isSupported(c) || !amount.isFinite()) return@withContext false
                RecurringPaymentEntity(
                    id=o.optInt("id",0),title=o.optString("title"),provider=o.optString("provider"),amount=amount,currencyCode=c,
                    transactionType=o.optString("transactionType","EXPENSE"),frequency=o.optString("frequency","ONCE"),
                    nextDueDateMillis=o.optLong("nextDueDateMillis"),categoryId=o.optInt("categoryId"),
                    categoryName=o.optString("categoryName"),colorHex=o.optLong("colorHex",0xFF6D4AFFL),
                    iconKey=o.optString("iconKey","BILL"),autoPay=o.optBoolean("autoPay",true),isDemo=o.optBoolean("isDemo")
                )
            }
            val debts = (0 until debtsJson.length()).map { i ->
                val o=debtsJson.getJSONObject(i); val c=o.optString("currencyCode","BDT").uppercase()
                if (!CurrencyCatalog.isSupported(c)) return@withContext false
                DebtRecordEntity(
                    id=o.optInt("id",0),personName=o.optString("personName"),relationOrNote=o.optString("relationOrNote",""),
                    totalAmount=o.optDouble("totalAmount",0.0),settledAmount=o.optDouble("settledAmount",0.0),currencyCode=c,
                    type=o.optString("type","LENT"),dueDateMillis=o.optLong("dueDateMillis"),phoneOrTag=o.optString("phoneOrTag",""),
                    colorHex=o.optLong("colorHex",0xFF6D4AFFL),repaymentHistory=o.optString("repaymentHistory",""),isDemo=o.optBoolean("isDemo")
                )
            }
            val goals = (0 until goalsJson.length()).map { i ->
                val o=goalsJson.getJSONObject(i); val c=o.optString("currencyCode","BDT").uppercase()
                if (!CurrencyCatalog.isSupported(c)) return@withContext false
                SavingsGoalEntity(
                    id=o.optInt("id",0),title=o.optString("title"),subtitle=o.optString("subtitle",""),
                    targetAmount=o.optDouble("targetAmount",0.0),savedAmount=o.optDouble("savedAmount",0.0),currencyCode=c,
                    deadlineMillis=o.optLong("deadlineMillis"),colorHex=o.optLong("colorHex",0xFF6D4AFFL),
                    iconKey=o.optString("iconKey","GOAL"),isDemo=o.optBoolean("isDemo")
                )
            }
            val notifications = (0 until notificationsJson.length()).map { i ->
                val o=notificationsJson.getJSONObject(i)
                AppNotificationEntity(
                    id=o.optInt("id",0),title=o.optString("title"),message=o.optString("message"),
                    categoryTag=o.optString("categoryTag","INSIGHT"),timestamp=o.optLong("timestamp"),isRead=o.optBoolean("isRead"),
                    accentColorHex=o.optLong("accentColorHex",0xFF6D4AFFL),isDemo=o.optBoolean("isDemo")
                )
            }
            val reminders = (0 until remindersJson.length()).map { i ->
                val o=remindersJson.getJSONObject(i); val c=o.optString("currencyCode","BDT").uppercase()
                val amount=o.optDouble("amount",Double.NaN)
                if (!CurrencyCatalog.isSupported(c) || !amount.isFinite()) return@withContext false
                ScheduledReminderEntity(
                    id=o.optInt("id",0),notificationId=o.optInt("notificationId"),sourceType=o.optString("sourceType","CUSTOM"),
                    sourceId=o.optInt("sourceId"),title=o.optString("title"),subtitle=o.optString("subtitle",""),
                    amount=amount,currencyCode=c,triggerTimeMillis=o.optLong("triggerTimeMillis"),
                    frequency=o.optString("frequency","ONCE"),leadTimeDays=o.optInt("leadTimeDays",1),
                    isEnabled=o.optBoolean("isEnabled",true),lastNotifiedMillis=o.optLong("lastNotifiedMillis"),
                    accentColorHex=o.optLong("accentColorHex",0xFF6D4AFFL),isDemo=o.optBoolean("isDemo")
                )
            }
            val exchangeRates = (0 until ratesJson.length()).map { i ->
                val o=ratesJson.getJSONObject(i); val c=o.optString("currencyCode","").uppercase()
                val rate=o.optDouble("rateToBdt",Double.NaN)
                if (!CurrencyCatalog.isSupported(c) || !rate.isFinite() || rate<=0.0) return@withContext false
                ExchangeRateEntity(
                    currencyCode=c,rateToBdt=rate,lastUpdatedMillis=o.optLong("lastUpdatedMillis"),
                    sourceLabel=o.optString("sourceLabel","Restored backup"),isCustomRate=o.optBoolean("isCustomRate")
                )
            }
            val preferenceJson=root.optJSONObject("preferences") ?: JSONObject()
            val current=dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
            val restored=current.copy(
                baseCurrencyCode=preferenceJson.optString("baseCurrencyCode",current.baseCurrencyCode),
                useSouthAsianGrouping=preferenceJson.optBoolean("useSouthAsianGrouping",current.useSouthAsianGrouping),
                themeMode=preferenceJson.optString("themeMode",current.themeMode),
                languageCode=preferenceJson.optString("languageCode",current.languageCode),
                regionCode=preferenceJson.optString("regionCode",current.regionCode),
                hasCompletedOnboarding=preferenceJson.optBoolean("hasCompletedOnboarding",current.hasCompletedOnboarding),
                isBalanceHidden=preferenceJson.optBoolean("isBalanceHidden",current.isBalanceHidden),
                budgetAlertsEnabled=preferenceJson.optBoolean("budgetAlertsEnabled",current.budgetAlertsEnabled),
                billRemindersEnabled=preferenceJson.optBoolean("billRemindersEnabled",current.billRemindersEnabled),
                debtRemindersEnabled=preferenceJson.optBoolean("debtRemindersEnabled",current.debtRemindersEnabled),
                goalMilestonesEnabled=preferenceJson.optBoolean("goalMilestonesEnabled",current.goalMilestonesEnabled),
                soundEnabled=preferenceJson.optBoolean("soundEnabled",current.soundEnabled),
                vibrationEnabled=preferenceJson.optBoolean("vibrationEnabled",current.vibrationEnabled),
                quietHoursEnabled=preferenceJson.optBoolean("quietHoursEnabled",current.quietHoursEnabled),
                quietHoursStartHour=preferenceJson.optInt("quietHoursStartHour",current.quietHoursStartHour),
                quietHoursEndHour=preferenceJson.optInt("quietHoursEndHour",current.quietHoursEndHour),
                reminderLeadTimeDays=preferenceJson.optInt("reminderLeadTimeDays",current.reminderLeadTimeDays),
                hapticsEnabled=preferenceJson.optBoolean("hapticsEnabled",current.hapticsEnabled)
            )

            database.withTransaction {
                dao.clearAllTransactions(); dao.clearAllAccounts(); dao.clearAllCategories()
                dao.clearAllRecurring(); dao.clearAllDebts(); dao.clearAllGoals()
                dao.clearAllNotifications(); dao.clearAllScheduledReminders(); dao.clearAllExchangeRates()
                dao.insertAccounts(accounts); dao.insertCategories(categories); dao.insertTransactions(transactions)
                dao.insertRecurringPayments(recurring); dao.insertDebtRecords(debts); dao.insertSavingsGoals(goals)
                dao.insertNotifications(notifications); dao.insertScheduledReminders(reminders); dao.insertExchangeRates(exchangeRates)
                dao.insertUserPreferences(restored)
            }
            HisabHaptics.isHapticsEnabled = restored.hapticsEnabled
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
