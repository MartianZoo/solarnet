package dev.martianzoo.tfm.text

/** Derived English text for one component. */
internal data class EnglishRendering(
    val text: String,
    val unresolved: List<Unresolved>,
)
