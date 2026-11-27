package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ChatSessionEntity
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.ApiConfigScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.GitHubStudioScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SystemPromptScreen
import com.example.ui.screens.UsageDashboardScreen
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

object GenDevRoutes {
    const val ROUTE_CHAT = "chat"
    const val ROUTE_GITHUB_STUDIO = "github_studio"
    const val ROUTE_API_CONFIG = "api_config"
    const val ROUTE_USAGE = "usage"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_SYSTEM_PROMPT = "system_prompt"
    const val ROUTE_ABOUT = "about"
}

private data class TopDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TOP_DESTINATIONS = listOf(
    TopDestination(GenDevRoutes.ROUTE_CHAT, "Chat", Icons.Rounded.Forum, Icons.Outlined.Forum),
    TopDestination(GenDevRoutes.ROUTE_GITHUB_STUDIO, "GitHub", Icons.Rounded.RocketLaunch, Icons.Outlined.RocketLaunch),
    TopDestination(GenDevRoutes.ROUTE_API_CONFIG, "Gemini", Icons.Rounded.Key, Icons.Outlined.Key),
    TopDestination(GenDevRoutes.ROUTE_USAGE, "Usage", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    TopDestination(GenDevRoutes.ROUTE_SETTINGS, "Lab", Icons.Rounded.Tune, Icons.Outlined.Tune)
)

@Composable
fun GenDevApp(
    chatViewModel: ChatViewModel,
    studioViewModel: StudioViewModel
) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: GenDevRoutes.ROUTE_CHAT

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val settings by chatViewModel.settings.collectAsStateWithLifecycle()
    val sessions by chatViewModel.sessions.collectAsStateWithLifecycle()
    val activeSession by chatViewModel.activeSession.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val usageRecords by chatViewModel.usageRecords.collectAsStateWithLifecycle()
    val promptPresets by chatViewModel.promptPresets.collectAsStateWithLifecycle()

    val studioUiState by studioViewModel.uiState.collectAsStateWithLifecycle()
    val projectFiles by studioViewModel.projectFiles.collectAsStateWithLifecycle()

    LaunchedEffect(chatUiState.statusSnackbar) {
        chatUiState.statusSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            chatViewModel.clearSnackbar()
        }
    }

    LaunchedEffect(studioUiState.snackbarMessage) {
        studioUiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            studioViewModel.clearSnackbar()
        }
    }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(GenDevRoutes.ROUTE_CHAT) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == GenDevRoutes.ROUTE_CHAT,
        drawerContent = {
            SessionHistoryDrawer(
                sessions = sessions,
                activeSessionId = chatUiState.activeSessionId,
                onNewSession = {
                    chatViewModel.createNewSession()
                    scope.launch { drawerState.close() }
                },
                onSelectSession = { id ->
                    chatViewModel.selectSession(id)
                    scope.launch { drawerState.close() }
                    navigateTop(GenDevRoutes.ROUTE_CHAT)
                },
                onDeleteSession = { id -> chatViewModel.deleteSession(id) },
                onOpenSystemPrompt = {
                    scope.launch { drawerState.close() }
                    navController.navigate(GenDevRoutes.ROUTE_SYSTEM_PROMPT)
                },
                onOpenAbout = {
                    scope.launch { drawerState.close() }
                    navController.navigate(GenDevRoutes.ROUTE_ABOUT)
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
            val useNavRail = maxWidth >= 680.dp
            val isTopLevelRoute = TOP_DESTINATIONS.any { it.route == currentRoute }

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail && isTopLevelRoute) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BrightViolet,
                                    selectedTextColor = BrightViolet,
                                    indicatorColor = VioletContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = ObsidianVoid,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail && isTopLevelRoute) {
                            NavigationBar(
                                containerColor = ObsidianSurface,
                                tonalElevation = 0.dp
                            ) {
                                TOP_DESTINATIONS.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTop(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = dest.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrightViolet,
                                            selectedTextColor = BrightViolet,
                                            indicatorColor = VioletContainer,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_${dest.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = GenDevRoutes.ROUTE_CHAT
                        ) {
                            composable(GenDevRoutes.ROUTE_CHAT) {
                                ChatScreen(
                                    uiState = chatUiState,
                                    settings = settings,
                                    activeSession = activeSession,
                                    messages = messages,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onNewSession = { chatViewModel.createNewSession() },
                                    onSelectModel = { chatViewModel.setActiveModel(it) },
                                    onSelectMode = { chatViewModel.setChatMode(it) },
                                    onSendPrompt = { chatViewModel.sendPrompt(it) },
                                    onStopGeneration = { chatViewModel.stopGeneration() },
                                    onAddAttachment = { chatViewModel.addAttachment(it) },
                                    onRemoveAttachment = { chatViewModel.removeAttachment(it) },
                                    onApproveRephrase = { chatViewModel.approvePendingRephrase(it) },
                                    onDismissRephrase = { chatViewModel.dismissPendingRephrase() },
                                    onDismissDiagnostic = { chatViewModel.dismissDiagnostic() },
                                    onEditMessage = { msg, text -> chatViewModel.editAndResendUserMessage(msg, text) },
                                    onRegenerateMessage = { chatViewModel.regenerateLastResponse(it) },
                                    onCompressContext = { chatViewModel.compressCurrentSessionContext() },
                                    onSendCodeToStudio = { code, lang ->
                                        studioViewModel.importCodeBlockFromChat(code, lang)
                                        navigateTop(GenDevRoutes.ROUTE_GITHUB_STUDIO)
                                    },
                                    onNavigateToGitHubStudio = { navigateTop(GenDevRoutes.ROUTE_GITHUB_STUDIO) },
                                    onLocalNotice = { notice ->
                                        scope.launch { snackbarHostState.showSnackbar(notice) }
                                    }
                                )
                            }

                            composable(GenDevRoutes.ROUTE_GITHUB_STUDIO) {
                                GitHubStudioScreen(
                                    uiState = studioUiState,
                                    settings = settings,
                                    projectFiles = projectFiles,
                                    initialOwner = studioViewModel.getStoredGitHubOwner(),
                                    initialRepo = studioViewModel.getStoredGitHubRepo(),
                                    hasStoredToken = studioViewModel.hasGitHubToken(),
                                    maskedToken = studioViewModel.getMaskedGitHubToken(),
                                    onGenerateProject = { idea, name, pkg ->
                                        studioViewModel.generateProjectWithGemini(idea, name, pkg)
                                    },
                                    onResetTemplate = { name, pkg ->
                                        studioViewModel.resetToDefaultScaffold(name, pkg)
                                    },
                                    onSelectFile = { studioViewModel.selectFile(it) },
                                    onSaveFileContent = { path, content, lang ->
                                        studioViewModel.saveEditedFile(path, content, lang)
                                    },
                                    onSaveGitHubConfig = { token, owner, repo, name, pkg, branch, priv ->
                                        studioViewModel.saveGitHubCredentials(token, owner, repo, name, pkg, branch, priv)
                                    },
                                    onVerifyGitHub = { studioViewModel.verifyGitHubConnection(it) },
                                    onPushToGitHub = { token, owner, repo, branch, priv, msg ->
                                        studioViewModel.pushToGitHubAndTriggerBuild(token, owner, repo, branch, priv, msg)
                                    },
                                    onRefreshBuilds = { token, owner, repo ->
                                        studioViewModel.refreshGitHubBuildsAndReleases(token, owner, repo)
                                    }
                                )
                            }

                            composable(GenDevRoutes.ROUTE_API_CONFIG) {
                                ApiConfigScreen(
                                    settings = settings,
                                    connectionTestState = chatUiState.connectionTestState,
                                    maskedApiKey = chatViewModel.getMaskedGeminiKey(),
                                    hasByokKey = chatViewModel.hasByokGeminiKey(),
                                    storedProxyUrl = chatViewModel.getStoredProxyUrl(),
                                    onSelectEngineMode = { chatViewModel.setEngineMode(it) },
                                    onSelectModel = { chatViewModel.setActiveModel(it) },
                                    onSaveByok = { key, proxy -> chatViewModel.saveByokCredentials(key, proxy) },
                                    onClearByokKey = { chatViewModel.clearByokApiKey() },
                                    onTestConnection = { chatViewModel.testGeminiConnection() }
                                )
                            }

                            composable(GenDevRoutes.ROUTE_USAGE) {
                                UsageDashboardScreen(
                                    settings = settings,
                                    records = usageRecords,
                                    onClearHistory = { chatViewModel.clearUsageHistory() }
                                )
                            }

                            composable(GenDevRoutes.ROUTE_SETTINGS) {
                                AdvancedSettingsScreen(
                                    settings = settings,
                                    onUpdateParams = { chatViewModel.setGenerationParams(it) },
                                    onUpdateAce = { chatViewModel.setAceSettings(it) },
                                    onOpenSystemPromptEditor = {
                                        navController.navigate(GenDevRoutes.ROUTE_SYSTEM_PROMPT)
                                    },
                                    onOpenAbout = {
                                        navController.navigate(GenDevRoutes.ROUTE_ABOUT)
                                    }
                                )
                            }

                            composable(GenDevRoutes.ROUTE_SYSTEM_PROMPT) {
                                SystemPromptScreen(
                                    settings = settings,
                                    presets = promptPresets,
                                    onBack = { navController.popBackStack() },
                                    onSelectMode = { chatViewModel.setChatMode(it, updatePrompt = true) },
                                    onApplyPrompt = { chatViewModel.setSystemPrompt(it) },
                                    onSavePreset = { name, mode, prompt ->
                                        chatViewModel.savePromptPreset(name, mode, prompt)
                                    },
                                    onDeletePreset = { chatViewModel.deletePromptPreset(it) }
                                )
                            }

                            composable(GenDevRoutes.ROUTE_ABOUT) {
                                AboutScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryDrawer(
    sessions: List<ChatSessionEntity>,
    activeSessionId: Long?,
    onNewSession: () -> Unit,
    onSelectSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onOpenSystemPrompt: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions
        else sessions.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    ModalDrawerSheet(
        drawerContainerColor = ObsidianSurface,
        modifier = Modifier.width(304.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("GENDEV AI", style = MaterialTheme.typography.headlineSmall, color = BrightViolet)
            Text("Local Room Session Vault (${sessions.size})", style = MaterialTheme.typography.labelSmall, color = NeonCyan)

            Button(
                onClick = onNewSession,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = ObsidianVoid)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Research Chat", color = ObsidianVoid, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search sessions...", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { session ->
                    val selected = session.id == activeSessionId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) VioletContainer else ObsidianCard,
                        border = BorderStroke(1.dp, if (selected) ElectricViolet else ObsidianBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSession(session.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) BrightViolet else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${session.modelId} · ${session.totalInputTokens + session.totalOutputTokens} tok",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete session",
                                    tint = CoralError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ObsidianBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSystemPrompt() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Psychology, contentDescription = null, tint = BrightViolet)
                Spacer(modifier = Modifier.width(10.dp))
                Text("System Prompt Editor", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAbout() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text("About & Security", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}
