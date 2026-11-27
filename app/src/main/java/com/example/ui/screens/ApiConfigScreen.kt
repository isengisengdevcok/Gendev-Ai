package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppSettings
import com.example.model.ConnectionTestState
import com.example.model.EngineMode
import com.example.model.GeminiModelSpec
import com.example.model.GeminiTier
import com.example.model.ModelSyncState
import com.example.ui.components.LabCard
import com.example.ui.components.LabSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.labTextFieldColors
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApiConfigScreen(
    settings: AppSettings,
    availableModels: List<GeminiModelSpec>,
    connectionTestState: ConnectionTestState,
    modelSyncState: ModelSyncState,
    maskedApiKey: String,
    hasByokKey: Boolean,
    storedProxyUrl: String,
    onSelectEngineMode: (EngineMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveByok: (apiKey: String, proxyUrl: String) -> Result<Unit>,
    onClearByokKey: () -> Unit,
    onTestConnection: () -> Unit,
    onToggleAutoSyncModels: (Boolean) -> Unit,
    onSyncModelsNow: () -> Unit,
    onAddCustomModel: (modelId: String, displayName: String) -> Unit,
    onDeleteDynamicModel: (modelId: String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var proxyInput by remember(storedProxyUrl) { mutableStateOf(storedProxyUrl) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var customModelId by remember { mutableStateOf("") }
    var customModelName by remember { mutableStateOf("") }

    val timeFormatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    val fieldColors = labTextFieldColors()

    val activeModelName = remember(availableModels, settings.activeModelId) {
        availableModels.firstOrNull { it.id == settings.activeModelId }?.displayName
            ?: settings.activeModelId
    }

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
                        text = "Gemini Engine",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Model catalog, auto-sync OTA & encrypted vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(text = activeModelName, accent = NeonCyan)
            }
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.65f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Execution Mode Selector (Compact 2-Option Segmented Surface)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Execution Provider")
                    LabCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianSurface)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            EngineMode.entries.forEach { mode ->
                                val selected = settings.engineMode == mode
                                val shortLabel = if (mode == EngineMode.FIREBASE_AI_LOGIC) {
                                    "Firebase AI"
                                } else {
                                    "BYOK API Key"
                                }
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) VioletContainer else Color.Transparent)
                                        .clickable { onSelectEngineMode(mode) }
                                        .padding(vertical = 11.dp, horizontal = 8.dp)
                                        .testTag("engine_mode_${mode.name}"),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (mode == EngineMode.FIREBASE_AI_LOGIC) {
                                            Icons.Outlined.Shield
                                        } else {
                                            Icons.Outlined.Key
                                        },
                                        contentDescription = null,
                                        tint = if (selected) NeonCyan else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) BrightViolet else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = settings.engineMode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 2. Encrypted BYOK Vault (Only shown when BYOK mode active)
            if (settings.engineMode == EngineMode.BYOK_GEMINI_API) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabSectionHeader(
                            title = "Encrypted BYOK Vault",
                            trailing = {
                                StatusPill(
                                    text = if (hasByokKey) "AES-256 Active" else "No Key",
                                    accent = if (hasByokKey) EmeraldOk else AmberWarn
                                )
                            }
                        )
                        LabCard(borderColor = ElectricViolet.copy(alpha = 0.55f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (hasByokKey) EmeraldOk else AmberWarn,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (hasByokKey) "Key: $maskedApiKey" else "Simpan Gemini API Key Anda di Android Keystore.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (hasByokKey) EmeraldOk else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    validationError = null
                                },
                                label = { Text("Gemini Paid / Pro API Key") },
                                placeholder = { Text("AIza...", color = TextMuted) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("byok_api_key_input")
                            )

                            OutlinedTextField(
                                value = proxyInput,
                                onValueChange = {
                                    proxyInput = it
                                    validationError = null
                                },
                                label = { Text("Optional HTTPS Proxy URL") },
                                placeholder = { Text("https://proxy.yourdomain.com", color = TextMuted) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("byok_proxy_url_input")
                            )

                            validationError?.let { err ->
                                Text(text = err, style = MaterialTheme.typography.bodySmall, color = CoralError)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val res = onSaveByok(apiKeyInput, proxyInput)
                                        if (res.isSuccess) {
                                            apiKeyInput = ""
                                            validationError = null
                                        } else {
                                            validationError = res.exceptionOrNull()?.localizedMessage
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Encrypt & Save",
                                        color = ObsidianVoid,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                if (hasByokKey) {
                                    OutlinedButton(
                                        onClick = onClearByokKey,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Clear", color = CoralError, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Connection Diagnostics Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Connection Diagnostics")
                    LabCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NetworkCheck,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = settings.activeModelId,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = settings.engineMode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Button(
                                onClick = onTestConnection,
                                enabled = connectionTestState !is ConnectionTestState.Testing,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("test_connection_button")
                            ) {
                                if (connectionTestState is ConnectionTestState.Testing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = ObsidianVoid
                                    )
                                } else {
                                    Text(
                                        text = "Test Ping",
                                        color = ObsidianVoid,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        when (connectionTestState) {
                            is ConnectionTestState.Idle -> Unit
                            is ConnectionTestState.Testing -> {
                                Text(
                                    text = "Mengirim attestation & ping ke Gemini...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                            }
                            is ConnectionTestState.Success -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldOk.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, EmeraldOk.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✓ ${connectionTestState.details} (${connectionTestState.latencyMs} ms)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldOk,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            is ConnectionTestState.Failure -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ErrorContainerDark,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Error: ${connectionTestState.error}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Over-The-Air Auto-Update Model Registry
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(
                        title = "Auto-Update Model Registry",
                        trailing = {
                            Text(
                                text = if (settings.lastModelSyncEpochMs > 0L) {
                                    timeFormatter.format(Date(settings.lastModelSyncEpochMs))
                                } else {
                                    "OTA Ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    )
                    LabCard(borderColor = BrightViolet.copy(alpha = 0.45f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    tint = BrightViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Auto-Sync Model Baru",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Update katalog Gemini 4/5 otomatis tanpa install ulang APK",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = settings.autoSyncModels,
                                onCheckedChange = onToggleAutoSyncModels
                            )
                        }

                        when (modelSyncState) {
                            is ModelSyncState.Success -> {
                                Text(
                                    text = "✓ ${modelSyncState.totalModels} model aktif (${modelSyncState.source}, +${modelSyncState.newlyAdded} baru)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldOk
                                )
                            }
                            is ModelSyncState.Failure -> {
                                Text(
                                    text = "Sync notice: ${modelSyncState.error}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberWarn
                                )
                            }
                            else -> Unit
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSyncModelsNow,
                                enabled = modelSyncState !is ModelSyncState.Syncing,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("sync_models_now_button")
                            ) {
                                if (modelSyncState is ModelSyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = ObsidianVoid
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", color = ObsidianVoid, maxLines = 1)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        tint = ObsidianVoid,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sync Models",
                                        color = ObsidianVoid,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showAddModelDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ObsidianBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("add_custom_model_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom ID", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 5. Gemini Model Catalog List
            item {
                LabSectionHeader(
                    title = "Gemini Models",
                    trailing = {
                        StatusPill(text = "${availableModels.size} Ready", accent = BrightViolet)
                    }
                )
            }

            items(availableModels, key = { it.id }) { model ->
                val selected = settings.activeModelId == model.id
                val tierColor = when (model.tier) {
                    GeminiTier.HYDROGEN -> NeonCyan
                    GeminiTier.PRO -> BrightViolet
                    GeminiTier.FLASH -> ElectricViolet
                    GeminiTier.LITE -> EmeraldOk
                    GeminiTier.DYNAMIC -> PlasmaMagenta
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) VioletContainer.copy(alpha = 0.55f) else ObsidianCard,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ElectricViolet else ObsidianBorder.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectModel(model.id) }
                        .testTag("select_model_${model.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.Bolt,
                                    contentDescription = null,
                                    tint = if (selected) EmeraldOk else tierColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatusPill(text = model.tier.label, accent = tierColor)
                                if (model.isDynamic) {
                                    IconButton(
                                        onClick = { onDeleteDynamicModel(model.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Remove dynamic model",
                                            tint = CoralError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = model.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = "${model.contextWindowTokens / 1024}K ctx · ${model.maxOutputTokens / 1024}K out",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (model.supportsLegacySamplingParams) {
                                    "Custom Temp / Top-P"
                                } else {
                                    "Native Thinking · Auto-Sampling"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$${model.inputPricePerMillionUsd} / $${model.outputPricePerMillionUsd} per 1M",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAddModelDialog) {
        AlertDialog(
            onDismissRequest = { showAddModelDialog = false },
            containerColor = ObsidianCard,
            title = { Text("Add Custom Gemini Model ID", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tambahkan ID model Gemini baru secara instan tanpa update APK (contoh: gemini-4-neon atau rilis preview).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = customModelId,
                        onValueChange = { customModelId = it },
                        label = { Text("Model ID") },
                        placeholder = { Text("gemini-4-neon", color = TextMuted) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customModelName,
                        onValueChange = { customModelName = it },
                        label = { Text("Display Name (opsional)") },
                        placeholder = { Text("Gemini 4 Neon", color = TextMuted) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomModel(customModelId, customModelName)
                        customModelId = ""
                        customModelName = ""
                        showAddModelDialog = false
                    },
                    enabled = customModelId.isNotBlank()
                ) {
                    Text("Add & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
