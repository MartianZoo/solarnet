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

  internal fun asText(): Rendering<EnglishText> {
    return Rendering(EnglishText.SentenceText(this), unresolved())
  }

  internal fun linearize(): String {
    val text = clause?.linearize() ?: checkNotNull(nounPhrase).linearize()
    return completeSentence(text, punctuation)
  }

  private fun unresolved(): List<Unresolved> =
      clause?.unresolved() ?: checkNotNull(nounPhrase).unresolved()
}
