package com.example

import com.example.data.local.ScheduledReminderEntity
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val testRatesToBdt = mapOf(
        "BDT" to 1.0,
        "USD" to 120.0,
        "EUR" to 130.5,
        "GBP" to 154.0,
        "INR" to 1.43,
        "JPY" to 0.8,
        "CNY" to 16.8,
        "SAR" to 32.0,
        "AED" to 32.67,
        "CAD" to 86.2,
        "AUD" to 78.4,
        "SGD" to 89.6,
        "MYR" to 27.3,
        "KWD" to 391.5
    )

    @Test
    fun currencyCatalog_containsAllRequiredIso4217Currencies() {
        val requiredCodes = listOf(
            "BDT", "USD", "EUR", "GBP", "INR", "JPY", "CNY",
            "SAR", "AED", "CAD", "AUD", "SGD", "MYR", "KWD"
        )
        for (code in requiredCodes) {
            val meta = CurrencyCatalog.getMetadata(code)
            assertEquals(code, meta.code)
            assertTrue(meta.symbol.isNotBlank())
        }
        assertEquals(0, CurrencyCatalog.getMetadata("JPY").decimalDigits)
        assertEquals(3, CurrencyCatalog.getMetadata("KWD").decimalDigits)
        assertEquals(2, CurrencyCatalog.getMetadata("BDT").decimalDigits)
        assertEquals(2, CurrencyCatalog.getMetadata("USD").decimalDigits)
        assertEquals(2, CurrencyCatalog.getMetadata("CNY").decimalDigits)
        assertEquals("ر.س", CurrencyCatalog.getMetadata("SAR").symbol)
        assertEquals("د.إ", CurrencyCatalog.getMetadata("AED").symbol)
    }

    @Test
    fun multiCurrencyConversion_convertsAccuratelyAcrossCurrencies() {
        val usdToBdt = CurrencyFormatter.convert(
            amount = 100.0,
            fromCurrency = "USD",
            toCurrency = "BDT",
            ratesToBdt = testRatesToBdt
        )
        assertEquals(12000.0, usdToBdt, 0.001)

        val bdtToUsd = CurrencyFormatter.convert(
            amount = 12000.0,
            fromCurrency = "BDT",
            toCurrency = "USD",
            ratesToBdt = testRatesToBdt
        )
        assertEquals(100.0, bdtToUsd, 0.001)

        val usdToJpy = CurrencyFormatter.convert(
            amount = 100.0,
            fromCurrency = "USD",
            toCurrency = "JPY",
            ratesToBdt = testRatesToBdt
        )
        assertEquals(15000.0, usdToJpy, 0.001)

        val original = 49.99
        val sameCurrency = CurrencyFormatter.convert(
            amount = original,
            fromCurrency = "EUR",
            toCurrency = "EUR",
            ratesToBdt = testRatesToBdt
        )
        assertEquals(original, sameCurrency, 0.00001)
    }

    @Test
    fun currencyFormatting_respectsGroupingDecimalsSpacingAndNegativeValues() {
        val bdtFormatted = CurrencyFormatter.formatCurrency(
            amount = 248650.5,
            currencyCode = "BDT",
            useSouthAsianGrouping = true
        )
        assertEquals("৳2,48,650.50", bdtFormatted)

        val usdFormatted = CurrencyFormatter.formatCurrency(
            amount = 248650.5,
            currencyCode = "USD",
            useSouthAsianGrouping = false
        )
        assertEquals("$248,650.50", usdFormatted)

        val jpyFormatted = CurrencyFormatter.formatCurrency(
            amount = 15420.75,
            currencyCode = "JPY",
            useSouthAsianGrouping = false
        )
        assertEquals("¥15,421", jpyFormatted)
        val jpyParts = CurrencyFormatter.splitCurrencyParts(
            amount = 15420.0,
            currencyCode = "JPY",
            useSouthAsianGrouping = false
        )
        assertEquals("", jpyParts.third)

        val kwdFormatted = CurrencyFormatter.formatCurrency(
            amount = 42.5,
            currencyCode = "KWD",
            useSouthAsianGrouping = false
        )
        assertEquals("د.ك 42.500", kwdFormatted)

        val sarFormatted = CurrencyFormatter.formatCurrency(
            amount = 1250.0,
            currencyCode = "SAR",
            useSouthAsianGrouping = false
        )
        assertEquals("ر.س 1,250.00", sarFormatted)

        val negUsd = CurrencyFormatter.formatCurrency(
            amount = -1250.25,
            currencyCode = "USD",
            useSouthAsianGrouping = false
        )
        assertEquals("-$1,250.25", negUsd)
    }

    @Test
    fun localNotificationPayload_preservesNativeCurrencyAndNeverHardcodesBdt() {
        // 1. USD recurring bill reminder when user base currency is BDT
        val usdBillReminder = ScheduledReminderEntity(
            id = 10,
            notificationId = HisabNotificationManager.computeStableNotificationId("BILL", 2),
            sourceType = "BILL",
            sourceId = 2,
            title = "Adobe Creative Cloud",
            subtitle = "Auto-pay via BRAC Visa Credit",
            amount = 24.0,
            currencyCode = "USD",
            triggerTimeMillis = System.currentTimeMillis() + 86_400_000L
        )
        val usdPayload = HisabNotificationManager.buildReminderNotificationPayload(
            reminder = usdBillReminder,
            baseCurrencyCode = "BDT",
            ratesToBdt = testRatesToBdt,
            useSouthAsianGrouping = true
        )
        assertEquals(HisabNotificationManager.CHANNEL_BILL_REMINDERS, usdPayload.channelId)
        assertEquals("$24.00 USD", usdPayload.primaryFormattedAmount)
        assertTrue(usdPayload.body.contains("$24.00 USD"))
        assertTrue(usdPayload.body.contains("≈ ৳2,880.00 BDT"))

        // 2. SAR savings goal milestone reminder when user base currency is USD
        val sarGoalReminder = ScheduledReminderEntity(
            id = 11,
            notificationId = HisabNotificationManager.computeStableNotificationId("GOAL", 3),
            sourceType = "GOAL",
            sourceId = 3,
            title = "Family Umrah Fund",
            subtitle = "Progress: ر.س 8,500.00 SAR of ر.س 15,000.00 SAR",
            amount = 15000.0,
            currencyCode = "SAR",
            triggerTimeMillis = System.currentTimeMillis() + 86_400_000L
        )
        val sarPayload = HisabNotificationManager.buildReminderNotificationPayload(
            reminder = sarGoalReminder,
            baseCurrencyCode = "USD",
            ratesToBdt = testRatesToBdt,
            useSouthAsianGrouping = false
        )
        assertEquals(HisabNotificationManager.CHANNEL_GOAL_MILESTONES, sarPayload.channelId)
        assertEquals("ر.س 15,000.00 SAR", sarPayload.primaryFormattedAmount)
        assertFalse(sarPayload.primaryFormattedAmount.contains("৳"))
        assertTrue(sarPayload.body.contains("ر.س 15,000.00 SAR"))
        assertTrue(sarPayload.body.contains("USD"))

        // 3. BDT bill when base currency is also BDT (no redundant conversion suffix)
        val bdtReminder = ScheduledReminderEntity(
            id = 12,
            notificationId = HisabNotificationManager.computeStableNotificationId("BILL", 1),
            sourceType = "BILL",
            sourceId = 1,
            title = "Amber IT Fiber Internet",
            subtitle = "Auto-pay via bKash Personal",
            amount = 1600.0,
            currencyCode = "BDT",
            triggerTimeMillis = System.currentTimeMillis() + 86_400_000L
        )
        val bdtPayload = HisabNotificationManager.buildReminderNotificationPayload(
            reminder = bdtReminder,
            baseCurrencyCode = "BDT",
            ratesToBdt = testRatesToBdt,
            useSouthAsianGrouping = true
        )
        assertEquals("৳1,600.00 BDT", bdtPayload.primaryFormattedAmount)
        assertFalse(bdtPayload.body.contains("≈"))
    }

    @Test
    fun notificationIdAndQuietHours_computeAccurately() {
        val idRec1 = HisabNotificationManager.computeStableNotificationId("BILL", 1)
        val idRec2 = HisabNotificationManager.computeStableNotificationId("BILL", 2)
        val idDebt1 = HisabNotificationManager.computeStableNotificationId("DEBT", 1)
        assertEquals(idRec1, HisabNotificationManager.computeStableNotificationId("BILL", 1))
        assertNotEquals(idRec1, idRec2)
        assertNotEquals(idRec1, idDebt1)

        // Quiet hours 22:00 -> 07:00
        assertTrue(HisabNotificationManager.isWithinQuietHours(23, true, 22, 7))
        assertTrue(HisabNotificationManager.isWithinQuietHours(2, true, 22, 7))
        assertFalse(HisabNotificationManager.isWithinQuietHours(14, true, 22, 7))
        assertFalse(HisabNotificationManager.isWithinQuietHours(23, false, 22, 7))
    }
}
