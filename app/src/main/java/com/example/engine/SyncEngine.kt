package com.example.engine

import android.util.Base64
import com.example.data.BookmarkEntity
import com.example.data.HistoryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SyncEngine {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun generateSyncPayload(
        historyList: List<HistoryEntity>,
        bookmarkList: List<BookmarkEntity>,
        passphrase: String
    ): String {
        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("device", android.os.Build.MODEL ?: "NoirDevice")
        rootJson.put("timestamp", System.currentTimeMillis())

        val historyArray = JSONArray()
        historyList.forEach { item ->
            val obj = JSONObject()
            obj.put("url", item.url)
            obj.put("title", item.title)
            obj.put("timestamp", item.timestamp)
            historyArray.put(obj)
        }
        rootJson.put("history", historyArray)

        val bookmarkArray = JSONArray()
        bookmarkList.forEach { item ->
            val obj = JSONObject()
            obj.put("url", item.url)
            obj.put("title", item.title)
            obj.put("category", item.category)
            obj.put("timestamp", item.timestamp)
            bookmarkArray.put(obj)
        }
        rootJson.put("bookmarks", bookmarkArray)

        val rawJson = rootJson.toString()
        val encryptedData = encrypt(rawJson, passphrase)
        
        // Add header tag for quick validation
        return "NOIR_SYNC_v1::$encryptedData"
    }

    fun parseSyncPayload(
        payload: String,
        passphrase: String
    ): Pair<List<HistoryEntity>, List<BookmarkEntity>>? {
        try {
            val cleanPayload = if (payload.startsWith("NOIR_SYNC_v1::")) {
                payload.removePrefix("NOIR_SYNC_v1::")
            } else {
                payload.trim()
            }

            val decryptedJson = decrypt(cleanPayload, passphrase) ?: return null
            val rootObj = JSONObject(decryptedJson)

            val historyResult = mutableListOf<HistoryEntity>()
            if (rootObj.has("history")) {
                val hArray = rootObj.getJSONArray("history")
                for (i in 0 until hArray.length()) {
                    val obj = hArray.getJSONObject(i)
                    historyResult.add(
                        HistoryEntity(
                            url = obj.optString("url", ""),
                            title = obj.optString("title", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val bookmarkResult = mutableListOf<BookmarkEntity>()
            if (rootObj.has("bookmarks")) {
                val bArray = rootObj.getJSONArray("bookmarks")
                for (i in 0 until bArray.length()) {
                    val obj = bArray.getJSONObject(i)
                    bookmarkResult.add(
                        BookmarkEntity(
                            url = obj.optString("url", ""),
                            title = obj.optString("title", ""),
                            category = obj.optString("category", "Utama"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            return Pair(historyResult, bookmarkResult)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = passphrase.toByteArray(Charsets.UTF_8)
        val keyBytes = digest.digest(bytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getIv(passphrase: String): IvParameterSpec {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = passphrase.toByteArray(Charsets.UTF_8)
        val ivBytes = digest.digest(bytes)
        return IvParameterSpec(ivBytes)
    }

    private fun encrypt(data: String, passphrase: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getKey(passphrase), getIv(passphrase))
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String, passphrase: String): String? {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getKey(passphrase), getIv(passphrase))
            val decodedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
