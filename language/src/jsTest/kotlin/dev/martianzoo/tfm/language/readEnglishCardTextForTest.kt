package dev.martianzoo.tfm.language

internal actual fun readEnglishCardText(fileName: String): String =
    readEnglishResource(fileName, "card-text")
