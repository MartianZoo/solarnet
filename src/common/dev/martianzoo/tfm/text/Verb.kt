package dev.martianzoo.tfm.text

/** A verb whose agreement is decided only when its clause is linearized. */
internal data class Verb(
    private val singular: String,
    private val plural: String = singular,
) {
  fun linearize(number: NounPhrase.GrammaticalNumber?): String =
      if (number == NounPhrase.GrammaticalNumber.SINGULAR) singular else plural

  companion object {
    val BE: Verb = Verb("is", "are")
    val HAVE: Verb = Verb("has", "have")
  }
}
