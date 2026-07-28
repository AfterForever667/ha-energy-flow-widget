package dev.dph.energyflow.data

import androidx.datastore.preferences.core.Preferences
import java.text.NumberFormat
import java.util.Locale

/** One slot's value at the moment of the last successful refresh. */
data class SlotValue(
    val slot: EnergySlot,
    val label: String,
    val rawState: String?,
    val numericValue: Double?,
    val unit: String?,
) {
    /** Human-friendly rendering for the widget, e.g. "1680 W", "36 %", "Running". */
    val display: String
        get() {
            if (numericValue == null) return rawState?.takeIf { it.isNotBlank() } ?: "—"
            return when (slot.kind) {
                SlotKind.POWER_W -> "${wholeNumber(numericValue)} W"
                SlotKind.ENERGY_KWH -> "${oneDecimal(numericValue)} kWh"
                SlotKind.PERCENT -> "${wholeNumber(numericValue)}%"
                SlotKind.VOLTAGE_V -> "${oneDecimal(numericValue)} V"
                SlotKind.CURRENT_A -> "${oneDecimal(numericValue)} A"
                SlotKind.FREQUENCY_HZ -> "${twoDecimals(numericValue)} Hz"
                SlotKind.TEXT -> rawState ?: "—"
            }
        }

    private fun wholeNumber(v: Double): String = wholeFormatter.format(v)
    private fun oneDecimal(v: Double): String = oneDecimalFormatter.format(v)
    private fun twoDecimals(v: Double): String = twoDecimalFormatter.format(v)

    companion object {
        private val wholeFormatter = NumberFormat.getIntegerInstance(Locale.US)
        private val oneDecimalFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 1; maximumFractionDigits = 1
        }
        private val twoDecimalFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
    }
}

/** Every slot's value as of the last successful (or last known) refresh. */
data class EnergySnapshot(
    val values: Map<EnergySlot, SlotValue>,
    val lastUpdatedMillis: Long?,
    val errorMessage: String? = null,
) {
    operator fun get(slot: EnergySlot): SlotValue? = values[slot]

    fun numeric(slot: EnergySlot): Double = values[slot]?.numericValue ?: 0.0

    companion object {
        val EMPTY = EnergySnapshot(emptyMap(), null)

        /** Rebuilds a snapshot purely from previously-persisted Glance preferences (no network). */
        fun fromPreferences(prefs: Preferences): EnergySnapshot {
            val values = EnergySlot.entries.associateWith { slot ->
                val raw = prefs[WidgetPrefsKeys.valueKey(slot)]
                val label = prefs[WidgetPrefsKeys.labelKey(slot)] ?: slot.defaultLabel
                if (raw == null) {
                    SlotValue(slot, label, null, null, null)
                } else {
                    // Persisted as "value|unit" — see EnergyRepository.persist().
                    val parts = raw.split('|', limit = 2)
                    val numeric = parts.getOrNull(0)?.toDoubleOrNull()
                    val stateStr = parts.getOrNull(0)
                    val unit = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    SlotValue(slot, label, stateStr, numeric, unit)
                }
            }
            return EnergySnapshot(
                values = values,
                lastUpdatedMillis = prefs[WidgetPrefsKeys.lastUpdatedMillis],
                errorMessage = prefs[WidgetPrefsKeys.lastErrorMessage],
            )
        }
    }
}
