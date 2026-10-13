package dev.martianzoo.state

/** Platform SHA-256 over UTF-8, rendered as lowercase hexadecimal. */
internal expect suspend fun sha256(value: String): String
