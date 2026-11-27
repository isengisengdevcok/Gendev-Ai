package com.example.data.ai

import android.util.Log
import com.example.data.local.GeminiModelEntity
import com.example.model.EngineMode
import com.example.model.GeminiModelCatalog
import com.example.model.GeminiTier
import com.example.security.SecureVault
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "ModelRegistrySync"
private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com"
private const val FIREBASE_MODELS_DOC_URL = "https://firebase.google.com/docs/ai-logic/models.md.txt"

data class ModelSyncResult(
    val discoveredModels: List<GeminiModelEntity>,
    val sourceLabel: String
)

/**
 * Over-the-air dynamic Gemini model catalog updater.
 * Fetches newly released Gemini models without requiring an APK rebuild:
 * 1. If BYOK Gemini API Key or secure HTTPS proxy is configured, queries live `GET /v1beta/models`.
 * 2. Also queries the official Firebase AI Logic models index (`models.md.txt`) for keyless discovery.
 * Filters out deprecated legacy models (1.x/2.x) and image/embedding/TTS-only endpoints.
 */
class ModelRegistrySyncService(
    private val secureVault: SecureVault
) {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchLatestModels(engineMode: EngineMode): Result<ModelSyncResult> = withContext(Dispatchers.IO) {
        try {
            val discovered = linkedMapOf<String, GeminiModelEntity>()
            val sources = mutableListOf<String>()

            // 1. Live Google Gemini REST /v1beta/models discovery when BYOK API key or proxy is available
            val apiKey = secureVault.getGeminiApiKey()
            val proxyUrl = secureVault.getCustomProxyUrl()
            if (apiKey.isNotBlank() || proxyUrl.isNotBlank()) {
                runCatching { fetchFromGeminiRestApi(apiKey, proxyUrl) }
                    .onSuccess { list ->
                        list.forEach { discovered[it.id] = it }
                        if (list.isNotEmpty()) sources.add("Gemini API")
                    }
                    .onFailure { Log.w(TAG, "Gemini API model listing skipped: ${it.message}") }
            }

            // 2. Official Firebase AI Logic live model registry feed (works keyless!)
            runCatching { fetchFromFirebaseModelsFeed() }
                .onSuccess { list ->
                    list.forEach { entity ->
                        discovered.putIfAbsent(entity.id, entity)
                    }
                    if (list.isNotEmpty()) sources.add("Firebase AI Logic")
                }
                .onFailure { Log.w(TAG, "Firebase models feed sync skipped: ${it.message}") }

            if (discovered.isEmpty() && sources.isEmpty()) {
                return@withContext Result.failure(
                    IOException("Tidak dapat menjangkau server katalog model Gemini. Periksa koneksi internet.")
                )
            }

            Result.success(
                ModelSyncResult(
                    discoveredModels = discovered.values.toList(),
                    sourceLabel = sources.joinToString(" + ").ifBlank { "Official Registry" }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Dynamic model sync failed", e)
            Result.failure(e)
        }
    }

    private fun fetchFromGeminiRestApi(apiKey: String, proxyUrl: String): List<GeminiModelEntity> {
        val baseUrl = if (proxyUrl.isNotBlank()) {
            SecureVault.validateProxyUrl(proxyUrl).getOrThrow()
        } else {
            GEMINI_API_BASE
        }
        val reqBuilder = Request.Builder()
            .url("$baseUrl/v1beta/models?pageSize=100")
            .get()
        if (apiKey.isNotBlank()) {
            reqBuilder.header("x-goog-api-key", apiKey)
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Gemini Models API HTTP ${response.code}")
            }
            val arr = JSONObject(body).optJSONArray("models") ?: return emptyList()
            val results = mutableListOf<GeminiModelEntity>()
            val now = System.currentTimeMillis()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawName = obj.optString("name", "")
                val modelId = rawName.removePrefix("models/").trim()
                if (!isEligibleChatModelId(modelId)) continue

                val methods = obj.optJSONArray("supportedGenerationMethods")
                var supportsGenerate = false
                if (methods != null) {
                    for (j in 0 until methods.length()) {
                        if (methods.optString(j) == "generateContent") {
                            supportsGenerate = true
                            break
                        }
                    }
                }
                if (!supportsGenerate) continue

                val existing = GeminiModelCatalog.models.firstOrNull { it.id == modelId }
                val displayName = obj.optString("displayName").takeIf { it.isNotBlank() }
                    ?: existing?.displayName
                    ?: GeminiModelCatalog.prettifyModelName(modelId)
                val description = obj.optString("description").takeIf { it.isNotBlank() }
                    ?: existing?.tagline
                    ?: "Auto-discovered Google Gemini model ($modelId)"
                val inputLimit = obj.optInt("inputTokenLimit", existing?.contextWindowTokens ?: 1_048_576)
                    .coerceAtLeast(32_768)
                val outputLimit = obj.optInt("outputTokenLimit", existing?.maxOutputTokens ?: 65_536)
                    .coerceAtLeast(4_096)

                results.add(
                    GeminiModelEntity(
                        id = modelId,
                        displayName = displayName,
                        tierName = (existing?.tier ?: inferTier(modelId)).name,
                        tagline = description.take(140),
                        contextWindowTokens = inputLimit,
                        maxOutputTokens = outputLimit,
                        inputPricePerMillionUsd = existing?.inputPricePerMillionUsd ?: inferInputPrice(modelId),
                        outputPricePerMillionUsd = existing?.outputPricePerMillionUsd ?: inferOutputPrice(modelId),
                        recommendedFor = existing?.recommendedFor ?: "Auto-synced live from Google Gemini API",
                        isDynamic = existing == null,
                        supportsLegacySamplingParams = !modelId.startsWith("gemini-4") && !modelId.startsWith("gemini-5"),
                        sortOrder = inferSortOrder(modelId),
                        updatedAt = now
                    )
                )
            }
            return results
        }
    }

    private fun fetchFromFirebaseModelsFeed(): List<GeminiModelEntity> {
        val req = Request.Builder()
            .url(FIREBASE_MODELS_DOC_URL)
            .header("User-Agent", "GenDev-AI-Android/1.0")
            .get()
            .build()

        client.newCall(req).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful || text.isBlank()) {
                throw IOException("Firebase Models Feed HTTP ${response.code}")
            }
            // Extract active Gemini 3.x, 4.x, 5.x model identifiers from official Firebase AI Logic docs
            val regex = Regex("`?(gemini-(?:3|4|5)(?:\\.[0-9]+)?-[a-z0-9-]+)`?")
            val foundIds = regex.findAll(text)
                .map { it.groupValues[1].lowercase().trim() }
                .filter { isEligibleChatModelId(it) }
                .distinct()
                .toList()

            val now = System.currentTimeMillis()
            return foundIds.map { modelId ->
                val existing = GeminiModelCatalog.models.firstOrNull { it.id == modelId }
                GeminiModelEntity(
                    id = modelId,
                    displayName = existing?.displayName ?: GeminiModelCatalog.prettifyModelName(modelId),
                    tierName = (existing?.tier ?: inferTier(modelId)).name,
                    tagline = existing?.tagline ?: "Auto-synced official Firebase AI Logic Gemini model",
                    contextWindowTokens = existing?.contextWindowTokens ?: 1_048_576,
                    maxOutputTokens = existing?.maxOutputTokens ?: 65_536,
                    inputPricePerMillionUsd = existing?.inputPricePerMillionUsd ?: inferInputPrice(modelId),
                    outputPricePerMillionUsd = existing?.outputPricePerMillionUsd ?: inferOutputPrice(modelId),
                    recommendedFor = existing?.recommendedFor ?: "Synced over-the-air from official Firebase AI Logic catalog",
                    isDynamic = existing == null,
                    supportsLegacySamplingParams = !modelId.startsWith("gemini-4") && !modelId.startsWith("gemini-5"),
                    sortOrder = inferSortOrder(modelId),
                    updatedAt = now
                )
            }
        }
    }

    companion object {
        private val EXCLUDED_KEYWORDS = listOf(
            "embedding", "imagen", "image", "tts", "audio", "live", "veo",
            "aqa", "vision-only", "nano-banana", "robotics", "computer-use"
        )

        fun isEligibleChatModelId(rawId: String): Boolean {
            val id = rawId.lowercase().trim()
            if (!id.startsWith("gemini-")) return false
            // Exclude deprecated Gemini 1.x, 2.0, 2.5 models
            if (id.startsWith("gemini-1") ||
                id.startsWith("gemini-2") ||
                id == "gemini-pro" ||
                id == "gemini-pro-vision"
            ) return false
            if (EXCLUDED_KEYWORDS.any { id.contains(it) }) return false
            return Regex("^gemini-[3-9][a-z0-9.-]*$").matches(id)
        }

        fun inferTier(id: String): GeminiTier = when {
            id.contains("hydrogen") -> GeminiTier.HYDROGEN
            id.contains("neon") || id.contains("argon") || id.contains("carbon") || id.contains("pro") -> GeminiTier.PRO
            id.contains("lite") -> GeminiTier.LITE
            id.contains("flash") -> GeminiTier.FLASH
            else -> GeminiTier.DYNAMIC
        }

        fun inferSortOrder(id: String): Int = when {
            id == "gemini-4-hydrogen" -> 1
            id == "gemini-4-neon" -> 2
            id == "gemini-4-argon" -> 3
            id.startsWith("gemini-5") -> 4
            id.startsWith("gemini-4") -> 5
            id == "gemini-3.8-flash" -> 6
            id == "gemini-3.5-flash-lite" -> 7
            else -> 10
        }

        private fun inferInputPrice(id: String): Double = when {
            id.contains("neon") || id.contains("argon") || id.contains("carbon") || id.contains("pro") -> 1.25
            id.contains("lite") -> 0.10
            else -> 0.35
        }

        private fun inferOutputPrice(id: String): Double = when {
            id.contains("neon") || id.contains("argon") || id.contains("carbon") || id.contains("pro") -> 10.00
            id.contains("lite") -> 0.40
            else -> 2.50
        }
    }
}
