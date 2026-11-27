package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppSettings
import com.example.model.AceSettings
import com.example.model.GenerationParameters
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun AdvancedSettingsScreen(
    settings: AppSettings,
    onUpdateParams: (GenerationParameters) -> Unit,
    onUpdateAce: (AceSettings) -> Unit,
    onOpenSystemPromptEditor: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val params = settings.generationParams
    val ace = settings.aceSettings
    var stopSeqDraft by remember(params.stopSequences) { mutableStateOf(params.stopSequences) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Advanced Lab & ACE Control",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Kontrol penuh parameter sampling Gemini, Adaptive Conversation Engine (ACE), dan System Prompt tanpa batasan tersembunyi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Quick Links: System Prompt Editor & About
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSystemPromptEditor() }
                        .testTag("open_system_prompt_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Psychology, contentDescription = null, tint = BrightViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("System Prompt", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Edit & Presets", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenAbout() }
                        .testTag("open_about_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("About Lab", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Security & CI", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        }

        // ACE - Adaptive Conversation Engine Controls
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACE — Adaptive Conversation Engine",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    AceToggleRow(
                        title = "Enable ACE Engine",
                        subtitle = "Active diagnostics, error classification, and continuity guard",
                        checked = ace.enabled,
                        onCheckedChange = { onUpdateAce(ace.copy(enabled = it)) }
                    )
                    AceToggleRow(
                        title = "Auto Smart Rephrase",
                        subtitle = "Clarify ambiguous or malformed prompts without changing user intent",
                        checked = ace.autoRephrase,
                        enabled = ace.enabled,
                        onCheckedChange = { onUpdateAce(ace.copy(autoRephrase = it)) }
                    )
                    AceToggleRow(
                        title = "Require Manual Approval",
                        subtitle = "Show Original vs Improved prompt before sending to Gemini",
                        checked = ace.requireManualApproval,
                        enabled = ace.enabled && ace.autoRephrase,
                        onCheckedChange = { onUpdateAce(ace.copy(requireManualApproval = it)) }
                    )
                    AceToggleRow(
                        title = "Automatic Network & 429 Recovery",
                        subtitle = "Retry transient network/quota errors transparently with backoff",
                        checked = ace.autoRecovery,
                        enabled = ace.enabled,
                        onCheckedChange = { onUpdateAce(ace.copy(autoRecovery = it)) }
                    )
                    AceToggleRow(
                        title = "Auto Context Compression",
                        subtitle = "Summarize old turns when approaching token limit while keeping facts",
                        checked = ace.autoCompressContext,
                        enabled = ace.enabled,
                        onCheckedChange = { onUpdateAce(ace.copy(autoCompressContext = it)) }
                    )

                    ParameterSliderRow(
                        label = "Context Compression Threshold",
                        valueText = "${ace.compressionThresholdPercent}%",
                        value = ace.compressionThresholdPercent.toFloat(),
                        range = 50f..95f,
                        onValueChange = {
                            onUpdateAce(ace.copy(compressionThresholdPercent = it.roundToInt()))
                        }
                    )
                }
            }
        }

        // Gemini Sampling & Token Parameters
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = BrightViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Generation Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        OutlinedButton(onClick = { onUpdateParams(GenerationParameters()) }) {
                            Text("Reset")
                        }
                    }

                    ParameterSliderRow(
                        label = "Temperature",
                        valueText = "%.2f".format(params.temperature),
                        value = params.temperature,
                        range = 0f..2f,
                        onValueChange = { onUpdateParams(params.copy(temperature = it)) }
                    )
                    ParameterSliderRow(
                        label = "Top-P",
                        valueText = "%.2f".format(params.topP),
                        value = params.topP,
                        range = 0.05f..1f,
                        onValueChange = { onUpdateParams(params.copy(topP = it)) }
                    )
                    ParameterSliderRow(
                        label = "Top-K",
                        valueText = "${params.topK}",
                        value = params.topK.toFloat(),
                        range = 1f..100f,
                        onValueChange = { onUpdateParams(params.copy(topK = it.roundToInt())) }
                    )
                    ParameterSliderRow(
                        label = "Max Output Tokens",
                        valueText = "${params.maxOutputTokens}",
                        value = params.maxOutputTokens.toFloat(),
                        range = 256f..32768f,
                        onValueChange = {
                            val snapped = ((it / 256f).roundToInt() * 256).coerceIn(256, 32768)
                            onUpdateParams(params.copy(maxOutputTokens = snapped))
                        }
                    )
                    ParameterSliderRow(
                        label = "Frequency Penalty",
                        valueText = "%.2f".format(params.frequencyPenalty),
                        value = params.frequencyPenalty,
                        range = -2f..2f,
                        onValueChange = { onUpdateParams(params.copy(frequencyPenalty = it)) }
                    )
                    ParameterSliderRow(
                        label = "Presence Penalty",
                        valueText = "%.2f".format(params.presencePenalty),
                        value = params.presencePenalty,
                        range = -2f..2f,
                        onValueChange = { onUpdateParams(params.copy(presencePenalty = it)) }
                    )

                    OutlinedTextField(
                        value = stopSeqDraft,
                        onValueChange = {
                            stopSeqDraft = it
                            onUpdateParams(params.copy(stopSequences = it))
                        },
                        label = { Text("Stop Sequences (comma-separated, max 5)") },
                        placeholder = { Text("e.g. END_JSON, ###", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = ObsidianSurface,
                            unfocusedContainerColor = ObsidianSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) TextPrimary else TextMuted
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun ParameterSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = EmeraldOk)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ElectricViolet,
                activeTrackColor = ElectricViolet,
                inactiveTrackColor = ObsidianBorder
            )
        )
    }
}
