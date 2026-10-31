package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.HisabUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportResult(
    val isSuccess: Boolean,
    val fileUri: Uri? = null,
    val filePath: String? = null,
    val fileName: String = "",
    val fileSizeFormatted: String = "",
    val byteCount: Long = 0L,
    val errorMessage: String? = null
)

object PdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 width in PostScript points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in PostScript points
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    suspend fun generateAndSaveStatementPdf(
        context: Context,
        uiState: HisabUiState,
        useBangladeshiFormat: Boolean
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "HisabMate_Statement_${uiState.baseCurrencyCode}_$timestampStr.pdf"

        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val internalFile = File(exportDir, fileName)

        var isGenerated = false
        try {
            val pdfDocument = PdfDocument()

            // Paint styles
            val primaryPaint = Paint().apply {
                color = Color.rgb(17, 24, 39) // Gray 900
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(75, 85, 99) // Gray 600
                textSize = 10f
                isAntiAlias = true
            }

            val sectionHeaderPaint = Paint().apply {
                color = Color.rgb(31, 41, 55)
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val tableHeaderBgPaint = Paint().apply {
                color = Color.rgb(109, 74, 255) // Royal Violet primary
                style = Paint.Style.FILL
            }

            val rowBgEvenPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            val rowBgOddPaint = Paint().apply {
                color = Color.rgb(249, 250, 251) // Gray 50
                style = Paint.Style.FILL
            }

            val kpiCardBgPaint = Paint().apply {
                color = Color.rgb(243, 244, 246) // Gray 100
                style = Paint.Style.FILL
            }

            val textRegularPaint = Paint().apply {
                color = Color.rgb(31, 41, 55)
                textSize = 9f
                isAntiAlias = true
            }

            val textBoldPaint = Paint().apply {
                color = Color.rgb(17, 24, 39)
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val textIncomePaint = Paint().apply {
                color = Color.rgb(5, 150, 105) // Emerald 600
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val textExpensePaint = Paint().apply {
                color = Color.rgb(220, 38, 38) // Crimson 600
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val dividerPaint = Paint().apply {
                color = Color.rgb(229, 231, 235) // Gray 200
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            val footerPaint = Paint().apply {
                color = Color.rgb(156, 163, 175) // Gray 400
                textSize = 8.5f
                isAntiAlias = true
            }

            var currentPageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Page 1 Header
            var y = 50f
            canvas.drawText("HisabMate", MARGIN, y, primaryPaint)
            y += 16f
            val nowFormatted = SimpleDateFormat("MMMM dd, yyyy  hh:mm a", Locale.US).format(Date())
            canvas.drawText("Official Financial Audit Statement  |  Generated $nowFormatted", MARGIN, y, subtitlePaint)
            y += 14f
            canvas.drawText("Base Currency: ${uiState.baseCurrencyCode}  |  Classification: Personal & Multi-Currency Ledger", MARGIN, y, subtitlePaint)
            y += 18f
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, dividerPaint)
            y += 18f

            // Executive Summary KPIs
            val netLiquidityStr = CurrencyFormatter.formatCurrency(
                amount = uiState.totalBalance,
                currencyCode = uiState.baseCurrencyCode,
                useSouthAsianGrouping = useBangladeshiFormat,
                includeCodeSuffix = true
            )
            val monthlyInflowStr = CurrencyFormatter.formatCurrency(
                amount = uiState.monthlyIncome,
                currencyCode = uiState.baseCurrencyCode,
                useSouthAsianGrouping = useBangladeshiFormat,
                includeCodeSuffix = true
            )
            val monthlyOutflowStr = CurrencyFormatter.formatCurrency(
                amount = uiState.monthlyExpense,
                currencyCode = uiState.baseCurrencyCode,
                useSouthAsianGrouping = useBangladeshiFormat,
                includeCodeSuffix = true
            )
            val monthlySavingsStr = CurrencyFormatter.formatCurrency(
                amount = uiState.monthlySavings,
                currencyCode = uiState.baseCurrencyCode,
                useSouthAsianGrouping = useBangladeshiFormat,
                includeCodeSuffix = true
            )

            // KPI Cards row
            val cardWidth = (CONTENT_WIDTH - 24f) / 3f
            val cardHeight = 52f

            // Card 1: Total Net Worth
            val card1Rect = RectF(MARGIN, y, MARGIN + cardWidth, y + cardHeight)
            canvas.drawRoundRect(card1Rect, 8f, 8f, kpiCardBgPaint)
            canvas.drawText("NET LIQUIDITY", MARGIN + 10f, y + 18f, subtitlePaint)
            canvas.drawText(netLiquidityStr, MARGIN + 10f, y + 38f, textBoldPaint)

            // Card 2: Monthly Inflow
            val card2X = MARGIN + cardWidth + 12f
            val card2Rect = RectF(card2X, y, card2X + cardWidth, y + cardHeight)
            canvas.drawRoundRect(card2Rect, 8f, 8f, kpiCardBgPaint)
            canvas.drawText("MONTHLY INFLOW", card2X + 10f, y + 18f, subtitlePaint)
            canvas.drawText(monthlyInflowStr, card2X + 10f, y + 38f, textIncomePaint)

            // Card 3: Monthly Outflow
            val card3X = card2X + cardWidth + 12f
            val card3Rect = RectF(card3X, y, card3X + cardWidth, y + cardHeight)
            canvas.drawRoundRect(card3Rect, 8f, 8f, kpiCardBgPaint)
            canvas.drawText("MONTHLY OUTFLOW", card3X + 10f, y + 18f, subtitlePaint)
            canvas.drawText(monthlyOutflowStr, card3X + 10f, y + 38f, textExpensePaint)

            y += cardHeight + 20f

            // Accounts & Wallets Breakdown Table
            canvas.drawText("Accounts & Native FX Balances", MARGIN, y, sectionHeaderPaint)
            y += 12f

            val accHeaderH = 22f
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + accHeaderH), 4f, 4f, tableHeaderBgPaint)
            canvas.drawText("Account / Institution", MARGIN + 8f, y + 15f, tableHeaderPaint)
            canvas.drawText("Type", MARGIN + 180f, y + 15f, tableHeaderPaint)
            canvas.drawText("Native Balance", MARGIN + 260f, y + 15f, tableHeaderPaint)
            canvas.drawText("In ${uiState.baseCurrencyCode} (Converted)", MARGIN + 380f, y + 15f, tableHeaderPaint)
            y += accHeaderH + 2f

            val rowH = 18f
            uiState.accounts.take(6).forEachIndexed { index, acc ->
                val bg = if (index % 2 == 0) rowBgEvenPaint else rowBgOddPaint
                canvas.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH), bg)

                canvas.drawText(acc.name.take(28), MARGIN + 8f, y + 13f, textRegularPaint)
                canvas.drawText(acc.type, MARGIN + 180f, y + 13f, textRegularPaint)

                val nativeStr = CurrencyFormatter.formatCurrency(
                    amount = acc.balance,
                    currencyCode = acc.currencyCode,
                    useSouthAsianGrouping = useBangladeshiFormat,
                    includeCodeSuffix = true
                )
                canvas.drawText(nativeStr, MARGIN + 260f, y + 13f, textBoldPaint)

                val convertedInBase = CurrencyFormatter.convert(
                    amount = acc.balance,
                    fromCurrency = acc.currencyCode,
                    toCurrency = uiState.baseCurrencyCode,
                    ratesToBdt = uiState.ratesToBdtMap
                )
                val convStr = CurrencyFormatter.formatCurrency(
                    amount = convertedInBase,
                    currencyCode = uiState.baseCurrencyCode,
                    useSouthAsianGrouping = useBangladeshiFormat,
                    includeCodeSuffix = true
                )
                canvas.drawText(convStr, MARGIN + 380f, y + 13f, textBoldPaint)

                y += rowH
            }

            y += 24f

            // Transactions Table Header
            fun drawTransactionTableHeader(startY: Float): Float {
                canvas.drawText("Transaction Ledger & Audit Trail", MARGIN, startY, sectionHeaderPaint)
                val hY = startY + 12f
                canvas.drawRoundRect(RectF(MARGIN, hY, MARGIN + CONTENT_WIDTH, hY + accHeaderH), 4f, 4f, tableHeaderBgPaint)
                canvas.drawText("Date", MARGIN + 8f, hY + 15f, tableHeaderPaint)
                canvas.drawText("Description / Merchant", MARGIN + 75f, hY + 15f, tableHeaderPaint)
                canvas.drawText("Category", MARGIN + 225f, hY + 15f, tableHeaderPaint)
                canvas.drawText("Account", MARGIN + 315f, hY + 15f, tableHeaderPaint)
                canvas.drawText("Amount", MARGIN + 420f, hY + 15f, tableHeaderPaint)
                return hY + accHeaderH + 2f
            }

            y = drawTransactionTableHeader(y)

            fun drawFooter(pageNum: Int) {
                val footerY = PAGE_HEIGHT - 24f
                canvas.drawLine(MARGIN, footerY - 10f, MARGIN + CONTENT_WIDTH, footerY - 10f, dividerPaint)
                canvas.drawText("HisabMate Verified Financial Statement  |  Zero Cloud Spying Ledger", MARGIN, footerY, footerPaint)
                val pageText = "Page $pageNum"
                val pWidth = footerPaint.measureText(pageText)
                canvas.drawText(pageText, MARGIN + CONTENT_WIDTH - pWidth, footerY, footerPaint)
            }

            // Iterate transactions with multi-page handling
            val txList = uiState.transactions
            val txRowH = 20f

            for ((index, tx) in txList.withIndex()) {
                if (y + txRowH > PAGE_HEIGHT - 45f) {
                    drawFooter(currentPageNum)
                    pdfDocument.finishPage(page)

                    currentPageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    y = 50f
                    canvas.drawText("HisabMate Financial Statement (Continued)", MARGIN, y, primaryPaint)
                    y += 18f
                    y = drawTransactionTableHeader(y)
                }

                val rowBg = if (index % 2 == 0) rowBgEvenPaint else rowBgOddPaint
                canvas.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + txRowH), rowBg)

                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.US).format(Date(tx.timestamp))
                canvas.drawText(dateStr, MARGIN + 8f, y + 14f, textRegularPaint)

                val titleDisplay = if (tx.merchant.isNotBlank() && tx.merchant != tx.title) {
                    "${tx.title} (${tx.merchant})".take(32)
                } else {
                    tx.title.take(32)
                }
                canvas.drawText(titleDisplay, MARGIN + 75f, y + 14f, textBoldPaint)

                canvas.drawText(tx.categoryName.take(16), MARGIN + 225f, y + 14f, textRegularPaint)
                canvas.drawText(tx.accountLabel.take(18), MARGIN + 315f, y + 14f, textRegularPaint)

                val sign = when (tx.type) {
                    "INCOME" -> "+"
                    "EXPENSE" -> "-"
                    else -> "→"
                }
                val paintForAmt = when (tx.type) {
                    "INCOME" -> textIncomePaint
                    "EXPENSE" -> textExpensePaint
                    else -> textBoldPaint
                }
                val amtStr = "$sign${
                    CurrencyFormatter.formatCurrency(
                        amount = tx.amount,
                        currencyCode = tx.currencyCode,
                        useSouthAsianGrouping = useBangladeshiFormat,
                        includeCodeSuffix = true
                    )
                }"
                canvas.drawText(amtStr, MARGIN + 420f, y + 14f, paintForAmt)

                y += txRowH
            }

            try {
                drawFooter(currentPageNum)
                pdfDocument.finishPage(page)
                FileOutputStream(internalFile).use { out ->
                    pdfDocument.writeTo(out)
                    out.flush()
                }
                isGenerated = internalFile.length() > 0L
            } catch (e: Throwable) {
                // Ignore and fall back to compliant PDF generation
            }
            try {
                pdfDocument.close()
            } catch (ignored: Throwable) {}
        } catch (e: Throwable) {
            // Native PdfDocument not available or threw on constructor
        }

        if (!isGenerated || internalFile.length() <= 0L) {
            writeCompliantPdfFile(internalFile, uiState, useBangladeshiFormat)
        }

        val internalBytes = internalFile.length()
        if (internalBytes <= 0L) {
            return@withContext PdfExportResult(
                isSuccess = false,
                errorMessage = "Failed to write PDF: generated file is 0 bytes."
            )
        }

        try {
            val contentUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    internalFile
                )
            } catch (e: Throwable) {
                Uri.fromFile(internalFile)
            }

            // Step 2: Save to public Downloads directory via MediaStore on Android 10+ (API 29+)
            var publicUri: Uri? = null
            var publicPath: String = internalFile.absolutePath

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HisabMate")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val downloadCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val insertedUri = resolver.insert(downloadCollection, contentValues)

                    if (insertedUri != null) {
                        resolver.openOutputStream(insertedUri)?.use { out ->
                            internalFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                            out.flush()
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(insertedUri, contentValues, null, null)
                        publicUri = insertedUri
                        publicPath = "Download/HisabMate/$fileName"
                    }
                } else {
                    // Fallback for older Android versions
                    @Suppress("DEPRECATION")
                    val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (publicDownloads != null && (publicDownloads.exists() || publicDownloads.mkdirs())) {
                        val targetPublicFile = File(publicDownloads, fileName)
                        FileOutputStream(targetPublicFile).use { out ->
                            internalFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                            out.flush()
                        }
                        if (targetPublicFile.length() > 0) {
                            publicPath = targetPublicFile.absolutePath
                        }
                    }
                }
            } catch (e: Throwable) {
                publicPath = internalFile.absolutePath
            }

            val sizeKb = internalBytes / 1024.0
            val sizeFormatted = String.format(Locale.US, "%.1f KB", sizeKb)

            PdfExportResult(
                isSuccess = true,
                fileUri = contentUri,
                filePath = publicPath,
                fileName = fileName,
                fileSizeFormatted = sizeFormatted,
                byteCount = internalBytes
            )
        } catch (e: Exception) {
            PdfExportResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Unknown error while creating PDF statement"
            )
        }
    }

    private fun writeCompliantPdfFile(
        targetFile: File,
        uiState: HisabUiState,
        useBangladeshiFormat: Boolean
    ) {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("%âãÏÓ\n")

        val bodyBuilder = StringBuilder()
        bodyBuilder.append("BT\n")
        bodyBuilder.append("/F1 18 Tf\n")
        bodyBuilder.append("50 790 Td\n")
        bodyBuilder.append("(HisabMate - Official Financial Statement) Tj\n")
        bodyBuilder.append("/F1 10 Tf\n")
        bodyBuilder.append("0 -20 Td\n")
        bodyBuilder.append("(Generated: $nowFormatted | Base Currency: ${uiState.baseCurrencyCode}) Tj\n")
        bodyBuilder.append("0 -24 Td\n")
        bodyBuilder.append("/F1 12 Tf\n")
        bodyBuilder.append("(EXECUTIVE LIQUIDITY SUMMARY) Tj\n")
        bodyBuilder.append("/F1 10 Tf\n")
        bodyBuilder.append("0 -16 Td\n")
        bodyBuilder.append("(Net Liquidity: ${uiState.totalBalance} ${uiState.baseCurrencyCode}) Tj\n")
        bodyBuilder.append("0 -14 Td\n")
        bodyBuilder.append("(Monthly Inflow: ${uiState.monthlyIncome} ${uiState.baseCurrencyCode}) Tj\n")
        bodyBuilder.append("0 -14 Td\n")
        bodyBuilder.append("(Monthly Outflow: ${uiState.monthlyExpense} ${uiState.baseCurrencyCode}) Tj\n")
        bodyBuilder.append("0 -14 Td\n")
        bodyBuilder.append("(Monthly Savings: ${uiState.monthlySavings} ${uiState.baseCurrencyCode}) Tj\n")
        bodyBuilder.append("0 -22 Td\n")
        bodyBuilder.append("/F1 12 Tf\n")
        bodyBuilder.append("(ACCOUNTS AND WALLETS) Tj\n")
        bodyBuilder.append("/F1 9 Tf\n")
        for (acc in uiState.accounts.take(8)) {
            val safeName = acc.name.replace("(", "").replace(")", "").replace("\\", "")
            bodyBuilder.append("0 -14 Td\n")
            bodyBuilder.append("(- $safeName [${acc.type}]: ${acc.balance} ${acc.currencyCode}) Tj\n")
        }
        bodyBuilder.append("0 -22 Td\n")
        bodyBuilder.append("/F1 12 Tf\n")
        bodyBuilder.append("(TRANSACTION AUDIT LEDGER) Tj\n")
        bodyBuilder.append("/F1 9 Tf\n")
        for (tx in uiState.transactions.take(30)) {
            val safeTitle = tx.title.replace("(", "").replace(")", "").replace("\\", "").take(32)
            val safeCat = tx.categoryName.replace("(", "").replace(")", "").replace("\\", "").take(16)
            val dStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(tx.timestamp))
            bodyBuilder.append("0 -13 Td\n")
            bodyBuilder.append("($dStr | $safeTitle | $safeCat | ${tx.type}: ${tx.amount} ${tx.currencyCode}) Tj\n")
        }
        bodyBuilder.append("ET\n")

        val contentBytes = bodyBuilder.toString().toByteArray(Charsets.ISO_8859_1)

        val obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        val obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        val obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>\nendobj\n"
        val obj4 = "4 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n${bodyBuilder}\nendstream\nendobj\n"
        val obj5 = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"

        val offset1 = sb.length
        sb.append(obj1)
        val offset2 = sb.length
        sb.append(obj2)
        val offset3 = sb.length
        sb.append(obj3)
        val offset4 = sb.length
        sb.append(obj4)
        val offset5 = sb.length
        sb.append(obj5)

        val xrefOffset = sb.length
        sb.append("xref\n")
        sb.append("0 6\n")
        sb.append("0000000000 65535 f \n")
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset1))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset2))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset3))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset4))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset5))
        sb.append("trailer\n")
        sb.append("<< /Size 6 /Root 1 0 R >>\n")
        sb.append("startxref\n")
        sb.append("$xrefOffset\n")
        sb.append("%%EOF\n")

        targetFile.writeBytes(sb.toString().toByteArray(Charsets.ISO_8859_1))
    }

    fun createViewPdfIntent(context: Context, contentUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createSharePdfIntent(context: Context, contentUri: Uri, title: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share HisabMate Statement via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
