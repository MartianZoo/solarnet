package dev.martianzoo.tfm.text

/**
 * Applies meaning-preserving paraphrases that depend only on English syntax, in dependency order.
 */
internal fun rewriteAdjacentClauses(clauses: List<Clause>): List<Clause> =
    factorAdjacentPredicates(coalesceStepChanges(clauses))

/** Factors a head shared by every coordinated noun while preserving their distinct modifiers. */
internal fun factorSharedNounHead(
    nouns: List<NounPhrase>,
    conjunction: Conjunction,
): NounPhrase? {
  val first = nouns.firstOrNull() ?: return null
  val modifiers = nouns.flatMap { noun ->
    noun.attributiveModifiers?.members ?: return null
  }
  if (modifiers.size != nouns.size) return null
  val shared = first.copy(attributiveModifiers = null)
  if (nouns.any { it.copy(attributiveModifiers = null) != shared }) return null
  return shared.withAttributiveModifiers(Coordination(modifiers, conjunction))
}

/** Attaches one result as the shared purpose of one or more coordinated costs. */
internal fun attachPurpose(costs: List<Clause.Simple>, result: Clause): Clause {
  require(costs.isNotEmpty())
  val withPurpose = costs.dropLast(1) + costs.last().withModifier(Modifier.Purpose(result))
  return if (withPurpose.size == 1) {
    withPurpose.single()
  } else {
    Clause.Coordinated(Coordination(withPurpose, Conjunction.AND))
  }
}

private fun coalesceStepChanges(clauses: List<Clause>): List<Clause> {
  val result = mutableListOf<Clause>()
  var index = 0
  while (index < clauses.size) {
    if (!isStepChange(clauses[index])) {
      result += clauses[index]
      index++
      continue
    }
    val run = clauses.drop(index).takeWhile(::isStepChange)
    val coalesced = coalesceStepRun(run)
    result +=
        if (coalesced.size == 1) coalesced.single()
        else Clause.Coordinated(Coordination(coalesced, Conjunction.AND))
    index += run.size
  }
  return result
}

private fun coalesceStepRun(clauses: List<Clause>): List<Clause> {
  val result = mutableListOf<Clause>()
  var index = 0
  while (index < clauses.size) {
    val first = clauses[index] as? Clause.Simple
    if (first == null) {
      result += clauses[index]
      index++
      continue
    }
    val matching = nonRepeatingStepRun(first, clauses.drop(index + 1))
    result += coalesceMatchingSteps(matching)
    index += matching.size
  }
  return result
}

private fun nonRepeatingStepRun(
    first: Clause.Simple,
    candidates: List<Clause>,
): List<Clause.Simple> {
  val matching = mutableListOf(first)
  val seen =
      mutableSetOf(checkNotNull(first.predicate.objects).members.single().withoutTrailingSteps())
  candidates.forEach { clause ->
    val candidate = clause as? Clause.Simple ?: return matching
    if (!samePredicateFrame(first, candidate)) return matching
    val noun = checkNotNull(candidate.predicate.objects).members.single().withoutTrailingSteps()
    if (!seen.add(noun)) return matching
    matching += candidate
  }
  return matching
}

private fun coalesceMatchingSteps(clauses: List<Clause.Simple>): Clause {
  if (clauses.size == 1) return clauses.single()
  val nouns = clauses.map { checkNotNull(it.predicate.objects).members.single() }
  val steps = nouns.map { checkNotNull(it.trailingSteps()) }
  if (
      steps.distinct().size == 1 &&
          nouns.map(NounPhrase::withoutTrailingSteps).map(NounPhrase::determiner).distinct().size ==
              1 &&
          nouns.first().determiner == Determiner.YOUR
  ) {
    val noun =
        NounPhrase.coordinated(
                Coordination(nouns.map(NounPhrase::withoutTrailingSteps), Conjunction.AND)
            )
            .withModifier(steps.first().copy(distributed = true))
    return clauses
        .first()
        .copy(predicate = clauses.first().predicate.copy(objects = Coordination.one(noun)))
  }
  return checkNotNull(coordinateClauseObjects(clauses, Conjunction.AND))
}

private fun samePredicateFrame(first: Clause.Simple, candidate: Clause.Simple): Boolean =
    first.subject == candidate.subject &&
        first.predicate.verb == candidate.predicate.verb &&
        first.predicate.modifiers == candidate.predicate.modifiers &&
        first.predicate.complement == candidate.predicate.complement

private fun isStepChange(clause: Clause): Boolean {
  return when (clause) {
    is Clause.Simple -> {
      if (clause.predicate.modifiers.isNotEmpty() || clause.predicate.complement != null) {
        false
      } else {
        val noun = clause.predicate.objects?.members?.singleOrNull()
        noun?.trailingSteps()?.distributed == false
      }
    }
    is Clause.Coordinated ->
        clause.clauses.conjunction == Conjunction.AND && clause.clauses.members.all(::isStepChange)
    is Clause.Either,
    is Clause.Prefaced,
    is Clause.RawPets,
    is Clause.SharedSubject -> false
  }
}

private fun factorAdjacentPredicates(clauses: List<Clause>): List<Clause> {
  val result = mutableListOf<Clause>()
  clauses.forEach { clause ->
    val previous = result.lastOrNull() as? Clause.Simple
    val current = clause as? Clause.Simple
    val previousObjects = previous?.predicate?.objects
    val currentObjects = current?.predicate?.objects
    val factored =
        if (
            previous != null &&
                current != null &&
                previous.predicate.modifiers.isEmpty() &&
                current.predicate.modifiers.isEmpty() &&
                previousObjects != null &&
                currentObjects != null &&
                previousObjects.members.none { it in currentObjects.members }
        ) {
          coordinateClauseObjects(listOf(previous, current), Conjunction.AND)
        } else {
          null
        }
    if (factored != null) {
      result[result.lastIndex] = factored
    } else {
      result += clause
    }
  }
  return result
}

internal fun coordinateSharedSubjectPredicates(
    clauses: List<Clause.Simple>
): Clause.SharedSubject? {
  val subject = clauses.firstOrNull()?.subject ?: return null
  if (clauses.any { it.subject != subject }) return null
  return Clause.SharedSubject(
      subject,
      Coordination(clauses.map(Clause.Simple::predicate), Conjunction.COMMA_OR),
  )
}
