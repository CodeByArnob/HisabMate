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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.ui.components.HeaderCircleIconButton
import com.example.ui.components.HisabHeaderSurface
import com.example.ui.components.HisabSectionCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.CalendarDaySummary
import com.example.util.CurrencyFormatter
import java.util.Calendar

@Composable
fun TransactionCalendarScreen(
    calendarDays: List<CalendarDaySummary>,
    monthOffset: Int,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onBack: () -> Unit,
    onShiftMonth: (Int) -> Unit,
    onOpenAddTransaction: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val cal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val (monthName, yearStr) = CurrencyFormatter.formatMonthYear(year, month)

    val firstDayCal = remember(year, month) {
        Calendar.getInstance().apply {
            set(year, month, 1)
        }
    }
    val leadingEmptyCells = (firstDayCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceAtLeast(0)

    val defaultSelectedDay = remember(monthOffset, calendarDays) {
        val todayCal = Calendar.getInstance()
        if (monthOffset == 0) {
            todayCal.get(Calendar.DAY_OF_MONTH).coerceAtMost(calendarDays.size.coerceAtLeast(1))
        } else {
            calendarDays.lastOrNull { it.transactions.isNotEmpty() }?.dayOfMonth ?: 1
        }
    }
    var selectedDay by remember(monthOffset) { mutableIntStateOf(defaultSelectedDay) }

    val selectedDaySummary = calendarDays.find { it.dayOfMonth == selectedDay }
    val monthTotalIncome = remember(calendarDays) { calendarDays.sumOf { it.incomeInBase } }
    val monthTotalExpense = remember(calendarDays) { calendarDays.sumOf { it.expenseInBase } }
    val activeDaysCount = remember(calendarDays) { calendarDays.count { it.transactions.isNotEmpty() } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 110.dp)
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
                            testTag = "btn_calendar_back",
                            onClick = onBack
                        )
                        Column {
                            Text(
                                text = "Transaction Calendar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = "$activeDaysCount active days in $monthName $yearStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    HeaderCircleIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add transaction",
                        testTag = "btn_calendar_add_tx",
                        onClick = onOpenAddTransaction
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Month Selector + Monthly Inflow/Outflow summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$monthName $yearStr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { onShiftMonth(-1) },
                            shape = CircleShape,
                            color = colors.containerInteractive,
                            border = BorderStroke(1.dp, colors.borderStrong),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_calendar_prev_month")
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
                            border = BorderStroke(1.dp, colors.borderStrong),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_calendar_next_month")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next month",
                                    tint = if (monthOffset < 0) colors.inkPrimary else colors.inkTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Month Inflow",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatCompactCurrency(
                                    monthTotalIncome,
                                    baseCurrencyCode,
                                    useBangladeshiFormat
                                ),
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.incomeText
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Month Outflow",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatCompactCurrency(
                                    monthTotalExpense,
                                    baseCurrencyCode,
                                    useBangladeshiFormat
                                ),
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.expenseText
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Net Cashflow",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary
                            )
                            val net = monthTotalIncome - monthTotalExpense
                            Text(
                                text = (if (net >= 0) "+" else "") + CurrencyFormatter.formatCompactCurrency(
                                    net,
                                    baseCurrencyCode,
                                    useBangladeshiFormat
                                ),
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (net >= 0) colors.incomeText else colors.expenseText
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Calendar Grid Card
        item {
            HisabSectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekdays.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val totalCells = leadingEmptyCells + calendarDays.size
                val rows = (totalCells + 6) / 7

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - leadingEmptyCells + 1
                            val summary = calendarDays.find { it.dayOfMonth == dayNum }

                            if (summary == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.95f)
                                )
                            } else {
                                val isSelected = summary.dayOfMonth == selectedDay
                                val hasIncome = summary.incomeInBase > 0
                                val hasExpense = summary.expenseInBase > 0

                                Surface(
                                    onClick = { selectedDay = summary.dayOfMonth },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (hasIncome || hasExpense) colors.containerInteractive
                                    else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (hasIncome || hasExpense) colors.borderStrong
                                        else colors.borderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.95f)
                                        .testTag("calendar_day_${summary.dayOfMonth}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${summary.dayOfMonth}",
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else colors.inkPrimary
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (hasIncome) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) Color.White else colors.incomeText
                                                        )
                                                )
                                            }
                                            if (hasExpense) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) Color(0xFFFFD166) else colors.expenseText
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.incomeText))
                        Text(
                            text = "Income logged",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.expenseText))
                        Text(
                            text = "Expense logged",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkSecondary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Selected Day Transactions Card
        item {
            HisabSectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$monthName $selectedDay, $yearStr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        val dayIncome = selectedDaySummary?.incomeInBase ?: 0.0
                        val dayExpense = selectedDaySummary?.expenseInBase ?: 0.0
                        Text(
                            text = "In: ${
                                CurrencyFormatter.formatCurrency(dayIncome, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)
                            }  •  Out: ${
                                CurrencyFormatter.formatCurrency(dayExpense, baseCurrencyCode, useBangladeshiFormat, showDecimals = false)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkSecondary
                        )
                    }

                    TextButton(onClick = onOpenAddTransaction) {
                        Text("+ Add entry", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val dayTxs = selectedDaySummary?.transactions.orEmpty()
                if (dayTxs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded on $monthName $selectedDay.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                    }
                } else {
                    dayTxs.forEachIndexed { idx, tx ->
                        TransactionRowItem(
                            transaction = tx,
                            useBangladeshiFormat = useBangladeshiFormat,
                            isBalanceHidden = isBalanceHidden,
                            baseCurrencyCode = baseCurrencyCode,
                            ratesToBdtMap = ratesToBdtMap,
                            onClick = { onSelectTransaction(tx) }
                        )
                        if (idx < dayTxs.lastIndex) {
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
 * Interactive PIN & Biometric Security Lock Screen Overlay.
 */
@Composable
fun PinLockOverlayScreen(
    biometricEnabled: Boolean,
    onSubmitPin: (String, (Boolean) -> Unit) -> Unit,
    onBiometricUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    fun submitPin(value: String) {
        if (isVerifying) return
        isVerifying = true
        onSubmitPin(value) { verified ->
            isVerifying = false
            if (!verified) {
                errorMessage = "Incorrect PIN. Try again."
                enteredPin = ""
            }
        }
    }

    Surface(
        color = colors.canvasBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("pin_lock_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "HisabMate Security Lock",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your 4-digit security PIN to access your financial records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else colors.containerInteractive
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.expenseText
                    )
                }
            }

            // Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { rowDigits ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowDigits.forEach { digit ->
                            PinKeyButton(
                                label = digit,
                                onClick = {
                                    errorMessage = null
                                    if (enteredPin.length < 4) {
                                        val nextPin = enteredPin + digit
                                        enteredPin = nextPin
                                        if (nextPin.length == 4) {
                                            submitPin(nextPin)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom row: Biometric, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (biometricEnabled) {
                        Surface(
                            onClick = onBiometricUnlock,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("btn_biometric_unlock")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Unlock",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    PinKeyButton(
                        label = "0",
                        onClick = {
                            errorMessage = null
                            if (enteredPin.length < 4) {
                                val nextPin = enteredPin + "0"
                                enteredPin = nextPin
                                if (nextPin.length == 4) {
                                    submitPin(nextPin)
                                }
                            }
                        }
                    )

                    Surface(
                        onClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        shape = CircleShape,
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = HisabTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderStrong),
        modifier = Modifier
            .size(72.dp)
            .testTag("pin_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.inkPrimary
            )
        }
    }
}

@Composable
fun PinSetupDialog(
    isAppLockEnabled: Boolean,
    biometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Boolean) -> Unit
) {
    var enabled by remember { mutableStateOf(isAppLockEnabled) }
    var pinInput by remember { mutableStateOf("") }
    var bioEnabled by remember { mutableStateOf(biometricEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("PIN & Biometric App Lock", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable App Lock", fontWeight = FontWeight.Bold)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(4)
                        pinInput = digits
                    },
                    label = { Text("4-Digit Security PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Fingerprint Unlock", fontWeight = FontWeight.Bold)
                        Text(
                            "Allow instant biometric passkey unlock",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Switch(checked = bioEnabled, onCheckedChange = { bioEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !enabled || pinInput.length == 4,
                onClick = {
                    onSave(enabled, if (enabled) pinInput else "", bioEnabled)
                    onDismiss()
                }
            ) {
                Text("Save Security", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
