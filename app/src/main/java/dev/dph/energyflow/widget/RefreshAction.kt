package dev.dph.energyflow.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dev.dph.energyflow.data.EnergyRepository

/** Bound to the widget's manual refresh (↻) button. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // A single-widget immediate fetch, not the fleet-wide worker: the user tapped refresh on
        // this widget specifically and wants it to feel instant.
        val repository = EnergyRepository(context)
        repository.refresh(glanceId)
        EnergyWidget().update(context, glanceId)
    }
}
