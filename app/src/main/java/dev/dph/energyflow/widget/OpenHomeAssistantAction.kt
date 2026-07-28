package dev.dph.energyflow.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import dev.dph.energyflow.data.ConnectionPrefs

private const val HA_COMPANION_PACKAGE = "io.homeassistant.companion.android"

/** Bound to the widget's optional "open Home Assistant" header button. */
class OpenHomeAssistantAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(HA_COMPANION_PACKAGE)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val baseUrl = ConnectionPrefs(context, appWidgetId.toString()).baseUrl

        val intent = launchIntent
            ?: baseUrl.takeIf { it.isNotBlank() }?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
            ?: return

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
