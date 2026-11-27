package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
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
            "Gemini-Exclusive Research Architecture",
            "GenDev AI difokuskan secara mendalam pada ekosistem model Google Gemini (Gemini 3.8 Flash, Gemini 4 Argon Pro, dan Gemini 3.5 Flash-Lite) dengan streaming latensi rendah, input multimodal gambar/dokumen, serta kontrol penuh parameter sampling.",
            BrightViolet
        ),
        Triple(
            "Zero Artificial Restrictions",
            "Tidak ada sistem kredit, paywall internal, batas jumlah pesan harian, filter kata buatan klien, atau system prompt pembatas tersembunyi. Aplikasi menghormati kebijakan keselamatan & kuota penyedia API secara transparan.",
            NeonCyan
        ),
        Triple(
            "Security & Encrypted Storage",
            "BYOK Gemini API Key, HTTPS Proxy URL, dan GitHub Personal Access Token dienkripsi menggunakan Android Keystore (AES-256-GCM) dan tidak pernah dicatat dalam log. Riwayat percakapan disimpan lokal secara efisien menggunakan Room Database.",
            EmeraldOk
        ),
        Triple(
            "Automated GitHub APK Release CI/CD",
            "Menu GitHub Build Studio menyusun proyek Android Kotlin/Compose lengkap beserta workflow GitHub Actions (JDK 17 + Gradle 8.11.1) yang otomatis membangun Debug APK & Release APK dan merilisnya langsung ke menu GitHub Releases tanpa PC.",
            ElectricViolet
        )
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
                Text("About GenDev AI", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gendev_app_icon_1795750213592),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("GenDev AI", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Version 1.0 · Native Kotlin & Compose", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                        Text("Room · Keystore AES-256 · Firebase AI Logic", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        cards.forEach { (title, body, accent) ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
                        Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}
