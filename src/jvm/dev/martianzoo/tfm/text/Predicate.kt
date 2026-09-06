package dev.martianzoo.tfm.text

/** The part of a clause that can be factored across coordinated alternatives. */
internal data class Predicate(
    val verb: String,
    val objects: Coordination<NounPhrase>? = null,
    val modifiers: List<Modifier> = emptyList(),
    val complement: Complement? = null,
) {
  init {
    require(objects == null || complement == null)
  }

  fun withModifier(modifier: Modifier): Predicate = copy(modifiers = modifiers + modifier)

  fun linearize(): String {
    val predicate =
        listOfNotNull(
                verb,
                objects?.linearize(NounPhrase::linearize),
                complement?.linearize(),
            )
            .filter(String::isNotEmpty)
            .joinToString(" ")
    return modifiers.fold(predicate) { rendered, modifier ->
      rendered + modifier.separator + modifier.linearize()
    }
  }

  data class Complement(val clause: Clause) {
    fun linearize(): String = "that ${clause.linearize()}"
  }
}
