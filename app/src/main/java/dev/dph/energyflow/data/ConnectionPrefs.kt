package dev.dph.energyflow.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores one Home Assistant base URL + long-lived access token, scoped by [widgetKey] so
 * different widget instances can point at different HA instances. Pass [TEMPLATE_KEY] for the
 * "last used" values that pre-fill a newly-placed widget before it has its own saved connection.
 */
class ConnectionPrefs(context: Context, private val widgetKey: String) {

    private val appContext = context.applicationContext

    private val masterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            "ha_connection_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).also { migrateLegacyGlobalValueIfNeeded(it) }
    }

    var baseUrl: String
        get() = prefs.getString(scopedKey(KEY_BASE_URL), "").orEmpty()
        set(value) = prefs.edit().putString(scopedKey(KEY_BASE_URL), value.trim()).apply()

    var token: String
        get() = prefs.getString(scopedKey(KEY_TOKEN), "").orEmpty()
        set(value) = prefs.edit().putString(scopedKey(KEY_TOKEN), value.trim()).apply()

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && token.isNotBlank()

    fun clear() = prefs.edit().remove(scopedKey(KEY_BASE_URL)).remove(scopedKey(KEY_TOKEN)).apply()

    private fun scopedKey(name: String): String = "${name}_$widgetKey"

    /**
     * One-time carry-over from the pre-multi-instance single global connection, so an existing
     * widget from before this feature doesn't suddenly lose its saved connection.
     */
    private fun migrateLegacyGlobalValueIfNeeded(prefs: android.content.SharedPreferences) {
        if (prefs.contains(scopedKey(KEY_BASE_URL)) || prefs.contains(scopedKey(KEY_TOKEN))) return
        val legacyUrl = prefs.getString(KEY_BASE_URL, null)
        val legacyToken = prefs.getString(KEY_TOKEN, null)
        if (legacyUrl != null || legacyToken != null) {
            prefs.edit()
                .putString(scopedKey(KEY_BASE_URL), legacyUrl.orEmpty())
                .putString(scopedKey(KEY_TOKEN), legacyToken.orEmpty())
                .apply()
        }
    }

    companion object {
        /** Key for the app-wide "last used" connection that pre-fills newly-placed widgets. */
        const val TEMPLATE_KEY = "template"

        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
    }
}
