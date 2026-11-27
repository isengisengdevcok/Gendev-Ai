package com.example.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore (AES-256-GCM) vault for user-supplied BYOK Gemini API keys,
 * secure HTTPS proxy URLs, and GitHub Personal Access Tokens. Never logs raw secrets.
 */
class SecureVault(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun saveSecret(key: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, encrypt(trimmed)).apply()
        }
    }

    fun readSecret(key: String): String {
        val raw = prefs.getString(key, null) ?: return ""
        return decrypt(raw).orEmpty()
    }

    fun hasSecret(key: String): Boolean = readSecret(key).isNotBlank()

    fun maskSecret(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "Not configured"
        if (trimmed.length <= 8) return "••••••••"
        return "${trimmed.take(4)}••••••••${trimmed.takeLast(4)}"
    }

    fun saveGeminiApiKey(apiKey: String) = saveSecret(KEY_GEMINI_API, apiKey)
    fun getGeminiApiKey(): String = readSecret(KEY_GEMINI_API)

    fun saveCustomProxyUrl(url: String) = saveSecret(KEY_CUSTOM_PROXY_URL, url)
    fun getCustomProxyUrl(): String = readSecret(KEY_CUSTOM_PROXY_URL)

    fun saveGitHubToken(token: String) = saveSecret(KEY_GITHUB_TOKEN, token)
    fun getGitHubToken(): String = readSecret(KEY_GITHUB_TOKEN)

    fun saveGitHubOwner(owner: String) = saveSecret(KEY_GITHUB_OWNER, owner)
    fun getGitHubOwner(): String = readSecret(KEY_GITHUB_OWNER)

    fun saveGitHubRepo(repo: String) = saveSecret(KEY_GITHUB_REPO, repo)
    fun getGitHubRepo(): String = readSecret(KEY_GITHUB_REPO)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gendev_ai_aes_gcm_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val PREFS_NAME = "gendev_secure_vault"

        private const val KEY_GEMINI_API = "enc_gemini_api_key"
        private const val KEY_CUSTOM_PROXY_URL = "enc_custom_proxy_url"
        private const val KEY_GITHUB_TOKEN = "enc_github_pat"
        private const val KEY_GITHUB_OWNER = "enc_github_owner"
        private const val KEY_GITHUB_REPO = "enc_github_repo"

        /**
         * Validates that an optional custom backend proxy URL uses HTTPS and does not target
         * private, loopback, or cloud metadata endpoints.
         */
        fun validateProxyUrl(rawUrl: String): Result<String> {
            val trimmed = rawUrl.trim().trimEnd('/')
            if (trimmed.isEmpty()) return Result.success("")
            val uri = try {
                Uri.parse(trimmed)
            } catch (_: Exception) {
                return Result.failure(IllegalArgumentException("Invalid proxy URL format."))
            }
            val scheme = uri.scheme?.lowercase()
            if (scheme != "https") {
                return Result.failure(IllegalArgumentException("Proxy URL must use HTTPS for API key protection."))
            }
            val host = uri.host?.lowercase().orEmpty()
            if (host.isBlank()) {
                return Result.failure(IllegalArgumentException("Proxy URL must contain a valid hostname."))
            }
            val blocked = host == "localhost" ||
                host == "127.0.0.1" ||
                host == "::1" ||
                host == "0.0.0.0" ||
                host == "169.254.169.254" ||
                host.startsWith("10.") ||
                host.startsWith("192.168.") ||
                Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*").matches(host) ||
                host.endsWith(".local") ||
                host.endsWith(".internal")
            if (blocked) {
                return Result.failure(IllegalArgumentException("Private, loopback, and metadata addresses are blocked for security."))
            }
            return Result.success(trimmed)
        }
    }
}
