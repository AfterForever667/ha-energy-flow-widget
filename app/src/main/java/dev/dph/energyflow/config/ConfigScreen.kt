package dev.dph.energyflow.config

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.dph.energyflow.data.BatteryCapacitySource
import dev.dph.energyflow.data.EnergySlot
import dev.dph.energyflow.data.FontSizeOption
import dev.dph.energyflow.data.HaState
import dev.dph.energyflow.data.SlotCategory
import dev.dph.energyflow.data.SlotKind
import dev.dph.energyflow.data.alphaPercentOf
import dev.dph.energyflow.data.colorToRgbHex
import dev.dph.energyflow.data.parseHexColor
import dev.dph.energyflow.data.withAlphaPercent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    state: ConfigUiState,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onEntityChange: (EnergySlot, String) -> Unit,
    onLabelChange: (EnergySlot, String) -> Unit,
    onBatteryCapacitySourceChange: (BatteryCapacitySource) -> Unit,
    onBatteryCapacityManualChange: (String) -> Unit,
    onBatteryCapacityEntityChange: (String) -> Unit,
    onShowOpenHaButtonChange: (Boolean) -> Unit,
    onShowSettingsButtonChange: (Boolean) -> Unit,
    onShowTitleChange: (Boolean) -> Unit,
    onShowHeaderChange: (Boolean) -> Unit,
    onTitleTextChange: (String) -> Unit,
    onShowSolarCardChange: (Boolean) -> Unit,
    onShowGridCardChange: (Boolean) -> Unit,
    onShowBatteryCardChange: (Boolean) -> Unit,
    onShowHomeCardChange: (Boolean) -> Unit,
    onShowFlowCardChange: (Boolean) -> Unit,
    onUse24HourFormatChange: (Boolean) -> Unit,
    onFontSizeChange: (FontSizeOption) -> Unit,
    onUseCustomWidgetBackgroundChange: (Boolean) -> Unit,
    onWidgetBackgroundArgbChange: (Int) -> Unit,
    onUseCustomCardBackgroundChange: (Boolean) -> Unit,
    onCardBackgroundArgbChange: (Int) -> Unit,
    onUseCustomPrimaryTextColorChange: (Boolean) -> Unit,
    onPrimaryTextArgbChange: (Int) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onResetAppearance: () -> Unit,
    onToggleUiTheme: () -> Unit,
    onSave: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy Flow setup") },
                actions = {
                    IconButton(onClick = onToggleUiTheme) {
                        Icon(
                            if (state.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (state.isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = !state.saving && state.baseUrl.isNotBlank() && state.token.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(if (state.saving) "Saving…" else "Save & add widget")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sensors") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Appearance") })
            }
            when (selectedTab) {
                0 -> SensorsTab(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onBaseUrlChange = onBaseUrlChange,
                    onTokenChange = onTokenChange,
                    onTestConnection = onTestConnection,
                    onEntityChange = onEntityChange,
                    onLabelChange = onLabelChange,
                    onBatteryCapacitySourceChange = onBatteryCapacitySourceChange,
                    onBatteryCapacityManualChange = onBatteryCapacityManualChange,
                    onBatteryCapacityEntityChange = onBatteryCapacityEntityChange,
                )
                1 -> AppearanceTab(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onShowTitleChange = onShowTitleChange,
                    onShowHeaderChange = onShowHeaderChange,
                    onTitleTextChange = onTitleTextChange,
                    onShowOpenHaButtonChange = onShowOpenHaButtonChange,
                    onShowSettingsButtonChange = onShowSettingsButtonChange,
                    onShowSolarCardChange = onShowSolarCardChange,
                    onShowGridCardChange = onShowGridCardChange,
                    onShowBatteryCardChange = onShowBatteryCardChange,
                    onShowHomeCardChange = onShowHomeCardChange,
                    onShowFlowCardChange = onShowFlowCardChange,
                    onUse24HourFormatChange = onUse24HourFormatChange,
                    onFontSizeChange = onFontSizeChange,
                    onUseCustomWidgetBackgroundChange = onUseCustomWidgetBackgroundChange,
                    onWidgetBackgroundArgbChange = onWidgetBackgroundArgbChange,
                    onUseCustomCardBackgroundChange = onUseCustomCardBackgroundChange,
                    onCardBackgroundArgbChange = onCardBackgroundArgbChange,
                    onUseCustomPrimaryTextColorChange = onUseCustomPrimaryTextColorChange,
                    onPrimaryTextArgbChange = onPrimaryTextArgbChange,
                    onCornerRadiusChange = onCornerRadiusChange,
                    onResetAppearance = onResetAppearance,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensorsTab(
    modifier: Modifier,
    state: ConfigUiState,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onEntityChange: (EnergySlot, String) -> Unit,
    onLabelChange: (EnergySlot, String) -> Unit,
    onBatteryCapacitySourceChange: (BatteryCapacitySource) -> Unit,
    onBatteryCapacityManualChange: (String) -> Unit,
    onBatteryCapacityEntityChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.padding(2.dp)) }
        item {
            Text("Home Assistant connection", style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("Base URL (e.g. https://ha.example.com)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.token,
                onValueChange = onTokenChange,
                label = { Text("Long-lived access token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Button(onClick = onTestConnection, enabled = state.connectionTest != ConnectionTestState.TESTING) {
                    Text("Test connection")
                }
                Spacer(modifier = Modifier.padding(2.dp))
                when (state.connectionTest) {
                    ConnectionTestState.TESTING -> CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                    ConnectionTestState.SUCCESS -> Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    ConnectionTestState.FAILED -> Icon(
                        Icons.Filled.Error,
                        contentDescription = "Failed",
                        tint = MaterialTheme.colorScheme.error,
                    )
                    ConnectionTestState.IDLE -> {}
                }
            }
        }
        if (state.connectionError != null) {
            item {
                Text(state.connectionError, color = MaterialTheme.colorScheme.error)
            }
        }

        item { HorizontalDivider() }
        item { Text("Battery capacity", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Optional — Home Assistant has no built-in capacity sensor for a generic setup. Fill this in (or point it at your own sensor) to show a full-charge / runtime estimate on the battery card.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.batteryCapacitySource == BatteryCapacitySource.MANUAL,
                    onClick = { onBatteryCapacitySourceChange(BatteryCapacitySource.MANUAL) },
                    label = { Text("Manual value") },
                )
                FilterChip(
                    selected = state.batteryCapacitySource == BatteryCapacitySource.ENTITY,
                    onClick = { onBatteryCapacitySourceChange(BatteryCapacitySource.ENTITY) },
                    label = { Text("From sensor") },
                )
            }
        }
        if (state.batteryCapacitySource == BatteryCapacitySource.MANUAL) {
            item {
                OutlinedTextField(
                    value = state.batteryCapacityManualKwh,
                    onValueChange = onBatteryCapacityManualChange,
                    label = { Text("Battery capacity (kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item {
                EntityPickerField(
                    label = "Capacity sensor",
                    entityId = state.batteryCapacityEntityId,
                    availableEntities = state.availableEntities,
                    expectedUnits = EnergySlot.expectedUnits(SlotKind.ENERGY_KWH),
                    onEntityChange = onBatteryCapacityEntityChange,
                )
            }
        }

        item { HorizontalDivider() }
        item { Text("Entity mapping", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Defaults are pre-filled from a typical Sunsynk/DSMR setup — remap any slot to your own entities.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        for (category in SlotCategory.entries) {
            item {
                Text(
                    category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(EnergySlot.entries.filter { it.category == category }) { slot ->
                SlotRow(
                    slot = slot,
                    entityId = state.entityMapping[slot] ?: slot.defaultEntityId,
                    label = state.labels[slot] ?: slot.defaultLabel,
                    availableEntities = state.availableEntities,
                    onEntityChange = { onEntityChange(slot, it) },
                    onLabelChange = { onLabelChange(slot, it) },
                )
            }
        }
        item { Spacer(modifier = Modifier.padding(2.dp)) }
    }
}

@Composable
private fun AppearanceTab(
    modifier: Modifier,
    state: ConfigUiState,
    onShowTitleChange: (Boolean) -> Unit,
    onShowHeaderChange: (Boolean) -> Unit,
    onTitleTextChange: (String) -> Unit,
    onShowOpenHaButtonChange: (Boolean) -> Unit,
    onShowSettingsButtonChange: (Boolean) -> Unit,
    onShowSolarCardChange: (Boolean) -> Unit,
    onShowGridCardChange: (Boolean) -> Unit,
    onShowBatteryCardChange: (Boolean) -> Unit,
    onShowHomeCardChange: (Boolean) -> Unit,
    onShowFlowCardChange: (Boolean) -> Unit,
    onUse24HourFormatChange: (Boolean) -> Unit,
    onFontSizeChange: (FontSizeOption) -> Unit,
    onUseCustomWidgetBackgroundChange: (Boolean) -> Unit,
    onWidgetBackgroundArgbChange: (Int) -> Unit,
    onUseCustomCardBackgroundChange: (Boolean) -> Unit,
    onCardBackgroundArgbChange: (Int) -> Unit,
    onUseCustomPrimaryTextColorChange: (Boolean) -> Unit,
    onPrimaryTextArgbChange: (Int) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onResetAppearance: () -> Unit,
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showHeaderOffConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.padding(2.dp)) }
        item { Text("Header", style = MaterialTheme.typography.titleMedium) }
        item {
            ToggleRow(
                title = "Show header",
                subtitle = "Hides the whole top row (title, buttons, and time) to free up space for cards.",
                checked = state.showHeader,
                onCheckedChange = { checked ->
                    if (checked) {
                        onShowHeaderChange(true)
                    } else {
                        showHeaderOffConfirm = true
                    }
                },
            )
        }
        item {
            ToggleRow(
                title = "Show title",
                subtitle = "Shows the title text at the top of the widget.",
                checked = state.showTitle,
                onCheckedChange = onShowTitleChange,
            )
        }
        item {
            OutlinedTextField(
                value = state.titleText,
                onValueChange = onTitleTextChange,
                label = { Text("Title text") },
                singleLine = true,
                enabled = state.showTitle && state.showHeader,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { HorizontalDivider() }
        item { Text("Widget buttons", style = MaterialTheme.typography.titleMedium) }
        item {
            ToggleRow(
                title = "Open Home Assistant button",
                subtitle = "Shows a button that opens the HA app (or your dashboard in a browser).",
                checked = state.showOpenHaButton,
                onCheckedChange = onShowOpenHaButtonChange,
            )
        }
        item {
            ToggleRow(
                title = "Widget settings button",
                subtitle = "Shows a button that reopens this screen for the placed widget.",
                checked = state.showSettingsButton,
                onCheckedChange = onShowSettingsButtonChange,
            )
        }

        item { HorizontalDivider() }
        item { Text("Widget cards", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Hide cards you don't need — e.g. no battery or no solar. At least one card must stay visible; the rest reflow to fill the space.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            ToggleRow(
                title = "Solar card",
                checked = state.showSolarCard,
                onCheckedChange = onShowSolarCardChange,
            )
        }
        item {
            ToggleRow(
                title = "Grid card",
                checked = state.showGridCard,
                onCheckedChange = onShowGridCardChange,
            )
        }
        item {
            ToggleRow(
                title = "Battery card",
                checked = state.showBatteryCard,
                onCheckedChange = onShowBatteryCardChange,
            )
        }
        item {
            ToggleRow(
                title = "Home card",
                checked = state.showHomeCard,
                onCheckedChange = onShowHomeCardChange,
            )
        }
        item {
            ToggleRow(
                title = "Power flow card",
                checked = state.showFlowCard,
                onCheckedChange = onShowFlowCardChange,
            )
        }

        item { HorizontalDivider() }
        item { Text("Time format", style = MaterialTheme.typography.titleMedium) }
        item {
            ToggleRow(
                title = "24-hour clock",
                subtitle = "Off shows 12-hour time with AM/PM instead.",
                checked = state.use24HourFormat,
                onCheckedChange = onUse24HourFormatChange,
            )
        }

        item { HorizontalDivider() }
        item { Text("Font size", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in FontSizeOption.entries) {
                    FilterChip(
                        selected = state.fontSize == option,
                        onClick = { onFontSizeChange(option) },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Colors", style = MaterialTheme.typography.titleMedium) }
        item {
            ColorSettingRow(
                title = "Widget background",
                useCustom = state.useCustomWidgetBackground,
                onUseCustomChange = onUseCustomWidgetBackgroundChange,
                argb = state.widgetBackgroundArgb,
                onArgbChange = onWidgetBackgroundArgbChange,
                showAlpha = true,
            )
        }
        item {
            ColorSettingRow(
                title = "Card background",
                useCustom = state.useCustomCardBackground,
                onUseCustomChange = onUseCustomCardBackgroundChange,
                argb = state.cardBackgroundArgb,
                onArgbChange = onCardBackgroundArgbChange,
                showAlpha = true,
            )
        }
        item {
            ColorSettingRow(
                title = "Value text color",
                useCustom = state.useCustomPrimaryTextColor,
                onUseCustomChange = onUseCustomPrimaryTextColorChange,
                argb = state.primaryTextArgb,
                onArgbChange = onPrimaryTextArgbChange,
                showAlpha = false,
            )
        }

        item { HorizontalDivider() }
        item { Text("Shape", style = MaterialTheme.typography.titleMedium) }
        item {
            Column {
                Text(
                    "Corner roundness: ${state.cornerRadiusDp.toInt()} dp",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = state.cornerRadiusDp,
                    onValueChange = onCornerRadiusChange,
                    valueRange = 0f..32f,
                    steps = 15,
                )
            }
        }

        item { HorizontalDivider() }
        item {
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset appearance to defaults")
            }
        }
        item { Spacer(modifier = Modifier.padding(2.dp)) }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset appearance?") },
            text = {
                Text(
                    "This resets the title, buttons, time format, font size, colors, and corner " +
                        "roundness back to their defaults. Your sensor mapping and HA connection are " +
                        "not affected. Tap Save afterwards to apply.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onResetAppearance()
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showHeaderOffConfirm) {
        AlertDialog(
            onDismissRequest = { showHeaderOffConfirm = false },
            title = { Text("Hide the header?") },
            text = {
                Text(
                    "This also hides the widget settings button, so you won't be able to reopen " +
                        "this screen from the widget itself afterwards. On most launchers you can " +
                        "still get back in by long-pressing the widget and choosing Edit/Configure. " +
                        "Continue?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onShowHeaderChange(false)
                    showHeaderOffConfirm = false
                }) { Text("Hide header") }
            },
            dismissButton = {
                TextButton(onClick = { showHeaderOffConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotRow(
    slot: EnergySlot,
    entityId: String,
    label: String,
    availableEntities: List<HaState>,
    onEntityChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
) {
    var labelField by remember(slot) { mutableStateOf(label) }
    var showInfo by remember(slot) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = labelField,
                onValueChange = {
                    labelField = it
                    onLabelChange(it)
                },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Filled.Info, contentDescription = "What is this sensor?")
            }
        }
        EntityPickerField(
            label = slot.defaultLabel,
            entityId = entityId,
            availableEntities = availableEntities,
            expectedUnits = EnergySlot.expectedUnits(slot.kind),
            onEntityChange = onEntityChange,
        )
    }

    if (showInfo) {
        InfoDialog(title = slot.defaultLabel, description = slot.description, onDismiss = { showInfo = false })
    }
}

@Composable
private fun InfoDialog(title: String, description: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

/**
 * Editable entity-id field with a dropdown suggestion list filtered to entities whose
 * `unit_of_measurement` actually matches what this field expects — e.g. a power (W) slot only
 * suggests other W sensors, not every entity in the house.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityPickerField(
    label: String,
    entityId: String,
    availableEntities: List<HaState>,
    expectedUnits: Set<String>,
    onEntityChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = entityId,
            onValueChange = onEntityChange,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        val candidates = availableEntities
            .filter { it.entity_id.startsWith("sensor.") }
            .filter { expectedUnits.isEmpty() || it.unitOfMeasurement in expectedUnits }
            .take(60)
        if (candidates.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                for (candidate in candidates) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(candidate.friendlyName ?: candidate.entity_id)
                                Text(
                                    candidate.entity_id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onEntityChange(candidate.entity_id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ColorSettingRow(
    title: String,
    useCustom: Boolean,
    onUseCustomChange: (Boolean) -> Unit,
    argb: Int,
    onArgbChange: (Int) -> Unit,
    showAlpha: Boolean,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = title,
            subtitle = "Use a custom color instead of the default theme color.",
            checked = useCustom,
            onCheckedChange = onUseCustomChange,
        )
        if (useCustom) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(argb), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showPicker = true },
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
                OutlinedTextField(
                    value = colorToRgbHex(argb),
                    onValueChange = { onArgbChange(parseHexColor(it, argb)) },
                    label = { Text("Hex color (#RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            if (showAlpha) {
                val alphaPercent = alphaPercentOf(argb)
                Text("Opacity: $alphaPercent%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = alphaPercent.toFloat(),
                    onValueChange = { onArgbChange(withAlphaPercent(argb, it.toInt())) },
                    valueRange = 0f..100f,
                )
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initialArgb = argb,
            onColorSelected = { onArgbChange(it) },
            onDismiss = { showPicker = false },
        )
    }
}

/** Hue-strip + saturation/value box color picker, opened by tapping a color swatch. */
@Composable
private fun ColorPickerDialog(
    initialArgb: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val startHsv = remember(initialArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialArgb, it) }
    }
    var hue by remember { mutableStateOf(startHsv[0]) }
    var sat by remember { mutableStateOf(startHsv[1]) }
    var value by remember { mutableStateOf(startHsv[2]) }

    val boxSize = 220.dp
    val currentColor = remember(hue, sat, value) {
        AndroidColor.HSVToColor(floatArrayOf(hue, sat, value))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(boxSize)) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(hue) {
                                detectTapGestures { offset ->
                                    sat = (offset.x / size.width).coerceIn(0f, 1f)
                                    value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                                }
                            }
                            .pointerInput(hue) {
                                detectDragGestures { change, _ ->
                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                }
                            },
                    ) {
                        val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = boxSize * sat - 8.dp, y = boxSize * (1f - value) - 8.dp)
                            .size(16.dp)
                            .border(2.dp, Color.White, CircleShape),
                    )
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        },
                ) {
                    val hueColors = (0..6).map { step ->
                        Color(AndroidColor.HSVToColor(floatArrayOf(step * 60f, 1f, 1f)))
                    }
                    drawRect(Brush.horizontalGradient(hueColors))
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(currentColor), RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                    )
                    Spacer(modifier = Modifier.padding(6.dp))
                    Text(colorToRgbHex(currentColor))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(withAlphaPercent(currentColor, alphaPercentOf(initialArgb)))
                onDismiss()
            }) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

