package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Exact
import dev.martianzoo.pets.types.Class as PetClass

/** Content audit shared by the Canon sweep and deliberately malformed card fixtures. */
internal fun validateCardClassification(catalog: TfmCatalog) {
  val table = catalog.classTable
  if (table.findClass(TfmClasses.TAG) == null) return
  val eventCard = table.findClass(TfmClasses.EVENT_CARD)
  val eventTagRequirement: Requirement = parse("=1 EventTag<This>")
  eventCard?.let {
    require(eventTagRequirement in it.declaration.invariants) {
      "EventCard must declare HAS $eventTagRequirement"
    }
  }
  val projectCard = table.findClass(TfmClasses.PROJECT_CARD)
  val activeCard = table.findClass(TfmClasses.ACTIVE_CARD)
  val automatedCard = table.findClass(TfmClasses.AUTOMATED_CARD)
  catalog.cards.forEach { card ->
    if (TfmClasses.EVENT_TAG in cardTags(card).elements) {
      require(eventCard != null && card.isSubtypeOf(eventCard)) {
        "non-EventCard ${card.className} has an EventTag"
      }
    }
    if (
        projectCard != null &&
            eventCard != null &&
            activeCard != null &&
            automatedCard != null &&
            cardBack(card)?.isSubtypeOf(projectCard) == true &&
            !card.isSubtypeOf(eventCard)
    ) {
      val hasNontrivialBehavior =
          cardActions(card).isNotEmpty() ||
              card.invariants.filterIsInstance<Exact>().any {
                val expression = (it.countedMetric as? Count)?.expression
                it.expected > 0 &&
                    expression != null &&
                    THIS.expression in expression.arguments &&
                    carriesPersistentBehavior(table.getClass(expression.className))
              } ||
              cardEffects(card).any { effect ->
                when {
                  isEndTrigger(effect.untransformed.trigger) -> false
                  !isSelfGainTrigger(effect.untransformed.trigger) -> true
                  else ->
                      effect.untransformed.instruction.descendantsOfType<Gain>().any { gain ->
                        carriesPersistentBehavior(table.getClass(gain.gaining.className))
                      }
                }
              }
      val active = card.isSubtypeOf(activeCard)
      val automated = card.isSubtypeOf(automatedCard)
      require(active == hasNontrivialBehavior && automated == !hasNontrivialBehavior) {
        "${card.className} must be ActiveCard exactly when it has actions or persistent effects; " +
            "otherwise it must be AutomatedCard"
      }
    }
  }
}

private fun isEndTrigger(trigger: Trigger): Boolean =
    when (trigger) {
      is OnGainOf -> trigger.expression.className == TfmClasses.END
      is Trigger.Or -> trigger.triggers.all(::isEndTrigger)
      is Trigger.WrappingTrigger -> isEndTrigger(trigger.inner)
      is Trigger.OnRemoveOf,
      WhenGain,
      Trigger.WhenRemove -> false
    }

private fun isSelfGainTrigger(trigger: Trigger): Boolean =
    when (trigger) {
      WhenGain -> true
      is Trigger.Or -> trigger.triggers.all(::isSelfGainTrigger)
      is Trigger.WrappingTrigger -> isSelfGainTrigger(trigger.inner)
      is OnGainOf,
      is Trigger.OnRemoveOf,
      Trigger.WhenRemove -> false
    }

private fun carriesPersistentBehavior(klass: PetClass): Boolean =
    klass.allSuperclasses().any { superclass ->
      superclass.declaration.authoredActions.isNotEmpty() ||
          superclass.declaration.authoredEffects.any { effect ->
            !isSelfGainTrigger(effect.untransformed.trigger) &&
                !isEndTrigger(effect.untransformed.trigger)
          }
    }
