package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AccountEntity
import com.example.data.local.AppNotificationEntity
import com.example.data.local.ScheduledReminderEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.HeaderCircleIconButton
import com.example.ui.components.HisabHeaderSurface
import com.example.ui.components.HisabSectionCard
import com.example.ui.components.StylizedBdtAmount
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HisabUiState
import com.example.ui.viewmodel.ThemePreference
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabHaptics
import java.io.File
import java.io.FileOutputStream

@Composable
fun AccountsScreen(
    uiState: HisabUiState,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onBack: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCurrencyManager: () -> Unit,
    onCreateAccount: (String, String, Double, String, String, Long) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val baseCurrency = uiState.baseCurrencyCode
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedAccountFilter by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
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
                            testTag = "btn_accounts_back",
                            onClick = onBack
                        )
                        Text(
                            text = "Accounts & Wallets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderCircleIconButton(
                            icon = Icons.Default.CurrencyExchange,
                            contentDescription = "Manage Currencies & FX",
                            testTag = "btn_accounts_fx",
                            onClick = onOpenCurrencyManager
                        )
                        HeaderCircleIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add Account",
                            testTag = "btn_add_account",
                            onClick = { showAddAccountDialog = true }
                        )
                        HeaderCircleIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Profile & Settings",
                            testTag = "btn_open_settings",
                            onClick = onOpenSettings
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Combined Net Liquidity ($baseCurrency)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

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
                        mainFontSize = 34.sp,
                        symbolFontSize = 18.sp,
                        decimalFontSize = 24.sp
                    )

                    OutlinedButton(
                        onClick = onOpenTransfer,
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_accounts_transfer")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Transfer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move Funds", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        items(uiState.accounts, key = { it.id }) { account ->
            val isSelected = selectedAccountFilter == account.id
            FintechAccountCard(
                account = account,
                baseCurrencyCode = baseCurrency,
                ratesToBdtMap = uiState.ratesToBdtMap,
                isSelected = isSelected,
                useBangladeshiFormat = useBangladeshiFormat,
                isBalanceHidden = isBalanceHidden,
                onClick = {
                    selectedAccountFilter = if (isSelected) null else account.id
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            val accountTxs = if (selectedAccountFilter == null) {
                uiState.transactions.take(6)
            } else {
                uiState.transactions.filter { it.accountId == selectedAccountFilter || it.toAccountId == selectedAccountFilter }
            }

            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = if (selectedAccountFilter == null) {
                        "Recent Account Activity"
                    } else {
                        val accName = uiState.accounts.find { it.id == selectedAccountFilter }?.name ?: "Account"
                        "$accName Activity"
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    if (selectedAccountFilter != null) {
                        Text(
                            text = "Show All",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { selectedAccountFilter = null }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                accountTxs.forEachIndexed { idx, tx ->
                    TransactionRowItem(
                        transaction = tx,
                        useBangladeshiFormat = useBangladeshiFormat,
                        isBalanceHidden = isBalanceHidden,
                        baseCurrencyCode = baseCurrency,
                        ratesToBdtMap = uiState.ratesToBdtMap,
                        onClick = { onSelectTransaction(tx) }
                    )
                    if (idx < accountTxs.lastIndex) {
                        HorizontalDivider(
                            color = colors.borderSubtle,
                            modifier = Modifier.padding(start = 58.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        PremiumAddAccountModal(
            baseCurrencyCode = baseCurrency,
            ratesToBdtMap = uiState.ratesToBdtMap,
            useBangladeshiFormat = useBangladeshiFormat,
            onDismiss = { showAddAccountDialog = false },
            onCreateAccount = { name, type, bal, currencyCode, masked, colorHex ->
                onCreateAccount(name, type, bal, currencyCode, masked, colorHex)
                showAddAccountDialog = false
            }
        )
    }
}

@Composable
private fun FintechAccountCard(
    account: AccountEntity,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    isSelected: Boolean,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(account.colorHex)
    val isForeign = !account.currencyCode.equals(baseCurrencyCode, ignoreCase = true)
    val convertedBalanceInBase = CurrencyFormatter.convert(
        amount = account.balance,
        fromCurrency = account.currencyCode,
        toCurrency = baseCurrencyCode,
        ratesToBdt = ratesToBdtMap
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = if (isSelected) 6.dp else 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.5.dp, colors.inkPrimary, RoundedCornerShape(24.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            )
            .testTag("account_card_${account.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.main)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.26f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveCategoryIcon(account.iconKey),
                                contentDescription = account.name,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = account.maskedNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.26f)
                        ) {
                            Text(
                                text = account.currencyCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = account.type,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Available Balance (${account.currencyCode})",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.94f)
                )
                Text(
                    text = if (isBalanceHidden) "${CurrencyCatalog.getMetadata(account.currencyCode).symbol} ••••••"
                    else CurrencyFormatter.formatCurrency(
                        amount = account.balance,
                        currencyCode = account.currencyCode,
                        useSouthAsianGrouping = useBangladeshiFormat,
                        includeCodeSuffix = true
                    ),
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    color = Color.White
                )

                if (isForeign && !isBalanceHidden) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "≈ ${
                            CurrencyFormatter.formatCurrency(
                                amount = convertedBalanceInBase,
                                currencyCode = baseCurrencyCode,
                                useSouthAsianGrouping = useBangladeshiFormat,
                                includeCodeSuffix = true
                            )
                        } (${CurrencyFormatter.formatRateString(account.currencyCode, baseCurrencyCode, ratesToBdtMap)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Global Search & Transaction Filter Screen
 */
@Composable
fun SearchAndFilterScreen(
    transactions: List<TransactionEntity>,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    searchQuery: String,
    selectedTypeFilter: String,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val filtered = remember(transactions, searchQuery, selectedTypeFilter) {
        transactions.filter { tx ->
            val matchesType = selectedTypeFilter == "ALL" || tx.type == selectedTypeFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.merchant.lowercase().contains(q) ||
                tx.categoryName.lowercase().contains(q) ||
                tx.accountLabel.lowercase().contains(q) ||
                tx.currencyCode.lowercase().contains(q) ||
                tx.note.lowercase().contains(q)
            matchesType && matchesQuery
        }
    }

    val filteredSumInBase = filtered.sumOf { tx ->
        CurrencyFormatter.convert(tx.amount, tx.currencyCode, baseCurrencyCode, ratesToBdtMap)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item {
            HisabHeaderSurface {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        testTag = "btn_search_back",
                        onClick = onBack
                    )
                    Text(
                        text = "Search & Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(25.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.inkSecondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search merchant, category, currency or note...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.inkTertiary
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.inkPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("global_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.inkSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onQueryChange("") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Income", "TRANSFER" to "Transfers").forEach { (key, label) ->
                        val selected = selectedTypeFilter == key
                        Surface(
                            onClick = { onTypeFilterChange(key) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) colors.inkPrimary else colors.containerInteractive,
                            border = BorderStroke(1.dp, if (selected) colors.inkPrimary else colors.borderStrong),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) colors.cardSurface else colors.inkPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filtered.size} matching records",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Text(
                        text = "Total: ${CurrencyFormatter.formatCurrency(filteredSumInBase, baseCurrencyCode, useBangladeshiFormat)}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.inkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions match your search criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                } else {
                    filtered.forEachIndexed { idx, tx ->
                        TransactionRowItem(
                            transaction = tx,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            baseCurrencyCode = baseCurrencyCode,
                            ratesToBdtMap = ratesToBdtMap,
                            onClick = { onSelectTransaction(tx) }
                        )
                        if (idx < filtered.lastIndex) {
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
}

/**
 * Notifications & Scheduled Local Reminders Center
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotificationEntity>,
    scheduledReminders: List<ScheduledReminderEntity> = emptyList(),
    baseCurrencyCode: String = "BDT",
    ratesToBdtMap: Map<String, Double> = emptyMap(),
    useBangladeshiFormat: Boolean = true,
    onMarkAllRead: () -> Unit,
    onDismiss: (Int) -> Unit,
    onToggleReminder: (ScheduledReminderEntity, Boolean) -> Unit = { _, _ -> },
    onDeleteReminder: (ScheduledReminderEntity) -> Unit = {},
    onCreateCustomReminder: (String, String, Double, String, Int) -> Unit = { _, _, _, _, _ -> },
    onTriggerLiveTestNotification: (ScheduledReminderEntity) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    var showCreateReminderModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
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
                            testTag = "btn_notifications_back",
                            onClick = onBack
                        )
                        Column {
                            Text(
                                text = "Alerts & Reminders",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = "${scheduledReminders.count { it.isEnabled }} scheduled • ${notifications.count { !it.isRead }} unread",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderCircleIconButton(
                            icon = Icons.Default.AlarmAdd,
                            contentDescription = "Schedule Custom Reminder",
                            testTag = "btn_add_custom_reminder",
                            onClick = { showCreateReminderModal = true }
                        )
                        HeaderCircleIconButton(
                            icon = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read",
                            testTag = "btn_mark_all_read",
                            onClick = onMarkAllRead
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // Scheduled Local Reminders Section (Currency-Aware)
        if (scheduledReminders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Local Reminders (Currency-Aware)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    TextButton(onClick = { showCreateReminderModal = true }) {
                        Text("+ Custom", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(scheduledReminders, key = { "rem_${it.id}" }) { reminder ->
                val nativeFormatted = CurrencyFormatter.formatCurrency(
                    amount = reminder.amount,
                    currencyCode = reminder.currencyCode,
                    useSouthAsianGrouping = useBangladeshiFormat,
                    includeCodeSuffix = true
                )
                val convertedSuffix = if (!reminder.currencyCode.equals(baseCurrencyCode, ignoreCase = true) && reminder.amount > 0.0) {
                    val inBase = CurrencyFormatter.convert(
                        reminder.amount,
                        reminder.currencyCode,
                        baseCurrencyCode,
                        ratesToBdtMap
                    )
                    " (≈ ${CurrencyFormatter.formatCurrency(inBase, baseCurrencyCode, useBangladeshiFormat, includeCodeSuffix = true)})"
                } else ""

                HisabSectionCard(
                    contentPadding = 14.dp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkPrimary
                                )
                                if (reminder.amount > 0.0) {
                                    Text(
                                        text = "$nativeFormatted$convertedSuffix",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${reminder.sourceType} • Next: ${CurrencyFormatter.formatDateShort(reminder.triggerTimeMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.inkSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onTriggerLiveTestNotification(reminder) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_fire_reminder_${reminder.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Fire Local Notification Now",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Switch(
                                checked = reminder.isEnabled,
                                onCheckedChange = { onToggleReminder(reminder, it) }
                            )
                            IconButton(
                                onClick = { onDeleteReminder(reminder) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete reminder",
                                    tint = colors.inkTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        item {
            Text(
                text = "Activity & Financial Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.inkPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You're all caught up! No new alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary
                    )
                }
            }
        } else {
            items(notifications, key = { it.id }) { item ->
                val palette = getCategoryPalette(item.accentColorHex)
                HisabSectionCard(
                    contentPadding = 16.dp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
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
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
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
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.inkPrimary
                                    )
                                    if (!item.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(palette.main)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.inkSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${item.categoryTag} • ${CurrencyFormatter.formatDateShort(item.timestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkTertiary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDismiss(item.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateReminderModal) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var currencyCode by remember(baseCurrencyCode) { mutableStateOf(baseCurrencyCode) }
        var daysFromNow by remember { mutableStateOf("3") }
        val currMeta = CurrencyCatalog.getMetadata(currencyCode)

        AlertDialog(
            onDismissRequest = { showCreateReminderModal = false },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Schedule Custom Financial Reminder",
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reminder Currency (ISO 4217):",
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
                                label = { Text("${meta.symbol} ${meta.code}") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Reminder Title") },
                        placeholder = { Text("e.g., Pay Credit Card Statement") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (${currMeta.symbol} ${currMeta.code})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Note / Action Prompt") },
                        placeholder = { Text("e.g., Settle before 5 PM to avoid late fee") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = daysFromNow,
                        onValueChange = { daysFromNow = it.filter { c -> c.isDigit() } },
                        label = { Text("Trigger in Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreateCustomReminder(
                                title,
                                subtitle.ifBlank { "Scheduled financial reminder" },
                                amountStr.toDoubleOrNull() ?: 0.0,
                                currencyCode,
                                daysFromNow.toIntOrNull()?.coerceIn(0, 365) ?: 1
                            )
                            showCreateReminderModal = false
                        }
                    }
                ) {
                    Text("Schedule Reminder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateReminderModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Profile & Settings Screen with Multi-Currency, Security Lock, Smart Local Notifications, Quiet Hours, Haptics, Backup/Restore, CSV & PDF Export
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndSettingsScreen(
    uiState: HisabUiState,
    baseCurrencyCode: String,
    themePreference: ThemePreference,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onOpenCurrencyPicker: () -> Unit,
    onOpenCurrencyManager: () -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onToggleNumberFormat: () -> Unit,
    onToggleBalanceHidden: () -> Unit,
    onConfigureAppLock: (Boolean, String, Boolean) -> Unit,
    onLockNow: () -> Unit,
    onUpdateNotificationPrefs: (Boolean, Boolean) -> Unit,
    onUpdateFullNotificationAndHapticPrefs: (
        budgetAlertsEnabled: Boolean,
        billRemindersEnabled: Boolean,
        debtRemindersEnabled: Boolean,
        goalMilestonesEnabled: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        quietHoursEnabled: Boolean,
        reminderLeadTimeDays: Int,
        hapticsEnabled: Boolean
    ) -> Unit = { budget, bill, _, _, _, _, _, _, _ -> onUpdateNotificationPrefs(budget, bill) },
    onTriggerLiveCurrencyTest: () -> Unit = {},
    onOpenOnboardingTour: () -> Unit = {},
    onGenerateBackupJson: ((String) -> Unit) -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    onGenerateCsv: ((String) -> Unit) -> Unit,
    onImportCsv: (String) -> Unit,
    onExportSummary: () -> Unit,
    onResetData: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = HisabTheme.colors
    val baseMeta = CurrencyCatalog.getMetadata(baseCurrencyCode)

    var showResetConfirmation by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showNotificationRationalePrimer by remember { mutableStateOf(false) }
    var dataDialogMode by remember { mutableStateOf<String?>(null) }
    var dataPayloadText by remember { mutableStateOf("") }

    var hasPostNotificationsPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotificationsPermission = granted
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
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
                            testTag = "btn_profile_back",
                            onClick = onBack
                        )
                        Text(
                            text = "Profile & Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenOnboardingTour,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_reopen_onboarding_tour")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Product Tour", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Arnob R.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "HisabMate Pro • Base Currency: ${baseMeta.code} (${baseMeta.symbol})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // Multi-Currency & Exchange Rate Management Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Base Currency & FX Rates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Active Base Currency: ${baseMeta.code} (${baseMeta.symbol} ${baseMeta.name} — ${baseMeta.countryOrRegion})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenCurrencyPicker,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_settings_change_currency")
                    ) {
                        Text("Change Currency", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenCurrencyManager,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_settings_fx_manager")
                    ) {
                        Text("FX Rates & Converter", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // PIN & Biometric Security Lock Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "PIN & Biometric App Lock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (uiState.isAppLockEnabled) {
                        "App Lock Active (PIN + ${if (uiState.biometricEnabled) "Biometrics Enabled" else "PIN Only"})"
                    } else {
                        "Protect your financial records with a 4-digit PIN and Biometric unlock."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showPinSetupDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_configure_app_lock")
                    ) {
                        Text(
                            text = if (uiState.isAppLockEnabled) "Manage PIN" else "Enable App Lock",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.isAppLockEnabled) {
                        OutlinedButton(
                            onClick = onLockNow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_lock_app_now")
                        ) {
                            Text("Lock App Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Appearance & Theme Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Appearance & Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreference.entries.forEach { pref ->
                        val selected = themePreference == pref
                        Surface(
                            onClick = { onSetTheme(pref) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else colors.containerInteractive,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else colors.borderStrong
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("theme_opt_${pref.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = pref.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Localization, Privacy, Smart Local Notifications & Haptics Card
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Formatting, Privacy, Smart Reminders & Haptics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OS Notification Permission Status Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (hasPostNotificationsPermission) {
                        Color(0xFF0EA568).copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (hasPostNotificationsPermission) Color(0xFF0EA568).copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPostNotificationsPermission) {
                                    "Android Local Notifications: Enabled ✓"
                                } else {
                                    "System Notification Permission Off"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = if (hasPostNotificationsPermission) {
                                    "Offline bill, budget, debt & goal alerts deliver in each item's native currency."
                                } else {
                                    "Enable permission to receive offline bill & budget alerts, or manage in Android Settings."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!hasPostNotificationsPermission) {
                            Button(
                                onClick = { showNotificationRationalePrimer = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_request_notif_permission")
                            ) {
                                Text("Enable", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onTriggerLiveCurrencyTest,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_test_currency_notification")
                            ) {
                                Text("Test Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "South Asian Lakh/Crore Grouping",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Format BDT/INR as 2,48,650.00 instead of 248,650.00",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = useBangladeshiFormat,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onToggleNumberFormat()
                        },
                        modifier = Modifier.testTag("switch_bdt_grouping")
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Mode (Mask Balances)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Hide sensitive financial figures in public places",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = isBalanceHidden,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onToggleBalanceHidden()
                        },
                        modifier = Modifier.testTag("switch_privacy_mask")
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Budget Threshold Alerts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Warn when category spending crosses its configured threshold",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.budgetAlertsEnabled,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onUpdateFullNotificationAndHapticPrefs(
                                it,
                                uiState.billRemindersEnabled,
                                uiState.debtRemindersEnabled,
                                uiState.goalMilestonesEnabled,
                                uiState.soundEnabled,
                                uiState.vibrationEnabled,
                                uiState.quietHoursEnabled,
                                uiState.reminderLeadTimeDays,
                                uiState.hapticsEnabled
                            )
                        }
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recurring Bill Reminders",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Notify before recurring bills & subscriptions in their original currency",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.billRemindersEnabled,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onUpdateFullNotificationAndHapticPrefs(
                                uiState.budgetAlertsEnabled,
                                it,
                                uiState.debtRemindersEnabled,
                                uiState.goalMilestonesEnabled,
                                uiState.soundEnabled,
                                uiState.vibrationEnabled,
                                uiState.quietHoursEnabled,
                                uiState.reminderLeadTimeDays,
                                uiState.hapticsEnabled
                            )
                        }
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Debt & Lending Due Reminders",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Remind before lent/borrowed settlement dates",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.debtRemindersEnabled,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onUpdateFullNotificationAndHapticPrefs(
                                uiState.budgetAlertsEnabled,
                                uiState.billRemindersEnabled,
                                it,
                                uiState.goalMilestonesEnabled,
                                uiState.soundEnabled,
                                uiState.vibrationEnabled,
                                uiState.quietHoursEnabled,
                                uiState.reminderLeadTimeDays,
                                uiState.hapticsEnabled
                            )
                        }
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Savings Goal Milestones (50%, 80%, 100%)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Celebrate savings milestones in each goal's native currency",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.goalMilestonesEnabled,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onUpdateFullNotificationAndHapticPrefs(
                                uiState.budgetAlertsEnabled,
                                uiState.billRemindersEnabled,
                                uiState.debtRemindersEnabled,
                                it,
                                uiState.soundEnabled,
                                uiState.vibrationEnabled,
                                uiState.quietHoursEnabled,
                                uiState.reminderLeadTimeDays,
                                uiState.hapticsEnabled
                            )
                        }
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quiet Hours (22:00 – 07:00)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Deliver overnight reminders silently without sound or vibration",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.quietHoursEnabled,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                            onUpdateFullNotificationAndHapticPrefs(
                                uiState.budgetAlertsEnabled,
                                uiState.billRemindersEnabled,
                                uiState.debtRemindersEnabled,
                                uiState.goalMilestonesEnabled,
                                uiState.soundEnabled,
                                uiState.vibrationEnabled,
                                it,
                                uiState.reminderLeadTimeDays,
                                uiState.hapticsEnabled
                            )
                        }
                    )
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Subtle tactile confirmation on saves, toggles, and presets",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.inkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.hapticsEnabled,
                        onCheckedChange = {
                            HisabHaptics.performSelection(view, it)
                            onUpdateFullNotificationAndHapticPrefs(
                                uiState.budgetAlertsEnabled,
                                uiState.billRemindersEnabled,
                                uiState.debtRemindersEnabled,
                                uiState.goalMilestonesEnabled,
                                uiState.soundEnabled,
                                uiState.vibrationEnabled,
                                uiState.quietHoursEnabled,
                                uiState.reminderLeadTimeDays,
                                it
                            )
                        },
                        modifier = Modifier.testTag("switch_haptics_enabled")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Bill & Debt Reminder Lead Time:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "Same Day", 1 to "1 Day Before", 3 to "3 Days Before").forEach { (days, label) ->
                        val isSel = uiState.reminderLeadTimeDays == days
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                HisabHaptics.performSelection(view, uiState.hapticsEnabled)
                                onUpdateFullNotificationAndHapticPrefs(
                                    uiState.budgetAlertsEnabled,
                                    uiState.billRemindersEnabled,
                                    uiState.debtRemindersEnabled,
                                    uiState.goalMilestonesEnabled,
                                    uiState.soundEnabled,
                                    uiState.vibrationEnabled,
                                    uiState.quietHoursEnabled,
                                    days,
                                    uiState.hapticsEnabled
                                )
                            },
                            label = { Text(label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Data Backup, Restore, CSV & PDF Statement Actions
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Backup, Restore & Export Statements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // PDF Statement Export
                Button(
                    onClick = {
                        val pdfPath = generatePdfStatementFile(
                            context = context,
                            uiState = uiState,
                            useBangladeshiFormat = useBangladeshiFormat
                        )
                        dataPayloadText = "PDF Financial Statement generated at:\n$pdfPath\n\n" +
                            "Base Currency: ${uiState.baseCurrencyCode}\n" +
                            "Total Balance: ${CurrencyFormatter.formatCurrency(uiState.totalBalance, uiState.baseCurrencyCode, useBangladeshiFormat)}\n" +
                            "Monthly Inflow: ${CurrencyFormatter.formatCurrency(uiState.monthlyIncome, uiState.baseCurrencyCode, useBangladeshiFormat)}\n" +
                            "Monthly Outflow: ${CurrencyFormatter.formatCurrency(uiState.monthlyExpense, uiState.baseCurrencyCode, useBangladeshiFormat)}\n" +
                            "Transactions Included: ${uiState.transactions.size}"
                        dataDialogMode = "PDF_READY"
                        onExportSummary()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_export_pdf_statement")
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF Financial Statement ($baseCurrencyCode)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CSV Export & Import Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onGenerateCsv { csv ->
                                val csvFile = File(context.filesDir, "hisabmate_transactions.csv")
                                runCatching { csvFile.writeText(csv) }
                                dataPayloadText = csv
                                dataDialogMode = "CSV_EXPORT"
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_export_csv")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            dataPayloadText = "Title,Merchant,Amount,Currency,Type,Category,Account,Note\n" +
                                "Metrorail Pass,MRT Line-6,500,BDT,EXPENSE,Transport,bKash Personal,Rapid Pass Topup"
                            dataDialogMode = "CSV_IMPORT"
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_import_csv")
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import CSV", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // JSON Full Backup & Restore Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onGenerateBackupJson { json ->
                                val backupFile = File(context.filesDir, "hisabmate_backup.json")
                                runCatching { backupFile.writeText(json) }
                                dataPayloadText = json
                                dataDialogMode = "JSON_BACKUP"
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_backup_json")
                    ) {
                        Text("Backup JSON", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val backupFile = File(context.filesDir, "hisabmate_backup.json")
                            dataPayloadText = if (backupFile.exists()) {
                                runCatching { backupFile.readText() }.getOrDefault("")
                            } else {
                                ""
                            }
                            dataDialogMode = "JSON_RESTORE"
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_restore_json")
                    ) {
                        Text("Restore JSON", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showResetConfirmation = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.expenseText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_reset_sample_data")
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Default Sample Data", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            isAppLockEnabled = uiState.isAppLockEnabled,
            currentPin = uiState.appPinCode,
            biometricEnabled = uiState.biometricEnabled,
            onDismiss = { showPinSetupDialog = false },
            onSave = onConfigureAppLock
        )
    }

    dataDialogMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { dataDialogMode = null },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = when (mode) {
                        "PDF_READY" -> "PDF Statement Generated"
                        "CSV_EXPORT" -> "CSV Ledger Exported"
                        "CSV_IMPORT" -> "Import Transactions from CSV"
                        "JSON_BACKUP" -> "Local JSON Backup Ready"
                        else -> "Restore Database from JSON Backup"
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (mode) {
                            "PDF_READY" -> "Your formatted multi-currency PDF statement has been saved to local storage."
                            "CSV_EXPORT" -> "Saved to hisabmate_transactions.csv. You can copy or inspect the CSV rows below:"
                            "CSV_IMPORT" -> "Paste CSV lines (Title,Merchant,Amount,Currency,Type,Category,Account,Note):"
                            "JSON_BACKUP" -> "Saved to hisabmate_backup.json. Copy or keep this JSON snapshot:"
                            else -> "Paste your HisabMate JSON backup below to restore accounts and transactions:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                    OutlinedTextField(
                        value = dataPayloadText,
                        onValueChange = { dataPayloadText = it },
                        readOnly = mode == "PDF_READY" || mode == "CSV_EXPORT" || mode == "JSON_BACKUP",
                        minLines = 5,
                        maxLines = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (mode) {
                            "CSV_IMPORT" -> onImportCsv(dataPayloadText)
                            "JSON_RESTORE" -> if (dataPayloadText.isNotBlank()) onRestoreBackupJson(dataPayloadText)
                        }
                        dataDialogMode = null
                    }
                ) {
                    Text(
                        text = when (mode) {
                            "CSV_IMPORT" -> "Import CSV Rows"
                            "JSON_RESTORE" -> "Restore Backup"
                            else -> "Done"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (mode == "CSV_IMPORT" || mode == "JSON_RESTORE") {
                    TextButton(onClick = { dataDialogMode = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = colors.cardSurface,
            title = { Text("Restore Sample Data?", color = colors.inkPrimary) },
            text = {
                Text(
                    text = "This will reset all transactions, accounts, budgets, goals, and exchange rates back to the default HisabMate sample dataset.",
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.expenseText)
                ) {
                    Text("Restore Defaults", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationRationalePrimer) {
        AlertDialog(
            onDismissRequest = { showNotificationRationalePrimer = false },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Enable Local Financial Reminders",
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Why HisabMate requests Notification permission:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "• Receive timely offline alerts before recurring bills and debt due dates.\n" +
                            "• Get instant budget threshold warnings (80% & 100%) in each budget's native currency.\n" +
                            "• Celebrate savings goal milestones without any cloud tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationRationalePrimer = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            hasPostNotificationsPermission = true
                        }
                    }
                ) {
                    Text("Allow Notifications", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            showNotificationRationalePrimer = false
                            runCatching {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Text("Open Settings")
                    }
                    TextButton(onClick = { showNotificationRationalePrimer = false }) {
                        Text("Not Now")
                    }
                }
            }
        )
    }
}

private fun generatePdfStatementFile(
    context: android.content.Context,
    uiState: HisabUiState,
    useBangladeshiFormat: Boolean
): String {
    val file = File(context.filesDir, "HisabMate_Statement_${uiState.baseCurrencyCode}.pdf")
    runCatching {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
        }

        var y = 48f
        canvas.drawText("HisabMate — Multi-Currency Financial Statement", 40f, y, titlePaint)
        y += 26f
        canvas.drawText(
            "Base Currency: ${uiState.baseCurrencyCode} | Total Net Worth: ${
                CurrencyFormatter.formatCurrency(uiState.totalBalance, uiState.baseCurrencyCode, useBangladeshiFormat)
            }",
            40f,
            y,
            bodyPaint
        )
        y += 18f
        canvas.drawText(
            "Monthly Inflow: ${
                CurrencyFormatter.formatCurrency(uiState.monthlyIncome, uiState.baseCurrencyCode, useBangladeshiFormat)
            } | Monthly Outflow: ${
                CurrencyFormatter.formatCurrency(uiState.monthlyExpense, uiState.baseCurrencyCode, useBangladeshiFormat)
            }",
            40f,
            y,
            bodyPaint
        )
        y += 28f
        canvas.drawText("Recent Transactions:", 40f, y, titlePaint)
        y += 20f

        uiState.transactions.take(22).forEach { tx ->
            if (y < 790f) {
                val amtStr = CurrencyFormatter.formatCurrency(
                    tx.amount,
                    tx.currencyCode,
                    useBangladeshiFormat,
                    includeCodeSuffix = true
                )
                val line = "${CurrencyFormatter.formatDateShort(tx.timestamp)}  |  ${tx.type}  |  ${tx.title} (${tx.accountLabel})  |  $amtStr"
                canvas.drawText(line.take(92), 40f, y, bodyPaint)
                y += 16f
            }
        }

        pdfDocument.finishPage(page)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
    }
    return file.absolutePath
}
