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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UsageRecordEntity
import com.example.data.preferences.AppSettings
import com.example.model.GeminiModelSpec
import com.example.ui.components.LabCard
import com.example.ui.components.LabSectionHeader
import com.example.ui.components.StatusPill
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
    availableModels: List<GeminiModelSpec>,
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

    val perModelUsage = remember(records, availableModels) {
        availableModels.map { spec ->
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Usage Telemetry",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Token usage & estimated API cost · Unlimited client cap",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (records.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Clear usage history",
                            tint = CoralError
                        )
                    }
                } else {
                    StatusPill(text = "UNLIMITED", accent = EmeraldOk)
                }
            }
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.65f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2x2 Telemetry KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TelemetryMetricCard(
                            label = "EST. COST (USD)",
                            value = "$${"%.4f".format(totalCost)}",
                            accent = EmeraldOk,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCard(
                            label = "LATENCY · OK",
                            value = "${avgLatency}ms · $successRate%",
                            accent = ElectricViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Engine & Quota Transparency Card
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Provider Quota & Status")
                    LabCard {
                        TelemetryStatusRow(label = "Active Engine", value = settings.engineMode.label, valueColor = NeonCyan)
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))
                        TelemetryStatusRow(label = "Active Model", value = settings.activeModelId, valueColor = BrightViolet)
                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.45f))
                        TelemetryStatusRow(label = "Client Limit", value = "UNLIMITED", valueColor = EmeraldOk)
                        Text(
                            text = "Kuota rate limit (RPM/TPM) mengikuti tier proyek Google Gemini / Firebase Anda secara transparan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Per-Model Token Breakdown Chart
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionHeader(title = "Tokens by Gemini Model")
                    LabCard {
                        val barColors = listOf(NeonCyan, ElectricViolet, EmeraldOk, BrightViolet)
                        perModelUsage.forEachIndexed { idx, (name, tokens, cost) ->
                            val ratio = (tokens.toFloat() / maxModelTokens.toFloat()).coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "${formatNumber(tokens)} tok · $${"%.4f".format(cost)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = barColors[idx % barColors.size],
                                        maxLines = 1
                                    )
                                }
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                ) {
                                    drawRoundRect(
                                        color = ObsidianElevated,
                                        size = size,
                                        cornerRadius = CornerRadius(12f, 12f)
                                    )
                                    val barWidth = if (tokens == 0) 0f else (size.width * ratio).coerceAtLeast(10f)
                                    if (barWidth > 0f) {
                                        drawRoundRect(
                                            color = barColors[idx % barColors.size],
                                            topLeft = Offset.Zero,
                                            size = Size(width = barWidth, height = size.height),
                                            cornerRadius = CornerRadius(12f, 12f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent API Calls Header
            item {
                LabSectionHeader(
                    title = "Recent API Call Log",
                    trailing = {
                        StatusPill(text = "${records.size} Calls", accent = NeonCyan)
                    }
                )
            }

            if (records.isEmpty()) {
                item {
                    LabCard {
                        Text(
                            text = "Belum ada riwayat permintaan Gemini. Mulai percakapan di tab Chat untuk memantau token dan latensi secara real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(records.take(30), key = { it.id }) { rec ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = rec.modelId,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    StatusPill(
                                        text = if (rec.success) "OK" else "ERR",
                                        accent = if (rec.success) EmeraldOk else CoralError
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${formatter.format(Date(rec.timestamp))} · ${rec.latencyMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                                rec.errorDetail?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CoralError,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${rec.inputTokens}↑ / ${rec.outputTokens}↓",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$${"%.5f".format(rec.estimatedCostUsd)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldOk,
                                    maxLines = 1
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

@Composable
private fun TelemetryStatusRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatNumber(n: Int): String = "%,d".format(n)
