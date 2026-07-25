package com.example.data

import com.example.engine.SyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BrowserRepository(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao
) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)

    suspend fun addHistory(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:blank") || url.startsWith("data:")) return
        val existing = historyDao.findByUrl(url)
        val cleanTitle = title.ifBlank { url }
        if (existing != null) {
            historyDao.insertHistory(
                existing.copy(
                    title = cleanTitle,
                    timestamp = System.currentTimeMillis(),
                    visitCount = existing.visitCount + 1
                )
            )
        } else {
            historyDao.insertHistory(
                HistoryEntity(
                    url = url,
                    title = cleanTitle,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)
    suspend fun clearHistory() = historyDao.clearAllHistory()

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.findByUrl(url) != null
    }

    suspend fun toggleBookmark(url: String, title: String) {
        val existing = bookmarkDao.findByUrl(url)
        if (existing != null) {
            bookmarkDao.deleteByUrl(url)
        } else {
            val cleanTitle = title.ifBlank { url }
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    url = url,
                    title = cleanTitle,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteBookmarkItem(id: Long) = bookmarkDao.deleteById(id)

    suspend fun exportSyncPayload(passphrase: String): String {
        val historyList = historyDao.getAllHistory().first()
        val bookmarkList = bookmarkDao.getAllBookmarks().first()
        return SyncEngine.generateSyncPayload(historyList, bookmarkList, passphrase)
    }

    suspend fun importSyncPayload(payload: String, passphrase: String): Boolean {
        val result = SyncEngine.parseSyncPayload(payload, passphrase) ?: return false
        val (incomingHistory, incomingBookmarks) = result

        if (incomingHistory.isNotEmpty()) {
            historyDao.insertAll(incomingHistory)
        }
        if (incomingBookmarks.isNotEmpty()) {
            bookmarkDao.insertAll(incomingBookmarks)
        }
        return true
    }
}
