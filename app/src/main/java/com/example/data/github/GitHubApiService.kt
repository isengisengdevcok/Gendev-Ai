package com.example.data.github

import android.util.Base64
import android.util.Log
import com.example.data.local.ProjectFileEntity
import com.example.model.GitHubReleaseAssetInfo
import com.example.model.GitHubReleaseInfo
import com.example.model.GitHubWorkflowRunInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "GitHubApiService"
private const val GITHUB_API = "https://api.github.com"

data class GitHubUserInfo(
    val login: String,
    val name: String,
    val htmlUrl: String
)

data class GitHubPushOutcome(
    val repoUrl: String,
    val actionsUrl: String,
    val releasesUrl: String,
    val commitSha: String,
    val pushedFileCount: Int
)

/**
 * Real GitHub REST API v3 client supporting:
 * - Token verification (GET /user)
 * - Automatic repo lookup/creation (POST /user/repos)
 * - Single atomic multi-file commit via Git Data API (blobs -> tree -> commit -> ref)
 * - GitHub Actions workflow monitoring & manual dispatch
 * - GitHub Releases APK asset discovery
 *
 * Security: Tokens are passed only in Authorization Bearer headers over HTTPS and are never logged.
 */
class GitHubApiService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun verifyToken(token: String): Result<GitHubUserInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            if (cleanToken.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("GitHub Personal Access Token is empty."))
            }
            val json = executeJsonRequest(
                method = "GET",
                url = "$GITHUB_API/user",
                token = cleanToken
            )
            Result.success(
                GitHubUserInfo(
                    login = json.getString("login"),
                    name = json.optString("name", json.getString("login")).ifBlank { json.getString("login") },
                    htmlUrl = json.optString("html_url", "https://github.com/${json.getString("login")}")
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "GitHub token verification failed")
            Result.failure(e)
        }
    }

    suspend fun pushProjectAtomically(
        token: String,
        owner: String,
        repoName: String,
        branch: String,
        isPrivate: Boolean,
        commitMessage: String,
        files: List<ProjectFileEntity>,
        onProgress: (String) -> Unit
    ): Result<GitHubPushOutcome> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val cleanRepo = sanitizeRepoName(repoName)
            if (cleanToken.isEmpty()) throw IllegalArgumentException("GitHub Token (PAT) belum diisi.")
            if (cleanRepo.isEmpty()) throw IllegalArgumentException("Nama repository GitHub tidak valid.")
            if (files.isEmpty()) throw IllegalArgumentException("Tidak ada file proyek untuk di-push.")

            onProgress("Memverifikasi akun GitHub...")
            val user = verifyToken(cleanToken).getOrThrow()
            val resolvedOwner = owner.trim().ifEmpty { user.login }

            onProgress("Memeriksa repository $resolvedOwner/$cleanRepo...")
            val repoExists = checkRepoExists(cleanToken, resolvedOwner, cleanRepo)
            var activeBranch = branch.trim().ifEmpty { "main" }

            if (!repoExists) {
                onProgress("Membuat repository baru $resolvedOwner/$cleanRepo...")
                val createBody = JSONObject()
                    .put("name", cleanRepo)
                    .put("description", "Android project generated and synced by GenDev AI (Gemini Studio)")
                    .put("private", isPrivate)
                    .put("auto_init", true)
                val created = executeJsonRequest(
                    method = "POST",
                    url = "$GITHUB_API/user/repos",
                    token = cleanToken,
                    body = createBody
                )
                activeBranch = created.optString("default_branch", "main").ifBlank { "main" }
                // Brief settle delay for GitHub initial commit creation
                delay(1500)
            } else {
                val repoInfo = executeJsonRequest(
                    method = "GET",
                    url = "$GITHUB_API/repos/$resolvedOwner/$cleanRepo",
                    token = cleanToken
                )
                val defaultBranch = repoInfo.optString("default_branch", activeBranch)
                if (defaultBranch.isNotBlank()) activeBranch = defaultBranch
            }

            onProgress("Mengambil referensi branch $activeBranch...")
            val refJson = getOrBootstrapBranchRef(cleanToken, resolvedOwner, cleanRepo, activeBranch)
            val baseCommitSha = refJson.getJSONObject("object").getString("sha")

            val baseCommitJson = executeJsonRequest(
                method = "GET",
                url = "$GITHUB_API/repos/$resolvedOwner/$cleanRepo/git/commits/$baseCommitSha",
                token = cleanToken
            )
            val baseTreeSha = baseCommitJson.getJSONObject("tree").getString("sha")

            val treeEntries = JSONArray()
            files.forEachIndexed { index, file ->
                val cleanPath = file.path.trim().trimStart('/')
                if (cleanPath.isBlank() || cleanPath.contains("..")) return@forEachIndexed
                onProgress("Mengunggah source (${index + 1}/${files.size}): $cleanPath")
                val blobPayload = JSONObject()
                    .put("content", file.content)
                    .put("encoding", "utf-8")
                val blobRes = executeJsonRequest(
                    method = "POST",
                    url = "$GITHUB_API/repos/$resolvedOwner/$cleanRepo/git/blobs",
                    token = cleanToken,
                    body = blobPayload
                )
                val blobSha = blobRes.getString("sha")
                treeEntries.put(
                    JSONObject()
                        .put("path", cleanPath)
                        .put("mode", "100644")
                        .put("type", "blob")
                        .put("sha", blobSha)
                )
            }

            onProgress("Menyusun atomic Git tree & workflow CI/CD...")
            val treeRes = executeJsonRequest(
                method = "POST",
                url = "$GITHUB_API/repos/$resolvedOwner/$cleanRepo/git/trees",
                token = cleanToken,
                body = JSONObject()
                    .put("base_tree", baseTreeSha)
                    .put("tree", treeEntries)
            )
            val newTreeSha = treeRes.getString("sha")

            onProgress("Membuat commit sinkronisasi...")
            val commitRes = executeJsonRequest(
                method = "POST",
                url = "$GITHUB_API/repos/$resolvedOwner/$cleanRepo/git/commits",
                token = cleanToken,
                body = JSONObject()
                    .put("message", commitMessage)
                    .put("tree", newTreeSha)
                    .put("parents", JSONArray().put(baseCommitSha))
            )
            val newCommitSha = commitRes.getString("sha")

            onProgress("Memperbarui branch $activeBranch & memicu build APK...")
            executeJsonRequest(
                method = "PATCH",
                url = "$GITHUB_API/repos/$resolvedOwner/$cleanRepo/git/refs/heads/$activeBranch",
                token = cleanToken,
                body = JSONObject()
                    .put("sha", newCommitSha)
                    .put("force", false)
            )

            val repoUrl = "https://github.com/$resolvedOwner/$cleanRepo"
            Result.success(
                GitHubPushOutcome(
                    repoUrl = repoUrl,
                    actionsUrl = "$repoUrl/actions",
                    releasesUrl = "$repoUrl/releases",
                    commitSha = newCommitSha.take(7),
                    pushedFileCount = treeEntries.length()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Push to GitHub failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchWorkflowRuns(
        token: String,
        owner: String,
        repoName: String
    ): Result<List<GitHubWorkflowRunInfo>> = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = sanitizeRepoName(repoName)
            val json = executeJsonRequest(
                method = "GET",
                url = "$GITHUB_API/repos/${owner.trim()}/$cleanRepo/actions/runs?per_page=5",
                token = token.trim()
            )
            val arr = json.optJSONArray("workflow_runs") ?: JSONArray()
            val runs = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        GitHubWorkflowRunInfo(
                            id = item.optLong("id", 0L),
                            name = item.optString("name", "Android APK Build & Release"),
                            status = item.optString("status", "unknown"),
                            conclusion = item.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
                            htmlUrl = item.optString("html_url", ""),
                            updatedAt = item.optString("updated_at", "")
                        )
                    )
                }
            }
            Result.success(runs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestRelease(
        token: String,
        owner: String,
        repoName: String
    ): Result<GitHubReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = sanitizeRepoName(repoName)
            val request = baseRequest("$GITHUB_API/repos/${owner.trim()}/$cleanRepo/releases/latest", token.trim())
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext Result.success(null)
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("GitHub Releases HTTP ${response.code}: ${extractGitHubError(body, response.message)}")
                }
                val json = JSONObject(body)
                val assetsArr = json.optJSONArray("assets") ?: JSONArray()
                val assets = buildList {
                    for (i in 0 until assetsArr.length()) {
                        val a = assetsArr.getJSONObject(i)
                        add(
                            GitHubReleaseAssetInfo(
                                name = a.optString("name", "app.apk"),
                                downloadUrl = a.optString("browser_download_url", ""),
                                sizeBytes = a.optLong("size", 0L)
                            )
                        )
                    }
                }
                Result.success(
                    GitHubReleaseInfo(
                        tagName = json.optString("tag_name", "latest"),
                        name = json.optString("name", json.optString("tag_name", "Release")),
                        htmlUrl = json.optString("html_url", ""),
                        publishedAt = json.optString("published_at", ""),
                        assets = assets
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun checkRepoExists(token: String, owner: String, repo: String): Boolean {
        val req = baseRequest("$GITHUB_API/repos/$owner/$repo", token).get().build()
        client.newCall(req).execute().use { resp ->
            return resp.isSuccessful
        }
    }

    private fun getOrBootstrapBranchRef(
        token: String,
        owner: String,
        repo: String,
        branch: String
    ): JSONObject {
        val url = "$GITHUB_API/repos/$owner/$repo/git/ref/heads/$branch"
        val req = baseRequest(url, token).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (resp.isSuccessful) return JSONObject(body)
            if (resp.code == 409 || resp.code == 404) {
                // Empty repo without initial commit: bootstrap README via Contents API
                val initContent = Base64.encodeToString(
                    "# $repo\nInitialized by GenDev AI\n".toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )
                executeJsonRequest(
                    method = "PUT",
                    url = "$GITHUB_API/repos/$owner/$repo/contents/README.md",
                    token = token,
                    body = JSONObject()
                        .put("message", "chore: initialize repository via GenDev AI")
                        .put("content", initContent)
                        .put("branch", branch)
                )
                return executeJsonRequest("GET", url, token)
            }
            throw IOException("GitHub Ref HTTP ${resp.code}: ${extractGitHubError(body, resp.message)}")
        }
    }

    private fun executeJsonRequest(
        method: String,
        url: String,
        token: String,
        body: JSONObject? = null
    ): JSONObject {
        val builder = baseRequest(url, token)
        val reqBody = body?.toString()?.toRequestBody(jsonMediaType)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(reqBody ?: "{}".toRequestBody(jsonMediaType))
            "PUT" -> builder.put(reqBody ?: "{}".toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch(reqBody ?: "{}".toRequestBody(jsonMediaType))
            else -> throw IllegalArgumentException("Unsupported method $method")
        }
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val hint = if (response.code == 403 || response.code == 422) {
                    " (Pastikan GitHub PAT memiliki izin 'repo' dan 'workflow'.)"
                } else ""
                throw IOException("GitHub API HTTP ${response.code}: ${extractGitHubError(raw, response.message)}$hint")
            }
            return if (raw.isBlank()) JSONObject() else JSONObject(raw)
        }
    }

    private fun baseRequest(url: String, token: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GenDev-AI-Android/1.0")
    }

    private fun extractGitHubError(raw: String, fallback: String): String {
        return runCatching {
            JSONObject(raw).optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun sanitizeRepoName(raw: String): String {
        return raw.trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}
