package dev.dph.energyflow.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.dph.energyflow.R
import dev.dph.energyflow.config.ConfigActivity
import dev.dph.energyflow.data.BatteryCapacitySource
import dev.dph.energyflow.data.EnergySlot
import dev.dph.energyflow.data.EnergySnapshot
import dev.dph.energyflow.data.FontSizeOption
import dev.dph.energyflow.data.SlotValue
import dev.dph.energyflow.data.WidgetOptions
import dev.dph.energyflow.data.WidgetPrefsKeys
import dev.dph.energyflow.data.scale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Resolved appearance for one widget instance — computed once per render from its Glance state. */
private data class WidgetStyle(
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val errorText: ColorProvider,
    val arrowTint: ColorProvider,
    val widgetBackground: ColorProvider,
    val cardBackground: ColorProvider,
    val rootCornerRadius: Dp,
    val cardCornerRadius: Dp,
    val fontScale: Float,
    val use24HourFormat: Boolean,
) {
    fun sp(base: Int): TextUnit = (base * fontScale).sp
}

/** Relative luminance (0-255) of an RGB color, used to pick a readable neutral text tone. */
private fun luminanceOf(argb: Int): Double {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return 0.299 * r + 0.587 * g + 0.114 * b
}

private fun resolveStyle(prefs: Preferences): WidgetStyle {
    val fontSize = prefs[WidgetPrefsKeys.fontSize]
        ?.let { runCatching { FontSizeOption.valueOf(it) }.getOrNull() }
        ?: FontSizeOption.NORMAL
    val cornerRadiusDp = prefs[WidgetPrefsKeys.cornerRadiusDp] ?: WidgetOptions.DEFAULT_CORNER_RADIUS_DP

    val primaryText = if (prefs[WidgetPrefsKeys.useCustomPrimaryTextColor] == true) {
        ColorProvider(Color(prefs[WidgetPrefsKeys.primaryTextArgb] ?: WidgetOptions.DEFAULT_PRIMARY_TEXT_ARGB))
    } else {
        ColorProvider(R.color.widget_text_primary)
    }
    val useCustomCardBackground = prefs[WidgetPrefsKeys.useCustomCardBackground] == true
    val cardBackgroundArgb = prefs[WidgetPrefsKeys.cardBackgroundArgb] ?: WidgetOptions.DEFAULT_CARD_BACKGROUND_ARGB
    val widgetBackground = if (prefs[WidgetPrefsKeys.useCustomWidgetBackground] == true) {
        ColorProvider(Color(prefs[WidgetPrefsKeys.widgetBackgroundArgb] ?: WidgetOptions.DEFAULT_WIDGET_BACKGROUND_ARGB))
    } else {
        ColorProvider(R.color.widget_background)
    }
    val cardBackground = if (useCustomCardBackground) {
        ColorProvider(Color(cardBackgroundArgb))
    } else {
        ColorProvider(R.color.widget_card)
    }

    // Non-configurable text (card titles, daily detail lines, flow values when inactive) must
    // stay readable no matter what card color the user picks, so it's derived from the actual
    // background luminance rather than a fixed day/night resource once that background is custom.
    val isLightCard = useCustomCardBackground && luminanceOf(cardBackgroundArgb) > 140.0
    val secondaryText = if (!useCustomCardBackground) {
        ColorProvider(R.color.widget_text_secondary)
    } else if (isLightCard) {
        ColorProvider(Color(0xFF4A4A4AL.toInt()))
    } else {
        ColorProvider(Color(0xFFC7C7C7L.toInt()))
    }
    val errorText = if (!useCustomCardBackground) {
        ColorProvider(R.color.widget_error)
    } else if (isLightCard) {
        ColorProvider(Color(0xFFB00020L.toInt()))
    } else {
        ColorProvider(Color(0xFFCF6679L.toInt()))
    }
    // The flow-arrow line/divider needs more contrast than plain detail text to read as a
    // distinct graphical element rather than disappearing into the card background.
    val arrowTint = if (!useCustomCardBackground) {
        ColorProvider(R.color.widget_divider)
    } else if (isLightCard) {
        ColorProvider(Color(0xFF404040L.toInt()))
    } else {
        ColorProvider(Color(0xFFD8D8D8L.toInt()))
    }

    return WidgetStyle(
        primaryText = primaryText,
        secondaryText = secondaryText,
        errorText = errorText,
        arrowTint = arrowTint,
        widgetBackground = widgetBackground,
        cardBackground = cardBackground,
        rootCornerRadius = cornerRadiusDp.dp,
        cardCornerRadius = (cornerRadiusDp * 0.7f).dp,
        fontScale = fontSize.scale,
        use24HourFormat = prefs[WidgetPrefsKeys.use24HourFormat] ?: true,
    )
}

