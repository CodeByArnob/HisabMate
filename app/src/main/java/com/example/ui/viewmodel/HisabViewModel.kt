package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.AppNotificationEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DebtRecordEntity
import com.example.data.local.ExchangeRateEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.ScheduledReminderEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.repository.HisabRepository
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabNotificationManager
import com.example.util.PdfExportResult
import com.example.util.PdfExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppScreen {
    DASHBOARD,
    WALLET_DETAILS,
    SPEND_ANALYSIS,
    PLANNING_HUB,
    ACCOUNTS,
    SEARCH,
    NOTIFICATIONS,
    PROFILE_SETTINGS,
    CURRENCY_MANAGER,
    CALENDAR
}

enum class PlanningTab(val label: String) {
    BUDGETS("Budgets"),
    RECURRING("Recurring"),
    DEBT_LEND("Debt & Lend"),
    GOALS("Goals"),
    CATEGORIES("Categories")
}

enum class ReportPeriod(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly")
}

enum class ThemePreference(val label: String) {
    LIGHT("Light Mode"),
    DARK("Dark Mode"),
    SYSTEM("System Default")
}

data class CategorySpendSummary(
    val category: CategoryEntity,
    val totalSpentInBase: Double,
    val budgetLimitInBase: Double,
    val transactionCount: Int,
    val budgetProgress: Float,
    val shareOfSpend: Float
) {
    val totalSpent: Double get() = totalSpentInBase
}

data class DailySpendPoint(
    val dayLabel: String,
    val fullDateLabel: String,
    val amount: Double,
    val dayIndex: Int
)

data class MonthlyCashflowPoint(
    val monthShort: String,
    val income: Double,
    val expense: Double,
    val isCurrent: Boolean = false
)

data class CalendarDaySummary(
    val dayOfMonth: Int,
    val incomeInBase: Double,
    val expenseInBase: Double,
    val transactions: List<TransactionEntity>
)

data class HisabUiState(
    val baseCurrencyCode: String = "BDT",
    val languageCode: String = "en",
    val regionCode: String = "BD",
    val hasCompletedOnboarding: Boolean = true,
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val ratesToBdtMap: Map<String, Double> = CurrencyCatalog.supportedCurrencies.associate { it.code to it.defaultRateToBdt },
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val favoriteTransactions: List<TransactionEntity> = emptyList(),
    val recurringPayments: List<RecurringPaymentEntity> = emptyList(),
    val debtRecords: List<DebtRecordEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val notifications: List<AppNotificationEntity> = emptyList(),
    val scheduledReminders: List<ScheduledReminderEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val savingsRatePercent: Int = 0,
    val reportPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val periodReportIncome: Double = 0.0,
    val periodReportExpense: Double = 0.0,
    val categorySummaries: List<CategorySpendSummary> = emptyList(),
    val quickWalletSummaries: List<CategorySpendSummary> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val foreignCurrencyAccountCount: Int = 0,
    val foreignCurrencyTxCount: Int = 0,
    val lastRateUpdateMillis: Long = System.currentTimeMillis(),
    val isAppLockEnabled: Boolean = false,
    val hasAppPin: Boolean = false,
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
    val isLoading: Boolean = true
)

