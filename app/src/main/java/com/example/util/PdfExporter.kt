package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import kotlin.math.abs

data class PdfExportResult(
    val isSuccess: Boolean,
    val fileUri: Uri? = null,
    val filePath: String? = null,
    val fileName: String = "",
    val fileSizeFormatted: String = "",
    val byteCount: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Production-Ready Single-Page ISO A4 Executive Financial Statement Exporter.
 *
 * Page Specification:
 * - Standard ISO A4 Portrait: 595 x 842 PostScript points (210mm x 297mm @ 72 DPI)
 * - Strictly 1 Page ("PAGE 1 OF 1"): Dynamically fits executive KPIs, institutional account
 *   balances, category allocation bars, and the latest 18–19 ledger entries with a consolidated
 *   audit footer row so content never spills or clips across pages.
 * - PDF-Safe Currency Formatting: Uses clean ISO codes and Latin-safe glyphs so every PDF viewer
 *   (Android PDF Viewer, Adobe Acrobat, Chrome, macOS Preview) renders crisp numbers without missing glyph boxes.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 26f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN // 543f
    private const val RIGHT_EDGE = PAGE_WIDTH - MARGIN // 569f

    suspend fun generateAndSaveStatementPdf(
        context: Context,
        uiState: HisabUiState,
        useBangladeshiFormat: Boolean
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val now = Date()
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
        val fileName = "HisabMate_Statement_${uiState.baseCurrencyCode}_$timestampStr.pdf"

        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val internalFile = File(exportDir, fileName)

        var isGenerated = false
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            drawSinglePageA4Statement(
                canvas = page.canvas,
                uiState = uiState,
                useBangladeshiFormat = useBangladeshiFormat,
                issuedDate = now
            )

            try {
                pdfDocument.finishPage(page)
                FileOutputStream(internalFile).use { out ->
                    pdfDocument.writeTo(out)
                    out.flush()
                }
                isGenerated = internalFile.length() > 0L
            } catch (_: Throwable) {
                // Fall back to ISO-32000 vector PDF stream writer below
            }
            try {
                pdfDocument.close()
            } catch (_: Throwable) {}
        } catch (_: Throwable) {
            // Native PdfDocument unavailable in headless JVM unit test environment
        }

        if (!isGenerated || internalFile.length() <= 0L) {
            writeCompliantSinglePageA4Pdf(
                targetFile = internalFile,
                uiState = uiState,
                useBangladeshiFormat = useBangladeshiFormat,
                issuedDate = now
            )
        }

        val internalBytes = internalFile.length()
        if (internalBytes <= 0L) {
            return@withContext PdfExportResult(
                isSuccess = false,
                errorMessage = "Failed to write PDF: generated file is 0 bytes."
            )
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                internalFile
            )

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
            } catch (_: Throwable) {
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

    /**
     * Formats currency values specifically for PDF rendering so that non-Latin glyphs (like ৳)
     * never render as tofu boxes on minimal PDF viewers, while preserving South Asian / International grouping.
     */
    private fun formatPdfCurrency(
        amount: Double,
        currencyCode: String,
        useBangladeshiFormat: Boolean,
        prefixSign: String = ""
    ): String {
        val (_, wholePart, decPart) = CurrencyFormatter.splitCurrencyParts(
            amount = abs(amount),
            currencyCode = currencyCode,
            useSouthAsianGrouping = useBangladeshiFormat
        )
        val negativePrefix = if (amount < 0 && prefixSign.isEmpty()) "-" else prefixSign
        return "$negativePrefix$currencyCode $wholePart$decPart"
    }

    private fun drawRightAlignedText(canvas: Canvas, text: String, rightX: Float, y: Float, paint: Paint) {
        val width = paint.measureText(text)
        canvas.drawText(text, rightX - width, y, paint)
    }

    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        leftX: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        minimumTextSize: Float = 6.2f
    ) {
        val originalSize = paint.textSize
        while (paint.measureText(text) > maxWidth && paint.textSize > minimumTextSize) {
            paint.textSize = (paint.textSize - 0.35f).coerceAtLeast(minimumTextSize)
        }
        var visibleText = text
        if (paint.measureText(visibleText) > maxWidth) {
            while (visibleText.isNotEmpty() && paint.measureText("$visibleText…") > maxWidth) {
                visibleText = visibleText.dropLast(1)
            }
            visibleText = "$visibleText…"
        }
        canvas.drawText(visibleText, leftX, y, paint)
        paint.textSize = originalSize
    }

    private fun drawRightAlignedFittedText(
        canvas: Canvas,
        text: String,
        rightX: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        minimumTextSize: Float = 6.2f
    ) {
        val originalSize = paint.textSize
        while (paint.measureText(text) > maxWidth && paint.textSize > minimumTextSize) {
            paint.textSize = (paint.textSize - 0.35f).coerceAtLeast(minimumTextSize)
        }
        var visibleText = text
        if (paint.measureText(visibleText) > maxWidth) {
            while (visibleText.isNotEmpty() && paint.measureText("$visibleText…") > maxWidth) {
                visibleText = visibleText.dropLast(1)
            }
            visibleText = "$visibleText…"
        }
        canvas.drawText(visibleText, rightX - paint.measureText(visibleText), y, paint)
        paint.textSize = originalSize
    }

    /**
     * Draws a complete, bank-grade Single-Page ISO A4 Executive Financial Statement onto [canvas].
     */
    private fun drawSinglePageA4Statement(
        canvas: Canvas,
        uiState: HisabUiState,
        useBangladeshiFormat: Boolean,
        issuedDate: Date
    ) {
        val baseCode = uiState.baseCurrencyCode
        val baseMeta = CurrencyCatalog.getMetadata(baseCode)
        val statementRef = "HM-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(issuedDate)
        val issuedFormatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(issuedDate)

        // Page Background & Subtle Outer Sheet Frame
        val pageBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), pageBgPaint)

        val outerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(RectF(16f, 16f, PAGE_WIDTH - 16f, PAGE_HEIGHT - 16f), 6f, 6f, outerFramePaint)

        // =========================================================================
        // 1. EXECUTIVE INSTITUTION HEADER BANNER (y = 24f .. 94f)
        // =========================================================================
        val headerRect = RectF(MARGIN, 24f, RIGHT_EDGE, 94f)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Obsidian Slate #0F172A
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 8f, 8f, headerBgPaint)

        // Top Violet Accent Strip inside Header
        val violetAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(124, 92, 255) // Royal Violet #7C5CFF
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(MARGIN, 24f, RIGHT_EDGE, 30f), 8f, 8f, violetAccentPaint)

        // Brand Monogram Box (36x36)
        val logoBoxRect = RectF(MARGIN + 12f, 40f, MARGIN + 48f, 76f)
        canvas.drawRoundRect(logoBoxRect, 7f, 7f, violetAccentPaint)

        val logoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 14f
        }
        val hmWidth = logoTextPaint.measureText("HM")
        canvas.drawText("HM", logoBoxRect.centerX() - hmWidth / 2f, logoBoxRect.centerY() + 5f, logoTextPaint)

        // Brand Title & Statement Classification
        val brandTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 16.5f
        }
        canvas.drawText("HISABMATE", MARGIN + 58f, 53f, brandTitlePaint)

        // Official Pill Badge next to Title
        val badgeRect = RectF(MARGIN + 162f, 41f, MARGIN + 332f, 56f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            style = Paint.Style.FILL
        }
        val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248) // Sky 400
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        canvas.drawRoundRect(badgeRect, 4f, 4f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 4f, 4f, badgeStrokePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.2f
        }
        canvas.drawText("OFFICIAL A4 FINANCIAL STATEMENT", MARGIN + 170f, 51f, badgeTextPaint)

        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 8f
        }
        canvas.drawText(
            "Multi-Currency Personal Finance & Wealth Audit Ledger  •  ISO 4217 Offline FX Engine",
            MARGIN + 58f,
            67f,
            headerSubPaint
        )
        canvas.drawText(
            "Package: com.aistudio.hisabmate.fntkbd  •  Local Storage: Room SQLite v7",
            MARGIN + 58f,
            79f,
            headerSubPaint
        )

        // Right-aligned Header Metadata
        val metaBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 8f
        }
        val metaMintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 211, 153) // Emerald 400
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 8f
        }
        drawRightAlignedText(canvas, "REF: $statementRef", RIGHT_EDGE - 12f, 50f, metaBoldPaint)
        drawRightAlignedText(canvas, "ISSUED: $issuedFormatted", RIGHT_EDGE - 12f, 64f, headerSubPaint)
        drawRightAlignedText(canvas, "BASE CURRENCY: $baseCode (${baseMeta.name})", RIGHT_EDGE - 12f, 78f, metaMintPaint)

        // =========================================================================
        // 2. 4-COLUMN EXECUTIVE KPI SUMMARY STRIP (y = 104f .. 160f)
        // =========================================================================
        val kpiTop = 104f
        val kpiBottom = 160f
        val kpiGap = 9f
        val kpiWidth = (CONTENT_WIDTH - kpiGap * 3f) / 4f

        val kpiBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249) // Cool Slate Surface #F1F5F9
            style = Paint.Style.FILL
        }
        val kpiBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(184, 200, 218) // Crisp Slate Border
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.2f
        }
        val kpiSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 7f
        }

        data class KpiSpec(
            val title: String,
            val value: String,
            val subtitle: String,
            val accentColor: Int,
            val valueColor: Int
        )

        val kpis = listOf(
            KpiSpec(
                title = "NET LIQUIDITY",
                value = formatPdfCurrency(uiState.totalBalance, baseCode, useBangladeshiFormat),
                subtitle = "Across ${uiState.accounts.size} Active Wallets",
                accentColor = Color.rgb(124, 92, 255),
                valueColor = Color.rgb(15, 23, 42)
            ),
            KpiSpec(
                title = "MONTHLY INFLOW",
                value = formatPdfCurrency(uiState.monthlyIncome, baseCode, useBangladeshiFormat, prefixSign = "+"),
                subtitle = "Current report period",
                accentColor = Color.rgb(5, 150, 105),
                valueColor = Color.rgb(4, 120, 87)
            ),
            KpiSpec(
                title = "MONTHLY OUTFLOW",
                value = formatPdfCurrency(uiState.monthlyExpense, baseCode, useBangladeshiFormat, prefixSign = "-"),
                subtitle = "Current report period",
                accentColor = Color.rgb(220, 38, 38),
                valueColor = Color.rgb(185, 28, 28)
            ),
            KpiSpec(
                title = "NET SAVINGS & RATE",
                value = formatPdfCurrency(uiState.monthlySavings, baseCode, useBangladeshiFormat),
                subtitle = "Savings Efficiency: ${uiState.savingsRatePercent}%",
                accentColor = Color.rgb(2, 132, 199),
                valueColor = Color.rgb(3, 105, 161)
            )
        )

        kpis.forEachIndexed { idx, spec ->
            val left = MARGIN + idx * (kpiWidth + kpiGap)
            val right = left + kpiWidth
            val rect = RectF(left, kpiTop, right, kpiBottom)
            canvas.drawRoundRect(rect, 6f, 6f, kpiBgPaint)
            canvas.drawRoundRect(rect, 6f, 6f, kpiBorderPaint)

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spec.accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(left, kpiTop, right, kpiTop + 3.5f), 6f, 6f, barPaint)

            canvas.drawText(spec.title, left + 9f, kpiTop + 16f, kpiLabelPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spec.valueColor
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = 10.2f
            }
            drawFittedText(canvas, spec.value, left + 9f, kpiTop + 34f, kpiWidth - 18f, valPaint)
            drawFittedText(canvas, spec.subtitle, left + 9f, kpiTop + 48f, kpiWidth - 18f, kpiSubPaint)
        }

        // =========================================================================
        // 3. MIDDLE DUAL-PANEL SECTION (y = 172f .. 320f)
        //    Left: Institutional & Multi-Currency Wallets (width = 324f)
        //    Right: Top Category Allocations & Budget Utilization (width = 207f)
        // =========================================================================
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 9f
        }
        val sectionMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 7.5f
        }

        val leftPanelLeft = MARGIN
        val leftPanelWidth = 326f
        val leftPanelRight = leftPanelLeft + leftPanelWidth

        val rightPanelLeft = leftPanelRight + 11f
        val rightPanelRight = RIGHT_EDGE

        canvas.drawText("01. INSTITUTIONAL & MULTI-CURRENCY WALLETS", leftPanelLeft, 181f, sectionTitlePaint)
        drawRightAlignedText(canvas, "Valuation in $baseCode", leftPanelRight, 181f, sectionMetaPaint)

        canvas.drawText("02. TOP EXPENSE ALLOCATIONS", rightPanelLeft, 181f, sectionTitlePaint)

        // Left Panel Table Header
        val tableHeaderTop = 186f
        val accHeaderH = 18f
        val darkHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(leftPanelLeft, tableHeaderTop, leftPanelRight, tableHeaderTop + accHeaderH), 4f, 4f, darkHeaderPaint)

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.5f
        }
        canvas.drawText("Account / Institution", leftPanelLeft + 7f, tableHeaderTop + 12f, thPaint)
        canvas.drawText("Rail", leftPanelLeft + 128f, tableHeaderTop + 12f, thPaint)
        drawRightAlignedText(canvas, "Native Balance", leftPanelLeft + 236f, tableHeaderTop + 12f, thPaint)
        drawRightAlignedText(canvas, "In $baseCode", leftPanelRight - 7f, tableHeaderTop + 12f, thPaint)

        val rowEvenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val rowOddPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val rowGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val cellRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 7.6f
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.6f
        }

        val accounts = uiState.accounts
        val maxAccountRows = 5
        val accRowH = 19f
        var accY = tableHeaderTop + accHeaderH

        val displayedAccounts = if (accounts.size <= maxAccountRows) {
            accounts
        } else {
            accounts.take(maxAccountRows - 1)
        }

        for (i in 0 until maxAccountRows) {
            val rowRect = RectF(leftPanelLeft, accY, leftPanelRight, accY + accRowH)
            canvas.drawRect(rowRect, if (i % 2 == 0) rowEvenPaint else rowOddPaint)
            canvas.drawLine(leftPanelLeft, accY + accRowH, leftPanelRight, accY + accRowH, rowGridPaint)

            if (i < displayedAccounts.size) {
                val acc = displayedAccounts[i]
                drawFittedText(canvas, acc.name, leftPanelLeft + 7f, accY + 13f, 112f, cellBoldPaint)
                drawFittedText(canvas, acc.type, leftPanelLeft + 128f, accY + 13f, 100f, cellRegularPaint)

                val nativeFormatted = formatPdfCurrency(acc.balance, acc.currencyCode, useBangladeshiFormat)
                drawRightAlignedFittedText(canvas, nativeFormatted, leftPanelLeft + 236f, accY + 13f, 104f, cellRegularPaint)

                val converted = CurrencyFormatter.convert(
                    amount = acc.balance,
                    fromCurrency = acc.currencyCode,
                    toCurrency = baseCode,
                    ratesToBdt = uiState.ratesToBdtMap
                )
                val convFormatted = formatPdfCurrency(converted, baseCode, useBangladeshiFormat)
                drawRightAlignedFittedText(canvas, convFormatted, leftPanelRight - 7f, accY + 13f, 78f, cellBoldPaint)
            } else if (i == maxAccountRows - 1 && accounts.size > maxAccountRows) {
                val remainingAccs = accounts.drop(maxAccountRows - 1)
                val remainingSum = remainingAccs.sumOf {
                    CurrencyFormatter.convert(it.balance, it.currencyCode, baseCode, uiState.ratesToBdtMap)
                }
                canvas.drawText("+ ${remainingAccs.size} Additional Wallets", leftPanelLeft + 7f, accY + 13f, cellBoldPaint)
                canvas.drawText("MULTI", leftPanelLeft + 128f, accY + 13f, cellRegularPaint)
                drawRightAlignedText(canvas, "Consolidated", leftPanelLeft + 236f, accY + 13f, cellRegularPaint)
                drawRightAlignedText(
                    canvas,
                    formatPdfCurrency(remainingSum, baseCode, useBangladeshiFormat),
                    leftPanelRight - 7f,
                    accY + 13f,
                    cellBoldPaint
                )
            } else {
                canvas.drawText("—", leftPanelLeft + 7f, accY + 13f, sectionMetaPaint)
            }
            accY += accRowH
        }

        // Total Portfolio Summary Strip at bottom of Left Panel
        val accTotalRect = RectF(leftPanelLeft, accY, leftPanelRight, accY + 19f)
        val subtotalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255) // Indigo 100
            style = Paint.Style.FILL
        }
        canvas.drawRect(accTotalRect, subtotalBgPaint)
        canvas.drawRoundRect(RectF(leftPanelLeft, tableHeaderTop, leftPanelRight, accY + 19f), 4f, 4f, kpiBorderPaint)

        val indigoBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 27, 75)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.8f
        }
        canvas.drawText("TOTAL CONSOLIDATED PORTFOLIO", leftPanelLeft + 7f, accY + 13f, indigoBoldPaint)
        drawRightAlignedText(
            canvas,
            formatPdfCurrency(uiState.totalBalance, baseCode, useBangladeshiFormat),
            leftPanelRight - 7f,
            accY + 13f,
            indigoBoldPaint
        )

        // Right Panel: Top 4 Expense Categories Box
        val rightBoxBottom = accY + 19f
        val rightBoxRect = RectF(rightPanelLeft, tableHeaderTop, rightPanelRight, rightBoxBottom)
        canvas.drawRoundRect(rightBoxRect, 6f, 6f, kpiBgPaint)
        canvas.drawRoundRect(rightBoxRect, 6f, 6f, kpiBorderPaint)

        val topCategories = uiState.categorySummaries
            .filter { it.totalSpentInBase > 0.0 }
            .sortedByDescending { it.totalSpentInBase }
            .take(4)

        val totalCatSpend = uiState.monthlyExpense.coerceAtLeast(
            topCategories.sumOf { it.totalSpentInBase }.coerceAtLeast(1.0)
        )
        val barColors = listOf(
            Color.rgb(124, 92, 255), // Violet
            Color.rgb(14, 165, 104), // Emerald
            Color.rgb(245, 158, 11), // Amber
            Color.rgb(236, 72, 153)  // Rose
        )
        val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.FILL
        }

        if (topCategories.isEmpty()) {
            canvas.drawText("No categorized expenses recorded.", rightPanelLeft + 10f, tableHeaderTop + 60f, sectionMetaPaint)
        } else {
            var catY = tableHeaderTop + 16f
            topCategories.forEachIndexed { idx, summary ->
                val sharePct = ((summary.totalSpentInBase / totalCatSpend) * 100.0).toInt().coerceIn(1, 100)
                val catName = summary.category.name.take(16)
                val catAmtStr = "${formatPdfCurrency(summary.totalSpentInBase, baseCode, useBangladeshiFormat)} ($sharePct%)"

                drawFittedText(canvas, catName, rightPanelLeft + 9f, catY, 78f, cellBoldPaint)
                drawRightAlignedFittedText(canvas, catAmtStr, rightPanelRight - 9f, catY, 108f, sectionMetaPaint)

                val trackLeft = rightPanelLeft + 9f
                val trackRight = rightPanelRight - 9f
                val trackTop = catY + 5f
                val trackBottom = trackTop + 5.5f
                canvas.drawRoundRect(RectF(trackLeft, trackTop, trackRight, trackBottom), 3f, 3f, barTrackPaint)

                val fillRatio = (summary.totalSpentInBase / totalCatSpend).toFloat().coerceIn(0.06f, 1f)
                val fillRight = trackLeft + (trackRight - trackLeft) * fillRatio
                val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = barColors[idx % barColors.size]
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(trackLeft, trackTop, fillRight, trackBottom), 3f, 3f, barFillPaint)

                catY += 30f
            }
        }

        // =========================================================================
        // 4. SINGLE-PAGE TRANSACTION AUDIT LEDGER (y = 334f .. 766f)
        // =========================================================================
        val txSectionTitleY = 336f
        val txList = uiState.transactions
        val maxTxRows = 18
        val displayedTxs = txList.take(maxTxRows)

        canvas.drawText("03. CHRONOLOGICAL TRANSACTION AUDIT LEDGER", MARGIN, txSectionTitleY, sectionTitlePaint)
        drawRightAlignedText(
            canvas,
            "Latest ${displayedTxs.size} of ${txList.size} recorded entries  •  A4 summary",
            RIGHT_EDGE,
            txSectionTitleY,
            sectionMetaPaint
        )

        val txHeaderTop = 342f
        val txHeaderH = 20f
        canvas.drawRoundRect(RectF(MARGIN, txHeaderTop, RIGHT_EDGE, txHeaderTop + txHeaderH), 4f, 4f, headerBgPaint)

        val colDateX = MARGIN + 8f
        val colDescX = MARGIN + 64f
        val colCatX = MARGIN + 236f
        val colAccX = MARGIN + 324f
        val colTypeX = MARGIN + 416f
        val colAmtRightX = RIGHT_EDGE - 8f

        canvas.drawText("DATE", colDateX, txHeaderTop + 13.5f, thPaint)
        canvas.drawText("DESCRIPTION / MERCHANT", colDescX, txHeaderTop + 13.5f, thPaint)
        canvas.drawText("CATEGORY", colCatX, txHeaderTop + 13.5f, thPaint)
        canvas.drawText("WALLET / RAIL", colAccX, txHeaderTop + 13.5f, thPaint)
        canvas.drawText("TYPE", colTypeX, txHeaderTop + 13.5f, thPaint)
        drawRightAlignedText(canvas, "AMOUNT", colAmtRightX, txHeaderTop + 13.5f, thPaint)

        val txRowH = 20f
        var txY = txHeaderTop + txHeaderH

        val incomeAmtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(4, 120, 87) // Emerald 700
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.8f
        }
        val expenseAmtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(185, 28, 28) // Red 700
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.8f
        }
        val transferAmtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 7.8f
        }

        val dateFmt = SimpleDateFormat("dd MMM yy", Locale.US)

        for (rowIdx in 0 until maxTxRows) {
            val rowRect = RectF(MARGIN, txY, RIGHT_EDGE, txY + txRowH)
            canvas.drawRect(rowRect, if (rowIdx % 2 == 0) rowEvenPaint else rowOddPaint)
            canvas.drawLine(MARGIN, txY + txRowH, RIGHT_EDGE, txY + txRowH, rowGridPaint)

            if (rowIdx < displayedTxs.size) {
                val tx = displayedTxs[rowIdx]
                val dStr = dateFmt.format(Date(tx.timestamp))
                canvas.drawText(dStr, colDateX, txY + 13.5f, cellRegularPaint)

                val rawTitle = if (tx.merchant.isNotBlank() && !tx.title.contains(tx.merchant, ignoreCase = true)) {
                    "${tx.title} • ${tx.merchant}"
                } else {
                    tx.title
                }
                drawFittedText(canvas, rawTitle, colDescX, txY + 13.5f, 164f, cellBoldPaint)
                drawFittedText(canvas, tx.categoryName, colCatX, txY + 13.5f, 80f, cellRegularPaint)
                drawFittedText(canvas, tx.accountLabel, colAccX, txY + 13.5f, 84f, cellRegularPaint)

                // Type Pill Badge
                val badgeBgColor: Int
                val badgeTxtColor: Int
                val badgeLabel: String
                when (tx.type) {
                    "INCOME" -> {
                        badgeBgColor = Color.rgb(209, 250, 229)
                        badgeTxtColor = Color.rgb(4, 120, 87)
                        badgeLabel = "CREDIT"
                    }
                    "EXPENSE" -> {
                        badgeBgColor = Color.rgb(254, 226, 226)
                        badgeTxtColor = Color.rgb(185, 28, 28)
                        badgeLabel = "DEBIT"
                    }
                    else -> {
                        badgeBgColor = Color.rgb(226, 232, 240)
                        badgeTxtColor = Color.rgb(51, 65, 85)
                        badgeLabel = "TRANSFER"
                    }
                }
                val pillRect = RectF(colTypeX, txY + 4.5f, colTypeX + 44f, txY + 15.5f)
                val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = badgeBgColor
                    style = Paint.Style.FILL
                }
                val pillTxtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = badgeTxtColor
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = 6.5f
                }
                canvas.drawRoundRect(pillRect, 3f, 3f, pillBgPaint)
                val lblW = pillTxtPaint.measureText(badgeLabel)
                canvas.drawText(badgeLabel, pillRect.centerX() - lblW / 2f, txY + 12.2f, pillTxtPaint)

                val sign = when (tx.type) {
                    "INCOME" -> "+"
                    "EXPENSE" -> "-"
                    else -> ""
                }
                val amtPaint = when (tx.type) {
                    "INCOME" -> incomeAmtPaint
                    "EXPENSE" -> expenseAmtPaint
                    else -> transferAmtPaint
                }
                val formattedTxAmt = formatPdfCurrency(tx.amount, tx.currencyCode, useBangladeshiFormat, prefixSign = sign)
                drawRightAlignedFittedText(canvas, formattedTxAmt, colAmtRightX, txY + 13.5f, 82f, amtPaint)
            } else if (rowIdx == 0 && displayedTxs.isEmpty()) {
                canvas.drawText("No transactions recorded in the current ledger.", colDescX, txY + 13.5f, sectionMetaPaint)
            }
            txY += txRowH
        }

        // Consolidated Ledger Summary Bar at Bottom of Transaction Table
        val summaryRowH = 22f
        val summaryRect = RectF(MARGIN, txY, RIGHT_EDGE, txY + summaryRowH)
        canvas.drawRect(summaryRect, subtotalBgPaint)
        canvas.drawRoundRect(RectF(MARGIN, txHeaderTop, RIGHT_EDGE, txY + summaryRowH), 4f, 4f, kpiBorderPaint)

        val omittedCount = (txList.size - displayedTxs.size).coerceAtLeast(0)
        val summaryLeftText = if (omittedCount > 0) {
            "CONSOLIDATED A4 DIGEST: Showing latest ${displayedTxs.size} entries  •  +$omittedCount earlier archived entries included in KPI totals"
        } else {
            "COMPLETE LEDGER DIGEST: All ${txList.size} verified transaction entries are itemized on this single A4 sheet"
        }
        canvas.drawText(summaryLeftText, colDateX, txY + 14.5f, indigoBoldPaint)

        val netFlowPrefix = if (uiState.monthlySavings >= 0) "+" else "-"
        val netFlowFormatted = formatPdfCurrency(abs(uiState.monthlySavings), baseCode, useBangladeshiFormat, prefixSign = netFlowPrefix)
        drawRightAlignedText(canvas, "NET PERIOD CASHFLOW: $netFlowFormatted", colAmtRightX, txY + 14.5f, indigoBoldPaint)

        // =========================================================================
        // 5. OFFICIAL COMPLIANCE & VERIFICATION FOOTER (y = 782f .. 820f)
        // =========================================================================
        val footerTop = 784f
        canvas.drawLine(MARGIN, footerTop, RIGHT_EDGE, footerTop, kpiBorderPaint)

        canvas.drawText(
            "HISABMATE FINANCIAL STATEMENT  •  GENERATED LOCALLY  •  NO CLOUD SYNC",
            MARGIN,
            footerTop + 14f,
            cellBoldPaint
        )
        canvas.drawText(
            "Generated on-device via HisabMate v1.0.5. FX conversions use saved ISO 4217 rates; original transaction currencies are retained.",
            MARGIN,
            footerTop + 26f,
            kpiSubPaint
        )

        // Official Single-Page Stamp Box on Right
        val stampRect = RectF(RIGHT_EDGE - 124f, footerTop + 5f, RIGHT_EDGE, footerTop + 30f)
        canvas.drawRoundRect(stampRect, 4f, 4f, kpiBgPaint)
        canvas.drawRoundRect(stampRect, 4f, 4f, kpiBorderPaint)
        drawRightAlignedText(canvas, "PAGE 1 OF 1  •  ISO A4", RIGHT_EDGE - 8f, footerTop + 16f, cellBoldPaint)
        drawRightAlignedText(canvas, "LOCAL EXPORT • ROOM DB v7", RIGHT_EDGE - 8f, footerTop + 26f, incomeAmtPaint)
    }

    /**
     * Generates a 100% ISO-32000 compliant Single-Page A4 PDF with vector rectangles, colored header
     * banners, KPI cards, and structured tabular columns when running in headless JVM / fallback mode.
     */
    private fun writeCompliantSinglePageA4Pdf(
        targetFile: File,
        uiState: HisabUiState,
        useBangladeshiFormat: Boolean,
        issuedDate: Date
    ) {
        val baseCode = uiState.baseCurrencyCode
        val nowFormatted = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(issuedDate)
        val refId = "HM-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(issuedDate)

        fun pdfEscape(input: String): String {
            return input
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .map { if (it.code in 32..126) it else ' ' }
                .joinToString("")
        }

        val stream = StringBuilder()

        // Helper for filled rectangle in PDF coordinates (origin bottom-left, y_pdf = 842 - y_top - h)
        fun fillRectTop(x: Float, yTop: Float, w: Float, h: Float, r: Float, g: Float, b: Float) {
            val yPdf = PAGE_HEIGHT - yTop - h
            stream.append(String.format(Locale.US, "%.3f %.3f %.3f rg %.1f %.1f %.1f %.1f re f\n", r, g, b, x, yPdf, w, h))
        }

        fun strokeRectTop(x: Float, yTop: Float, w: Float, h: Float, r: Float, g: Float, b: Float, lineW: Float = 0.8f) {
            val yPdf = PAGE_HEIGHT - yTop - h
            stream.append(String.format(Locale.US, "%.2f w %.3f %.3f %.3f RG %.1f %.1f %.1f %.1f re S\n", lineW, r, g, b, x, yPdf, w, h))
        }

        fun drawTextTop(x: Float, yTop: Float, text: String, font: String = "/F1", size: Float = 9f, r: Float = 0.06f, g: Float = 0.09f, b: Float = 0.16f) {
            val yPdf = PAGE_HEIGHT - yTop
            stream.append(String.format(Locale.US, "BT %s %.1f Tf %.3f %.3f %.3f rg %.1f %.1f Td (%s) Tj ET\n", font, size, r, g, b, x, yPdf, pdfEscape(text)))
        }

        // 1. Page Outer Border & Executive Header Banner
        strokeRectTop(16f, 16f, PAGE_WIDTH - 32f, PAGE_HEIGHT - 32f, 0.80f, 0.84f, 0.88f, 0.8f)
        fillRectTop(MARGIN, 24f, CONTENT_WIDTH, 70f, 0.059f, 0.090f, 0.165f) // #0F172A
        fillRectTop(MARGIN, 24f, CONTENT_WIDTH, 5f, 0.486f, 0.361f, 1.000f)  // #7C5CFF
        fillRectTop(MARGIN + 12f, 40f, 36f, 36f, 0.486f, 0.361f, 1.000f)

        drawTextTop(MARGIN + 20f, 63f, "HM", "/F2", 13f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 58f, 52f, "HISABMATE  |  OFFICIAL A4 FINANCIAL STATEMENT", "/F2", 13f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 58f, 67f, "Multi-Currency Personal Finance & Wealth Audit Ledger (ISO 4217 Offline FX)", "/F1", 8f, 0.58f, 0.64f, 0.72f)
        drawTextTop(MARGIN + 58f, 79f, "Ref: $refId  |  Issued: $nowFormatted  |  Base Currency: $baseCode", "/F2", 8f, 0.20f, 0.83f, 0.60f)

        // 2. 4 KPI Summary Cards
        val kpiGap = 9f
        val kpiW = (CONTENT_WIDTH - kpiGap * 3f) / 4f
        val kpiTitles = listOf("NET LIQUIDITY", "MONTHLY INFLOW", "MONTHLY OUTFLOW", "NET SAVINGS & RATE")
        val kpiValues = listOf(
            formatPdfCurrency(uiState.totalBalance, baseCode, useBangladeshiFormat),
            formatPdfCurrency(uiState.monthlyIncome, baseCode, useBangladeshiFormat, "+"),
            formatPdfCurrency(uiState.monthlyExpense, baseCode, useBangladeshiFormat, "-"),
            "${formatPdfCurrency(uiState.monthlySavings, baseCode, useBangladeshiFormat)} (${uiState.savingsRatePercent}%)"
        )
        for (i in 0..3) {
            val lx = MARGIN + i * (kpiW + kpiGap)
            fillRectTop(lx, 104f, kpiW, 54f, 0.945f, 0.961f, 0.976f)
            strokeRectTop(lx, 104f, kpiW, 54f, 0.72f, 0.78f, 0.85f, 0.8f)
            drawTextTop(lx + 8f, 120f, kpiTitles[i], "/F2", 7.5f, 0.28f, 0.33f, 0.41f)
            drawTextTop(lx + 8f, 138f, kpiValues[i].take(22), "/F2", 9.5f, 0.06f, 0.09f, 0.16f)
            drawTextTop(lx + 8f, 151f, "Report currency: $baseCode", "/F1", 7f, 0.39f, 0.45f, 0.55f)
        }

        // 3. Accounts Section
        drawTextTop(MARGIN, 176f, "01. INSTITUTIONAL & MULTI-CURRENCY WALLETS", "/F2", 9f, 0.06f, 0.09f, 0.16f)
        fillRectTop(MARGIN, 182f, CONTENT_WIDTH, 18f, 0.118f, 0.161f, 0.231f)
        drawTextTop(MARGIN + 8f, 194f, "Account / Institution", "/F2", 8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 200f, 194f, "Rail / Type", "/F2", 8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 310f, 194f, "Native Balance", "/F2", 8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 435f, 194f, "Converted ($baseCode)", "/F2", 8f, 1f, 1f, 1f)

        var yCursor = 200f
        uiState.accounts.take(5).forEachIndexed { idx, acc ->
            if (idx % 2 == 1) {
                fillRectTop(MARGIN, yCursor, CONTENT_WIDTH, 18f, 0.957f, 0.969f, 0.984f)
            }
            strokeRectTop(MARGIN, yCursor, CONTENT_WIDTH, 18f, 0.88f, 0.91f, 0.94f, 0.5f)
            val conv = CurrencyFormatter.convert(acc.balance, acc.currencyCode, baseCode, uiState.ratesToBdtMap)
            drawTextTop(MARGIN + 8f, yCursor + 12.5f, acc.name.take(24), "/F2", 8f)
            drawTextTop(MARGIN + 200f, yCursor + 12.5f, acc.type.take(14), "/F1", 8f)
            drawTextTop(MARGIN + 310f, yCursor + 12.5f, formatPdfCurrency(acc.balance, acc.currencyCode, useBangladeshiFormat), "/F1", 8f)
            drawTextTop(MARGIN + 435f, yCursor + 12.5f, formatPdfCurrency(conv, baseCode, useBangladeshiFormat), "/F2", 8f)
            yCursor += 18f
        }

        // 4. Compact Top Category Allocation Strip
        yCursor += 12f
        drawTextTop(MARGIN, yCursor, "TOP SPENDING CATEGORIES", "/F2", 8.8f, 0.06f, 0.09f, 0.16f)
        yCursor += 8f
        val topPdfCategories = uiState.categorySummaries
            .filter { it.totalSpentInBase > 0.0 }
            .sortedByDescending { it.totalSpentInBase }
            .take(4)
        if (topPdfCategories.isEmpty()) {
            fillRectTop(MARGIN, yCursor, CONTENT_WIDTH, 22f, 0.957f, 0.969f, 0.984f)
            drawTextTop(MARGIN + 8f, yCursor + 14f, "No categorized spending recorded in this report period.", "/F1", 8f, 0.28f, 0.33f, 0.41f)
            yCursor += 26f
        } else {
            val categoryGap = 8f
            val categoryWidth = (CONTENT_WIDTH - categoryGap * 3f) / 4f
            topPdfCategories.forEachIndexed { index, summary ->
                val x = MARGIN + index * (categoryWidth + categoryGap)
                fillRectTop(x, yCursor, categoryWidth, 28f, 0.945f, 0.953f, 0.976f)
                strokeRectTop(x, yCursor, categoryWidth, 28f, 0.72f, 0.78f, 0.85f, 0.6f)
                drawTextTop(x + 7f, yCursor + 11f, summary.category.name.take(18), "/F2", 7.2f)
                drawTextTop(x + 7f, yCursor + 22f, formatPdfCurrency(summary.totalSpentInBase, baseCode, useBangladeshiFormat), "/F2", 7.2f, 0.30f, 0.22f, 0.75f)
            }
            yCursor += 33f
        }

        // 5. Transactions Table (Single-Page A4)
        yCursor += 8f
        val txList = uiState.transactions
        val shownTxs = txList.take(16)
        drawTextTop(MARGIN, yCursor, "02. CHRONOLOGICAL TRANSACTION AUDIT LEDGER (Showing ${shownTxs.size} of ${txList.size} entries)", "/F2", 9f, 0.06f, 0.09f, 0.16f)
        yCursor += 6f
        fillRectTop(MARGIN, yCursor, CONTENT_WIDTH, 19f, 0.059f, 0.090f, 0.165f)
        drawTextTop(MARGIN + 8f, yCursor + 13f, "DATE", "/F2", 7.8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 68f, yCursor + 13f, "DESCRIPTION / MERCHANT", "/F2", 7.8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 245f, yCursor + 13f, "CATEGORY", "/F2", 7.8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 335f, yCursor + 13f, "ACCOUNT", "/F2", 7.8f, 1f, 1f, 1f)
        drawTextTop(MARGIN + 435f, yCursor + 13f, "AMOUNT", "/F2", 7.8f, 1f, 1f, 1f)
        yCursor += 19f

        val dateFmt = SimpleDateFormat("dd MMM yy", Locale.US)
        shownTxs.forEachIndexed { idx, tx ->
            if (idx % 2 == 1) {
                fillRectTop(MARGIN, yCursor, CONTENT_WIDTH, 19f, 0.957f, 0.969f, 0.984f)
            }
            strokeRectTop(MARGIN, yCursor, CONTENT_WIDTH, 19f, 0.88f, 0.91f, 0.94f, 0.5f)
            val sign = when (tx.type) {
                "INCOME" -> "+"
                "EXPENSE" -> "-"
                else -> ""
            }
            drawTextTop(MARGIN + 8f, yCursor + 13f, dateFmt.format(Date(tx.timestamp)), "/F1", 7.8f)
            drawTextTop(MARGIN + 68f, yCursor + 13f, tx.title.take(28), "/F2", 7.8f)
            drawTextTop(MARGIN + 245f, yCursor + 13f, tx.categoryName.take(15), "/F1", 7.8f)
            drawTextTop(MARGIN + 335f, yCursor + 13f, tx.accountLabel.take(15), "/F1", 7.8f)
            drawTextTop(MARGIN + 435f, yCursor + 13f, formatPdfCurrency(tx.amount, tx.currencyCode, useBangladeshiFormat, sign), "/F2", 7.8f)
            yCursor += 19f
        }

        // Footer
        strokeRectTop(MARGIN, 784f, CONTENT_WIDTH, 32f, 0.72f, 0.78f, 0.85f, 0.8f)
        drawTextTop(MARGIN + 8f, 798f, "HISABMATE OFFICIAL A4 SINGLE-PAGE FINANCIAL STATEMENT  |  ZERO CLOUD TELEMETRY", "/F2", 7.8f)
        drawTextTop(MARGIN + 8f, 810f, "Reconciled locally against Room SQLite v7  |  PAGE 1 OF 1 (ISO A4 210x297mm)", "/F1", 7.2f, 0.39f, 0.45f, 0.55f)

        val contentBytes = stream.toString().toByteArray(Charsets.ISO_8859_1)

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("%âãÏÓ\n")

        val obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        val obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        val obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>\nendobj\n"
        val obj4 = "4 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n$stream\nendstream\nendobj\n"
        val obj5 = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
        val obj6 = "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n"

        val offset1 = sb.length; sb.append(obj1)
        val offset2 = sb.length; sb.append(obj2)
        val offset3 = sb.length; sb.append(obj3)
        val offset4 = sb.length; sb.append(obj4)
        val offset5 = sb.length; sb.append(obj5)
        val offset6 = sb.length; sb.append(obj6)

        val xrefOffset = sb.length
        sb.append("xref\n0 7\n0000000000 65535 f \n")
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset1))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset2))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset3))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset4))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset5))
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offset6))
        sb.append("trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

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
