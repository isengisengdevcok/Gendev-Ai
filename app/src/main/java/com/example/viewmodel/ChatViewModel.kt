package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GenDevRepository
import com.example.data.ai.ChatTurn
import com.example.data.ai.GeminiEngine
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.PromptPresetEntity
import com.example.data.local.UsageRecordEntity
import com.example.data.preferences.AppSettings
import com.example.model.AceDiagnosticBanner
import com.example.model.AceErrorType
import com.example.model.AceRephraseProposal
import com.example.model.AceSettings
import com.example.model.AttachmentItem
import com.example.model.ChatMode
import com.example.model.ConnectionTestState
import com.example.model.EngineMode
import com.example.model.GeminiModelCatalog
import com.example.model.GeminiModelSpec
import com.example.model.GenerationParameters
import com.example.model.ModelSyncState
import com.example.security.SecureVault
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ChatViewModel"

data class ChatUiState(
    val activeSessionId: Long? = null,
    val isGenerating: Boolean = false,
    val isEvaluatingAce: Boolean = false,
    val isCompressingContext: Boolean = false,
    val streamingText: String = "",
    val streamingModelId: String = "",
    val pendingAttachments: List<AttachmentItem> = emptyList(),
    val pendingRephrase: AceRephraseProposal? = null,
    val diagnosticBanner: AceDiagnosticBanner? = null,
    val aceRecoveryNotice: String? = null,
    val editingMessageId: Long? = null,
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle,
    val modelSyncState: ModelSyncState = ModelSyncState.Idle,
    val statusSnackbar: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: GenDevRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.sessionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val usageRecords: StateFlow<List<UsageRecordEntity>> = repository.usageRecordsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val promptPresets: StateFlow<List<PromptPresetEntity>> = repository.promptPresetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val availableModels: StateFlow<List<GeminiModelSpec>> = repository.availableModelsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GeminiModelCatalog.models
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val activeSessionIdFlow = MutableStateFlow<Long?>(null)

    val activeSession: StateFlow<ChatSessionEntity?> = activeSessionIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeSession(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<ChatMessageEntity>> = activeSessionIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var activeGenerationJob: Job? = null
    private var hasAutoSyncedThisLaunch = false

    init {
        viewModelScope.launch {
            repository.ensureDefaultGeminiModels()
        }
        viewModelScope.launch {
            settings.collect { cfg ->
                if (!hasAutoSyncedThisLaunch && cfg.autoSyncModels) {
                    hasAutoSyncedThisLaunch = true
                    syncLatestModels(showSnackbar = false)
                }
            }
        }
        viewModelScope.launch {
            sessions.collect { list ->
                if (activeSessionIdFlow.value == null && list.isNotEmpty()) {
                    selectSession(list.first().id)
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        stopGeneration()
        activeSessionIdFlow.value = sessionId
        _uiState.update {
            it.copy(
                activeSessionId = sessionId,
                diagnosticBanner = null,
                pendingRephrase = null,
                editingMessageId = null
            )
        }
    }

    fun createNewSession(initialTitle: String = "New Research Session") {
        stopGeneration()
        viewModelScope.launch {
            val cfg = settings.value
            val newId = repository.createSession(
                title = initialTitle,
                mode = cfg.activeChatMode,
                modelId = cfg.activeModelId,
                systemPrompt = cfg.customSystemPrompt
            )
            activeSessionIdFlow.value = newId
            _uiState.update {
                it.copy(
                    activeSessionId = newId,
                    diagnosticBanner = null,
                    pendingRephrase = null,
                    pendingAttachments = emptyList(),
                    editingMessageId = null
                )
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (activeSessionIdFlow.value == sessionId) {
                val remaining = sessions.value.filterNot { it.id == sessionId }
                val nextId = remaining.firstOrNull()?.id
                activeSessionIdFlow.value = nextId
                _uiState.update { it.copy(activeSessionId = nextId) }
            }
        }
    }

    fun addAttachment(item: AttachmentItem) {
        _uiState.update { state ->
            state.copy(pendingAttachments = (state.pendingAttachments + item).take(5))
        }
    }

    fun removeAttachment(id: String) {
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments.filterNot { it.id == id })
        }
    }

    fun dismissDiagnostic() {
        _uiState.update { it.copy(diagnosticBanner = null, aceRecoveryNotice = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(statusSnackbar = null) }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        val partial = _uiState.value.streamingText
        val sid = activeSessionIdFlow.value
        val modelId = _uiState.value.streamingModelId
        if (_uiState.value.isGenerating && partial.isNotBlank() && sid != null) {
            viewModelScope.launch {
                repository.insertMessage(
                    ChatMessageEntity(
                        sessionId = sid,
                        role = "model",
                        content = "$partial\n\n_[Generation stopped by user]_",
                        modelId = modelId
                    )
                )
            }
        }
        _uiState.update {
            it.copy(isGenerating = false, streamingText = "", aceRecoveryNotice = null)
        }
    }

    fun sendPrompt(rawPrompt: String, bypassAceRephrase: Boolean = false) {
        val trimmed = rawPrompt.trim()
        val attachments = _uiState.value.pendingAttachments
        if (trimmed.isEmpty() && attachments.isEmpty()) return
        if (_uiState.value.isGenerating || _uiState.value.isEvaluatingAce) return

        val cfg = settings.value
        viewModelScope.launch {
            val sessionId = ensureActiveSession(trimmed.ifEmpty { attachments.first().displayName })
            val currentMessages = repository.getMessagesForSession(sessionId)

            if (!bypassAceRephrase && cfg.aceSettings.enabled && cfg.aceSettings.autoRephrase && trimmed.isNotBlank()) {
                _uiState.update { it.copy(isEvaluatingAce = true, diagnosticBanner = null) }
                val recentTurns = currentMessages.takeLast(6).map { ChatTurn(it.role, it.content) }
                val proposal = repository.aceEngine.evaluateSmartRephrase(
                    engineMode = cfg.engineMode,
                    originalPrompt = trimmed,
                    recentHistory = recentTurns,
                    attachments = attachments
                )
                _uiState.update { it.copy(isEvaluatingAce = false) }
                if (proposal != null) {
                    if (cfg.aceSettings.requireManualApproval) {
                        _uiState.update { it.copy(pendingRephrase = proposal) }
                        return@launch
                    } else {
                        executePromptSend(
                            sessionId = sessionId,
                            originalPrompt = proposal.originalPrompt,
                            effectivePrompt = proposal.improvedPrompt,
                            aceNote = "ACE Smart Rephrase applied: ${proposal.reason}",
                            attachments = attachments
                        )
                        return@launch
                    }
                }
            }

            executePromptSend(
                sessionId = sessionId,
                originalPrompt = trimmed,
                effectivePrompt = trimmed,
                aceNote = null,
                attachments = attachments
            )
        }
    }

    fun approvePendingRephrase(useImproved: Boolean) {
        val proposal = _uiState.value.pendingRephrase ?: return
        _uiState.update { it.copy(pendingRephrase = null) }
        viewModelScope.launch {
            val sid = ensureActiveSession(proposal.originalPrompt)
            executePromptSend(
                sessionId = sid,
                originalPrompt = proposal.originalPrompt,
                effectivePrompt = if (useImproved) proposal.improvedPrompt else proposal.originalPrompt,
                aceNote = if (useImproved) "ACE Approved Rephrase: ${proposal.reason}" else null,
                attachments = proposal.attachments
            )
        }
    }

    fun dismissPendingRephrase() {
        _uiState.update { it.copy(pendingRephrase = null) }
    }

    fun editAndResendUserMessage(message: ChatMessageEntity, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        stopGeneration()
        viewModelScope.launch {
            repository.deleteMessagesFrom(message.sessionId, message.id)
            executePromptSend(
                sessionId = message.sessionId,
                originalPrompt = trimmed,
                effectivePrompt = trimmed,
                aceNote = "Edited prompt",
                attachments = emptyList()
            )
        }
    }

    fun regenerateLastResponse(modelMessage: ChatMessageEntity) {
        stopGeneration()
        viewModelScope.launch {
            val all = repository.getMessagesForSession(modelMessage.sessionId)
            val targetIdx = all.indexOfFirst { it.id == modelMessage.id }
            if (targetIdx <= 0) return@launch
            val precedingUser = all.subList(0, targetIdx).lastOrNull { it.role == "user" } ?: return@launch
            repository.deleteMessagesFrom(modelMessage.sessionId, modelMessage.id)
            runModelInference(
                sessionId = modelMessage.sessionId,
                effectivePrompt = precedingUser.rephrasedPrompt ?: precedingUser.content,
                attachments = emptyList()
            )
        }
    }

    fun compressCurrentSessionContext() {
        val sid = activeSessionIdFlow.value ?: return
        if (_uiState.value.isCompressingContext) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCompressingContext = true) }
            try {
                val session = repository.getSessionById(sid) ?: return@launch
                val msgs = repository.getMessagesForSession(sid)
                if (msgs.size <= 4) {
                    _uiState.update { it.copy(statusSnackbar = "Context is already compact (${msgs.size} messages).") }
                    return@launch
                }
                val older = msgs.dropLast(4).map { ChatTurn(it.role, it.content) }
                val summary = repository.aceEngine.compressContext(
                    engineMode = settings.value.engineMode,
                    existingSummary = session.compressedContextSummary,
                    turnsToCompress = older
                )
                repository.updateSession(
                    session.copy(
                        compressedContextSummary = summary,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _uiState.update {
                    it.copy(
                        statusSnackbar = "ACE Context Compression complete — preserved key technical memory.",
                        aceRecoveryNotice = "Compressed ${older.size} earlier turns into lossless memory."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Context compression failed", e)
                _uiState.update { it.copy(statusSnackbar = "Compression failed: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isCompressingContext = false) }
            }
        }
    }

    private suspend fun ensureActiveSession(firstPrompt: String): Long {
        val existing = activeSessionIdFlow.value
        if (existing != null && repository.getSessionById(existing) != null) {
            return existing
        }
        val cfg = settings.value
        val title = firstPrompt.take(44).replace("\n", " ").ifBlank { "Research Session" }
        val newId = repository.createSession(
            title = title,
            mode = cfg.activeChatMode,
            modelId = cfg.activeModelId,
            systemPrompt = cfg.customSystemPrompt
        )
        activeSessionIdFlow.value = newId
        _uiState.update { it.copy(activeSessionId = newId) }
        return newId
    }

    private suspend fun executePromptSend(
        sessionId: Long,
        originalPrompt: String,
        effectivePrompt: String,
        aceNote: String?,
        attachments: List<AttachmentItem>
    ) {
        val attachmentDesc = attachments.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.displayName }

        val existingMsgs = repository.getMessagesForSession(sessionId)
        val session = repository.getSessionById(sessionId)
        if (session != null && existingMsgs.isEmpty()) {
            val autoTitle = originalPrompt.take(44).replace("\n", " ").ifBlank { "Research Session" }
            repository.updateSession(session.copy(title = autoTitle, updatedAt = System.currentTimeMillis()))
        }

        repository.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = originalPrompt.ifBlank { "Attached: ${attachmentDesc.orEmpty()}" },
                originalPrompt = originalPrompt,
                rephrasedPrompt = if (effectivePrompt != originalPrompt) effectivePrompt else null,
                attachmentSummary = attachmentDesc,
                aceNote = aceNote
            )
        )
        _uiState.update { it.copy(pendingAttachments = emptyList(), diagnosticBanner = null) }
        runModelInference(sessionId, effectivePrompt, attachments)
    }

    private fun runModelInference(
        sessionId: Long,
        effectivePrompt: String,
        attachments: List<AttachmentItem>
    ) {
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            val cfg = settings.value
            val modelSpec = GeminiModelCatalog.findById(cfg.activeModelId, availableModels.value)
            val startTime = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    streamingText = "",
                    streamingModelId = modelSpec.id,
                    diagnosticBanner = null,
                    aceRecoveryNotice = null
                )
            }

            try {
                var session = repository.getSessionById(sessionId)
                val allMessages = repository.getMessagesForSession(sessionId)
                val historyWithoutCurrent = if (allMessages.isNotEmpty() && allMessages.last().role == "user") {
                    allMessages.dropLast(1)
                } else {
                    allMessages
                }

                // Auto Context Compression if approaching threshold
                val estimatedHistoryTokens = historyWithoutCurrent.sumOf { GeminiEngine.estimateTokens(it.content) }
                val thresholdTokens = (modelSpec.contextWindowTokens * cfg.aceSettings.compressionThresholdPercent) / 100
                if (cfg.aceSettings.enabled &&
                    cfg.aceSettings.autoCompressContext &&
                    estimatedHistoryTokens > thresholdTokens &&
                    historyWithoutCurrent.size > 6 &&
                    session != null
                ) {
                    _uiState.update { it.copy(aceRecoveryNotice = "ACE Auto-Compressing older context...") }
                    val summary = runCatching {
                        repository.aceEngine.compressContext(
                            engineMode = cfg.engineMode,
                            existingSummary = session.compressedContextSummary,
                            turnsToCompress = historyWithoutCurrent.dropLast(6).map { ChatTurn(it.role, it.content) }
                        )
                    }.getOrDefault(session.compressedContextSummary)
                    session = session.copy(compressedContextSummary = summary)
                    repository.updateSession(session)
                }

                val effectiveSystemPrompt = buildString {
                    val userSys = cfg.customSystemPrompt.trim()
                    if (userSys.isNotEmpty()) appendLine(userSys)
                    val memory = session?.compressedContextSummary.orEmpty().trim()
                    if (memory.isNotEmpty()) {
                        appendLine()
                        appendLine("[ACE Preserved Conversation Memory]")
                        append(memory)
                    }
                }

                val turns = if (!session?.compressedContextSummary.isNullOrBlank() && historyWithoutCurrent.size > 8) {
                    historyWithoutCurrent.takeLast(8).map { ChatTurn(it.role, it.content) }
                } else {
                    historyWithoutCurrent.map { ChatTurn(it.role, it.content) }
                }

                val maxAttempts = if (cfg.aceSettings.enabled && cfg.aceSettings.autoRecovery) 2 else 1
                var attempt = 0
                var succeeded = false
                var finalOutput = ""
                var promptTokens = 0
                var outputTokens = 0
                var finishReason: String? = null
                var lastError: Throwable? = null

                while (attempt < maxAttempts && !succeeded) {
                    attempt++
                    val sb = StringBuilder()
                    try {
                        repository.geminiEngine.streamChat(
                            engineMode = cfg.engineMode,
                            modelId = modelSpec.id,
                            systemPrompt = effectiveSystemPrompt,
                            history = turns,
                            userPrompt = effectivePrompt,
                            attachments = attachments,
                            params = cfg.generationParams
                        ).collect { chunk ->
                            if (chunk.deltaText.isNotEmpty()) {
                                sb.append(chunk.deltaText)
                                _uiState.update { it.copy(streamingText = sb.toString()) }
                            }
                            if (chunk.promptTokens > 0) promptTokens = chunk.promptTokens
                            if (chunk.outputTokens > 0) outputTokens = chunk.outputTokens
                            if (chunk.finishReason != null) finishReason = chunk.finishReason
                        }
                        finalOutput = sb.toString()
                        succeeded = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        val classified = repository.aceEngine.classifyError(e)
                        if (attempt < maxAttempts && repository.aceEngine.isTransientRecoverable(classified.errorType)) {
                            _uiState.update {
                                it.copy(aceRecoveryNotice = "ACE Auto-Recovery: ${classified.errorType.title}. Retrying...")
                            }
                            delay(1_800L)
                        } else {
                            break
                        }
                    }
                }

                val latencyMs = System.currentTimeMillis() - startTime

                if (succeeded) {
                    val safePromptTok = if (promptTokens > 0) promptTokens else GeminiEngine.estimateTokens(effectivePrompt)
                    val safeOutTok = if (outputTokens > 0) outputTokens else GeminiEngine.estimateTokens(finalOutput)
                    val policyBlocked = finishReason != null &&
                        finishReason.equals("SAFETY", ignoreCase = true) ||
                        finishReason.equals("PROHIBITED_CONTENT", ignoreCase = true)

                    if (finalOutput.isBlank() && policyBlocked) {
                        val banner = AceDiagnosticBanner(
                            errorType = AceErrorType.PROVIDER_POLICY_BLOCK,
                            message = "Model Gemini menghentikan respons karena kebijakan keselamatan penyedia.",
                            providerReason = "finishReason = $finishReason",
                            suggestedAction = "Coba ulangi dengan konteks riset defensif atau akademis yang jelas. GenDev AI tidak memfilter atau menyamarkan permintaan Anda.",
                            canRetry = false
                        )
                        _uiState.update { it.copy(diagnosticBanner = banner) }
                    } else {
                        val contentToSave = finalOutput.ifBlank {
                            "Model mengembalikan respons kosong (finishReason: ${finishReason ?: "UNKNOWN"})."
                        }
                        repository.insertMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                role = "model",
                                content = contentToSave,
                                modelId = modelSpec.id,
                                inputTokens = safePromptTok,
                                outputTokens = safeOutTok,
                                latencyMs = latencyMs,
                                aceNote = if (attempt > 1) "Recovered automatically via ACE retry #$attempt" else null
                            )
                        )
                        if (session != null) {
                            repository.updateSession(
                                session.copy(
                                    modelId = modelSpec.id,
                                    totalInputTokens = session.totalInputTokens + safePromptTok,
                                    totalOutputTokens = session.totalOutputTokens + safeOutTok,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                        repository.recordUsage(
                            UsageRecordEntity(
                                sessionId = sessionId,
                                modelId = modelSpec.id,
                                engineMode = cfg.engineMode.name,
                                inputTokens = safePromptTok,
                                outputTokens = safeOutTok,
                                estimatedCostUsd = GeminiModelCatalog.estimateCostUsd(
                                    modelSpec.id,
                                    safePromptTok,
                                    safeOutTok,
                                    availableModels.value
                                ),
                                latencyMs = latencyMs,
                                success = true
                            )
                        )
                    }
                } else {
                    val err = lastError ?: IllegalStateException("Unknown Gemini inference failure")
                    val banner = repository.aceEngine.classifyError(err)
                    _uiState.update { it.copy(diagnosticBanner = banner) }
                    repository.recordUsage(
                        UsageRecordEntity(
                            sessionId = sessionId,
                            modelId = modelSpec.id,
                            engineMode = cfg.engineMode.name,
                            inputTokens = GeminiEngine.estimateTokens(effectivePrompt),
                            outputTokens = 0,
                            estimatedCostUsd = 0.0,
                            latencyMs = latencyMs,
                            success = false,
                            errorDetail = err.localizedMessage?.take(180)
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        streamingText = "",
                        aceRecoveryNotice = null
                    )
                }
            }
        }
    }

    // Settings & Configuration Mutations
    fun setEngineMode(mode: EngineMode) = viewModelScope.launch {
        repository.updateEngineMode(mode)
    }

    fun setActiveModel(modelId: String) = viewModelScope.launch {
        repository.updateModelId(modelId)
    }

    fun setChatMode(mode: ChatMode, updatePrompt: Boolean = true) = viewModelScope.launch {
        repository.updateChatMode(mode, updatePrompt)
    }

    fun setSystemPrompt(prompt: String) = viewModelScope.launch {
        repository.updateSystemPrompt(prompt)
    }

    fun setGenerationParams(params: GenerationParameters) = viewModelScope.launch {
        repository.updateGenerationParams(params)
    }

    fun setAceSettings(ace: AceSettings) = viewModelScope.launch {
        repository.updateAceSettings(ace)
    }

    fun savePromptPreset(name: String, mode: ChatMode, prompt: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repository.savePromptPreset(name, mode, prompt)
        _uiState.update { it.copy(statusSnackbar = "Preset '$name' saved to Room DB.") }
    }

    fun deletePromptPreset(id: Long) = viewModelScope.launch {
        repository.deletePromptPreset(id)
    }

    fun clearUsageHistory() = viewModelScope.launch {
        repository.clearUsageRecords()
        _uiState.update { it.copy(statusSnackbar = "Usage log cleared.") }
    }

    fun saveByokCredentials(apiKey: String, proxyUrl: String): Result<Unit> {
        val proxyValidation = SecureVault.validateProxyUrl(proxyUrl)
        if (proxyValidation.isFailure) {
            return Result.failure(proxyValidation.exceptionOrNull()!!)
        }
        if (apiKey.isNotBlank()) {
            repository.secureVault.saveGeminiApiKey(apiKey)
        }
        repository.secureVault.saveCustomProxyUrl(proxyValidation.getOrDefault(""))
        _uiState.update { it.copy(statusSnackbar = "Encrypted in Android Keystore (AES-256-GCM).") }
        return Result.success(Unit)
    }

    fun clearByokApiKey() {
        repository.secureVault.saveGeminiApiKey("")
        _uiState.update { it.copy(statusSnackbar = "Stored BYOK Gemini API key cleared.") }
    }

    fun getMaskedGeminiKey(): String =
        repository.secureVault.maskSecret(repository.secureVault.getGeminiApiKey())

    fun hasByokGeminiKey(): Boolean =
        repository.secureVault.getGeminiApiKey().isNotBlank()

    fun getStoredProxyUrl(): String = repository.secureVault.getCustomProxyUrl()

    fun testGeminiConnection() {
        viewModelScope.launch {
            val cfg = settings.value
            _uiState.update { it.copy(connectionTestState = ConnectionTestState.Testing) }
            val result = repository.geminiEngine.testConnection(cfg.engineMode, cfg.activeModelId)
            _uiState.update {
                it.copy(
                    connectionTestState = result.fold(
                        onSuccess = { (details, latency) -> ConnectionTestState.Success(details, latency) },
                        onFailure = { err -> ConnectionTestState.Failure(err.localizedMessage ?: "Connection failed") }
                    )
                )
            }
        }
    }

    fun syncLatestModels(showSnackbar: Boolean = true) {
        if (_uiState.value.modelSyncState is ModelSyncState.Syncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(modelSyncState = ModelSyncState.Syncing) }
            val result = repository.syncLatestGeminiModels(settings.value.engineMode)
            result.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            modelSyncState = ModelSyncState.Success(
                                totalModels = summary.totalModels,
                                newlyAdded = summary.newlyAdded,
                                source = summary.source
                            ),
                            statusSnackbar = if (showSnackbar) {
                                "Model registry synced (${summary.totalModels} model aktif, +${summary.newlyAdded} baru dari ${summary.source})."
                            } else it.statusSnackbar
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            modelSyncState = ModelSyncState.Failure(
                                err.localizedMessage ?: "Gagal menyinkronkan katalog model terbaru."
                            ),
                            statusSnackbar = if (showSnackbar) err.localizedMessage else it.statusSnackbar
                        )
                    }
                }
            )
        }
    }

    fun setAutoSyncModels(enabled: Boolean) = viewModelScope.launch {
        repository.updateAutoSyncModels(enabled)
    }

    fun addCustomModel(modelId: String, displayName: String) {
        viewModelScope.launch {
            val result = repository.addCustomGeminiModel(modelId, displayName)
            result.fold(
                onSuccess = { spec ->
                    repository.updateModelId(spec.id)
                    _uiState.update {
                        it.copy(statusSnackbar = "Model '${spec.displayName}' (${spec.id}) ditambahkan & diaktifkan.")
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(statusSnackbar = err.localizedMessage ?: "Model ID tidak valid.")
                    }
                }
            )
        }
    }

    fun deleteDynamicModel(modelId: String) {
        viewModelScope.launch {
            repository.deleteDynamicModel(modelId)
            if (settings.value.activeModelId == modelId) {
                repository.updateModelId(GeminiModelCatalog.defaultModel.id)
            }
            _uiState.update { it.copy(statusSnackbar = "Model '$modelId' dihapus dari daftar lokal.") }
        }
    }
}
