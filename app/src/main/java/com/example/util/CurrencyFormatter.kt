package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

data class CurrencyMetadata(
    val code: String,              // ISO 4217 code e.g. "BDT", "USD"
    val symbol: String,            // Authentic native symbol e.g. "৳", "$", "€", "£", "₹", "¥", "ر.س", "د.إ"
    val name: String,              // Full English name
    val countryOrRegion: String,   // Region description
    val flagCode: String,          // 2-letter region code badge
    val decimalDigits: Int,        // 2 for most, 0 for JPY, 3 for KWD
    val symbolOnLeft: Boolean = true,
    val symbolSpacing: String = "", // Space between symbol and digits for Arabic/alphabetic symbols
    val isSymbolAmbiguous: Boolean = false, // True for $, ¥, etc. where ISO code aids clarity
    val supportsSouthAsianGrouping: Boolean = false,
    val defaultRateToBdt: Double   // 1 unit of this currency = X BDT
)

object CurrencyCatalog {

    val supportedCurrencies: List<CurrencyMetadata> = listOf(
        CurrencyMetadata(
            code = "BDT",
            symbol = "৳",
            name = "Bangladeshi Taka",
            countryOrRegion = "Bangladesh",
            flagCode = "BD",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = true,
            defaultRateToBdt = 1.0
        ),
        CurrencyMetadata(
            code = "USD",
            symbol = "$",
            name = "US Dollar",
            countryOrRegion = "United States & Global",
            flagCode = "US",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = true,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 120.50
        ),
        CurrencyMetadata(
            code = "EUR",
            symbol = "€",
            name = "Euro",
            countryOrRegion = "European Union (Eurozone)",
            flagCode = "EU",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 130.80
        ),
        CurrencyMetadata(
            code = "GBP",
            symbol = "£",
            name = "British Pound Sterling",
            countryOrRegion = "United Kingdom",
            flagCode = "GB",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 155.40
        ),
        CurrencyMetadata(
            code = "INR",
            symbol = "₹",
            name = "Indian Rupee",
            countryOrRegion = "India",
            flagCode = "IN",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = true,
            defaultRateToBdt = 1.43
        ),
        CurrencyMetadata(
            code = "JPY",
            symbol = "¥",
            name = "Japanese Yen",
            countryOrRegion = "Japan",
            flagCode = "JP",
            decimalDigits = 0,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = true,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 0.79
        ),
        CurrencyMetadata(
            code = "CNY",
            symbol = "¥",
            name = "Chinese Yuan Renminbi",
            countryOrRegion = "China",
            flagCode = "CN",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = true,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 16.80
        ),
        CurrencyMetadata(
            code = "SAR",
            symbol = "ر.س",
            name = "Saudi Riyal",
            countryOrRegion = "Saudi Arabia",
            flagCode = "SA",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = " ",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 32.10
        ),
        CurrencyMetadata(
            code = "AED",
            symbol = "د.إ",
            name = "UAE Dirham",
            countryOrRegion = "United Arab Emirates",
            flagCode = "AE",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = " ",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 32.80
        ),
        CurrencyMetadata(
            code = "CAD",
            symbol = "CA$",
            name = "Canadian Dollar",
            countryOrRegion = "Canada",
            flagCode = "CA",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = true,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 86.40
        ),
        CurrencyMetadata(
            code = "AUD",
            symbol = "A$",
            name = "Australian Dollar",
            countryOrRegion = "Australia",
            flagCode = "AU",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = true,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 78.90
        ),
        CurrencyMetadata(
            code = "SGD",
            symbol = "S$",
            name = "Singapore Dollar",
            countryOrRegion = "Singapore",
            flagCode = "SG",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = "",
            isSymbolAmbiguous = true,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 90.20
        ),
        CurrencyMetadata(
            code = "MYR",
            symbol = "RM",
            name = "Malaysian Ringgit",
            countryOrRegion = "Malaysia",
            flagCode = "MY",
            decimalDigits = 2,
            symbolOnLeft = true,
            symbolSpacing = " ",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 27.60
        ),
        CurrencyMetadata(
            code = "KWD",
            symbol = "د.ك",
            name = "Kuwaiti Dinar",
            countryOrRegion = "Kuwait",
            flagCode = "KW",
            decimalDigits = 3,
            symbolOnLeft = true,
            symbolSpacing = " ",
            isSymbolAmbiguous = false,
            supportsSouthAsianGrouping = false,
            defaultRateToBdt = 392.50
        )
    )

    private val byCode: Map<String, CurrencyMetadata> =
        supportedCurrencies.associateBy { it.code.uppercase(Locale.US) }

    fun isSupported(code: String): Boolean = byCode.containsKey(code.uppercase(Locale.US))

    fun getMetadata(code: String): CurrencyMetadata =
        byCode[code.uppercase(Locale.US)] ?: throw IllegalArgumentException("Unsupported currency code: $code")
}

