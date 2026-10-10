package dev.martianzoo.tfm.text

/** A clause retained as structure until its enclosing Pets element has been rendered. */
internal sealed interface Clause {
  fun linearize(): String

  /** Pets source retained visibly when this renderer does not understand the node. */
  data class RawPets(
      val source: String,
      val refusals: List<Unresolved>,
  ) : Clause {
    init {
      require(refusals.isNotEmpty())
    }

    constructor(unresolved: Unresolved) : this(unresolved.node.toString(), listOf(unresolved))

    override fun linearize(): String = "[$source]"
  }

  data class Simple(
      val predicate: Predicate,
      internal val subject: NounPhrase? = null,
  ) : Clause {
    fun withModifier(modifier: Modifier): Simple =
        copy(predicate = predicate.withModifier(modifier))

    override fun linearize(): String =
        listOfNotNull(subject?.linearize(), predicate.linearize(subject?.number()))
            .joinToString(" ")
  }

  data class Coordinated(public val clauses: Coordination<Clause>) : Clause {
    override fun linearize(): String = clauses.linearize(Clause::linearize)
  }

  data class Either(public val alternatives: Coordination<Clause>) : Clause {
    override fun linearize(): String = "either ${alternatives.linearize(Clause::linearize)}"
  }

  data class SharedSubject(
      val subject: NounPhrase,
      val predicates: Coordination<Predicate>,
  ) : Clause {
    override fun linearize(): String =
        "${subject.linearize()} ${predicates.linearize { it.linearize(subject.number()) }}"
  }

  data class Prefaced(val preface: Preface, val clause: Clause) : Clause {
    override fun linearize(): String = "${preface.linearize()}, ${clause.linearize()}"
  }

  sealed interface Preface {
    fun linearize(): String

    data class Conditional(val condition: Clause) : Preface {
      override fun linearize(): String = "if ${condition.linearize()}"
    }

    data class Temporal(val event: Clause) : Preface {
      override fun linearize(): String = "when ${event.linearize()}"
    }

    data object FirstAction : Preface {
      override fun linearize(): String = "as your first action"
    }

    data object OncePerAction : Preface {
      override fun linearize(): String = "once per action you take"
    }
  }
}

/** Whether a clause can follow a modal or purpose verb without introducing another subject. */
internal fun canBeInfinitive(clause: Clause): Boolean =
    when (clause) {
      is Clause.Simple -> clause.subject == null
      is Clause.Coordinated -> clause.clauses.members.all(::canBeInfinitive)
      is Clause.RawPets -> true
      is Clause.Either,
      is Clause.Prefaced,
      is Clause.SharedSubject -> false
    }

internal fun Clause.unresolved(): List<Unresolved> =
    when (this) {
      is Clause.RawPets -> refusals
      is Clause.Simple -> subject?.unresolved().orEmpty() + predicate.unresolved()
      is Clause.Coordinated -> clauses.members.flatMap(Clause::unresolved)
      is Clause.Either -> alternatives.members.flatMap(Clause::unresolved)
      is Clause.SharedSubject ->
          subject.unresolved() + predicates.members.flatMap(Predicate::unresolved)
      is Clause.Prefaced ->
          when (val preface = preface) {
            is Clause.Preface.Conditional -> preface.condition.unresolved()
            is Clause.Preface.Temporal -> preface.event.unresolved()
            Clause.Preface.FirstAction,
            Clause.Preface.OncePerAction -> emptyList()
          } + clause.unresolved()
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

/** Coordinates predicate objects only when the surrounding predicate structure is shared. */
private fun coordinatePredicateObjects(
    predicates: List<Predicate>,
    conjunction: Conjunction,
): Predicate? {
  val first = predicates.firstOrNull() ?: return null
  if (
      predicates.any {
        it.verb != first.verb ||
            it.modifiers != first.modifiers ||
            it.complement != first.complement
      }
  ) {
    return null
  }
  val objects = predicates.map { it.objects ?: return null }
  return first.copy(
      objects = Coordination(objects.flatMap { it.members }, conjunction),
  )
}
