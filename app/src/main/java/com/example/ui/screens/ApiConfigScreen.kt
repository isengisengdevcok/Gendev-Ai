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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppSettings
import com.example.model.ConnectionTestState
import com.example.model.EngineMode
import com.example.model.GeminiModelCatalog
import com.example.model.GeminiTier
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer

@Composable
fun ApiConfigScreen(
    settings: AppSettings,
    connectionTestState: ConnectionTestState,
    maskedApiKey: String,
    hasByokKey: Boolean,
    storedProxyUrl: String,
    onSelectEngineMode: (EngineMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveByok: (apiKey: String, proxyUrl: String) -> Result<Unit>,
    onClearByokKey: () -> Unit,
    onTestConnection: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var proxyInput by remember(storedProxyUrl) { mutableStateOf(storedProxyUrl) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Gemini Pro Engine & API Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Fokus eksklusif pada ekosistem model Google Gemini (Pro, Flash, Flash-Lite). Tanpa sistem kredit internal dan tanpa pembatasan percakapan buatan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Engine Mode Selector
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Execution Engine Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    EngineMode.entries.forEach { mode ->
                        val selected = settings.engineMode == mode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) VioletContainer else ObsidianSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) ElectricViolet else ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectEngineMode(mode) }
                                .testTag("engine_mode_${mode.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (mode == EngineMode.FIREBASE_AI_LOGIC) Icons.Outlined.Shield else Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = if (selected) NeonCyan else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) BrightViolet else TextPrimary
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = EmeraldOk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BYOK Encrypted Key & Secure HTTPS Proxy Section
        if (settings.engineMode == EngineMode.BYOK_GEMINI_API) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = EmeraldOk)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Encrypted Gemini BYOK Vault (Android Keystore)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (hasByokKey) "Active encrypted key: $maskedApiKey" else "Belum ada Gemini API Key tersimpan.",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasByokKey) EmeraldOk else AmberWarn
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                validationError = null
                            },
                            label = { Text("Google Gemini Paid / Pro API Key") },
                            placeholder = { Text("AIza...", color = TextMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("byok_api_key_input")
                        )

                        OutlinedTextField(
                            value = proxyInput,
                            onValueChange = {
                                proxyInput = it
                                validationError = null
                            },
                            label = { Text("Optional Secure HTTPS Backend Proxy URL") },
                            placeholder = { Text("https://proxy.yourdomain.com", color = TextMuted) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("byok_proxy_url_input")
                        )

                        validationError?.let { err ->
                            Text(text = err, style = MaterialTheme.typography.bodySmall, color = CoralError)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Encrypt & Save", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                            }
                            if (hasByokKey) {
                                OutlinedButton(
                                    onClick = onClearByokKey,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Clear Key", color = CoralError)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Test Connection Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NetworkCheck, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connection Diagnostics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Button(
                            onClick = onTestConnection,
                            enabled = connectionTestState !is ConnectionTestState.Testing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.testTag("test_connection_button")
                        ) {
                            if (connectionTestState is ConnectionTestState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianVoid
                                )
                            } else {
                                Text("Test Ping", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    when (connectionTestState) {
                        is ConnectionTestState.Idle -> {
                            Text(
                                text = "Uji koneksi langsung ke ${settings.activeModelId} melalui ${settings.engineMode.label}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        is ConnectionTestState.Testing -> {
                            Text(
                                text = "Sending attestation & ping to Gemini...",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                        is ConnectionTestState.Success -> {
                            Text(
                                text = "✓ ${connectionTestState.details} (${connectionTestState.latencyMs} ms)",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldOk
                            )
                        }
                        is ConnectionTestState.Failure -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ErrorContainerDark
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

        // Gemini Pro / Flash / Lite Model Selector
        item {
            Text(
                text = "Google Gemini Model Selection",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        items(GeminiModelCatalog.models, key = { it.id }) { model ->
            val selected = settings.activeModelId == model.id
            val tierColor = when (model.tier) {
                GeminiTier.PRO -> BrightViolet
                GeminiTier.FLASH -> NeonCyan
                GeminiTier.LITE -> EmeraldOk
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected) VioletContainer.copy(alpha = 0.65f) else ObsidianCard,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) ElectricViolet else ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectModel(model.id) }
                    .testTag("select_model_${model.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = tierColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, tierColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = model.tier.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tierColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = model.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Model ID: ${model.id} · Context: ${model.contextWindowTokens / 1024}K · Max Out: ${model.maxOutputTokens / 1024}K",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Text(
                        text = "Est. API Rate: $${model.inputPricePerMillionUsd}/1M in · $${model.outputPricePerMillionUsd}/1M out · Vision + Docs",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
