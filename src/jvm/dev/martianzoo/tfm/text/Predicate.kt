package dev.martianzoo.tfm.text

/** The part of a clause that can be factored across coordinated alternatives. */
internal data class Predicate(
    val verb: Verb,
    val objects: Coordination<NounPhrase>? = null,
    val modifiers: List<Modifier> = emptyList(),
    val complement: Complement? = null,
) {
  init {
    require(objects == null || complement !is Complement.That)
  }

  fun withModifier(modifier: Modifier): Predicate = copy(modifiers = modifiers + modifier)

  fun linearize(subjectNumber: NounPhrase.GrammaticalNumber? = null): String {
    val predicate =
        listOfNotNull(
                verb.linearize(subjectNumber),
                objects?.linearize(NounPhrase::linearize),
                complement?.linearize(),
            )
            .filter(String::isNotEmpty)
            .joinToString(" ")
    return modifiers.fold(predicate) { rendered, modifier ->
      rendered + modifier.separator + modifier.linearize()
    }
  }

  internal fun unresolved(): List<Unresolved> =
      objects?.members.orEmpty().flatMap(NounPhrase::unresolved) +
          modifiers.flatMap(Modifier::unresolved) +
          complement?.clause?.unresolved().orEmpty()

  sealed interface Complement {
    val clause: Clause

    fun linearize(): String

    data class That(override val clause: Clause) : Complement {
      override fun linearize(): String = "that ${clause.linearize()}"
    }

    data class BareInfinitive(override val clause: Clause) : Complement {
      override fun linearize(): String = clause.linearize()
    }
  }
}