/** One stat card's content, independent of where/how it ends up laid out. */
private data class CardSpec(
    val icon: String,
    val title: String,
    val primary: String,
    val secondary: String?,
    val tertiary: String? = null,
)

@Composable
fun EnergyWidgetContent() {
    val prefs = currentState<Preferences>()
    val snapshot = EnergySnapshot.fromPreferences(prefs)
    val configured = prefs[WidgetPrefsKeys.isConfigured] == true
    val style = resolveStyle(prefs)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(style.widgetBackground)
            .cornerRadius(style.rootCornerRadius)
            .padding(10.dp),
    ) {
        if (!configured) {
            NotConfiguredContent(style)
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                if (prefs[WidgetPrefsKeys.showHeader] ?: true) {
                    HeaderRow(
                        snapshot = snapshot,
                        style = style,
                        showTitle = prefs[WidgetPrefsKeys.showTitle] ?: true,
                        titleText = prefs[WidgetPrefsKeys.titleText] ?: "Energy Flow",
                        showOpenHaButton = prefs[WidgetPrefsKeys.showOpenHaButton] ?: true,
                        showSettingsButton = prefs[WidgetPrefsKeys.showSettingsButton] ?: true,
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }

                val cards = buildList {
                    if (prefs[WidgetPrefsKeys.showSolarCard] ?: true) {
                        add(
                            CardSpec(
                                icon = "☀️",
                                title = "Solar",
                                primary = snapshot[EnergySlot.SOLAR_TOTAL]?.display ?: "—",
                                secondary = solarSecondary(snapshot),
                                tertiary = solarTertiary(snapshot),
                            ),
                        )
                    }
                    if (prefs[WidgetPrefsKeys.showGridCard] ?: true) {
                        add(
                            CardSpec(
                                icon = "⚡",
                                title = "Grid",
                                primary = gridPrimary(snapshot),
                                secondary = gridSecondary(snapshot),
                                tertiary = gridTertiary(snapshot),
                            ),
                        )
                    }
                    if (prefs[WidgetPrefsKeys.showBatteryCard] ?: true) {
                        add(
                            CardSpec(
                                icon = "🔋",
                                title = "Battery",
                                primary = snapshot[EnergySlot.BATTERY_SOC]?.display ?: "—",
                                secondary = batterySecondary(snapshot),
                                tertiary = batteryTimeEstimate(snapshot, resolveBatteryCapacityKwh(prefs), style.use24HourFormat),
                            ),
                        )
                    }
                    if (prefs[WidgetPrefsKeys.showHomeCard] ?: true) {
                        add(
                            CardSpec(
                                icon = "🏠",
                                title = "Home",
                                primary = snapshot[EnergySlot.HOME_ESSENTIAL_POWER]?.display ?: "—",
                                secondary = snapshot[EnergySlot.HOME_LOAD_DAILY]?.let { "${it.display} used today" },
                            ),
                        )
                    }
                }
                val showFlowCard = prefs[WidgetPrefsKeys.showFlowCard] ?: true

                if (cards.isNotEmpty()) {
                    StatCardGrid(cards, style, modifier = GlanceModifier.defaultWeight())
                    if (showFlowCard) Spacer(modifier = GlanceModifier.height(6.dp))
                }
                if (showFlowCard) {
                    FlowStrip(snapshot, style)
                }
            }
        }
    }
}

