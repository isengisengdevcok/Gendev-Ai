package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GenDevRepository
import com.example.data.github.GitHubUserInfo
import com.example.data.local.ProjectFileEntity
import com.example.data.preferences.AppSettings
import com.example.model.GitHubReleaseInfo
import com.example.model.GitHubSyncState
import com.example.model.GitHubWorkflowRunInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "StudioViewModel"

data class StudioUiState(
    val isGeneratingProject: Boolean = false,
    val generationSummary: String? = null,
    val selectedFilePath: String? = null,
    val verifiedUser: GitHubUserInfo? = null,
    val isVerifyingGitHub: Boolean = false,
    val githubVerificationError: String? = null,
    val syncState: GitHubSyncState = GitHubSyncState.Idle,
    val workflowRuns: List<GitHubWorkflowRunInfo> = emptyList(),
    val latestRelease: GitHubReleaseInfo? = null,
    val isRefreshingBuilds: Boolean = false,
    val snackbarMessage: String? = null
)

class StudioViewModel(
    private val repository: GenDevRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    val projectFiles: StateFlow<List<ProjectFileEntity>> = repository.projectFilesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cfg = settings.value
            repository.ensureDefaultProjectFiles(cfg.githubProjectName, cfg.githubPackageName)
        }
    }

    fun selectFile(path: String) {
        _uiState.update { it.copy(selectedFilePath = path) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun getStoredGitHubOwner(): String = repository.secureVault.getGitHubOwner()
    fun getStoredGitHubRepo(): String = repository.secureVault.getGitHubRepo()
    fun hasGitHubToken(): Boolean = repository.secureVault.getGitHubToken().isNotBlank()
    fun getMaskedGitHubToken(): String = repository.secureVault.maskSecret(repository.secureVault.getGitHubToken())

    fun saveGitHubCredentials(
        tokenInput: String,
        owner: String,
        repo: String,
        projectName: String,
        packageName: String,
        branch: String,
        isPrivate: Boolean
    ) {
        if (tokenInput.isNotBlank()) {
            repository.secureVault.saveGitHubToken(tokenInput)
        }
        repository.secureVault.saveGitHubOwner(owner.trim())
        repository.secureVault.saveGitHubRepo(repo.trim())
        viewModelScope.launch {
            repository.updateGitHubProjectConfig(projectName, packageName, branch, isPrivate)
            _uiState.update { it.copy(snackbarMessage = "Konfigurasi GitHub disimpan terenkripsi (Android Keystore).") }
        }
    }

    fun verifyGitHubConnection(tokenInput: String) {
        val token = tokenInput.trim().ifEmpty { repository.secureVault.getGitHubToken() }
        if (token.isEmpty()) {
            _uiState.update { it.copy(githubVerificationError = "Masukkan GitHub Personal Access Token (PAT) terlebih dahulu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingGitHub = true, githubVerificationError = null) }
            val result = repository.gitHubApiService.verifyToken(token)
            result.fold(
                onSuccess = { user ->
                    repository.secureVault.saveGitHubToken(token)
                    if (repository.secureVault.getGitHubOwner().isBlank()) {
                        repository.secureVault.saveGitHubOwner(user.login)
                    }
                    _uiState.update {
                        it.copy(
                            isVerifyingGitHub = false,
                            verifiedUser = user,
                            snackbarMessage = "Terhubung ke GitHub sebagai @${user.login}"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isVerifyingGitHub = false,
                            githubVerificationError = err.localizedMessage ?: "Verifikasi token GitHub gagal."
                        )
                    }
                }
            )
        }
    }

    fun resetToDefaultScaffold(appName: String, packageName: String) {
        viewModelScope.launch {
            val files = repository.templateGenerator.buildDefaultProjectFiles(appName, packageName)
            repository.replaceProjectFiles(files)
            _uiState.update {
                it.copy(
                    selectedFilePath = files.firstOrNull()?.path,
                    generationSummary = "Scaffold Android Jetpack Compose + GitHub Actions APK Release siap.",
                    snackbarMessage = "Template Android & Workflow CI/CD di-reset."
                )
            }
        }
    }

    fun generateProjectWithGemini(
        appIdea: String,
        appName: String,
        packageName: String
    ) {
        if (appIdea.isBlank() || _uiState.value.isGeneratingProject) return
        viewModelScope.launch {
            val cfg = settings.value
            _uiState.update {
                it.copy(
                    isGeneratingProject = true,
                    generationSummary = null
                )
            }
            try {
                val (files, summary) = repository.templateGenerator.generateAiAndroidProject(
                    engineMode = cfg.engineMode,
                    modelId = cfg.activeModelId,
                    appIdea = appIdea.trim(),
                    appName = appName.trim().ifEmpty { cfg.githubProjectName },
                    packageName = packageName.trim().ifEmpty { cfg.githubPackageName }
                )
                repository.replaceProjectFiles(files)
                val mainKt = files.firstOrNull { it.path.endsWith("MainActivity.kt") }?.path ?: files.firstOrNull()?.path
                _uiState.update {
                    it.copy(
                        isGeneratingProject = false,
                        selectedFilePath = mainKt,
                        generationSummary = summary,
                        snackbarMessage = "Proyek Android (${files.size} file + Workflow Release) berhasil dibuat oleh Gemini!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Android project generation failed", e)
                _uiState.update {
                    it.copy(
                        isGeneratingProject = false,
                        snackbarMessage = "Gagal generate proyek: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun importCodeBlockFromChat(code: String, language: String) {
        viewModelScope.launch {
            val cfg = settings.value
            val safePkg = cfg.githubPackageName.trim().lowercase().ifEmpty { "com.gendev.sampleapp" }
            val pkgPath = safePkg.replace('.', '/')
            val targetPath = when (language.lowercase()) {
                "xml" -> "app/src/main/AndroidManifest.xml"
                "yaml", "yml" -> ".github/workflows/android-apk-release.yml"
                "gradle", "kts" -> "app/build.gradle.kts"
                else -> "app/src/main/java/$pkgPath/MainActivity.kt"
            }
            repository.ensureDefaultProjectFiles(cfg.githubProjectName, safePkg)
            repository.upsertProjectFile(targetPath, code, language.ifBlank { "kotlin" })
            _uiState.update {
                it.copy(
                    selectedFilePath = targetPath,
                    snackbarMessage = "Kode dikirim ke GitHub Build Studio -> $targetPath"
                )
            }
        }
    }

    fun saveEditedFile(path: String, newContent: String, language: String) {
        viewModelScope.launch {
            repository.upsertProjectFile(path, newContent, language)
            _uiState.update { it.copy(snackbarMessage = "File $path diperbarui.") }
        }
    }

    fun pushToGitHubAndTriggerBuild(
        tokenInput: String,
        ownerInput: String,
        repoInput: String,
        branchInput: String,
        isPrivate: Boolean,
        commitMessage: String
    ) {
        val token = tokenInput.trim().ifEmpty { repository.secureVault.getGitHubToken() }
        val owner = ownerInput.trim().ifEmpty { repository.secureVault.getGitHubOwner() }
        val repo = repoInput.trim().ifEmpty { repository.secureVault.getGitHubRepo() }
        val branch = branchInput.trim().ifEmpty { "main" }

        if (token.isBlank()) {
            _uiState.update {
                it.copy(syncState = GitHubSyncState.Failure("Masukkan GitHub Personal Access Token (PAT) dengan scope 'repo' & 'workflow'."))
            }
            return
        }
        if (repo.isBlank()) {
            _uiState.update {
                it.copy(syncState = GitHubSyncState.Failure("Nama repository GitHub harus diisi."))
            }
            return
        }

        // Persist encrypted config before pushing
        repository.secureVault.saveGitHubToken(token)
        if (owner.isNotBlank()) repository.secureVault.saveGitHubOwner(owner)
        repository.secureVault.saveGitHubRepo(repo)

        viewModelScope.launch {
            val cfg = settings.value
            repository.updateGitHubProjectConfig(cfg.githubProjectName, cfg.githubPackageName, branch, isPrivate)
            val files = projectFiles.value.ifEmpty {
                repository.templateGenerator.buildDefaultProjectFiles(cfg.githubProjectName, cfg.githubPackageName)
            }
            _uiState.update { it.copy(syncState = GitHubSyncState.InProgress("Memulai sinkronisasi ke GitHub...")) }

            val result = repository.gitHubApiService.pushProjectAtomically(
                token = token,
                owner = owner,
                repoName = repo,
                branch = branch,
                isPrivate = isPrivate,
                commitMessage = commitMessage.ifBlank { "feat: sync Android source & APK Release workflow via GenDev AI" },
                files = files,
                onProgress = { step ->
                    _uiState.update { it.copy(syncState = GitHubSyncState.InProgress(step)) }
                }
            )

            result.fold(
                onSuccess = { outcome ->
                    _uiState.update {
                        it.copy(
                            syncState = GitHubSyncState.Success(
                                repoUrl = outcome.repoUrl,
                                actionsUrl = outcome.actionsUrl,
                                commitSha = outcome.commitSha
                            ),
                            snackbarMessage = "Berhasil push ${outcome.pushedFileCount} file! GitHub Actions sedang membangun APK Release."
                        )
                    }
                    refreshGitHubBuildsAndReleases(token, owner.ifEmpty { _uiState.value.verifiedUser?.login.orEmpty() }, repo)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            syncState = GitHubSyncState.Failure(
                                err.localizedMessage ?: "Gagal push source ke GitHub."
                            )
                        )
                    }
                }
            )
        }
    }

    fun refreshGitHubBuildsAndReleases(
        tokenOverride: String = "",
        ownerOverride: String = "",
        repoOverride: String = ""
    ) {
        val token = tokenOverride.trim().ifEmpty { repository.secureVault.getGitHubToken() }
        val owner = ownerOverride.trim().ifEmpty { repository.secureVault.getGitHubOwner() }
        val repo = repoOverride.trim().ifEmpty { repository.secureVault.getGitHubRepo() }
        if (token.isBlank() || owner.isBlank() || repo.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingBuilds = true) }
            val runs = repository.gitHubApiService.fetchWorkflowRuns(token, owner, repo).getOrDefault(emptyList())
            val release = repository.gitHubApiService.fetchLatestRelease(token, owner, repo).getOrNull()
            _uiState.update {
                it.copy(
                    isRefreshingBuilds = false,
                    workflowRuns = runs,
                    latestRelease = release
                )
            }
        }
    }
}
