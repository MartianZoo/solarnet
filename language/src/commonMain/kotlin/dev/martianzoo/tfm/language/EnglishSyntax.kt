package dev.martianzoo.tfm.language

/** A clause retained as structure until its enclosing Pets element has been rendered. */
internal sealed interface Clause {
  fun linearize(): String

  data class Simple(
      val predicate: Predicate,
      val subject: NounPhrase? = null,
  ) : Clause {
    fun withModifier(modifier: Modifier): Simple =
        copy(predicate = predicate.withModifier(modifier))

    override fun linearize(): String =
        listOfNotNull(subject?.linearize(), predicate.linearize()).joinToString(" ")
  }

  data class Coordinated(val clauses: Coordination<Clause>) : Clause {
    override fun linearize(): String = clauses.linearize(Clause::linearize)
  }
}

/** The part of a clause that can be factored across coordinated alternatives. */
internal data class Predicate(
    val verb: String,
    val objects: Coordination<NounPhrase>,
    val modifiers: List<Modifier> = emptyList(),
) {
  fun withModifier(modifier: Modifier): Predicate = copy(modifiers = modifiers + modifier)

  fun linearize(): String = buildList {
    add(verb)
    add(objects.linearize(NounPhrase::linearize))
    addAll(modifiers.map(Modifier::linearize))
  }
      .filter(String::isNotEmpty)
      .joinToString(" ")
}

/** A noun phrase whose number agreement is decided only by the final linearizer. */
internal data class NounPhrase(
    val singular: String,
    val plural: String = singular,
    val count: Int? = null,
    val determiner: String? = null,
) {
  fun noun(): String = if (count == null || count == 1) singular else plural

  fun linearize(): String {
    return listOfNotNull(count?.toString(), determiner, noun()).joinToString(" ")
  }

  companion object {
    fun text(text: String): NounPhrase = NounPhrase(text)
  }
}

/**
 * A clause modifier kept separate so factoring cannot cross different destinations or conditions.
 */
internal sealed interface Modifier {
  fun linearize(): String

  data class Phrase(val text: String) : Modifier {
    override fun linearize(): String = text
  }

  data class Parenthetical(val text: String) : Modifier {
    override fun linearize(): String = "($text)"
  }
}

/** Ordered conjunction or disjunction. */
internal data class Coordination<T>(
    val members: List<T>,
    val conjunction: Conjunction? = null,
) {
  init {
    require(members.isNotEmpty())
    require(members.size == 1 || conjunction != null)
  }

  fun linearize(render: (T) -> String): String {
    val parts = members.map(render)
    return when (conjunction) {
      null -> parts.single()
      Conjunction.AND -> englishList(parts)
      Conjunction.OR -> englishAlternatives(parts)
      Conjunction.EITHER_OR -> "either ${englishAlternatives(parts)}"
    }
  }

  companion object {
    fun <T> one(member: T): Coordination<T> = Coordination(listOf(member))
  }
}

internal enum class Conjunction {
  AND,
  OR,
  EITHER_OR,
}

/** The sole capitalization and punctuation boundary. */
internal data class Sentence(
    val clause: Clause,
    val punctuation: String = ".",
) {
  fun linearize(): String = completeSentence(clause.linearize(), punctuation)
}
