package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.GeminiEngine
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.preferences.AppSettings
import com.example.model.AceDiagnosticBanner
import com.example.model.AceRephraseProposal
import com.example.model.AttachmentItem
import com.example.model.AttachmentType
import com.example.model.ChatMode
import com.example.model.GeminiModelCatalog
import com.example.ui.components.MarkdownMessageView
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepViolet
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
import com.example.viewmodel.ChatUiState
import java.util.UUID

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    settings: AppSettings,
    activeSession: ChatSessionEntity?,
    messages: List<ChatMessageEntity>,
    onOpenDrawer: () -> Unit,
    onNewSession: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectMode: (ChatMode) -> Unit,
    onSendPrompt: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onAddAttachment: (AttachmentItem) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onApproveRephrase: (Boolean) -> Unit,
    onDismissRephrase: () -> Unit,
    onDismissDiagnostic: () -> Unit,
    onEditMessage: (ChatMessageEntity, String) -> Unit,
    onRegenerateMessage: (ChatMessageEntity) -> Unit,
    onCompressContext: () -> Unit,
    onSendCodeToStudio: (code: String, language: String) -> Unit,
    onNavigateToGitHubStudio: () -> Unit,
    onLocalNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var promptInput by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var editDraft by remember { mutableStateOf("") }

    val activeModel = remember(settings.activeModelId) {
        GeminiModelCatalog.findById(settings.activeModelId)
    }

    val estimatedContextTokens = remember(messages, activeSession) {
        val msgTokens = messages.sumOf { GeminiEngine.estimateTokens(it.content) }
        val memTokens = GeminiEngine.estimateTokens(activeSession?.compressedContextSummary.orEmpty())
        msgTokens + memTokens
    }
    val contextRatio = (estimatedContextTokens.toFloat() / activeModel.contextWindowTokens.toFloat())
        .coerceIn(0f, 1f)

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "image_${System.currentTimeMillis()}.jpg"
            onAddAttachment(
                AttachmentItem(
                    id = UUID.randomUUID().toString(),
                    type = AttachmentType.IMAGE,
                    displayName = name,
                    uriString = uri.toString(),
                    mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                )
            )
        }
    }

    // Zero-permission SAF Document Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "document.txt"
            val mime = context.contentResolver.getType(uri) ?: "text/plain"
            val textContent = readDocumentPreview(context, uri)
            onAddAttachment(
                AttachmentItem(
                    id = UUID.randomUUID().toString(),
                    type = AttachmentType.DOCUMENT,
                    displayName = name,
                    uriString = uri.toString(),
                    mimeType = mime,
                    extractedText = textContent
                )
            )
        }
    }

    LaunchedEffect(messages.size, uiState.streamingText.length) {
        val targetIndex = (messages.size + if (uiState.isGenerating) 1 else 0) - 1
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianSurface, ObsidianVoid)
                )
            )
            .imePadding()
    ) {
        // Futuristic Header Bar
        Surface(
            color = ObsidianSurface.copy(alpha = 0.96f),
            border = BorderStroke(0.5.dp, ObsidianBorder.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = stringResource(R.string.cd_open_drawer),
                                tint = TextPrimary
                            )
                        }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clickable { showModelMenu = true }
                                    .testTag("model_selector_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldOk)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = activeModel.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${activeModel.tier.label} · ${settings.engineMode.label.substringBefore(" ")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false },
                                containerColor = ObsidianCard
                            ) {
                                GeminiModelCatalog.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${model.displayName} (${model.tier.label})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (model.id == activeModel.id) BrightViolet else TextPrimary
                                                )
                                                Text(
                                                    text = model.tagline,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            showModelMenu = false
                                            onSelectModel(model.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateToGitHubStudio,
                            modifier = Modifier.testTag("quick_github_studio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = "Push to GitHub Build Studio",
                                tint = NeonCyan
                            )
                        }
                        IconButton(
                            onClick = onNewSession,
                            modifier = Modifier.testTag("new_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_new_chat),
                                tint = BrightViolet
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mode Switcher Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatMode.entries.forEach { mode ->
                        val selected = settings.activeChatMode == mode
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) VioletContainer else ObsidianCard,
                            border = BorderStroke(
                                1.dp,
                                if (selected) ElectricViolet else ObsidianBorder.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .clickable { onSelectMode(mode) }
                                .testTag("mode_chip_${mode.id}")
                        ) {
                            Text(
                                text = mode.shortBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) BrightViolet else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Transparent Context Window & ACE Status Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CTX ${formatCompactTokens(estimatedContextTokens)}/${formatCompactTokens(activeModel.contextWindowTokens)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { contextRatio.coerceAtLeast(0.02f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (contextRatio > 0.7f) AmberWarn else NeonCyan,
                            trackColor = ObsidianElevated
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianElevated,
                        modifier = Modifier
                            .clickable(enabled = !uiState.isCompressingContext && messages.size > 4) {
                                onCompressContext()
                            }
                            .testTag("ace_compress_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Compress,
                                contentDescription = "Compress Context with ACE",
                                tint = if (settings.aceSettings.enabled) EmeraldOk else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isCompressingContext) "ACE..." else "ACE ${if (settings.aceSettings.enabled) "ON" else "OFF"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.aceSettings.enabled) EmeraldOk else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // ACE Rephrase Manual Approval Panel
        AnimatedVisibility(visible = uiState.pendingRephrase != null) {
            uiState.pendingRephrase?.let { proposal ->
                AceRephraseCard(
                    proposal = proposal,
                    onApprove = { onApproveRephrase(true) },
                    onUseOriginal = { onApproveRephrase(false) },
                    onDismiss = onDismissRephrase
                )
            }
        }

        // ACE Diagnostic / Transparent Error / Policy Banner
        AnimatedVisibility(visible = uiState.diagnosticBanner != null) {
            uiState.diagnosticBanner?.let { diag ->
                AceDiagnosticCard(
                    banner = diag,
                    onDismiss = onDismissDiagnostic,
                    onRetry = {
                        val lastUser = messages.lastOrNull { it.role == "user" }
                        if (lastUser != null) {
                            onEditMessage(lastUser, lastUser.originalPrompt ?: lastUser.content)
                        }
                    }
                )
            }
        }

        // ACE Recovery Status Pill
        AnimatedVisibility(visible = uiState.aceRecoveryNotice != null || uiState.isEvaluatingAce) {
            Surface(
                color = VioletContainer.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.aceRecoveryNotice ?: "ACE Smart Rephrase evaluating prompt clarity...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        // Messages List or Empty Research Lab Welcome
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !uiState.isGenerating) {
                EmptyResearchLabState(
                    activeModelName = activeModel.displayName,
                    activeMode = settings.activeChatMode,
                    onPickStarter = { promptInput = it }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onCopy = {
                                clipboard.setText(AnnotatedString(msg.content))
                                onLocalNotice("Message copied")
                            },
                            onEdit = {
                                editingMessage = msg
                                editDraft = msg.originalPrompt ?: msg.content
                            },
                            onRegenerate = { onRegenerateMessage(msg) },
                            onSendCodeToStudio = onSendCodeToStudio,
                            onCopiedNotice = onLocalNotice
                        )
                    }

                    if (uiState.isGenerating) {
                        item(key = "streaming_live_bubble") {
                            StreamingBubble(
                                modelId = uiState.streamingModelId,
                                streamingText = uiState.streamingText
                            )
                        }
                    }
                }
            }
        }

        // Pending Attachments Bar
        AnimatedVisibility(visible = uiState.pendingAttachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.pendingAttachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (att.type == AttachmentType.IMAGE) Icons.Outlined.Image else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = att.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove attachment",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveAttachment(att.id) }
                            )
                        }
                    }
                }
            }
        }

        // Futuristic Composer Bar
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(0.5.dp, ObsidianBorder.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = stringResource(R.string.cd_attach_image),
                        tint = BrightViolet
                    )
                }

                IconButton(
                    onClick = {
                        documentPickerLauncher.launch(
                            arrayOf("text/*", "application/json", "application/xml", "application/pdf")
                        )
                    },
                    modifier = Modifier.testTag("attach_doc_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = stringResource(R.string.cd_attach_document),
                        tint = NeonCyan
                    )
                }

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            text = "Ask Gemini (${settings.activeChatMode.shortBadge})...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (uiState.isGenerating) {
                    Surface(
                        shape = CircleShape,
                        color = CoralError,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onStopGeneration() }
                            .testTag("stop_generation_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = stringResource(R.string.cd_stop_generation),
                                tint = ObsidianVoid
                            )
                        }
                    }
                } else {
                    val canSend = promptInput.isNotBlank() || uiState.pendingAttachments.isNotEmpty()
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) ElectricViolet else ObsidianElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(enabled = canSend) {
                                val text = promptInput
                                promptInput = ""
                                onSendPrompt(text)
                            }
                            .testTag("send_message_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.cd_send_message),
                                tint = if (canSend) ObsidianVoid else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Prompt Dialog
    editingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            containerColor = ObsidianCard,
            title = { Text("Edit & Resend Prompt", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    minLines = 3,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditMessage(msg, editDraft)
                        editingMessage = null
                    }
                ) {
                    Text("Resend")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyResearchLabState(
    activeModelName: String,
    activeMode: ChatMode,
    onPickStarter: (String) -> Unit
) {
    val starters = listOf(
        "Rancang arsitektur aplikasi Android Jetpack Compose offline-first dengan Room & Flow beserta kode lengkap MainActivity.kt",
        "Bandingkan strategi caching & manajemen state MVVM untuk aplikasi AI real-time di Android API 26–35",
        "Buat komponen UI Material 3 futuristic dashboard dengan Canvas chart dan animasi spring di Compose"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = VioletContainer,
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.7f)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = BrightViolet,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GenDev AI Research Lab",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$activeModelName · ${activeMode.label} · Unlimited Client Sessions",
            style = MaterialTheme.typography.labelMedium,
            color = NeonCyan
        )
        Spacer(modifier = Modifier.height(20.dp))
        starters.forEach { sample ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(vertical = 5.dp)
                    .clickable { onPickStarter(sample) }
            ) {
                Text(
                    text = sample,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onSendCodeToStudio: (String, String) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) DeepViolet.copy(alpha = 0.52f) else ObsidianCard,
            border = BorderStroke(
                1.dp,
                if (isUser) ElectricViolet.copy(alpha = 0.75f) else ObsidianBorder.copy(alpha = 0.7f)
            ),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!message.attachmentSummary.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianElevated,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Attachments: ${message.attachmentSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    if (!message.rephrasedPrompt.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianElevated.copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "ACE REPHRASED SENT TO GEMINI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                                Text(
                                    text = message.rephrasedPrompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    MarkdownMessageView(
                        markdown = message.content,
                        onSendCodeToStudio = onSendCodeToStudio,
                        onCopiedNotice = onCopiedNotice
                    )
                }

                if (!message.aceNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.aceNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldOk
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val telemetry = if (!isUser) {
                        buildString {
                            if (message.modelId.isNotBlank()) append(message.modelId)
                            if (message.outputTokens > 0) append(" · ${message.inputTokens}↑ ${message.outputTokens}↓")
                            if (message.latencyMs > 0) append(" · ${"%.1f".format(message.latencyMs / 1000.0)}s")
                        }
                    } else "YOU"

                    Text(
                        text = telemetry,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy),
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isUser) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = BrightViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = onRegenerate, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.action_regenerate),
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(
    modelId: String,
    streamingText: String
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Streaming $modelId...",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (streamingText.isNotBlank()) {
                MarkdownMessageView(markdown = "$streamingText ▌")
            } else {
                Text(
                    text = "Synthesizing response...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun AceRephraseCard(
    proposal: AceRephraseProposal,
    onApprove: () -> Unit,
    onUseOriginal: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ACE SMART REPHRASE PROPOSAL",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )
            Text(
                text = proposal.reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "Original: ${proposal.originalPrompt}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Text(
                text = "Improved: ${proposal.improvedPrompt}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                    Text("Approve & Send")
                }
                OutlinedButton(onClick = onUseOriginal, modifier = Modifier.weight(1f)) {
                    Text("Send Original")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun AceDiagnosticCard(
    banner: AceDiagnosticBanner,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        color = ErrorContainerDark,
        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = CoralError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACE · ${banner.errorType.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            Text(text = banner.message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            banner.providerReason?.let {
                Text(
                    text = "Provider detail: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarn
                )
            }
            banner.suggestedAction?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (banner.canRetry) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text("Retry")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        }
    }
}

private fun formatCompactTokens(tokens: Int): String = when {
    tokens >= 1_000_000 -> "${"%.1f".format(tokens / 1_000_000.0)}M"
    tokens >= 1_000 -> "${"%.1f".format(tokens / 1_000.0)}K"
    else -> tokens.toString()
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()
}

private fun readDocumentPreview(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText().take(24_000)
        }
    }.getOrNull()
}
