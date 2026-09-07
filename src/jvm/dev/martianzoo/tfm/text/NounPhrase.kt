package dev.martianzoo.tfm.text

/** A noun phrase whose number agreement is decided only by the final linearizer. */
internal data class NounPhrase(
    private val singular: String,
    internal val plural: String = singular,
    private val count: Int? = null,
    private val determiner: Determiner? = null,
    private val modifiers: List<Modifier> = emptyList(),
    private val grammaticalNumber: GrammaticalNumber? = null,
    private val upperBounded: Boolean = false,
) {
  init {
    require(count == null || grammaticalNumber == null)
    require(!upperBounded || count != null)
  }

  internal fun number(): GrammaticalNumber =
      when (grammaticalNumber) {
        null ->
            if (count == null || count == 1) GrammaticalNumber.SINGULAR
            else GrammaticalNumber.PLURAL
        else -> grammaticalNumber
      }

  fun noun(): String = if (number() == GrammaticalNumber.SINGULAR) singular else plural

  fun withModifier(modifier: Modifier): NounPhrase = copy(modifiers = modifiers + modifier)

  fun atMost(): NounPhrase = copy(upperBounded = true)

  fun linearize(): String {
    val noun = noun()
    val phrase =
        listOfNotNull(
                "up to".takeIf { upperBounded },
                count?.toString(),
                determiner?.linearize(noun),
                noun,
            )
            .joinToString(" ")
    return modifiers.fold(phrase) { rendered, modifier ->
      rendered + modifier.separator + modifier.linearize()
    }
  }

  internal fun unresolved(): List<Unresolved> = modifiers.flatMap(Modifier::unresolved)

  companion object {
    fun text(text: String): NounPhrase = NounPhrase(text)

    fun plural(text: String): NounPhrase =
        NounPhrase(text, grammaticalNumber = GrammaticalNumber.PLURAL)

    fun you(): NounPhrase = NounPhrase("you", grammaticalNumber = GrammaticalNumber.PLURAL)
  }

  internal enum class GrammaticalNumber {
    SINGULAR,
    PLURAL,
  }
}

internal fun oneOfYour(pluralNoun: String): NounPhrase =
    NounPhrase.text("one")
        .withModifier(
            Modifier.Relation(
                "of",
                NounPhrase(pluralNoun, determiner = Determiner.YOUR),
            )
        )
