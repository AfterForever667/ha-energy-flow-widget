package dev.dph.energyflow.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

/** Result of asking the repository to refresh one widget instance's data from HA. */
sealed class RefreshOutcome {
    data class Success(val snapshot: EnergySnapshot) : RefreshOutcome()
    data class Failed(val message: String, val cached: EnergySnapshot) : RefreshOutcome()
}

/**
 * Every per-widget setting that isn't an entity mapping: header buttons, title, battery capacity
 * source, and appearance (colors/shape/text/format). Defaults reproduce the original shipped
 * look exactly, so an un-configured widget is unaffected by any of this.
 */
data class WidgetOptions(
    val showOpenHaButton: Boolean = true,
    val showSettingsButton: Boolean = true,
    val showTitle: Boolean = true,
    val showHeader: Boolean = true,
    val titleText: String = "Energy Flow",

    val showSolarCard: Boolean = true,
    val showGridCard: Boolean = true,
    val showBatteryCard: Boolean = true,
    val showHomeCard: Boolean = true,
    val showFlowCard: Boolean = true,

    val batteryCapacitySource: BatteryCapacitySource = BatteryCapacitySource.MANUAL,
    val batteryCapacityManualKwh: Float = 0f,
    val batteryCapacityEntityId: String = "",

    val use24HourFormat: Boolean = true,
    val fontSize: FontSizeOption = FontSizeOption.NORMAL,

    val useCustomWidgetBackground: Boolean = false,
    val widgetBackgroundArgb: Int = DEFAULT_WIDGET_BACKGROUND_ARGB,
    val useCustomCardBackground: Boolean = false,
    val cardBackgroundArgb: Int = DEFAULT_CARD_BACKGROUND_ARGB,
    val useCustomPrimaryTextColor: Boolean = false,
    val primaryTextArgb: Int = DEFAULT_PRIMARY_TEXT_ARGB,
    val cornerRadiusDp: Float = DEFAULT_CORNER_RADIUS_DP,
) {
    companion object {
        const val DEFAULT_WIDGET_BACKGROUND_ARGB = 0xFF1C1C1C.toInt()
        const val DEFAULT_CARD_BACKGROUND_ARGB = 0xFF2A2A2A.toInt()
        const val DEFAULT_PRIMARY_TEXT_ARGB = 0xFFF2F2F2.toInt()
        const val DEFAULT_CORNER_RADIUS_DP = 20f

        /** Appearance-only reset — [showSettingsButton]/entity mapping etc. are untouched by callers. */
        val APPEARANCE_DEFAULTS = WidgetOptions()
    }
}

/**
 * Orchestrates a widget refresh: reads the widget's entity mapping out of its Glance state, hits
 * Home Assistant for those entities, and writes the results back into Glance state so the widget
 * composable can render them.
 */
class EnergyRepository(private val context: Context) {

    /** Each widget instance has its own HA connection — see PLAN.md multi-instance support. */
    private fun connectionPrefsFor(glanceId: GlanceId): ConnectionPrefs {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        return ConnectionPrefs(context, appWidgetId.toString())
    }

    /** Applies the (possibly edited) slot -> entity-id / label mapping to a widget's Glance state. */
    suspend fun saveMapping(
        glanceId: GlanceId,
        mapping: Map<EnergySlot, String>,
        labels: Map<EnergySlot, String>,
        options: WidgetOptions,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefsKeys.isConfigured] = true
            prefs[WidgetPrefsKeys.showOpenHaButton] = options.showOpenHaButton
            prefs[WidgetPrefsKeys.showSettingsButton] = options.showSettingsButton
            prefs[WidgetPrefsKeys.showTitle] = options.showTitle
            prefs[WidgetPrefsKeys.showHeader] = options.showHeader
            prefs[WidgetPrefsKeys.titleText] = options.titleText

            prefs[WidgetPrefsKeys.showSolarCard] = options.showSolarCard
            prefs[WidgetPrefsKeys.showGridCard] = options.showGridCard
            prefs[WidgetPrefsKeys.showBatteryCard] = options.showBatteryCard
            prefs[WidgetPrefsKeys.showHomeCard] = options.showHomeCard
            prefs[WidgetPrefsKeys.showFlowCard] = options.showFlowCard

