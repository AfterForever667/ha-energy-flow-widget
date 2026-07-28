package dev.dph.energyflow.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Preference keys used inside a widget instance's Glance state (via
 * [androidx.glance.appwidget.state.updateAppWidgetState], keyed automatically per widget by
 * `PreferencesGlanceStateDefinition`). Holds the per-widget entity mapping / label overrides plus
 * the last-fetched snapshot, so the widget has something to render immediately on process start
 * and while a refresh is in flight.
 */
object WidgetPrefsKeys {
    fun entityIdKey(slot: EnergySlot): Preferences.Key<String> =
        stringPreferencesKey("entity_${slot.key}")

    fun labelKey(slot: EnergySlot): Preferences.Key<String> =
        stringPreferencesKey("label_${slot.key}")

    fun valueKey(slot: EnergySlot): Preferences.Key<String> =
        stringPreferencesKey("value_${slot.key}")

    val lastUpdatedMillis: Preferences.Key<Long> = longPreferencesKey("last_updated_millis")
    val lastErrorMessage: Preferences.Key<String> = stringPreferencesKey("last_error_message")
    val isConfigured: Preferences.Key<Boolean> = booleanPreferencesKey("is_configured")

    // --- Header / title ---
    val showOpenHaButton: Preferences.Key<Boolean> = booleanPreferencesKey("show_open_ha_button")
    val showSettingsButton: Preferences.Key<Boolean> = booleanPreferencesKey("show_settings_button")
    val titleText: Preferences.Key<String> = stringPreferencesKey("title_text")
    val showTitle: Preferences.Key<Boolean> = booleanPreferencesKey("show_title")
    val showHeader: Preferences.Key<Boolean> = booleanPreferencesKey("show_header")

    // --- Card visibility (so the widget works for setups without solar/battery, etc.) ---
    val showSolarCard: Preferences.Key<Boolean> = booleanPreferencesKey("show_solar_card")
    val showGridCard: Preferences.Key<Boolean> = booleanPreferencesKey("show_grid_card")
    val showBatteryCard: Preferences.Key<Boolean> = booleanPreferencesKey("show_battery_card")
    val showHomeCard: Preferences.Key<Boolean> = booleanPreferencesKey("show_home_card")
    val showFlowCard: Preferences.Key<Boolean> = booleanPreferencesKey("show_flow_card")

    // --- Battery capacity (manual value or a HA entity) ---
    val batteryCapacitySource: Preferences.Key<String> = stringPreferencesKey("battery_capacity_source")
    val batteryCapacityManualKwh: Preferences.Key<Float> = floatPreferencesKey("battery_capacity_manual_kwh")
    val batteryCapacityEntityId: Preferences.Key<String> = stringPreferencesKey("battery_capacity_entity_id")
    val batteryCapacityEntityValue: Preferences.Key<String> = stringPreferencesKey("battery_capacity_entity_value")

    // --- Formatting ---
    val use24HourFormat: Preferences.Key<Boolean> = booleanPreferencesKey("use_24_hour_format")
    val fontSize: Preferences.Key<String> = stringPreferencesKey("font_size")

    // --- Colors & shape ---
    val useCustomWidgetBackground: Preferences.Key<Boolean> = booleanPreferencesKey("use_custom_widget_bg")
    val widgetBackgroundArgb: Preferences.Key<Int> = intPreferencesKey("widget_background_argb")
    val useCustomCardBackground: Preferences.Key<Boolean> = booleanPreferencesKey("use_custom_card_bg")
    val cardBackgroundArgb: Preferences.Key<Int> = intPreferencesKey("card_background_argb")
    val useCustomPrimaryTextColor: Preferences.Key<Boolean> = booleanPreferencesKey("use_custom_primary_text_color")
    val primaryTextArgb: Preferences.Key<Int> = intPreferencesKey("primary_text_argb")
    val cornerRadiusDp: Preferences.Key<Float> = floatPreferencesKey("corner_radius_dp")
}
