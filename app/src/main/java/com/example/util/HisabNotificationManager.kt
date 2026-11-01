package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppNotificationEntity
import com.example.data.local.HisabDatabase
import com.example.data.local.ScheduledReminderEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

data class FormattedNotificationPayload(
    val notificationId: Int,
    val channelId: String,
    val title: String,
    val body: String,
    val primaryFormattedAmount: String,
    val convertedBaseSummary: String?,
    val categoryTag: String
)

/**
 * Complete offline local notification, reminder scheduling, currency-aware formatting,
 * and permission helper for HisabMate.
 */
object HisabNotificationManager {

    const val CHANNEL_BILL_REMINDERS = "hisab_bill_reminders"
    const val CHANNEL_BUDGET_ALERTS = "hisab_budget_alerts"
    const val CHANNEL_DEBT_REMINDERS = "hisab_debt_reminders"
    const val CHANNEL_GOAL_MILESTONES = "hisab_goal_milestones"
    const val CHANNEL_CUSTOM_REMINDERS = "hisab_custom_reminders"
    const val CHANNEL_TRANSACTION_UPDATES = "hisab_transaction_updates"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SOURCE_TYPE = "extra_source_type"
    const val EXTRA_SOURCE_ID = "extra_source_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_SUBTITLE = "extra_subtitle"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_CURRENCY_CODE = "extra_currency_code"
    const val EXTRA_FREQUENCY = "extra_frequency"

