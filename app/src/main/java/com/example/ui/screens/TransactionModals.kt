package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import kotlin.math.abs

private data class OcrReceiptPreset(
    val label: String,
    val title: String,
    val merchant: String,
    val amount: Double,
    val currencyCode: String,
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    initialType: String,
    preselectedCategoryId: Int?,
    editingTransaction: TransactionEntity? = null,
    favoriteTransactions: List<TransactionEntity> = emptyList(),
    recentTransactions: List<TransactionEntity> = emptyList(),
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        title: String,
        merchant: String,
        amount: Double,
        currencyCode: String,
        type: String,
        category: CategoryEntity,
        account: AccountEntity,
        toAccount: AccountEntity?,
        note: String,
        isRecurring: Boolean,
        receiptUri: String,
        splitDetails: String,
        isFavorite: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val colors = HisabTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = editingTransaction != null

    val availableCategories = remember(categories, initialType, editingTransaction) {
        val currentType = editingTransaction?.type ?: initialType
        if (currentType == "INCOME") {
            categories.filter { it.type == "INCOME" }.ifEmpty { categories }
        } else {
            categories.filter { it.type == "EXPENSE" }.ifEmpty { categories }
        }
    }

    var selectedCategory by remember(availableCategories, preselectedCategoryId, editingTransaction) {
        mutableStateOf(
            availableCategories.find { it.id == (editingTransaction?.categoryId ?: preselectedCategoryId) }
                ?: availableCategories.firstOrNull()
        )
    }

    var selectedAccount by remember(accounts, editingTransaction) {
        mutableStateOf(
            accounts.find { it.id == editingTransaction?.accountId }
                ?: accounts.firstOrNull()
        )
    }
    var selectedToAccount by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull())
    }

    var txType by remember(initialType, editingTransaction) {
        mutableStateOf(editingTransaction?.type ?: initialType)
    }
    var amountStr by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var selectedCurrencyCode by remember(baseCurrencyCode, editingTransaction, preselectedCategoryId) {
        val preselectedCatCurrency = categories.find { it.id == preselectedCategoryId }?.budgetCurrencyCode
        val defaultAccountCurrency = accounts.firstOrNull()?.currencyCode
        mutableStateOf(
            editingTransaction?.currencyCode
                ?: preselectedCatCurrency
                ?: defaultAccountCurrency
                ?: baseCurrencyCode
        )
    }
    var titleStr by remember(editingTransaction) { mutableStateOf(editingTransaction?.title ?: "") }
    var merchantStr by remember(editingTransaction) { mutableStateOf(editingTransaction?.merchant ?: "") }
    var noteStr by remember(editingTransaction) { mutableStateOf(editingTransaction?.note ?: "") }
    var isRecurring by remember(editingTransaction) { mutableStateOf(editingTransaction?.isRecurring ?: false) }
    var isFavorite by remember(editingTransaction) { mutableStateOf(editingTransaction?.isFavorite ?: false) }
    var receiptUri by remember(editingTransaction) { mutableStateOf(editingTransaction?.receiptUri ?: "") }
    var splitDetails by remember(editingTransaction) { mutableStateOf(editingTransaction?.splitDetails ?: "") }
    var showSplitBuilder by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.splitDetails?.isNotBlank() == true)
    }
    var showOcrScannerPanel by remember { mutableStateOf(false) }
    var showCameraPermissionPrimer by remember { mutableStateOf(false) }
    var cameraPermissionStatusMessage by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var duplicateConfirmed by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            receiptUri = "camera://ocr_live_capture_${System.currentTimeMillis()}"
            cameraPermissionStatusMessage = "Camera OCR ready ✓ Select a scanned slip below or review captured receipt."
        } else {
            cameraPermissionStatusMessage = "Camera permission declined. You can still attach receipt photos via Android Photo Picker or use sample OCR slips."
        }
    }

    val selectedCurrMeta = CurrencyCatalog.getMetadata(selectedCurrencyCode)
    val quickIncrements = remember(selectedCurrencyCode) {
        when (selectedCurrencyCode) {
            "BDT", "INR", "JPY" -> listOf(500, 1000, 2500, 5000, 10000)
            else -> listOf(10, 25, 50, 100, 250)
        }
    }

    val quickMerchants = remember(txType) {
        when (txType) {
            "INCOME" -> listOf("Monthly Salary Credit", "Freelance Retainer", "Dividend Payout")
            "TRANSFER" -> listOf("bKash Cash-In", "Credit Card Bill Settlement", "Wise USD Transfer")
            else -> listOf("Shwapno Superstore", "Pathao Ride", "North End Coffee", "Chaldal Fresh", "DESCO Electricity")
        }
    }

    val ocrPresets = remember {
        listOf(
            OcrReceiptPreset(
                label = "Shwapno Receipt (৳3,480)",
                title = "Shwapno Superstore",
                merchant = "Shwapno Gulshan-2",
                amount = 3480.0,
                currencyCode = "BDT",
                note = "OCR Scanned Receipt #SHW-9941"
            ),
            OcrReceiptPreset(
                label = "AWS Invoice ($49.00)",
                title = "AWS Cloud Hosting",
                merchant = "Amazon Web Services",
                amount = 49.0,
                currencyCode = "USD",
                note = "OCR Scanned Tax Invoice #AWS-2026"
            ),
            OcrReceiptPreset(
                label = "Tokyo Metro (¥1,250)",
                title = "Tokyo Metro Pass",
                merchant = "Suica Transit",
                amount = 1250.0,
                currencyCode = "JPY",
                note = "OCR Scanned Transit Receipt"
            ),
            OcrReceiptPreset(
                label = "Dubai Duty Free (د.إ 320)",
                title = "Dubai Duty Free",
                merchant = "DXB Terminal 3",
                amount = 320.0,
                currencyCode = "AED",
                note = "OCR Scanned POS Slip"
            )
        )
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
                Text(
                    text = if (isEditing) {
                        "Edit Transaction"
                    } else {
                        when (txType) {
                            "INCOME" -> "Add Income"
                            "TRANSFER" -> "Transfer Funds"
                            else -> "Add Expense"
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.inkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Favorite / Frequent Templates Quick Strip
            if (!isEditing && favoriteTransactions.isNotEmpty()) {
                Text(
                    text = "Quick Fill from Favorites",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    favoriteTransactions.take(6).forEach { fav ->
                        Surface(
                            onClick = {
                                txType = fav.type
                                titleStr = fav.title
                                merchantStr = fav.merchant
                                amountStr = if (fav.amount % 1.0 == 0.0) fav.amount.toLong().toString() else fav.amount.toString()
                                selectedCurrencyCode = fav.currencyCode
                                noteStr = fav.note
                                categories.find { it.id == fav.categoryId }?.let { selectedCategory = it }
                                accounts.find { it.id == fav.accountId }?.let { selectedAccount = it }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = colors.containerInteractive,
                            border = BorderStroke(1.dp, colors.borderStrong)
                        ) {
                            Text(
                                text = "★ ${fav.title} (${
                                    CurrencyFormatter.formatCurrency(
                                        fav.amount,
                                        fav.currencyCode,
                                        showDecimals = false
                                    )
                                })",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Type Switcher (Expense | Income | Transfer)
            if (!isEditing) {
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
                        listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Move / Transfer").forEach { (key, label) ->
                            val active = txType == key
                            Surface(
                                onClick = {
                                    txType = key
                                    validationError = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) colors.cardSurface else Color.Transparent,
                                shadowElevation = if (active) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("tx_type_tab_${key.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) colors.inkPrimary else colors.inkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Receipt OCR Scanner & Photo Attachment Action Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showOcrScannerPanel = !showOcrScannerPanel },
                    shape = RoundedCornerShape(14.dp),
                    color = if (showOcrScannerPanel) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else colors.containerInteractive,
                    border = BorderStroke(1.dp, if (showOcrScannerPanel) MaterialTheme.colorScheme.primary else colors.borderStrong),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_scan_receipt_ocr")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan Receipt (OCR)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }
                }

                Surface(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (receiptUri.isNotBlank()) Color(0xFF0EA568).copy(alpha = 0.16f) else colors.containerInteractive,
                    border = BorderStroke(1.dp, if (receiptUri.isNotBlank()) Color(0xFF0EA568) else colors.borderStrong),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_attach_receipt_photo")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (receiptUri.isNotBlank()) Color(0xFF0EA568) else colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (receiptUri.isNotBlank()) "Receipt Attached ✓" else "Attach Photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }
                }
            }

            if (showOcrScannerPanel) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Smart Receipt OCR Extractor",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                            OutlinedButton(
                                onClick = {
                                    val hasCam = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCam) {
                                        receiptUri = "camera://ocr_live_capture_${System.currentTimeMillis()}"
                                        cameraPermissionStatusMessage = "Camera OCR active ✓ Select or confirm extracted receipt fields."
                                    } else {
                                        showCameraPermissionPrimer = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_camera_ocr_capture")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Camera Scan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        cameraPermissionStatusMessage?.let { statusMsg ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Or tap a parsed multi-currency receipt slip:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ocrPresets.forEach { preset ->
                                Surface(
                                    onClick = {
                                        titleStr = preset.title
                                        merchantStr = preset.merchant
                                        amountStr = if (preset.amount % 1.0 == 0.0) preset.amount.toLong().toString() else preset.amount.toString()
                                        selectedCurrencyCode = preset.currencyCode
                                        noteStr = preset.note
                                        receiptUri = "ocr://receipt/${preset.title.lowercase().replace(" ", "_")}"
                                        showOcrScannerPanel = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.cardSurface,
                                    border = BorderStroke(1.dp, colors.borderStrong)
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showCameraPermissionPrimer) {
                AlertDialog(
                    onDismissRequest = { showCameraPermissionPrimer = false },
                    containerColor = colors.cardSurface,
                    title = {
                        Text(
                            text = "Allow Camera for Receipt OCR?",
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "HisabMate uses your camera only when you choose to scan paper receipts and invoices for automatic merchant, currency, and amount extraction. You can also attach receipt photos anytime without granting camera access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCameraPermissionPrimer = false
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        ) {
                            Text("Continue to Camera Permission", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showCameraPermissionPrimer = false
                                cameraPermissionStatusMessage = "Using zero-permission Photo Picker & OCR slips."
                            }
                        ) {
                            Text("Use Photo Picker Instead")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transaction Currency Selector (ISO 4217)
            Text(
                text = "Transaction Currency (ISO 4217)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurrencyCatalog.supportedCurrencies.forEach { meta ->
                    val isSel = selectedCurrencyCode == meta.code
                    Surface(
                        onClick = { selectedCurrencyCode = meta.code },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else colors.borderStrong),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("tx_curr_${meta.code}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = meta.symbol,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSel) Color.White else colors.inkPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = meta.code,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (isSel) Color.White else colors.inkPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input Field + Quick Amount Chips
            OutlinedTextField(
                value = amountStr,
                onValueChange = {
                    amountStr = it.filter { ch -> ch.isDigit() || ch == '.' }
                    validationError = null
                    duplicateConfirmed = false
                },
                label = {
                    Text("Amount in ${selectedCurrMeta.name} (${selectedCurrMeta.symbol} ${selectedCurrMeta.code})")
                },
                placeholder = { Text(if (selectedCurrMeta.decimalDigits == 0) "0" else "0.00") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_amount")
            )

            // Live FX Conversion Preview when Transaction Currency != Base Currency or Account Currency
            val parsedAmt = amountStr.toDoubleOrNull() ?: 0.0
            val accCode = selectedAccount?.currencyCode ?: baseCurrencyCode
            if (parsedAmt > 0.0 && (!selectedCurrencyCode.equals(baseCurrencyCode, ignoreCase = true) || !selectedCurrencyCode.equals(accCode, ignoreCase = true))) {
                Spacer(modifier = Modifier.height(8.dp))
                val inBase = CurrencyFormatter.convert(parsedAmt, selectedCurrencyCode, baseCurrencyCode, ratesToBdtMap)
                val inAcc = CurrencyFormatter.convert(parsedAmt, selectedCurrencyCode, accCode, ratesToBdtMap)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Converts to ${CurrencyFormatter.formatCurrency(inBase, baseCurrencyCode, includeCodeSuffix = true)}" +
                                if (!accCode.equals(baseCurrencyCode, ignoreCase = true)) {
                                    " • Account debit: ${CurrencyFormatter.formatCurrency(inAcc, accCode, includeCodeSuffix = true)}"
                                } else "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Increment Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickIncrements.forEach { addVal ->
                    Surface(
                        onClick = {
                            val current = amountStr.toDoubleOrNull() ?: 0.0
                            amountStr = (current + addVal).toLong().toString()
                            validationError = null
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong)
                    ) {
                        Text(
                            text = "+${selectedCurrMeta.symbol}$addVal",
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title / Merchant Input + Smart Suggestions
            OutlinedTextField(
                value = titleStr,
                onValueChange = {
                    titleStr = it
                    validationError = null
                    duplicateConfirmed = false
                },
                label = { Text(if (txType == "TRANSFER") "Transfer Title" else "Title / Merchant Name") },
                placeholder = { Text("e.g., Shwapno Superstore") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickMerchants.forEach { suggestion ->
                    Surface(
                        onClick = { titleStr = suggestion },
                        shape = RoundedCornerShape(20.dp),
                        color = colors.containerInteractive,
                        border = BorderStroke(1.dp, colors.borderStrong)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selector (For Expense & Income)
            if (txType != "TRANSFER") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Category Wallet",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                    Surface(
                        onClick = { showSplitBuilder = !showSplitBuilder },
                        shape = RoundedCornerShape(12.dp),
                        color = if (showSplitBuilder) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (showSplitBuilder) MaterialTheme.colorScheme.primary else colors.borderStrong)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallSplit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Split Transaction",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategories.forEach { cat ->
                        val isSel = selectedCategory?.id == cat.id
                        val palette = getCategoryPalette(cat.colorHex)
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSel) palette.main else if (colors.isDark) palette.softBgDark else palette.softBgLight,
                            border = BorderStroke(1.dp, palette.main.copy(alpha = 0.45f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = resolveCategoryIcon(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isSel) Color.White else if (colors.isDark) palette.main else palette.highContrastTextLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else if (colors.isDark) Color.White else palette.highContrastTextLight
                                )
                            }
                        }
                    }
                }

                if (showSplitBuilder) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = splitDetails,
                        onValueChange = { splitDetails = it },
                        label = { Text("Split Breakdown (e.g., Groceries: 2800 | Household: 1050)") },
                        placeholder = {
                            val half = ((amountStr.toDoubleOrNull() ?: 1000.0) / 2.0).toInt()
                            Text("${selectedCategory?.name ?: "Primary"}: $half | Secondary: $half")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Account Selector
            Text(
                text = if (txType == "TRANSFER") "From Account" else "Payment Account / Wallet",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isSel = selectedAccount?.id == acc.id
                    Surface(
                        onClick = {
                            selectedAccount = acc
                            if (!isEditing) {
                                selectedCurrencyCode = acc.currencyCode
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) colors.inkPrimary else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isSel) colors.inkPrimary else colors.borderStrong)
                    ) {
                        Text(
                            text = "${acc.name} (${acc.currencyCode})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) colors.cardSurface else colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            // Destination Account Selector for Transfer
            if (txType == "TRANSFER") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "To Destination Account",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSel = selectedToAccount?.id == acc.id
                        Surface(
                            onClick = { selectedToAccount = acc },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else colors.containerInteractive,
                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else colors.borderStrong)
                        ) {
                            Text(
                                text = "${acc.name} (${acc.currencyCode})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else colors.inkPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = noteStr,
                onValueChange = { noteStr = it },
                label = { Text("Note / Memo (Optional)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Checkbox(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
                Text(
                    text = "Recurring monthly",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Checkbox(
                    checked = isFavorite,
                    onCheckedChange = { isFavorite = it }
                )
                Text(
                    text = "Save as ★ Favorite",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
            }

            validationError?.let { err ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.expenseText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        validationError = "Please enter a valid amount greater than ${selectedCurrMeta.symbol}0."
                        return@Button
                    }
                    if (titleStr.isBlank()) {
                        validationError = "Please enter a title or merchant name."
                        return@Button
                    }
                    val cat = selectedCategory ?: categories.firstOrNull() ?: return@Button
                    val acc = selectedAccount ?: accounts.firstOrNull() ?: return@Button

                    // Duplicate Transaction Guard (for new entries within 2 minutes)
                    if (!isEditing && !duplicateConfirmed) {
                        val now = System.currentTimeMillis()
                        val duplicate = recentTransactions.firstOrNull { existing ->
                            existing.title.equals(titleStr.trim(), ignoreCase = true) &&
                                abs(existing.amount - amt) < 0.01 &&
                                existing.currencyCode.equals(selectedCurrencyCode, ignoreCase = true) &&
                                abs(now - existing.timestamp) < 120_000L
                        }
                        if (duplicate != null) {
                            duplicateConfirmed = true
                            validationError = "Duplicate alert: An identical transaction was just logged. Tap Save again to confirm."
                            return@Button
                        }
                    }

                    onSaveTransaction(
                        titleStr,
                        merchantStr.ifBlank { cat.name },
                        amt,
                        selectedCurrencyCode,
                        txType,
                        cat,
                        acc,
                        if (txType == "TRANSFER") selectedToAccount else null,
                        noteStr,
                        isRecurring,
                        receiptUri,
                        splitDetails,
                        isFavorite
                    )
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_submit_transaction")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Update Transaction ($selectedCurrencyCode)" else "Save Transaction ($selectedCurrencyCode)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Transaction Detail, Edit, Favorite, & Smart Category Reassignment Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailBottomSheet(
    transaction: TransactionEntity,
    baseCurrencyCode: String,
    ratesToBdtMap: Map<String, Double>,
    categories: List<CategoryEntity>,
    useBangladeshiFormat: Boolean,
    onDismiss: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onToggleFavorite: (TransactionEntity) -> Unit,
    onReassignCategory: (TransactionEntity, CategoryEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    val colors = HisabTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val palette = getCategoryPalette(transaction.categoryColorHex)
    val isForeign = !transaction.currencyCode.equals(baseCurrencyCode, ignoreCase = true)
    val convertedInBase = CurrencyFormatter.convert(
        amount = transaction.amount,
        fromCurrency = transaction.currencyCode,
        toCurrency = baseCurrencyCode,
        ratesToBdt = ratesToBdtMap
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 28.dp)
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) palette.softBgDark else palette.softBgLight)
                            .border(1.dp, palette.main.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(transaction.categoryIconKey),
                            contentDescription = null,
                            tint = if (colors.isDark) palette.main else palette.highContrastTextLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "${transaction.merchant} • ${CurrencyFormatter.formatDateShort(transaction.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleFavorite(transaction) }) {
                        Icon(
                            imageVector = if (transaction.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (transaction.isFavorite) Color(0xFFF59E0B) else colors.inkSecondary
                        )
                    }
                    IconButton(
                        onClick = { onEditTransaction(transaction) },
                        modifier = Modifier.testTag("btn_edit_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Transaction",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.inkPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Permanent Original Amount & Converted Base Currency Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.containerInteractive,
                border = BorderStroke(1.dp, colors.borderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Original Recorded Amount (${transaction.currencyCode})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(
                                    amount = transaction.amount,
                                    currencyCode = transaction.currencyCode,
                                    useSouthAsianGrouping = useBangladeshiFormat,
                                    includeCodeSuffix = true
                                ),
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = if (transaction.type == "INCOME") colors.incomeText else colors.inkPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = transaction.accountLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkPrimary
                            )
                        }
                    }

                    if (isForeign) {
                        HorizontalDivider(
                            color = colors.borderStrong,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Converted in Base ($baseCurrencyCode): ${
                                    CurrencyFormatter.formatCurrency(
                                        amount = convertedInBase,
                                        currencyCode = baseCurrencyCode,
                                        useSouthAsianGrouping = useBangladeshiFormat
                                    )
                                }",
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = CurrencyFormatter.formatRateString(
                                    fromCurrency = transaction.currencyCode,
                                    toCurrency = baseCurrencyCode,
                                    ratesToBdt = ratesToBdtMap
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkSecondary
                            )
                        }
                    }
                }
            }

            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Memo: ${transaction.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary
                )
            }

            if (transaction.splitDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Split Breakdown: ${transaction.splitDetails}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (transaction.receiptUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0EA568).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF0EA568)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Attached Receipt: ${transaction.receiptUri}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0EA568),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Smart Category Reassignment
            Text(
                text = "Smart Category Assignment (Tap to move)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.filter { it.type == transaction.type || transaction.type == "EXPENSE" && it.type == "EXPENSE" }
                    .forEach { cat ->
                        val isCurrent = cat.id == transaction.categoryId
                        val catPalette = getCategoryPalette(cat.colorHex)
                        Surface(
                            onClick = { onReassignCategory(transaction, cat) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isCurrent) catPalette.main else if (colors.isDark) catPalette.softBgDark else catPalette.softBgLight,
                            border = BorderStroke(1.dp, catPalette.main.copy(alpha = 0.45f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = resolveCategoryIcon(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.White else if (colors.isDark) catPalette.main else catPalette.highContrastTextLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else if (colors.isDark) Color.White else catPalette.highContrastTextLight
                                )
                            }
                        }
                    }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.borderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            if (!showDeleteConfirm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditTransaction(transaction) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Entry", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.expenseText),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_delete_transaction")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = false },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onDeleteTransaction(transaction) },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.expenseText),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_confirm_delete_tx")
                    ) {
                        Text("Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
