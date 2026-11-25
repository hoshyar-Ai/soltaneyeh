package com.example

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MainTab
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.SoltaniyehViewModel
import com.example.ui.components.OpenAiSettingsDialog
import com.example.ui.screens.AttractionDetailScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.GuideChatScreen
import com.example.ui.screens.IdentifierScreen
import com.example.ui.theme.SoltaniyehTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val persianLocale = Locale.forLanguageTag("fa-IR")
        Locale.setDefault(persianLocale)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(persianLocale)
            setLayoutDirection(persianLocale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SoltaniyehTheme {
                SoltaniyehApp()
            }
        }
    }
}

private data class NavItemSpec(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_ITEMS = listOf(
    NavItemSpec(
        tab = MainTab.EXPLORE,
        labelRes = R.string.nav_explore,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_tab_explore"
    ),
    NavItemSpec(
        tab = MainTab.IDENTIFIER,
        labelRes = R.string.nav_identifier,
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        testTag = "nav_tab_identifier"
    ),
    NavItemSpec(
        tab = MainTab.GUIDE,
        labelRes = R.string.nav_guide,
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        testTag = "nav_tab_guide"
    )
)

@Composable
fun SoltaniyehApp(
    viewModel: SoltaniyehViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val audienceLevel by viewModel.audienceLevel.collectAsStateWithLifecycle()
    val attractions by viewModel.attractions.collectAsStateWithLifecycle()
    val localFaqs by viewModel.localFaqs.collectAsStateWithLifecycle()
    val selectedAttraction by viewModel.selectedAttraction.collectAsStateWithLifecycle()
    val detailAiState by viewModel.detailAiState.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val imageNote by viewModel.imageNote.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()
    val runtimeApiKey by viewModel.runtimeApiKey.collectAsStateWithLifecycle()
    var showOpenAiSettings by remember { mutableStateOf(false) }

    if (showOpenAiSettings) {
        OpenAiSettingsDialog(
            currentRuntimeKey = runtimeApiKey,
            onSaveKey = viewModel::saveTempOpenAiKey,
            onClearKey = viewModel::clearTempOpenAiKey,
            onDismiss = { showOpenAiSettings = false }
        )
    }

    val activeAttraction = selectedAttraction
    if (activeAttraction != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            AttractionDetailScreen(
                attraction = activeAttraction,
                audienceLevel = audienceLevel,
                detailAiState = detailAiState,
                onSelectAudienceLevel = viewModel::selectAudienceLevel,
                onRefreshGuide = {
                    viewModel.enrichAttractionWithAi(activeAttraction)
                },
                onAskInChat = viewModel::askGuideAboutAttraction,
                onOpenSettings = { showOpenAiSettings = true },
                onBack = viewModel::closeAttractionDetail,
                modifier = Modifier.widthIn(max = 700.dp)
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NAV_ITEMS.forEach { item ->
                        val selected = currentTab == item.tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(item.tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    TabContent(
                        currentTab = currentTab,
                        attractions = attractions,
                        quickSuggestions = localFaqs.map { it.question },
                        audienceLevel = audienceLevel,
                        selectedImageUri = selectedImageUri,
                        imageNote = imageNote,
                        analysisState = analysisState,
                        chatMessages = chatMessages,
                        isChatStreaming = isChatStreaming,
                        onOpenSettings = { showOpenAiSettings = true },
                        viewModel = viewModel,
                        modifier = Modifier.widthIn(max = 680.dp)
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                TabContent(
                    currentTab = currentTab,
                    attractions = attractions,
                    quickSuggestions = localFaqs.map { it.question },
                    audienceLevel = audienceLevel,
                    selectedImageUri = selectedImageUri,
                    imageNote = imageNote,
                    analysisState = analysisState,
                    chatMessages = chatMessages,
                    isChatStreaming = isChatStreaming,
                    onOpenSettings = { showOpenAiSettings = true },
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    currentTab: MainTab,
    attractions: List<com.example.model.Attraction>,
    quickSuggestions: List<String>,
    audienceLevel: com.example.model.AudienceLevel,
    selectedImageUri: android.net.Uri?,
    imageNote: String,
    analysisState: com.example.model.AnalysisUiState,
    chatMessages: List<com.example.model.ChatMessage>,
    isChatStreaming: Boolean,
    onOpenSettings: () -> Unit,
    viewModel: SoltaniyehViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = currentTab,
        modifier = modifier.fillMaxSize(),
        label = "tabCrossfade"
    ) { tab ->
        when (tab) {
            MainTab.EXPLORE -> ExploreScreen(
                attractions = attractions,
                audienceLevel = audienceLevel,
                onSelectAudienceLevel = viewModel::selectAudienceLevel,
                onOpenAttraction = viewModel::openAttractionDetail,
                onNavigateTab = viewModel::selectTab,
                onOpenSettings = onOpenSettings
            )
            MainTab.IDENTIFIER -> IdentifierScreen(
                attractions = attractions,
                audienceLevel = audienceLevel,
                selectedImageUri = selectedImageUri,
                imageNote = imageNote,
                analysisState = analysisState,
                onSelectAudienceLevel = viewModel::selectAudienceLevel,
                onImageSelected = viewModel::onImageSelected,
                onImageNoteChanged = viewModel::onImageNoteChanged,
                onClearImage = viewModel::clearSelectedImage,
                onAnalyzeClick = viewModel::analyzeSelectedImage,
                onOpenAttraction = viewModel::openAttractionDetail,
                onOpenSettings = onOpenSettings
            )
            MainTab.GUIDE -> GuideChatScreen(
                audienceLevel = audienceLevel,
                messages = chatMessages,
                quickSuggestions = quickSuggestions,
                isStreaming = isChatStreaming,
                onSelectAudienceLevel = viewModel::selectAudienceLevel,
                onSendMessage = viewModel::sendChatMessage,
                onResetChat = viewModel::resetChat,
                onOpenSettings = onOpenSettings
            )
        }
    }
}
