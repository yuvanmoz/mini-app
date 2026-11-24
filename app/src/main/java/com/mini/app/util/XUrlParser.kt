package com.mini.app.util

object XUrlParser {
    // Matches x.com/username/status/ID or twitter.com/username/status/ID
    private val STATUS_URL_REGEX = Regex(
        """https?://(?:www\.)?(?:twitter\.com|x\.com)/([A-Za-z0-9_]+)/status/(\d+)"""
    )

    data class ParsedXUrl(
        val username: String,
        val statusId: String,
        val canonicalUrl: String
    )

    fun parse(url: String): ParsedXUrl? {
        val trimmed = url.trim()
        val match = STATUS_URL_REGEX.find(trimmed) ?: return null
        val username = match.groupValues[1]
        val statusId = match.groupValues[2]
        return ParsedXUrl(
            username = username,
            statusId = statusId,
            canonicalUrl = "https://x.com/$username/status/$statusId"
        )
    }

    fun isXUrl(text: String): Boolean {
        return STATUS_URL_REGEX.containsMatchIn(text)
    }

    /** Extract first X/Twitter URL from a block of text (e.g. from share sheet) */
    fun extractFromText(text: String): String? {
        return STATUS_URL_REGEX.find(text)?.value
    }
}
