package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LabCard
import com.example.ui.components.LabSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val cards = listOf(
        Triple(
            "Gemini 4 & Auto-Sync Architecture",
            "GenDev AI difokuskan pada ekosistem Google Gemini (Gemini 4 Hydrogen, Gemini 4 Neon, Gemini 4 Argon, Gemini 3.8 Flash) dengan pembaruan katalog model otomatis OTA, streaming latensi rendah, dan input multimodal.",
            BrightViolet
        ),
        Triple(
            "Zero Artificial Restrictions",
            "Tidak ada sistem kredit, paywall internal, batas pesan harian, filter kata buatan klien, atau system prompt pembatas tersembunyi. Aplikasi menghormati kebijakan keselamatan & kuota penyedia API secara transparan.",
            NeonCyan
        ),
        Triple(
            "Security & Encrypted Storage",
            "BYOK Gemini API Key, HTTPS Proxy URL, dan GitHub Personal Access Token dienkripsi memakai Android Keystore (AES-256-GCM) dan tidak pernah dicatat di log. Riwayat sesi & katalog model disimpan lokal di Room Database.",
            EmeraldOk
        ),
        Triple(
            "Automated GitHub APK Release CI/CD",
            "Menu GitHub Build Studio menyusun proyek Android Kotlin/Compose lengkap beserta workflow GitHub Actions (JDK 21 + Gradle 9.3.1) yang otomatis membangun Debug & Release APK lalu merilisnya ke GitHub Releases tanpa PC.",
            ElectricViolet
        )
    )

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
            Text(
                text = "About GenDev AI",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.65f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Identity Hero Card
            item {
                LabCard(borderColor = ElectricViolet.copy(alpha = 0.55f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.gendev_app_icon_1795750213592),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "GenDev AI",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Version 1.0 · Native Kotlin & Compose",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(text = "Firebase AI Logic", accent = BrightViolet)
                        StatusPill(text = "Room v2", accent = NeonCyan)
                        StatusPill(text = "AES-256", accent = EmeraldOk)
                    }
                }
            }

            item {
                LabSectionHeader(title = "Platform Capabilities")
            }

            cards.forEach { (title, body, accent) ->
                item {
                    LabCard {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = accent
                        )
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
