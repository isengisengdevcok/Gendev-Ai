package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectFileEntity
import com.example.data.preferences.AppSettings
import com.example.model.GitHubSyncState
import com.example.ui.components.LabCard
import com.example.ui.components.LabSectionHeader
import com.example.ui.components.SegmentedTabBar
import com.example.ui.components.StatusPill
import com.example.ui.components.labTextFieldColors
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CodeSurface
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import com.example.viewmodel.StudioUiState

@Composable
fun GitHubStudioScreen(
    uiState: StudioUiState,
    settings: AppSettings,
    projectFiles: List<ProjectFileEntity>,
    initialOwner: String,
    initialRepo: String,
    hasStoredToken: Boolean,
    maskedToken: String,
    onGenerateProject: (idea: String, appName: String, packageName: String) -> Unit,
    onResetTemplate: (appName: String, packageName: String) -> Unit,
    onSelectFile: (String) -> Unit,
    onSaveFileContent: (path: String, content: String, language: String) -> Unit,
    onSaveGitHubConfig: (
        token: String,
        owner: String,
        repo: String,
        appName: String,
        packageName: String,
        branch: String,
        isPrivate: Boolean
    ) -> Unit,
    onVerifyGitHub: (token: String) -> Unit,
    onPushToGitHub: (
        token: String,
        owner: String,
        repo: String,
        branch: String,
        isPrivate: Boolean,
        commitMessage: String
    ) -> Unit,
    onRefreshBuilds: (token: String, owner: String, repo: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var appIdea by remember { mutableStateOf("") }
    var projectName by remember(settings.githubProjectName) { mutableStateOf(settings.githubProjectName) }
    var packageName by remember(settings.githubPackageName) { mutableStateOf(settings.githubPackageName) }
    var githubToken by remember { mutableStateOf("") }
    var githubOwner by remember(initialOwner) { mutableStateOf(initialOwner) }
    var githubRepo by remember(initialRepo) { mutableStateOf(initialRepo.ifBlank { "gendev-android-app" }) }
    var githubBranch by remember(settings.githubBranch) { mutableStateOf(settings.githubBranch) }
    var isPrivateRepo by remember(settings.githubPrivateRepo) { mutableStateOf(settings.githubPrivateRepo) }
    var commitMessage by remember { mutableStateOf("feat: sync Android source & APK release workflow") }

    val selectedFile = remember(projectFiles, uiState.selectedFilePath) {
        projectFiles.firstOrNull { it.path == uiState.selectedFilePath }
            ?: projectFiles.firstOrNull { it.path.endsWith("MainActivity.kt") }
            ?: projectFiles.firstOrNull()
    }
    var editorDraft by remember(selectedFile?.path, selectedFile?.updatedAt) {
        mutableStateOf(selectedFile?.content.orEmpty())
    }

    LaunchedEffect(uiState.verifiedUser?.login) {
        val login = uiState.verifiedUser?.login
        if (!login.isNullOrBlank() && githubOwner.isBlank()) {
            githubOwner = login
        }
    }

    val fieldColors = labTextFieldColors()

    fun openExternalUrl(url: String) {
        if (url.startsWith("https://")) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid)))
    ) {
        // Compact Organized Header + 3-Step Segmented Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GitHub Build Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Generate Android → Edit → Push & Auto-Release APK",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(text = "${projectFiles.size} Files", accent = NeonCyan)
            }

            SegmentedTabBar(
                tabs = listOf("1. Generate", "2. Files", "3. Push & APK"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
        HorizontalDivider(thickness = 0.8.dp, color = ObsidianBorder.copy(alpha = 0.55f))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                // TAB 0: AI PROJECT GENERATOR
                0 -> {
                    item {
                        LabSectionHeader(title = "Android Project Identity")
                    }
                    item {
                        LabCard {
                            OutlinedTextField(
                                value = projectName,
                                onValueChange = { projectName = it },
                                label = { Text("App Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = packageName,
                                onValueChange = { packageName = it },
                                label = { Text("Android Package ID") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        LabSectionHeader(title = "Gemini App Blueprint")
                    }
                    item {
                        LabCard(borderColor = ElectricViolet.copy(alpha = 0.55f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = BrightViolet, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Model aktif: ${settings.activeModelId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                            }

                            OutlinedTextField(
                                value = appIdea,
                                onValueChange = { appIdea = it },
                                label = { Text("Deskripsikan aplikasi Android yang ingin dibuat") },
                                placeholder = {
                                    Text(
                                        "Contoh: Aplikasi catatan riset dengan kategori, pencarian cepat, dan dark theme neon...",
                                        color = TextMuted
                                    )
                                },
                                minLines = 4,
                                maxLines = 7,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_app_idea_input")
                            )

                            Button(
                                onClick = { onGenerateProject(appIdea, projectName, packageName) },
                                enabled = appIdea.isNotBlank() && !uiState.isGeneratingProject,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("generate_android_project_button")
                            ) {
                                if (uiState.isGeneratingProject) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianVoid)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating Source...", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Generate Android Source (Gemini)", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { onResetTemplate(projectName, packageName) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("Reset Template", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { selectedTab = 2 },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("Lanjut Push →", color = NeonCyan, maxLines = 1)
                                }
                            }

                            uiState.generationSummary?.let { summary ->
                                Surface(shape = RoundedCornerShape(12.dp), color = ObsidianElevated) {
                                    Text(
                                        text = "✓ $summary",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldOk,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 1: SOURCE TREE & WORKFLOW EDITOR
                1 -> {
                    item {
                        LabSectionHeader(
                            title = "Source Files & CI/CD Workflow",
                            trailing = {
                                if (selectedFile != null) {
                                    TextButton(
                                        onClick = {
                                            onSaveFileContent(selectedFile.path, editorDraft, selectedFile.language)
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Save, contentDescription = "Save file", tint = EmeraldOk, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save", color = EmeraldOk)
                                    }
                                }
                            }
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            projectFiles.forEach { file ->
                                val isSelected = file.path == selectedFile?.path
                                val shortName = file.path.substringAfterLast('/')
                                val isWorkflow = file.path.contains(".github/workflows")
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) VioletContainer else ObsidianSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isSelected -> ElectricViolet
                                            isWorkflow -> NeonCyan.copy(alpha = 0.6f)
                                            else -> ObsidianBorder
                                        }
                                    ),
                                    modifier = Modifier.clickable { onSelectFile(file.path) }
                                ) {
                                    Text(
                                        text = if (isWorkflow) "⚡ $shortName" else shortName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) BrightViolet else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedFile != null) {
                        item {
                            LabCard {
                                Text(
                                    text = selectedFile.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                OutlinedTextField(
                                    value = editorDraft,
                                    onValueChange = { editorDraft = it },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.5.sp,
                                        lineHeight = 17.sp
                                    ),
                                    minLines = 14,
                                    maxLines = 24,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedContainerColor = CodeSurface,
                                        unfocusedContainerColor = CodeSurface,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // TAB 2: PUSH TO GITHUB & LIVE APK RELEASES
                2 -> {
                    item {
                        LabSectionHeader(title = "Encrypted GitHub Repository Sync")
                    }
                    item {
                        LabCard(borderColor = NeonCyan.copy(alpha = 0.5f)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = ObsidianElevated) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = EmeraldOk, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (hasStoredToken) {
                                            "PAT terenkripsi Keystore: $maskedToken"
                                        } else {
                                            "Masukkan GitHub PAT (scope: repo + workflow). Dienkripsi AES-256-GCM."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasStoredToken) EmeraldOk else TextSecondary
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = githubToken,
                                onValueChange = { githubToken = it },
                                label = { Text(if (hasStoredToken) "Update GitHub PAT (opsional)" else "GitHub Personal Access Token") },
                                placeholder = { Text("ghp_...", color = TextMuted) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("github_pat_input")
                            )

                            OutlinedTextField(
                                value = githubOwner,
                                onValueChange = { githubOwner = it },
                                label = { Text("GitHub Username / Owner") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = githubRepo,
                                onValueChange = { githubRepo = it },
                                label = { Text("Repository Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().testTag("github_repo_input")
                            )

                            OutlinedTextField(
                                value = githubBranch,
                                onValueChange = { githubBranch = it },
                                label = { Text("Target Branch") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = commitMessage,
                                onValueChange = { commitMessage = it },
                                label = { Text("Commit Message") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Private Repository", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("Otomatis membuat repo jika belum ada", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Switch(checked = isPrivateRepo, onCheckedChange = { isPrivateRepo = it })
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { onVerifyGitHub(githubToken) },
                                    enabled = !uiState.isVerifyingGitHub,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    if (uiState.isVerifyingGitHub) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verify PAT", maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        onSaveGitHubConfig(
                                            githubToken, githubOwner, githubRepo,
                                            projectName, packageName, githubBranch, isPrivateRepo
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("Save Config", maxLines = 1)
                                }
                            }

                            uiState.verifiedUser?.let { user ->
                                Text("✓ Verified GitHub: @${user.login}", style = MaterialTheme.typography.labelMedium, color = EmeraldOk)
                            }
                            uiState.githubVerificationError?.let { err ->
                                Text(err, style = MaterialTheme.typography.bodySmall, color = CoralError)
                            }

                            val isSyncing = uiState.syncState is GitHubSyncState.InProgress
                            Button(
                                onClick = {
                                    onPushToGitHub(
                                        githubToken, githubOwner, githubRepo,
                                        githubBranch, isPrivateRepo, commitMessage
                                    )
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = ObsidianVoid),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("push_to_github_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianVoid)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = (uiState.syncState as GitHubSyncState.InProgress).step,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Push to GitHub & Build APK", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }

                            AnimatedVisibility(visible = uiState.syncState is GitHubSyncState.Success) {
                                val success = uiState.syncState as? GitHubSyncState.Success
                                if (success != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DeepViolet.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, EmeraldOk.copy(alpha = 0.7f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = EmeraldOk)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Commit #${success.commitSha} tersinkronisasi!",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = EmeraldOk
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { openExternalUrl(success.actionsUrl) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Actions")
                                                }
                                                Button(
                                                    onClick = { openExternalUrl("${success.repoUrl}/releases") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = ObsidianVoid, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Releases", color = ObsidianVoid)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = uiState.syncState is GitHubSyncState.Failure) {
                                val fail = uiState.syncState as? GitHubSyncState.Failure
                                if (fail != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ErrorContainerDark,
                                        border = BorderStroke(1.dp, CoralError)
                                    ) {
                                        Text(
                                            text = fail.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        LabSectionHeader(
                            title = "GitHub Actions & Releases APK",
                            trailing = {
                                TextButton(
                                    onClick = { onRefreshBuilds(githubToken, githubOwner, githubRepo) },
                                    enabled = !uiState.isRefreshingBuilds
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh builds", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (uiState.isRefreshingBuilds) "Syncing..." else "Refresh", color = NeonCyan)
                                }
                            }
                        )
                    }

                    item {
                        LabCard {
                            if (uiState.workflowRuns.isEmpty() && uiState.latestRelease == null) {
                                Text(
                                    text = "Belum ada riwayat build yang dimuat. Tekan Refresh setelah Push untuk memantau progress GitHub Actions & mengunduh file APK dari GitHub Releases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            } else {
                                uiState.workflowRuns.forEach { run ->
                                    val badgeColor = when (run.conclusion) {
                                        "success" -> EmeraldOk
                                        "failure", "cancelled" -> CoralError
                                        else -> AmberWarn
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ObsidianElevated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { openExternalUrl(run.htmlUrl) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(badgeColor))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(run.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(
                                                        text = "${run.status.uppercase()} ${run.conclusion?.let { "· ${it.uppercase()}" }.orEmpty()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = badgeColor
                                                    )
                                                }
                                            }
                                            Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                uiState.latestRelease?.let { release ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = VioletContainer.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.65f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Latest Release: ${release.name} (${release.tagName})",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = BrightViolet
                                            )
                                            release.assets.forEach { asset ->
                                                Button(
                                                    onClick = { openExternalUrl(asset.downloadUrl) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = ObsidianVoid)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Download ${asset.name} (${"%.1f".format(asset.sizeBytes / 1_048_576.0)} MB)",
                                                        color = ObsidianVoid,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
