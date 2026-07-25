package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BrowserSettings
import com.example.model.TabItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val urlInputText by viewModel.urlInputText.collectAsState()
    val blockedAdsCount by viewModel.blockedAdsCount.collectAsState()
    val dataSavedFormatted by viewModel.dataSavedFormatted.collectAsState()

    val isTabsSheetOpen by viewModel.isTabsSheetOpen.collectAsState()
    val isMenuSheetOpen by viewModel.isMenuSheetOpen.collectAsState()
    val isHistorySheetOpen by viewModel.isHistorySheetOpen.collectAsState()
    val isSyncSheetOpen by viewModel.isSyncSheetOpen.collectAsState()
    val isCurrentUrlBookmarked by viewModel.isCurrentUrlBookmarked.collectAsState()

    val historyList by viewModel.historyList.collectAsState()
    val bookmarksList by viewModel.bookmarksList.collectAsState()

    val activeTab = viewModel.getActiveTab()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isUrlEditDialogVisible by remember { mutableStateOf(false) }

    // Intercept hardware back button
    BackHandler(enabled = activeTab?.canGoBack == true) {
        webViewRef?.goBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Ultra-Minimalist Navigation Dock
            UltraMinimalistBottomDock(
                tab = activeTab,
                urlText = urlInputText,
                isBookmarked = isCurrentUrlBookmarked,
                tabsCount = tabs.size,
                onBack = { webViewRef?.goBack() },
                onForward = { webViewRef?.goForward() },
                onUrlClick = { isUrlEditDialogVisible = true },
                onToggleBookmark = { viewModel.toggleCurrentBookmark() },
                onOpenTabs = { viewModel.setTabsSheetOpen(true) },
                onOpenMenu = { viewModel.setMenuSheetOpen(true) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTab == null || activeTab.url == "about:blank") {
                HomeView(
                    settings = settings,
                    blockedAdsCount = blockedAdsCount,
                    dataSavedFormatted = dataSavedFormatted,
                    onSearch = { url -> viewModel.openUrl(url) },
                    onSelectEngine = { name, searchUrl -> viewModel.setSearchEngine(name, searchUrl) },
                    onOpenSync = { viewModel.setSyncSheetOpen(true) }
                )
            } else {
                // Real WebView
                Column(modifier = Modifier.fillMaxSize()) {
                    // Thin Loading Line
                    if (activeTab.isLoading) {
                        LinearProgressIndicator(
                            progress = { activeTab.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("browser_webview"),
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                configureWebSettings(this, settings)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        return viewModel.adBlockEngine.shouldBlock(
                                            request,
                                            settings.isAdBlockEnabled
                                        ) ?: super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        url?.let {
                                            viewModel.updateTabProgress(activeTab.id, 10, true)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let {
                                            viewModel.updateTabProgress(activeTab.id, 100, false)
                                            viewModel.updateTabInfo(
                                                tabId = activeTab.id,
                                                url = it,
                                                title = view?.title ?: it,
                                                canBack = view?.canGoBack() ?: false,
                                                canForward = view?.canGoForward() ?: false
                                            )
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        viewModel.updateTabProgress(activeTab.id, newProgress, newProgress < 100)
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        title?.let {
                                            viewModel.updateTabInfo(
                                                tabId = activeTab.id,
                                                url = view?.url ?: activeTab.url,
                                                title = it,
                                                canBack = view?.canGoBack() ?: false,
                                                canForward = view?.canGoForward() ?: false
                                            )
                                        }
                                    }
                                }

                                loadUrl(activeTab.url)
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                            configureWebSettings(webView, settings)
                            if (webView.url != activeTab.url && activeTab.url != "about:blank") {
                                webView.loadUrl(activeTab.url)
                            }
                        }
                    )
                }
            }
        }
    }

    // URL Dialog Box
    if (isUrlEditDialogVisible) {
        UrlInputDialog(
            initialUrl = if (activeTab?.url == "about:blank") "" else (activeTab?.url ?: ""),
            onConfirm = { newUrl ->
                viewModel.openUrl(newUrl)
                isUrlEditDialogVisible = false
            },
            onDismiss = { isUrlEditDialogVisible = false }
        )
    }

    // Tabs Sheet
    if (isTabsSheetOpen) {
        TabsSheet(
            tabs = tabs,
            activeTabId = activeTabId,
            onSelectTab = { id -> viewModel.selectTab(id) },
            onCloseTab = { id -> viewModel.closeTab(id) },
            onNewTab = { incognito -> viewModel.addNewTab(isIncognito = incognito) },
            onDismiss = { viewModel.setTabsSheetOpen(false) }
        )
    }

    // Menu Sheet
    if (isMenuSheetOpen) {
        MenuSheet(
            settings = settings,
            blockedAdsCount = blockedAdsCount,
            dataSavedFormatted = dataSavedFormatted,
            onToggleAdBlock = { viewModel.toggleAdBlock() },
            onToggleDataSaver = { viewModel.toggleDataSaver() },
            onToggleTempImages = { viewModel.toggleTempImages() },
            onToggleTheme = { viewModel.toggleTheme() },
            onToggleDesktop = { viewModel.toggleDesktopMode() },
            onOpenHistory = { viewModel.setHistorySheetOpen(true) },
            onOpenSync = { viewModel.setSyncSheetOpen(true) },
            onDismiss = { viewModel.setMenuSheetOpen(false) }
        )
    }

    // History & Bookmarks Sheet
    if (isHistorySheetOpen) {
        HistoryBookmarksSheet(
            historyList = historyList,
            bookmarksList = bookmarksList,
            onSelectUrl = { url -> viewModel.openUrl(url) },
            onDeleteHistoryItem = { id -> viewModel.deleteHistoryItem(id) },
            onClearHistory = { viewModel.clearAllHistory() },
            onDeleteBookmarkItem = { id -> viewModel.deleteBookmarkItem(id) },
            onDismiss = { viewModel.setHistorySheetOpen(false) }
        )
    }

    // Secure Sync Sheet
    if (isSyncSheetOpen) {
        SecureSyncSheet(
            onExportPayload = { pass -> viewModel.repository.exportSyncPayload(pass) },
            onImportPayload = { code, pass -> viewModel.repository.importSyncPayload(code, pass) },
            onDismiss = { viewModel.setSyncSheetOpen(false) }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebSettings(webView: WebView, settings: BrowserSettings) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true

        // User Agent Desktop Mode
        if (settings.isDesktopMode) {
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            userAgentString = null
        }

        // Apply Data Saver
        if (settings.isDataSaverEnabled) {
            val allowImg = settings.allowImagesTemporarily
            loadsImagesAutomatically = allowImg
            blockNetworkImage = !allowImg
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        } else {
            loadsImagesAutomatically = true
            blockNetworkImage = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
    }
}

@Composable
fun UltraMinimalistBottomDock(
    tab: TabItem?,
    urlText: String,
    isBookmarked: Boolean,
    tabsCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUrlClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Navigation Back/Forward
            IconButton(
                onClick = onBack,
                enabled = tab?.canGoBack == true,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("nav_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    modifier = Modifier.size(18.dp),
                    tint = if (tab?.canGoBack == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = onForward,
                enabled = tab?.canGoForward == true,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("nav_forward_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Maju",
                    modifier = Modifier.size(18.dp),
                    tint = if (tab?.canGoForward == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }

            // Central Minimalist URL Pill
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .clickable { onUrlClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (tab?.isIncognito == true) Icons.Default.Security else Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (urlText.isBlank()) "Ketik URL / Cari..." else urlText.replace("https://", "").replace("http://", ""),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (urlText.isBlank()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bookmark Star
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("nav_bookmark_button")
            ) {
                Icon(
                    if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Bookmark",
                    modifier = Modifier.size(18.dp),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Tabs Counter Badge
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenTabs() },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$tabsCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu Button
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("nav_menu_button")
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun UrlInputDialog(
    initialUrl: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "MASUKKAN URL / CARI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("google.com atau cari kata...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_url_input"),
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                modifier = Modifier.testTag("dialog_url_confirm_button")
            ) {
                Text("Buka", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
