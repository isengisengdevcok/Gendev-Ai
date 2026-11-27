package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.model.AttachmentItem
import com.example.model.AttachmentType
import com.example.model.EngineMode
import com.example.model.GenerationParameters
import com.example.security.SecureVault
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiEngine"
private const val DEFAULT_TIMEOUT_MS = 60_000L
private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com"

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)

data class StreamChunk(
    val deltaText: String,
    val promptTokens: Int = 0,
    val outputTokens: Int = 0,
    val finishReason: String? = null
)

data class StructuredResult(
    val jsonText: String,
    val promptTokens: Int,
    val outputTokens: Int
)

/**
 * Dual-mode Google Gemini engine:
 * 1. FIREBASE_AI_LOGIC: Built-in keyless Gemini via Firebase AI Logic + App Check (default).
 * 2. BYOK_GEMINI_API: User's own paid Gemini API key or secure HTTPS backend proxy via OkHttp SSE.
 */
class GeminiEngine(
    private val context: Context,
    private val secureVault: SecureVault
) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildFirebaseModel(
        modelId: String,
        systemPrompt: String,
        params: GenerationParameters,
        responseMimeType: String? = null,
        responseSchema: Schema? = null
    ): GenerativeModel {
        val config = generationConfig {
            temperature = params.temperature
            topP = params.topP
            topK = params.topK
            maxOutputTokens = params.maxOutputTokens
            val stops = params.parsedStopSequences()
            if (stops.isNotEmpty()) {
                stopSequences = stops
            }
            if (params.presencePenalty != 0.0f) {
                presencePenalty = params.presencePenalty
            }
            if (params.frequencyPenalty != 0.0f) {
                frequencyPenalty = params.frequencyPenalty
            }
            if (!responseMimeType.isNullOrBlank()) {
                this.responseMimeType = responseMimeType
            }
            if (responseSchema != null) {
                this.responseSchema = responseSchema
            }
        }
        val sysContent: Content? = if (systemPrompt.isNotBlank()) {
            content { text(systemPrompt) }
        } else null

        return Firebase.ai.generativeModel(
            modelName = modelId,
            generationConfig = config,
            systemInstruction = sysContent
        )
    }

    fun streamChat(
        engineMode: EngineMode,
        modelId: String,
        systemPrompt: String,
        history: List<ChatTurn>,
        userPrompt: String,
        attachments: List<AttachmentItem>,
        params: GenerationParameters
    ): Flow<StreamChunk> = flow {
        when (engineMode) {
            EngineMode.FIREBASE_AI_LOGIC -> {
                val model = buildFirebaseModel(modelId, systemPrompt, params)
                val firebaseHistory = history.map { turn ->
                    content(role = turn.role) { text(turn.text) }
                }
                val loadedImages = loadScaledBitmaps(attachments)
                val combinedPrompt = buildPromptWithDocs(userPrompt, attachments)
                val userContent = content(role = "user") {
                    loadedImages.forEach { bitmap -> image(bitmap) }
                    text(combinedPrompt)
                }

                try {
                    withTimeout(DEFAULT_TIMEOUT_MS) {
                        val chat = model.startChat(history = firebaseHistory)
                        chat.sendMessageStream(userContent).collect { chunk ->
                            val delta = chunk.text.orEmpty()
                            val pTokens = chunk.usageMetadata?.promptTokenCount ?: 0
                            val cTokens = chunk.usageMetadata?.candidatesTokenCount ?: 0
                            val finish = chunk.candidates.firstOrNull()?.finishReason?.name
                            emit(
                                StreamChunk(
                                    deltaText = delta,
                                    promptTokens = pTokens,
                                    outputTokens = cTokens,
                                    finishReason = finish
                                )
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase AI Logic stream failed for $modelId", e)
                    throw e
                }
            }

            EngineMode.BYOK_GEMINI_API -> {
                streamByokGeminiApi(
                    modelId = modelId,
                    systemPrompt = systemPrompt,
                    history = history,
                    userPrompt = userPrompt,
                    attachments = attachments,
                    params = params
                ).collect { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateStructuredJson(
        engineMode: EngineMode,
        modelId: String,
        systemPrompt: String,
        userPrompt: String,
        schema: Schema,
        jsonSchemaHint: String
    ): StructuredResult = withContext(Dispatchers.IO) {
        val params = GenerationParameters(
            temperature = 0.25f,
            topP = 0.9f,
            maxOutputTokens = 8192
        )
        try {
            when (engineMode) {
                EngineMode.FIREBASE_AI_LOGIC -> {
                    val model = buildFirebaseModel(
                        modelId = modelId,
                        systemPrompt = systemPrompt,
                        params = params,
                        responseMimeType = "application/json",
                        responseSchema = schema
                    )
                    val response = withTimeout(DEFAULT_TIMEOUT_MS) {
                        model.generateContent(userPrompt)
                    }
                    val text = response.text ?: throw IllegalStateException("Empty structured JSON response.")
                    StructuredResult(
                        jsonText = text,
                        promptTokens = response.usageMetadata?.promptTokenCount ?: estimateTokens(userPrompt),
                        outputTokens = response.usageMetadata?.candidatesTokenCount ?: estimateTokens(text)
                    )
                }

                EngineMode.BYOK_GEMINI_API -> {
                    generateByokJson(
                        modelId = modelId,
                        systemPrompt = "$systemPrompt\nReturn ONLY raw valid JSON matching this schema:\n$jsonSchemaHint",
                        userPrompt = userPrompt,
                        params = params
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Structured JSON call timed out", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Structured JSON call failed", e)
            throw e
        }
    }

    suspend fun generateSimpleText(
        engineMode: EngineMode,
        modelId: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.3f,
        maxTokens: Int = 2048
    ): Pair<String, Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val params = GenerationParameters(
            temperature = temperature,
            maxOutputTokens = maxTokens
        )
        when (engineMode) {
            EngineMode.FIREBASE_AI_LOGIC -> {
                val model = buildFirebaseModel(modelId, systemPrompt, params)
                val response = withTimeout(DEFAULT_TIMEOUT_MS) {
                    model.generateContent(userPrompt)
                }
                val text = response.text.orEmpty()
                val inTok = response.usageMetadata?.promptTokenCount ?: estimateTokens(userPrompt)
                val outTok = response.usageMetadata?.candidatesTokenCount ?: estimateTokens(text)
                text to (inTok to outTok)
            }

            EngineMode.BYOK_GEMINI_API -> {
                val sb = StringBuilder()
                var inTok = 0
                var outTok = 0
                streamByokGeminiApi(
                    modelId = modelId,
                    systemPrompt = systemPrompt,
                    history = emptyList(),
                    userPrompt = userPrompt,
                    attachments = emptyList(),
                    params = params
                ).collect { chunk ->
                    sb.append(chunk.deltaText)
                    if (chunk.promptTokens > 0) inTok = chunk.promptTokens
                    if (chunk.outputTokens > 0) outTok = chunk.outputTokens
                }
                val text = sb.toString()
                text to ((if (inTok > 0) inTok else estimateTokens(userPrompt)) to
                    (if (outTok > 0) outTok else estimateTokens(text)))
            }
        }
    }

    suspend fun testConnection(
        engineMode: EngineMode,
        modelId: String
    ): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val (reply, tokens) = generateSimpleText(
                engineMode = engineMode,
                modelId = modelId,
                systemPrompt = "Respond briefly with OK and model readiness.",
                userPrompt = "Ping check: reply with 'GenDev Gemini Ready'.",
                temperature = 0.1f,
                maxTokens = 64
            )
            val latency = System.currentTimeMillis() - start
            val summary = "Connected to $modelId (${reply.trim().take(48)}) • ${tokens.first + tokens.second} tok"
            Result.success(summary to latency)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }

    private fun streamByokGeminiApi(
        modelId: String,
        systemPrompt: String,
        history: List<ChatTurn>,
        userPrompt: String,
        attachments: List<AttachmentItem>,
        params: GenerationParameters
    ): Flow<StreamChunk> = flow {
        val apiKey = secureVault.getGeminiApiKey()
        val proxyUrl = secureVault.getCustomProxyUrl()
        if (apiKey.isBlank() && proxyUrl.isBlank()) {
            throw IllegalStateException(
                "BYOK mode requires a Gemini API key or a verified HTTPS proxy URL in Gemini API settings."
            )
        }
        val baseUrl = if (proxyUrl.isNotBlank()) {
            SecureVault.validateProxyUrl(proxyUrl).getOrThrow()
        } else {
            GEMINI_API_BASE
        }
        val endpoint = "$baseUrl/v1beta/models/$modelId:streamGenerateContent?alt=sse"
        val payload = buildByokRequestJson(
            systemPrompt = systemPrompt,
            history = history,
            userPrompt = userPrompt,
            attachments = attachments,
            params = params,
            jsonMode = false
        )

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")

        if (apiKey.isNotBlank()) {
            // Pass API key via header rather than URL query string to prevent proxy/server URL log exposure.
            requestBuilder.header("x-goog-api-key", apiKey)
        }

        val call = httpClient.newCall(requestBuilder.build())
        call.execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty().take(500)
                throw IOException("HTTP ${response.code}: ${extractGeminiErrorMessage(errBody, response.message)}")
            }
            val source = response.body?.source() ?: throw IOException("Empty streaming response body.")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty() || data == "[DONE]") continue

                val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
                val usage = json.optJSONObject("usageMetadata")
                val pTok = usage?.optInt("promptTokenCount", 0) ?: 0
                val cTok = usage?.optInt("candidatesTokenCount", 0) ?: 0

                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val finishReason = firstCandidate?.optString("finishReason")?.takeIf { it.isNotBlank() }
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                        textBuilder.append(partText)
                    }
                }
                emit(
                    StreamChunk(
                        deltaText = textBuilder.toString(),
                        promptTokens = pTok,
                        outputTokens = cTok,
                        finishReason = finishReason
                    )
                )
            }
        }
    }

    private fun generateByokJson(
        modelId: String,
        systemPrompt: String,
        userPrompt: String,
        params: GenerationParameters
    ): StructuredResult {
        val apiKey = secureVault.getGeminiApiKey()
        val proxyUrl = secureVault.getCustomProxyUrl()
        if (apiKey.isBlank() && proxyUrl.isBlank()) {
            throw IllegalStateException("BYOK mode requires a Gemini API key or verified HTTPS proxy.")
        }
        val baseUrl = if (proxyUrl.isNotBlank()) {
            SecureVault.validateProxyUrl(proxyUrl).getOrThrow()
        } else {
            GEMINI_API_BASE
        }
        val endpoint = "$baseUrl/v1beta/models/$modelId:generateContent"
        val payload = buildByokRequestJson(
            systemPrompt = systemPrompt,
            history = emptyList(),
            userPrompt = userPrompt,
            attachments = emptyList(),
            params = params,
            jsonMode = true
        )

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (apiKey.isNotBlank()) {
            requestBuilder.header("x-goog-api-key", apiKey)
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${extractGeminiErrorMessage(body, response.message)}")
            }
            val json = JSONObject(body)
            val usage = json.optJSONObject("usageMetadata")
            val text = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
            if (text.isBlank()) throw IllegalStateException("Empty JSON response from Gemini API.")
            return StructuredResult(
                jsonText = text,
                promptTokens = usage?.optInt("promptTokenCount", estimateTokens(userPrompt))
                    ?: estimateTokens(userPrompt),
                outputTokens = usage?.optInt("candidatesTokenCount", estimateTokens(text))
                    ?: estimateTokens(text)
            )
        }
    }

    private fun buildByokRequestJson(
        systemPrompt: String,
        history: List<ChatTurn>,
        userPrompt: String,
        attachments: List<AttachmentItem>,
        params: GenerationParameters,
        jsonMode: Boolean
    ): JSONObject {
        val root = JSONObject()
        if (systemPrompt.isNotBlank()) {
            root.put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemPrompt))
                )
            )
        }

        val contents = JSONArray()
        history.forEach { turn ->
            contents.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
            )
        }

        val userParts = JSONArray()
        loadScaledBitmaps(attachments).forEach { bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            userParts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", b64)
                )
            )
        }
        userParts.put(JSONObject().put("text", buildPromptWithDocs(userPrompt, attachments)))
        contents.put(JSONObject().put("role", "user").put("parts", userParts))
        root.put("contents", contents)

        val genCfg = JSONObject()
            .put("temperature", params.temperature.toDouble())
            .put("topP", params.topP.toDouble())
            .put("topK", params.topK)
            .put("maxOutputTokens", params.maxOutputTokens)
        if (params.frequencyPenalty != 0.0f) {
            genCfg.put("frequencyPenalty", params.frequencyPenalty.toDouble())
        }
        if (params.presencePenalty != 0.0f) {
            genCfg.put("presencePenalty", params.presencePenalty.toDouble())
        }
        val stops = params.parsedStopSequences()
        if (stops.isNotEmpty()) {
            genCfg.put("stopSequences", JSONArray(stops))
        }
        if (jsonMode) {
            genCfg.put("responseMimeType", "application/json")
        }
        root.put("generationConfig", genCfg)
        return root
    }

    private fun buildPromptWithDocs(userPrompt: String, attachments: List<AttachmentItem>): String {
        val docs = attachments.filter { it.type == AttachmentType.DOCUMENT }
        if (docs.isEmpty()) return userPrompt
        val sb = StringBuilder()
        docs.forEach { doc ->
            sb.appendLine("[Attached Document: ${doc.displayName} (${doc.mimeType})]")
            sb.appendLine(doc.extractedText?.take(24_000) ?: "(Binary document attached)")
            sb.appendLine("[End Document]")
            sb.appendLine()
        }
        sb.append(userPrompt)
        return sb.toString()
    }

    private fun loadScaledBitmaps(attachments: List<AttachmentItem>): List<Bitmap> {
        return attachments
            .filter { it.type == AttachmentType.IMAGE }
            .mapNotNull { item ->
                runCatching {
                    val uri = Uri.parse(item.uriString)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream) ?: return@use null
                        scaleBitmapIfNeeded(original, maxDimension = 1536)
                    }
                }.getOrNull()
            }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (width >= height) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun extractGeminiErrorMessage(rawBody: String, fallback: String): String {
        return runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
    }

    companion object {
        fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)
    }
}
