package dev.martianzoo.tfm.text

/** The sole capitalization and punctuation stage. */
internal data class Sentence(
    private val clause: Clause,
    private val punctuation: String = ".",
) {
  fun linearize(): String = completeSentence(clause.linearize(), punctuation)
}
