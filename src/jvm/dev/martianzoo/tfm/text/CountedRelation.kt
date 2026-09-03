package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key

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
      val ownershipSuffix: String,
      val ownedByYou: Boolean,
  ) {
    fun linearize(): String = "$determiner $singular$ownershipSuffix"

    fun noun(count: Int): String = "${if (count == 1) singular else plural}$ownershipSuffix"
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
  val (determiner, ownership) =
      when {
        resolved.sourceDependency(ownerKey) == describers.anyoneExpression ->
            when (placement.anyoneOwnership ?: return null) {
              ComponentDescriber.OwnershipPhrase.IMPLICIT ->
                  "any" to ComponentDescriber.OwnershipPhrase.IMPLICIT
              ComponentDescriber.OwnershipPhrase.ANYONES ->
                  "a" to ComponentDescriber.OwnershipPhrase.ANYONES
              ComponentDescriber.OwnershipPhrase.YOURS -> return null
            }
        resolved.sourceDependencies.isNotEmpty() -> return null
        placement.article == "this" -> "this" to ComponentDescriber.OwnershipPhrase.IMPLICIT
        else ->
            when (placement.unqualifiedOwnership) {
              ComponentDescriber.OwnershipPhrase.YOURS ->
                  "your" to ComponentDescriber.OwnershipPhrase.YOURS
              ComponentDescriber.OwnershipPhrase.ANYONES ->
                  "a" to ComponentDescriber.OwnershipPhrase.ANYONES
              ComponentDescriber.OwnershipPhrase.IMPLICIT,
              null -> placement.article to ComponentDescriber.OwnershipPhrase.IMPLICIT
            }
      }
  val placementNoun = ComponentDescriber.Noun.Counted(placement.singular, placement.plural)
  val noun = if (determiner == "this") placementNoun else placement.referenceNoun ?: placementNoun
  return CountedRelation.Participant(
      noun.singular,
      noun.plural,
      determiner,
      ownershipSuffix =
          if (ownership == ComponentDescriber.OwnershipPhrase.ANYONES) " anyone owns" else "",
      ownedByYou = ownership == ComponentDescriber.OwnershipPhrase.YOURS,
  )
}

private val ADJACENCY = cn("Adjacency")
