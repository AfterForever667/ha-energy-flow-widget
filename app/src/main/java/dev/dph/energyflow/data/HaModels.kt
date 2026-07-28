package dev.dph.energyflow.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Raw shape of one row from Home Assistant's `GET /api/states`. */
@Serializable
data class HaState(
    val entity_id: String,
    val state: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
) {
    val friendlyName: String?
        get() = (attributes["friendly_name"] as? JsonPrimitive)?.content

    val unitOfMeasurement: String?
        get() = (attributes["unit_of_measurement"] as? JsonPrimitive)?.content

    /** Parses [state] as a double, or null if it's a non-numeric HA state like "unavailable". */
    val numericValue: Double?
        get() = state.toDoubleOrNull()
}