    /**
     * Generates a deterministic, collision-free notification ID per sourceType and sourceId.
     */
    fun computeStableNotificationId(sourceType: String, sourceId: Int): Int {
        val prefix = when (sourceType.uppercase()) {
            "BILL" -> 10_000
            "DEBT" -> 20_000
            "BUDGET" -> 30_000
            "GOAL" -> 40_000
            else -> 50_000
        }
        return prefix + abs(sourceId)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_BILL_REMINDERS,
                "Bill & Subscription Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming recurring bill and subscription payment alerts in their native currency"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                "Budget Threshold & Limit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when category spending reaches your configured threshold or exceeds budget"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_DEBT_REMINDERS,
                "Debt & Lending Settlement Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for money lent (receivable) and borrowed (payable) due dates"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_GOAL_MILESTONES,
                "Savings Goal Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Progress celebrations and contribution reminders for financial goals"
            },
            NotificationChannel(
                CHANNEL_CUSTOM_REMINDERS,
                "Custom Financial Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "User-scheduled financial check-ins and custom payment reminders"
            }
            ,
            NotificationChannel(
                CHANNEL_TRANSACTION_UPDATES,
                "Transaction Activity",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "A confirmation whenever an expense, income or transfer is successfully saved"
                enableVibration(true)
            }
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }

    /**
     * Pure, testable builder that formats a notification using the record's exact ISO 4217 currency
     * (never hardcoding BDT) and optionally includes the converted base-currency equivalent.
     */
    fun buildReminderNotificationPayload(
        reminder: ScheduledReminderEntity,
        baseCurrencyCode: String,
        ratesToBdt: Map<String, Double>,
        useSouthAsianGrouping: Boolean = true
    ): FormattedNotificationPayload {
        val recordCurrency = reminder.currencyCode.uppercase()
        val baseCode = baseCurrencyCode.uppercase()
        val primaryFormatted = CurrencyFormatter.formatCurrency(
            amount = reminder.amount,
            currencyCode = recordCurrency,
            useSouthAsianGrouping = useSouthAsianGrouping,
            includeCodeSuffix = true
        )

        val convertedSummary: String? = if (!recordCurrency.equals(baseCode, ignoreCase = true)) {
            val converted = CurrencyFormatter.convert(
                amount = reminder.amount,
                fromCurrency = recordCurrency,
                toCurrency = baseCode,
                ratesToBdt = ratesToBdt
            )
            val convFormatted = CurrencyFormatter.formatCurrency(
                amount = converted,
                currencyCode = baseCode,
                useSouthAsianGrouping = useSouthAsianGrouping,
                includeCodeSuffix = true
            )
            "≈ $convFormatted"
        } else {
            null
        }

        val amountWithConversion = if (convertedSummary != null) {
            "$primaryFormatted ($convertedSummary)"
        } else {
            primaryFormatted
        }

        val (channelId, titleText, bodyText, tag) = when (reminder.sourceType.uppercase()) {
            "BILL" -> Quad(
                CHANNEL_BILL_REMINDERS,
                "Upcoming Bill: ${reminder.title}",
                "${reminder.subtitle} • Due amount: $amountWithConversion",
                "BILL"
            )
            "BUDGET" -> Quad(
                CHANNEL_BUDGET_ALERTS,
                "Budget Alert: ${reminder.title}",
                "${reminder.subtitle} • Limit: $amountWithConversion",
                "BUDGET"
            )
            "DEBT" -> Quad(
                CHANNEL_DEBT_REMINDERS,
                reminder.title,
                "${reminder.subtitle} • Remaining settlement: $amountWithConversion",
                "DEBT"
            )
            "GOAL" -> Quad(
                CHANNEL_GOAL_MILESTONES,
                "Savings Goal: ${reminder.title}",
                "${reminder.subtitle} • Target: $amountWithConversion",
                "GOAL"
            )
            else -> Quad(
                CHANNEL_CUSTOM_REMINDERS,
                reminder.title,
                "${reminder.subtitle} • Amount: $amountWithConversion",
                "REMINDER"
            )
        }

        return FormattedNotificationPayload(
            notificationId = reminder.notificationId,
            channelId = channelId,
            title = titleText,
            body = bodyText,
            primaryFormattedAmount = primaryFormatted,
            convertedBaseSummary = convertedSummary,
            categoryTag = tag
        )
    }

    /**
     * Builds a local confirmation for a transaction that has already been committed to Room.
     * Amounts are always displayed in the transaction's original ISO 4217 currency.
     */
    fun buildTransactionNotificationPayload(
        transaction: TransactionEntity,
        isUpdate: Boolean = false,
        useSouthAsianGrouping: Boolean = true
    ): FormattedNotificationPayload {
        val currencyCode = transaction.currencyCode.uppercase()
        val formattedAmount = CurrencyFormatter.formatCurrency(
            amount = transaction.amount,
            currencyCode = currencyCode,
            useSouthAsianGrouping = useSouthAsianGrouping,
            includeCodeSuffix = true
        )
        val typeLabel = when (transaction.type.uppercase()) {
            "INCOME" -> "Income"
            "TRANSFER" -> "Transfer"
            else -> "Expense"
        }
        val titleText = when {
            isUpdate -> "$typeLabel updated"
            transaction.type.equals("INCOME", true) -> "Income received"
            transaction.type.equals("TRANSFER", true) -> "Transfer completed"
            else -> "Expense recorded"
        }
        val details = when (transaction.type.uppercase()) {
            "TRANSFER" -> "Moved $formattedAmount • ${transaction.accountLabel}"
            "INCOME" -> "${transaction.title} • $formattedAmount • ${transaction.categoryName} • ${transaction.accountLabel}"
            else -> "${transaction.title} • $formattedAmount • ${transaction.categoryName} • ${transaction.accountLabel}"
        }
        val summary = if (transaction.merchant.isNotBlank() && transaction.merchant != transaction.title) {
            "$details • ${transaction.merchant}"
        } else {
            details
        }

        return FormattedNotificationPayload(
            notificationId = 60_000 + (transaction.id % 1_000_000),
            channelId = CHANNEL_TRANSACTION_UPDATES,
            title = titleText,
            body = summary,
            primaryFormattedAmount = formattedAmount,
            convertedBaseSummary = null,
            categoryTag = "TRANSACTION"
        )
    }

    fun isWithinQuietHours(
        currentHourOfDay: Int,
        quietHoursEnabled: Boolean,
        startHour: Int = 22,
        endHour: Int = 7
    ): Boolean {
        if (!quietHoursEnabled) return false
        return if (startHour > endHour) {
            // Spans midnight (e.g., 22:00 to 07:00)
            currentHourOfDay >= startHour || currentHourOfDay < endHour
        } else {
            currentHourOfDay in startHour until endHour
        }
    }

    fun isCategoryEnabledInPreferences(sourceType: String, prefs: UserPreferencesEntity): Boolean {
        return when (sourceType.uppercase()) {
            "BILL" -> prefs.billRemindersEnabled
            "BUDGET" -> prefs.budgetAlertsEnabled
            "DEBT" -> prefs.debtRemindersEnabled
            "GOAL" -> prefs.goalMilestonesEnabled
            else -> true
        }
    }

    fun scheduleReminderAlarm(context: Context, reminder: ScheduledReminderEntity) {
        if (!reminder.isEnabled) {
            cancelReminderAlarm(context, reminder.notificationId)
            return
        }
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val leadOffsetMs = reminder.leadTimeDays.coerceAtLeast(0) * 24L * 60L * 60L * 1000L
            val now = System.currentTimeMillis()
            val effectiveTrigger = (reminder.triggerTimeMillis - leadOffsetMs).coerceAtLeast(now + 60_000L)

            val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_ID, reminder.notificationId)
                putExtra(EXTRA_SOURCE_TYPE, reminder.sourceType)
                putExtra(EXTRA_SOURCE_ID, reminder.sourceId)
                putExtra(EXTRA_TITLE, reminder.title)
                putExtra(EXTRA_SUBTITLE, reminder.subtitle)
                putExtra(EXTRA_AMOUNT, reminder.amount)
                putExtra(EXTRA_CURRENCY_CODE, reminder.currencyCode)
                putExtra(EXTRA_FREQUENCY, reminder.frequency)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use battery-safe inexact idle alarm that does not require SCHEDULE_EXACT_ALARM permission
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                effectiveTrigger,
                pendingIntent
            )
        }
    }

    fun cancelReminderAlarm(context: Context, notificationId: Int) {
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    suspend fun rescheduleAllActiveReminders(context: Context) {
        val db = HisabDatabase.getInstance(context)
        val dao = db.hisabDao()
        val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
        val reminders = dao.getAllScheduledRemindersOnce()
        val now = System.currentTimeMillis()

        reminders.forEach { rem ->
            val categoryAllowed = isCategoryEnabledInPreferences(rem.sourceType, prefs)
            if (rem.isEnabled && categoryAllowed) {
                var targetTime = rem.triggerTimeMillis
                // Roll forward recurring reminders whose trigger time has already passed
                if (targetTime < now && rem.frequency.uppercase() != "ONCE") {
                    val stepMs = when (rem.frequency.uppercase()) {
                        "DAILY" -> 24L * 60L * 60L * 1000L
                        "WEEKLY" -> 7L * 24L * 60L * 60L * 1000L
                        "YEARLY" -> 365L * 24L * 60L * 60L * 1000L
                        else -> 30L * 24L * 60L * 60L * 1000L
                    }
                    while (targetTime < now) {
                        targetTime += stepMs
                    }
                    val updated = rem.copy(triggerTimeMillis = targetTime)
                    dao.updateScheduledReminder(updated)
                    scheduleReminderAlarm(context, updated)
                } else {
                    scheduleReminderAlarm(context, rem)
                }
            } else {
                cancelReminderAlarm(context, rem.notificationId)
            }
        }
    }

    fun postSystemNotification(
        context: Context,
        payload: FormattedNotificationPayload,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ): Boolean {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return false

        return runCatching {
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingTap = PendingIntent.getActivity(
                context,
                payload.notificationId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, payload.channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingTap)

            if (!soundEnabled) {
                builder.setSilent(true)
            }
            if (!vibrationEnabled) {
                builder.setVibrate(longArrayOf(0L))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@runCatching false
            }

            try {
                NotificationManagerCompat.from(context).notify(payload.notificationId, builder.build())
                true
            } catch (_: SecurityException) {
                false
            }
        }.getOrDefault(false)
    }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}

