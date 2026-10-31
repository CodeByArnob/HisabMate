package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.ui.components.BaseCurrencyBadgeButton
import com.example.ui.components.HeaderCircleIconButton
import com.example.ui.components.HisabHeaderSurface
import com.example.ui.components.HisabSectionCard
import com.example.ui.components.MonthlyCashflowBarChart
import com.example.ui.components.MultiSegmentSpendBar
import com.example.ui.components.SmartCategoryBanner
import com.example.ui.components.StylizedBdtAmount
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.ui.viewmodel.CategorySpendSummary
import com.example.ui.viewmodel.HisabUiState
import com.example.ui.viewmodel.MonthlyCashflowPoint
import com.example.ui.viewmodel.ReportPeriod
import com.example.util.CurrencyFormatter

@Composable
fun SpendAnalysisScreen(
    uiState: HisabUiState,
    cashflowPoints: List<MonthlyCashflowPoint>,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onBack: () -> Unit,
    onSelectReportPeriod: (ReportPeriod) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onOpenWalletDetails: (Int) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val baseCurrency = uiState.baseCurrencyCode
    var showCashflowChart by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val filteredTxs = remember(uiState.transactions, searchText) {
        if (searchText.isBlank()) {
            uiState.transactions
        } else {
            val q = searchText.trim().lowercase()
            uiState.transactions.filter {
                it.title.lowercase().contains(q) ||
                    it.categoryName.lowercase().contains(q) ||
                    it.merchant.lowercase().contains(q) ||
                    it.accountLabel.lowercase().contains(q) ||
                    it.currencyCode.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        // 1. Spend Analysis Main Card
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
                            testTag = "btn_analysis_back",
                            onClick = onBack
                        )
                        Text(
                            text = "Spend analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCircleIconButton(
                            icon = Icons.Default.CalendarMonth,
                            contentDescription = "Transaction Calendar",
                            testTag = "btn_analysis_calendar",
                            onClick = onOpenCalendar
                        )
                        BaseCurrencyBadgeButton(
                            currencyCode = baseCurrency,
                            onClick = onOpenCurrencyPicker,
                            testTag = "btn_analysis_currency"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily / Weekly / Monthly / Yearly Report Period Selector
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReportPeriod.entries.forEach { period ->
                            val isSelected = uiState.reportPeriod == period
                            Surface(
                                onClick = { onSelectReportPeriod(period) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colors.cardSurface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("report_period_${period.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.inkPrimary else colors.inkSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total Spending Label + Hero Amount + Chart Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.reportPeriod.label} spending ($baseCurrency)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inkSecondary
                    )
                    Text(
                        text = "Inflow: ${
                            CurrencyFormatter.formatCompactCurrency(
                                uiState.periodReportIncome,
                                baseCurrency,
                                useBangladeshiFormat
                            )
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.incomeText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StylizedBdtAmount(
                        amount = uiState.periodReportExpense,
                        useBangladeshiFormat = useBangladeshiFormat,
                        isHidden = isBalanceHidden,
                        currencyCode = baseCurrency,
                        mainFontSize = 36.sp,
                        symbolFontSize = 20.sp,
                        decimalFontSize = 26.sp
                    )

                    HeaderCircleIconButton(
                        icon = if (showCashflowChart) Icons.Default.PieChart else Icons.Default.BarChart,
                        contentDescription = "Toggle cashflow chart",
                        testTag = "btn_toggle_chart_mode",
                        onClick = { showCashflowChart = !showCashflowChart }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Multi-Segment Proportional Horizontal Spend Bar
                MultiSegmentSpendBar(summaries = uiState.categorySummaries)

                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 Category Breakdown Grid
                val topFour = uiState.categorySummaries.take(4)
                if (topFour.size >= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpendBreakdownGridCard(
                            summary = topFour[0],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(topFour[0].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                        SpendBreakdownGridCard(
                            summary = topFour[1],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(topFour[1].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (topFour.size >= 4) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpendBreakdownGridCard(
                            summary = topFour[2],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(topFour[2].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                        SpendBreakdownGridCard(
                            summary = topFour[3],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(topFour[3].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Additional categories horizontal pills if more than 4
                if (uiState.categorySummaries.size > 4) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.categorySummaries.drop(4).take(3).forEach { extra ->
                            val palette = getCategoryPalette(extra.category.colorHex)
                            Surface(
                                onClick = { onOpenWalletDetails(extra.category.id) },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.containerInteractive,
                                border = BorderStroke(1.dp, colors.borderStrong),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = extra.category.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colors.isDark) palette.main else palette.highContrastTextLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBalanceHidden) "••••"
                                        else CurrencyFormatter.formatCompactCurrency(
                                            amount = extra.totalSpentInBase,
                                            currencyCode = baseCurrency,
                                            useSouthAsianGrouping = useBangladeshiFormat
                                        ),
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.inkPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SmartCategoryBanner(
                    title = "Smart category & FX conversion",
                    description = "All foreign-currency transactions are converted into $baseCurrency using your active exchange rate table while preserving original amounts."
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 2. 6-Month Cashflow Comparison Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "6-Month Cashflow Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Income vs Expense comparison ($baseCurrency)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = Color(0xFF0EA568), label = "Income")
                        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Expense")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                MonthlyCashflowBarChart(
                    points = cashflowPoints,
                    useBangladeshiFormat = useBangladeshiFormat
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 3. Search Bar & Latest Transactions Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.inkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchText.isEmpty()) {
                                Text(
                                    text = "Search for any transaction",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.inkTertiary
                                )
                            }
                            BasicTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.inkPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("analysis_search_input")
                            )
                        }
                        if (searchText.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.inkSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchText = "" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Latest transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                filteredTxs.take(10).forEachIndexed { idx, tx ->
                    TransactionRowItem(
                        transaction = tx,
                        useBangladeshiFormat = useBangladeshiFormat,
                        isBalanceHidden = isBalanceHidden,
                        baseCurrencyCode = baseCurrency,
                        ratesToBdtMap = uiState.ratesToBdtMap,
                        onClick = { onSelectTransaction(tx) }
                    )
                    if (idx < filteredTxs.take(10).lastIndex) {
                        HorizontalDivider(
                            color = colors.borderSubtle,
                            modifier = Modifier.padding(start = 58.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendBreakdownGridCard(
    summary: CategorySpendSummary,
    baseCurrencyCode: String,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(summary.category.colorHex)
    val icon = resolveCategoryIcon(summary.category.iconKey)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurfaceElevated,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        modifier = modifier.testTag("analysis_cat_card_${summary.category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = summary.category.name,
                    tint = palette.main,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = summary.category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) palette.main else palette.highContrastTextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isBalanceHidden) "••••"
                else CurrencyFormatter.formatCurrency(
                    amount = summary.totalSpentInBase,
                    currencyCode = baseCurrencyCode,
                    useSouthAsianGrouping = useBangladeshiFormat
                ),
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = HisabTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.inkSecondary
        )
    }
}
