package dev.martianzoo.tfm.text

/** A noun phrase whose number agreement is decided only by the final linearizer. */
internal data class NounPhrase(
    private val singular: String,
    internal val plural: String = singular,
    private val count: Int? = null,
    private val determiner: String? = null,
    private val modifiers: List<Modifier> = emptyList(),
) {
  fun noun(): String = if (count == null || count == 1) singular else plural

  fun withModifier(modifier: Modifier): NounPhrase = copy(modifiers = modifiers + modifier)

  fun linearize(): String {
    val phrase = listOfNotNull(count?.toString(), determiner, noun()).joinToString(" ")
    return modifiers.fold(phrase) { rendered, modifier ->
      rendered + modifier.separator + modifier.linearize()
    }
  }

  companion object {
    fun text(text: String): NounPhrase = NounPhrase(text)
  }
}
