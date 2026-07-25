package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import com.example.data.BrowserRepository
import com.example.data.HistoryEntity
import com.example.engine.AdBlockEngine
import com.example.engine.DataSaverEngine
import com.example.model.BrowserSettings
import com.example.model.TabItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = BrowserRepository(db.historyDao(), db.bookmarkDao())

    val adBlockEngine = AdBlockEngine()
    val dataSaverEngine = DataSaverEngine()

    // Active Tabs State
    private val _tabs = MutableStateFlow<List<TabItem>>(listOf(TabItem()))
    val tabs: StateFlow<List<TabItem>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Browser Settings State
    private val _settings = MutableStateFlow(BrowserSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // Address Bar Text
    private val _urlInputText = MutableStateFlow("")
    val urlInputText: StateFlow<String> = _urlInputText.asStateFlow()

    // Stats
    private val _blockedAdsCount = MutableStateFlow(0)
    val blockedAdsCount: StateFlow<Int> = _blockedAdsCount.asStateFlow()

    private val _dataSavedFormatted = MutableStateFlow("0 KB")
    val dataSavedFormatted: StateFlow<String> = _dataSavedFormatted.asStateFlow()

    // Sheets State
    private val _isTabsSheetOpen = MutableStateFlow(false)
    val isTabsSheetOpen: StateFlow<Boolean> = _isTabsSheetOpen.asStateFlow()

    private val _isMenuSheetOpen = MutableStateFlow(false)
    val isMenuSheetOpen: StateFlow<Boolean> = _isMenuSheetOpen.asStateFlow()

    private val _isHistorySheetOpen = MutableStateFlow(false)
    val isHistorySheetOpen: StateFlow<Boolean> = _isHistorySheetOpen.asStateFlow()

    private val _isSyncSheetOpen = MutableStateFlow(false)
    val isSyncSheetOpen: StateFlow<Boolean> = _isSyncSheetOpen.asStateFlow()

    private val _isCurrentUrlBookmarked = MutableStateFlow(false)
    val isCurrentUrlBookmarked: StateFlow<Boolean> = _isCurrentUrlBookmarked.asStateFlow()

    // Database Flows
    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksList: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab Helper
    fun getActiveTab(): TabItem? {
        return _tabs.value.find { it.id == _activeTabId.value }
    }

    fun setUrlInputText(text: String) {
        _urlInputText.value = text
    }

    fun openUrl(rawUrl: String) {
        val active = getActiveTab() ?: return
        var formatted = rawUrl.trim()
        if (formatted.isBlank()) return

        if (!formatted.startsWith("http://") && !formatted.startsWith("https://") && !formatted.startsWith("about:")) {
            if (formatted.contains(".") && !formatted.contains(" ")) {
                formatted = "https://$formatted"
            } else {
                formatted = "${_settings.value.searchEngineUrl}${java.net.URLEncoder.encode(formatted, "UTF-8")}"
            }
        }

        updateTab(active.id) {
            it.url = formatted
            it.title = formatted
        }
        _urlInputText.value = formatted
        checkBookmarkStatus(formatted)
    }

    fun updateTabProgress(tabId: String, progress: Int, isLoading: Boolean) {
        updateTab(tabId) {
            it.progress = progress
            it.isLoading = isLoading
        }
        // Update stats
        _blockedAdsCount.value = adBlockEngine.getBlockedCount()
        if (_settings.value.isDataSaverEnabled) {
            dataSaverEngine.addEstimatedSavedData(15_000L) // Estimate saved overhead per load
            _dataSavedFormatted.value = dataSaverEngine.getSavedDataFormatted()
        }
    }

    fun updateTabInfo(tabId: String, url: String, title: String, canBack: Boolean, canForward: Boolean) {
        updateTab(tabId) {
            it.url = url
            it.title = title
            it.canGoBack = canBack
            it.canGoForward = canForward
        }
        if (tabId == _activeTabId.value) {
            _urlInputText.value = if (url == "about:blank") "" else url
            checkBookmarkStatus(url)
        }

        // Save history if not incognito
        val active = getActiveTab()
        if (active != null && !active.isIncognito && url.isNotBlank() && !url.startsWith("about:blank")) {
            viewModelScope.launch {
                repository.addHistory(url, title)
            }
        }
    }

    private fun updateTab(tabId: String, transform: (TabItem) -> Unit) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) {
                val copy = it.copy()
                transform(copy)
                copy
            } else it
        }
    }

    fun addNewTab(url: String = "about:blank", isIncognito: Boolean = false) {
        val newTab = TabItem(url = url, isIncognito = isIncognito)
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _urlInputText.value = if (url == "about:blank") "" else url
        _isTabsSheetOpen.value = false
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // Reset sole tab
            val fresh = TabItem()
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            _urlInputText.value = ""
            return
        }

        val remaining = currentTabs.filter { it.id != tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            _activeTabId.value = remaining.last().id
            _urlInputText.value = if (remaining.last().url == "about:blank") "" else remaining.last().url
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        val tab = getActiveTab()
        _urlInputText.value = if (tab?.url == "about:blank") "" else (tab?.url ?: "")
        _isTabsSheetOpen.value = false
        tab?.url?.let { checkBookmarkStatus(it) }
    }

    // Toggle Settings
    fun toggleAdBlock() {
        _settings.value = _settings.value.copy(isAdBlockEnabled = !_settings.value.isAdBlockEnabled)
    }

    fun toggleDataSaver() {
        _settings.value = _settings.value.copy(isDataSaverEnabled = !_settings.value.isDataSaverEnabled)
    }

    fun toggleTempImages() {
        _settings.value = _settings.value.copy(allowImagesTemporarily = !_settings.value.allowImagesTemporarily)
    }

    fun toggleTheme() {
        _settings.value = _settings.value.copy(isDarkTheme = !_settings.value.isDarkTheme)
    }

    fun toggleDesktopMode() {
        _settings.value = _settings.value.copy(isDesktopMode = !_settings.value.isDesktopMode)
    }

    fun setSearchEngine(name: String, searchUrl: String) {
        _settings.value = _settings.value.copy(searchEngineName = name, searchEngineUrl = searchUrl)
    }

    // Bookmarks
    fun toggleCurrentBookmark() {
        val active = getActiveTab() ?: return
        if (active.url.isBlank() || active.url.startsWith("about:blank")) return
        viewModelScope.launch {
            repository.toggleBookmark(active.url, active.title)
            checkBookmarkStatus(active.url)
        }
    }

    private fun checkBookmarkStatus(url: String) {
        viewModelScope.launch {
            _isCurrentUrlBookmarked.value = repository.isBookmarked(url)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteBookmarkItem(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmarkItem(id)
            getActiveTab()?.url?.let { checkBookmarkStatus(it) }
        }
    }

    // Sheet Controls
    fun setTabsSheetOpen(open: Boolean) { _isTabsSheetOpen.value = open }
    fun setMenuSheetOpen(open: Boolean) { _isMenuSheetOpen.value = open }
    fun setHistorySheetOpen(open: Boolean) { _isHistorySheetOpen.value = open }
    fun setSyncSheetOpen(open: Boolean) { _isSyncSheetOpen.value = open }
}
