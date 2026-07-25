package com.example.model

import java.util.UUID

data class TabItem(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "about:blank",
    var title: String = "Tab Baru",
    var isIncognito: Boolean = false,
    var isLoading: Boolean = false,
    var progress: Int = 0,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
    var faviconUrl: String? = null
)
