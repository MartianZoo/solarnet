package dev.martianzoo.tfm.text

/** Derived English text for one milestone or award. */
internal data class EnglishGoalRendering(
    val text: String,
    val unresolved: List<Unresolved>,
)
