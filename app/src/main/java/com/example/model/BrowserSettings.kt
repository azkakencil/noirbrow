package com.example.model

data class BrowserSettings(
    val isAdBlockEnabled: Boolean = true,
    val isDataSaverEnabled: Boolean = false,
    val allowImagesTemporarily: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isDesktopMode: Boolean = false,
    val searchEngineUrl: String = "https://www.google.com/search?q=",
    val searchEngineName: String = "Google",
    val historyPin: String? = null,
    val isPinRequiredForHistory: Boolean = false
)
