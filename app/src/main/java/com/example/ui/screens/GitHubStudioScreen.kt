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
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.theme.ObsidianCard
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

    var appIdea by remember { mutableStateOf("") }
    var projectName by remember(settings.githubProjectName) { mutableStateOf(settings.githubProjectName) }
    var packageName by remember(settings.githubPackageName) { mutableStateOf(settings.githubPackageName) }
    var githubToken by remember { mutableStateOf("") }
    var githubOwner by remember(initialOwner) { mutableStateOf(initialOwner) }
    var githubRepo by remember(initialRepo) { mutableStateOf(initialRepo.ifBlank { "gendev-android-app" }) }
    var githubBranch by remember(settings.githubBranch) { mutableStateOf(settings.githubBranch) }
    var isPrivateRepo by remember(settings.githubPrivateRepo) { mutableStateOf(settings.githubPrivateRepo) }
    var commitMessage by remember { mutableStateOf("feat: sync Android Compose source & APK release workflow via GenDev AI") }

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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ElectricViolet,
        unfocusedBorderColor = ObsidianBorder,
        focusedContainerColor = ObsidianSurface,
        unfocusedContainerColor = ObsidianSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    fun openExternalUrl(url: String) {
        if (url.startsWith("https://")) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "GitHub Android Build Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Generate source Android Jetpack Compose dengan Gemini, sinkronisasi ke GitHub, dan build otomatis Debug + Release APK ke menu GitHub Releases tanpa PC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 1. AI Android Project Generator Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = BrightViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Gemini Android Project Generator",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            label = { Text("App Name") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Package ID") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    OutlinedTextField(
                        value = appIdea,
                        onValueChange = { appIdea = it },
                        label = { Text("Deskripsikan fitur aplikasi Android yang ingin dibangun") },
                        placeholder = {
                            Text(
                                "Contoh: Buat aplikasi catatan riset AI dengan daftar kategori, pencarian, dan dark theme neon...",
                                color = TextMuted
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_app_idea_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onGenerateProject(appIdea, projectName, packageName) },
                            enabled = appIdea.isNotBlank() && !uiState.isGeneratingProject,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("generate_android_project_button")
                        ) {
                            if (uiState.isGeneratingProject) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianVoid
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating...", color = ObsidianVoid)
                            } else {
                                Text("Generate Source (Gemini)", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { onResetTemplate(projectName, packageName) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Template")
                        }
                    }

                    uiState.generationSummary?.let { summary ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianElevated
                        ) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldOk,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Project Source Tree & Workflow Inspector
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. Source Tree & CI/CD (${projectFiles.size} Files)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }

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
                                color = when {
                                    isSelected -> VioletContainer
                                    isWorkflow -> ObsidianElevated
                                    else -> ObsidianSurface
                                },
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    if (selectedFile != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedFile.path,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    onSaveFileContent(selectedFile.path, editorDraft, selectedFile.language)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = "Save file edits",
                                    tint = EmeraldOk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = EmeraldOk)
                            }
                        }

                        OutlinedTextField(
                            value = editorDraft,
                            onValueChange = { editorDraft = it },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            ),
                            minLines = 8,
                            maxLines = 14,
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

        // 3. GitHub Repository Sync & Push to Build APK Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Push to GitHub & Auto-Build APK",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = EmeraldOk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasStoredToken) {
                                    "GitHub PAT terenkripsi (Android Keystore): $maskedToken"
                                } else {
                                    "Masukkan GitHub PAT (Classic scope: repo + workflow). Disimpan terenkripsi AES-256-GCM."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasStoredToken) EmeraldOk else TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = githubToken,
                        onValueChange = { githubToken = it },
                        label = { Text(if (hasStoredToken) "Update GitHub PAT (opsional)" else "GitHub Personal Access Token (ghp_...)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_pat_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = githubOwner,
                            onValueChange = { githubOwner = it },
                            label = { Text("GitHub Username") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = githubRepo,
                            onValueChange = { githubRepo = it },
                            label = { Text("Repository Name") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f).testTag("github_repo_input")
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = githubBranch,
                            onValueChange = { githubBranch = it },
                            label = { Text("Branch") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Private Repo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = isPrivateRepo, onCheckedChange = { isPrivateRepo = it })
                        }
                    }

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = { Text("Commit Message") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onVerifyGitHub(githubToken) },
                            enabled = !uiState.isVerifyingGitHub,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            if (uiState.isVerifyingGitHub) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify PAT")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onSaveGitHubConfig(
                                    githubToken, githubOwner, githubRepo,
                                    projectName, packageName, githubBranch, isPrivateRepo
                                )
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Save Config")
                        }
                    }

                    uiState.verifiedUser?.let { user ->
                        Text(
                            text = "✓ Verified GitHub account: @${user.login}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldOk
                        )
                    }
                    uiState.githubVerificationError?.let { err ->
                        Text(text = err, style = MaterialTheme.typography.bodySmall, color = CoralError)
                    }

                    // Primary Push to GitHub & Build APK Button
                    val isSyncing = uiState.syncState is GitHubSyncState.InProgress
                    Button(
                        onClick = {
                            onPushToGitHub(
                                githubToken,
                                githubOwner,
                                githubRepo,
                                githubBranch,
                                isPrivateRepo,
                                commitMessage
                            )
                        },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = ObsidianVoid
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("push_to_github_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = ObsidianVoid
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = (uiState.syncState as GitHubSyncState.InProgress).step,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Push to GitHub & Build APK Release",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Sync Result Feedback
                    AnimatedVisibility(visible = uiState.syncState is GitHubSyncState.Success) {
                        val success = uiState.syncState as? GitHubSyncState.Success
                        if (success != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DeepViolet.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, EmeraldOk.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = EmeraldOk)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Synced Commit #${success.commitSha} · Build APK Aktif!",
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
        }

        // 4. Live GitHub Actions Workflow & Releases Monitor
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. Live Actions & GitHub Releases APK",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = { onRefreshBuilds(githubToken, githubOwner, githubRepo) },
                            enabled = !uiState.isRefreshingBuilds
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh build runs", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isRefreshingBuilds) "Syncing..." else "Refresh", color = NeonCyan)
                        }
                    }

                    if (uiState.workflowRuns.isEmpty() && uiState.latestRelease == null) {
                        Text(
                            text = "Setelah menekan 'Push to GitHub & Build APK Release', tekan Refresh untuk memantau progress build GitHub Actions dan mengunduh file APK langsung dari GitHub Releases.",
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
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(run.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(
                                                text = "Status: ${run.status.uppercase()} ${run.conclusion?.let { "· ${it.uppercase()}" }.orEmpty()}",
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = VioletContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Latest Release: ${release.name} (${release.tagName})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BrightViolet
                                    )
                                    release.assets.forEach { asset ->
                                        Button(
                                            onClick = { openExternalUrl(asset.downloadUrl) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = ObsidianVoid)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Download ${asset.name} (${"%.1f".format(asset.sizeBytes / 1_048_576.0)} MB)",
                                                color = ObsidianVoid,
                                                fontWeight = FontWeight.Bold
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
