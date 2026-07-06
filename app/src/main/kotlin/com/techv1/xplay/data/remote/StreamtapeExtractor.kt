package com.techv1.xplay.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.regex.Pattern

/**
 * Extracts a direct download/stream link from a Streamtape embed or video URL.
 * Automatically tries official Streamtape API using credentials first, falling back to Jsoup parsing.
 */
object StreamtapeExtractor {

    private const val API_LOGIN = "67039df88009a5123291"
    private const val API_KEY = "PJk0Og38oJF027z"
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * @param link Streamtape embed URL (may use /e/ or /v/ path)
     * @return Direct streamable URL, or null if extraction fails
     */
    fun getDlLink(link: String): String? {
        // 1. Try resolving using official REST API
        val fileId = extractFileId(link)
        if (fileId != null) {
            val apiLink = getDlLinkViaApi(fileId)
            if (apiLink != null) {
                return apiLink
            }
        }

        // 2. Fallback to Jsoup Scraper if API call fails
        return getDlLinkViaScraper(link)
    }

    private fun extractFileId(link: String): String? {
        return try {
            val pattern = Pattern.compile("/(?:v|e)/([a-zA-Z0-9]+)")
            val matcher = pattern.matcher(link)
            if (matcher.find()) {
                matcher.group(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDlLinkViaApi(fileId: String): String? {
        return try {
            // Step 1: Request a Download Ticket
            val ticketUrl = "https://api.streamtape.com/file/dlticket?file=$fileId&login=$API_LOGIN&key=$API_KEY"
            val ticketRequest = Request.Builder().url(ticketUrl).build()
            val ticketResponse = client.newCall(ticketRequest).execute()
            if (!ticketResponse.isSuccessful) return null
            
            val ticketBody = ticketResponse.body?.string() ?: return null
            val ticketResult = gson.fromJson(ticketBody, StreamtapeTicketResponse::class.java)
            if (ticketResult.status != 200) return null
            
            val ticket = ticketResult.result?.ticket ?: return null
            
            val waitTime = ticketResult.result.waitTime ?: 0
            if (waitTime > 0) {
                Thread.sleep(waitTime * 1000L)
            }

            // Step 2: Request the Direct Download URL using the ticket
            val dlUrl = "https://api.streamtape.com/file/dl?file=$fileId&ticket=$ticket"
            val dlRequest = Request.Builder().url(dlUrl).build()
            val dlResponse = client.newCall(dlRequest).execute()
            if (!dlResponse.isSuccessful) return null
            
            val dlBody = dlResponse.body?.string() ?: return null
            val dlResult = gson.fromJson(dlBody, StreamtapeDlResponse::class.java)
            if (dlResult.status != 200) return null
            
            dlResult.result?.url
        } catch (e: Exception) {
            null
        }
    }

    private fun getDlLinkViaScraper(link: String): String? {
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

    private data class StreamtapeTicketResponse(
        val status: Int,
        val msg: String,
        val result: TicketResult?
    )

    private data class TicketResult(
        val ticket: String?,
        @SerializedName("wait_time") val waitTime: Int?
    )

    private data class StreamtapeDlResponse(
        val status: Int,
        val msg: String,
        val result: DlResult?
    )

    private data class DlResult(
        val url: String?
    )
}
