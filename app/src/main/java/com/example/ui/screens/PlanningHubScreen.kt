package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.DebtRecordEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.SavingsGoalEntity
import com.example.ui.components.HeaderCircleIconButton
import com.example.ui.components.HisabHeaderSurface
import com.example.ui.components.HisabSectionCard
import com.example.ui.components.StylizedBdtAmount
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.ui.viewmodel.CategorySpendSummary
import com.example.ui.viewmodel.HisabUiState
import com.example.ui.viewmodel.PlanningTab
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter

@Composable
fun PlanningHubScreen(
    uiState: HisabUiState,
    selectedTab: PlanningTab,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onSelectTab: (PlanningTab) -> Unit,
    onBack: () -> Unit,
    onOpenWalletDetails: (Int) -> Unit,
    onUpdateBudget: (CategoryEntity, Double, String, String, Int, Long, Long) -> Unit,
    onCreateCategory: (String, String, Double, Long, String, String, String, Int) -> Unit,
    onPayRecurring: (RecurringPaymentEntity) -> Unit,
    onCreateRecurring: (String, String, Double, String, String, Int, CategoryEntity, Boolean, String) -> Unit,
    onDeleteRecurring: (Int) -> Unit,
    onSettleDebt: (DebtRecordEntity, Double) -> Unit,
    onCreateDebt: (String, String, Double, String, String, Int, String) -> Unit,
    onDeleteDebt: (Int) -> Unit,
    onContributeGoal: (SavingsGoalEntity, Double) -> Unit,
    onCreateGoal: (String, String, Double, Double, String, Int, Long, String) -> Unit,
    onDeleteGoal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val baseCurrency = uiState.baseCurrencyCode
    val baseMeta = CurrencyCatalog.getMetadata(baseCurrency)
    val ratesMap = uiState.ratesToBdtMap

    var editingBudgetCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var settlingDebtRecord by remember { mutableStateOf<DebtRecordEntity?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val totalBudgetLimitInBase = uiState.categorySummaries.sumOf { it.budgetLimitInBase }
    val totalLentRemainingInBase = uiState.debtRecords.filter { it.type == "LENT" }.sumOf {
        val rem = (it.totalAmount - it.settledAmount).coerceAtLeast(0.0)
        CurrencyFormatter.convert(rem, it.currencyCode, baseCurrency, ratesMap)
    }
    val totalBorrowedRemainingInBase = uiState.debtRecords.filter { it.type == "BORROWED" }.sumOf {
        val rem = (it.totalAmount - it.settledAmount).coerceAtLeast(0.0)
        CurrencyFormatter.convert(rem, it.currencyCode, baseCurrency, ratesMap)
    }
    val totalGoalsSavedInBase = uiState.savingsGoals.sumOf {
        CurrencyFormatter.convert(it.savedAmount, it.currencyCode, baseCurrency, ratesMap)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        // 1. Header + Pill Tab Switcher + Dynamic Summary Hero
        item {
            HisabHeaderSurface {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HeaderCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            testTag = "btn_planning_back",
                            onClick = onBack
                        )
                        Text(
                            text = "Financial Planning",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    HeaderCircleIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add new item",
                        testTag = "btn_planning_top_add",
                        onClick = {
                            when (selectedTab) {
                                PlanningTab.BUDGETS -> editingBudgetCategory = uiState.categories.firstOrNull { it.type == "EXPENSE" }
                                PlanningTab.RECURRING -> showAddRecurringDialog = true
                                PlanningTab.DEBT_LEND -> showAddDebtDialog = true
                                PlanningTab.GOALS -> showAddGoalDialog = true
                                PlanningTab.CATEGORIES -> showAddCategoryDialog = true
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-Contrast Scrollable Pill Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanningTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            onClick = { onSelectTab(tab) },
                            shape = RoundedCornerShape(24.dp),
                            color = if (isSelected) colors.inkPrimary else colors.containerInteractive,
                            border = BorderStroke(1.dp, if (isSelected) colors.inkPrimary else colors.borderStrong),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("planning_tab_${tab.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.cardSurface else colors.inkPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Contextual Summary Metric inside Header
                when (selectedTab) {
                    PlanningTab.BUDGETS -> {
                        Text(
                            text = "Monthly Budget Allocation ($baseCurrency)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            StylizedBdtAmount(
                                amount = totalBudgetLimitInBase,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isHidden = isBalanceHidden,
                                currencyCode = baseCurrency,
                                mainFontSize = 30.sp,
                                symbolFontSize = 16.sp,
                                decimalFontSize = 22.sp
                            )
                            val pctUsed = if (totalBudgetLimitInBase > 0) ((uiState.monthlyExpense / totalBudgetLimitInBase) * 100).toInt() else 0
                            Text(
                                text = "$pctUsed% utilized",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (pctUsed > 90) colors.expenseText else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    PlanningTab.RECURRING -> {
                        val totalMonthlyRecurringInBase = uiState.recurringPayments.sumOf {
                            CurrencyFormatter.convert(it.amount, it.currencyCode, baseCurrency, ratesMap)
                        }
                        Text(
                            text = "Scheduled Monthly Commitments ($baseCurrency)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StylizedBdtAmount(
                            amount = totalMonthlyRecurringInBase,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isHidden = isBalanceHidden,
                            currencyCode = baseCurrency,
                            mainFontSize = 30.sp,
                            symbolFontSize = 16.sp,
                            decimalFontSize = 22.sp
                        )
                    }
                    PlanningTab.DEBT_LEND -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.incomeContainer,
                                border = BorderStroke(1.dp, colors.incomeText.copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "You Will Receive (Lent)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.incomeText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBalanceHidden) "${baseMeta.symbol} ••••"
                                        else CurrencyFormatter.formatCurrency(totalLentRemainingInBase, baseCurrency, useBangladeshiFormat),
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = colors.incomeText
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.expenseContainer,
                                border = BorderStroke(1.dp, colors.expenseText.copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "You Owe (Borrowed)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.expenseText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBalanceHidden) "${baseMeta.symbol} ••••"
                                        else CurrencyFormatter.formatCurrency(totalBorrowedRemainingInBase, baseCurrency, useBangladeshiFormat),
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = colors.expenseText
                                    )
                                }
                            }
                        }
                    }
                    PlanningTab.GOALS -> {
                        Text(
                            text = "Total Saved Across Financial Goals ($baseCurrency)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StylizedBdtAmount(
                            amount = totalGoalsSavedInBase,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isHidden = isBalanceHidden,
                            currencyCode = baseCurrency,
                            mainFontSize = 30.sp,
                            symbolFontSize = 16.sp,
                            decimalFontSize = 22.sp
                        )
                    }
                    PlanningTab.CATEGORIES -> {
                        Text(
                            text = "${uiState.categories.size} Active Expense & Income Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 2. Active Tab Content
        when (selectedTab) {
            PlanningTab.BUDGETS -> {
                items(uiState.categorySummaries, key = { it.category.id }) { summary ->
                    BudgetCategoryCard(
                        summary = summary,
                        baseCurrencyCode = baseCurrency,
                        useBangladeshiFormat = useBangladeshiFormat,
                        isBalanceHidden = isBalanceHidden,
                        onOpenDetails = { onOpenWalletDetails(summary.category.id) },
                        onEditBudget = { editingBudgetCategory = summary.category },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            PlanningTab.RECURRING -> {
                items(uiState.recurringPayments, key = { it.id }) { bill ->
                    RecurringPaymentCard(
                        bill = bill,
                        baseCurrencyCode = baseCurrency,
                        ratesToBdtMap = ratesMap,
                        useBangladeshiFormat = useBangladeshiFormat,
                        onPayNow = { onPayRecurring(bill) },
                        onDelete = { onDeleteRecurring(bill.id) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            PlanningTab.DEBT_LEND -> {
                items(uiState.debtRecords, key = { it.id }) { debt ->
                    DebtRecordCard(
                        record = debt,
                        baseCurrencyCode = baseCurrency,
                        ratesToBdtMap = ratesMap,
                        useBangladeshiFormat = useBangladeshiFormat,
                        onSettleClick = { settlingDebtRecord = debt },
                        onDeleteClick = { onDeleteDebt(debt.id) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            PlanningTab.GOALS -> {
                items(uiState.savingsGoals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        baseCurrencyCode = baseCurrency,
                        ratesToBdtMap = ratesMap,
                        useBangladeshiFormat = useBangladeshiFormat,
                        isBalanceHidden = isBalanceHidden,
                        onContribute = { contributingGoal = goal },
                        onDelete = { onDeleteGoal(goal.id) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            PlanningTab.CATEGORIES -> {
                items(uiState.categories, key = { it.id }) { cat ->
                    val convertedBudget = CurrencyFormatter.convert(
                        cat.monthlyBudget,
                        cat.budgetCurrencyCode,
                        baseCurrency,
                        ratesMap
                    )
                    CategoryManagementRow(
                        category = cat,
                        budgetInBase = convertedBudget,
                        baseCurrencyCode = baseCurrency,
                        useBangladeshiFormat = useBangladeshiFormat,
                        onClick = {
                            if (cat.type == "EXPENSE") onOpenWalletDetails(cat.id)
                        },
                        onEditBudget = { editingBudgetCategory = cat },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    // Modal 1: Premium Budget Configuration & Period Modal
    editingBudgetCategory?.let { cat ->
        val catSpentInBase = uiState.categorySummaries.find { it.category.id == cat.id }?.totalSpentInBase ?: 0.0
        PremiumBudgetModal(
            initialCategory = cat,
            allExpenseCategories = uiState.categories.filter { it.type == "EXPENSE" }.ifEmpty { listOf(cat) },
            currentSpentInBase = catSpentInBase,
            baseCurrencyCode = baseCurrency,
            ratesToBdtMap = ratesMap,
            useBangladeshiFormat = useBangladeshiFormat,
            onDismiss = { editingBudgetCategory = null },
            onSaveBudget = { selectedCat, newAmt, period, currCode, thresholdPct, startMs, endMs ->
                onUpdateBudget(selectedCat, newAmt, period, currCode, thresholdPct, startMs, endMs)
            }
        )
    }

    // Modal 2: Add Recurring Payment / Income (Currency-aware across all 14 ISO 4217 currencies)
    if (showAddRecurringDialog) {
        var title by remember { mutableStateOf("") }
        var provider by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var currencyCode by remember(baseCurrency) { mutableStateOf(baseCurrency) }
        var frequency by remember { mutableStateOf("Monthly") }
        var daysUntilDue by remember { mutableStateOf("7") }
        var recurringType by remember { mutableStateOf("EXPENSE") }
        val filteredCats = uiState.categories.filter { it.type == recurringType }.ifEmpty { uiState.categories }
        val selectedCat = filteredCats.firstOrNull()
        val currMeta = CurrencyCatalog.getMetadata(currencyCode)

        AlertDialog(
            onDismissRequest = { showAddRecurringDialog = false },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Schedule Recurring Bill or Income",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = recurringType == "EXPENSE",
                            onClick = { recurringType = "EXPENSE" },
                            label = { Text("Bill / Expense", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = recurringType == "INCOME",
                            onClick = { recurringType = "INCOME" },
                            label = { Text("Recurring Income", fontWeight = FontWeight.Bold) }
                        )
                    }
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                label = { Text(freq, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Text(
                        text = "Billing Currency (${currMeta.symbol} ${currMeta.code})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyCatalog.supportedCurrencies.forEach { meta ->
                            FilterChip(
                                selected = currencyCode == meta.code,
                                onClick = { currencyCode = meta.code },
                                label = { Text("${meta.symbol} ${meta.code}", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (recurringType == "INCOME") "Income Stream Name" else "Bill or Subscription Name") },
                        placeholder = { Text(if (recurringType == "INCOME") "e.g., Monthly Retainer / Rental" else "e.g., Carnival Internet / AWS") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider / Biller") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount in ${currMeta.name} (${currMeta.symbol} ${currMeta.code})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = daysUntilDue,
                        onValueChange = { daysUntilDue = it.filter { c -> c.isDigit() } },
                        label = { Text("Next Due In (Days) — Local Reminder Active") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        val days = daysUntilDue.toIntOrNull() ?: 7
                        val cat = selectedCat ?: uiState.categories.firstOrNull()
                        if (title.isNotBlank() && amt > 0 && cat != null) {
                            onCreateRecurring(
                                title,
                                provider.ifBlank { if (recurringType == "INCOME") "Recurring Income" else "$frequency Bill" },
                                amt,
                                currencyCode,
                                frequency,
                                days,
                                cat,
                                true,
                                recurringType
                            )
                            showAddRecurringDialog = false
                        }
                    }
                ) {
                    Text("Schedule (${currMeta.code})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecurringDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 3: Add Debt / Lending Record (Currency-aware across all 14 ISO 4217 currencies)
    if (showAddDebtDialog) {
        var person by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var currencyCode by remember(baseCurrency) { mutableStateOf(baseCurrency) }
        var phoneTag by remember { mutableStateOf("bKash / Bank Transfer") }
        var daysUntilDue by remember { mutableStateOf("10") }
        var type by remember { mutableStateOf("LENT") }
        val currMeta = CurrencyCatalog.getMetadata(currencyCode)

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Add Debt or Lending Record",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "LENT",
                            onClick = { type = "LENT" },
                            label = { Text("Money Lent (Receivable)", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = type == "BORROWED",
                            onClick = { type = "BORROWED" },
                            label = { Text("Borrowed", fontWeight = FontWeight.Bold) }
                        )
                    }
                    Text(
                        text = "Settlement Currency (${currMeta.symbol} ${currMeta.code})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyCatalog.supportedCurrencies.forEach { meta ->
                            FilterChip(
                                selected = currencyCode == meta.code,
                                onClick = { currencyCode = meta.code },
                                label = { Text("${meta.symbol} ${meta.code}", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = { Text("Contact Name") },
                        placeholder = { Text("e.g., Farhan Sadiq") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount in ${currMeta.name} (${currMeta.symbol} ${currMeta.code})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Reason / Note") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = daysUntilDue,
                        onValueChange = { daysUntilDue = it.filter { c -> c.isDigit() } },
                        label = { Text("Due In (Days) — Local Reminder Active") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        val dueDays = daysUntilDue.toIntOrNull() ?: 10
                        if (person.isNotBlank() && amt > 0) {
                            onCreateDebt(
                                person,
                                note.ifBlank { "Personal settlement" },
                                amt,
                                currencyCode,
                                type,
                                dueDays,
                                phoneTag
                            )
                            showAddDebtDialog = false
                        }
                    }
                ) {
                    Text("Save Record (${currMeta.code})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 4: Settle Debt (Uses the debt record's exact native currency)
    settlingDebtRecord?.let { rec ->
        val remaining = (rec.totalAmount - rec.settledAmount).coerceAtLeast(0.0)
        val recMeta = CurrencyCatalog.getMetadata(rec.currencyCode)
        var settleInput by remember(rec) {
            mutableStateOf(if (remaining % 1.0 == 0.0) remaining.toLong().toString() else remaining.toString())
        }
        AlertDialog(
            onDismissRequest = { settlingDebtRecord = null },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Settle with ${rec.personName}",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Remaining balance: ${
                            CurrencyFormatter.formatCurrency(
                                amount = remaining,
                                currencyCode = rec.currencyCode,
                                useSouthAsianGrouping = useBangladeshiFormat,
                                includeCodeSuffix = true
                            )
                        }",
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    OutlinedTextField(
                        value = settleInput,
                        onValueChange = { settleInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Settlement Amount (${recMeta.symbol} ${recMeta.code})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = settleInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onSettleDebt(rec, amt)
                            settlingDebtRecord = null
                        }
                    }
                ) {
                    Text("Record Payment (${recMeta.code})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { settlingDebtRecord = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 5: Add Financial Goal (Currency-aware across all 14 ISO 4217 currencies)
    if (showAddGoalDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var initial by remember { mutableStateOf("0") }
        var currencyCode by remember(baseCurrency) { mutableStateOf(baseCurrency) }
        val currMeta = CurrencyCatalog.getMetadata(currencyCode)

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Create Financial Savings Goal",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Goal Currency (${currMeta.symbol} ${currMeta.code})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyCatalog.supportedCurrencies.forEach { meta ->
                            FilterChip(
                                selected = currencyCode == meta.code,
                                onClick = { currencyCode = meta.code },
                                label = { Text("${meta.symbol} ${meta.code}", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Goal Name") },
                        placeholder = { Text("e.g., Emergency Fund / MacBook / Umrah Pot") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Description") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Target Amount (${currMeta.symbol} ${currMeta.code})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initial,
                        onValueChange = { initial = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Initial Deposit (${currMeta.symbol} ${currMeta.code})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tgt = target.toDoubleOrNull() ?: 0.0
                        val initAmt = initial.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && tgt > 0) {
                            onCreateGoal(
                                title,
                                subtitle.ifBlank { "Dedicated savings pot" },
                                tgt,
                                initAmt,
                                currencyCode,
                                90,
                                0xFF6D4AFFL,
                                "SAVINGS"
                            )
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Create Goal (${currMeta.code})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 6: Contribute to Goal (Uses goal's native currency)
    contributingGoal?.let { goal ->
        val goalMeta = CurrencyCatalog.getMetadata(goal.currencyCode)
        val defaultContribution = when (goal.currencyCode) {
            "BDT", "INR", "JPY" -> "5000"
            else -> "100"
        }
        var amountInput by remember(goal) { mutableStateOf(defaultContribution) }
        AlertDialog(
            onDismissRequest = { contributingGoal = null },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Contribute to ${goal.title}",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current progress: ${
                            CurrencyFormatter.formatCurrency(goal.savedAmount, goal.currencyCode, useBangladeshiFormat)
                        } of ${
                            CurrencyFormatter.formatCurrency(goal.targetAmount, goal.currencyCode, useBangladeshiFormat, includeCodeSuffix = true)
                        }",
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Contribution (${goalMeta.symbol} ${goalMeta.code})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onContributeGoal(goal, amt)
                            contributingGoal = null
                        }
                    }
                ) {
                    Text("Add Funds (${goalMeta.code})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contributingGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 7: Create Category (Supports Currency, Period & Alert Threshold)
    if (showAddCategoryDialog) {
        var name by remember { mutableStateOf("") }
        var budget by remember { mutableStateOf("10000") }
        var catType by remember { mutableStateOf("EXPENSE") }
        var budgetCurrency by remember(baseCurrency) { mutableStateOf(baseCurrency) }
        var budgetPeriod by remember { mutableStateOf("MONTHLY") }
        var selectedColor by remember { mutableLongStateOf(0xFF6D4AFFL) }
        val colorList = listOf(0xFF6D4AFFL, 0xFF0084E9L, 0xFF059669L, 0xFFF26419L, 0xFFDB2777L, 0xFF0891B2L)
        val currMeta = CurrencyCatalog.getMetadata(budgetCurrency)

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Create New Category Wallet",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = catType == "EXPENSE",
                            onClick = { catType = "EXPENSE" },
                            label = { Text("Expense", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = catType == "INCOME",
                            onClick = { catType = "INCOME" },
                            label = { Text("Income", fontWeight = FontWeight.Bold) }
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g., Education / Gadgets / Travel") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (catType == "EXPENSE") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { per ->
                                FilterChip(
                                    selected = budgetPeriod == per,
                                    onClick = { budgetPeriod = per },
                                    label = { Text(per, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyCatalog.supportedCurrencies.forEach { meta ->
                                FilterChip(
                                    selected = budgetCurrency == meta.code,
                                    onClick = { budgetCurrency = meta.code },
                                    label = { Text("${meta.symbol} ${meta.code}", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = budget,
                            onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("$budgetPeriod Budget (${currMeta.symbol} ${currMeta.code})") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colorList.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selectedColor == hex) 2.5.dp else 0.dp,
                                        color = colors.inkPrimary,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val b = if (catType == "EXPENSE") (budget.toDoubleOrNull() ?: 10000.0) else 0.0
                        if (name.isNotBlank()) {
                            onCreateCategory(
                                name,
                                catType,
                                b,
                                selectedColor,
                                "SHOPPING",
                                budgetCurrency,
                                budgetPeriod,
                                80
                            )
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Create Category", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BudgetCategoryCard(
    summary: CategorySpendSummary,
    baseCurrencyCode: String,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onOpenDetails: () -> Unit,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(summary.category.colorHex)
    val ratio = summary.budgetProgress.coerceIn(0f, 1f)
    val remaining = (summary.budgetLimitInBase - summary.totalSpentInBase).coerceAtLeast(0.0)
    val statusColor = when {
        summary.budgetProgress >= 1.0f -> colors.expenseText
        summary.budgetProgress >= 0.8f -> colors.warningText
        else -> palette.main
    }

    HisabSectionCard(
        onClick = onOpenDetails,
        contentPadding = 16.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) palette.softBgDark else palette.softBgLight)
                        .border(1.dp, palette.main.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(summary.category.iconKey),
                        contentDescription = summary.category.name,
                        tint = if (colors.isDark) palette.main else palette.highContrastTextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = summary.category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.containerInteractive,
                            border = BorderStroke(1.dp, colors.borderStrong)
                        ) {
                            Text(
                                text = summary.category.budgetPeriod,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${summary.transactionCount} transactions • Alert at ${summary.category.alertThresholdPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary
                    )
                }
            }

            IconButton(onClick = onEditBudget) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit budget",
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isBalanceHidden) "Spent: ••••"
                else "Spent: ${CurrencyFormatter.formatCurrency(summary.totalSpentInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)}",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colors.inkPrimary
            )
            Text(
                text = if (isBalanceHidden) "Limit: ••••"
                else "Limit: ${CurrencyFormatter.formatCurrency(summary.budgetLimitInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)}",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.inkSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { ratio },
            color = statusColor,
            trackColor = colors.containerInteractive,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        Spacer(modifier = Modifier.height(6.dp))

        val alertTag = when {
            summary.budgetProgress >= 1.0f -> " • Budget Exceeded!"
            summary.budgetProgress >= 0.8f -> " • Near Threshold Alert"
            else -> ""
        }
        Text(
            text = if (isBalanceHidden) "•••• remaining"
            else "${CurrencyFormatter.formatCurrency(remaining, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)} remaining (${((1f - ratio) * 100).toInt()}% left)$alertTag",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun RecurringPaymentCard(
    bill: RecurringPaymentEntity,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    useBangladeshiFormat: Boolean,
    onPayNow: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(bill.colorHex)
    val isForeign = !bill.currencyCode.equals(baseCurrencyCode, ignoreCase = true)
    val amountInBase = CurrencyFormatter.convert(bill.amount, bill.currencyCode, baseCurrencyCode, ratesToBdtMap)
    val isIncome = bill.transactionType == "INCOME"

    HisabSectionCard(
        contentPadding = 16.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) palette.softBgDark else palette.softBgLight)
                        .border(1.dp, palette.main.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = bill.title,
                        tint = if (colors.isDark) palette.main else palette.highContrastTextLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bill.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isIncome) colors.incomeContainer else colors.containerInteractive,
                            border = BorderStroke(1.dp, if (isIncome) colors.incomeText.copy(alpha = 0.4f) else colors.borderStrong)
                        ) {
                            Text(
                                text = if (isIncome) "INCOME" else "BILL",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) colors.incomeText else colors.inkSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${bill.provider} • Due ${CurrencyFormatter.formatDateShort(bill.nextDueDateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+" else "") + CurrencyFormatter.formatCurrency(amountInBase, baseCurrencyCode, useBangladeshiFormat),
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isIncome) colors.incomeText else colors.inkPrimary
                )
                if (isForeign) {
                    Text(
                        text = "Orig: ${CurrencyFormatter.formatCurrency(bill.amount, bill.currencyCode, false, includeCodeSuffix = true)}",
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (bill.autoPay) colors.incomeContainer else colors.containerInteractive,
                border = BorderStroke(1.dp, if (bill.autoPay) colors.incomeText.copy(alpha = 0.35f) else colors.borderStrong)
            ) {
                Text(
                    text = if (bill.autoPay) "Auto-Pay Enabled • ${bill.frequency}" else "Manual • ${bill.frequency}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (bill.autoPay) colors.incomeText else colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete recurring bill",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Button(
                    onClick = onPayNow,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_pay_recurring_${bill.id}")
                ) {
                    Text(
                        text = if (isIncome) "Receive Now" else "Pay Now",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtRecordCard(
    record: DebtRecordEntity,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    useBangladeshiFormat: Boolean,
    onSettleClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val isLent = record.type == "LENT"
    val accentText = if (isLent) colors.incomeText else colors.expenseText
    val accentContainer = if (isLent) colors.incomeContainer else colors.expenseContainer
    val remainingOrig = (record.totalAmount - record.settledAmount).coerceAtLeast(0.0)
    val remainingInBase = CurrencyFormatter.convert(remainingOrig, record.currencyCode, baseCurrencyCode, ratesToBdtMap)
    val totalInBase = CurrencyFormatter.convert(record.totalAmount, record.currencyCode, baseCurrencyCode, ratesToBdtMap)
    val progress = if (record.totalAmount > 0) (record.settledAmount / record.totalAmount).toFloat().coerceIn(0f, 1f) else 1f

    HisabSectionCard(
        contentPadding = 16.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = record.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentContainer,
                        border = BorderStroke(1.dp, accentText.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = if (isLent) "LENT" else "BORROWED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.relationOrNote} • ${record.phoneOrTag}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.inkSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatCurrency(remainingInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false),
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = accentText
                )
                Text(
                    text = "of ${CurrencyFormatter.formatCurrency(totalInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            color = accentText,
            trackColor = colors.containerInteractive,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        if (record.repaymentHistory.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.containerInteractive,
                border = BorderStroke(1.dp, colors.borderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Repayment Log: ${record.repaymentHistory.replace(";", " • ")}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Due ${CurrencyFormatter.formatDateShort(record.dueDateMillis)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.inkSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete debt record",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (remainingOrig > 0.0) {
                    OutlinedButton(
                        onClick = onSettleClick,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_settle_debt_${record.id}")
                    ) {
                        Text("Record Settlement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Settled",
                            tint = colors.incomeText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settled", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.incomeText)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(goal.colorHex)
    val ratio = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val savedInBase = CurrencyFormatter.convert(goal.savedAmount, goal.currencyCode, baseCurrencyCode, ratesToBdtMap)
    val targetInBase = CurrencyFormatter.convert(goal.targetAmount, goal.currencyCode, baseCurrencyCode, ratesToBdtMap)

    HisabSectionCard(
        contentPadding = 16.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) palette.softBgDark else palette.softBgLight)
                        .border(1.dp, palette.main.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(goal.iconKey),
                        contentDescription = goal.title,
                        tint = if (colors.isDark) palette.main else palette.highContrastTextLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = goal.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary
                    )
                }
            }

            Text(
                text = "${(ratio * 100).toInt()}%",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (colors.isDark) palette.main else palette.highContrastTextLight
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { ratio },
            color = palette.main,
            trackColor = colors.containerInteractive,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isBalanceHidden) "•••• / ••••"
                    else "${CurrencyFormatter.formatCurrency(savedInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)} / ${CurrencyFormatter.formatCurrency(targetInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)}",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.inkPrimary
                )
                Text(
                    text = "Target date: ${CurrencyFormatter.formatDateShort(goal.deadlineMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete goal",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Button(
                    onClick = onContribute,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.main),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_contribute_goal_${goal.id}")
                ) {
                    Text("+ Add Funds", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CategoryManagementRow(
    category: CategoryEntity,
    budgetInBase: Double,
    baseCurrencyCode: String,
    useBangladeshiFormat: Boolean,
    onClick: () -> Unit,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(category.colorHex)
    HisabSectionCard(
        onClick = onClick,
        shapeRadius = 18.dp,
        contentPadding = 14.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) palette.softBgDark else palette.softBgLight)
                        .border(1.dp, palette.main.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(category.iconKey),
                        contentDescription = category.name,
                        tint = if (colors.isDark) palette.main else palette.highContrastTextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = if (category.type == "EXPENSE") {
                            "Monthly Budget: ${CurrencyFormatter.formatCurrency(budgetInBase, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)}"
                        } else {
                            "Income Source"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary
                    )
                }
            }

            if (category.type == "EXPENSE") {
                IconButton(onClick = onEditBudget) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        tint = colors.inkSecondary
                    )
                }
            }
        }
    }
}
