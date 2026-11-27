package com.example.data.ai

import android.util.Log
import com.example.model.AceDiagnosticBanner
import com.example.model.AceErrorType
import com.example.model.AceRephraseProposal
import com.example.model.AttachmentItem
import com.example.model.EngineMode
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import org.json.JSONObject

private const val TAG = "AceEngine"
private const val ACE_FAST_MODEL = "gemini-3.5-flash-lite"

/**
 * ACE - Adaptive Conversation Engine.
 * Handles:
 * 1. Error & false-positive / policy classification with transparent diagnostics.
 * 2. Gemini-powered Smart Rephrasing (clarifies ambiguity/format without altering user intent).
 * 3. Transparent Context Window Compression preserving system prompt, key facts, and recent turns.
 */
class AceEngine(
    private val geminiEngine: GeminiEngine
) {
    private val rephraseSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "needsRephrase" to Schema.boolean(
                    description = "True only if prompt is genuinely ambiguous, malformed, or structurally unclear"
                ),
                "reason" to Schema.string(
                    description = "Concise explanation of why rephrasing helps clarity or format"
                ),
                "improvedPrompt" to Schema.string(
                    description = "Clarified prompt preserving exact user intent and language"
                )
            )
        )
    }

    suspend fun evaluateSmartRephrase(
        engineMode: EngineMode,
        originalPrompt: String,
        recentHistory: List<ChatTurn>,
        attachments: List<AttachmentItem>
    ): AceRephraseProposal? {
        if (originalPrompt.trim().length < 6) return null
        return try {
            val contextHint = recentHistory.takeLast(4).joinToString("\n") {
                "${it.role}: ${it.text.take(240)}"
            }
            val system = """
                You are ACE (Adaptive Conversation Engine) prompt clarity analyzer.
                Analyze the user's draft prompt for formatting issues, severe ambiguity, or missing technical structure.
                NEVER censor, alter, or refuse the user's intent. Preserve the user's language (Indonesian/English/etc.).
                Only set needsRephrase=true if improving structure or clarity materially improves Gemini output quality.
                Do not attempt to disguise or bypass any provider safety policies.
            """.trimIndent()
            val input = buildString {
                if (contextHint.isNotBlank()) {
                    appendLine("Recent conversation context:")
                    appendLine(contextHint)
                    appendLine()
                }
                appendLine("User draft prompt:")
                append(originalPrompt)
            }

            val result = geminiEngine.generateStructuredJson(
                engineMode = engineMode,
                modelId = ACE_FAST_MODEL,
                systemPrompt = system,
                userPrompt = input,
                schema = rephraseSchema,
                jsonSchemaHint = """{"needsRephrase": boolean, "reason": string, "improvedPrompt": string}"""
            )
            val json = JSONObject(result.jsonText)
            val needs = json.optBoolean("needsRephrase", false)
            val reason = json.optString("reason", "Clarified structure for higher precision.")
            val improved = json.optString("improvedPrompt", originalPrompt).trim()

            if (needs && improved.isNotBlank() && improved != originalPrompt.trim()) {
                AceRephraseProposal(
                    originalPrompt = originalPrompt,
                    improvedPrompt = improved,
                    reason = reason,
                    attachments = attachments
                )
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "ACE rephrase analysis skipped due to error: ${e.localizedMessage}")
            null
        }
    }

    suspend fun compressContext(
        engineMode: EngineMode,
        existingSummary: String,
        turnsToCompress: List<ChatTurn>
    ): String {
        if (turnsToCompress.isEmpty()) return existingSummary
        val transcript = turnsToCompress.joinToString("\n\n") {
            "[${it.role.uppercase()}]: ${it.text.take(1600)}"
        }
        val prompt = buildString {
            if (existingSummary.isNotBlank()) {
                appendLine("Previous compressed memory:")
                appendLine(existingSummary)
                appendLine()
            }
            appendLine("Compress the following conversation turns into a dense, factual technical memory.")
            appendLine("Preserve all user goals, code decisions, file paths, variable names, constraints, and unresolved questions:")
            appendLine(transcript)
        }
        val (summary, _) = geminiEngine.generateSimpleText(
            engineMode = engineMode,
            modelId = ACE_FAST_MODEL,
            systemPrompt = "You are a lossless technical context compressor. Keep bullet points concise and accurate.",
            userPrompt = prompt,
            temperature = 0.15f,
            maxTokens = 1024
        )
        return summary.trim()
    }

    fun classifyError(error: Throwable): AceDiagnosticBanner {
        val msg = error.localizedMessage.orEmpty()
        val lower = msg.lowercase()

        return when {
            error is TimeoutCancellationException || lower.contains("timeout") -> AceDiagnosticBanner(
                errorType = AceErrorType.TIMEOUT,
                message = "Permintaan melebihi batas waktu 60 detik.",
                providerReason = msg.ifBlank { "Inference timeout" },
                suggestedAction = "Coba ulangi permintaan atau gunakan Gemini 3.8 Flash untuk latensi lebih rendah.",
                canRetry = true
            )

            lower.contains("429") || lower.contains("rate limit") || lower.contains("quota") || lower.contains("resource_exhausted") -> AceDiagnosticBanner(
                errorType = AceErrorType.RATE_LIMIT,
                message = "Batas kuota / rate limit dari penyedia API Gemini tercapai.",
                providerReason = msg,
                suggestedAction = "Tunggu beberapa detik lalu coba lagi, atau beralih ke model Gemini 3.5 Flash-Lite / BYOK key berbayar.",
                canRetry = true
            )

            lower.contains("403") || lower.contains("401") || lower.contains("app check") || lower.contains("api key") || lower.contains("permission_denied") -> AceDiagnosticBanner(
                errorType = AceErrorType.AUTH_ERROR,
                message = "Autentikasi API Gemini atau Firebase App Check ditolak.",
                providerReason = msg,
                suggestedAction = "Periksa status Firebase AI Logic atau validitas Gemini API Key di menu Gemini API.",
                canRetry = false
            )

            lower.contains("context") || lower.contains("token") && lower.contains("exceed") || lower.contains("too long") -> AceDiagnosticBanner(
                errorType = AceErrorType.CONTEXT_OVERFLOW,
                message = "Panjang percakapan mendekati atau melewati batas context window.",
                providerReason = msg,
                suggestedAction = "Aktifkan ACE Context Compression untuk meringkas riwayat lama secara otomatis.",
                canRetry = true
            )

            lower.contains("safety") || lower.contains("blocked") || lower.contains("policy") || lower.contains("recitation") -> AceDiagnosticBanner(
                errorType = AceErrorType.PROVIDER_POLICY_BLOCK,
                message = "Penyedia API membatasi respons berdasarkan kebijakan keselamatan model.",
                providerReason = msg,
                suggestedAction = "GenDev AI tidak menambahkan filter internal dan tidak menyamarkan permintaan. Coba bingkai pertanyaan dengan konteks riset akademis atau arsitektur defensif yang jelas.",
                canRetry = false
            )

            lower.contains("400") || lower.contains("invalid_argument") || lower.contains("bad request") -> AceDiagnosticBanner(
                errorType = AceErrorType.INVALID_FORMAT,
                message = "Format permintaan atau parameter tidak kompatibel dengan model yang dipilih.",
                providerReason = msg,
                suggestedAction = "Periksa parameter Temperature/Top-P/Max Tokens atau gunakan ACE Smart Rephrase.",
                canRetry = true
            )

            lower.contains("unable to resolve host") || lower.contains("network") || lower.contains("socket") || lower.contains("connect") -> AceDiagnosticBanner(
                errorType = AceErrorType.NETWORK_TRANSIENT,
                message = "Gangguan koneksi jaringan sementara terdeteksi.",
                providerReason = msg,
                suggestedAction = "Periksa koneksi internet perangkat lalu tekan Retry.",
                canRetry = true
            )

            else -> AceDiagnosticBanner(
                errorType = AceErrorType.UNKNOWN,
                message = "Terjadi kendala teknis saat berkomunikasi dengan Gemini.",
                providerReason = msg.ifBlank { error::class.java.simpleName },
                suggestedAction = "Tekan Retry untuk pemulihan otomatis ACE.",
                canRetry = true
            )
        }
    }

    fun isTransientRecoverable(type: AceErrorType): Boolean =
        type == AceErrorType.NETWORK_TRANSIENT ||
            type == AceErrorType.RATE_LIMIT ||
            type == AceErrorType.TIMEOUT
}