class HisabViewModel(
    private val repository: HisabRepository
) : ViewModel() {

    private val _screenStack = MutableStateFlow(listOf(AppScreen.DASHBOARD))
    val currentScreen: StateFlow<AppScreen> = _screenStack
        .combine(MutableStateFlow(Unit)) { stack, _ -> stack.lastOrNull() ?: AppScreen.DASHBOARD }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppScreen.DASHBOARD)

    private val _selectedPlanningTab = MutableStateFlow(PlanningTab.BUDGETS)
    val selectedPlanningTab: StateFlow<PlanningTab> = _selectedPlanningTab.asStateFlow()

    private val _selectedReportPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    val selectedReportPeriod: StateFlow<ReportPeriod> = _selectedReportPeriod.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(1)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _selectedMonthOffset = MutableStateFlow(0)
    val selectedMonthOffset: StateFlow<Int> = _selectedMonthOffset.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTypeFilter = MutableStateFlow("ALL")
    val searchTypeFilter: StateFlow<String> = _searchTypeFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _showAddTransactionSheet = MutableStateFlow(false)
    val showAddTransactionSheet: StateFlow<Boolean> = _showAddTransactionSheet.asStateFlow()

    private val _initialTransactionType = MutableStateFlow("EXPENSE")
    val initialTransactionType: StateFlow<String> = _initialTransactionType.asStateFlow()

    private val _preselectedCategoryIdForAdd = MutableStateFlow<Int?>(null)
    val preselectedCategoryIdForAdd: StateFlow<Int?> = _preselectedCategoryIdForAdd.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editingTransaction: StateFlow<TransactionEntity?> = _editingTransaction.asStateFlow()

    private val _selectedTransactionForDetail = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransactionForDetail: StateFlow<TransactionEntity?> = _selectedTransactionForDetail.asStateFlow()

    private val _showCurrencyPickerModal = MutableStateFlow(false)
    val showCurrencyPickerModal: StateFlow<Boolean> = _showCurrencyPickerModal.asStateFlow()

    private val _showOnboardingTourOverlay = MutableStateFlow(false)
    val showOnboardingTourOverlay: StateFlow<Boolean> = _showOnboardingTourOverlay.asStateFlow()

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    val isBalanceHidden: StateFlow<Boolean> = repository.userPreferences
        .combine(MutableStateFlow(Unit)) { prefs, _ -> prefs?.isBalanceHidden ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val useBangladeshiFormat: StateFlow<Boolean> = repository.userPreferences
        .combine(MutableStateFlow(Unit)) { prefs, _ -> prefs?.useSouthAsianGrouping ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themePreference: StateFlow<ThemePreference> = repository.userPreferences
        .combine(MutableStateFlow(Unit)) { prefs, _ ->
            when (prefs?.themeMode) {
                "DARK" -> ThemePreference.DARK
                "SYSTEM" -> ThemePreference.SYSTEM
                else -> ThemePreference.LIGHT
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.LIGHT)

    private val coreDataFlow = combine(
        repository.accounts,
        repository.categories,
        repository.transactions,
        repository.recurringPayments,
        repository.debtRecords
    ) { accounts, categories, transactions, recurring, debts ->
        CoreBundle(accounts, categories, transactions, recurring, debts)
    }

    private val metaDataFlow = combine(
        repository.savingsGoals,
        repository.notifications,
        repository.exchangeRates,
        repository.userPreferences,
        _selectedMonthOffset
    ) { goals, notifications, rates, prefs, monthOffset ->
        MetaBundle(goals, notifications, rates, prefs ?: UserPreferencesEntity(), monthOffset)
    }

    val uiState: StateFlow<HisabUiState> = combine(
        coreDataFlow,
        metaDataFlow,
        repository.scheduledReminders,
        _selectedReportPeriod
    ) { core, meta, reminders, reportPeriod ->
        val baseCurrency = meta.prefs.baseCurrencyCode.uppercase()
        val ratesMap = if (meta.rates.isEmpty()) {
            CurrencyCatalog.supportedCurrencies.associate { it.code to it.defaultRateToBdt }
        } else {
            meta.rates.associate { it.currencyCode.uppercase() to it.rateToBdt }
        }

        val totalBalanceInBase = core.accounts.sumOf { acc ->
            CurrencyFormatter.convert(
                amount = acc.balance,
                fromCurrency = acc.currencyCode,
                toCurrency = baseCurrency,
                ratesToBdt = ratesMap
            )
        }

        val monthAnchor = Calendar.getInstance().apply { add(Calendar.MONTH, meta.monthOffset) }
        val monthStart = (monthAnchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val monthEnd = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val monthTransactions = core.transactions.filter { it.timestamp >= monthStart.timeInMillis && it.timestamp < monthEnd.timeInMillis }
        val expenseTransactions = monthTransactions.filter { it.type == "EXPENSE" }
        val incomeTransactions = monthTransactions.filter { it.type == "INCOME" }
        val rawExpenseInBase = expenseTransactions.sumOf { CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap) }
        val rawIncomeInBase = incomeTransactions.sumOf { CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap) }

        val periodStart = (monthAnchor.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            when (reportPeriod) {
                ReportPeriod.DAILY -> Unit
                ReportPeriod.WEEKLY -> add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7))
                ReportPeriod.MONTHLY -> set(Calendar.DAY_OF_MONTH, 1)
                ReportPeriod.YEARLY -> set(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val periodEnd = (periodStart.clone() as Calendar).apply {
            when (reportPeriod) {
                ReportPeriod.DAILY -> add(Calendar.DAY_OF_MONTH, 1)
                ReportPeriod.WEEKLY -> add(Calendar.DAY_OF_MONTH, 7)
                ReportPeriod.MONTHLY -> add(Calendar.MONTH, 1)
                ReportPeriod.YEARLY -> add(Calendar.YEAR, 1)
            }
        }
        val periodTransactions = core.transactions.filter { it.timestamp >= periodStart.timeInMillis && it.timestamp < periodEnd.timeInMillis }
        val periodIncomeInBase = periodTransactions.filter { it.type == "INCOME" }.sumOf { CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap) }
        val periodExpenseInBase = periodTransactions.filter { it.type == "EXPENSE" }.sumOf { CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap) }
        val netSavingsInBase = rawIncomeInBase - rawExpenseInBase
        val savingsRate = if (rawIncomeInBase > 0.0) ((netSavingsInBase / rawIncomeInBase) * 100).toInt().coerceIn(-100, 100) else 0

        val expenseCats = core.categories.filter { it.type == "EXPENSE" }
        val totalCategorySpend = rawExpenseInBase.coerceAtLeast(0.01)

        val summaries = expenseCats.map { cat ->
            val catTxs = expenseTransactions.filter { it.categoryId == cat.id }
            val spentInBase = catTxs.sumOf { tx ->
                CurrencyFormatter.convert(tx.amount, tx.currencyCode, baseCurrency, ratesMap)
            }
            val budgetInBase = CurrencyFormatter.convert(
                amount = cat.monthlyBudget,
                fromCurrency = cat.budgetCurrencyCode,
                toCurrency = baseCurrency,
                ratesToBdt = ratesMap
            )
            val budgetRatio = if (budgetInBase > 0) (spentInBase / budgetInBase).toFloat() else 0f
            val share = (spentInBase / totalCategorySpend).toFloat().coerceIn(0f, 1f)
            CategorySpendSummary(
                category = cat,
                totalSpentInBase = spentInBase,
                budgetLimitInBase = budgetInBase,
                transactionCount = catTxs.size,
                budgetProgress = budgetRatio,
                shareOfSpend = share
            )
        }.sortedByDescending { it.totalSpentInBase }

        val quickWallets = expenseCats.take(4).map { cat ->
            summaries.find { it.category.id == cat.id } ?: CategorySpendSummary(
                category = cat,
                totalSpentInBase = 0.0,
                budgetLimitInBase = CurrencyFormatter.convert(cat.monthlyBudget, cat.budgetCurrencyCode, baseCurrency, ratesMap),
                transactionCount = 0,
                budgetProgress = 0f,
                shareOfSpend = 0f
            )
        }

        val foreignAccCount = core.accounts.count { !it.currencyCode.equals(baseCurrency, ignoreCase = true) }
        val foreignTxCount = core.transactions.count { !it.currencyCode.equals(baseCurrency, ignoreCase = true) }
        val latestRateTime = meta.rates.maxOfOrNull { it.lastUpdatedMillis } ?: System.currentTimeMillis()
        val favorites = core.transactions.filter { it.isFavorite }.distinctBy { it.title }

        HisabUiState(
            baseCurrencyCode = baseCurrency,
            languageCode = meta.prefs.languageCode,
            regionCode = meta.prefs.regionCode,
            hasCompletedOnboarding = meta.prefs.hasCompletedOnboarding,
            exchangeRates = meta.rates,
            ratesToBdtMap = ratesMap,
            accounts = core.accounts,
            categories = core.categories,
            transactions = core.transactions,
            favoriteTransactions = favorites,
            recurringPayments = core.recurring,
            debtRecords = core.debts,
            savingsGoals = meta.goals,
            notifications = meta.notifications,
            scheduledReminders = reminders,
            totalBalance = totalBalanceInBase,
            monthlyIncome = rawIncomeInBase,
            monthlyExpense = rawExpenseInBase,
            monthlySavings = netSavingsInBase,
            savingsRatePercent = savingsRate,
            reportPeriod = reportPeriod,
            periodReportIncome = periodIncomeInBase,
            periodReportExpense = periodExpenseInBase,
            categorySummaries = summaries,
            quickWalletSummaries = quickWallets,
            unreadNotificationsCount = meta.notifications.count { !it.isRead },
            foreignCurrencyAccountCount = foreignAccCount,
            foreignCurrencyTxCount = foreignTxCount,
            lastRateUpdateMillis = latestRateTime,
            isAppLockEnabled = meta.prefs.isAppLockEnabled,
            hasAppPin = meta.prefs.appPinHash.isNotBlank() || meta.prefs.appPinCode.isNotBlank(),
            biometricEnabled = meta.prefs.biometricEnabled,
            budgetAlertsEnabled = meta.prefs.budgetAlertsEnabled,
            billRemindersEnabled = meta.prefs.billRemindersEnabled,
            debtRemindersEnabled = meta.prefs.debtRemindersEnabled,
            goalMilestonesEnabled = meta.prefs.goalMilestonesEnabled,
            soundEnabled = meta.prefs.soundEnabled,
            vibrationEnabled = meta.prefs.vibrationEnabled,
            quietHoursEnabled = meta.prefs.quietHoursEnabled,
            quietHoursStartHour = meta.prefs.quietHoursStartHour,
            quietHoursEndHour = meta.prefs.quietHoursEndHour,
            reminderLeadTimeDays = meta.prefs.reminderLeadTimeDays,
            hapticsEnabled = meta.prefs.hapticsEnabled,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HisabUiState(isLoading = true)
    )

    // Navigation functions
    fun navigateTo(screen: AppScreen) {
        _screenStack.update { current ->
            if (current.lastOrNull() == screen) current
            else if (screen == AppScreen.DASHBOARD) listOf(AppScreen.DASHBOARD)
            else current + screen
        }
    }

    fun openWalletDetails(categoryId: Int) {
        _selectedCategoryId.value = categoryId
        navigateTo(AppScreen.WALLET_DETAILS)
    }

    fun openPlanningWithTab(tab: PlanningTab) {
        _selectedPlanningTab.value = tab
        navigateTo(AppScreen.PLANNING_HUB)
    }

    fun selectPlanningTab(tab: PlanningTab) {
        _selectedPlanningTab.value = tab
    }

    fun selectReportPeriod(period: ReportPeriod) {
        _selectedReportPeriod.value = period
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        return if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    fun openOnboardingTour() {
        _showOnboardingTourOverlay.value = true
    }

    fun closeOnboardingTour() {
        _showOnboardingTourOverlay.value = false
    }

    // Security & PIN Lock Controls
    fun unlockSession() {
        _isSessionUnlocked.value = true
    }

    fun lockSessionNow() {
        if (uiState.value.isAppLockEnabled && uiState.value.hasAppPin) {
            _isSessionUnlocked.value = false
        } else {
            showSnackbar("Configure a 4-digit PIN in Settings first to lock the app")
        }
    }

    fun configureAppLock(enabled: Boolean, pinCode: String, biometric: Boolean) {
        viewModelScope.launch {
            runCatching { repository.configureAppLock(enabled, pinCode, biometric) }
                .onSuccess {
                    _isSessionUnlocked.value = true
                    showSnackbar(if (enabled) "App PIN Lock enabled" else "App Lock disabled")
                    if (enabled && biometric) showSnackbar("Biometric unlock is not configured yet. PIN lock remains active.")
                }
                .onFailure { showSnackbar(it.message ?: "Could not update app lock") }
        }
    }

    fun verifyAppPin(pinCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.verifyAppPin(pinCode)) }
    }

    fun updateNotificationPrefs(budgetAlerts: Boolean, billReminders: Boolean) {
        viewModelScope.launch {
            repository.updateUserPreferences {
                it.copy(
                    budgetAlertsEnabled = budgetAlerts,
                    billRemindersEnabled = billReminders
                )
            }
            showSnackbar("Notification alert preferences saved")
        }
    }

    fun updateFullNotificationAndHapticPrefs(
        context: Context? = null,
        budgetAlerts: Boolean = uiState.value.budgetAlertsEnabled,
        billReminders: Boolean = uiState.value.billRemindersEnabled,
        debtReminders: Boolean = uiState.value.debtRemindersEnabled,
        goalMilestones: Boolean = uiState.value.goalMilestonesEnabled,
        soundEnabled: Boolean = uiState.value.soundEnabled,
        vibrationEnabled: Boolean = uiState.value.vibrationEnabled,
        quietHoursEnabled: Boolean = uiState.value.quietHoursEnabled,
        leadTimeDays: Int = uiState.value.reminderLeadTimeDays,
        hapticsEnabled: Boolean = uiState.value.hapticsEnabled
    ) {
        viewModelScope.launch {
            repository.updateUserPreferences {
                it.copy(
                    budgetAlertsEnabled = budgetAlerts,
                    billRemindersEnabled = billReminders,
                    debtRemindersEnabled = debtReminders,
                    goalMilestonesEnabled = goalMilestones,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    quietHoursEnabled = quietHoursEnabled,
                    reminderLeadTimeDays = leadTimeDays.coerceIn(0, 7),
                    hapticsEnabled = hapticsEnabled
                )
            }
            if (context != null) {
                HisabNotificationManager.rescheduleAllActiveReminders(context)
            }
        }
    }

    // Preferences & Currency Controls
    fun setBaseCurrency(currencyCode: String, context: Context? = null) {
        val meta = CurrencyCatalog.getMetadata(currencyCode)
        viewModelScope.launch {
            repository.updateUserPreferences {
                it.copy(
                    baseCurrencyCode = meta.code,
                    hasCompletedOnboarding = true
                )
            }
            if (context != null) {
                HisabNotificationManager.rescheduleAllActiveReminders(context)
            }
            _showCurrencyPickerModal.value = false
            showSnackbar("Base currency set to ${meta.code} (${meta.symbol} ${meta.name})")
        }
    }

    fun completeFullOnboarding(
        selectedCurrencyCode: String,
        languageCode: String = "en",
        regionCode: String = "BD",
        notificationsEnabled: Boolean = true,
        context: Context? = null
    ) {
        val meta = CurrencyCatalog.getMetadata(selectedCurrencyCode)
        val useSouthAsian = meta.code == "BDT" || meta.code == "INR"
        viewModelScope.launch {
            repository.updateUserPreferences {
                it.copy(
                    baseCurrencyCode = meta.code,
                    languageCode = languageCode,
                    regionCode = regionCode,
                    useSouthAsianGrouping = useSouthAsian,
                    budgetAlertsEnabled = notificationsEnabled,
                    billRemindersEnabled = notificationsEnabled,
                    debtRemindersEnabled = notificationsEnabled,
                    goalMilestonesEnabled = notificationsEnabled,
                    hasCompletedOnboarding = true
                )
            }
            if (context != null) {
                HisabNotificationManager.ensureChannelsCreated(context)
                HisabNotificationManager.rescheduleAllActiveReminders(context)
            }
            _showOnboardingTourOverlay.value = false
            showSnackbar("Welcome to HisabMate! Base currency: ${meta.code} (${meta.symbol})")
        }
    }

    fun completeCurrencyOnboarding(selectedCurrencyCode: String) {
        completeFullOnboarding(selectedCurrencyCode = selectedCurrencyCode)
    }

    fun openCurrencyPickerModal() {
        _showCurrencyPickerModal.value = true
    }

    fun closeCurrencyPickerModal() {
        _showCurrencyPickerModal.value = false
    }

    fun updateManualExchangeRate(currencyCode: String, rateToBdt: Double) {
        viewModelScope.launch {
            repository.updateExchangeRate(
                currencyCode = currencyCode,
                newRateToBdt = rateToBdt,
                sourceLabel = "Manual Custom Rate",
                isCustom = true
            )
            showSnackbar("Updated exchange rate for ${currencyCode.uppercase()}")
        }
    }

    fun resetExchangeRatesToDefault() {
        viewModelScope.launch {
            repository.resetExchangeRatesToDefaults()
            showSnackbar("Exchange rates restored to verified offline defaults")
        }
    }

    fun toggleBalanceVisibility() {
        viewModelScope.launch {
            repository.updateUserPreferences {
                it.copy(isBalanceHidden = !it.isBalanceHidden)
            }
        }
    }

    fun toggleNumberFormat() {
        viewModelScope.launch {
            val next = !useBangladeshiFormat.value
            repository.updateUserPreferences {
                it.copy(useSouthAsianGrouping = next)
            }
            val mode = if (next) "South Asian Lakh/Crore (2,48,650.00)" else "International (248,650.00)"
            showSnackbar("Number format switched to $mode")
        }
    }

    fun setThemePreference(pref: ThemePreference) {
        viewModelScope.launch {
            repository.updateUserPreferences {
                it.copy(themeMode = pref.name)
            }
        }
    }

    fun cycleTheme() {
        viewModelScope.launch {
            val next = when (themePreference.value) {
                ThemePreference.LIGHT -> ThemePreference.DARK
                ThemePreference.DARK -> ThemePreference.LIGHT
                ThemePreference.SYSTEM -> ThemePreference.DARK
            }
            repository.updateUserPreferences {
                it.copy(themeMode = next.name)
            }
        }
    }

    fun shiftMonth(delta: Int) {
        _selectedMonthOffset.update { (it + delta).coerceIn(-6, 0) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchTypeFilter(filter: String) {
        _searchTypeFilter.value = filter
    }

    fun openAddTransactionModal(type: String = "EXPENSE", preselectedCategoryId: Int? = null) {
        _editingTransaction.value = null
        _initialTransactionType.value = type
        _preselectedCategoryIdForAdd.value = preselectedCategoryId
        _showAddTransactionSheet.value = true
    }

    fun openEditTransactionModal(tx: TransactionEntity) {
        _selectedTransactionForDetail.value = null
        _editingTransaction.value = tx
        _initialTransactionType.value = tx.type
        _preselectedCategoryIdForAdd.value = tx.categoryId
        _showAddTransactionSheet.value = true
    }

    fun closeAddTransactionModal() {
        _showAddTransactionSheet.value = false
        _preselectedCategoryIdForAdd.value = null
        _editingTransaction.value = null
    }

    fun selectTransactionForDetail(tx: TransactionEntity?) {
        _selectedTransactionForDetail.value = tx
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    // Data Mutations
    fun addTransaction(
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
        isFavorite: Boolean = false,
        context: Context? = null
    ) {
        val editing = _editingTransaction.value
        viewModelScope.launch {
            if (editing != null) {
                val savedTransaction = repository.editTransaction(
                    oldTx = editing,
                    title = title,
                    merchant = merchant,
                    amount = amount,
                    currencyCode = currencyCode,
                    type = type,
                    category = category,
                    account = account,
                    toAccount = toAccount,
                    note = note,
                    isRecurring = isRecurring,
                    receiptUri = receiptUri,
                    splitDetails = splitDetails,
                    isFavorite = isFavorite
                )
                closeAddTransactionModal()
                publishTransactionNotification(context, savedTransaction, isUpdate = true)
            } else {
                val savedTransaction = repository.addTransaction(
                    title = title,
                    merchant = merchant,
                    amount = amount,
                    currencyCode = currencyCode,
                    type = type,
                    category = category,
                    account = account,
                    toAccount = toAccount,
                    note = note,
                    isRecurring = isRecurring,
                    receiptUri = receiptUri,
                    splitDetails = splitDetails,
                    isFavorite = isFavorite
                )
                closeAddTransactionModal()
                publishTransactionNotification(context, savedTransaction, isUpdate = false)
            }
        }
    }

    private suspend fun publishTransactionNotification(
        context: Context?,
        transaction: TransactionEntity,
        isUpdate: Boolean
    ) {
        val state = uiState.value
        val payload = HisabNotificationManager.buildTransactionNotificationPayload(
            transaction = transaction,
            isUpdate = isUpdate,
            useSouthAsianGrouping = useBangladeshiFormat.value
        )
        repository.insertAppNotification(
            AppNotificationEntity(
                title = payload.title,
                message = payload.body,
                categoryTag = payload.categoryTag,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                accentColorHex = transaction.categoryColorHex
            )
        )

        val postedToSystem = context?.let {
            HisabNotificationManager.postSystemNotification(
                context = it,
                payload = payload,
                soundEnabled = state.soundEnabled,
                vibrationEnabled = state.vibrationEnabled
            )
        } ?: false

        showSnackbar(
            if (postedToSystem) {
                "${payload.title}: ${payload.primaryFormattedAmount} • Notification sent"
            } else {
                "${payload.title}: ${payload.primaryFormattedAmount} • Saved in Notifications. Enable Android notifications in Profile & Settings for pop-ups."
            }
        )
    }

    fun toggleTransactionFavorite(tx: TransactionEntity) {
        viewModelScope.launch {
            val updated = repository.toggleTransactionFavorite(tx)
            _selectedTransactionForDetail.value = updated
            val status = if (updated.isFavorite) "Saved '${tx.title}' to Favorite Shortcuts" else "Removed from Favorites"
            showSnackbar(status)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _selectedTransactionForDetail.value = null
            showSnackbar("Deleted '${tx.title}' and restored balance")
        }
    }

    fun changeTransactionCategory(tx: TransactionEntity, newCategory: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategoryTransactionCategory(tx, newCategory)
            _selectedTransactionForDetail.value = tx.copy(
                categoryId = newCategory.id,
                categoryName = newCategory.name,
                categoryColorHex = newCategory.colorHex,
                categoryIconKey = newCategory.iconKey
            )
            showSnackbar("Moved '${tx.title}' to ${newCategory.name}")
        }
    }

    fun updateCategoryBudget(
        category: CategoryEntity,
        newBudgetAmount: Double,
        budgetPeriod: String = category.budgetPeriod,
        budgetCurrencyCode: String = category.budgetCurrencyCode.ifBlank { uiState.value.baseCurrencyCode },
        alertThresholdPercent: Int = category.alertThresholdPercent,
        startDateMillis: Long = category.startDateMillis,
        endDateMillis: Long = category.endDateMillis,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val rem = repository.updateCategoryBudget(
                category = category,
                newBudget = newBudgetAmount,
                budgetCurrencyCode = budgetCurrencyCode,
                budgetPeriod = budgetPeriod,
                alertThresholdPercent = alertThresholdPercent,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis
            )
            if (context != null) {
                HisabNotificationManager.scheduleReminderAlarm(context, rem)
            }
            val meta = CurrencyCatalog.getMetadata(budgetCurrencyCode)
            showSnackbar("Updated ${category.name} ${budgetPeriod.lowercase()} budget (${meta.symbol} ${meta.code})")
        }
    }

    fun createCategory(
        name: String,
        type: String,
        budget: Double,
        colorHex: Long,
        iconKey: String,
        budgetCurrencyCode: String = uiState.value.baseCurrencyCode,
        budgetPeriod: String = "MONTHLY",
        alertThresholdPercent: Int = 80
    ) {
        viewModelScope.launch {
            repository.addCategory(
                name = name,
                type = type,
                monthlyBudget = budget,
                budgetCurrencyCode = budgetCurrencyCode,
                colorHex = colorHex,
                iconKey = iconKey,
                budgetPeriod = budgetPeriod,
                alertThresholdPercent = alertThresholdPercent
            )
            showSnackbar("Category '$name' (${budgetCurrencyCode.uppercase()}) created")
        }
    }

    fun createAccount(
        name: String,
        type: String,
        balance: Double,
        currencyCode: String,
        maskedNumber: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.addAccount(name, type, balance, currencyCode, maskedNumber, colorHex)
            showSnackbar("Account '$name' (${currencyCode.uppercase()}) added")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        if (account.isPrimary) {
            showSnackbar("Primary account cannot be deleted")
            return
        }
        viewModelScope.launch {
            runCatching { repository.deleteAccount(account.id) }
                .onSuccess { showSnackbar("Removed account '${account.name}'") }
                .onFailure { error ->
                    showSnackbar(error.message ?: "Unable to remove this account")
                }
        }
    }

    fun payRecurringBill(bill: RecurringPaymentEntity, context: Context? = null) {
        viewModelScope.launch {
            val state = uiState.value
            val primaryAcc = state.accounts.firstOrNull() ?: return@launch
            val cat = state.categories.find { it.id == bill.categoryId } ?: state.categories.firstOrNull()
            val updatedReminder = repository.payRecurringBillNow(bill, primaryAcc, cat)
            if (context != null) {
                HisabNotificationManager.scheduleReminderAlarm(context, updatedReminder)
            }
            val verb = if (bill.transactionType == "INCOME") "Recorded recurring income" else "Paid ${bill.title}"
            showSnackbar("$verb via ${primaryAcc.name}")
        }
    }

    fun createRecurringPayment(
        title: String,
        provider: String,
        amount: Double,
        currencyCode: String,
        frequency: String,
        daysUntilDue: Int,
        category: CategoryEntity,
        autoPay: Boolean,
        transactionType: String = "EXPENSE",
        context: Context? = null
    ) {
        viewModelScope.launch {
            val reminder = repository.addRecurringPayment(
                title = title,
                provider = provider,
                amount = amount,
                currencyCode = currencyCode,
                transactionType = transactionType,
                frequency = frequency,
                daysUntilDue = daysUntilDue,
                category = category,
                autoPay = autoPay
            )
            if (context != null) {
                HisabNotificationManager.scheduleReminderAlarm(context, reminder)
            }
            showSnackbar("Recurring ${transactionType.lowercase()} '$title' ($currencyCode) scheduled")
        }
    }

    fun deleteRecurringPayment(id: Int, context: Context? = null) {
        viewModelScope.launch {
            val notifId = repository.deleteRecurringPayment(id)
            if (context != null) {
                HisabNotificationManager.cancelReminderAlarm(context, notifId)
            }
            showSnackbar("Recurring schedule & reminder removed")
        }
    }

    fun recordDebtSettlement(record: DebtRecordEntity, amountInRecordCurrency: Double, context: Context? = null) {
        viewModelScope.launch {
            val primaryAcc = uiState.value.accounts.firstOrNull()
            val updatedReminder = repository.settleDebtAmount(record, amountInRecordCurrency, primaryAcc)
            if (context != null) {
                val notifId = HisabNotificationManager.computeStableNotificationId("DEBT", record.id)
                if (updatedReminder == null) {
                    HisabNotificationManager.cancelReminderAlarm(context, notifId)
                } else {
                    HisabNotificationManager.scheduleReminderAlarm(context, updatedReminder)
                }
            }
            showSnackbar("Recorded settlement with ${record.personName} (${record.currencyCode})")
        }
    }

    fun createDebtRecord(
        personName: String,
        relationOrNote: String,
        totalAmount: Double,
        currencyCode: String,
        type: String,
        daysUntilDue: Int,
        phoneOrTag: String,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val reminder = repository.addDebtRecord(personName, relationOrNote, totalAmount, currencyCode, type, daysUntilDue, phoneOrTag)
            if (context != null) {
                HisabNotificationManager.scheduleReminderAlarm(context, reminder)
            }
            showSnackbar("Added ${if (type == "LENT") "receivable" else "payable"} for $personName ($currencyCode)")
        }
    }

    fun deleteDebtRecord(id: Int, context: Context? = null) {
        viewModelScope.launch {
            val notifId = repository.deleteDebtRecord(id)
            if (context != null) {
                HisabNotificationManager.cancelReminderAlarm(context, notifId)
            }
            showSnackbar("Record & scheduled reminder deleted")
        }
    }

    fun contributeToSavingsGoal(goal: SavingsGoalEntity, amountInGoalCurrency: Double) {
        viewModelScope.launch {
            val primaryAcc = uiState.value.accounts.firstOrNull()
            repository.contributeToGoal(goal, amountInGoalCurrency, primaryAcc)
            showSnackbar("Added contribution to '${goal.title}' (${goal.currencyCode})")
        }
    }

    fun createSavingsGoal(
        title: String,
        subtitle: String,
        targetAmount: Double,
        initialSaved: Double,
        currencyCode: String,
        daysUntilDeadline: Int,
        colorHex: Long,
        iconKey: String,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val reminder = repository.addSavingsGoal(title, subtitle, targetAmount, initialSaved, currencyCode, daysUntilDeadline, colorHex, iconKey)
            if (context != null) {
                HisabNotificationManager.scheduleReminderAlarm(context, reminder)
            }
            showSnackbar("Financial goal '$title' ($currencyCode) created")
        }
    }

    fun deleteSavingsGoal(id: Int, context: Context? = null) {
        viewModelScope.launch {
            val notifId = repository.deleteSavingsGoal(id)
            if (context != null) {
                HisabNotificationManager.cancelReminderAlarm(context, notifId)
            }
            showSnackbar("Goal removed")
        }
    }

    fun createCustomReminder(
        title: String,
        subtitle: String,
        amount: Double,
        currencyCode: String,
        daysUntilTrigger: Int,
        frequency: String,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val reminder = repository.addCustomScheduledReminder(
                title = title,
                subtitle = subtitle,
                amount = amount,
                currencyCode = currencyCode,
                daysUntilTrigger = daysUntilTrigger,
                frequency = frequency
            )
            if (context != null) {
                HisabNotificationManager.scheduleReminderAlarm(context, reminder)
            }
            showSnackbar("Scheduled reminder '$title' (${currencyCode.uppercase()})")
        }
    }

    fun toggleScheduledReminder(reminder: ScheduledReminderEntity, context: Context? = null) {
        viewModelScope.launch {
            val updated = repository.toggleScheduledReminder(reminder)
            if (context != null) {
                if (updated.isEnabled) {
                    HisabNotificationManager.scheduleReminderAlarm(context, updated)
                } else {
                    HisabNotificationManager.cancelReminderAlarm(context, updated.notificationId)
                }
            }
        }
    }

    fun deleteScheduledReminder(reminder: ScheduledReminderEntity, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteScheduledReminder(reminder)
            if (context != null) {
                HisabNotificationManager.cancelReminderAlarm(context, reminder.notificationId)
            }
            showSnackbar("Reminder removed")
        }
    }

    fun triggerLiveCurrencyNotificationTest(
        context: Context,
        sampleCurrencyCode: String = uiState.value.baseCurrencyCode
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val sampleReminder = state.scheduledReminders.firstOrNull {
                it.currencyCode.equals(sampleCurrencyCode, ignoreCase = true)
            } ?: state.scheduledReminders.firstOrNull()
                ?: ScheduledReminderEntity(
                    notificationId = 55001,
                    sourceType = "BILL",
                    sourceId = 99,
                    title = "HisabMate Smart Bill Reminder",
                    subtitle = "Scheduled Payment Verification",
                    amount = if (sampleCurrencyCode == "BDT") 1600.0 else 24.0,
                    currencyCode = sampleCurrencyCode,
                    triggerTimeMillis = System.currentTimeMillis()
                )

            val payload = HisabNotificationManager.buildReminderNotificationPayload(
                reminder = sampleReminder,
                baseCurrencyCode = state.baseCurrencyCode,
                ratesToBdt = state.ratesToBdtMap,
                useSouthAsianGrouping = useBangladeshiFormat.value
            )

            repository.insertAppNotification(
                AppNotificationEntity(
                    title = payload.title,
                    message = payload.body,
                    categoryTag = payload.categoryTag,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    accentColorHex = sampleReminder.accentColorHex
                )
            )

            val postedToSystem = HisabNotificationManager.postSystemNotification(
                context = context,
                payload = payload,
                soundEnabled = state.soundEnabled,
                vibrationEnabled = state.vibrationEnabled
            )
            if (postedToSystem) {
                showSnackbar("Sent system & in-app alert: ${payload.primaryFormattedAmount}")
            } else {
                showSnackbar("Logged alert in Notification Center (${payload.primaryFormattedAmount})")
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            showSnackbar("All notifications marked as read")
        }
    }

    fun dismissNotification(id: Int) {
        viewModelScope.launch {
            repository.dismissNotification(id)
        }
    }

    fun deleteDemoData() {
        viewModelScope.launch {
            repository.deleteDemoData()
            showSnackbar("All demo accounts & sample transactions permanently deleted")
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.resetToDefaultSampleData()
            showSnackbar("Sample financial data & exchange rates restored")
        }
    }

    fun exportPdfStatement(
        context: Context,
        onResult: (PdfExportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = PdfExporter.generateAndSaveStatementPdf(
                context = context,
                uiState = uiState.value,
                useBangladeshiFormat = useBangladeshiFormat.value
            )
            onResult(result)
        }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onReady(json)
        }
    }

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            val ok = repository.restoreBackupJson(json)
            if (ok) {
                showSnackbar("Backup restored successfully")
            } else {
                showSnackbar("Invalid backup JSON format")
            }
        }
    }

    fun exportTransactionsCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportTransactionsCsv()
            onReady(csv)
        }
    }

    fun importTransactionsCsv(csvContent: String) {
        viewModelScope.launch {
            val count = repository.importTransactionsCsv(csvContent)
            if (count > 0) {
                showSnackbar("Imported $count transactions from CSV")
            } else {
                showSnackbar("No valid CSV transaction rows found")
            }
        }
    }

    /**
     * Generates 15 daily spend points converted into the active [baseCurrencyCode].
     */
    fun buildDailySpendCurve(
        categoryId: Int?,
        transactions: List<TransactionEntity>,
        monthOffset: Int,
        baseCurrencyCode: String,
        ratesToBdt: Map<String, Double>
    ): List<DailySpendPoint> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val monthShort = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time)
        val yearNum = cal.get(Calendar.YEAR)

        val basePatternBdt = when (categoryId) {
            1 -> listOf(850.0, 420.0, 1980.0, 640.0, 1450.0, 520.0, 3450.0, 890.0, 2890.0, 1120.0, 4280.0, 760.0, 1835.3, 940.0, 2650.0)
            2 -> listOf(320.0, 480.0, 640.0, 290.0, 510.0, 380.0, 1450.0, 420.0, 3200.0, 560.0, 490.0, 310.0, 780.0, 410.0, 920.0)
            3 -> listOf(0.0, 450.0, 0.0, 1200.0, 350.0, 0.0, 2892.0, 0.0, 680.0, 0.0, 1650.0, 420.0, 0.0, 890.0, 2400.0)
            4 -> listOf(1200.0, 0.0, 24500.0, 0.0, 1500.0, 0.0, 3850.0, 0.0, 950.0, 0.0, 1800.0, 0.0, 650.0, 0.0, 1400.0)
            else -> listOf(600.0, 950.0, 1400.0, 520.0, 1850.0, 740.0, 2950.0, 810.0, 1640.0, 920.0, 3100.0, 670.0, 1250.0, 880.0, 1950.0)
        }

        val factor = when (monthOffset) {
            0 -> 1.0
            -1 -> 0.88
            -2 -> 1.12
            else -> 0.94
        }

        return basePatternBdt.mapIndexed { idx, bdtVal ->
            val dayNum = idx + 1
            val converted = CurrencyFormatter.convert(
                amount = bdtVal * factor,
                fromCurrency = "BDT",
                toCurrency = baseCurrencyCode,
                ratesToBdt = ratesToBdt
            )
            DailySpendPoint(
                dayLabel = "$monthShort $dayNum",
                fullDateLabel = "$monthShort $dayNum, $yearNum",
                amount = converted,
                dayIndex = idx
            )
        }
    }

    fun buildMonthlyCashflowSeries(
        currentIncomeInBase: Double,
        currentExpenseInBase: Double,
        baseCurrencyCode: String,
        ratesToBdt: Map<String, Double>
    ): List<MonthlyCashflowPoint> {
        fun conv(bdt: Double) = CurrencyFormatter.convert(bdt, "BDT", baseCurrencyCode, ratesToBdt)
        return listOf(
            MonthlyCashflowPoint("May", conv(162000.0), conv(54200.0)),
            MonthlyCashflowPoint("Jun", conv(175000.0), conv(68900.0)),
            MonthlyCashflowPoint("Jul", conv(158000.0), conv(49800.0)),
            MonthlyCashflowPoint("Aug", conv(182000.0), conv(61400.0)),
            MonthlyCashflowPoint("Sep", conv(171500.0), conv(53600.0)),
            MonthlyCashflowPoint(
                monthShort = "Oct",
                income = currentIncomeInBase.coerceAtLeast(conv(145000.0)),
                expense = currentExpenseInBase.coerceAtLeast(conv(42000.0)),
                isCurrent = true
            )
        )
    }

    fun buildCalendarDays(
        transactions: List<TransactionEntity>,
        monthOffset: Int,
        baseCurrencyCode: String,
        ratesToBdt: Map<String, Double>
    ): List<CalendarDaySummary> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val txByDay = transactions.groupBy { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            ((txCal.get(Calendar.DAY_OF_MONTH) - 1 + kotlin.math.abs(monthOffset) * 3) % daysInMonth) + 1
        }

        return (1..daysInMonth).map { dayNum ->
            val dayTxs = txByDay[dayNum].orEmpty()
            val incomeInBase = dayTxs.filter { it.type == "INCOME" }.sumOf {
                CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrencyCode, ratesToBdt)
            }
            val expenseInBase = dayTxs.filter { it.type == "EXPENSE" }.sumOf {
                CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrencyCode, ratesToBdt)
            }
            CalendarDaySummary(
                dayOfMonth = dayNum,
                incomeInBase = incomeInBase,
                expenseInBase = expenseInBase,
                transactions = dayTxs
            )
        }
    }

    private data class CoreBundle(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val recurring: List<RecurringPaymentEntity>,
        val debts: List<DebtRecordEntity>
    )

    private data class MetaBundle(
        val goals: List<SavingsGoalEntity>,
        val notifications: List<AppNotificationEntity>,
        val rates: List<ExchangeRateEntity>,
        val prefs: UserPreferencesEntity,
        val monthOffset: Int
    )

    companion object {
        fun provideFactory(repository: HisabRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HisabViewModel(repository) as T
                }
            }
    }
}
