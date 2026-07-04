package com.techv1.xplay.data.remote

import org.jsoup.Jsoup
import java.util.regex.Pattern

/**
 * Extracts a direct download/stream link from a Streamtape embed or video URL.
 * Adapted from the original Java implementation in the project spec.
 */
object StreamtapeExtractor {

    /**
     * @param link Streamtape embed URL (may use /e/ or /v/ path)
     * @return Direct streamable URL, or null if extraction fails
     */
    fun getDlLink(link: String): String? {
        return try {
            val resolvedLink = if (link.contains("/e/")) link.replace("/e/", "/v/") else link
            val doc = Jsoup.connect(resolvedLink).get()
            val html = doc.html()

            val norobotPattern = Pattern.compile(
                "document\\.getElementById\\('norobotlink'\\)\\.innerHTML = (.+);"
            )
            val norobotMatcher = norobotPattern.matcher(html)
            if (!norobotMatcher.find()) return null

            val norobotContent = norobotMatcher.group(1) ?: return null

            val tokenPattern = Pattern.compile("token=([^&']+)")
            val tokenMatcher = tokenPattern.matcher(norobotContent)
            if (!tokenMatcher.find()) return null

            val token = tokenMatcher.group(1) ?: return null

            val divElements = doc.select("div#ideoooolink[style=display:none;]")
            if (divElements.isEmpty()) return null

            val streamtapePath = divElements.first()!!.text()
            "https:/$streamtapePath&token=$token&dl=1s"
        } catch (e: Exception) {
            null
        }
    }
}
