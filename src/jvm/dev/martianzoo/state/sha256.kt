package dev.martianzoo.state

import java.security.MessageDigest

internal actual suspend fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray()).toHexString()
