package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Dependency.Key

/** One countable component related spatially to another component. */
internal data class CountedRelation(
    val source: Participant,
    val target: Participant,
    val phrase: String,
) {
  internal data class Participant(
      val singular: String,
      val plural: String,
      val determiner: String,
      val ownedByYou: Boolean,
  ) {
    fun linearize(): String = "$determiner $singular"

    fun noun(count: Int): String = if (count == 1) singular else plural
  }

  fun countedObject(count: Int): String = "${target.noun(count)} $phrase ${source.linearize()}"

  fun asRequirement(): String = "${source.linearize()} $phrase ${target.linearize()}"
}

internal fun renderCountedRelation(
    expression: Expression,
    describers: Describers,
): CountedRelation? {
  if (expression.refinement != null || expression.complement) return null
  val relation =
      describers.fact(expression.className, ComponentDescriber::spatialRelation) ?: return null
  if (!relation.countedPair) return null
  val resolved = describers.resolveExpression(expression) ?: return null
  if (resolved.authoredDependencies.size != 2) return null
  val sourceExpression = resolved.authored(Key(ADJACENCY, 0)) ?: return null
  val targetExpression = resolved.authored(Key(ADJACENCY, 1)) ?: return null
  val source = renderParticipant(sourceExpression, describers) ?: return null
  val target = renderParticipant(targetExpression, describers) ?: return null
  return CountedRelation(source, target, relation.phrase)
}

private fun renderParticipant(
    expression: Expression,
    describers: Describers,
): CountedRelation.Participant? {
  if (expression.refinement != null || expression.complement) return null
  val placement =
      describers.fact(expression.className, ComponentDescriber::placement) ?: return null
  val resolved = describers.resolveExpression(expression) ?: return null
  val owner = resolved.authored(Key(OWNED, 0))
  val (determiner, ownedByYou) =
      when {
        resolved.authoredDependencies.size == 1 && owner == describers.anyoneExpression ->
            "any" to false
        resolved.authoredDependencies.isNotEmpty() -> return null
        placement.article == "this" -> "this" to false
        describers.isUnqualifiedPlacementOwned(expression) -> "your" to true
        else -> placement.article to false
      }
  return CountedRelation.Participant(
      placement.singular,
      placement.plural,
      determiner,
      ownedByYou,
  )
}

private val ADJACENCY = cn("Adjacency")

private fun Describers.isUnqualifiedPlacementOwned(expression: Expression): Boolean {
  val placement = fact(expression.className, ComponentDescriber::placement) ?: return false
  if (placement.unqualifiedMetricOwner == ComponentDescriber.MetricOwner.YOU) return true
  val requirement = fact(expression.className, ComponentDescriber::requirement) ?: return false
  if (requirement.ownedCount != null) return true
  val minimum = requirement.minimum as? ComponentDescriber.Requirement.Bound.Count ?: return false
  return minimum.syntax == ComponentDescriber.Requirement.CountSyntax.REQUIRES_OWNED_COUNT
}
