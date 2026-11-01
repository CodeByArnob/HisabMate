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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.BaseCurrencyBadgeButton
import com.example.ui.components.CircularQuickActionButton
import com.example.ui.components.HeaderCircleIconButton
import com.example.ui.components.HisabBalanceHeroCard
import com.example.ui.components.HisabHeaderSurface
import com.example.ui.components.HisabSectionCard
import com.example.ui.components.InteractiveSpendLineChart
import com.example.ui.components.MiniWalletFolderIcon
import com.example.ui.components.StylizedBdtAmount
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.WalletFolderCard
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DailySpendPoint
import com.example.ui.viewmodel.HisabUiState
import com.example.ui.viewmodel.PlanningTab
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import java.util.Calendar

@Composable
fun DashboardScreen(
    uiState: HisabUiState,
    isBalanceHidden: Boolean,
    useBangladeshiFormat: Boolean,
    isDarkTheme: Boolean,
    onToggleBalance: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlanningTab: (PlanningTab) -> Unit,
    onOpenWalletDetails: (Int) -> Unit,
    onOpenAddTransaction: (String, Int?) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val baseCurrency = uiState.baseCurrencyCode
    val baseMeta = CurrencyCatalog.getMetadata(baseCurrency)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp)
    ) {
        // 1. Compact, Authoritative Upper Main Balance Card (prominent elevated financial panel)
        item {
            HisabBalanceHeroCard(
                modifier = Modifier.padding(horizontal = 14.dp),
                shapeRadius = 24.dp,
                contentPadding = 18.dp
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeaderCircleIconButton(
                            icon = Icons.Default.Menu,
                            contentDescription = "Profile & Settings",
                            testTag = "btn_profile_menu",
                            onClick = { onNavigate(AppScreen.PROFILE_SETTINGS) }
                        )
                        BaseCurrencyBadgeButton(
                            currencyCode = baseCurrency,
                            onClick = onOpenCurrencyPicker
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCircleIconButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search",
                            testTag = "btn_header_search",
                            onClick = { onNavigate(AppScreen.SEARCH) }
                        )
                        HeaderCircleIconButton(
                            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                            testTag = "btn_header_theme",
                            onClick = onCycleTheme
                        )
                        HeaderCircleIconButton(
                            icon = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            testTag = "btn_header_notifications",
                            badgeCount = uiState.unreadNotificationsCount,
                            onClick = { onNavigate(AppScreen.NOTIFICATIONS) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Balance Label & FX Rates Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Main balance (${baseMeta.code})",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Surface(
                        onClick = { onNavigate(AppScreen.CURRENCY_MANAGER) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                        modifier = Modifier.testTag("btn_open_fx_manager")
                    ) {
                        Text(
                            text = "FX Rates & Converter →",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hero Balance Amount + Compact Visibility Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StylizedBdtAmount(
                        amount = uiState.totalBalance,
                        useBangladeshiFormat = useBangladeshiFormat,
                        isHidden = isBalanceHidden,
                        currencyCode = baseCurrency,
                        mainFontSize = 33.sp,
                        symbolFontSize = 19.sp,
                        decimalFontSize = 23.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Surface(
                        onClick = onToggleBalance,
                        shape = CircleShape,
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .size(38.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_toggle_balance")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle balance visibility",
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(11.dp))

                // Compact 3-Column Financial Strip (Income / Expenses / Net Saved)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricMiniColumn(
                            label = "Income",
                            value = if (isBalanceHidden) "${baseMeta.symbol} ••••"
                            else CurrencyFormatter.formatCompactCurrency(uiState.monthlyIncome, baseCurrency, useBangladeshiFormat),
                            valueColor = colors.incomeText,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(26.dp)
                                .background(colors.borderStrong)
                        )
                        MetricMiniColumn(
                            label = "Expenses",
                            value = if (isBalanceHidden) "${baseMeta.symbol} ••••"
                            else CurrencyFormatter.formatCompactCurrency(uiState.monthlyExpense, baseCurrency, useBangladeshiFormat),
                            valueColor = colors.expenseText,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(26.dp)
                                .background(colors.borderStrong)
                        )
                        MetricMiniColumn(
                            label = "Net Saved",
                            value = if (isBalanceHidden) "••%"
                            else "${uiState.savingsRatePercent}% (${CurrencyFormatter.formatCompactCurrency(uiState.monthlySavings, baseCurrency, useBangladeshiFormat)})",
                            valueColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1.15f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4 Responsive Pill Quick Actions (Add, Move, Send, Details)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularQuickActionButton(
                        icon = Icons.Default.Add,
                        label = "Add",
                        testTag = "quick_action_add",
                        onClick = { onOpenAddTransaction("EXPENSE", null) },
                        modifier = Modifier.weight(1f)
                    )
                    CircularQuickActionButton(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        label = "Move",
                        testTag = "quick_action_move",
                        onClick = { onOpenAddTransaction("TRANSFER", null) },
                        modifier = Modifier.weight(1f)
                    )
                    CircularQuickActionButton(
                        icon = Icons.AutoMirrored.Filled.Send,
                        label = "Send",
                        testTag = "quick_action_send",
                        onClick = { onOpenPlanningTab(PlanningTab.DEBT_LEND) },
                        modifier = Modifier.weight(1f)
                    )
                    CircularQuickActionButton(
                        icon = Icons.Default.MoreHoriz,
                        label = "Details",
                        testTag = "quick_action_details",
                        onClick = { onNavigate(AppScreen.SPEND_ANALYSIS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 2. Spacious, Harmoniously Proportioned Quick Actions & Budget Categories Section
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 14.dp),
                shapeRadius = 22.dp,
                contentPadding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budgets & categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onNavigate(AppScreen.CALENDAR) },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.containerInteractive,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.testTag("btn_open_calendar")
                        ) {
                            Text(
                                text = "Calendar",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            onClick = { onOpenPlanningTab(PlanningTab.CATEGORIES) },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.containerInteractive,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.testTag("btn_edit_quick_wallets")
                        ) {
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val wallets = uiState.quickWalletSummaries
                if (wallets.size >= 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletFolderCard(
                            summary = wallets[0],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(wallets[0].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                        WalletFolderCard(
                            summary = wallets[1],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(wallets[1].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletFolderCard(
                            summary = wallets[2],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(wallets[2].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                        WalletFolderCard(
                            summary = wallets[3],
                            baseCurrencyCode = baseCurrency,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onOpenWalletDetails(wallets[3].category.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Summary Cards for Budgets, Bills, and Goals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardModuleChip(
                        title = "Budgets",
                        subtitle = "${uiState.categorySummaries.count { it.budgetLimitInBase > 0 }} active",
                        colorHex = 0xFF7C5CFFL,
                        onClick = { onOpenPlanningTab(PlanningTab.BUDGETS) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleChip(
                        title = "Bills",
                        subtitle = "${uiState.recurringPayments.size} upcoming",
                        colorHex = 0xFFFF6E1AL,
                        onClick = { onOpenPlanningTab(PlanningTab.RECURRING) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleChip(
                        title = "Goals",
                        subtitle = "${uiState.savingsGoals.size} pots",
                        colorHex = 0xFF0EA568L,
                        onClick = { onOpenPlanningTab(PlanningTab.GOALS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Frequent / Favorite Transaction Shortcuts
                if (uiState.favoriteTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Favorite shortcuts",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Tap to log",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.favoriteTransactions.take(3).forEach { fav ->
                            val favFormatted = CurrencyFormatter.formatCurrency(
                                amount = fav.amount,
                                currencyCode = fav.currencyCode,
                                useSouthAsianGrouping = useBangladeshiFormat,
                                showDecimals = false
                            )
                            Surface(
                                onClick = { onOpenAddTransaction(fav.type, fav.categoryId) },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.containerInteractive,
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = "★ ${fav.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.inkPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = favFormatted,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 3. Latest Transactions Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 14.dp),
                shapeRadius = 22.dp,
                contentPadding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Surface(
                        onClick = { onNavigate(AppScreen.SEARCH) },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.testTag("btn_see_all_transactions")
                    ) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val recentTxs = uiState.transactions.take(7)
                if (recentTxs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Tap + to log your first entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                } else {
                    recentTxs.forEachIndexed { index, tx ->
                        TransactionRowItem(
                            transaction = tx,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            baseCurrencyCode = baseCurrency,
                            ratesToBdtMap = uiState.ratesToBdtMap,
                            onClick = { onSelectTransaction(tx) }
                        )
                        if (index < recentTxs.lastIndex) {
                            HorizontalDivider(
                                color = colors.borderSubtle,
                                modifier = Modifier.padding(start = 58.dp)
                            )
                        }
                    }
                }
            }
        }

        // Scroll-safe bottom clearance for the floating navigation dock and Android system navigation bar
        item {
            Spacer(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(86.dp)
            )
        }
    }
}

@Composable
private fun MetricMiniColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.inkSecondary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = valueColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashboardModuleChip(
    title: String,
    subtitle: String,
    colorHex: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(colorHex)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (colors.isDark) palette.softBgDark else palette.softBgLight,
        border = BorderStroke(1.dp, palette.main.copy(alpha = if (colors.isDark) 0.34f else 0.18f)),
        modifier = modifier.height(66.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.main)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (colors.isDark) Color.White else palette.highContrastTextLight,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Wallet / Category Details Screen with WCAG AA contrast and multi-currency conversion.
 */
@Composable
fun WalletDetailsScreen(
    category: CategoryEntity,
    categorySpentInBase: Double,
    budgetLimitInBase: Double,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    transactions: List<TransactionEntity>,
    dailyPoints: List<DailySpendPoint>,
    monthOffset: Int,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onBack: () -> Unit,
    onShiftMonth: (Int) -> Unit,
    onOpenAddForCategory: (Int) -> Unit,
    onOpenTransfer: () -> Unit,
    onUpdateBudget: (CategoryEntity, Double, String, String, Int, Long, Long) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(category.colorHex)
    var localSearchQuery by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, monthOffset)
    val (monthName, yearStr) = CurrencyFormatter.formatMonthYear(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH)
    )

    val filteredTransactions = remember(transactions, localSearchQuery) {
        if (localSearchQuery.isBlank()) {
            transactions
        } else {
            val q = localSearchQuery.trim().lowercase()
            transactions.filter {
                it.title.lowercase().contains(q) ||
                    it.merchant.lowercase().contains(q) ||
                    it.accountLabel.lowercase().contains(q) ||
                    it.note.lowercase().contains(q) ||
                    it.currencyCode.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // 1. Top Wallet Header Card
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
                                testTag = "btn_wallet_back",
                                onClick = onBack
                            )
                            Text(
                                text = "Wallet details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeaderCircleIconButton(
                                icon = Icons.Default.TrackChanges,
                                contentDescription = "Edit Budget",
                                testTag = "btn_wallet_edit_budget",
                                onClick = { showBudgetDialog = true }
                            )
                            HeaderCircleIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = "Add Expense to Category",
                                testTag = "btn_wallet_top_add",
                                onClick = { onOpenAddForCategory(category.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Miniature 3D Folder Icon + Category Title + Balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MiniWalletFolderIcon(
                            colorHex = category.colorHex,
                            iconKey = category.iconKey
                        )

                        Column {
                            Text(
                                text = "${category.name} (${baseCurrencyCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StylizedBdtAmount(
                                amount = categorySpentInBase,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isHidden = isBalanceHidden,
                                currencyCode = baseCurrencyCode,
                                mainFontSize = 30.sp,
                                symbolFontSize = 16.sp,
                                decimalFontSize = 22.sp
                            )
                        }
                    }

                    if (budgetLimitInBase > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val ratio = (categorySpentInBase / budgetLimitInBase).toFloat().coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly budget limit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = "${(ratio * 100).toInt()}% of ${
                                    CurrencyFormatter.formatCurrency(
                                        amount = budgetLimitInBase,
                                        currencyCode = baseCurrencyCode,
                                        useSouthAsianGrouping = useBangladeshiFormat,
                                        showDecimals = false
                                    )
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (ratio > 0.9f) colors.expenseText else palette.main
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (ratio > 0.9f) colors.expenseText else palette.main,
                            trackColor = colors.containerInteractive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 2. Interactive Chart Section
            item {
                HisabSectionCard(
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$monthName, ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = yearStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { onShiftMonth(-1) },
                                shape = CircleShape,
                                color = colors.containerInteractive,
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_prev_month")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous month",
                                        tint = colors.inkPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = { if (monthOffset < 0) onShiftMonth(1) },
                                enabled = monthOffset < 0,
                                shape = CircleShape,
                                color = colors.containerInteractive,
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_next_month")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next month",
                                        tint = if (monthOffset < 0) colors.inkPrimary
                                        else colors.inkTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val avgDaily = categorySpentInBase / 15.0
                    Row(verticalAlignment = Alignment.Bottom) {
                        StylizedBdtAmount(
                            amount = avgDaily,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isHidden = isBalanceHidden,
                            currencyCode = baseCurrencyCode,
                            mainFontSize = 24.sp,
                            symbolFontSize = 14.sp,
                            decimalFontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Avg / active day",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    InteractiveSpendLineChart(
                        points = dailyPoints,
                        accentColor = palette.main,
                        useBangladeshiFormat = useBangladeshiFormat,
                        currencyCode = baseCurrencyCode
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 3. Search Bar & Filtered Category Transactions
            item {
                HisabSectionCard(
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderSubtle),
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
                                if (localSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search for any transaction",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.inkTertiary
                                    )
                                }
                                BasicTextField(
                                    value = localSearchQuery,
                                    onValueChange = { localSearchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.inkPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("wallet_search_input")
                                )
                            }
                            if (localSearchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = colors.inkSecondary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { localSearchQuery = "" }
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

                    if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching transactions in ${category.name}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = colors.inkSecondary
                            )
                        }
                    } else {
                        filteredTransactions.forEachIndexed { idx, tx ->
                            TransactionRowItem(
                                transaction = tx,
                                useBangladeshiFormat = useBangladeshiFormat,
                                isBalanceHidden = isBalanceHidden,
                                baseCurrencyCode = baseCurrencyCode,
                                ratesToBdtMap = ratesToBdtMap,
                                onClick = { onSelectTransaction(tx) }
                            )
                            if (idx < filteredTransactions.lastIndex) {
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

        // Bottom Floating Obsidian Action Pills (+ Add | ⇄ Move | 🎯 Budget)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingDarkPillAction(
                icon = Icons.Default.Add,
                label = "Add",
                onClick = { onOpenAddForCategory(category.id) },
                modifier = Modifier.weight(1f)
            )
            FloatingDarkPillAction(
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                label = "Move",
                onClick = onOpenTransfer,
                modifier = Modifier.weight(1f)
            )
            FloatingDarkPillAction(
                icon = Icons.Default.Edit,
                label = "Budget",
                onClick = { showBudgetDialog = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showBudgetDialog) {
        PremiumBudgetModal(
            initialCategory = category,
            allExpenseCategories = listOf(category),
            currentSpentInBase = categorySpentInBase,
            baseCurrencyCode = baseCurrencyCode,
            ratesToBdtMap = ratesToBdtMap,
            useBangladeshiFormat = useBangladeshiFormat,
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { updatedCat, newAmt, period, currCode, thresholdPct, startMs, endMs ->
                onUpdateBudget(updatedCat, newAmt, period, currCode, thresholdPct, startMs, endMs)
            }
        )
    }
}

@Composable
private fun FloatingDarkPillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        color = colors.dockBackground,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 10.dp,
        modifier = modifier
            .height(50.dp)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
