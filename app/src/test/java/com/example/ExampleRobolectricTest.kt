package com.example

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HisabDatabase
import com.example.data.local.SeedDataGenerator
import com.example.data.repository.HisabRepository
import com.example.ui.components.FloatingFintechDock
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CinematicHisabOnboardingFlow
import com.example.ui.screens.CurrencyManagementScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PlanningHubScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.SpendAnalysisScreen
import com.example.ui.screens.WalletDetailsScreen
import com.example.ui.theme.HisabMateTheme
import com.example.ui.theme.HisabTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.CategorySpendSummary
import com.example.ui.viewmodel.DailySpendPoint
import com.example.ui.viewmodel.HisabUiState
import com.example.ui.viewmodel.MonthlyCashflowPoint
import com.example.ui.viewmodel.PlanningTab
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.ThemePreference
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabNotificationManager
import com.example.util.PdfExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: HisabDatabase
    private lateinit var repository: HisabRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HisabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HisabRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("HisabMate", appName)
    }

    @Test
    fun `notification channels are registered on initialization`() {
        HisabNotificationManager.ensureChannelsCreated(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val billChannel = nm.getNotificationChannel(HisabNotificationManager.CHANNEL_BILL_REMINDERS)
        val budgetChannel = nm.getNotificationChannel(HisabNotificationManager.CHANNEL_BUDGET_ALERTS)
        val debtChannel = nm.getNotificationChannel(HisabNotificationManager.CHANNEL_DEBT_REMINDERS)
        val goalChannel = nm.getNotificationChannel(HisabNotificationManager.CHANNEL_GOAL_MILESTONES)
        assertNotNull(billChannel)
        assertNotNull(budgetChannel)
        assertNotNull(debtChannel)
        assertNotNull(goalChannel)
    }

    @Test
    fun `multi-currency formatter formats authentic ISO 4217 symbols and converts accurately`() {
        val rates = CurrencyCatalog.supportedCurrencies.associate { it.code to it.defaultRateToBdt }

        val bdtStr = CurrencyFormatter.formatCurrency(125400.50, "BDT", useSouthAsianGrouping = true)
        assertTrue(bdtStr.startsWith("৳"))

        val usdStr = CurrencyFormatter.formatCurrency(1245.30, "USD", useSouthAsianGrouping = false)
        assertTrue(usdStr.startsWith("$"))

        val eurStr = CurrencyFormatter.formatCurrency(890.00, "EUR", useSouthAsianGrouping = false)
        assertTrue(eurStr.startsWith("€"))

        val jpyStr = CurrencyFormatter.formatCurrency(15800.0, "JPY", useSouthAsianGrouping = false)
        assertEquals("¥15,800", jpyStr)

        // 100 USD at 120.50 BDT/USD = 12,050 BDT
        val convertedToBdt = CurrencyFormatter.convert(100.0, "USD", "BDT", rates)
        assertEquals(12050.0, convertedToBdt, 0.01)

        // Round-trip conversion back to USD
        val roundTripUsd = CurrencyFormatter.convert(convertedToBdt, "BDT", "USD", rates)
        assertEquals(100.0, roundTripUsd, 0.01)
    }

    @Test
    fun `onboarding step 3 test ocr button and step 5 savings percentage render horizontally without vertical wrapping`() {
        composeTestRule.setContent {
            HisabMateTheme(darkTheme = false) {
                CinematicHisabOnboardingFlow(
                    currentBaseCurrency = "BDT",
                    onCompleteOnboarding = { _, _, _, _ -> }
                )
            }
        }

        // Navigate to Step 3 (Flows & Smart OCR)
        composeTestRule.onNodeWithTag("onboarding_step_indicator_2").performClick()
        composeTestRule.waitForIdle()

        val testOcrNode = composeTestRule.onNodeWithTag("btn_test_ocr")
        testOcrNode.performScrollTo()
        testOcrNode.assertIsDisplayed()
        val ocrSize = testOcrNode.fetchSemanticsNode().size
        assertTrue(
            "Expected Test OCR button width (${ocrSize.width}) to be greater than height (${ocrSize.height})",
            ocrSize.width > ocrSize.height * 1.5f
        )

        // Click Test OCR and verify receipt switches horizontally
        testOcrNode.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Amazon Web Services EMEA").performScrollTo().assertIsDisplayed()

        // Navigate to Step 5 (Goals & Debt Ledger)
        composeTestRule.onNodeWithTag("onboarding_step_indicator_4").performClick()
        composeTestRule.waitForIdle()

        val goalPercentNode = composeTestRule.onNodeWithTag("txt_savings_goal_percentage")
        goalPercentNode.performScrollTo()
        goalPercentNode.assertIsDisplayed()
        goalPercentNode.assertTextEquals("72%")
        val percentSize = goalPercentNode.fetchSemanticsNode().size
        assertTrue(
            "Expected 72% badge width (${percentSize.width}) to be greater than height (${percentSize.height})",
            percentSize.width > percentSize.height
        )
    }

    @Test
    fun `accounts budgets recurring and scheduled reminders persist with native currencies`() = runBlocking {
        repository.ensureSeeded()

        repository.addAccount(
            name = "Revolut EUR Account",
            type = "BANK",
            balance = 2450.0,
            currencyCode = "EUR",
            maskedNumber = "IBAN •••• 7781",
            colorHex = 0xFF6D4AFFL
        )
        val accounts = repository.accounts.first()
        val eurAcc = accounts.find { it.name == "Revolut EUR Account" }
        assertNotNull(eurAcc)
        assertEquals("EUR", eurAcc?.currencyCode)
        assertEquals(2450.0, eurAcc?.balance ?: 0.0, 0.001)

        repository.addCategory(
            name = "Cloud Infrastructure",
            type = "EXPENSE",
            monthlyBudget = 400.0,
            colorHex = 0xFF0077E6L,
            iconKey = "BILLS",
            budgetCurrencyCode = "USD",
            budgetPeriod = "WEEKLY",
            alertThresholdPercent = 75
        )
        val categories = repository.categories.first()
        val cloudCat = categories.find { it.name == "Cloud Infrastructure" }!!
        assertNotNull(cloudCat)
        assertEquals("USD", cloudCat.budgetCurrencyCode)
        assertEquals("WEEKLY", cloudCat.budgetPeriod)
        assertEquals(75, cloudCat.alertThresholdPercent)

        repository.addRecurringPayment(
            title = "Financial Times Subscription",
            provider = "FT Digital London",
            amount = 39.0,
            currencyCode = "GBP",
            transactionType = "EXPENSE",
            frequency = "Monthly",
            daysUntilDue = 5,
            category = cloudCat,
            autoPay = true
        )
        val reminders = repository.scheduledReminders.first()
        val gbpReminder = reminders.find { it.title.contains("Financial Times Subscription") }
        assertNotNull(gbpReminder)
        assertEquals("GBP", gbpReminder?.currencyCode)
        assertEquals(39.0, gbpReminder?.amount ?: 0.0, 0.001)
        assertTrue(gbpReminder?.isEnabled == true)
    }

    @Test
    fun `backup json and csv export round-trip preserve financial records`() = runBlocking {
        repository.ensureSeeded()
        val jsonPayload = repository.exportBackupJson()
        assertTrue(jsonPayload.contains("BRAC Bank Visa Signature"))
        assertTrue(jsonPayload.contains("transactions"))

        val csvPayload = repository.exportTransactionsCsv()
        assertTrue(csvPayload.contains("Date,Title,Merchant,Type,OriginalAmount,CurrencyCode"))

        val restoredSuccess = repository.restoreBackupJson(jsonPayload)
        assertTrue(restoredSuccess)
    }

    @Test
    fun `delete all demo data deletes sample records and preserves real user records`() = runBlocking {
        repository.ensureSeeded()
        val initialAccounts = repository.accounts.first()
        val initialTxs = repository.transactions.first()
        assertTrue("Expected seeded accounts", initialAccounts.any { it.isDemo })
        assertTrue("Expected seeded transactions", initialTxs.any { it.isDemo })

        // Create a real user account and user transaction
        repository.addAccount(
            name = "Personal HSBC Salary",
            type = "BANK",
            balance = 85000.0,
            currencyCode = "BDT",
            maskedNumber = "HSBC •••• 9912",
            colorHex = 0xFF059669L
        )
        val userAcc = repository.accounts.first().find { it.name == "Personal HSBC Salary" }!!
        val userCat = repository.categories.first().first()

        repository.addTransaction(
            title = "Personal Grocery Store",
            merchant = "Meena Bazar",
            amount = 3200.0,
            currencyCode = "BDT",
            type = "EXPENSE",
            category = userCat,
            account = userAcc,
            note = "Real User Purchase"
        )

        // Verify user items exist alongside demo items
        val preDeleteAccounts = repository.accounts.first()
        val preDeleteTxs = repository.transactions.first()
        assertTrue(preDeleteAccounts.any { it.name == "Personal HSBC Salary" && !it.isDemo })
        assertTrue(preDeleteTxs.any { it.title == "Personal Grocery Store" && !it.isDemo })

        // Execute Delete All Demo Data
        repository.deleteDemoData()

        val postDeleteAccounts = repository.accounts.first()
        val postDeleteTxs = repository.transactions.first()
        val postDeleteDebts = repository.debtRecords.first()
        val postDeleteGoals = repository.savingsGoals.first()
        val postDeleteRecurring = repository.recurringPayments.first()

        // Verify ALL demo items are eradicated
        assertTrue("No demo accounts should exist", postDeleteAccounts.none { it.isDemo })
        assertTrue("No demo transactions should exist", postDeleteTxs.none { it.isDemo })
        assertTrue("No demo debts should exist", postDeleteDebts.none { it.isDemo })
        assertTrue("No demo goals should exist", postDeleteGoals.none { it.isDemo })
        assertTrue("No demo recurring should exist", postDeleteRecurring.none { it.isDemo })

        // Verify the real user account and transaction were completely preserved
        assertTrue("Real user account must be preserved", postDeleteAccounts.any { it.name == "Personal HSBC Salary" })
        assertTrue("Real user transaction must be preserved", postDeleteTxs.any { it.title == "Personal Grocery Store" })
    }

    @Test
    fun `app restart and database reopen never re-seeds demo data after deletion`() = runBlocking {
        repository.ensureSeeded()
        assertTrue(repository.accounts.first().isNotEmpty())

        // Delete all demo data
        repository.deleteDemoData()
        val accountsAfterDelete = repository.accounts.first()
        assertTrue("Demo accounts should be removed", accountsAfterDelete.none { it.isDemo })

        // Simulate application restart or fresh ViewModel init by calling ensureSeeded() again
        repository.ensureSeeded()

        val accountsAfterRestart = repository.accounts.first()
        val txsAfterRestart = repository.transactions.first()

        // Critical Check: Demo data must NEVER reappear!
        assertTrue("Demo accounts must NOT reappear after app restart", accountsAfterRestart.none { it.isDemo })
        assertTrue("Demo transactions must NOT reappear after app restart", txsAfterRestart.none { it.isDemo })
    }

    @Test
    fun `reset sample data restores demo records and re-arms idempotent state`() = runBlocking {
        repository.ensureSeeded()
        repository.deleteDemoData()
        assertTrue(repository.transactions.first().none { it.isDemo })

        // User explicitly triggers Reset Sample Data
        repository.resetToDefaultSampleData()

        val restoredAccounts = repository.accounts.first()
        val restoredTxs = repository.transactions.first()

        assertTrue("Demo accounts should be restored", restoredAccounts.any { it.isDemo })
        assertTrue("Demo transactions should be restored", restoredTxs.any { it.isDemo })
        assertEquals(5, restoredAccounts.filter { it.isDemo }.size)
        assertEquals(19, restoredTxs.filter { it.isDemo }.size)
    }

    @Test
    fun `pdf statement generation produces verified non-empty physical document with valid byte count`() = runBlocking {
        val sampleUiState = buildSampleUiState(baseCurrency = "BDT")
        val result = PdfExporter.generateAndSaveStatementPdf(
            context = context,
            uiState = sampleUiState,
            useBangladeshiFormat = true
        )

        assertTrue("Expected PDF export to succeed: ${result.errorMessage}", result.isSuccess)
        assertNotNull("Expected valid file URI", result.fileUri)
        assertNotNull("Expected valid file path", result.filePath)
        assertTrue("Expected PDF byte count > 1000 bytes (was ${result.byteCount})", result.byteCount > 1000L)
        assertTrue("Expected non-empty filename", result.fileName.endsWith(".pdf"))

        val internalFile = File(context.filesDir, "exports/${result.fileName}")
        assertTrue("Internal PDF file must physically exist", internalFile.exists())
        assertEquals(result.byteCount, internalFile.length())
    }

    @Test
    fun `capture genuine application screenshots across all primary screens`() {
        val screenshotsDir = resolveScreenshotsDir()
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }

        val sampleUiState = buildSampleUiState(baseCurrency = "BDT")
        val cashflowSeries = listOf(
            MonthlyCashflowPoint("May", 168000.0, 94500.0, false),
            MonthlyCashflowPoint("Jun", 175000.0, 102400.0, false),
            MonthlyCashflowPoint("Jul", 182000.0, 98200.0, false),
            MonthlyCashflowPoint("Aug", 179500.0, 111000.0, false),
            MonthlyCashflowPoint("Sep", 192000.0, 104600.0, false),
            MonthlyCashflowPoint("Oct", sampleUiState.monthlyIncome, sampleUiState.monthlyExpense, true)
        )
        val dailySpendPoints = listOf(
            DailySpendPoint("Oct 1", "Oct 1, 2026", 1250.0, 0),
            DailySpendPoint("Oct 3", "Oct 3, 2026", 3400.0, 1),
            DailySpendPoint("Oct 5", "Oct 5, 2026", 1890.0, 2),
            DailySpendPoint("Oct 7", "Oct 7, 2026", 5640.0, 3),
            DailySpendPoint("Oct 9", "Oct 9, 2026", 2100.0, 4),
            DailySpendPoint("Oct 11", "Oct 11, 2026", 4250.0, 5),
            DailySpendPoint("Oct 13", "Oct 13, 2026", 2950.0, 6),
            DailySpendPoint("Oct 15", "Oct 15, 2026", 3310.0, 7)
        )

        var currentScreenState = androidx.compose.runtime.mutableStateOf("DASHBOARD_LIGHT")

        composeTestRule.setContent {
            val mode = currentScreenState.value
            val isDark = mode == "DASHBOARD_DARK"
            HisabMateTheme(darkTheme = isDark) {
                val colors = HisabTheme.colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.canvasBackground)
                ) {
                    when (mode) {
                        "DASHBOARD_LIGHT", "DASHBOARD_DARK" -> {
                            DashboardScreen(
                                uiState = sampleUiState,
                                isBalanceHidden = false,
                                useBangladeshiFormat = true,
                                isDarkTheme = isDark,
                                onToggleBalance = {},
                                onCycleTheme = {},
                                onOpenCurrencyPicker = {},
                                onNavigate = {},
                                onOpenPlanningTab = {},
                                onOpenWalletDetails = {},
                                onOpenAddTransaction = { _, _ -> },
                                onSelectTransaction = {}
                            )
                            FloatingFintechDock(
                                currentScreen = AppScreen.DASHBOARD,
                                onSelectScreen = {},
                                onAddClick = {},
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        "ACCOUNTS_WALLETS" -> {
                            AccountsScreen(
                                uiState = sampleUiState,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onBack = {},
                                onOpenTransfer = {},
                                onOpenSettings = {},
                                onOpenCurrencyManager = {},
                                onCreateAccount = { _, _, _, _, _, _ -> },
                                onSelectTransaction = {}
                            )
                            FloatingFintechDock(
                                currentScreen = AppScreen.ACCOUNTS,
                                onSelectScreen = {},
                                onAddClick = {},
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        "WALLET_DETAILS" -> {
                            val cat = sampleUiState.categories.first()
                            val summary = sampleUiState.categorySummaries.first()
                            WalletDetailsScreen(
                                category = cat,
                                categorySpentInBase = summary.totalSpentInBase,
                                budgetLimitInBase = summary.budgetLimitInBase,
                                baseCurrencyCode = sampleUiState.baseCurrencyCode,
                                ratesToBdtMap = sampleUiState.ratesToBdtMap,
                                transactions = sampleUiState.transactions.filter { it.categoryId == cat.id },
                                dailyPoints = dailySpendPoints,
                                monthOffset = 0,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onBack = {},
                                onShiftMonth = {},
                                onOpenAddForCategory = {},
                                onOpenTransfer = {},
                                onUpdateBudget = { _, _, _, _, _, _, _ -> },
                                onSelectTransaction = {}
                            )
                        }

                        "BUDGET_MANAGEMENT" -> {
                            PlanningHubScreen(
                                uiState = sampleUiState,
                                selectedTab = PlanningTab.BUDGETS,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onSelectTab = {},
                                onBack = {},
                                onOpenWalletDetails = {},
                                onUpdateBudget = { _, _, _, _, _, _, _ -> },
                                onCreateCategory = { _, _, _, _, _, _, _, _ -> },
                                onPayRecurring = {},
                                onCreateRecurring = { _, _, _, _, _, _, _, _, _ -> },
                                onDeleteRecurring = {},
                                onSettleDebt = { _, _ -> },
                                onCreateDebt = { _, _, _, _, _, _, _ -> },
                                onDeleteDebt = {},
                                onContributeGoal = { _, _ -> },
                                onCreateGoal = { _, _, _, _, _, _, _, _ -> },
                                onDeleteGoal = {}
                            )
                            FloatingFintechDock(
                                currentScreen = AppScreen.PLANNING_HUB,
                                onSelectScreen = {},
                                onAddClick = {},
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        "SAVINGS_GOALS" -> {
                            PlanningHubScreen(
                                uiState = sampleUiState,
                                selectedTab = PlanningTab.GOALS,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onSelectTab = {},
                                onBack = {},
                                onOpenWalletDetails = {},
                                onUpdateBudget = { _, _, _, _, _, _, _ -> },
                                onCreateCategory = { _, _, _, _, _, _, _, _ -> },
                                onPayRecurring = {},
                                onCreateRecurring = { _, _, _, _, _, _, _, _, _ -> },
                                onDeleteRecurring = {},
                                onSettleDebt = { _, _ -> },
                                onCreateDebt = { _, _, _, _, _, _, _ -> },
                                onDeleteDebt = {},
                                onContributeGoal = { _, _ -> },
                                onCreateGoal = { _, _, _, _, _, _, _, _ -> },
                                onDeleteGoal = {}
                            )
                        }

                        "DEBT_MANAGEMENT" -> {
                            PlanningHubScreen(
                                uiState = sampleUiState,
                                selectedTab = PlanningTab.DEBT_LEND,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onSelectTab = {},
                                onBack = {},
                                onOpenWalletDetails = {},
                                onUpdateBudget = { _, _, _, _, _, _, _ -> },
                                onCreateCategory = { _, _, _, _, _, _, _, _ -> },
                                onPayRecurring = {},
                                onCreateRecurring = { _, _, _, _, _, _, _, _, _ -> },
                                onDeleteRecurring = {},
                                onSettleDebt = { _, _ -> },
                                onCreateDebt = { _, _, _, _, _, _, _ -> },
                                onDeleteDebt = {},
                                onContributeGoal = { _, _ -> },
                                onCreateGoal = { _, _, _, _, _, _, _, _ -> },
                                onDeleteGoal = {}
                            )
                        }

                        "MULTI_CURRENCY_FX" -> {
                            CurrencyManagementScreen(
                                uiState = sampleUiState,
                                useBangladeshiFormat = true,
                                onSelectBaseCurrency = {},
                                onUpdateManualRate = { _, _ -> },
                                onResetRates = {},
                                onBack = {}
                            )
                        }

                        "FINANCIAL_REPORTS" -> {
                            SpendAnalysisScreen(
                                uiState = sampleUiState,
                                cashflowPoints = cashflowSeries,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onBack = {},
                                onSelectReportPeriod = {},
                                onOpenCalendar = {},
                                onOpenCurrencyPicker = {},
                                onOpenWalletDetails = {},
                                onSelectTransaction = {}
                            )
                            FloatingFintechDock(
                                currentScreen = AppScreen.SPEND_ANALYSIS,
                                onSelectScreen = {},
                                onAddClick = {},
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        "BILLS_NOTIFICATIONS" -> {
                            NotificationsScreen(
                                notifications = sampleUiState.notifications,
                                scheduledReminders = sampleUiState.scheduledReminders,
                                baseCurrencyCode = sampleUiState.baseCurrencyCode,
                                ratesToBdtMap = sampleUiState.ratesToBdtMap,
                                useBangladeshiFormat = true,
                                onMarkAllRead = {},
                                onDismiss = {},
                                onBack = {}
                            )
                        }

                        "SETTINGS_SECURITY" -> {
                            ProfileAndSettingsScreen(
                                uiState = sampleUiState,
                                baseCurrencyCode = sampleUiState.baseCurrencyCode,
                                themePreference = ThemePreference.LIGHT,
                                useBangladeshiFormat = true,
                                isBalanceHidden = false,
                                onOpenCurrencyPicker = {},
                                onOpenCurrencyManager = {},
                                onSetTheme = {},
                                onToggleNumberFormat = {},
                                onToggleBalanceHidden = {},
                                onConfigureAppLock = { _, _, _ -> },
                                onLockNow = {},
                                onUpdateNotificationPrefs = { _, _ -> },
                                onGenerateBackupJson = {},
                                onRestoreBackupJson = {},
                                onGenerateCsv = {},
                                onImportCsv = {},
                                onExportSummary = {},
                                onResetData = {},
                                onNavigate = {},
                                onBack = {}
                            )
                        }

                        "ONBOARDING_OVERVIEW" -> {
                            CinematicHisabOnboardingFlow(
                                currentBaseCurrency = "BDT",
                                onCompleteOnboarding = { _, _, _, _ -> }
                            )
                        }
                    }
                }
            }
        }

        val capturePlan = listOf(
            "DASHBOARD_LIGHT" to "dashboard-light.png",
            "DASHBOARD_DARK" to "dashboard-dark.png",
            "ACCOUNTS_WALLETS" to "accounts-wallets.png",
            "WALLET_DETAILS" to "wallet-details-analytics.png",
            "BUDGET_MANAGEMENT" to "budget-management.png",
            "MULTI_CURRENCY_FX" to "multi-currency-fx.png",
            "FINANCIAL_REPORTS" to "financial-reports.png",
            "SAVINGS_GOALS" to "savings-goals.png",
            "BILLS_NOTIFICATIONS" to "bills-notifications.png",
            "DEBT_MANAGEMENT" to "debt-management.png",
            "SETTINGS_SECURITY" to "settings-security.png",
            "ONBOARDING_OVERVIEW" to "onboarding-overview.png"
        )

        for ((screenKey, fileName) in capturePlan) {
            composeTestRule.runOnIdle {
                currentScreenState.value = screenKey
            }
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(600)
            composeTestRule.waitForIdle()

            val targetFile = File(screenshotsDir, fileName)
            captureActivityToPng(composeTestRule.activity, targetFile)
            assertTrue("Expected screenshot ${targetFile.absolutePath} to exist and be non-empty", targetFile.exists() && targetFile.length() > 5000)
        }
    }

    private fun resolveScreenshotsDir(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val rootDir = if (cwd.name == "app") cwd.parentFile ?: cwd else cwd
        return File(rootDir, "docs/screenshots")
    }

    private fun captureActivityToPng(activity: Activity, outputFile: File) {
        val rootView = activity.window.decorView
        val width = rootView.width.coerceAtLeast(1080)
        val height = rootView.height.coerceAtLeast(2200)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeTestRule.runOnUiThread {
            rootView.draw(canvas)
        }
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun buildSampleUiState(baseCurrency: String): HisabUiState {
        val rates = SeedDataGenerator.initialExchangeRates()
        val ratesMap = rates.associate { it.currencyCode to it.rateToBdt }
        val accounts = SeedDataGenerator.initialAccounts()
        val categories = SeedDataGenerator.initialCategories()
        val transactions = SeedDataGenerator.initialTransactions()
        val recurring = SeedDataGenerator.initialRecurringPayments()
        val debts = SeedDataGenerator.initialDebts()
        val goals = SeedDataGenerator.initialGoals()
        val reminders = SeedDataGenerator.initialScheduledReminders()
        val notifications = SeedDataGenerator.initialNotifications()

        val totalBalance = accounts.sumOf {
            CurrencyFormatter.convert(it.balance, it.currencyCode, baseCurrency, ratesMap)
        }
        val monthlyIncome = transactions.filter { it.type == "INCOME" }.sumOf {
            CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap)
        }
        val monthlyExpense = transactions.filter { it.type == "EXPENSE" }.sumOf {
            CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap)
        }
        val monthlySavings = (monthlyIncome - monthlyExpense).coerceAtLeast(0.0)
        val savingsRate = if (monthlyIncome > 0) ((monthlySavings / monthlyIncome) * 100).toInt() else 0

        val expenseCats = categories.filter { it.type == "EXPENSE" }
        val summaries = expenseCats.map { cat ->
            val catTxs = transactions.filter { it.type == "EXPENSE" && it.categoryId == cat.id }
            val spentInBase = catTxs.sumOf {
                CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap)
            }
            val limitInBase = CurrencyFormatter.convert(cat.monthlyBudget, cat.budgetCurrencyCode, baseCurrency, ratesMap)
            val progress = if (limitInBase > 0) (spentInBase / limitInBase).toFloat().coerceIn(0f, 1.5f) else 0f
            val share = if (monthlyExpense > 0) (spentInBase / monthlyExpense).toFloat().coerceIn(0f, 1f) else 0f
            CategorySpendSummary(
                category = cat,
                totalSpentInBase = spentInBase,
                budgetLimitInBase = limitInBase,
                transactionCount = catTxs.size,
                budgetProgress = progress,
                shareOfSpend = share
            )
        }

        return HisabUiState(
            baseCurrencyCode = baseCurrency,
            languageCode = "en",
            regionCode = "BD",
            hasCompletedOnboarding = true,
            exchangeRates = rates,
            ratesToBdtMap = ratesMap,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            favoriteTransactions = transactions.filter { it.isFavorite },
            recurringPayments = recurring,
            debtRecords = debts,
            savingsGoals = goals,
            notifications = notifications,
            scheduledReminders = reminders,
            totalBalance = totalBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlySavings = monthlySavings,
            savingsRatePercent = savingsRate,
            reportPeriod = ReportPeriod.MONTHLY,
            periodReportIncome = monthlyIncome,
            periodReportExpense = monthlyExpense,
            categorySummaries = summaries,
            quickWalletSummaries = summaries.take(4),
            unreadNotificationsCount = notifications.count { !it.isRead },
            foreignCurrencyAccountCount = accounts.count { it.currencyCode != baseCurrency },
            foreignCurrencyTxCount = transactions.count { it.currencyCode != baseCurrency },
            isLoading = false
        )
    }
}
