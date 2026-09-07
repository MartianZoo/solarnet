package dev.martianzoo.tfm.text

/** The sole capitalization and punctuation stage. */
internal class Sentence
private constructor(
    private val clause: Clause?,
    private val nounPhrase: NounPhrase?,
    private val punctuation: String,
) {
  init {
    require((clause == null) != (nounPhrase == null))
  }

  internal constructor(
      clause: Clause,
      punctuation: String = ".",
  ) : this(clause, null, punctuation)

  internal constructor(
      nounPhrase: NounPhrase,
      punctuation: String = ".",
  ) : this(null, nounPhrase, punctuation)

  fun render(): Rendering<String> {
    val text = clause?.linearize() ?: checkNotNull(nounPhrase).linearize()
    val unresolved = clause?.unresolved() ?: checkNotNull(nounPhrase).unresolved()
    return Rendering(completeSentence(text, punctuation), unresolved)
  }
}
