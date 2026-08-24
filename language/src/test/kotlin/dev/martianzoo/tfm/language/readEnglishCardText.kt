package dev.martianzoo.tfm.language

internal fun readEnglishCardText(fileName: String): String =
    English::class.java.getResource("/language/$fileName")?.readText()
        ?: error("Missing English card-text data: $fileName")
