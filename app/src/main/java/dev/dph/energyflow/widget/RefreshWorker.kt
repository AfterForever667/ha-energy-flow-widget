package dev.dph.energyflow.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dev.dph.energyflow.data.EnergyRepository

/**
 * Refreshes every placed [EnergyWidget] instance from Home Assistant. Used both for the
 * WorkManager 15-min periodic schedule and for one-off "refresh now" work (widget just added, or
 * the manual refresh button — see [RefreshScheduler] and [RefreshAction]).
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = EnergyRepository(applicationContext)
        val manager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = manager.getGlanceIds(EnergyWidget::class.java)

        if (glanceIds.isEmpty()) return Result.success()

        for (glanceId in glanceIds) {
            repository.refresh(glanceId)
        }
        EnergyWidget().updateAll(applicationContext)

        return Result.success()
    }
}
