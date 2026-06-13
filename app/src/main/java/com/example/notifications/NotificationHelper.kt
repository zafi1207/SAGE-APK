package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.Bill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "sage_bill_reminders"
    private const val CHANNEL_NAME = "Bill Reminders"
    private const val ALARM_REQ_CODE = 100

    /**
     * Set up the daily alarm at 08:00 AM.
     */
    fun scheduleDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If 8:00 AM has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d(TAG, "Scheduling daily alarm at: ${calendar.time}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm, falling back to inexact.", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel the daily repeating alarm.
     */
    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Creates the notifications channel for API 26+
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Offline bill reminders"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dismisses all active notifications posted by Sage.
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
    }

    /**
     * Queries database to active bills and displays exact reminder notifications if due.
     */
    fun checkAndShowNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.sageDao()

                val settings = dao.getSettings() ?: com.example.data.SettingsEntity()

                // If disabled, cancel everything and exit early
                if (!settings.isNotificationsEnabled) {
                    cancelAllNotifications(context)
                    return@launch
                }

                createNotificationChannel(context)

                // Fetch unpaid bills
                val unpaidBills = dao.getAllBills().filter { !it.isPaid && !it.isCompleted }
                val users = dao.getAllUsers()

                // Calculate today and tomorrow calendar dates
                val todayCal = Calendar.getInstance()
                val todayY = todayCal.get(Calendar.YEAR)
                val todayM = todayCal.get(Calendar.MONTH) + 1
                val todayD = todayCal.get(Calendar.DAY_OF_MONTH)

                val tomCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomY = tomCal.get(Calendar.YEAR)
                val tomM = tomCal.get(Calendar.MONTH) + 1
                val tomD = tomCal.get(Calendar.DAY_OF_MONTH)

                // Segregate bills due today and tomorrow
                val dueToday = unpaidBills.filter { it.day == todayD && it.month == todayM && it.year == todayY }
                val dueTomorrow = unpaidBills.filter { it.day == tomD && it.month == tomM && it.year == tomY }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return@launch

                // 1. Process Due Today notifications
                if (dueToday.isNotEmpty()) {
                    if (dueToday.size == 1) {
                        val bill = dueToday[0]
                        val ownerName = users.find { it.id == bill.ownerId }?.name ?: if (bill.ownerId == 1) "Rama" else "Nadiya"
                        val formattedAmount = formatRupiah(bill.amount)
                        val monthAbbrev = getMonthAbbrev(bill.month, settings.language)

                        val title = "⚠️ Bill Due Today"
                        val body = "$ownerName - ${bill.name}\nDue Today ($todayD $monthAbbrev)\n$formattedAmount"

                        postNotification(context, notificationManager, bill.id, title, body)
                    } else {
                        // Grouped bills due today
                        val count = dueToday.size
                        val totalAmount = dueToday.sumOf { it.amount }
                        val formattedTotal = formatRupiah(totalAmount)

                        // Collect unique owner names
                        val uniqueOwners = dueToday.map { bill ->
                            users.find { it.id == bill.ownerId }?.name ?: if (bill.ownerId == 1) "Rama" else "Nadiya"
                        }.distinct()
                        val ownersJoined = uniqueOwners.joinToString(" & ")

                        val title = "⚠️ $count Bills Due Today"
                        val body = "$ownersJoined\nTotal Due: $formattedTotal"

                        // Use a fixed higher notification ID for the group notification
                        postNotification(context, notificationManager, 9999, title, body)
                    }
                } else {
                    // Clear grouped notification if no more multiple bills today
                    notificationManager.cancel(9999)
                }

                // 2. Process Due Tomorrow notifications
                // Dismiss any yesterday's tomorrow notifications if needed, or simply let specific bill IDs overwrite themselves
                for (bill in dueTomorrow) {
                    val ownerName = users.find { it.id == bill.ownerId }?.name ?: if (bill.ownerId == 1) "Rama" else "Nadiya"
                    val formattedAmount = formatRupiah(bill.amount)

                    val title = "🔔 Upcoming Bill Reminder"
                    val body = "${ownerName}'s ${bill.name}\nDue Tomorrow\n$formattedAmount"

                    postNotification(context, notificationManager, bill.id, title, body)
                }

                // Auto-cancel notifications of paid bills
                val paidBills = dao.getAllBills().filter { it.isPaid || it.isCompleted }
                for (bill in paidBills) {
                    notificationManager.cancel(bill.id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndShowNotifications", e)
            }
        }
    }

    private fun postNotification(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_bills", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Built standard notification with the custom Sage Logo!
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.sage_logo) // Use sage_logo for notification icon as requested
            .setContentTitle(title)
            .setContentText(body.replace("\n", " | ")) // Single line ticker fallback
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Multi-line rich body display
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun formatRupiah(value: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        var result = formatter.format(value)
        if (result.contains(",")) {
            result = result.substringBefore(",")
        }
        result = result.replace("Rp ", "Rp ").replace("Rp", "Rp ")
        while (result.contains("  ")) {
            result = result.replace("  ", " ")
        }
        return result.trim()
    }

    private fun getMonthAbbrev(month: Int, lang: String): String {
        val monthsEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthsId = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        val idx = (month - 1).coerceIn(0, 11)
        return if (lang.equals("id", ignoreCase = true)) monthsId[idx] else monthsEn[idx]
    }
}

/**
 * Executes a background check daily at 08:00 AM, posts local notifications and chains the next day alarm safely.
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("NotificationReceiver", "Fired! Checking bills...")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Perform check and update notifications offline
                NotificationHelper.checkAndShowNotifications(context)
                // Schedule the next alarm for tomorrow morning at 08:00 AM
                NotificationHelper.scheduleDailyAlarm(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Restores and reschedules the offline Alarm schedules upon phone boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted. Rescheduling Sage reminders...")
            NotificationHelper.scheduleDailyAlarm(context)
        }
    }
}
