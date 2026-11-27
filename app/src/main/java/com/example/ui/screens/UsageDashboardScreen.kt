package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.UsageRecordEntity
import com.example.data.preferences.AppSettings
import com.example.model.GeminiModelCatalog
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsageDashboardScreen(
    settings: AppSettings,
    records: List<UsageRecordEntity>,
    onClearHistory: () -> Unit
) {
    val totalInput = remember(records) { records.sumOf { it.inputTokens } }
    val totalOutput = remember(records) { records.sumOf { it.outputTokens } }
    val totalCost = remember(records) { records.sumOf { it.estimatedCostUsd } }
    val avgLatency = remember(records) {
        if (records.isEmpty()) 0L else records.map { it.latencyMs }.average().toLong()
    }
    val successRate = remember(records) {
        if (records.isEmpty()) 100 else (records.count { it.success } * 100) / records.size
    }

    val perModelUsage = remember(records) {
        GeminiModelCatalog.models.map { spec ->
            val modelRecords = records.filter { it.modelId == spec.id }
            val tokens = modelRecords.sumOf { it.inputTokens + it.outputTokens }
            val cost = modelRecords.sumOf { it.estimatedCostUsd }
            Triple(spec.displayName, tokens, cost)
        }
    }
    val maxModelTokens = remember(perModelUsage) {
        perModelUsage.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    }

    val formatter = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Usage & Cost Dashboard", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        "Transparansi token input/output & estimasi biaya API Gemini. Tidak ada limit internal aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (records.isNotEmpty()) {
                    OutlinedButton(onClick = onClearHistory) {
                        Text("Clear")
                    }
                }
            }
        }

        // Metric Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryMetricCard(
                    label = "INPUT TOKENS",
                    value = formatNumber(totalInput),
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    label = "OUTPUT TOKENS",
                    value = formatNumber(totalOutput),
                    accent = BrightViolet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryMetricCard(
                    label = "EST. COST (USD)",
                    value = "$${"%.4f".format(totalCost)}",
                    accent = EmeraldOk,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    label = "AVG LATENCY · OK",
                    value = "${avgLatency}ms · $successRate%",
                    accent = ElectricViolet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Engine & Quota Transparency Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Provider Quota & Engine Status", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "Active Engine: ${settings.engineMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                    Text(
                        text = "Active Model: ${settings.activeModelId} · Client Message Cap: UNLIMITED",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldOk
                    )
                    Text(
                        text = "Kuota rate limit (RPM/TPM) mengikuti tier akun Google Gemini / Firebase proyek Anda secara langsung tanpa pemotongan sisi klien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Per-Model Token Breakdown Chart (Compose Canvas)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Token Distribution per Gemini Model", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    val barColors = listOf(NeonCyan, ElectricViolet, EmeraldOk)
                    perModelUsage.forEachIndexed { idx, (name, tokens, cost) ->
                        val ratio = (tokens.toFloat() / maxModelTokens.toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Text(
                                    "${formatNumber(tokens)} tok · $${"%.4f".format(cost)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = barColors[idx % barColors.size]
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            ) {
                                drawRoundRect(
                                    color = ObsidianElevated,
                                    size = size,
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                drawRoundRect(
                                    color = barColors[idx % barColors.size],
                                    topLeft = Offset.Zero,
                                    size = Size(width = (size.width * ratio).coerceAtLeast(10f), height = size.height),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Recent API Call Log (${records.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }

        if (records.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada riwayat permintaan Gemini. Mulai percakapan di tab Chat untuk memantau token dan latensi secara real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(records.take(30), key = { it.id }) { rec ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rec.modelId} · ${if (rec.success) "OK" else "ERR"}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (rec.success) TextPrimary else CoralError
                            )
                            Text(
                                text = "${formatter.format(Date(rec.timestamp))} · ${rec.latencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            rec.errorDetail?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = CoralError)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${rec.inputTokens}↑ / ${rec.outputTokens}↓",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                            Text(
                                text = "$${"%.5f".format(rec.estimatedCostUsd)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

private fun formatNumber(n: Int): String = "%,d".format(n)
