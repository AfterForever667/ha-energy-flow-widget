package dev.dph.energyflow.config

import android.content.Context

/**
 * Whether the config screen itself uses light or dark Material theme — purely a UI preference for
 * this app's own screens, unrelated to the widget's own (separately configurable) appearance.
 */
class UiThemePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ui_theme_prefs", Context.MODE_PRIVATE)

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    private companion object {
        const val KEY_DARK_THEME = "is_dark_theme"
    }
}
