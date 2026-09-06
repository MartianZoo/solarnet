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
      internal val subject: NounPhrase? = null,
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
  }
}

internal fun Clause.unresolved(): List<Unresolved> =
    when (this) {
      is Clause.RawPets -> listOf(unresolved)
      is Clause.Simple -> subject?.unresolved().orEmpty() + predicate.unresolved()
      is Clause.Coordinated -> clauses.members.flatMap(Clause::unresolved)
      is Clause.SharedSubject ->
          subject.unresolved() + predicates.members.flatMap(Predicate::unresolved)
      is Clause.Prefaced ->
          when (val preface = preface) {
            is Clause.Preface.Conditional -> preface.condition.unresolved()
            is Clause.Preface.Temporal -> preface.event.unresolved()
            Clause.Preface.FirstAction -> emptyList()
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
