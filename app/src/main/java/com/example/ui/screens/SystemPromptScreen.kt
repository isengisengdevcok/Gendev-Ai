package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PromptPresetEntity
import com.example.data.preferences.AppSettings
import com.example.model.ChatMode
import com.example.ui.components.LabCard
import com.example.ui.components.LabSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.labTextFieldColors
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer

@Composable
fun SystemPromptScreen(
    settings: AppSettings,
    presets: List<PromptPresetEntity>,
    onBack: () -> Unit,
    onSelectMode: (ChatMode) -> Unit,
    onApplyPrompt: (String) -> Unit,
    onSavePreset: (name: String, mode: ChatMode, prompt: String) -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    BackHandler { onBack() }

    var promptDraft by remember(settings.customSystemPrompt) { mutableStateOf(settings.customSystemPrompt) }
    var presetName by remember { mutableStateOf("") }
    val fieldColors = labTextFieldColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid)))
    ) {
        // Consistent Sticky Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Prompt Editor",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "100% transparent · No hidden client restrictions",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.65f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Template Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Mode Templates")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatMode.entries.forEach { mode ->
                            val selected = settings.activeChatMode == mode
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) VioletContainer else ObsidianCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ElectricViolet else ObsidianBorder.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.clickable {
                                    onSelectMode(mode)
                                    if (mode != ChatMode.CUSTOM) {
                                        promptDraft = mode.defaultSystemPrompt
                                    }
                                }
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) BrightViolet else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Instruction Editor
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Active System Instruction")
                    LabCard {
                        OutlinedTextField(
                            value = promptDraft,
                            onValueChange = { promptDraft = it },
                            label = { Text("System Prompt (empty = raw unguided Gemini)") },
                            minLines = 6,
                            maxLines = 12,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onApplyPrompt(promptDraft) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Apply Prompt",
                                    color = ObsidianVoid,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    promptDraft = ""
                                    onApplyPrompt("")
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Clear", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Save New Preset Card
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Save as Reusable Preset")
                    LabCard {
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            label = { Text("Preset Name") },
                            placeholder = { Text("e.g. Kotlin Compose Architect", color = TextMuted) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSavePreset(presetName, settings.activeChatMode, promptDraft)
                                presetName = ""
                            },
                            enabled = presetName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Save Preset to Room",
                                color = ObsidianVoid,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Saved Presets List
            if (presets.isNotEmpty()) {
                item {
                    LabSectionHeader(
                        title = "Saved Room Presets",
                        trailing = { StatusPill(text = "${presets.size}", accent = BrightViolet) }
                    )
                }
                items(presets, key = { it.id }) { preset ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                promptDraft = preset.systemPrompt
                                onApplyPrompt(preset.systemPrompt)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrightViolet,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.systemPrompt.ifBlank { "(Raw unguided prompt)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeletePreset(preset.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete preset",
                                    tint = CoralError
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
