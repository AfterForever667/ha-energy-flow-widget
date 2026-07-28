package dev.dph.energyflow.data

/** How a slot's numeric value should be interpreted/formatted. */
enum class SlotKind {
    POWER_W,
    ENERGY_KWH,
    PERCENT,
    VOLTAGE_V,
    CURRENT_A,
    FREQUENCY_HZ,
    TEXT,
}

/** Which part of the widget layout a slot feeds. Purely for grouping in the config UI. */
enum class SlotCategory {
    SOLAR,
    BATTERY,
    HOME,
    GRID,
    FLOW,
}

/**
 * One logical piece of data the widget can show. [key] is the stable identifier persisted in
 * storage (never rename once shipped); [defaultEntityId] is a placeholder starting point (a
 * typical Sunsynk/DSMR setup), remappable via the config screen; [defaultLabel] is the
 * friendly-name override shown in the widget, also user-editable. [description] explains what the
 * sensor is expected to represent, shown via the info icon next to each field in the config UI.
 *
 * Only slots actually rendered somewhere on the widget belong here — see EnergyWidgetContent.
 */
enum class EnergySlot(
    val key: String,
    val category: SlotCategory,
    val kind: SlotKind,
    val defaultEntityId: String,
    val defaultLabel: String,
    val description: String,
    val required: Boolean = false,
) {
    // --- Solar ---
    SOLAR_TOTAL(
        "solar_total", SlotCategory.SOLAR, SlotKind.POWER_W,
        "sensor.inverter_input_power", "Solar",
        "Total instantaneous power currently being produced by all solar panels/strings combined, in Watts. Usually your inverter's total PV/DC input power sensor.",
        required = true,
    ),
    SOLAR_DAILY_YIELD(
        "solar_daily_yield", SlotCategory.SOLAR, SlotKind.ENERGY_KWH,
        "sensor.inverter_daily_yield", "Solar Today",
        "Total solar energy produced so far today, in kWh. Resets to zero at midnight.",
    ),
    SOLAR_REMAINING_TODAY(
        "solar_remaining_today", SlotCategory.SOLAR, SlotKind.ENERGY_KWH,
        "sensor.energy_production_today_remaining", "Solar Forecast Left",
        "Estimated solar energy still expected for the rest of today, in kWh. Typically comes from a solar forecast integration (e.g. Forecast.Solar, Solcast).",
    ),

    // --- Battery ---
    BATTERY_POWER(
        "battery_power", SlotCategory.BATTERY, SlotKind.POWER_W,
        "sensor.battery_charge_discharge_power", "Battery Power",
        "Instantaneous battery charge/discharge power, in Watts. Must be positive while charging and negative while discharging — the widget uses the sign to tell the two apart.",
        required = true,
    ),
    BATTERY_SOC(
        "battery_soc", SlotCategory.BATTERY, SlotKind.PERCENT,
        "sensor.battery_state_of_capacity", "Battery",
        "Battery state of charge as a percentage, 0-100%.",
        required = true,
    ),

    // --- Home ---
    HOME_LOAD_DAILY(
        "home_load_daily", SlotCategory.HOME, SlotKind.ENERGY_KWH,
        "sensor.energy_consumption_by_home_today", "Home Today",
        "Total energy consumed by the whole home so far today, in kWh.",
    ),
    HOME_ESSENTIAL_POWER(
        "home_essential_power", SlotCategory.HOME, SlotKind.POWER_W,
        "sensor.power_going_to_home", "Home Load",
        "Instantaneous power currently being consumed by the home, in Watts.",
        required = true,
    ),

    // --- Grid ---
    GRID_IMPORT_POWER(
        "grid_import_power", SlotCategory.GRID, SlotKind.POWER_W,
        "sensor.power_coming_from_grid", "Grid Import",
        "Instantaneous power currently being imported (bought) from the grid, in Watts. Should read 0 when not importing.",
        required = true,
    ),
    GRID_EXPORT_POWER(
        "grid_export_power", SlotCategory.GRID, SlotKind.POWER_W,
        "sensor.power_going_to_grid", "Grid Export",
        "Instantaneous power currently being exported (sold) to the grid, in Watts. Should read 0 when not exporting.",
        required = true,
    ),
    GRID_BUY_DAILY(
        "grid_buy_daily", SlotCategory.GRID, SlotKind.ENERGY_KWH,
        "sensor.dsmr_energy_from_grid_daily", "Bought Today",
        "Total energy bought from the grid so far today, in kWh.",
    ),
    GRID_SELL_DAILY(
        "grid_sell_daily", SlotCategory.GRID, SlotKind.ENERGY_KWH,
        "sensor.dsmr_energy_to_grid_daily", "Sold Today",
        "Total energy sold to the grid so far today, in kWh.",
    ),

    // --- Directional flow (all non-negative "X to Y" sensors, used to draw arrows) ---
    FLOW_PV_TO_HOME(
        "flow_pv_to_home", SlotCategory.FLOW, SlotKind.POWER_W,
        "sensor.power_coming_from_pv_to_home", "Solar → Home",
        "Instantaneous power flowing directly from solar panels to home consumption, in Watts. Non-negative.",
        required = true,
    ),
    FLOW_PV_TO_BATTERY(
        "flow_pv_to_battery", SlotCategory.FLOW, SlotKind.POWER_W,
        "sensor.power_coming_from_pv_to_battery", "Solar → Battery",
        "Instantaneous power flowing from solar panels into the battery (charging), in Watts. Non-negative.",
        required = true,
    ),
    FLOW_PV_TO_GRID(
        "flow_pv_to_grid", SlotCategory.FLOW, SlotKind.POWER_W,
        "sensor.power_going_to_grid", "Solar → Grid",
        "Instantaneous power flowing from solar panels out to the grid (export), in Watts. Non-negative.",
        required = true,
    ),
    FLOW_GRID_TO_HOME(
        "flow_grid_to_home", SlotCategory.FLOW, SlotKind.POWER_W,
        "sensor.power_coming_from_grid_to_home", "Grid → Home",
        "Instantaneous power flowing from the grid to home consumption, in Watts. Non-negative.",
        required = true,
    ),
    FLOW_GRID_TO_BATTERY(
        "flow_grid_to_battery", SlotCategory.FLOW, SlotKind.POWER_W,
        "sensor.power_coming_from_grid_to_battery", "Grid → Battery",
        "Instantaneous power flowing from the grid into the battery (charging from grid), in Watts. Non-negative.",
        required = true,
    ),
    FLOW_BATTERY_TO_HOME(
        "flow_battery_to_home", SlotCategory.FLOW, SlotKind.POWER_W,
        "sensor.power_coming_from_battery", "Battery → Home",
        "Instantaneous power flowing from the battery to home consumption (discharging), in Watts. Non-negative.",
        required = true,
    ),
    ;

    companion object {
        fun fromKey(key: String): EnergySlot? = entries.firstOrNull { it.key == key }

        /** HA `unit_of_measurement` values that make sense for this slot's [SlotKind]. */
        fun expectedUnits(kind: SlotKind): Set<String> = when (kind) {
            SlotKind.POWER_W -> setOf("W")
            SlotKind.ENERGY_KWH -> setOf("kWh")
            SlotKind.PERCENT -> setOf("%")
            SlotKind.VOLTAGE_V -> setOf("V")
            SlotKind.CURRENT_A -> setOf("A")
            SlotKind.FREQUENCY_HZ -> setOf("Hz")
            SlotKind.TEXT -> emptySet()
        }

        const val BATTERY_CAPACITY_DESCRIPTION =
            "A sensor reporting your battery bank's total usable capacity in kWh — a fixed, " +
                "rarely-changing value, not a live percentage. Only needed for the full-charge / " +
                "runtime estimate on the battery card."
    }
}
