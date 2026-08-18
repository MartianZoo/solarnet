package dev.martianzoo.tfm.language

internal actual fun readEnglishCardText(): String =
    English::class.java.getResource("/language/english-card-text.tsv")?.readText()
        ?: error("Missing English card-text data")
