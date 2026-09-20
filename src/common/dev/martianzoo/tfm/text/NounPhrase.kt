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
    private val coordinatedHead: Coordination<NounPhrase>? = null,
) {
  init {
    require(count == null || grammaticalNumber == null)
    require(!upperBounded || count != null)
    require(coordinatedHead == null || singular.isEmpty())
  }

  internal fun number(): GrammaticalNumber =
      when (grammaticalNumber) {
        null ->
            if (count == null || count == 1) GrammaticalNumber.SINGULAR
            else GrammaticalNumber.PLURAL
        else -> grammaticalNumber
      }

  fun noun(): String =
      coordinatedHead?.linearize(NounPhrase::linearize)
          ?: if (number() == GrammaticalNumber.SINGULAR) singular else plural

  fun withModifier(modifier: Modifier): NounPhrase = copy(modifiers = modifiers + modifier)

  fun withDeterminer(determiner: Determiner): NounPhrase = copy(determiner = determiner)

  fun asPlural(): NounPhrase = copy(count = null, grammaticalNumber = GrammaticalNumber.PLURAL)

  fun quantified(count: Int): NounPhrase = copy(count = count, grammaticalNumber = null)

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

  internal fun unresolved(): List<Unresolved> =
      coordinatedHead?.members.orEmpty().flatMap(NounPhrase::unresolved) +
          modifiers.flatMap(Modifier::unresolved)

  companion object {
    fun text(text: String): NounPhrase = NounPhrase(text)

    fun plural(text: String): NounPhrase =
        NounPhrase(text, grammaticalNumber = GrammaticalNumber.PLURAL)

    fun coordinated(nouns: Coordination<NounPhrase>): NounPhrase =
        NounPhrase("", coordinatedHead = nouns)

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
