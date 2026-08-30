package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.Multiset
import dev.martianzoo.tfm.canon.TfmClasses.CARD_RESOURCE
import dev.martianzoo.tfm.canon.TfmClasses.RESOURCE_CARD
import dev.martianzoo.tfm.canon.TfmClasses.TAG

/** The card back represented by this card face's `Class<CardBack>` dependency. */
public fun cardBack(card: Class): Class? {
  val cardBack = card.classTable.getClass(CARD_BACK)
  return representedClasses(card).singleOrNull { it.isSubtypeOf(cardBack) }
}

/** Tags printed on this card, including the Event tag authored on event declarations. */
public fun cardTags(card: Class): Multiset<ClassName> {
  val tag = card.classTable.getClass(TAG)
  val names =
      card.declaration.authoredEffects
          .filter { it.automatic && it.trigger == WhenGain }
          .flatMap { it.instruction.descendantsOfType<Gain>() }
          .flatMap { gained ->
            val gainedClass = card.classTable.getClass(gained.gaining.className)
            if (gainedClass.isSubtypeOf(tag)) {
              List((gained.count as ActualScalar).value) { gainedClass.className }
            } else {
              emptyList()
            }
          }
  return HashMultiset.of(names)
}

/** Instructions performed when this card enters play. */
public fun cardImmediate(card: Class): InstructionGroup? {
  val instructions =
      card.declaration.authoredEffects
          .filter { !it.automatic && it.trigger == WhenGain }
          .map { it.instruction }
  return instructions
      .takeIf { it.isNotEmpty() }
      ?.let { InstructionGroup.of(InstructionGroup.createTree(it)) }
}

/** Actions authored by this card declaration. */
public fun cardActions(card: Class): List<Action> = card.declaration.authoredActions

/** Non-action effects authored by this card declaration. */
public fun cardEffects(card: Class): List<Effect> =
    card.declaration.authoredEffects.filterNot { effect ->
      effect.trigger == WhenGain && (!effect.automatic || containsTagGain(effect, card))
    }

/** This card's printed play requirement, if any. */
public fun cardRequirement(card: Class): Requirement? =
    card.properties[REQUIREMENT_PROPERTY]?.let { value -> (value as? RequirementValue)?.value }

/** This card's non-negative cost in MC. */
public fun cardCost(card: Class): Int =
    (card.properties.getValue(COST_PROPERTY) as NumberValue).value

/** The resource type represented by this resource card, if any. */
public fun cardResourceType(card: Class): ClassName? {
  if (!card.isSubtypeOf(card.classTable.getClass(RESOURCE_CARD))) return null
  val cardResource = card.classTable.getClass(CARD_RESOURCE)
  return representedClasses(card).singleOrNull { it.isSubtypeOf(cardResource) }?.className
}

private fun representedClasses(card: Class): List<Class> =
    card.dependencies.typeDependencies().mapNotNull { dependency ->
      dependency.boundType.takeIf { it.rootClass.className == CLASS }?.representedClass
    }

private fun containsTagGain(effect: Effect, card: Class): Boolean {
  val tag = card.classTable.getClass(TAG)
  return effect.instruction.descendantsOfType<Gain>().any { gain ->
    card.classTable.getClass(gain.gaining.className).isSubtypeOf(tag)
  }
}

private val CARD_BACK = cn("CardBack")
private val COST_PROPERTY = PropertyName("cost")
private val REQUIREMENT_PROPERTY = PropertyName("requirement")
