package dev.martianzoo.tfm.language

internal actual fun readEnglishCardText(): String =
    English::class.java.getResource("/language/english-card-text.tsv")?.readText()
        ?: error("Missing English card-text data")

internal actual fun readEnglishFilteredDraws(): String =
    English::class.java.getResource("/language/english-filtered-draws.tsv")?.readText()
        ?: error("Missing English filtered-draw data")