            prefs[WidgetPrefsKeys.batteryCapacitySource] = options.batteryCapacitySource.name
            prefs[WidgetPrefsKeys.batteryCapacityManualKwh] = options.batteryCapacityManualKwh
            prefs[WidgetPrefsKeys.batteryCapacityEntityId] = options.batteryCapacityEntityId

            prefs[WidgetPrefsKeys.use24HourFormat] = options.use24HourFormat
            prefs[WidgetPrefsKeys.fontSize] = options.fontSize.name

            prefs[WidgetPrefsKeys.useCustomWidgetBackground] = options.useCustomWidgetBackground
            prefs[WidgetPrefsKeys.widgetBackgroundArgb] = options.widgetBackgroundArgb
            prefs[WidgetPrefsKeys.useCustomCardBackground] = options.useCustomCardBackground
            prefs[WidgetPrefsKeys.cardBackgroundArgb] = options.cardBackgroundArgb
            prefs[WidgetPrefsKeys.useCustomPrimaryTextColor] = options.useCustomPrimaryTextColor
            prefs[WidgetPrefsKeys.primaryTextArgb] = options.primaryTextArgb
            prefs[WidgetPrefsKeys.cornerRadiusDp] = options.cornerRadiusDp

            for (slot in EnergySlot.entries) {
                prefs[WidgetPrefsKeys.entityIdKey(slot)] = mapping[slot] ?: slot.defaultEntityId
                prefs[WidgetPrefsKeys.labelKey(slot)] = labels[slot] ?: slot.defaultLabel
            }
        }
    }

    suspend fun loadOptions(glanceId: GlanceId): WidgetOptions {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val defaults = WidgetOptions()
        return WidgetOptions(
            showOpenHaButton = prefs[WidgetPrefsKeys.showOpenHaButton] ?: defaults.showOpenHaButton,
            showSettingsButton = prefs[WidgetPrefsKeys.showSettingsButton] ?: defaults.showSettingsButton,
            showTitle = prefs[WidgetPrefsKeys.showTitle] ?: defaults.showTitle,
            showHeader = prefs[WidgetPrefsKeys.showHeader] ?: defaults.showHeader,
            titleText = prefs[WidgetPrefsKeys.titleText] ?: defaults.titleText,
            showSolarCard = prefs[WidgetPrefsKeys.showSolarCard] ?: defaults.showSolarCard,
            showGridCard = prefs[WidgetPrefsKeys.showGridCard] ?: defaults.showGridCard,
            showBatteryCard = prefs[WidgetPrefsKeys.showBatteryCard] ?: defaults.showBatteryCard,
            showHomeCard = prefs[WidgetPrefsKeys.showHomeCard] ?: defaults.showHomeCard,
            showFlowCard = prefs[WidgetPrefsKeys.showFlowCard] ?: defaults.showFlowCard,
            batteryCapacitySource = prefs[WidgetPrefsKeys.batteryCapacitySource]
                ?.let { runCatching { BatteryCapacitySource.valueOf(it) }.getOrNull() }
                ?: defaults.batteryCapacitySource,
            batteryCapacityManualKwh = prefs[WidgetPrefsKeys.batteryCapacityManualKwh]
                ?: defaults.batteryCapacityManualKwh,
            batteryCapacityEntityId = prefs[WidgetPrefsKeys.batteryCapacityEntityId]
                ?: defaults.batteryCapacityEntityId,
            use24HourFormat = prefs[WidgetPrefsKeys.use24HourFormat] ?: defaults.use24HourFormat,
            fontSize = prefs[WidgetPrefsKeys.fontSize]
                ?.let { runCatching { FontSizeOption.valueOf(it) }.getOrNull() }
                ?: defaults.fontSize,
            useCustomWidgetBackground = prefs[WidgetPrefsKeys.useCustomWidgetBackground]
                ?: defaults.useCustomWidgetBackground,
            widgetBackgroundArgb = prefs[WidgetPrefsKeys.widgetBackgroundArgb] ?: defaults.widgetBackgroundArgb,
            useCustomCardBackground = prefs[WidgetPrefsKeys.useCustomCardBackground]
                ?: defaults.useCustomCardBackground,
            cardBackgroundArgb = prefs[WidgetPrefsKeys.cardBackgroundArgb] ?: defaults.cardBackgroundArgb,
            useCustomPrimaryTextColor = prefs[WidgetPrefsKeys.useCustomPrimaryTextColor]
                ?: defaults.useCustomPrimaryTextColor,
            primaryTextArgb = prefs[WidgetPrefsKeys.primaryTextArgb] ?: defaults.primaryTextArgb,
            cornerRadiusDp = prefs[WidgetPrefsKeys.cornerRadiusDp] ?: defaults.cornerRadiusDp,
        )
    }

    /** Reads the currently-saved entity mapping for a widget (defaults if never configured). */
    suspend fun loadMapping(glanceId: GlanceId): Map<EnergySlot, String> {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        return EnergySlot.entries.associateWith { slot ->
            prefs[WidgetPrefsKeys.entityIdKey(slot)] ?: slot.defaultEntityId
        }
    }

    suspend fun loadLabels(glanceId: GlanceId): Map<EnergySlot, String> {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        return EnergySlot.entries.associateWith { slot ->
            prefs[WidgetPrefsKeys.labelKey(slot)] ?: slot.defaultLabel
        }
    }

    /**
     * Fetches fresh values from HA for [glanceId]'s configured entities and persists them into its
     * Glance state. Callers must still call `EnergyWidget().update(context, glanceId)` afterwards
     * to force a re-render; this function only updates the underlying state.
     */
    suspend fun refresh(glanceId: GlanceId): RefreshOutcome {
        val connectionPrefs = connectionPrefsFor(glanceId)
        if (!connectionPrefs.isConfigured) {
            val cached = EnergySnapshot.fromPreferences(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId))
            return RefreshOutcome.Failed("Not set up yet — tap to configure.", cached)
        }

        val mapping = loadMapping(glanceId)
        val labels = loadLabels(glanceId)
        val options = loadOptions(glanceId)
        val client = HaClient(connectionPrefs.baseUrl, connectionPrefs.token)

        val capacityEntityId = options.batteryCapacityEntityId
            .takeIf { options.batteryCapacitySource == BatteryCapacitySource.ENTITY && it.isNotBlank() }
        val fetchIds = if (capacityEntityId != null) mapping.values + capacityEntityId else mapping.values

        return when (val result = client.fetchStates(fetchIds)) {
            is HaResult.Success -> {
                val statesByEntity = result.value
                val now = System.currentTimeMillis()
                val values = EnergySlot.entries.associateWith { slot ->
                    val entityId = mapping[slot] ?: slot.defaultEntityId
                    val state = statesByEntity[entityId]
                    SlotValue(
                        slot = slot,
                        label = labels[slot] ?: slot.defaultLabel,
                        rawState = state?.state,
                        numericValue = state?.numericValue,
                        unit = state?.unitOfMeasurement,
                    )
                }
                val capacityEntityValue = capacityEntityId?.let { statesByEntity[it]?.state }
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs.remove(WidgetPrefsKeys.lastErrorMessage)
                    prefs[WidgetPrefsKeys.lastUpdatedMillis] = now
                    if (capacityEntityValue != null) {
                        prefs[WidgetPrefsKeys.batteryCapacityEntityValue] = capacityEntityValue
                    }
                    for (slot in EnergySlot.entries) {
                        val v = values.getValue(slot)
                        prefs[WidgetPrefsKeys.valueKey(slot)] = "${v.rawState.orEmpty()}|${v.unit.orEmpty()}"
                    }
                }
                RefreshOutcome.Success(EnergySnapshot(values, now))
            }
            is HaResult.Failure -> {
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[WidgetPrefsKeys.lastErrorMessage] = result.message
                }
                val cached = EnergySnapshot.fromPreferences(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId))
                RefreshOutcome.Failed(result.message, cached)
            }
        }
    }
}
