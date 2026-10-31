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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExchangeRateEntity
import com.example.ui.components.HeaderCircleIconButton
import com.example.ui.components.HisabHeaderSurface
import com.example.ui.components.HisabSectionCard
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.HisabUiState
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.CurrencyMetadata

@Composable
fun CurrencyManagementScreen(
    uiState: HisabUiState,
    useBangladeshiFormat: Boolean,
    onSelectBaseCurrency: (String) -> Unit,
    onUpdateManualRate: (String, Double) -> Unit,
    onResetRates: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val baseMeta = CurrencyCatalog.getMetadata(uiState.baseCurrencyCode)

    var showBaseCurrencyPicker by remember { mutableStateOf(false) }
    var editingCurrencyMeta by remember { mutableStateOf<CurrencyMetadata?>(null) }

    // Interactive FX Converter Calculator State
    var converterAmount by remember { mutableStateOf("100") }
    var converterFromCode by remember { mutableStateOf(if (uiState.baseCurrencyCode == "USD") "BDT" else "USD") }
    var converterToCode by remember(uiState.baseCurrencyCode) { mutableStateOf(uiState.baseCurrencyCode) }
    var pickingConverterSide by remember { mutableStateOf<String?>(null) } // "FROM" or "TO"

    val parsedAmount = converterAmount.toDoubleOrNull() ?: 0.0
    val convertedResult = CurrencyFormatter.convert(
        amount = parsedAmount,
        fromCurrency = converterFromCode,
        toCurrency = converterToCode,
        ratesToBdt = uiState.ratesToBdtMap
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        // 1. Header + Base Currency Overview
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
                            testTag = "btn_currency_back",
                            onClick = onBack
                        )
                        Text(
                            text = "Currencies & FX Rates",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    HeaderCircleIconButton(
                        icon = Icons.Default.RestartAlt,
                        contentDescription = "Reset default rates",
                        testTag = "btn_reset_rates",
                        onClick = onResetRates
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Active Base Currency Banner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = baseMeta.symbol,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "Active Base Currency",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkSecondary
                                )
                                Text(
                                    text = "${baseMeta.code} • ${baseMeta.name} (${baseMeta.symbol})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = "${baseMeta.countryOrRegion} • ${baseMeta.decimalDigits} decimals",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.inkSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showBaseCurrencyPicker = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_change_base_currency")
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dashboard totals, budgets, and analytics convert into ${baseMeta.code}. Original transaction currencies and amounts are permanently preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.inkSecondary
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 2. Interactive Currency Converter Calculator
        item {
            HisabSectionCard(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Offline FX Converter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }

                    Surface(
                        onClick = {
                            val tmp = converterFromCode
                            converterFromCode = converterToCode
                            converterToCode = tmp
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        modifier = Modifier.testTag("btn_swap_converter_currencies")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap currencies",
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Swap",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = converterAmount,
                        onValueChange = { converterAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount ($converterFromCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_converter_amount")
                    )

                    Surface(
                        onClick = { pickingConverterSide = "FROM" },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "${CurrencyCatalog.getMetadata(converterFromCode).symbol} $converterFromCode",
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.inkPrimary
                            )
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Converted Result",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(
                                    amount = convertedResult,
                                    currencyCode = converterToCode,
                                    useSouthAsianGrouping = useBangladeshiFormat,
                                    includeCodeSuffix = true
                                ),
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = CurrencyFormatter.formatRateString(
                                    fromCurrency = converterFromCode,
                                    toCurrency = converterToCode,
                                    ratesToBdt = uiState.ratesToBdtMap
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { pickingConverterSide = "TO" },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "To: $converterToCode",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 3. Exchange Rate Table Header
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ISO 4217 Exchange Rates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Updated ${CurrencyFormatter.formatDateShort(uiState.lastRateUpdateMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inkSecondary
                    )
                }
                Text(
                    text = "Offline-first cached & manual rates. Tap any currency row to set a custom exchange rate.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.inkSecondary
                )
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }

        // 4. All Supported Currencies & Rates List
        items(CurrencyCatalog.supportedCurrencies, key = { it.code }) { meta ->
            val rateEntity: ExchangeRateEntity? = uiState.exchangeRates.find {
                it.currencyCode.equals(meta.code, ignoreCase = true)
            }
            val rateToBdt = rateEntity?.rateToBdt ?: meta.defaultRateToBdt
            val isBase = meta.code.equals(uiState.baseCurrencyCode, ignoreCase = true)

            HisabSectionCard(
                onClick = {
                    if (meta.code != "BDT") {
                        editingCurrencyMeta = meta
                    }
                },
                contentPadding = 14.dp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
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
                                .background(
                                    if (isBase) MaterialTheme.colorScheme.primary
                                    else colors.containerInteractive
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = meta.symbol,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isBase) Color.White else colors.inkPrimary
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${meta.code} • ${meta.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkPrimary
                                )
                                if (isBase) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "BASE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${meta.countryOrRegion} • ${meta.decimalDigits} dec • ${rateEntity?.sourceLabel ?: "Offline Verified Rate"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatRateString(
                                fromCurrency = meta.code,
                                toCurrency = uiState.baseCurrencyCode,
                                ratesToBdt = uiState.ratesToBdtMap
                            ),
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.inkPrimary
                        )
                        if (meta.code != "BDT") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "1 ${meta.code} = ৳${String.format(java.util.Locale.US, "%.2f", rateToBdt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.inkSecondary
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit rate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Reference Anchor (1.00)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBaseCurrencyPicker) {
        CurrencyPickerBottomSheet(
            selectedCurrencyCode = uiState.baseCurrencyCode,
            ratesToBdtMap = uiState.ratesToBdtMap,
            title = "Select Base Currency",
            subtitle = "All dashboard summaries, budgets, and reports will convert into this currency.",
            onSelectCurrency = { code ->
                onSelectBaseCurrency(code)
                showBaseCurrencyPicker = false
            },
            onDismiss = { showBaseCurrencyPicker = false }
        )
    }

    pickingConverterSide?.let { side ->
        CurrencyPickerBottomSheet(
            selectedCurrencyCode = if (side == "FROM") converterFromCode else converterToCode,
            ratesToBdtMap = uiState.ratesToBdtMap,
            title = if (side == "FROM") "Convert From Currency" else "Convert To Currency",
            subtitle = "Choose any ISO 4217 currency",
            onSelectCurrency = { code ->
                if (side == "FROM") converterFromCode = code else converterToCode = code
                pickingConverterSide = null
            },
            onDismiss = { pickingConverterSide = null }
        )
    }

    editingCurrencyMeta?.let { meta ->
        val currentRate = uiState.ratesToBdtMap[meta.code] ?: meta.defaultRateToBdt
        var rateInput by remember(meta) { mutableStateOf(currentRate.toString()) }
        AlertDialog(
            onDismissRequest = { editingCurrencyMeta = null },
            title = { Text("Set Manual Exchange Rate (${meta.code})") },
            text = {
                Column {
                    Text(
                        text = "Specify how many Bangladeshi Taka (৳ BDT) equal 1 ${meta.code} (${meta.symbol} ${meta.name}):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("1 ${meta.code} in BDT (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = rateInput.toDoubleOrNull() ?: currentRate
                        if (parsed > 0.0) {
                            onUpdateManualRate(meta.code, parsed)
                            editingCurrencyMeta = null
                        }
                    }
                ) {
                    Text("Save Custom Rate")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCurrencyMeta = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable Searchable ISO 4217 Currency Selector Modal Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    selectedCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    title: String = "Select Currency",
    subtitle: String = "Search by currency name, ISO 4217 code, symbol, or country",
    onSelectCurrency: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = HisabTheme.colors
    var query by remember { mutableStateOf("") }

    val filteredCurrencies = remember(query) {
        if (query.isBlank()) {
            CurrencyCatalog.supportedCurrencies
        } else {
            val q = query.trim().lowercase()
            CurrencyCatalog.supportedCurrencies.filter {
                it.code.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.countryOrRegion.lowercase().contains(q) ||
                    it.symbol.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.containerInteractive,
                border = BorderStroke(1.dp, colors.borderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search currency",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search BDT, USD, EUR, GBP, CNY, SAR, JPY...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkTertiary
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.inkPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("currency_picker_search")
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = colors.inkSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { query = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCurrencies, key = { it.code }) { meta ->
                    val isSelected = meta.code.equals(selectedCurrencyCode, ignoreCase = true)
                    val rateToBdt = ratesToBdtMap[meta.code] ?: meta.defaultRateToBdt

                    Surface(
                        onClick = { onSelectCurrency(meta.code) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else colors.containerInteractive,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else colors.borderStrong
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_option_${meta.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else colors.cardSurface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = meta.symbol,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else colors.inkPrimary
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${meta.code} (${meta.symbol}) — ${meta.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.inkPrimary
                                    )
                                    Text(
                                        text = "${meta.countryOrRegion} • 1 ${meta.code} = ৳${String.format(java.util.Locale.US, "%.2f", rateToBdt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.inkSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * First-Launch / Onboarding Base Currency Selection Dialog.
 */
@Composable
fun BaseCurrencyOnboardingDialog(
    currentBaseCurrency: String,
    onConfirmCurrency: (String) -> Unit
) {
    val colors = HisabTheme.colors
    var chosenCode by remember(currentBaseCurrency) { mutableStateOf(currentBaseCurrency) }

    AlertDialog(
        onDismissRequest = { onConfirmCurrency(chosenCode) },
        containerColor = colors.cardSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Choose Your Base Currency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select your primary reporting currency for HisabMate. Individual accounts and transactions can still use any global currency, and you can switch this anytime in Settings or the top header.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    CurrencyCatalog.supportedCurrencies.take(6).forEach { meta ->
                        val isSel = chosenCode == meta.code
                        Surface(
                            onClick = { chosenCode = meta.code },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else colors.containerInteractive,
                            border = BorderStroke(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) MaterialTheme.colorScheme.primary else colors.borderStrong
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboard_currency_${meta.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${meta.symbol}  ${meta.code} • ${meta.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkPrimary
                                )
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmCurrency(chosenCode) },
                modifier = Modifier.testTag("btn_confirm_onboarding_currency")
            ) {
                Text("Continue with $chosenCode", fontWeight = FontWeight.Bold)
            }
        }
    )
}
