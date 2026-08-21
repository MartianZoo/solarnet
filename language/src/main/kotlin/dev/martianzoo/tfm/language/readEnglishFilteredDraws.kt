package dev.martianzoo.tfm.language

internal fun readEnglishFilteredDraws(): String =
    English::class.java.getResource("/language/english-filtered-draws.tsv")?.readText()
        ?: error("Missing English filtered-draw data")
