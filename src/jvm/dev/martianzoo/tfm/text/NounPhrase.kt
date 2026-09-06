package dev.martianzoo.tfm.text

/** A noun phrase whose number agreement is decided only by the final linearizer. */
internal data class NounPhrase(
    private val singular: String,
    internal val plural: String = singular,
    private val count: Int? = null,
    private val determiner: String? = null,
    private val modifiers: List<Modifier> = emptyList(),
    private val grammaticalNumber: GrammaticalNumber? = null,
    private val quantifier: Quantifier? = null,
) {
  init {
    require(count == null || grammaticalNumber == null)
    require(quantifier == null || count != null)
  }

  fun noun(): String =
      when (grammaticalNumber) {
        GrammaticalNumber.SINGULAR -> singular
        GrammaticalNumber.PLURAL -> plural
        null -> if (count == null || count == 1) singular else plural
      }

  fun withModifier(modifier: Modifier): NounPhrase = copy(modifiers = modifiers + modifier)

  fun atMost(): NounPhrase = copy(quantifier = Quantifier.UP_TO)

  fun linearize(): String {
    val phrase =
        listOfNotNull(quantifier?.text, count?.toString(), determiner, noun()).joinToString(" ")
    return modifiers.fold(phrase) { rendered, modifier ->
      rendered + modifier.separator + modifier.linearize()
    }
  }

  internal fun unresolved(): List<Unresolved> = modifiers.flatMap(Modifier::unresolved)

  companion object {
    fun text(text: String): NounPhrase = NounPhrase(text)
  }

  internal enum class GrammaticalNumber {
    SINGULAR,
    PLURAL,
  }

  internal enum class Quantifier(val text: String) {
    UP_TO("up to"),
  }
}