/**
 * BroadcastReceiver that handles firing scheduled local financial notifications.
 */
class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HisabDatabase.getInstance(context)
                val dao = db.hisabDao()
                val prefs = dao.getUserPreferencesOnce() ?: UserPreferencesEntity()
                val rates = dao.getExchangeRatesList().associate { it.currencyCode.uppercase() to it.rateToBdt }
                    .ifEmpty { CurrencyCatalog.supportedCurrencies.associate { it.code to it.defaultRateToBdt } }

                val notificationId = intent.getIntExtra(HisabNotificationManager.EXTRA_NOTIFICATION_ID, 0)
                val stored = dao.getScheduledReminderByNotificationId(notificationId)
                val reminder = stored ?: ScheduledReminderEntity(
                    notificationId = notificationId.takeIf { it != 0 } ?: 50001,
                    sourceType = intent.getStringExtra(HisabNotificationManager.EXTRA_SOURCE_TYPE) ?: "BILL",
                    sourceId = intent.getIntExtra(HisabNotificationManager.EXTRA_SOURCE_ID, 1),
                    title = intent.getStringExtra(HisabNotificationManager.EXTRA_TITLE) ?: "HisabMate Payment Reminder",
                    subtitle = intent.getStringExtra(HisabNotificationManager.EXTRA_SUBTITLE) ?: "Scheduled reminder",
                    amount = intent.getDoubleExtra(HisabNotificationManager.EXTRA_AMOUNT, 0.0),
                    currencyCode = intent.getStringExtra(HisabNotificationManager.EXTRA_CURRENCY_CODE) ?: prefs.baseCurrencyCode,
                    triggerTimeMillis = System.currentTimeMillis(),
                    frequency = intent.getStringExtra(HisabNotificationManager.EXTRA_FREQUENCY) ?: "ONCE"
                )

                if (!reminder.isEnabled || !HisabNotificationManager.isCategoryEnabledInPreferences(reminder.sourceType, prefs)) {
                    return@launch
                }

                // Prevent duplicate rapid notifications within 5 minutes
                val now = System.currentTimeMillis()
                if (now - reminder.lastNotifiedMillis < 5 * 60_000L) {
                    return@launch
                }

                val payload = HisabNotificationManager.buildReminderNotificationPayload(
                    reminder = reminder,
                    baseCurrencyCode = prefs.baseCurrencyCode,
                    ratesToBdt = rates,
                    useSouthAsianGrouping = prefs.useSouthAsianGrouping
                )

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val inQuietHours = HisabNotificationManager.isWithinQuietHours(
                    currentHourOfDay = currentHour,
                    quietHoursEnabled = prefs.quietHoursEnabled,
                    startHour = prefs.quietHoursStartHour,
                    endHour = prefs.quietHoursEndHour
                )

                // Always log into the in-app notification center
                dao.insertNotification(
                    AppNotificationEntity(
                        title = payload.title,
                        message = payload.body,
                        categoryTag = payload.categoryTag,
                        timestamp = now,
                        isRead = false,
                        accentColorHex = reminder.accentColorHex
                    )
                )

                if (!inQuietHours) {
                    HisabNotificationManager.postSystemNotification(
                        context = context,
                        payload = payload,
                        soundEnabled = prefs.soundEnabled,
                        vibrationEnabled = prefs.vibrationEnabled
                    )
                }

                // Advance recurring reminders or mark one-off reminder completed
                if (stored != null) {
                    if (stored.frequency.uppercase() == "ONCE") {
                        dao.updateScheduledReminder(stored.copy(isEnabled = false, lastNotifiedMillis = now))
                    } else {
                        val stepMs = when (stored.frequency.uppercase()) {
                            "DAILY" -> 24L * 60L * 60L * 1000L
                            "WEEKLY" -> 7L * 24L * 60L * 60L * 1000L
                            "YEARLY" -> 365L * 24L * 60L * 60L * 1000L
                            else -> 30L * 24L * 60L * 60L * 1000L
                        }
                        val nextReminder = stored.copy(
                            triggerTimeMillis = stored.triggerTimeMillis + stepMs,
                            lastNotifiedMillis = now
                        )
                        dao.updateScheduledReminder(nextReminder)
                        HisabNotificationManager.scheduleReminderAlarm(context, nextReminder)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * BroadcastReceiver that restores and recalculates all scheduled reminders after device reboot,
 * app update, system clock adjustment, or timezone change.
 */
class ReminderBootAndTimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    HisabNotificationManager.rescheduleAllActiveReminders(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
