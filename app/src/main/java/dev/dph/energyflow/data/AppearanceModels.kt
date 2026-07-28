package dev.dph.energyflow.data

enum class BatteryCapacitySource { MANUAL, ENTITY }

enum class FontSizeOption { SMALL, NORMAL, LARGE }

/** Multiplier applied to every text size in the widget. NORMAL matches the original design. */
val FontSizeOption.scale: Float
    get() = when (this) {
        FontSizeOption.SMALL -> 0.85f
        FontSizeOption.NORMAL -> 1f
        FontSizeOption.LARGE -> 1.2f
    }

/** Parses "#RRGGBB" / "#AARRGGBB" (case-insensitive); falls back to [default] on any bad input. */
fun parseHexColor(hex: String, default: Int): Int {
    val cleaned = hex.trim().removePrefix("#")
    return try {
        when (cleaned.length) {
            6 -> (0xFF000000.toInt()) or (cleaned.toLong(16).toInt() and 0x00FFFFFF)
            8 -> cleaned.toLong(16).toInt()
            else -> default
        }
    } catch (e: NumberFormatException) {
        default
    }
}

/** Formats an ARGB int as "#RRGGBB" (alpha carried separately by callers that need it). */
fun colorToRgbHex(argb: Int): String = "#%06X".format(argb and 0x00FFFFFF)

fun alphaPercentOf(argb: Int): Int = ((argb ushr 24) and 0xFF) * 100 / 255

fun withAlphaPercent(argb: Int, alphaPercent: Int): Int {
    val alpha = (alphaPercent.coerceIn(0, 100) * 255 / 100) and 0xFF
    return (alpha shl 24) or (argb and 0x00FFFFFF)
}
