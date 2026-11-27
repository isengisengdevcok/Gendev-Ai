package com.example.data

import android.content.Context
import com.example.data.ai.AceEngine
import com.example.data.ai.GeminiEngine
import com.example.data.ai.ModelRegistrySyncService
import com.example.data.github.AndroidTemplateGenerator
import com.example.data.github.GitHubApiService
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.GeminiModelEntity
import com.example.data.local.GenDevDao
import com.example.data.local.GenDevDatabase
import com.example.data.local.ProjectFileEntity
import com.example.data.local.PromptPresetEntity
import com.example.data.local.UsageRecordEntity
import com.example.data.preferences.AppSettings
import com.example.data.preferences.SettingsStore
import com.example.model.AceSettings
import com.example.model.ChatMode
import com.example.model.EngineMode
import com.example.model.GeminiModelCatalog
import com.example.model.GeminiModelSpec
import com.example.model.GeminiTier
import com.example.model.GenerationParameters
import com.example.security.SecureVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DynamicSyncSummary(
    val totalModels: Int,
    val newlyAdded: Int,
    val source: String
)

class GenDevRepository(context: Context) {
    private val appContext = context.applicationContext
    val database: GenDevDatabase = GenDevDatabase.getInstance(appContext)
    val dao: GenDevDao = database.genDevDao()
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val secureVault: SecureVault = SecureVault(appContext)
    val geminiEngine: GeminiEngine = GeminiEngine(appContext, secureVault)
    val aceEngine: AceEngine = AceEngine(geminiEngine)
    val modelSyncService: ModelRegistrySyncService = ModelRegistrySyncService(secureVault)
    val gitHubApiService: GitHubApiService = GitHubApiService()
    val templateGenerator: AndroidTemplateGenerator = AndroidTemplateGenerator(geminiEngine)

    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
    val sessionsFlow: Flow<List<ChatSessionEntity>> = dao.observeSessions()
    val usageRecordsFlow: Flow<List<UsageRecordEntity>> = dao.observeUsageRecords()
    val promptPresetsFlow: Flow<List<PromptPresetEntity>> = dao.observePromptPresets()
    val projectFilesFlow: Flow<List<ProjectFileEntity>> = dao.observeProjectFiles()

    val availableModelsFlow: Flow<List<GeminiModelSpec>> = dao.observeGeminiModels().map { rows ->
        if (rows.isEmpty()) {
            GeminiModelCatalog.models
        } else {
            rows.map { it.toSpec() }
        }
    }

    suspend fun ensureDefaultGeminiModels() {
        val existingIds = dao.getGeminiModels().map { it.id }.toSet()
        val missingDefaults = GeminiModelCatalog.models
            .filterNot { it.id in existingIds }
            .map { spec -> spec.toEntity() }
        if (missingDefaults.isNotEmpty()) {
            dao.upsertGeminiModels(missingDefaults)
        }
    }

    suspend fun syncLatestGeminiModels(engineMode: EngineMode): Result<DynamicSyncSummary> {
        ensureDefaultGeminiModels()
        val beforeIds = dao.getGeminiModels().map { it.id }.toSet()
        val syncResult = modelSyncService.fetchLatestModels(engineMode)
        return syncResult.map { outcome ->
            if (outcome.discoveredModels.isNotEmpty()) {
                dao.upsertGeminiModels(outcome.discoveredModels)
            }
            settingsStore.updateLastModelSync(System.currentTimeMillis())
            val after = dao.getGeminiModels()
            val added = after.count { it.id !in beforeIds }
            DynamicSyncSummary(
                totalModels = after.size,
                newlyAdded = added,
                source = outcome.sourceLabel
            )
        }
    }

    suspend fun addCustomGeminiModel(rawId: String, customDisplayName: String): Result<GeminiModelSpec> {
        val cleanId = rawId.trim().lowercase().removePrefix("models/")
        if (!Regex("^[a-z0-9][a-z0-9._-]{2,64}$").matches(cleanId)) {
            return Result.failure(
                IllegalArgumentException("Format Model ID tidak valid (contoh: gemini-4-carbon atau gemini-4-neon).")
            )
        }
        val entity = GeminiModelEntity(
            id = cleanId,
            displayName = customDisplayName.trim().ifBlank { GeminiModelCatalog.prettifyModelName(cleanId) },
            tierName = ModelRegistrySyncService.inferTier(cleanId).name,
            tagline = "Custom / Early-Access Gemini model registered by user",
            contextWindowTokens = 1_048_576,
            maxOutputTokens = 65_536,
            inputPricePerMillionUsd = 0.75,
            outputPricePerMillionUsd = 5.00,
            recommendedFor = "Custom Gemini model endpoint",
            isDynamic = true,
            supportsLegacySamplingParams = !cleanId.startsWith("gemini-4") && !cleanId.startsWith("gemini-5"),
            sortOrder = ModelRegistrySyncService.inferSortOrder(cleanId)
        )
        dao.upsertGeminiModel(entity)
        return Result.success(entity.toSpec())
    }

