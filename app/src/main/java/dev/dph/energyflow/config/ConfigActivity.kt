package dev.dph.energyflow.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Doubles as: (1) the launcher entry point, for setting up the HA connection before any widget
 * exists, and (2) the per-widget `APPWIDGET_CONFIGURE` screen, for remapping one widget
 * instance's entities. See AndroidManifest.xml — both intent-filters point here.
 */
class ConfigActivity : ComponentActivity() {

    private val viewModel: ConfigViewModel by viewModels()
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readWidgetIdAndLoad(intent)
        setContent {
            val state by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            // Match this screen's accent to the phone's own Material You system accent (the
            // same source themed app icons in Settings draw from) instead of the generic
            // Material3 purple default, on Android 12+ where that's available.
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && state.isDarkTheme -> dynamicDarkColorScheme(context)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
                state.isDarkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen(
                        state = state,
                        onToggleUiTheme = viewModel::onToggleUiTheme,
                        onBaseUrlChange = viewModel::onBaseUrlChange,
                        onTokenChange = viewModel::onTokenChange,
                        onTestConnection = viewModel::testConnectionAndLoadEntities,
                        onEntityChange = viewModel::onEntityChange,
                        onLabelChange = viewModel::onLabelChange,
                        onBatteryCapacitySourceChange = viewModel::onBatteryCapacitySourceChange,
                        onBatteryCapacityManualChange = viewModel::onBatteryCapacityManualChange,
                        onBatteryCapacityEntityChange = viewModel::onBatteryCapacityEntityChange,
                        onShowOpenHaButtonChange = viewModel::onShowOpenHaButtonChange,
                        onShowSettingsButtonChange = viewModel::onShowSettingsButtonChange,
                        onShowTitleChange = viewModel::onShowTitleChange,
                        onShowHeaderChange = viewModel::onShowHeaderChange,
                        onTitleTextChange = viewModel::onTitleTextChange,
                        onShowSolarCardChange = viewModel::onShowSolarCardChange,
                        onShowGridCardChange = viewModel::onShowGridCardChange,
                        onShowBatteryCardChange = viewModel::onShowBatteryCardChange,
                        onShowHomeCardChange = viewModel::onShowHomeCardChange,
                        onShowFlowCardChange = viewModel::onShowFlowCardChange,
                        onUse24HourFormatChange = viewModel::onUse24HourFormatChange,
                        onFontSizeChange = viewModel::onFontSizeChange,
                        onUseCustomWidgetBackgroundChange = viewModel::onUseCustomWidgetBackgroundChange,
                        onWidgetBackgroundArgbChange = viewModel::onWidgetBackgroundArgbChange,
                        onUseCustomCardBackgroundChange = viewModel::onUseCustomCardBackgroundChange,
                        onCardBackgroundArgbChange = viewModel::onCardBackgroundArgbChange,
                        onUseCustomPrimaryTextColorChange = viewModel::onUseCustomPrimaryTextColorChange,
                        onPrimaryTextArgbChange = viewModel::onPrimaryTextArgbChange,
                        onCornerRadiusChange = viewModel::onCornerRadiusChange,
                        onResetAppearance = viewModel::resetAppearanceToDefaults,
                        onSave = {
                            viewModel.save(appWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }) {
                                finishConfig()
                            }
                        },
                    )
                }
            }
        }
    }

    // Android can reuse this Activity's task instead of starting a fresh one — e.g. FLAG_ACTIVITY_NEW_TASK
    // (used both by the system's APPWIDGET_CONFIGURE flow and by the widget's own "open settings"
    // button) brings an existing backgrounded ConfigActivity task to the front and delivers the new
    // intent here rather than to onCreate. Without this override, that reused screen would keep
    // showing the PREVIOUS widget's settings while silently saving over the previous widget instead
    // of the one the user actually meant to configure.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readWidgetIdAndLoad(intent)
    }

    private fun readWidgetIdAndLoad(intent: Intent?) {
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Android widget-host contract: until RESULT_OK is explicitly returned, the host discards
        // the placement (see finishConfig()). Setting this up front means a swipe-back cancels
        // cleanly instead of leaving a half-configured widget on the home screen.
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }

        viewModel.loadForWidget(appWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID })
    }

    private fun finishConfig() {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
        }
        finish()
    }
}
