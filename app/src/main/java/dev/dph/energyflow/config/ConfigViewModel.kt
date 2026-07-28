package dev.dph.energyflow.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.dph.energyflow.data.BatteryCapacitySource
import dev.dph.energyflow.data.ConnectionPrefs
import dev.dph.energyflow.data.EnergyRepository
import dev.dph.energyflow.data.EnergySlot
import dev.dph.energyflow.data.FontSizeOption
import dev.dph.energyflow.data.HaClient
import dev.dph.energyflow.data.HaResult
import dev.dph.energyflow.data.HaState
import dev.dph.energyflow.data.WidgetOptions
import dev.dph.energyflow.widget.EnergyWidget
import dev.dph.energyflow.widget.RefreshScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, FAILED }

data class ConfigUiState(
    val baseUrl: String = "",
    val token: String = "",
    val connectionTest: ConnectionTestState = ConnectionTestState.IDLE,
    val connectionError: String? = null,
    val availableEntities: List<HaState> = emptyList(),
    val loadingEntities: Boolean = false,
    val entityMapping: Map<EnergySlot, String> = EnergySlot.entries.associateWith { it.defaultEntityId },
    val labels: Map<EnergySlot, String> = EnergySlot.entries.associateWith { it.defaultLabel },

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
    val batteryCapacityManualKwh: String = "",
    val batteryCapacityEntityId: String = "",

    val use24HourFormat: Boolean = true,
    val fontSize: FontSizeOption = FontSizeOption.NORMAL,

    val useCustomWidgetBackground: Boolean = false,
    val widgetBackgroundArgb: Int = WidgetOptions.DEFAULT_WIDGET_BACKGROUND_ARGB,
    val useCustomCardBackground: Boolean = false,
    val cardBackgroundArgb: Int = WidgetOptions.DEFAULT_CARD_BACKGROUND_ARGB,
    val useCustomPrimaryTextColor: Boolean = false,
    val primaryTextArgb: Int = WidgetOptions.DEFAULT_PRIMARY_TEXT_ARGB,
    val cornerRadiusDp: Float = WidgetOptions.DEFAULT_CORNER_RADIUS_DP,

    val saving: Boolean = false,
    val saved: Boolean = false,

    /** This config screen's own light/dark Material theme — unrelated to the widget's appearance. */
    val isDarkTheme: Boolean = true,
)

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    // Each widget has its own HA connection (see PLAN.md multi-instance support). The "template"
    // holds the last-used values purely to pre-fill a newly-placed widget's fields.
    private val templatePrefs = ConnectionPrefs(application, ConnectionPrefs.TEMPLATE_KEY)
    private var connectionPrefs: ConnectionPrefs = templatePrefs
    private val repository = EnergyRepository(application)
    private val uiThemePrefs = UiThemePrefs(application)

    private val _uiState = MutableStateFlow(
        ConfigUiState(
            baseUrl = templatePrefs.baseUrl,
            token = templatePrefs.token,
            isDarkTheme = uiThemePrefs.isDarkTheme,
        ),
    )
    val uiState: StateFlow<ConfigUiState> = _uiState

    private var glanceId: GlanceId? = null

    init {
        // Pre-populate the entity picker without forcing the user to re-tap "Test connection"
        // every time they reopen settings for an already-configured widget.
        if (templatePrefs.isConfigured) {
            testConnectionAndLoadEntities()
        }
    }

    fun loadForWidget(appWidgetId: Int?) {
        if (appWidgetId == null) return
        // The Activity/ViewModel instance can be reused for a different widget than the one it
        // last showed (see ConfigActivity.onNewIntent) — wipe any in-memory state from that
        // previous widget so it can't leak into this one, or get silently saved over it.
        glanceId = null
        _uiState.update {
            ConfigUiState(
                baseUrl = templatePrefs.baseUrl,
                token = templatePrefs.token,
                isDarkTheme = it.isDarkTheme,
            )
        }
        viewModelScope.launch {
            val manager = GlanceAppWidgetManager(getApplication())
            val id = runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull() ?: return@launch
            glanceId = id

            // This widget's own saved connection, if it has one — otherwise keep whatever the
            // template pre-filled the fields with (a fresh widget being configured for the
            // first time), so single-HA-instance users don't need to retype anything.
            val widgetPrefs = ConnectionPrefs(getApplication(), appWidgetId.toString())
            connectionPrefs = widgetPrefs
            if (widgetPrefs.isConfigured) {
                _uiState.update { it.copy(baseUrl = widgetPrefs.baseUrl, token = widgetPrefs.token) }
                testConnectionAndLoadEntities()
            }

            val mapping = repository.loadMapping(id)
            val labels = repository.loadLabels(id)
            val options = repository.loadOptions(id)
            _uiState.update {
                it.copy(
                    entityMapping = mapping,
                    labels = labels,
                    showOpenHaButton = options.showOpenHaButton,
                    showSettingsButton = options.showSettingsButton,
                    showTitle = options.showTitle,
                    showHeader = options.showHeader,
                    titleText = options.titleText,
                    showSolarCard = options.showSolarCard,
                    showGridCard = options.showGridCard,
                    showBatteryCard = options.showBatteryCard,
                    showHomeCard = options.showHomeCard,
                    showFlowCard = options.showFlowCard,
                    batteryCapacitySource = options.batteryCapacitySource,
                    batteryCapacityManualKwh = if (options.batteryCapacityManualKwh > 0f) {
                        options.batteryCapacityManualKwh.toString()
                    } else {
                        ""
                    },
                    batteryCapacityEntityId = options.batteryCapacityEntityId,
                    use24HourFormat = options.use24HourFormat,
                    fontSize = options.fontSize,
                    useCustomWidgetBackground = options.useCustomWidgetBackground,
                    widgetBackgroundArgb = options.widgetBackgroundArgb,
                    useCustomCardBackground = options.useCustomCardBackground,
                    cardBackgroundArgb = options.cardBackgroundArgb,
                    useCustomPrimaryTextColor = options.useCustomPrimaryTextColor,
                    primaryTextArgb = options.primaryTextArgb,
                    cornerRadiusDp = options.cornerRadiusDp,
                )
            }
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value, connectionTest = ConnectionTestState.IDLE) }
    }

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(token = value, connectionTest = ConnectionTestState.IDLE) }
    }

    fun onEntityChange(slot: EnergySlot, entityId: String) {
        _uiState.update { it.copy(entityMapping = it.entityMapping + (slot to entityId)) }
    }

    fun onLabelChange(slot: EnergySlot, label: String) {
        _uiState.update { it.copy(labels = it.labels + (slot to label)) }
    }

    fun onShowOpenHaButtonChange(value: Boolean) {
        _uiState.update { it.copy(showOpenHaButton = value) }
    }

    fun onShowSettingsButtonChange(value: Boolean) {
        _uiState.update { it.copy(showSettingsButton = value) }
    }

    fun onShowTitleChange(value: Boolean) {
        _uiState.update { it.copy(showTitle = value) }
    }

    fun onShowHeaderChange(value: Boolean) {
        _uiState.update { it.copy(showHeader = value) }
    }

    fun onTitleTextChange(value: String) {
        _uiState.update { it.copy(titleText = value) }
    }

    fun onShowSolarCardChange(value: Boolean) = updateCardVisibility { it.copy(showSolarCard = value) }
    fun onShowGridCardChange(value: Boolean) = updateCardVisibility { it.copy(showGridCard = value) }
    fun onShowBatteryCardChange(value: Boolean) = updateCardVisibility { it.copy(showBatteryCard = value) }
    fun onShowHomeCardChange(value: Boolean) = updateCardVisibility { it.copy(showHomeCard = value) }
    fun onShowFlowCardChange(value: Boolean) = updateCardVisibility { it.copy(showFlowCard = value) }

    /** Refuses a change that would leave every one of the 5 cards hidden — at least one must show. */
    private fun updateCardVisibility(transform: (ConfigUiState) -> ConfigUiState) {
        _uiState.update { current ->
            val next = transform(current)
            val anyVisible = next.showSolarCard || next.showGridCard || next.showBatteryCard ||
                next.showHomeCard || next.showFlowCard
            if (anyVisible) next else current
        }
    }

    fun onBatteryCapacitySourceChange(value: BatteryCapacitySource) {
        _uiState.update { it.copy(batteryCapacitySource = value) }
    }

    fun onBatteryCapacityManualChange(value: String) {
        _uiState.update { it.copy(batteryCapacityManualKwh = value) }
    }

    fun onBatteryCapacityEntityChange(value: String) {
        _uiState.update { it.copy(batteryCapacityEntityId = value) }
    }

    fun onUse24HourFormatChange(value: Boolean) {
        _uiState.update { it.copy(use24HourFormat = value) }
    }

    fun onFontSizeChange(value: FontSizeOption) {
        _uiState.update { it.copy(fontSize = value) }
    }

    fun onUseCustomWidgetBackgroundChange(value: Boolean) {
        _uiState.update { it.copy(useCustomWidgetBackground = value) }
    }

    fun onWidgetBackgroundArgbChange(value: Int) {
        _uiState.update { it.copy(widgetBackgroundArgb = value) }
    }

    fun onUseCustomCardBackgroundChange(value: Boolean) {
        _uiState.update { it.copy(useCustomCardBackground = value) }
    }

    fun onCardBackgroundArgbChange(value: Int) {
        _uiState.update { it.copy(cardBackgroundArgb = value) }
    }

    fun onUseCustomPrimaryTextColorChange(value: Boolean) {
        _uiState.update { it.copy(useCustomPrimaryTextColor = value) }
    }

    fun onPrimaryTextArgbChange(value: Int) {
        _uiState.update { it.copy(primaryTextArgb = value) }
    }

    fun onCornerRadiusChange(value: Float) {
        _uiState.update { it.copy(cornerRadiusDp = value) }
    }

    fun onToggleUiTheme() {
        val newValue = !_uiState.value.isDarkTheme
        uiThemePrefs.isDarkTheme = newValue
        _uiState.update { it.copy(isDarkTheme = newValue) }
    }

    /** Resets only the Appearance-tab fields; entity mapping and the HA connection are untouched. */
    fun resetAppearanceToDefaults() {
        val d = WidgetOptions.APPEARANCE_DEFAULTS
        _uiState.update {
            it.copy(
                showOpenHaButton = d.showOpenHaButton,
                showSettingsButton = d.showSettingsButton,
                showTitle = d.showTitle,
                showHeader = d.showHeader,
                titleText = d.titleText,
                showSolarCard = d.showSolarCard,
                showGridCard = d.showGridCard,
                showBatteryCard = d.showBatteryCard,
                showHomeCard = d.showHomeCard,
                showFlowCard = d.showFlowCard,
                use24HourFormat = d.use24HourFormat,
                fontSize = d.fontSize,
                useCustomWidgetBackground = d.useCustomWidgetBackground,
                widgetBackgroundArgb = d.widgetBackgroundArgb,
                useCustomCardBackground = d.useCustomCardBackground,
                cardBackgroundArgb = d.cardBackgroundArgb,
                useCustomPrimaryTextColor = d.useCustomPrimaryTextColor,
                primaryTextArgb = d.primaryTextArgb,
                cornerRadiusDp = d.cornerRadiusDp,
            )
        }
    }

    fun testConnectionAndLoadEntities() {
        val state = _uiState.value
        if (state.baseUrl.isBlank() || state.token.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionTest = ConnectionTestState.FAILED,
                    connectionError = "Enter both the HA URL and an access token.",
                )
            }
            return
        }
        _uiState.update { it.copy(connectionTest = ConnectionTestState.TESTING, loadingEntities = true) }
        viewModelScope.launch {
            val client = HaClient(state.baseUrl, state.token)
            when (val result = client.testConnection()) {
                is HaResult.Success -> {
                    _uiState.update { it.copy(connectionTest = ConnectionTestState.SUCCESS, connectionError = null) }
                    loadEntities(client)
                }
                is HaResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            connectionTest = ConnectionTestState.FAILED,
                            connectionError = result.message,
                            loadingEntities = false,
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadEntities(client: HaClient) {
        when (val result = client.fetchAllStates()) {
            is HaResult.Success -> {
                val sorted = result.value.sortedBy { it.entity_id }
                _uiState.update { it.copy(availableEntities = sorted, loadingEntities = false) }
            }
            is HaResult.Failure -> {
                _uiState.update {
                    it.copy(loadingEntities = false, connectionError = result.message)
                }
            }
        }
    }

    /**
     * Persists the HA connection + this widget's entity mapping, then kicks an immediate refresh
     * (see PLAN.md: "Immediate refresh the moment the widget is added to the home screen").
     */
    fun save(appWidgetId: Int?, onDone: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = glanceId
                ?: appWidgetId?.let {
                    runCatching { GlanceAppWidgetManager(getApplication()).getGlanceIdBy(it) }.getOrNull()
                }

            // Resolve (or re-resolve) this specific widget's own connection store — loadForWidget
            // may not have run yet if save() is reached very quickly after a fresh placement.
            val targetPrefs = if (id != null && appWidgetId != null) {
                ConnectionPrefs(getApplication(), appWidgetId.toString()).also { connectionPrefs = it }
            } else {
                connectionPrefs
            }
            targetPrefs.baseUrl = state.baseUrl
            targetPrefs.token = state.token
            // Keep the template in sync too, so the next new widget pre-fills with these values.
            if (targetPrefs !== templatePrefs) {
                templatePrefs.baseUrl = state.baseUrl
                templatePrefs.token = state.token
            }

            if (id != null) {
                val options = WidgetOptions(
                    showOpenHaButton = state.showOpenHaButton,
                    showSettingsButton = state.showSettingsButton,
                    showTitle = state.showTitle,
                    showHeader = state.showHeader,
                    titleText = state.titleText.ifBlank { "Energy Flow" },
                    showSolarCard = state.showSolarCard,
                    showGridCard = state.showGridCard,
                    showBatteryCard = state.showBatteryCard,
                    showHomeCard = state.showHomeCard,
                    showFlowCard = state.showFlowCard,
                    batteryCapacitySource = state.batteryCapacitySource,
                    batteryCapacityManualKwh = state.batteryCapacityManualKwh.toFloatOrNull() ?: 0f,
                    batteryCapacityEntityId = state.batteryCapacityEntityId,
                    use24HourFormat = state.use24HourFormat,
                    fontSize = state.fontSize,
                    useCustomWidgetBackground = state.useCustomWidgetBackground,
                    widgetBackgroundArgb = state.widgetBackgroundArgb,
                    useCustomCardBackground = state.useCustomCardBackground,
                    cardBackgroundArgb = state.cardBackgroundArgb,
                    useCustomPrimaryTextColor = state.useCustomPrimaryTextColor,
                    primaryTextArgb = state.primaryTextArgb,
                    cornerRadiusDp = state.cornerRadiusDp,
                )
                repository.saveMapping(id, state.entityMapping, state.labels, options)
                repository.refresh(id)
                EnergyWidget().update(getApplication(), id)
            }
            if (id == null) {
                // Nothing was actually persisted — don't claim success or close the screen, or
                // the user is left believing settings saved when they silently didn't.
                _uiState.update {
                    it.copy(
                        saving = false,
                        connectionError = "Couldn't identify this widget instance. Please close this screen and try again.",
                    )
                }
                return@launch
            }

            RefreshScheduler.requestImmediate(getApplication())

            _uiState.update { it.copy(saving = false, saved = true) }
            onDone()
        }
    }
}
