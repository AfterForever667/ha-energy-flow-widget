package dev.dph.energyflow.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.dph.energyflow.data.ConnectionPrefs

class EnergyWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = EnergyWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // First widget instance placed on a home screen: start the 15-min background refresh.
        RefreshScheduler.schedulePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed: stop burning battery on a schedule nobody needs.
        RefreshScheduler.cancelPeriodic(context)
        super.onDisabled(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Glance clears its own per-instance Preferences state automatically, but the HA
        // connection (EncryptedSharedPreferences, keyed by appWidgetId) is our own store and
        // would otherwise survive forever. If Android later reuses a freed appWidgetId for a
        // new widget, that new instance would silently inherit an old, unrelated HA connection.
        for (id in appWidgetIds) {
            ConnectionPrefs(context, id.toString()).clear()
        }
        super.onDeleted(context, appWidgetIds)
    }
}
