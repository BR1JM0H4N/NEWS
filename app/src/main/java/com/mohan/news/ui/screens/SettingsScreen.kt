package com.mohan.news.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohan.news.data.AppSettings
import com.mohan.news.data.AppThemeMode
import com.mohan.news.data.NewsCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    availableVoiceNames: List<String>,
    onBack: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCountryChange: (code: String, language: String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onShowRelatedChange: (Boolean) -> Unit,
    onTtsSpeedChange: (Float) -> Unit,
    onTtsPitchChange: (Float) -> Unit,
    onTtsVoiceChange: (String?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader("Appearance") }
            item {
                SettingsCard {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                    ThemeModeSelector(current = settings.themeMode, onSelect = onThemeModeChange)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                    SwitchRow(
                        title = "Use system color palette",
                        subtitle = "Match Material You dynamic colors from your wallpaper (Android 12+)",
                        checked = settings.dynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            }

            item { SectionHeader("News Feed") }
            item {
                SettingsCard {
                    DropdownSettingRow(
                        title = "Country",
                        currentLabel = NewsCatalog.countries.firstOrNull { it.code == settings.countryCode }?.displayName
                            ?: settings.countryCode,
                        options = NewsCatalog.countries.map { it.displayName },
                        onOptionSelected = { index ->
                            val country = NewsCatalog.countries[index]
                            onCountryChange(country.code, country.language)
                        }
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                    DropdownSettingRow(
                        title = "Category",
                        currentLabel = NewsCatalog.categories.firstOrNull { it.id == settings.categoryId }?.displayName
                            ?: settings.categoryId,
                        options = NewsCatalog.categories.map { it.displayName },
                        onOptionSelected = { index ->
                            onCategoryChange(NewsCatalog.categories[index].id)
                        }
                    )
                }
            }

            item { SectionHeader("Story Coverage") }
            item {
                SettingsCard {
                    SwitchRow(
                        title = "Show related coverage",
                        subtitle = "Display differing takes from other sources under each story",
                        checked = settings.showRelatedCoverage,
                        onCheckedChange = onShowRelatedChange
                    )
                }
            }

            item { SectionHeader("Read Aloud") }
            item {
                SettingsCard {
                    Text("Speech speed: ${String.format("%.1fx", settings.ttsSpeed)}", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = settings.ttsSpeed,
                        onValueChange = onTtsSpeedChange,
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                    Text("Speech pitch: ${String.format("%.1f", settings.ttsPitch)}", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = settings.ttsPitch,
                        onValueChange = onTtsPitchChange,
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                    if (availableVoiceNames.isNotEmpty()) {
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                        DropdownSettingRow(
                            title = "Voice",
                            currentLabel = settings.ttsVoiceName?.let { simplifyVoiceName(it) } ?: "Default",
                            options = listOf("Default") + availableVoiceNames.map { simplifyVoiceName(it) },
                            onOptionSelected = { index ->
                                if (index == 0) onTtsVoiceChange(null) else onTtsVoiceChange(availableVoiceNames[index - 1])
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun simplifyVoiceName(name: String): String {
    // Android TTS voice names look like "en-us-x-tpf-local", make them a bit friendlier.
    return name.replace("-", " ").replace("x ", "").trim().ifBlank { name }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeModeSelector(current: AppThemeMode, onSelect: (AppThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemeMode.values().forEach { mode ->
            val selected = mode == current
            androidx.compose.material3.FilterChip(
                selected = selected,
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        when (mode) {
                            AppThemeMode.SYSTEM -> "System"
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.DARK -> "Dark"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun DropdownSettingRow(
    title: String,
    currentLabel: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Column {
            TextButton(onClick = { expanded = true }) {
                Text(currentLabel)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
