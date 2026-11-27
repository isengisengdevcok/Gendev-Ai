package com.example.model

enum class GeminiTier(val label: String) {
    PRO("PRO TIER"),
    FLASH("FLASH"),
    LITE("FLASH-LITE")
}

data class GeminiModelSpec(
    val id: String,
    val displayName: String,
    val tier: GeminiTier,
    val tagline: String,
    val contextWindowTokens: Int,
    val maxOutputTokens: Int,
    val inputPricePerMillionUsd: Double,
    val outputPricePerMillionUsd: Double,
    val supportsVision: Boolean = true,
    val supportsDocuments: Boolean = true,
    val recommendedFor: String
)

object GeminiModelCatalog {
    val models: List<GeminiModelSpec> = listOf(
        GeminiModelSpec(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            tier = GeminiTier.FLASH,
            tagline = "Frontier multimodal research & rapid coding engine (Default)",
            contextWindowTokens = 1_048_576,
            maxOutputTokens = 65_536,
            inputPricePerMillionUsd = 0.30,
            outputPricePerMillionUsd = 2.50,
            recommendedFor = "Balanced research, fast Android Compose prototyping, multimodal analysis"
        ),
        GeminiModelSpec(
            id = "gemini-4-argon",
            displayName = "Gemini 4 Argon (Pro)",
            tier = GeminiTier.PRO,
            tagline = "Flagship Pro reasoning, deep architecture & complex Android engineering",
            contextWindowTokens = 2_097_152,
            maxOutputTokens = 65_536,
            inputPricePerMillionUsd = 1.25,
            outputPricePerMillionUsd = 10.00,
            recommendedFor = "Complex multi-file Android builds, deep research, STEM & system design"
        ),
        GeminiModelSpec(
            id = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash-Lite",
            tier = GeminiTier.LITE,
            tagline = "Ultra-low latency high-volume model for rapid experiments & ACE",
            contextWindowTokens = 1_048_576,
            maxOutputTokens = 32_768,
            inputPricePerMillionUsd = 0.10,
            outputPricePerMillionUsd = 0.40,
            recommendedFor = "High-throughput testing, ACE smart rephrasing & context compression"
        )
    )

    val defaultModel: GeminiModelSpec = models.first()

    fun findById(id: String): GeminiModelSpec =
        models.firstOrNull { it.id == id } ?: defaultModel

    fun estimateCostUsd(modelId: String, inputTokens: Int, outputTokens: Int): Double {
        val spec = findById(modelId)
        val inCost = (inputTokens.toDouble() / 1_000_000.0) * spec.inputPricePerMillionUsd
        val outCost = (outputTokens.toDouble() / 1_000_000.0) * spec.outputPricePerMillionUsd
        return inCost + outCost
    }
}

enum class EngineMode(val label: String, val subtitle: String) {
    FIREBASE_AI_LOGIC(
        label = "Firebase AI Logic (Built-in)",
        subtitle = "Keyless Gemini execution protected by Firebase App Check"
    ),
    BYOK_GEMINI_API(
        label = "Gemini API Key (BYOK / Proxy)",
        subtitle = "Direct paid Gemini API key or secure HTTPS backend proxy"
    )
}

enum class ChatMode(
    val id: String,
    val label: String,
    val shortBadge: String,
    val defaultSystemPrompt: String
) {
    NORMAL(
        id = "normal",
        label = "Normal Chat",
        shortBadge = "NORMAL",
        defaultSystemPrompt = "You are GenDev AI, a precise, helpful, and transparent AI assistant powered by Google Gemini. Respond clearly in the user's language without unnecessary disclaimers."
    ),
    RESEARCH(
        id = "research",
        label = "Research Mode",
        shortBadge = "RESEARCH",
        defaultSystemPrompt = "You are GenDev AI in Research Mode. Provide rigorous, structured analysis, cite trade-offs, explore multiple hypotheses objectively, and present reproducible methodologies."
    ),
    DEVELOPER(
        id = "developer",
        label = "Developer Mode",
        shortBadge = "ANDROID DEV",
        defaultSystemPrompt = "You are GenDev AI in Senior Android Engineer Mode. Focus on production-grade Kotlin, Jetpack Compose, Material 3, MVVM, Room, Coroutines/Flow, and Gradle Kotlin DSL. Provide complete, error-free code blocks with explicit file paths."
    ),
    EXPERIMENTAL(
        id = "experimental",
        label = "Experimental Mode",
        shortBadge = "LAB",
        defaultSystemPrompt = "You are GenDev AI in Experimental Sandbox Mode. Help the user test prompts, benchmark edge cases, prototype architectures, and analyze model behavior objectively and transparently."
    ),
    ROLEPLAY(
        id = "roleplay",
        label = "Roleplay & Simulation",
        shortBadge = "SIM",
        defaultSystemPrompt = "You are GenDev AI in Simulation & Scenario Modeling Mode. Maintain consistent persona, domain context, and interactive scenario state as directed by the user."
    ),
    CUSTOM(
        id = "custom",
        label = "Custom System Prompt",
        shortBadge = "CUSTOM",
        defaultSystemPrompt = ""
    );

    companion object {
        fun fromId(id: String): ChatMode = entries.firstOrNull { it.id == id } ?: DEVELOPER
    }
}

data class GenerationParameters(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 8192,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopSequences: String = ""
) {
    fun parsedStopSequences(): List<String> =
        stopSequences.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(5)
}

data class AceSettings(
    val enabled: Boolean = true,
    val autoRephrase: Boolean = false,
    val requireManualApproval: Boolean = true,
    val autoRecovery: Boolean = true,
    val autoCompressContext: Boolean = true,
    val compressionThresholdPercent: Int = 75
)

enum class AceErrorType(val title: String) {
    RATE_LIMIT("Rate Limit / Quota (429)"),
    NETWORK_TRANSIENT("Network Interruption"),
    TIMEOUT("Inference Timeout"),
    CONTEXT_OVERFLOW("Context Window Limit"),
    INVALID_FORMAT("Invalid Request Format"),
    PROVIDER_POLICY_BLOCK("Provider Safety Policy"),
    AUTH_ERROR("Authentication / App Check"),
    UNKNOWN("API Execution Error")
}

data class AceRephraseProposal(
    val originalPrompt: String,
    val improvedPrompt: String,
    val reason: String,
    val attachments: List<AttachmentItem> = emptyList()
)

data class AceDiagnosticBanner(
    val errorType: AceErrorType,
    val message: String,
    val providerReason: String? = null,
    val suggestedAction: String? = null,
    val canRetry: Boolean = true
)

enum class AttachmentType { IMAGE, DOCUMENT }

data class AttachmentItem(
    val id: String,
    val type: AttachmentType,
    val displayName: String,
    val uriString: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val extractedText: String? = null
)

data class GitHubWorkflowRunInfo(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val htmlUrl: String,
    val updatedAt: String
)

data class GitHubReleaseAssetInfo(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val publishedAt: String,
    val assets: List<GitHubReleaseAssetInfo>
)

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Success(val details: String, val latencyMs: Long) : ConnectionTestState
    data class Failure(val error: String) : ConnectionTestState
}

sealed interface GitHubSyncState {
    data object Idle : GitHubSyncState
    data class InProgress(val step: String) : GitHubSyncState
    data class Success(val repoUrl: String, val actionsUrl: String, val commitSha: String) : GitHubSyncState
    data class Failure(val message: String) : GitHubSyncState
}
