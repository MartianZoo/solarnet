package dev.martianzoo.tfm.text

/** The sole capitalization and punctuation stage. */
internal data class Sentence(
    private val clause: Clause,
    private val punctuation: String = ".",
) {
  fun render(): Rendering<String> =
      Rendering(completeSentence(clause.linearize(), punctuation), clause.unresolved())
}
