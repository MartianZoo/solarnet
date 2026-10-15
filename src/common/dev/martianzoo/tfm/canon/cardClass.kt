package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ActionTree
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.EffectTree
import dev.martianzoo.pets.ast.Instruction.Transform as InstructionTransform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Exact
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

/** Printed tag icons, including Wild and the Event icon authored on event declarations. */
public fun cardTags(card: Class): Multiset<ClassName> {
  val tag = card.classTable.getClass(TAG)
  val names =
      card.invariants.filterIsInstance<Exact>().flatMap { required ->
        val expression = (required.countedMetric as? Count)?.expression
        val part =
            expression
                ?.takeIf { THIS.expression in it.arguments }
                ?.let { card.classTable.getClass(it.className) }
        if (part != null && (part.isSubtypeOf(tag) || part.className == cn("WildTag"))) {
          List(required.expected) { part.className }
        } else {
          emptyList()
        }
      }
  return HashMultiset.of(names)
}

/**
 * The card's direct effect, collected from its authored, nonautomatic `This:` effects. Returns null
 * when none are declared. Automatic and conditional self-gain effects are excluded.
 */
public fun cardDirectEffect(card: Class): InstructionGroup? {
  val instructions =
      card.declaration.authoredEffects
          .filter { !it.untransformed.automatic && it.untransformed.trigger == WhenGain }
          .map { it.instructionWithTransforms() }
  return instructions
      .takeIf { it.isNotEmpty() }
      ?.let { InstructionGroup.of(InstructionGroup.createTree(it)) }
}

/**
 * Authored instruction-position `PROD` blocks in this card's [cardDirectEffect] instructions.
 * `Plant / PROD[Energy]` contains a production metric and contributes no production box. Returns
 * every instruction block with its enclosing transform marks; callers must check cardinality and
 * enclosing choices or bindings before extracting a block for copying. No elaboration is applied.
 */
public fun cardProductionBoxes(card: Class): List<InstructionTree> {
  fun collect(node: dev.martianzoo.pets.ast.PetNode): List<InstructionTree> =
      when (node) {
        is InstructionTransform ->
            if (node.transformKind == TfmClasses.PROD) listOf(node)
            else collect(node.instruction).map { InstructionTransform(it, node.transformKind) }
        else -> node.childNodes().flatMap(::collect)
      }
  return cardDirectEffect(card)?.let(::collect).orEmpty()
}

/**
 * Returns this card's authored actions in declaration order, retaining enclosing transform marks.
 */
public fun cardActions(card: Class): List<ActionTree> = card.declaration.authoredActions

/**
 * Returns authored effects other than the nonautomatic `This:` gain effects exposed by
 * [cardDirectEffect], retaining enclosing transform marks. Automatic effects remain in the result;
 * lowered action effects do not.
 */
public fun cardEffects(card: Class): List<EffectTree> =
    card.declaration.authoredEffects.filterNot { effect ->
      effect.untransformed.trigger == WhenGain && !effect.untransformed.automatic
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

private val CARD_BACK = cn("CardBack")
private val COST_PROPERTY = PropertyName("cost")
private val REQUIREMENT_PROPERTY = PropertyName("requirement")
