package dev.martianzoo.search

import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.types.Class as PetClass
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardTags

/**
 * Authored actions or ongoing behavior, excluding direct play benefits and end-only scoring.
 * Includes behavior supplied by attached components or gains in nondirect self-gain effects. This
 * syntactic classification is independent of the card's declared color and query form; conditions
 * are not evaluated and arbitrary chains of gained components are not followed.
 */
internal fun hasPersistentBehavior(card: PetClass): Boolean {
  val table = card.classTable
  val tags = cardTags(card).elements
  return cardActions(card).isNotEmpty() ||
      card.invariants.filterIsInstance<Exact>().any {
        val expression = (it.countedMetric as? Count)?.expression
        it.expected > 0 &&
            expression != null &&
            THIS.expression in expression.arguments &&
            expression.className !in tags &&
            carriesPersistentBehavior(table.getClass(expression.className))
      } ||
      cardEffects(card).any { effect ->
        when {
          isEndOnly(effect.trigger) -> false
          !isSelfGainOnly(effect.trigger) -> true
          else ->
              effect.instruction.descendantsOfType<Gain>().any {
                carriesPersistentBehavior(table.getClass(it.gaining.className))
              }
        }
      }
}

private fun carriesPersistentBehavior(klass: PetClass): Boolean =
    klass.allSuperclasses().any { superclass ->
      superclass.declaration.authoredActions.isNotEmpty() ||
          superclass.declaration.authoredEffects.any {
            !isSelfGainOnly(it.trigger) && !isEndOnly(it.trigger)
          }
    }
