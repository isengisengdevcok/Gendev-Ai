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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.model.GeminiModelSpec
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.StatusPill
import com.example.ui.components.labTextFieldColors
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
    availableModels: List<GeminiModelSpec>,
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

    val activeModel = remember(settings.activeModelId, availableModels) {
        GeminiModelCatalog.findById(settings.activeModelId, availableModels)
    }

    val estimatedContextTokens = remember(messages, activeSession) {
        val msgTokens = messages.sumOf { GeminiEngine.estimateTokens(it.content) }
        val memTokens = GeminiEngine.estimateTokens(activeSession?.compressedContextSummary.orEmpty())
        msgTokens + memTokens
    }

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
            .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianVoid)))
            .imePadding()
    ) {
        // Clean, uncluttered 2-row Top Bar
        Surface(color = ObsidianSurface) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { showModelMenu = true }
                                .testTag("model_selector_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldOk)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeModel.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Switch Gemini model",
                                    tint = BrightViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            containerColor = ObsidianCard
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = model.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (model.id == activeModel.id) BrightViolet else TextPrimary
                                                )
                                                Text(
                                                    text = model.tier.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeonCyan
                                                )
                                            }
                                            Text(
                                                text = model.tagline,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

                // Slim Mode + Context Utility Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
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
                                if (selected) ElectricViolet else ObsidianBorder.copy(alpha = 0.55f)
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

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .clickable(enabled = !uiState.isCompressingContext && messages.size > 4) {
                                onCompressContext()
                            }
                            .testTag("ace_compress_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Compress,
                                contentDescription = "ACE context status",
                                tint = if (settings.aceSettings.enabled) EmeraldOk else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "CTX ${formatCompactTokens(estimatedContextTokens)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.8.dp, color = ObsidianBorder.copy(alpha = 0.55f))
            }
        }

        // ACE Banners
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

        AnimatedVisibility(visible = uiState.aceRecoveryNotice != null || uiState.isEvaluatingAce) {
            Surface(
                color = VioletContainer.copy(alpha = 0.9f),
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
                        text = uiState.aceRecoveryNotice ?: "ACE Smart Rephrase evaluating prompt...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        // Main Conversation Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !uiState.isGenerating) {
                EmptyResearchLabState(
                    activeModelName = activeModel.displayName,
                    activeMode = settings.activeChatMode,
                    onPickStarter = { promptInput = it },
                    onOpenGitHubStudio = onNavigateToGitHubStudio
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
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

        // Pending Attachments Chips
        AnimatedVisibility(visible = uiState.pendingAttachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.pendingAttachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (att.type == AttachmentType.IMAGE) Icons.Outlined.Image else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
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
                            Spacer(modifier = Modifier.width(6.dp))
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

        // Modern 2-Tier Floating Composer Dock (full-width typing + clean bottom action bar)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.85f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (promptInput.isEmpty()) {
                        Text(
                            text = "Ask ${activeModel.displayName} anything...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                    BasicTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(ElectricViolet),
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_field")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianElevated,
                            modifier = Modifier
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("attach_image_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = stringResource(R.string.cd_attach_image),
                                    tint = BrightViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Photo", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianElevated,
                            modifier = Modifier
                                .clickable {
                                    documentPickerLauncher.launch(
                                        arrayOf("text/*", "application/json", "application/xml", "application/pdf")
                                    )
                                }
                                .testTag("attach_doc_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = stringResource(R.string.cd_attach_document),
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("File", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    if (uiState.isGenerating) {
                        Surface(
                            shape = CircleShape,
                            color = CoralError,
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onStopGeneration() }
                                .testTag("stop_generation_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = stringResource(R.string.cd_stop_generation),
                                    tint = ObsidianVoid,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        val canSend = promptInput.isNotBlank() || uiState.pendingAttachments.isNotEmpty()
                        Surface(
                            shape = CircleShape,
                            color = if (canSend) ElectricViolet else ObsidianElevated,
                            modifier = Modifier
                                .size(42.dp)
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
                                    tint = if (canSend) ObsidianVoid else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

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
                    colors = labTextFieldColors(),
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
    onPickStarter: (String) -> Unit,
    onOpenGitHubStudio: () -> Unit
) {
    val starters = listOf(
        "Rancang aplikasi Android Jetpack Compose lengkap dengan Room DB & Material 3 beserta kode MainActivity.kt",
        "Bandingkan arsitektur MVVM + Flow vs MVI untuk aplikasi riset AI real-time di Android",
        "Buat komponen UI dashboard futuristik dengan animasi spring dan Canvas chart di Compose"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VioletContainer,
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.65f)),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = BrightViolet,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GenDev AI Studio",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(text = activeModelName, accent = NeonCyan)
            StatusPill(text = activeMode.shortBadge, accent = BrightViolet)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            starters.forEach { sample ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickStarter(sample) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sample,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = BrightViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onOpenGitHubStudio,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Buka GitHub Android Build Studio", color = NeonCyan, fontWeight = FontWeight.SemiBold)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isUser) 36.dp else 0.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 18.dp
            ),
            color = if (isUser) DeepViolet.copy(alpha = 0.45f) else ObsidianCard,
            border = BorderStroke(
                1.dp,
                if (isUser) ElectricViolet.copy(alpha = 0.65f) else ObsidianBorder.copy(alpha = 0.65f)
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.94f else 1f).widthIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!message.attachmentSummary.isNullOrBlank()) {
                    StatusPill(text = "📎 ${message.attachmentSummary}", accent = NeonCyan)
                }

                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    if (!message.rephrasedPrompt.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("ACE REPHRASED", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                Text(message.rephrasedPrompt, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                    Text(text = message.aceNote, style = MaterialTheme.typography.labelSmall, color = EmeraldOk)
                }

                HorizontalDivider(thickness = 0.6.dp, color = ObsidianBorder.copy(alpha = 0.4f))

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

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().widthIn(max = 580.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Streaming $modelId...", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            }
            if (streamingText.isNotBlank()) {
                MarkdownMessageView(markdown = "$streamingText ▌")
            } else {
                Text("Synthesizing response...", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
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
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ACE SMART REPHRASE", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
            Text(proposal.reason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("Original: ${proposal.originalPrompt}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text("Improved: ${proposal.improvedPrompt}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Approve") }
                OutlinedButton(onClick = onUseOriginal, modifier = Modifier.weight(1f)) { Text("Original") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
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
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ACE · ${banner.errorType.title}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }
            Text(banner.message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            banner.providerReason?.let {
                Text("Provider detail: $it", style = MaterialTheme.typography.labelSmall, color = AmberWarn)
            }
            banner.suggestedAction?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (banner.canRetry) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) { Text("Retry") }
                }
                TextButton(onClick = onDismiss) { Text("Dismiss", color = TextSecondary) }
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
