package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CategoryEntity
import com.example.data.local.HisabDatabase
import com.example.data.repository.HisabRepository
import com.example.ui.components.FloatingFintechDock
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddTransactionBottomSheet
import com.example.ui.screens.CurrencyManagementScreen
import com.example.ui.screens.CurrencyPickerBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HisabOnboardingFlowScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PinLockOverlayScreen
import com.example.ui.screens.PlanningHubScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.SearchAndFilterScreen
import com.example.ui.screens.SpendAnalysisScreen
import com.example.ui.screens.TransactionCalendarScreen
import com.example.ui.screens.TransactionDetailBottomSheet
import com.example.ui.screens.WalletDetailsScreen
import com.example.ui.theme.HisabMateTheme
import com.example.ui.theme.HisabTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HisabViewModel
import com.example.ui.viewmodel.ThemePreference
import com.example.util.CurrencyFormatter
import com.example.util.HisabNotificationManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HisabNotificationManager.ensureChannelsCreated(this)
        enableEdgeToEdge()
        setContent {
            HisabMateAppRoot()
        }
    }
}

@Composable
fun HisabMateAppRoot() {
    val context = LocalContext.current
    val repository = remember {
        val db = HisabDatabase.getInstance(context)
        HisabRepository(db.hisabDao())
    }
    val viewModel: HisabViewModel = viewModel(
        factory = HisabViewModel.provideFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedPlanningTab by viewModel.selectedPlanningTab.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedMonthOffset by viewModel.selectedMonthOffset.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val useBangladeshiFormat by viewModel.useBangladeshiFormat.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchTypeFilter by viewModel.searchTypeFilter.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val showAddSheet by viewModel.showAddTransactionSheet.collectAsStateWithLifecycle()
    val initialTxType by viewModel.initialTransactionType.collectAsStateWithLifecycle()
    val preselectedCatId by viewModel.preselectedCategoryIdForAdd.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransactionForDetail.collectAsStateWithLifecycle()
    val showCurrencyPickerModal by viewModel.showCurrencyPickerModal.collectAsStateWithLifecycle()
    val showOnboardingTourOverlay by viewModel.showOnboardingTourOverlay.collectAsStateWithLifecycle()
    val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.scheduledReminders.size) {
        if (uiState.scheduledReminders.isNotEmpty()) {
            HisabNotificationManager.rescheduleAllActiveReminders(context)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> systemDark
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateBack()
    }

    HisabMateTheme(darkTheme = isDarkTheme) {
        val colors = HisabTheme.colors

        Scaffold(
            containerColor = colors.canvasBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 90.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.canvasBackground)
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(180))
                    },
                    label = "screenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                uiState = uiState,
                                isBalanceHidden = isBalanceHidden,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isDarkTheme = isDarkTheme,
                                onToggleBalance = viewModel::toggleBalanceVisibility,
                                onCycleTheme = viewModel::cycleTheme,
                                onOpenCurrencyPicker = viewModel::openCurrencyPickerModal,
                                onNavigate = viewModel::navigateTo,
                                onOpenPlanningTab = viewModel::openPlanningWithTab,
                                onOpenWalletDetails = viewModel::openWalletDetails,
                                onOpenAddTransaction = viewModel::openAddTransactionModal,
                                onSelectTransaction = viewModel::selectTransactionForDetail
                            )
                        }

                        AppScreen.WALLET_DETAILS -> {
                            val activeCat = uiState.categories.find { it.id == selectedCategoryId }
                                ?: uiState.categories.firstOrNull()
                                ?: CategoryEntity(
                                    id = 1,
                                    name = "Groceries",
                                    type = "EXPENSE",
                                    monthlyBudget = 18000.0,
                                    budgetCurrencyCode = "BDT",
                                    colorHex = 0xFF6D4AFFL,
                                    iconKey = "GROCERIES"
                                )
                            val catSummary = uiState.categorySummaries.find { it.category.id == activeCat.id }
                            val catTransactions = uiState.transactions.filter { it.categoryId == activeCat.id }
                            val dailyCurve = remember(
                                activeCat.id,
                                uiState.transactions,
                                selectedMonthOffset,
                                uiState.baseCurrencyCode,
                                uiState.ratesToBdtMap
                            ) {
                                viewModel.buildDailySpendCurve(
                                    categoryId = activeCat.id,
                                    transactions = catTransactions,
                                    monthOffset = selectedMonthOffset,
                                    baseCurrencyCode = uiState.baseCurrencyCode,
                                    ratesToBdt = uiState.ratesToBdtMap
                                )
                            }
                            val spentInBase = catSummary?.totalSpentInBase ?: catTransactions.sumOf {
                                CurrencyFormatter.convert(it.amount, it.currencyCode, uiState.baseCurrencyCode, uiState.ratesToBdtMap)
                            }
                            val budgetInBase = catSummary?.budgetLimitInBase ?: CurrencyFormatter.convert(
                                activeCat.monthlyBudget,
                                activeCat.budgetCurrencyCode,
                                uiState.baseCurrencyCode,
                                uiState.ratesToBdtMap
                            )

                            WalletDetailsScreen(
                                category = activeCat,
                                categorySpentInBase = spentInBase,
                                budgetLimitInBase = budgetInBase,
                                baseCurrencyCode = uiState.baseCurrencyCode,
                                ratesToBdtMap = uiState.ratesToBdtMap,
                                transactions = catTransactions,
                                dailyPoints = dailyCurve,
                                monthOffset = selectedMonthOffset,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onBack = { viewModel.navigateBack() },
                                onShiftMonth = viewModel::shiftMonth,
                                onOpenAddForCategory = { catId ->
                                    viewModel.openAddTransactionModal("EXPENSE", catId)
                                },
                                onOpenTransfer = {
                                    viewModel.openAddTransactionModal("TRANSFER", null)
                                },
                                onUpdateBudget = { cat, amt, period, curr, threshold, startMs, endMs ->
                                    viewModel.updateCategoryBudget(
                                        category = cat,
                                        newBudgetAmount = amt,
                                        budgetPeriod = period,
                                        budgetCurrencyCode = curr,
                                        alertThresholdPercent = threshold,
                                        startDateMillis = startMs,
                                        endDateMillis = endMs,
                                        context = context
                                    )
                                },
                                onSelectTransaction = viewModel::selectTransactionForDetail
                            )
                        }

                        AppScreen.SPEND_ANALYSIS -> {
                            val cashflowPoints = remember(
                                uiState.monthlyIncome,
                                uiState.monthlyExpense,
                                uiState.baseCurrencyCode,
                                uiState.ratesToBdtMap
                            ) {
                                viewModel.buildMonthlyCashflowSeries(
                                    currentIncomeInBase = uiState.monthlyIncome,
                                    currentExpenseInBase = uiState.monthlyExpense,
                                    baseCurrencyCode = uiState.baseCurrencyCode,
                                    ratesToBdt = uiState.ratesToBdtMap
                                )
                            }
                            SpendAnalysisScreen(
                                uiState = uiState,
                                cashflowPoints = cashflowPoints,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onBack = { viewModel.navigateBack() },
                                onSelectReportPeriod = viewModel::selectReportPeriod,
                                onOpenCalendar = { viewModel.navigateTo(AppScreen.CALENDAR) },
                                onOpenCurrencyPicker = viewModel::openCurrencyPickerModal,
                                onOpenWalletDetails = viewModel::openWalletDetails,
                                onSelectTransaction = viewModel::selectTransactionForDetail
                            )
                        }

                        AppScreen.PLANNING_HUB -> {
                            PlanningHubScreen(
                                uiState = uiState,
                                selectedTab = selectedPlanningTab,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onSelectTab = viewModel::selectPlanningTab,
                                onBack = { viewModel.navigateBack() },
                                onOpenWalletDetails = viewModel::openWalletDetails,
                                onUpdateBudget = { cat, amt, period, curr, threshold, startMs, endMs ->
                                    viewModel.updateCategoryBudget(
                                        category = cat,
                                        newBudgetAmount = amt,
                                        budgetPeriod = period,
                                        budgetCurrencyCode = curr,
                                        alertThresholdPercent = threshold,
                                        startDateMillis = startMs,
                                        endDateMillis = endMs,
                                        context = context
                                    )
                                },
                                onCreateCategory = { name, type, budget, colorHex, iconKey, curr, period, threshold ->
                                    viewModel.createCategory(
                                        name = name,
                                        type = type,
                                        budget = budget,
                                        colorHex = colorHex,
                                        iconKey = iconKey,
                                        budgetCurrencyCode = curr,
                                        budgetPeriod = period,
                                        alertThresholdPercent = threshold
                                    )
                                },
                                onPayRecurring = viewModel::payRecurringBill,
                                onCreateRecurring = viewModel::createRecurringPayment,
                                onDeleteRecurring = viewModel::deleteRecurringPayment,
                                onSettleDebt = viewModel::recordDebtSettlement,
                                onCreateDebt = viewModel::createDebtRecord,
                                onDeleteDebt = viewModel::deleteDebtRecord,
                                onContributeGoal = viewModel::contributeToSavingsGoal,
                                onCreateGoal = viewModel::createSavingsGoal,
                                onDeleteGoal = viewModel::deleteSavingsGoal
                            )
                        }

                        AppScreen.ACCOUNTS -> {
                            AccountsScreen(
                                uiState = uiState,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onBack = { viewModel.navigateBack() },
                                onOpenTransfer = { viewModel.openAddTransactionModal("TRANSFER", null) },
                                onOpenSettings = { viewModel.navigateTo(AppScreen.PROFILE_SETTINGS) },
                                onOpenCurrencyManager = { viewModel.navigateTo(AppScreen.CURRENCY_MANAGER) },
                                onCreateAccount = viewModel::createAccount,
                                onSelectTransaction = viewModel::selectTransactionForDetail
                            )
                        }

                        AppScreen.SEARCH -> {
                            SearchAndFilterScreen(
                                transactions = uiState.transactions,
                                baseCurrencyCode = uiState.baseCurrencyCode,
                                ratesToBdtMap = uiState.ratesToBdtMap,
                                searchQuery = searchQuery,
                                selectedTypeFilter = searchTypeFilter,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onQueryChange = viewModel::updateSearchQuery,
                                onTypeFilterChange = viewModel::updateSearchTypeFilter,
                                onSelectTransaction = viewModel::selectTransactionForDetail,
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.NOTIFICATIONS -> {
                            NotificationsScreen(
                                notifications = uiState.notifications,
                                scheduledReminders = uiState.scheduledReminders,
                                baseCurrencyCode = uiState.baseCurrencyCode,
                                ratesToBdtMap = uiState.ratesToBdtMap,
                                useBangladeshiFormat = useBangladeshiFormat,
                                onMarkAllRead = viewModel::markAllNotificationsRead,
                                onDismiss = viewModel::dismissNotification,
                                onToggleReminder = { reminder, _ ->
                                    viewModel.toggleScheduledReminder(reminder, context)
                                },
                                onDeleteReminder = { reminder ->
                                    viewModel.deleteScheduledReminder(reminder, context)
                                },
                                onCreateCustomReminder = { title, sub, amt, curr, days ->
                                    viewModel.createCustomReminder(
                                        title = title,
                                        subtitle = sub,
                                        amount = amt,
                                        currencyCode = curr,
                                        daysUntilTrigger = days,
                                        frequency = "ONCE",
                                        context = context
                                    )
                                },
                                onTriggerLiveTestNotification = { reminder ->
                                    viewModel.triggerLiveCurrencyNotificationTest(context, reminder.currencyCode)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.PROFILE_SETTINGS -> {
                            ProfileAndSettingsScreen(
                                uiState = uiState,
                                baseCurrencyCode = uiState.baseCurrencyCode,
                                themePreference = themePreference,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onOpenCurrencyPicker = viewModel::openCurrencyPickerModal,
                                onOpenCurrencyManager = { viewModel.navigateTo(AppScreen.CURRENCY_MANAGER) },
                                onSetTheme = viewModel::setThemePreference,
                                onToggleNumberFormat = viewModel::toggleNumberFormat,
                                onToggleBalanceHidden = viewModel::toggleBalanceVisibility,
                                onConfigureAppLock = viewModel::configureAppLock,
                                onLockNow = viewModel::lockSessionNow,
                                onUpdateNotificationPrefs = viewModel::updateNotificationPrefs,
                                onUpdateFullNotificationAndHapticPrefs = { budget, bill, debt, goal, sound, vib, quiet, leadDays, haptics ->
                                    viewModel.updateFullNotificationAndHapticPrefs(
                                        context = context,
                                        budgetAlerts = budget,
                                        billReminders = bill,
                                        debtReminders = debt,
                                        goalMilestones = goal,
                                        soundEnabled = sound,
                                        vibrationEnabled = vib,
                                        quietHoursEnabled = quiet,
                                        leadTimeDays = leadDays,
                                        hapticsEnabled = haptics
                                    )
                                },
                                onTriggerLiveCurrencyTest = {
                                    viewModel.triggerLiveCurrencyNotificationTest(context)
                                },
                                onOpenOnboardingTour = viewModel::openOnboardingTour,
                                onGenerateBackupJson = viewModel::exportBackupJson,
                                onRestoreBackupJson = viewModel::restoreBackupJson,
                                onGenerateCsv = viewModel::exportTransactionsCsv,
                                onImportCsv = viewModel::importTransactionsCsv,
                                onExportSummary = {
                                    val netStr = CurrencyFormatter.formatCurrency(
                                        amount = uiState.totalBalance,
                                        currencyCode = uiState.baseCurrencyCode,
                                        useSouthAsianGrouping = useBangladeshiFormat,
                                        includeCodeSuffix = true
                                    )
                                    viewModel.showSnackbar("PDF Statement exported: Net Liquidity $netStr (${uiState.transactions.size} entries)")
                                },
                                onResetData = viewModel::resetSampleData,
                                onDeleteDemoData = viewModel::deleteDemoData,
                                onNavigate = viewModel::navigateTo,
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.CURRENCY_MANAGER -> {
                            CurrencyManagementScreen(
                                uiState = uiState,
                                useBangladeshiFormat = useBangladeshiFormat,
                                onSelectBaseCurrency = viewModel::setBaseCurrency,
                                onUpdateManualRate = viewModel::updateManualExchangeRate,
                                onResetRates = viewModel::resetExchangeRatesToDefault,
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.CALENDAR -> {
                            val calendarDays = remember(
                                uiState.transactions,
                                selectedMonthOffset,
                                uiState.baseCurrencyCode,
                                uiState.ratesToBdtMap
                            ) {
                                viewModel.buildCalendarDays(
                                    transactions = uiState.transactions,
                                    monthOffset = selectedMonthOffset,
                                    baseCurrencyCode = uiState.baseCurrencyCode,
                                    ratesToBdt = uiState.ratesToBdtMap
                                )
                            }
                            TransactionCalendarScreen(
                                calendarDays = calendarDays,
                                monthOffset = selectedMonthOffset,
                                baseCurrencyCode = uiState.baseCurrencyCode,
                                ratesToBdtMap = uiState.ratesToBdtMap,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                onBack = { viewModel.navigateBack() },
                                onShiftMonth = viewModel::shiftMonth,
                                onOpenAddTransaction = { viewModel.openAddTransactionModal("EXPENSE", null) },
                                onSelectTransaction = viewModel::selectTransactionForDetail
                            )
                        }
                    }
                }

                // Floating Dark Pill Dock on all primary screens (except Wallet Details which has its own 3-pill dock)
                if (currentScreen != AppScreen.WALLET_DETAILS) {
                    FloatingFintechDock(
                        currentScreen = currentScreen,
                        onSelectScreen = viewModel::navigateTo,
                        onAddClick = { viewModel.openAddTransactionModal("EXPENSE", null) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // PIN & Biometric Security Lock Overlay when enabled and locked
                if (uiState.isAppLockEnabled && uiState.appPinCode.isNotBlank() && !isSessionUnlocked) {
                    PinLockOverlayScreen(
                        biometricEnabled = uiState.biometricEnabled,
                        onSubmitPin = { entered ->
                            if (entered == uiState.appPinCode) {
                                viewModel.unlockSession()
                                viewModel.showSnackbar("Unlocked HisabMate")
                                true
                            } else {
                                false
                            }
                        },
                        onBiometricUnlock = {
                            viewModel.unlockSession()
                            viewModel.showSnackbar("Biometric verification succeeded")
                        }
                    )
                }
            }

            if (showAddSheet) {
                AddTransactionBottomSheet(
                    initialType = initialTxType,
                    preselectedCategoryId = preselectedCatId,
                    editingTransaction = editingTransaction,
                    favoriteTransactions = uiState.favoriteTransactions,
                    recentTransactions = uiState.transactions,
                    baseCurrencyCode = uiState.baseCurrencyCode,
                    ratesToBdtMap = uiState.ratesToBdtMap,
                    categories = uiState.categories,
                    accounts = uiState.accounts,
                    onDismiss = viewModel::closeAddTransactionModal,
                    onSaveTransaction = viewModel::addTransaction
                )
            }

            selectedTransaction?.let { tx ->
                TransactionDetailBottomSheet(
                    transaction = tx,
                    baseCurrencyCode = uiState.baseCurrencyCode,
                    ratesToBdtMap = uiState.ratesToBdtMap,
                    categories = uiState.categories,
                    useBangladeshiFormat = useBangladeshiFormat,
                    onDismiss = { viewModel.selectTransactionForDetail(null) },
                    onEditTransaction = viewModel::openEditTransactionModal,
                    onToggleFavorite = viewModel::toggleTransactionFavorite,
                    onReassignCategory = viewModel::changeTransactionCategory,
                    onDeleteTransaction = viewModel::deleteTransaction
                )
            }

            if (showCurrencyPickerModal) {
                CurrencyPickerBottomSheet(
                    selectedCurrencyCode = uiState.baseCurrencyCode,
                    ratesToBdtMap = uiState.ratesToBdtMap,
                    title = "Select Base Currency",
                    subtitle = "All dashboard summaries, budgets, and reports convert into your chosen base currency.",
                    onSelectCurrency = viewModel::setBaseCurrency,
                    onDismiss = viewModel::closeCurrencyPickerModal
                )
            }

            if ((!uiState.isLoading && !uiState.hasCompletedOnboarding) || showOnboardingTourOverlay) {
                HisabOnboardingFlowScreen(
                    currentBaseCurrency = uiState.baseCurrencyCode,
                    onCompleteOnboarding = { baseCurr, lang, region, enableAlerts ->
                        viewModel.completeFullOnboarding(
                            selectedCurrencyCode = baseCurr,
                            languageCode = lang,
                            regionCode = region,
                            notificationsEnabled = enableAlerts,
                            context = context
                        )
                    }
                )
            }
        }
    }
}