object CurrencyFormatter {

    /**
     * Converts an amount from [fromCurrency] to [toCurrency] using the active rate map
     * (where each rate represents 1 unit of currency = X BDT).
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        ratesToBdt: Map<String, Double>
    ): Double {
        val fromCode = fromCurrency.uppercase(Locale.US)
        val toCode = toCurrency.uppercase(Locale.US)
        if (fromCode == toCode) return amount

        require(amount.isFinite()) { "Amount must be finite" }
        val fromRateToBdt = ratesToBdt[fromCode] ?: CurrencyCatalog.getMetadata(fromCode).defaultRateToBdt
        val toRateToBdt = ratesToBdt[toCode] ?: CurrencyCatalog.getMetadata(toCode).defaultRateToBdt
        require(fromRateToBdt.isFinite() && fromRateToBdt > 0.0) { "Invalid $fromCode exchange rate" }
        require(toRateToBdt.isFinite() && toRateToBdt > 0.0) { "Invalid $toCode exchange rate" }
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(fromRateToBdt))
            .divide(BigDecimal.valueOf(toRateToBdt), CurrencyCatalog.getMetadata(toCode).decimalDigits, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun convertWithSnapshots(amount: Double, fromRateToBdt: Double, toRateToBdt: Double, toCurrency: String): Double {
        require(amount.isFinite() && fromRateToBdt.isFinite() && fromRateToBdt > 0.0)
        require(toRateToBdt.isFinite() && toRateToBdt > 0.0)
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(fromRateToBdt))
            .divide(BigDecimal.valueOf(toRateToBdt), CurrencyCatalog.getMetadata(toCurrency).decimalDigits, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun addMoney(balance: Double, delta: Double, currencyCode: String): Double =
        BigDecimal.valueOf(balance).add(BigDecimal.valueOf(delta))
            .setScale(CurrencyCatalog.getMetadata(currencyCode).decimalDigits, RoundingMode.HALF_UP).toDouble()

    fun subtractMoney(balance: Double, delta: Double, currencyCode: String): Double =
        BigDecimal.valueOf(balance).subtract(BigDecimal.valueOf(delta))
            .setScale(CurrencyCatalog.getMetadata(currencyCode).decimalDigits, RoundingMode.HALF_UP).toDouble()

    /**
     * Returns how many units of [toCurrency] equal 1 unit of [fromCurrency].
     */
    fun getExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        ratesToBdt: Map<String, Double>
    ): Double {
        return convert(1.0, fromCurrency, toCurrency, ratesToBdt)
    }

    /**
     * Formats an amount in any ISO 4217 currency with authentic native symbol,
     * proper decimal precision (0 for JPY, 2 for standard, 3 for KWD), symbol spacing,
     * negative sign handling, and South Asian / International grouping.
     */
    fun formatCurrency(
        amount: Double,
        currencyCode: String = "BDT",
        useSouthAsianGrouping: Boolean = true,
        showDecimals: Boolean? = null,
        includeSymbol: Boolean = true,
        includeCodeSuffix: Boolean = false,
        showPlusForPositive: Boolean = false
    ): String {
        val meta = CurrencyCatalog.getMetadata(currencyCode)
        val effectiveDecimals = when {
            meta.decimalDigits == 0 -> 0
            showDecimals == false -> 0
            else -> meta.decimalDigits
        }

        val isNegative = amount < -0.0001
        val absAmount = abs(amount)

        val factor = 10.0.pow(effectiveDecimals.toDouble())
        val roundedTotal = (absAmount * factor).roundToLong()
        val factorLong = factor.toLong().coerceAtLeast(1L)

        val wholePart = if (effectiveDecimals == 0) absAmount.roundToLong() else roundedTotal / factorLong
        val decimalPart = if (effectiveDecimals == 0) 0L else roundedTotal % factorLong

        val applySouthAsian = useSouthAsianGrouping && meta.supportsSouthAsianGrouping
        val formattedWhole = if (applySouthAsian) {
            formatSouthAsianNumber(wholePart)
        } else {
            String.format(Locale.US, "%,d", wholePart)
        }

        val decimalsStr = if (effectiveDecimals > 0) {
            String.format(Locale.US, ".%0${effectiveDecimals}d", decimalPart)
        } else {
            ""
        }

        val signPrefix = when {
            isNegative -> "-"
            showPlusForPositive && amount > 0.0001 -> "+"
            else -> ""
        }

        val symbolWithSpace = if (includeSymbol) "${meta.symbol}${meta.symbolSpacing}" else ""
        val codeSuffix = if (includeCodeSuffix) " ${meta.code}" else ""

        return if (meta.symbolOnLeft) {
            "$signPrefix$symbolWithSpace$formattedWhole$decimalsStr$codeSuffix"
        } else {
            "$signPrefix$formattedWhole$decimalsStr ${meta.symbol}$codeSuffix"
        }
    }

    /**
     * Backward-compatible helper that delegates to [formatCurrency] with the specified currency code.
     */
    fun formatBdt(
        amount: Double,
        useBangladeshiGrouping: Boolean = true,
        showDecimals: Boolean = true,
        includeSymbol: Boolean = true,
        showPlusForPositive: Boolean = false,
        currencyCode: String = "BDT"
    ): String {
        return formatCurrency(
            amount = amount,
            currencyCode = currencyCode,
            useSouthAsianGrouping = useBangladeshiGrouping,
            showDecimals = showDecimals,
            includeSymbol = includeSymbol,
            showPlusForPositive = showPlusForPositive
        )
    }

    /**
     * Splits an amount into (symbol, wholeString, decimalString) for hero displays,
     * supporting 0 decimals (JPY), 2 decimals (BDT, USD, EUR, CNY, etc.), and 3 decimals (KWD).
     */
    fun splitCurrencyParts(
        amount: Double,
        currencyCode: String = "BDT",
        useSouthAsianGrouping: Boolean = true
    ): Triple<String, String, String> {
        val meta = CurrencyCatalog.getMetadata(currencyCode)
        val isNegative = amount < -0.0001
        val absAmount = abs(amount)
        val sign = if (isNegative) "-" else ""

        if (meta.decimalDigits == 0) {
            val wholePart = absAmount.roundToLong()
            val formattedWhole = String.format(Locale.US, "%,d", wholePart)
            return Triple(meta.symbol, "$sign$formattedWhole", "")
        }

        val factor = 10.0.pow(meta.decimalDigits.toDouble())
        val factorLong = factor.toLong().coerceAtLeast(1L)
        val roundedUnits = (absAmount * factor).roundToLong()
        val wholePart = roundedUnits / factorLong
        val decimalPart = (roundedUnits % factorLong).toInt()

        val applySouthAsian = useSouthAsianGrouping && meta.supportsSouthAsianGrouping
        val formattedWhole = if (applySouthAsian) {
            formatSouthAsianNumber(wholePart)
        } else {
            String.format(Locale.US, "%,d", wholePart)
        }
        val decStr = String.format(Locale.US, ".%0${meta.decimalDigits}d", decimalPart)
        return Triple(meta.symbol, "$sign$formattedWhole", decStr)
    }

    fun formatCompactCurrency(
        amount: Double,
        currencyCode: String = "BDT",
        useSouthAsianGrouping: Boolean = true
    ): String {
        val meta = CurrencyCatalog.getMetadata(currencyCode)
        val absVal = abs(amount)
        val sign = if (amount < -0.0001) "-" else ""
        val sym = "${meta.symbol}${meta.symbolSpacing}"

        if (useSouthAsianGrouping && meta.supportsSouthAsianGrouping) {
            return when {
                absVal >= 10_000_000 -> String.format(Locale.US, "%s%s%.2fCr", sign, sym, absVal / 10_000_000.0)
                absVal >= 100_000 -> String.format(Locale.US, "%s%s%.2fL", sign, sym, absVal / 100_000.0)
                absVal >= 1_000 -> String.format(Locale.US, "%s%s%.1fK", sign, sym, absVal / 1_000.0)
                else -> String.format(Locale.US, "%s%s%.0f", sign, sym, absVal)
            }
        }

        return when {
            absVal >= 1_000_000_000 -> String.format(Locale.US, "%s%s%.2fB", sign, sym, absVal / 1_000_000_000.0)
            absVal >= 1_000_000 -> String.format(Locale.US, "%s%s%.2fM", sign, sym, absVal / 1_000_000.0)
            absVal >= 1_000 -> String.format(Locale.US, "%s%s%.1fK", sign, sym, absVal / 1_000.0)
            else -> String.format(Locale.US, "%s%s%.0f", sign, sym, absVal)
        }
    }

    fun formatRateString(
        fromCurrency: String,
        toCurrency: String,
        ratesToBdt: Map<String, Double>
    ): String {
        val rate = getExchangeRate(fromCurrency, toCurrency, ratesToBdt)
        val toMeta = CurrencyCatalog.getMetadata(toCurrency)
        val formattedRate = when {
            rate >= 100.0 -> String.format(Locale.US, "%,.2f", rate)
            rate >= 1.0 -> String.format(Locale.US, "%.4f", rate).trimEnd('0').trimEnd('.')
            else -> String.format(Locale.US, "%.5f", rate)
        }
        return "1 ${fromCurrency.uppercase(Locale.US)} = ${toMeta.symbol}${toMeta.symbolSpacing}$formattedRate ${toMeta.code}"
    }

    private fun formatSouthAsianNumber(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i > 0) {
                sb.append(',')
                count = 0
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTimeShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.time)
        return Pair(monthName, year.toString())
    }
}
