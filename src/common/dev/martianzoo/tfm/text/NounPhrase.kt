package dev.martianzoo.tfm.text

/** A noun phrase whose number agreement is decided only by the final linearizer. */
internal data class NounPhrase(
    private val singular: String,
    internal val plural: String = singular,
    private val count: Int? = null,
    internal val determiner: Determiner? = null,
    private val modifiers: List<Modifier> = emptyList(),
    private val grammaticalNumber: GrammaticalNumber? = null,
    private val upperBounded: Boolean = false,
    private val coordinatedHead: Coordination<NounPhrase>? = null,
    internal val attributiveModifiers: Coordination<NounPhrase>? = null,
    private val rawPets: Unresolved? = null,
) {
  init {
    require(count == null || grammaticalNumber == null)
    require(!upperBounded || count != null)
    require(coordinatedHead == null || singular.isEmpty())
    require(rawPets == null || singular.isEmpty())
    require(rawPets == null || coordinatedHead == null)
    require(rawPets == null || count == null)
    require(rawPets == null || determiner == null)
    require(rawPets == null || modifiers.isEmpty())
    require(rawPets == null || grammaticalNumber == null)
    require(rawPets == null || !upperBounded)
    require(rawPets == null || attributiveModifiers == null)
  }

  internal fun number(): GrammaticalNumber =
      when (grammaticalNumber) {
        null ->
            if (count == null || count == 1) GrammaticalNumber.SINGULAR
            else GrammaticalNumber.PLURAL
        else -> grammaticalNumber
      }

  /** The selected head noun together with any attributive modifier. */
  fun noun(): String {
    rawPets?.let {
      return "[${it.node}]"
    }
    val head =
        coordinatedHead?.linearize(NounPhrase::linearize)
            ?: if (number() == GrammaticalNumber.SINGULAR) singular else plural
    return listOfNotNull(
            attributiveModifiers?.linearize(NounPhrase::linearize),
            head,
        )
        .joinToString(" ")
  }

  fun withModifier(modifier: Modifier): NounPhrase =
      if (rawPets != null) this else copy(modifiers = modifiers + modifier)

  fun withDeterminer(determiner: Determiner): NounPhrase =
      if (rawPets != null) this else copy(determiner = determiner)

  fun withAttributiveModifier(modifier: NounPhrase): NounPhrase =
      withAttributiveModifiers(Coordination.one(modifier))

  fun withAttributiveModifiers(modifiers: Coordination<NounPhrase>): NounPhrase =
      if (rawPets != null) this
      else
          copy(
              attributiveModifiers =
                  Coordination(
                      modifiers.members.map {
                        it.copy(count = null, determiner = null, upperBounded = false)
                      },
                      modifiers.conjunction,
                  )
          )

  fun asPlural(): NounPhrase =
      if (rawPets != null) this
      else copy(count = null, grammaticalNumber = GrammaticalNumber.PLURAL)

  fun quantified(count: Int): NounPhrase =
      if (rawPets != null) this else copy(count = count, grammaticalNumber = null)

  fun atMost(): NounPhrase = if (rawPets != null) this else copy(upperBounded = true)

  internal fun trailingSteps(): Modifier.Steps? = modifiers.lastOrNull() as? Modifier.Steps

  internal fun withoutTrailingSteps(): NounPhrase {
    require(trailingSteps() != null)
    return copy(modifiers = modifiers.dropLast(1))
  }

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
      listOfNotNull(rawPets) +
          coordinatedHead?.members.orEmpty().flatMap(NounPhrase::unresolved) +
          attributiveModifiers?.members.orEmpty().flatMap(NounPhrase::unresolved) +
          modifiers.flatMap(Modifier::unresolved)

  companion object {
    fun text(text: String): NounPhrase = NounPhrase(text)

    fun plural(text: String): NounPhrase =
        NounPhrase(text, grammaticalNumber = GrammaticalNumber.PLURAL)

    fun coordinated(nouns: Coordination<NounPhrase>): NounPhrase =
        NounPhrase("", coordinatedHead = nouns)

    fun rawPets(unresolved: Unresolved): NounPhrase = NounPhrase("", rawPets = unresolved)

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