/**
 * Lays out however many stat cards are currently visible so there's never a blank slot: 4 cards
 * keep the original 2x2 grid, but 1/2/3 cards use that many columns in a single row instead.
 */
@Composable
private fun StatCardGrid(cards: List<CardSpec>, style: WidgetStyle, modifier: GlanceModifier) {
    // Each row needs a bounded height for fillMaxHeight() further down to resolve against — an
    // un-weighted Row inside this fillMaxSize() column has no defined height of its own, so a
    // fillMaxHeight() child inside it would happily consume ALL remaining vertical space,
    // starving a second stacked row (and the flow card below it) of any room to render.
    // defaultWeight() (applied by the caller, and here between the two stacked rows) gives each
    // row an explicit, bounded share of the available height.
    when (cards.size) {
        0 -> Unit
        4 -> {
            val row1 = cards.subList(0, 2)
            val row2 = cards.subList(2, 4)
            Column(modifier = modifier.fillMaxWidth()) {
                CardRow(row1, style, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
                Spacer(modifier = GlanceModifier.height(6.dp))
                CardRow(row2, style, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
            }
        }
        else -> CardRow(cards, style, modifier = modifier.fillMaxWidth())
    }
}

@Composable
private fun CardRow(cards: List<CardSpec>, style: WidgetStyle, modifier: GlanceModifier) {
    Row(modifier = modifier) {
        for ((index, card) in cards.withIndex()) {
            if (index > 0) Spacer(modifier = GlanceModifier.width(8.dp))
            StatCard(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                style = style,
                icon = card.icon,
                title = card.title,
                primary = card.primary,
                secondary = card.secondary,
                tertiary = card.tertiary,
            )
        }
    }
}

@Composable
private fun NotConfiguredContent(style: WidgetStyle) {
    Column(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<ConfigActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "⚡ Energy Flow",
            style = TextStyle(color = style.primaryText, fontSize = style.sp(16), fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            "Tap to set up your Home Assistant connection",
            style = TextStyle(color = style.secondaryText, fontSize = style.sp(12), textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun HeaderRow(
    snapshot: EnergySnapshot,
    style: WidgetStyle,
    showTitle: Boolean,
    titleText: String,
    showOpenHaButton: Boolean,
    showSettingsButton: Boolean,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTitle) {
            Text(
                titleText,
                style = TextStyle(color = style.primaryText, fontSize = style.sp(14), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
        } else {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
        if (snapshot.errorMessage != null) {
            Text("⚠", style = TextStyle(color = style.errorText, fontSize = style.sp(13)))
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        if (showSettingsButton) {
            HeaderIconButton(icon = "⚙", style = style, callback = actionRunCallback<OpenWidgetSettingsAction>())
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        if (showOpenHaButton) {
            HeaderIconButton(icon = "🏠", style = style, callback = actionRunCallback<OpenHomeAssistantAction>())
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        HeaderIconButton(icon = "↻", style = style, callback = actionRunCallback<RefreshAction>())
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            timeLabel(snapshot.lastUpdatedMillis, style.use24HourFormat),
            style = TextStyle(color = style.secondaryText, fontSize = style.sp(10), fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun HeaderIconButton(icon: String, style: WidgetStyle, callback: Action) {
    Box(
        modifier = GlanceModifier
            .size(22.dp)
            .background(style.cardBackground)
            .cornerRadius(11.dp)
            .clickable(callback),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, style = TextStyle(color = style.primaryText, fontSize = style.sp(12)))
    }
}

@Composable
private fun StatCard(
    modifier: GlanceModifier,
    style: WidgetStyle,
    icon: String,
    title: String,
    primary: String,
    secondary: String?,
    tertiary: String? = null,
) {
    Column(
        modifier = modifier
            .background(style.cardBackground)
            .cornerRadius(style.cardCornerRadius)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, style = TextStyle(fontSize = style.sp(12)))
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(title, style = TextStyle(color = style.secondaryText, fontSize = style.sp(11)))
        }
        Text(
            primary,
            style = TextStyle(color = style.primaryText, fontSize = style.sp(16), fontWeight = FontWeight.Bold),
        )
        if (secondary != null) {
            Text(secondary, style = TextStyle(color = style.secondaryText, fontSize = style.sp(10)))
        }
        if (tertiary != null) {
            Text(tertiary, style = TextStyle(color = style.secondaryText, fontSize = style.sp(10)))
        }
    }
}

@Composable
private fun FlowStrip(snapshot: EnergySnapshot, style: WidgetStyle) {
    val flows = listOf(
        Triple("☀️", "🏠", snapshot[EnergySlot.FLOW_PV_TO_HOME]),
        Triple("☀️", "🔋", snapshot[EnergySlot.FLOW_PV_TO_BATTERY]),
        Triple("☀️", "⚡", snapshot[EnergySlot.FLOW_PV_TO_GRID]),
        Triple("⚡", "🏠", snapshot[EnergySlot.FLOW_GRID_TO_HOME]),
        Triple("⚡", "🔋", snapshot[EnergySlot.FLOW_GRID_TO_BATTERY]),
        Triple("🔋", "🏠", snapshot[EnergySlot.FLOW_BATTERY_TO_HOME]),
    )
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(style.cardBackground)
            .cornerRadius(style.cardCornerRadius)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        for (rowPair in flows.chunked(2)) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FlowEntry(
                    modifier = GlanceModifier.defaultWeight(),
                    style = style,
                    sourceIcon = rowPair[0].first,
                    targetIcon = rowPair[0].second,
                    value = rowPair[0].third,
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(style.arrowTint),
                ) {}
                Spacer(modifier = GlanceModifier.width(6.dp))
                if (rowPair.size > 1) {
                    FlowEntry(
                        modifier = GlanceModifier.defaultWeight(),
                        style = style,
                        sourceIcon = rowPair[1].first,
                        targetIcon = rowPair[1].second,
                        value = rowPair[1].third,
                    )
                }
            }
        }
    }
}

/**
 * One "source icon —(arrow)→ target icon" entry: a plain line segment, the value, then a line
 * segment ending in an arrowhead — laid out as plain Row siblings (not layered) so the value can
 * never visually overlap the line, regardless of how the arrow's tint gets composited.
 */
@Composable
private fun FlowEntry(modifier: GlanceModifier, style: WidgetStyle, sourceIcon: String, targetIcon: String, value: SlotValue?) {
    val isActive = (value?.numericValue ?: 0.0).let { abs(it) >= 1.0 }
    Row(
        modifier = modifier.padding(vertical = 3.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sourceIcon,
            style = TextStyle(fontSize = style.sp(11)),
        )
        Image(
            provider = ImageProvider(R.drawable.ic_flow_line),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(style.arrowTint),
            modifier = GlanceModifier.defaultWeight().height(8.dp).padding(horizontal = 2.dp),
        )
        Text(
            value?.display ?: "—",
            style = TextStyle(
                color = if (isActive) style.primaryText else style.secondaryText,
                fontSize = style.sp(10),
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            ),
        )
        Image(
            provider = ImageProvider(R.drawable.ic_flow_arrow),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(style.arrowTint),
            modifier = GlanceModifier.defaultWeight().height(8.dp).padding(horizontal = 2.dp),
        )
        Text(
            targetIcon,
            style = TextStyle(fontSize = style.sp(11)),
        )
    }
}

// Each of these is deliberately its own short line — the card height reserved for a "secondary"
// or "tertiary" line assumes exactly one line, and a combined "X today / Y left" string is long
// enough to wrap onto two lines at some widget widths/font sizes, which used to get clipped.
private fun solarSecondary(snapshot: EnergySnapshot): String? =
    snapshot[EnergySlot.SOLAR_DAILY_YIELD]?.display?.let { "$it today" }

private fun solarTertiary(snapshot: EnergySnapshot): String? =
    snapshot[EnergySlot.SOLAR_REMAINING_TODAY]?.display?.let { "$it left (est.)" }

private fun gridPrimary(snapshot: EnergySnapshot): String {
    val import = snapshot.numeric(EnergySlot.GRID_IMPORT_POWER)
    val export = snapshot.numeric(EnergySlot.GRID_EXPORT_POWER)
    return if (export > import) {
        "↑ ${snapshot[EnergySlot.GRID_EXPORT_POWER]?.display ?: "—"}"
    } else {
        "↓ ${snapshot[EnergySlot.GRID_IMPORT_POWER]?.display ?: "—"}"
    }
}

private fun gridSecondary(snapshot: EnergySnapshot): String? =
    snapshot[EnergySlot.GRID_BUY_DAILY]?.display?.let { "$it in ↓" }

private fun gridTertiary(snapshot: EnergySnapshot): String? =
    snapshot[EnergySlot.GRID_SELL_DAILY]?.display?.let { "$it out ↑" }

private fun batterySecondary(snapshot: EnergySnapshot): String? {
    val power = snapshot[EnergySlot.BATTERY_POWER] ?: return null
    val watts = power.numericValue ?: return power.display
    val soc = snapshot[EnergySlot.BATTERY_SOC]?.numericValue
    if (soc != null && soc >= 99.5 && abs(watts) < 50) return "Full"
    val direction = if (watts >= 0) "charging" else "discharging"
    return "$direction · ${power.display}"
}

/** Resolves the configured battery capacity, whichever source (manual value or HA entity) is active. */
private fun resolveBatteryCapacityKwh(prefs: Preferences): Float {
    val source = prefs[WidgetPrefsKeys.batteryCapacitySource]
        ?.let { runCatching { BatteryCapacitySource.valueOf(it) }.getOrNull() }
        ?: BatteryCapacitySource.MANUAL
    return when (source) {
        BatteryCapacitySource.MANUAL -> prefs[WidgetPrefsKeys.batteryCapacityManualKwh] ?: 0f
        BatteryCapacitySource.ENTITY -> prefs[WidgetPrefsKeys.batteryCapacityEntityValue]?.toFloatOrNull() ?: 0f
    }
}

/**
 * Home Assistant has no native "time to full" / "runtime remaining" sensor for a generic
 * inverter+battery setup (the reference sunsynk-power-flow-card computes this client-side from a
 * user-supplied battery capacity too) — so this is derived here from SOC% + instantaneous power,
 * only when a battery capacity is configured (manual value or entity).
 */
private fun batteryTimeEstimate(snapshot: EnergySnapshot, capacityKwh: Float, use24Hour: Boolean): String? {
    if (capacityKwh <= 0f) return null
    val soc = snapshot[EnergySlot.BATTERY_SOC]?.numericValue ?: return null
    val watts = snapshot[EnergySlot.BATTERY_POWER]?.numericValue ?: return null
    val reserveSoc = 5.0

    return when {
        soc >= 99.5 -> "Fully charged"
        watts > 50 -> {
            val remainingKwh = (100.0 - soc) / 100.0 * capacityKwh
            "To 100% @ ${targetTimeLabel(remainingKwh / (watts / 1000.0), use24Hour)}"
        }
        watts < -50 -> {
            if (soc <= reserveSoc) {
                "At ${reserveSoc.toInt()}% reserve"
            } else {
                val remainingKwh = (soc - reserveSoc) / 100.0 * capacityKwh
                "To ${reserveSoc.toInt()}% @ ${targetTimeLabel(remainingKwh / (-watts / 1000.0), use24Hour)}"
            }
        }
        soc <= reserveSoc -> "At ${reserveSoc.toInt()}% reserve"
        else -> "Idle"
    }
}

private fun targetTimeLabel(hoursFromNow: Double, use24Hour: Boolean): String {
    if (hoursFromNow.isNaN() || hoursFromNow.isInfinite() || hoursFromNow < 0) return "—"
    val targetMillis = System.currentTimeMillis() + (hoursFromNow * 3_600_000).toLong()
    return timeLabel(targetMillis, use24Hour)
}

private fun timeLabel(millis: Long?, use24Hour: Boolean): String {
    if (millis == null) return "never"
    val pattern = if (use24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
