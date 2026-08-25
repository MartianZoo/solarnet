package dev.martianzoo.tfm.text

/** A clause retained as structure until its enclosing Pets element has been rendered. */
internal sealed interface Clause {
  fun linearize(): String

  /** Pets source retained visibly when this renderer does not understand the node. */
  data class RawPets(val unresolved: Unresolved) : Clause {
    override fun linearize(): String = "[${unresolved.node}]"
  }

  data class Simple(
      val predicate: Predicate,
      public val subject: NounPhrase? = null,
  ) : Clause {
    fun withModifier(modifier: Modifier): Simple =
        copy(predicate = predicate.withModifier(modifier))

    override fun linearize(): String =
        listOfNotNull(subject?.linearize(), predicate.linearize()).joinToString(" ")
  }

  data class Coordinated(public val clauses: Coordination<Clause>) : Clause {
    override fun linearize(): String = clauses.linearize(Clause::linearize)
  }

  data class SharedSubject(
      val subject: NounPhrase,
      val predicates: Coordination<Predicate>,
  ) : Clause {
    override fun linearize(): String =
        "${subject.linearize()} ${predicates.linearize(Predicate::linearize)}"
  }

  data class Prefaced(val preface: String, val clause: Clause) : Clause {
    override fun linearize(): String = "$preface, ${clause.linearize()}"
  }
}

internal fun Clause.unresolved(): List<Unresolved> =
    when (this) {
      is Clause.RawPets -> listOf(unresolved)
      is Clause.Simple -> emptyList()
      is Clause.Coordinated -> clauses.members.flatMap(Clause::unresolved)
      is Clause.SharedSubject -> emptyList()
      is Clause.Prefaced -> clause.unresolved()
    }

/** The part of a clause that can be factored across coordinated alternatives. */
internal data class Predicate(
    val verb: String,
    val objects: Coordination<NounPhrase>? = null,
    val modifiers: List<Modifier> = emptyList(),
) {
  fun withModifier(modifier: Modifier): Predicate = copy(modifiers = modifiers + modifier)

  fun linearize(): String {
    val predicate =
        listOfNotNull(verb, objects?.linearize(NounPhrase::linearize))
            .filter(String::isNotEmpty)
            .joinToString(" ")
    return modifiers.fold(predicate) { rendered, modifier ->
      rendered + modifier.separator + modifier.linearize()
    }
  }
}

/** Coordinates predicate objects only when the surrounding predicate structure is shared. */
internal fun coordinatePredicateObjects(
    predicates: List<Predicate>,
    conjunction: Conjunction,
): Predicate? {
  val first = predicates.firstOrNull() ?: return null
  if (predicates.any { it.verb != first.verb || it.modifiers != first.modifiers }) return null
  val objects = predicates.map { it.objects ?: return null }
  return first.copy(
      objects = Coordination(objects.flatMap { it.members }, conjunction),
  )
}

/** Coordinates clause objects without discarding a subject owned by each alternative. */
internal fun coordinateClauseObjects(
    clauses: List<Clause.Simple>,
    conjunction: Conjunction,
): Clause.Simple? {
  val first = clauses.firstOrNull() ?: return null
  if (clauses.any { it.subject != first.subject }) return null
  val predicate =
      coordinatePredicateObjects(clauses.map(Clause.Simple::predicate), conjunction) ?: return null
  return first.copy(predicate = predicate)
}

/** A noun phrase whose number agreement is decided only by the final linearizer. */
internal data class NounPhrase(
    private val singular: String,
    public val plural: String = singular,
    private val count: Int? = null,
    private val determiner: String? = null,
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
  val separator: String
    get() = " "

  fun linearize(): String

  data class Phrase(private val text: String) : Modifier {
    override fun linearize(): String = text
  }

  data class Parenthetical(private val text: String) : Modifier {
    override fun linearize(): String = "($text)"
  }

  data class Supplement(val text: String) : Modifier {
    override val separator: String = ", "

    override fun linearize(): String = text
  }
}

/** Ordered conjunction or disjunction. */
internal data class Coordination<T>(
    val members: List<T>,
    private val conjunction: Conjunction? = null,
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
      Conjunction.COMMA_OR -> parts.joinToString(", or ")
      Conjunction.EITHER_OR -> "either ${englishAlternatives(parts)}"
      Conjunction.THEN -> parts.joinToString(", then ")
    }
  }

  companion object {
    fun <T> one(member: T): Coordination<T> = Coordination(listOf(member))
  }
}

internal enum class Conjunction {
  AND,
  OR,
  COMMA_OR,
  EITHER_OR,
  THEN,
}

/** The sole capitalization and punctuation boundary. */
internal data class Sentence(
    private val clause: Clause,
    private val punctuation: String = ".",
) {
  fun linearize(): String = completeSentence(clause.linearize(), punctuation)
}
