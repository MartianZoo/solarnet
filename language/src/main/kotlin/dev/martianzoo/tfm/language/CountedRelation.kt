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
    fun linearize(uppercaseAny: Boolean = false): String =
        "${if (uppercaseAny && determiner == "any") "ANY" else determiner} $singular"

    fun noun(count: Int): String = if (count == 1) singular else plural
  }

  fun countedObject(count: Int): String = "${target.noun(count)} $phrase ${source.linearize()}"

  fun asRequirement(): String =
      "${source.linearize(uppercaseAny = true)} $phrase ${target.linearize(uppercaseAny = true)}"
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
  val sourceExpression = resolved.sourceDependency(Key(ADJACENCY, 0)) ?: return null
  val targetExpression = resolved.sourceDependency(Key(ADJACENCY, 1)) ?: return null
  val source = renderParticipant(sourceExpression, describers) ?: return null
  val target = renderParticipant(targetExpression, describers) ?: return null
  return CountedRelation(source, target, relation.phrase)
}

private fun renderParticipant(
    expression: Expression,
    describers: Describers,
): CountedRelation.Participant? {
  if (expression.refinement != null || expression.complement) return null
  val placement = describers.positionedFrame(expression.className) ?: return null
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val (determiner, ownedByYou) =
      when {
        resolved.sourceDependency(ownerKey) == describers.anyoneExpression -> "any" to false
        resolved.sourceDependencies.isNotEmpty() -> return null
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
  val placement = positionedFrame(expression.className) ?: return false
  if (placement.unqualifiedMetricOwner == ComponentDescriber.MetricOwner.YOU) return true
  return isPlayerOwned(expression.className)
}
