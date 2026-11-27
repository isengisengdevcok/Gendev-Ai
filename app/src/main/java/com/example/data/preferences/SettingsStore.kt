package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AceSettings
import com.example.model.ChatMode
import com.example.model.EngineMode
import com.example.model.GeminiModelCatalog
import com.example.model.GenerationParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "gendev_settings")

data class AppSettings(
    val engineMode: EngineMode = EngineMode.FIREBASE_AI_LOGIC,
    val activeModelId: String = GeminiModelCatalog.defaultModel.id,
    val activeChatMode: ChatMode = ChatMode.DEVELOPER,
    val customSystemPrompt: String = ChatMode.DEVELOPER.defaultSystemPrompt,
    val generationParams: GenerationParameters = GenerationParameters(),
    val aceSettings: AceSettings = AceSettings(),
    val githubProjectName: String = "GenDevSampleApp",
    val githubPackageName: String = "com.gendev.sampleapp",
    val githubBranch: String = "main",
    val githubPrivateRepo: Boolean = false,
    val autoSyncModels: Boolean = true,
    val lastModelSyncEpochMs: Long = 0L
)

class SettingsStore(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            engineMode = runCatching {
                EngineMode.valueOf(prefs[KEY_ENGINE_MODE] ?: EngineMode.FIREBASE_AI_LOGIC.name)
            }.getOrDefault(EngineMode.FIREBASE_AI_LOGIC),
            activeModelId = prefs[KEY_MODEL_ID] ?: GeminiModelCatalog.defaultModel.id,
            activeChatMode = ChatMode.fromId(prefs[KEY_CHAT_MODE] ?: ChatMode.DEVELOPER.id),
            customSystemPrompt = prefs[KEY_CUSTOM_PROMPT] ?: ChatMode.DEVELOPER.defaultSystemPrompt,
            generationParams = GenerationParameters(
                temperature = prefs[KEY_TEMPERATURE] ?: 0.7f,
                topP = prefs[KEY_TOP_P] ?: 0.95f,
                topK = prefs[KEY_TOP_K] ?: 40,
                maxOutputTokens = prefs[KEY_MAX_TOKENS] ?: 8192,
                frequencyPenalty = prefs[KEY_FREQ_PENALTY] ?: 0.0f,
                presencePenalty = prefs[KEY_PRESENCE_PENALTY] ?: 0.0f,
                stopSequences = prefs[KEY_STOP_SEQUENCES] ?: ""
            ),
            aceSettings = AceSettings(
                enabled = prefs[KEY_ACE_ENABLED] ?: true,
                autoRephrase = prefs[KEY_ACE_AUTO_REPHRASE] ?: false,
                requireManualApproval = prefs[KEY_ACE_MANUAL_APPROVAL] ?: true,
                autoRecovery = prefs[KEY_ACE_AUTO_RECOVERY] ?: true,
                autoCompressContext = prefs[KEY_ACE_AUTO_COMPRESS] ?: true,
                compressionThresholdPercent = prefs[KEY_ACE_COMPRESS_THRESHOLD] ?: 75
            ),
            githubProjectName = prefs[KEY_GH_PROJECT_NAME] ?: "GenDevSampleApp",
            githubPackageName = prefs[KEY_GH_PACKAGE_NAME] ?: "com.gendev.sampleapp",
            githubBranch = prefs[KEY_GH_BRANCH] ?: "main",
            githubPrivateRepo = prefs[KEY_GH_PRIVATE_REPO] ?: false,
            autoSyncModels = prefs[KEY_AUTO_SYNC_MODELS] ?: true,
            lastModelSyncEpochMs = prefs[KEY_LAST_MODEL_SYNC] ?: 0L
        )
    }

    suspend fun updateEngineMode(mode: EngineMode) {
        context.settingsDataStore.edit { it[KEY_ENGINE_MODE] = mode.name }
    }

    suspend fun updateModelId(modelId: String) {
        context.settingsDataStore.edit { it[KEY_MODEL_ID] = modelId }
    }

    suspend fun updateChatMode(mode: ChatMode, updateSystemPrompt: Boolean = true) {
        context.settingsDataStore.edit {
            it[KEY_CHAT_MODE] = mode.id
            if (updateSystemPrompt && mode != ChatMode.CUSTOM) {
                it[KEY_CUSTOM_PROMPT] = mode.defaultSystemPrompt
            }
        }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        context.settingsDataStore.edit { it[KEY_CUSTOM_PROMPT] = prompt }
    }

    suspend fun updateGenerationParams(params: GenerationParameters) {
        context.settingsDataStore.edit {
            it[KEY_TEMPERATURE] = params.temperature
            it[KEY_TOP_P] = params.topP
            it[KEY_TOP_K] = params.topK
            it[KEY_MAX_TOKENS] = params.maxOutputTokens
            it[KEY_FREQ_PENALTY] = params.frequencyPenalty
            it[KEY_PRESENCE_PENALTY] = params.presencePenalty
            it[KEY_STOP_SEQUENCES] = params.stopSequences
        }
    }

    suspend fun updateAceSettings(ace: AceSettings) {
        context.settingsDataStore.edit {
            it[KEY_ACE_ENABLED] = ace.enabled
            it[KEY_ACE_AUTO_REPHRASE] = ace.autoRephrase
            it[KEY_ACE_MANUAL_APPROVAL] = ace.requireManualApproval
            it[KEY_ACE_AUTO_RECOVERY] = ace.autoRecovery
            it[KEY_ACE_AUTO_COMPRESS] = ace.autoCompressContext
            it[KEY_ACE_COMPRESS_THRESHOLD] = ace.compressionThresholdPercent
        }
    }

    suspend fun updateGitHubProjectConfig(
        projectName: String,
        packageName: String,
        branch: String,
        isPrivate: Boolean
    ) {
        context.settingsDataStore.edit {
            it[KEY_GH_PROJECT_NAME] = projectName.trim().ifEmpty { "GenDevSampleApp" }
            it[KEY_GH_PACKAGE_NAME] = packageName.trim().ifEmpty { "com.gendev.sampleapp" }
            it[KEY_GH_BRANCH] = branch.trim().ifEmpty { "main" }
            it[KEY_GH_PRIVATE_REPO] = isPrivate
        }
    }

    suspend fun updateAutoSyncModels(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AUTO_SYNC_MODELS] = enabled }
    }

    suspend fun updateLastModelSync(epochMs: Long) {
        context.settingsDataStore.edit { it[KEY_LAST_MODEL_SYNC] = epochMs }
    }

    companion object {
        private val KEY_ENGINE_MODE = stringPreferencesKey("engine_mode")
        private val KEY_MODEL_ID = stringPreferencesKey("model_id")
        private val KEY_CHAT_MODE = stringPreferencesKey("chat_mode")
        private val KEY_CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
        private val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        private val KEY_TOP_P = floatPreferencesKey("top_p")
        private val KEY_TOP_K = intPreferencesKey("top_k")
        private val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        private val KEY_FREQ_PENALTY = floatPreferencesKey("freq_penalty")
        private val KEY_PRESENCE_PENALTY = floatPreferencesKey("presence_penalty")
        private val KEY_STOP_SEQUENCES = stringPreferencesKey("stop_sequences")
        private val KEY_ACE_ENABLED = booleanPreferencesKey("ace_enabled")
        private val KEY_ACE_AUTO_REPHRASE = booleanPreferencesKey("ace_auto_rephrase")
        private val KEY_ACE_MANUAL_APPROVAL = booleanPreferencesKey("ace_manual_approval")
        private val KEY_ACE_AUTO_RECOVERY = booleanPreferencesKey("ace_auto_recovery")
        private val KEY_ACE_AUTO_COMPRESS = booleanPreferencesKey("ace_auto_compress")
        private val KEY_ACE_COMPRESS_THRESHOLD = intPreferencesKey("ace_compress_threshold")
        private val KEY_GH_PROJECT_NAME = stringPreferencesKey("gh_project_name")
        private val KEY_GH_PACKAGE_NAME = stringPreferencesKey("gh_project_package_name")
        private val KEY_GH_BRANCH = stringPreferencesKey("gh_branch")
        private val KEY_GH_PRIVATE_REPO = booleanPreferencesKey("gh_private_repo")
        private val KEY_AUTO_SYNC_MODELS = booleanPreferencesKey("auto_sync_models")
        private val KEY_LAST_MODEL_SYNC = androidx.datastore.preferences.core.longPreferencesKey("last_model_sync")
    }
}
