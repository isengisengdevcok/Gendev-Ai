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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.local.PromptPresetEntity
import com.example.data.preferences.AppSettings
import com.example.model.ChatMode
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ElectricViolet,
        unfocusedBorderColor = ObsidianBorder,
        focusedContainerColor = ObsidianSurface,
        unfocusedContainerColor = ObsidianSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("System Prompt Editor", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text(
                        "100% transparan — tidak ada instruksi pembatas tersembunyi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
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
                        border = BorderStroke(1.dp, if (selected) ElectricViolet else ObsidianBorder),
                        modifier = Modifier.clickable {
                            onSelectMode(mode)
                            if (mode != ChatMode.CUSTOM) {
                                promptDraft = mode.defaultSystemPrompt
                            }
                        }
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) BrightViolet else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = promptDraft,
                        onValueChange = { promptDraft = it },
                        label = { Text("Active System Instruction (leave empty for raw unguided Gemini)") },
                        minLines = 6,
                        maxLines = 12,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onApplyPrompt(promptDraft) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Apply Prompt", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                promptDraft = ""
                                onApplyPrompt("")
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Clear (Raw)")
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Save Current Prompt as Preset", style = MaterialTheme.typography.titleSmall, color = NeonCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            label = { Text("Preset Name") },
                            placeholder = { Text("e.g. Kotlin Compose Architect", color = TextMuted) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onSavePreset(presetName, settings.activeChatMode, promptDraft)
                                presetName = ""
                            },
                            enabled = presetName.isNotBlank(),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        if (presets.isNotEmpty()) {
            item {
                Text("Saved Room Presets (${presets.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(presets, key = { it.id }) { preset ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall, color = BrightViolet)
                            Text(
                                text = preset.systemPrompt.take(120),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = { onDeletePreset(preset.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete preset", tint = CoralError)
                        }
                    }
                }
            }
        }
    }
}
