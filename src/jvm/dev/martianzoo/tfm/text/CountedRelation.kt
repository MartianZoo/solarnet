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
      val ownership: ComponentDescriber.OwnershipPhrase,
  ) {
    val ownedByYou: Boolean
      get() = ownership == ComponentDescriber.OwnershipPhrase.YOURS

    fun reference(): NounPhrase = nounPhrase(determiner = determiner)

    fun referenceWithoutOwnership(determiner: String): NounPhrase =
        NounPhrase(singular, plural, determiner = determiner)

    fun counted(count: Int?): NounPhrase = nounPhrase(count = count)

    private fun nounPhrase(
        count: Int? = null,
        determiner: String? = null,
    ): NounPhrase {
      val noun = NounPhrase(singular, plural, count = count, determiner = determiner)
      return if (ownership == ComponentDescriber.OwnershipPhrase.ANYONES) {
        noun.withModifier(Modifier.Phrase("anyone owns"))
      } else {
        noun
      }
    }
  }

  fun countedObject(count: Int?): NounPhrase =
      target.counted(count).withModifier(Modifier.Relation(phrase, source.reference()))

  fun asRequirement(): NounPhrase =
      source.reference().withModifier(Modifier.Relation(phrase, target.reference()))
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
      ownership,
  )
}

private val ADJACENCY = cn("Adjacency")
