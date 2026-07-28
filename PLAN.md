# Android Home-Screen Widget: Home Assistant Energy Flow

## Goal
An Android home-screen widget showing a live energy-flow overview (solar, battery,
home load, grid) sourced from a self-hosted Home Assistant instance, refreshed
periodically and on demand.

## Background / constraints
- Developer is new to Android dev, has some general programming experience.
- Mac-based development, Android Studio not yet installed.
- HA instance is reachable via a public domain (Cloudflare DNS + Nginx Proxy
  Manager reverse proxy) over HTTPS — treat it as a normal HTTPS API, no VPN
  or local-network-only assumptions.
- Reference UI is an existing HA dashboard card: `custom:sunsynk-power-flow-card`.
  Widget should approximate its information, not its live animation.

## V1 scope (deliberately limited)
Included: solar, battery detail, home load (aggregate), grid.
Excluded (later phase): individual appliance sub-circuits (Wallbox, Wasmachine,
Droogkast, Vaatwas), non-essential load split, colored-threshold styling
(v1 uses neutral/static colors — thresholds are a v2 feature).

## Entity mapping (from the user's actual HA instance)
Widget setup screen should let the user remap each of these to their own
entity via a picker (populated from `/api/states`), pre-filled with these
defaults where they match:

| Logical slot | Example entity_id |
|---|---|
| Solar power (string 1) | `sensor.inverter_pv_1_power` |
| Solar power (string 2) | `sensor.inverter_pv_2_power` |
| Solar power (total) | `sensor.inverter_input_power` |
| Solar daily yield | `sensor.inverter_daily_yield` |
| Solar remaining today | `sensor.energy_production_today_remaining` |
| Inverter AC power / current | `sensor.inverter_active_power` / `sensor.inverter_phase_a_current` |
| Inverter status | `sensor.inverter_inverter_state` |
| Battery power | `sensor.battery_charge_discharge_power` |
| Battery voltage / current | `sensor.battery_bus_voltage` / `sensor.battery_bus_current` |
| Battery SOC | `sensor.battery_state_of_capacity` |
| Battery daily charge / discharge | `sensor.battery_charge_daily` / `sensor.battery_discharge_daily` |
| Battery status | `sensor.battery_status` |
| Home load (daily) | `sensor.energy_consumption_by_home_today` |
| Home essential power | `sensor.power_going_to_home` |
| Grid power | `sensor.power_coming_from_grid` |
| Grid daily buy / sell | `sensor.dsmr_energy_from_grid_daily` / `sensor.dsmr_energy_to_grid_daily` |
| Grid voltage / frequency | `sensor.power_meter_voltage` / `sensor.power_meter_frequency` |

## Tech stack
- Kotlin
- Jetpack Glance (modern widget framework, not legacy RemoteViews directly)
- WorkManager for background refresh (15-min floor, see refresh strategy)
- HA REST API (`/api/states`, `/api/states/<entity_id>`) over HTTPS, auth via
  long-lived access token
- `EncryptedSharedPreferences` for storing the HA URL + token

## Config screen requirements
- Fields: HA base URL (user's own domain), long-lived access token.
- Entity picker per logical slot (fetched live from `/api/states`), with the
  defaults above pre-filled as a starting point, editable.
- Per-slot friendly name override (dashboard card currently sets these via
  HACS card config, not in HA itself, so the widget needs its own labels).

## Refresh strategy (decided)
- Immediate refresh the moment the widget is added to the home screen.
- Background refresh via WorkManager every 15 minutes (the real platform
  floor for `PeriodicWorkRequest`; native `updatePeriodMillis` only supports
  a 30-min floor, so WorkManager is required to hit 15 min).
- Manual "refresh now" icon/button on the widget, always available.
- True "widget became visible" detection does **not** exist as a public
  Android API for home-screen widgets — not attempting it in v1. (Optional
  future addition: trigger a refresh on screen-unlock `ACTION_USER_PRESENT`
  as an approximate proxy, at some battery cost — deferred, not yet decided.)

## Sizing
- Design for a 4x4 (or 4x2) cell widget as the primary target given the
  amount of information; resizable support is a nice-to-have, not required
  for v1.

## Styling (v1)
- Neutral, static colors for borders/icons (no dynamic thresholds yet).
- Skip animated "flowing dot" effects — not meaningful at a 15-min refresh
  cadence. Consider a static directional arrow/indicator instead if desired
  later.

## Next steps (for Claude Code session)
1. Confirm Android Studio installation / SDK setup on the user's Mac.
2. Scaffold a new Android Studio project with Jetpack Glance + WorkManager
   dependencies.
3. Build the config Activity (HA URL/token entry + entity pickers).
4. Build the Glance widget composable matching the v1 layout above.
5. Wire up WorkManager periodic refresh + manual refresh action.
6. Test on physical device via USB debugging.
