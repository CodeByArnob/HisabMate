package com.example.data.repository

import com.example.data.local.AccountEntity
import com.example.data.local.AppNotificationEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DebtRecordEntity
import com.example.data.local.ExchangeRateEntity
import com.example.data.local.HisabDao
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
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class HisabRepository(private val dao: HisabDao) {

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

    suspend fun ensureSeeded() {
        val existingRates = dao.getExchangeRatesList()
        if (existingRates.size < CurrencyCatalog.supportedCurrencies.size) {
            val existingCodes = existingRates.map { it.currencyCode.uppercase() }.toSet()
            val missing = SeedDataGenerator.initialExchangeRates().filter { it.currencyCode.uppercase() !in existingCodes }
            if (missing.isNotEmpty()) {
                dao.insertExchangeRates(missing)
            }
        }
        val currentPrefs = dao.getUserPreferencesOnce()
        if (currentPrefs == null) {
            val defaultPrefs = UserPreferencesEntity(
                id = 1,
                baseCurrencyCode = "BDT",
                languageCode = "en",
                regionCode = "BD",
                hasCompletedOnboarding = false,
                useSouthAsianGrouping = true,
                themeMode = "LIGHT",
                isBalanceHidden = false
            )
            dao.insertUserPreferences(defaultPrefs)
            HisabHaptics.isHapticsEnabled = defaultPrefs.hapticsEnabled
        } else {
            HisabHaptics.isHapticsEnabled = currentPrefs.hapticsEnabled
        }
        if (dao.getAccountCount() == 0) {
            seedAllDefaultData()
        }
    }

    suspend fun resetToDefaultSampleData() {
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
    }

    private suspend fun seedAllDefaultData() {
        dao.insertAccounts(SeedDataGenerator.initialAccounts())
        dao.insertCategories(SeedDataGenerator.initialCategories())
        dao.insertTransactions(SeedDataGenerator.initialTransactions())
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
        title: String,
        merchant: String,
        amount: Double,
        currencyCode: String,
        type: String,
        category: CategoryEntity,
        account: AccountEntity,
        toAccount: AccountEntity? = null,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "",
        isRecurring: Boolean = false,
        receiptUri: String = "",
        splitDetails: String = "",
        isFavorite: Boolean = false
    ) {
        val ratesMap = getRateMap()
        val rateToBdt = ratesMap[currencyCode.uppercase()]
            ?: CurrencyCatalog.getMetadata(currencyCode).defaultRateToBdt

        val label = when {
            type == "TRANSFER" && toAccount != null -> "${account.name} → ${toAccount.name}"
            account.type == "MFS" -> "Paid with ${account.name.substringBefore(" ")}"
            else -> account.maskedNumber
        }

        val tx = TransactionEntity(
            title = title.trim(),
            merchant = merchant.trim().ifEmpty { category.name },
            amount = amount,
            currencyCode = currencyCode.uppercase(),
            exchangeRateSnapshotToBdt = rateToBdt,
            type = type,
            categoryId = category.id,
            categoryName = if (type == "TRANSFER") "Transfer" else category.name,
            categoryColorHex = if (type == "TRANSFER") 0xFF1D9BF0L else category.colorHex,
            categoryIconKey = if (type == "TRANSFER") "TRANSFER" else category.iconKey,
            accountId = account.id,
            accountLabel = label,
            toAccountId = toAccount?.id,
            timestamp = timestamp,
            note = note.trim(),
            isRecurring = isRecurring,
            receiptUri = receiptUri.trim(),
            splitDetails = splitDetails.trim(),
            isFavorite = isFavorite
        )
        dao.insertTransaction(tx)

        applyAccountDeltaForTransaction(tx, ratesMap, reverse = false)

        if (type == "EXPENSE" && category.monthlyBudget > 0) {
            checkAndRecordBudgetThresholdAlert(category, ratesMap)
        }
    }

    private suspend fun checkAndRecordBudgetThresholdAlert(
        category: CategoryEntity,
        ratesMap: Map<String, Double>
    ) {
        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        if (!prefs.budgetAlertsEnabled) return

        val budgetCurrency = category.budgetCurrencyCode.uppercase()
        val catExpenses = dao.getAllTransactionsOnce().filter { it.type == "EXPENSE" && it.categoryId == category.id }
        val totalSpentInBudgetCurrency = catExpenses.sumOf { tx ->
            CurrencyFormatter.convert(tx.amount, tx.currencyCode, budgetCurrency, ratesMap)
        }
        val pct = if (category.monthlyBudget > 0) {
            ((totalSpentInBudgetCurrency / category.monthlyBudget) * 100.0).toInt()
        } else 0

        val threshold = category.alertThresholdPercent.coerceIn(50, 100)
        if (pct >= threshold) {
            val spentFormatted = CurrencyFormatter.formatCurrency(
                amount = totalSpentInBudgetCurrency,
                currencyCode = budgetCurrency,
                useSouthAsianGrouping = prefs.useSouthAsianGrouping,
                includeCodeSuffix = true
            )
            val limitFormatted = CurrencyFormatter.formatCurrency(
                amount = category.monthlyBudget,
                currencyCode = budgetCurrency,
                useSouthAsianGrouping = prefs.useSouthAsianGrouping,
                includeCodeSuffix = true
            )
            val statusTitle = if (pct >= 100) {
                "${category.name} Budget Exceeded ($pct%)"
            } else {
                "${category.name} Reached $pct% of ${category.budgetPeriod} Budget"
            }
            dao.insertNotification(
                AppNotificationEntity(
                    title = statusTitle,
                    message = "Spent $spentFormatted of your $limitFormatted ${category.budgetPeriod.lowercase()} limit.",
                    categoryTag = "BUDGET",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    accentColorHex = category.colorHex
                )
            )
        }
    }

    suspend fun editTransaction(
        oldTx: TransactionEntity,
        title: String,
        merchant: String,
        amount: Double,
        currencyCode: String,
        type: String,
        category: CategoryEntity,
        account: AccountEntity,
        toAccount: AccountEntity? = null,
        note: String = "",
        isRecurring: Boolean = false,
        receiptUri: String = "",
        splitDetails: String = "",
        isFavorite: Boolean = false
    ) {
        val ratesMap = getRateMap()
        applyAccountDeltaForTransaction(oldTx, ratesMap, reverse = true)

        val rateToBdt = ratesMap[currencyCode.uppercase()]
            ?: CurrencyCatalog.getMetadata(currencyCode).defaultRateToBdt

        val label = when {
            type == "TRANSFER" && toAccount != null -> "${account.name} → ${toAccount.name}"
            account.type == "MFS" -> "Paid with ${account.name.substringBefore(" ")}"
            else -> account.maskedNumber
        }

        val updatedTx = oldTx.copy(
            title = title.trim(),
            merchant = merchant.trim().ifEmpty { category.name },
            amount = amount,
            currencyCode = currencyCode.uppercase(),
            exchangeRateSnapshotToBdt = rateToBdt,
            type = type,
            categoryId = category.id,
            categoryName = if (type == "TRANSFER") "Transfer" else category.name,
            categoryColorHex = if (type == "TRANSFER") 0xFF1D9BF0L else category.colorHex,
            categoryIconKey = if (type == "TRANSFER") "TRANSFER" else category.iconKey,
            accountId = account.id,
            accountLabel = label,
            toAccountId = toAccount?.id,
            note = note.trim(),
            isRecurring = isRecurring,
            receiptUri = receiptUri.trim(),
            splitDetails = splitDetails.trim(),
            isFavorite = isFavorite
        )
        dao.updateTransaction(updatedTx)
        applyAccountDeltaForTransaction(updatedTx, ratesMap, reverse = false)
    }

    suspend fun toggleTransactionFavorite(tx: TransactionEntity): TransactionEntity {
        val updated = tx.copy(isFavorite = !tx.isFavorite)
        dao.updateTransaction(updated)
        return updated
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val ratesMap = getRateMap()
        dao.deleteTransactionById(transaction.id)
        applyAccountDeltaForTransaction(transaction, ratesMap, reverse = true)
    }

    private suspend fun applyAccountDeltaForTransaction(
        tx: TransactionEntity,
        ratesMap: Map<String, Double>,
        reverse: Boolean
    ) {
        val currentAcc = dao.getAccountById(tx.accountId)
        if (currentAcc != null) {
            val deltaInAccCurrency = CurrencyFormatter.convert(
                amount = tx.amount,
                fromCurrency = tx.currencyCode,
                toCurrency = currentAcc.currencyCode,
                ratesToBdt = ratesMap
            )
            when (tx.type) {
                "EXPENSE" -> {
                    val newBal = if (reverse) currentAcc.balance + deltaInAccCurrency
                    else (currentAcc.balance - deltaInAccCurrency).coerceAtLeast(0.0)
                    dao.updateAccount(currentAcc.copy(balance = newBal))
                }
                "INCOME" -> {
                    val newBal = if (reverse) (currentAcc.balance - deltaInAccCurrency).coerceAtLeast(0.0)
                    else currentAcc.balance + deltaInAccCurrency
                    dao.updateAccount(currentAcc.copy(balance = newBal))
                }
                "TRANSFER" -> {
                    val newSourceBal = if (reverse) currentAcc.balance + deltaInAccCurrency
                    else (currentAcc.balance - deltaInAccCurrency).coerceAtLeast(0.0)
                    dao.updateAccount(currentAcc.copy(balance = newSourceBal))

                    tx.toAccountId?.let { targetId ->
                        dao.getAccountById(targetId)?.let { targetAcc ->
                            val targetDelta = CurrencyFormatter.convert(
                                amount = tx.amount,
                                fromCurrency = tx.currencyCode,
                                toCurrency = targetAcc.currencyCode,
                                ratesToBdt = ratesMap
                            )
                            val newTargetBal = if (reverse) (targetAcc.balance - targetDelta).coerceAtLeast(0.0)
                            else targetAcc.balance + targetDelta
                            dao.updateAccount(targetAcc.copy(balance = newTargetBal))
                        }
                    }
                }
            }
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

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 5)
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
                }
            )
        }
        root.put("transactions", txsArr)
        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val txsArr = root.optJSONArray("transactions") ?: return false
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
                        isFavorite = obj.optBoolean("isFavorite", false)
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

    suspend fun exportTransactionsCsv(): String {
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
        return sb.toString()
    }

    suspend fun importTransactionsCsv(csvContent: String): Int {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return 0
        val accounts = dao.getAllAccountsOnce()
        val categories = dao.getAllCategoriesOnce()
        val defaultAcc = accounts.firstOrNull() ?: return 0
        val defaultCat = categories.firstOrNull() ?: return 0

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
        return importedCount
    }
}
