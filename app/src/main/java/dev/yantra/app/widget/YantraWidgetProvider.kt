package dev.yantra.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.yantra.app.MainActivity
import dev.yantra.app.R
import dev.yantra.app.calendar.YantraCalendarEngine
import dev.yantra.app.engine.AstronomyEngine
import dev.yantra.app.engine.EphemerisAssets
import dev.yantra.app.engine.Observer
import dev.yantra.app.engine.SwissEphemeris
import java.time.ZonedDateTime

class YantraWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> updateAllWidgets(context)
        }
    }

    companion object {
        private val defaultObserver = Observer(latitude = 45.5019, longitude = -73.5674)

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, YantraWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { appWidgetId ->
                updateWidget(context, manager, appWidgetId)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_yantra)
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.yantra_widget_root, pendingIntent)

            val now = ZonedDateTime.now()
            val state = runCatching {
                val ephemerisDirectory = EphemerisAssets(context).install()
                val engine = YantraCalendarEngine(
                    AstronomyEngine(SwissEphemeris(ephemerisDirectory.absolutePath))
                )
                engine.compute(now, defaultObserver)
            }.getOrElse {
                YantraCalendarEngine().compute(now, defaultObserver)
            }

            val bitmap = YantraWidgetRenderer.render(state, now)
            views.setImageViewBitmap(R.id.yantra_widget_image, bitmap)
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
