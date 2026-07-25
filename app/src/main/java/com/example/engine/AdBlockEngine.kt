package com.example.engine

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

class AdBlockEngine {
    private val blockedCount = AtomicInteger(0)

    private val adDomains = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "adsystem.com",
        "popads.net",
        "popcash.net",
        "taboola.com",
        "outbrain.com",
        "amazon-adsystem.com",
        "criteo.com",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "zedo.com",
        "scorecardresearch.com",
        "exoclick.com",
        "propellerads.com",
        "juicyads.com",
        "adroll.com",
        "smartadserver.com",
        "adform.net",
        "mopub.com",
        "flurry.com",
        "chartbeat.com",
        "hotjar.com",
        "segment.com"
    )

    private val adKeywords = listOf(
        "/ads/", "/ad/", "/adserver/", "/banner/", "popunder", "popup",
        "google_ad_", "ad_client=", "/pagead/", "/analytics.js", "/gtag/js"
    )

    fun isAd(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // Fast host check
        for (domain in adDomains) {
            if (lowerUrl.contains(domain)) {
                blockedCount.incrementAndGet()
                return true
            }
        }

        // Keyword check
        for (keyword in adKeywords) {
            if (lowerUrl.contains(keyword)) {
                blockedCount.incrementAndGet()
                return true
            }
        }

        return false
    }

    fun shouldBlock(request: WebResourceRequest?, isEnabled: Boolean): WebResourceResponse? {
        if (!isEnabled || request == null) return null
        val url = request.url.toString()
        if (isAd(url)) {
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }
        return null
    }

    fun getBlockedCount(): Int = blockedCount.get()

    fun resetCount() {
        blockedCount.set(0)
    }
}