    suspend fun deleteDynamicModel(modelId: String) = dao.deleteDynamicModel(modelId)

    suspend fun updateAutoSyncModels(enabled: Boolean) = settingsStore.updateAutoSyncModels(enabled)

    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>> =
        dao.observeMessagesForSession(sessionId)

    fun observeSession(sessionId: Long): Flow<ChatSessionEntity?> =
        dao.observeSessionById(sessionId)

    suspend fun createSession(
        title: String,
        mode: ChatMode,
        modelId: String,
        systemPrompt: String
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insertSession(
            ChatSessionEntity(
                title = title,
                modeId = mode.id,
                modelId = modelId,
                systemPrompt = systemPrompt,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    suspend fun updateSession(session: ChatSessionEntity) = dao.updateSession(session)

    suspend fun insertMessage(message: ChatMessageEntity): Long = dao.insertMessage(message)

    suspend fun updateMessage(message: ChatMessageEntity) = dao.updateMessage(message)

    suspend fun deleteMessagesFrom(sessionId: Long, fromMessageId: Long) =
        dao.deleteMessagesFrom(sessionId, fromMessageId)

    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity> =
        dao.getMessagesForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): ChatSessionEntity? =
        dao.getSessionById(sessionId)

    suspend fun recordUsage(record: UsageRecordEntity) = dao.insertUsageRecord(record)

    suspend fun clearUsageRecords() = dao.clearUsageRecords()

    suspend fun savePromptPreset(name: String, mode: ChatMode, prompt: String) {
        dao.insertPromptPreset(
            PromptPresetEntity(
                name = name.trim(),
                modeId = mode.id,
                systemPrompt = prompt.trim()
            )
        )
    }

    suspend fun deletePromptPreset(id: Long) = dao.deletePromptPreset(id)

    suspend fun updateEngineMode(mode: EngineMode) = settingsStore.updateEngineMode(mode)
    suspend fun updateModelId(modelId: String) = settingsStore.updateModelId(modelId)
    suspend fun updateChatMode(mode: ChatMode, syncPrompt: Boolean = true) =
        settingsStore.updateChatMode(mode, syncPrompt)
    suspend fun updateSystemPrompt(prompt: String) = settingsStore.updateSystemPrompt(prompt)
    suspend fun updateGenerationParams(params: GenerationParameters) =
        settingsStore.updateGenerationParams(params)
    suspend fun updateAceSettings(ace: AceSettings) = settingsStore.updateAceSettings(ace)
    suspend fun updateGitHubProjectConfig(
        projectName: String,
        packageName: String,
        branch: String,
        isPrivate: Boolean
    ) = settingsStore.updateGitHubProjectConfig(projectName, packageName, branch, isPrivate)

    suspend fun ensureDefaultProjectFiles(appName: String, packageName: String) {
        val current = dao.getProjectFiles()
        if (current.isEmpty()) {
            dao.upsertProjectFiles(templateGenerator.buildDefaultProjectFiles(appName, packageName))
        }
    }

    suspend fun replaceProjectFiles(files: List<ProjectFileEntity>) {
        dao.clearProjectFiles()
        dao.upsertProjectFiles(files)
    }

    suspend fun upsertProjectFile(path: String, content: String, language: String) {
        dao.upsertProjectFile(
            ProjectFileEntity(
                path = path.trim().trimStart('/'),
                content = content,
                language = language,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProjectFile(id: Long) = dao.deleteProjectFile(id)
}

private fun GeminiModelEntity.toSpec(): GeminiModelSpec = GeminiModelSpec(
    id = id,
    displayName = displayName,
    tier = runCatching { GeminiTier.valueOf(tierName) }.getOrDefault(GeminiTier.DYNAMIC),
    tagline = tagline,
    contextWindowTokens = contextWindowTokens,
    maxOutputTokens = maxOutputTokens,
    inputPricePerMillionUsd = inputPricePerMillionUsd,
    outputPricePerMillionUsd = outputPricePerMillionUsd,
    recommendedFor = recommendedFor,
    isDynamic = isDynamic,
    supportsLegacySamplingParams = supportsLegacySamplingParams
)

private fun GeminiModelSpec.toEntity(): GeminiModelEntity = GeminiModelEntity(
    id = id,
    displayName = displayName,
    tierName = tier.name,
    tagline = tagline,
    contextWindowTokens = contextWindowTokens,
    maxOutputTokens = maxOutputTokens,
    inputPricePerMillionUsd = inputPricePerMillionUsd,
    outputPricePerMillionUsd = outputPricePerMillionUsd,
    recommendedFor = recommendedFor,
    isDynamic = isDynamic,
    supportsLegacySamplingParams = supportsLegacySamplingParams,
    sortOrder = ModelRegistrySyncService.inferSortOrder(id)
)
