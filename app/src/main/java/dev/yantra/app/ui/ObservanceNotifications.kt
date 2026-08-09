package dev.yantra.app.ui

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
import dev.yantra.app.MainActivity
import dev.yantra.app.R
import dev.yantra.app.calendar.YantraCalendarEngine
import dev.yantra.app.calendar.YantraState
import dev.yantra.app.engine.AstronomyEngine
import dev.yantra.app.engine.EphemerisAssets
import dev.yantra.app.engine.SwissEphemeris
import java.time.ZonedDateTime

private const val NOTIFICATION_PREFS = "yantra_notifications"
private const val ENABLED_KEY = "enabled"
private const val CHANNEL_ID = "yantra_observances"
private const val DAILY_REQUEST = 7101
private const val CANCEL_REQUEST = 7102
private const val NOTIFICATION_ID = 7103

internal fun notificationsEnabled(context: Context): Boolean =
    context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED_KEY, true)

internal fun setNotificationsEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED_KEY, enabled).apply()
    if (enabled) ObservanceNotificationScheduler.schedule(context) else ObservanceNotificationScheduler.cancel(context)
}

internal object ObservanceNotificationScheduler {
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Yantra observances", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Quiet status-bar reminders for important calendar days"
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        if (!notificationsEnabled(context)) return
        createChannel(context)
        val zone = loadObserverLocation(context).zoneId
        val now = ZonedDateTime.now(zone)
        var next = now.withHour(6).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val intent = PendingIntent.getBroadcast(
            context,
            DAILY_REQUEST,
            Intent(context, ObservanceNotificationReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), intent)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(PendingIntent.getBroadcast(context, DAILY_REQUEST, Intent(context, ObservanceNotificationReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        manager.cancel(PendingIntent.getBroadcast(context, CANCEL_REQUEST, Intent(context, ObservanceNotificationCancelReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun reschedule(context: Context) {
        cancel(context)
        schedule(context)
    }
}

private data class ObservanceOccurrence(val label: String, val key: String, val at: ZonedDateTime)

private fun engine(context: Context): YantraCalendarEngine {
    val ephemeris = EphemerisAssets(context).install()
    val ayanamsa = selectedAyanamsa(loadAyanamsaId(context))
    return YantraCalendarEngine(
        AstronomyEngine(SwissEphemeris(ephemeris.absolutePath, ayanamsa)),
        ayanamsa = ayanamsa,
    )
}

private fun observance(context: Context, state: YantraState, previous: YantraState): Pair<String, String>? {
    loadSpecialDays(context).firstOrNull { it.matches(state) }?.let { return it.name to "special:${it.name}" }
    loadUserEvents(context).firstOrNull { it.matches(state) }?.let { return it.name to "user:${it.identityKey()}" }
    val tithiNumber = (state.tithi.index % 15) + 1
    if (state.paksha == "Krishna" && tithiNumber == 15) return "Amavasya" to "amavasya:${state.lunarMonth}"
    FestivalCatalog.match(state, previous)?.takeIf { it.rank == FestivalRank.Major }?.let { return it.name to "festival:${it.name}" }
    return null
}

class ObservanceNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!notificationsEnabled(context)) return
        val observerLocation = loadObserverLocation(context)
        val calendar = engine(context)
        val dayStart = ZonedDateTime.now(observerLocation.zoneId).toLocalDate().atStartOfDay(observerLocation.zoneId)
        val occurrence = (0..47).firstNotNullOfOrNull { halfHour ->
            val at = dayStart.plusMinutes(halfHour * 30L)
            val state = calendar.compute(at, observerLocation.observer)
            val previous = calendar.compute(at.minusHours(6), observerLocation.observer)
            observance(context, state, previous)?.let { ObservanceOccurrence(it.first, it.second, at) }
        }
        if (occurrence != null && canNotify(context)) {
            val end = findEnd(context, calendar, observerLocation, occurrence)
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(occurrence.label)
                .setContentText("Today in ${observerLocation.label}")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setTimeoutAfter((end.toInstant().toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(60_000L))
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            scheduleCancellation(context, end)
        }
        ObservanceNotificationScheduler.schedule(context)
    }

    private fun findEnd(context: Context, calendar: YantraCalendarEngine, location: ObserverLocation, occurrence: ObservanceOccurrence): ZonedDateTime {
        var cursor = occurrence.at
        val limit = cursor.plusDays(2)
        while (cursor.isBefore(limit)) {
            cursor = cursor.plusMinutes(10)
            val state = calendar.compute(cursor, location.observer)
            val previous = calendar.compute(cursor.minusHours(6), location.observer)
            if (observance(context, state, previous)?.second != occurrence.key) return refineEnd(context, calendar, location, occurrence, cursor.minusMinutes(10), cursor)
        }
        return occurrence.at.toLocalDate().plusDays(1).atStartOfDay(location.zoneId)
    }

    private fun refineEnd(context: Context, calendar: YantraCalendarEngine, location: ObserverLocation, occurrence: ObservanceOccurrence, start: ZonedDateTime, end: ZonedDateTime): ZonedDateTime {
        var low = start
        var high = end
        while (java.time.Duration.between(low, high).toMinutes() > 1) {
            val middle = low.plusSeconds(java.time.Duration.between(low, high).seconds / 2)
            val state = calendar.compute(middle, location.observer)
            val previous = calendar.compute(middle.minusHours(6), location.observer)
            if (observance(context, state, previous)?.second == occurrence.key) low = middle else high = middle
        }
        return high
    }

    private fun scheduleCancellation(context: Context, end: ZonedDateTime) {
        val pending = PendingIntent.getBroadcast(context, CANCEL_REQUEST, Intent(context, ObservanceNotificationCancelReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end.toInstant().toEpochMilli(), pending)
    }
}

class ObservanceNotificationCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

class ObservanceNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)) {
            ObservanceNotificationScheduler.reschedule(context)
        }
    }
}

private fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
