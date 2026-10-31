package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabHaptics
import com.example.util.HisabNotificationManager

/**
 * Complete Multi-Step Interactive Onboarding & Animated Brand Experience for HisabMate.
 * Delegates to [CinematicHisabOnboardingFlow] for the 7-stage interactive fintech onboarding journey.
 */
@Composable
fun HisabOnboardingFlowScreen(
    currentBaseCurrency: String,
    onCompleteOnboarding: (
        selectedCurrencyCode: String,
        languageCode: String,
        regionCode: String,
        notificationsEnabled: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    CinematicHisabOnboardingFlow(
        currentBaseCurrency = currentBaseCurrency,
        onCompleteOnboarding = onCompleteOnboarding,
        modifier = modifier
    )
}


// =========================================================================
// PREMIUM ADD ACCOUNT / WALLET MODAL (Bank, MFS, Card, Cash)
// =========================================================================

private data class AccountInstitutionPreset(
    val label: String,
    val defaultName: String,
    val accountType: String,
    val currencyCode: String,
    val maskedIdentifier: String,
    val colorHex: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PremiumAddAccountModal(
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    useBangladeshiFormat: Boolean,
    onDismiss: () -> Unit,
    onCreateAccount: (
        name: String,
        type: String,
        balance: Double,
        currencyCode: String,
        maskedNumber: String,
        colorHex: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = HisabTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf("MFS") }
    var accountName by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var selectedCurrency by remember(baseCurrencyCode) { mutableStateOf(baseCurrencyCode) }
    var identifierText by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableLongStateOf(0xFFE2136EL) }
    var showCurrencySearch by remember { mutableStateOf(false) }
    var currencySearchQuery by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presets = remember {
        listOf(
            AccountInstitutionPreset("bKash", "bKash Personal Wallet", "MFS", "BDT", "bKash •• 9012", 0xFFE2136EL),
            AccountInstitutionPreset("Nagad", "Nagad Personal Wallet", "MFS", "BDT", "Nagad •• 4410", 0xFFF26419L),
            AccountInstitutionPreset("Rocket", "Rocket DBBL Wallet", "MFS", "BDT", "Rocket •• 712", 0xFF8B5CF6L),
            AccountInstitutionPreset("BRAC Bank", "BRAC Bank Savings", "BANK", "BDT", "A/C •••• 4821", 0xFF6D4AFFL),
            AccountInstitutionPreset("City Amex", "City Bank Amex Gold", "CARD", "BDT", "Card •••• 1234", 0xFF0084E9L),
            AccountInstitutionPreset("Wise USD", "Wise Global USD", "BANK", "USD", "USD •••• 7740", 0xFF059669L),
            AccountInstitutionPreset("Revolut EUR", "Revolut Euro Vault", "CARD", "EUR", "EUR •••• 3091", 0xFF0891B2L),
            AccountInstitutionPreset("Cash Wallet", "Liquid Cash in Hand", "CASH", "BDT", "Physical Cash", 0xFF059669L)
        )
    }

    val accountTypes = remember {
        listOf(
            Triple("MFS", "MFS Wallet", Icons.Default.Smartphone),
            Triple("BANK", "Bank A/C", Icons.Default.AccountBalance),
            Triple("CARD", "Credit/Debit", Icons.Default.CreditCard),
            Triple("CASH", "Cash Wallet", Icons.Default.Payments)
        )
    }

    val colorSwatches = remember {
        listOf(
            0xFFE2136EL, // bKash Magenta
            0xFF6D4AFFL, // Royal Violet
            0xFF0084E9L, // Cobalt Blue
            0xFF059669L, // Emerald Green
            0xFFF26419L, // Nagad Tangerine
            0xFF0891B2L, // Deep Cyan
            0xFF8B5CF6L, // Rocket Purple
            0xFF1E293BL  // Obsidian Slate
        )
    }

    val currMeta = CurrencyCatalog.getMetadata(selectedCurrency)
    val parsedBalance = balanceText.toDoubleOrNull() ?: 0.0
    val quickBalanceChips = remember(selectedCurrency) {
        when (selectedCurrency) {
            "BDT", "INR", "JPY" -> listOf(5000, 15000, 50000, 100000)
            else -> listOf(100, 500, 1000, 2500)
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Account or Wallet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Configure Bank, Card, bKash/Nagad/Rocket or Foreign Currency Wallet",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inkSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.inkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live 3D Fintech Account Card Preview
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(selectedColorHex),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_account_live_preview_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(selectedColorHex),
                                    Color(selectedColorHex).copy(alpha = 0.84f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$selectedType • ${currMeta.code} (${currMeta.symbol})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = identifierText.ifBlank {
                                when (selectedType) {
                                    "MFS" -> "$selectedType •• 9012"
                                    "CARD" -> "Card •••• 4821"
                                    "CASH" -> "Liquid Cash"
                                    else -> "A/C •••• 4821"
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = accountName.ifBlank { "New ${currMeta.code} Wallet" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            amount = parsedBalance,
                            currencyCode = selectedCurrency,
                            useSouthAsianGrouping = useBangladeshiFormat,
                            includeCodeSuffix = true
                        ),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White
                    )

                    if (!selectedCurrency.equals(baseCurrencyCode, ignoreCase = true) && parsedBalance > 0.0) {
                        val inBase = CurrencyFormatter.convert(parsedBalance, selectedCurrency, baseCurrencyCode, ratesToBdtMap)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "≈ ${CurrencyFormatter.formatCurrency(inBase, baseCurrencyCode, useBangladeshiFormat, includeCodeSuffix = true)} in Base Currency",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Institution & MFS Presets
            Text(
                text = "Quick Wallet & Bank Presets",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            accountName = preset.defaultName
                            selectedType = preset.accountType
                            selectedCurrency = preset.currencyCode
                            identifierText = preset.maskedIdentifier
                            selectedColorHex = preset.colorHex
                            validationError = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(preset.colorHex).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(preset.colorHex).copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("preset_account_${preset.label.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.colorHex))
                            )
                            Text(
                                text = "${preset.label} (${preset.currencyCode})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Responsive Account-Type Selector
            Text(
                text = "Account Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accountTypes.forEach { (typeKey, typeLabel, typeIcon) ->
                    val isSelected = selectedType == typeKey
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            selectedType = typeKey
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colors.inkPrimary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isSelected) colors.inkPrimary else colors.borderStrong),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("account_type_chip_${typeKey.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = typeLabel,
                                tint = if (isSelected) colors.cardSurface else colors.inkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) colors.cardSurface else colors.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Searchable ISO 4217 Account Currency Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Native Currency (${currMeta.symbol} ${currMeta.code})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                TextButton(
                    onClick = { showCurrencySearch = !showCurrencySearch }
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showCurrencySearch) "Hide Search" else "Search 14 Currencies",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showCurrencySearch) {
                OutlinedTextField(
                    value = currencySearchQuery,
                    onValueChange = { currencySearchQuery = it },
                    label = { Text("Search ISO code, country or symbol") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val displayedCurrencies = remember(currencySearchQuery) {
                val q = currencySearchQuery.trim().lowercase()
                if (q.isEmpty()) CurrencyCatalog.supportedCurrencies
                else CurrencyCatalog.supportedCurrencies.filter {
                    it.code.lowercase().contains(q) ||
                        it.name.lowercase().contains(q) ||
                        it.countryOrRegion.lowercase().contains(q) ||
                        it.symbol.lowercase().contains(q)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedCurrencies.forEach { meta ->
                    val isCurrSelected = selectedCurrency == meta.code
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            selectedCurrency = meta.code
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrSelected) MaterialTheme.colorScheme.primary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isCurrSelected) MaterialTheme.colorScheme.primary else colors.borderStrong),
                        modifier = Modifier.testTag("add_acc_curr_${meta.code}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = meta.symbol,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (isCurrSelected) Color.White else colors.inkPrimary
                            )
                            Text(
                                text = meta.code,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = if (isCurrSelected) Color.White else colors.inkPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Account / Wallet Name
            OutlinedTextField(
                value = accountName,
                onValueChange = {
                    accountName = it
                    validationError = null
                },
                label = { Text("Account or Wallet Name") },
                placeholder = { Text("e.g., bKash Personal / BRAC Bank Visa / Wise USD") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_name")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Opening Balance Input (Currency-aware, never hardcoded BDT)
            OutlinedTextField(
                value = balanceText,
                onValueChange = {
                    balanceText = it.filter { ch -> ch.isDigit() || ch == '.' }
                    validationError = null
                },
                label = {
                    Text("Opening Balance in ${currMeta.name} (${currMeta.symbol} ${currMeta.code})")
                },
                placeholder = { Text(if (currMeta.decimalDigits == 0) "0" else "0.00") },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_balance")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Opening Balance Chips in the Selected Currency
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickBalanceChips.forEach { presetAmt ->
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            balanceText = presetAmt.toString()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong)
                    ) {
                        Text(
                            text = "${currMeta.symbol}$presetAmt ${currMeta.code}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Masked Identifier
            OutlinedTextField(
                value = identifierText,
                onValueChange = { identifierText = it },
                label = { Text("Account / Card / MFS Identifier (Optional)") },
                placeholder = { Text("e.g., Card •••• 4821 or bKash •• 9012") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_identifier")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Card Accent Color Swatches
            Text(
                text = "Card Theme Accent",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorSwatches.forEach { hex ->
                    val isSwatchSelected = selectedColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (isSwatchSelected) 3.dp else 1.dp,
                                color = if (isSwatchSelected) colors.inkPrimary else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable {
                                HisabHaptics.performSelection(view)
                                selectedColorHex = hex
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSwatchSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            validationError?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.expenseText
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (accountName.isBlank()) {
                        HisabHaptics.performError(context, view)
                        validationError = "Please enter an account or wallet name (or tap a preset above)."
                        return@Button
                    }
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val finalIdentifier = identifierText.ifBlank {
                        when (selectedType) {
                            "MFS" -> "MFS •• 9012"
                            "CARD" -> "Card •••• 4821"
                            "CASH" -> "Liquid Cash"
                            else -> "A/C •••• 4821"
                        }
                    }
                    HisabHaptics.performSuccess(context, view)
                    onCreateAccount(
                        accountName.trim(),
                        selectedType,
                        bal,
                        selectedCurrency,
                        finalIdentifier,
                        selectedColorHex
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_confirm_add_account")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save ${currMeta.code} Account (${currMeta.symbol})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// =========================================================================
// PREMIUM BUDGET CREATION & UPDATE MODAL (Daily, Weekly, Monthly, Yearly, Custom)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PremiumBudgetModal(
    initialCategory: CategoryEntity,
    allExpenseCategories: List<CategoryEntity> = listOf(initialCategory),
    currentSpentInBase: Double,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    useBangladeshiFormat: Boolean,
    onDismiss: () -> Unit,
    onSaveBudget: (
        category: CategoryEntity,
        newBudgetAmount: Double,
        budgetPeriod: String,
        budgetCurrencyCode: String,
        alertThresholdPercent: Int,
        startDateMillis: Long,
        endDateMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = HisabTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var selectedCurrency by remember(selectedCategory, baseCurrencyCode) {
        mutableStateOf(selectedCategory.budgetCurrencyCode.ifBlank { baseCurrencyCode })
    }
    var selectedPeriod by remember(selectedCategory) {
        mutableStateOf(selectedCategory.budgetPeriod.ifBlank { "MONTHLY" }.uppercase())
    }
    var budgetAmountText by remember(selectedCategory) {
        val initialVal = selectedCategory.monthlyBudget.takeIf { it > 0 } ?: 15000.0
        mutableStateOf(if (initialVal % 1.0 == 0.0) initialVal.toLong().toString() else initialVal.toString())
    }
    var thresholdFloat by remember(selectedCategory) {
        mutableFloatStateOf(selectedCategory.alertThresholdPercent.coerceIn(50, 100).toFloat())
    }
    var customDurationDays by remember { mutableIntStateOf(45) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val currMeta = CurrencyCatalog.getMetadata(selectedCurrency)
    val palette = getCategoryPalette(selectedCategory.colorHex)

    // Convert current category spend into the selected budget currency for accurate live preview
    val currentSpentInBudgetCurrency = remember(currentSpentInBase, baseCurrencyCode, selectedCurrency, ratesToBdtMap) {
        CurrencyFormatter.convert(
            amount = currentSpentInBase,
            fromCurrency = baseCurrencyCode,
            toCurrency = selectedCurrency,
            ratesToBdt = ratesToBdtMap
        )
    }

    val newLimitVal = budgetAmountText.toDoubleOrNull() ?: 0.0
    val previewRatio = if (newLimitVal > 0.0) {
        (currentSpentInBudgetCurrency / newLimitVal).toFloat().coerceIn(0f, 1f)
    } else 0f
    val previewPercent = if (newLimitVal > 0.0) {
        ((currentSpentInBudgetCurrency / newLimitVal) * 100.0).toInt()
    } else 0
    val thresholdInt = thresholdFloat.toInt().coerceIn(50, 100)

    val quickBudgetPresets = remember(selectedCurrency, selectedPeriod) {
        val base = when (selectedCurrency) {
            "BDT", "INR", "JPY" -> listOf(5000, 15000, 30000, 50000)
            else -> listOf(100, 300, 600, 1200)
        }
        when (selectedPeriod) {
            "DAILY" -> base.map { (it / 15).coerceAtLeast(10) }
            "WEEKLY" -> base.map { (it / 4).coerceAtLeast(25) }
            "YEARLY" -> base.map { it * 10 }
            else -> base
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Configure ${selectedCategory.name} Budget",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Period, currency-aware cap & threshold notification",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inkSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.inkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Budget Progress Preview Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.containerInteractive,
                border = BorderStroke(1.2.dp, palette.main.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_modal_live_preview")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(palette.main),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = resolveCategoryIcon(selectedCategory.iconKey),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "${selectedCategory.name} • $selectedPeriod",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = "Alert trigger at $thresholdInt% utilization",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.inkSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (previewPercent >= thresholdInt) colors.expenseContainer else palette.main.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$previewPercent% used",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (previewPercent >= thresholdInt) colors.expenseText else palette.main,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${
                                CurrencyFormatter.formatCurrency(
                                    amount = currentSpentInBudgetCurrency,
                                    currencyCode = selectedCurrency,
                                    useSouthAsianGrouping = useBangladeshiFormat
                                )
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "New Limit: ${
                                CurrencyFormatter.formatCurrency(
                                    amount = newLimitVal,
                                    currencyCode = selectedCurrency,
                                    useSouthAsianGrouping = useBangladeshiFormat,
                                    includeCodeSuffix = true
                                )
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.main
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { previewRatio },
                        color = if (previewPercent >= 100) colors.expenseText else palette.main,
                        trackColor = colors.cardSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            // Category Switcher if multiple categories are provided
            if (allExpenseCategories.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Category Wallet",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allExpenseCategories.forEach { cat ->
                        val isCatSel = cat.id == selectedCategory.id
                        val catPal = getCategoryPalette(cat.colorHex)
                        Surface(
                            onClick = {
                                HisabHaptics.performSelection(view)
                                selectedCategory = cat
                                selectedCurrency = cat.budgetCurrencyCode.ifBlank { baseCurrencyCode }
                                selectedPeriod = cat.budgetPeriod.ifBlank { "MONTHLY" }.uppercase()
                                budgetAmountText = cat.monthlyBudget.toLong().toString()
                                thresholdFloat = cat.alertThresholdPercent.toFloat()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCatSel) catPal.main else colors.containerInteractive,
                            border = BorderStroke(1.dp, if (isCatSel) catPal.main else colors.borderStrong)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = resolveCategoryIcon(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isCatSel) Color.White else colors.inkPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCatSel) Color.White else colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Period Selector (Daily, Weekly, Monthly, Yearly, Custom)
            Text(
                text = "Budget Period",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "DAILY" to "Daily",
                    "WEEKLY" to "Weekly",
                    "MONTHLY" to "Monthly",
                    "YEARLY" to "Yearly",
                    "CUSTOM" to "Custom Range"
                ).forEach { (key, label) ->
                    val isPeriodSel = selectedPeriod == key
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            selectedPeriod = key
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPeriodSel) colors.inkPrimary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isPeriodSel) colors.inkPrimary else colors.borderStrong),
                        modifier = Modifier.testTag("budget_period_${key.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPeriodSel) colors.cardSurface else colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (selectedPeriod == "CUSTOM") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(14 to "14 Days", 45 to "45 Days", 60 to "60 Days", 90 to "90 Days").forEach { (days, label) ->
                        val isDaysSel = customDurationDays == days
                        Surface(
                            onClick = { customDurationDays = days },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDaysSel) MaterialTheme.colorScheme.primary else colors.containerInteractive,
                            border = BorderStroke(1.dp, if (isDaysSel) MaterialTheme.colorScheme.primary else colors.borderStrong),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaysSel) Color.White else colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Budget Currency Selector (ISO 4217 — Never hardcoded BDT)
            Text(
                text = "Budget Currency (${currMeta.symbol} ${currMeta.code})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrencyCatalog.supportedCurrencies.forEach { meta ->
                    val isCurrSel = selectedCurrency == meta.code
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            selectedCurrency = meta.code
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrSel) MaterialTheme.colorScheme.primary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isCurrSel) MaterialTheme.colorScheme.primary else colors.borderStrong),
                        modifier = Modifier.testTag("budget_curr_${meta.code}")
                    ) {
                        Text(
                            text = "${meta.symbol} ${meta.code}",
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (isCurrSel) Color.White else colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Spending Limit Input Field
            OutlinedTextField(
                value = budgetAmountText,
                onValueChange = {
                    budgetAmountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                    validationError = null
                },
                label = {
                    Text("$selectedPeriod Spending Limit (${currMeta.symbol} ${currMeta.code})")
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_budget_limit")
            )

            if (!selectedCurrency.equals(baseCurrencyCode, ignoreCase = true) && newLimitVal > 0.0) {
                val limitInBase = CurrencyFormatter.convert(newLimitVal, selectedCurrency, baseCurrencyCode, ratesToBdtMap)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Equivalent in Base Currency: ${
                        CurrencyFormatter.formatCurrency(
                            amount = limitInBase,
                            currencyCode = baseCurrencyCode,
                            useSouthAsianGrouping = useBangladeshiFormat,
                            includeCodeSuffix = true
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Budget Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickBudgetPresets.forEach { presetVal ->
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            budgetAmountText = presetVal.toString()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong)
                    ) {
                        Text(
                            text = "${currMeta.symbol}$presetVal ${currMeta.code}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Alert Threshold Slider & Quick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification Alert Threshold",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                Text(
                    text = "Notify at $thresholdInt%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = thresholdFloat,
                onValueChange = { thresholdFloat = it },
                valueRange = 50f..100f,
                steps = 9
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(60, 75, 80, 90, 100).forEach { pct ->
                    val isPctSel = thresholdInt == pct
                    Surface(
                        onClick = {
                            HisabHaptics.performSelection(view)
                            thresholdFloat = pct.toFloat()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPctSel) MaterialTheme.colorScheme.primary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isPctSel) MaterialTheme.colorScheme.primary else colors.borderStrong),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPctSel) Color.White else colors.inkPrimary
                            )
                        }
                    }
                }
            }

            validationError?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.expenseText
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = colors.borderSubtle)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val parsed = budgetAmountText.toDoubleOrNull() ?: 0.0
                    if (parsed <= 0.0) {
                        HisabHaptics.performError(context, view)
                        validationError = "Please enter a budget limit greater than ${currMeta.symbol}0."
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val durationDays = when (selectedPeriod) {
                        "DAILY" -> 1L
                        "WEEKLY" -> 7L
                        "YEARLY" -> 365L
                        "CUSTOM" -> customDurationDays.toLong()
                        else -> 30L
                    }
                    val endMillis = now + durationDays * 24L * 60L * 60L * 1000L
                    HisabHaptics.performSuccess(context, view)
                    onSaveBudget(
                        selectedCategory,
                        parsed,
                        selectedPeriod,
                        selectedCurrency,
                        thresholdInt,
                        now,
                        endMillis
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_confirm_save_budget")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save $selectedPeriod Budget (${currMeta.symbol} ${currMeta.code})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
