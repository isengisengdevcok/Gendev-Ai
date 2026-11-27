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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppSettings
import com.example.model.AceSettings
import com.example.model.GenerationParameters
import com.example.ui.components.LabCard
import com.example.ui.components.LabSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.labTextFieldColors
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
    val fieldColors = labTextFieldColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid)))
    ) {
        // Compact Sticky Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Lab & ACE Control",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "System prompt, ACE recovery & Gemini parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    text = if (ace.enabled) "ACE Active" else "ACE Off",
                    accent = if (ace.enabled) EmeraldOk else TextMuted
                )
            }
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.65f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Clean Grouped Navigation Menu (Replaces cramped 2-column cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Prompt & Info")
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            SettingsNavRow(
                                icon = Icons.Outlined.Psychology,
                                tint = BrightViolet,
                                title = "System Prompt Editor",
                                subtitle = "Edit custom instructions & Room presets",
                                testTag = "open_system_prompt_button",
                                onClick = onOpenSystemPromptEditor
                            )
                            HorizontalDivider(
                                color = ObsidianBorder.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            SettingsNavRow(
                                icon = Icons.Outlined.Info,
                                tint = NeonCyan,
                                title = "About GenDev AI",
                                subtitle = "Architecture, encryption & GitHub APK CI/CD",
                                testTag = "open_about_button",
                                onClick = onOpenAbout
                            )
                        }
                    }
                }
            }

            // 2. ACE — Adaptive Conversation Engine
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "ACE — Adaptive Conversation Engine")
                    LabCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continuity & Smart Recovery",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }

                        AceToggleRow(
                            title = "Enable ACE Engine",
                            subtitle = "Active diagnostics, error classification & continuity guard",
                            checked = ace.enabled,
                            onCheckedChange = { onUpdateAce(ace.copy(enabled = it)) }
                        )
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))

                        AceToggleRow(
                            title = "Auto Smart Rephrase",
                            subtitle = "Clarify ambiguous prompts without altering user intent",
                            checked = ace.autoRephrase,
                            enabled = ace.enabled,
                            onCheckedChange = { onUpdateAce(ace.copy(autoRephrase = it)) }
                        )
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))

                        AceToggleRow(
                            title = "Require Manual Approval",
                            subtitle = "Preview Original vs Improved prompt before sending",
                            checked = ace.requireManualApproval,
                            enabled = ace.enabled && ace.autoRephrase,
                            onCheckedChange = { onUpdateAce(ace.copy(requireManualApproval = it)) }
                        )
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))

                        AceToggleRow(
                            title = "Auto Network & 429 Recovery",
                            subtitle = "Retry transient network & rate-limit errors with backoff",
                            checked = ace.autoRecovery,
                            enabled = ace.enabled,
                            onCheckedChange = { onUpdateAce(ace.copy(autoRecovery = it)) }
                        )
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))

                        AceToggleRow(
                            title = "Auto Context Compression",
                            subtitle = "Summarize older turns near token cap while keeping key facts",
                            checked = ace.autoCompressContext,
                            enabled = ace.enabled,
                            onCheckedChange = { onUpdateAce(ace.copy(autoCompressContext = it)) }
                        )
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))

                        ParameterSliderRow(
                            label = "Compression Threshold",
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

            // 3. Gemini Sampling & Token Parameters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(
                        title = "Gemini Generation Parameters",
                        trailing = {
                            TextButton(
                                onClick = { onUpdateParams(GenerationParameters()) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Reset Defaults", color = BrightViolet)
                            }
                        }
                    )
                    LabCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = BrightViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Gemini 4 menggunakan Native Auto-Sampling otomatis; slider di bawah aktif untuk seri Gemini 3.x & Max Tokens untuk semua model.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))

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
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
