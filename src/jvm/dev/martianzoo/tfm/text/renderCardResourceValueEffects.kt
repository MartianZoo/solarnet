package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup

/** Renders card-owned resource-value changes as the persistent effect they implement. */
internal fun renderCardResourceValueEffects(
    effects: List<Effect>,
    describers: Describers,
): Pair<Set<Effect>, String?> {
  val resourceValueEffects = effects.filter { effect ->
    val instructions = InstructionGroup.of(effect.instruction).instructions
    instructions.isNotEmpty() && instructions.all(::isResourceValueChange)
  }
  if (resourceValueEffects.isEmpty()) return emptySet<Effect>() to null

  val grants =
      resourceValueEffects
          .filter { it.automatic && it.trigger == WhenGain }
          .flatMap { effect ->
            InstructionGroup.of(effect.instruction).instructions.mapNotNull { instruction ->
              (instruction as? Gain)?.resourceValue(effect, describers)
            }
          }
  if (
      grants.mapTo(linkedSetOf(), ResourceValueGrant::effect) == resourceValueEffects.toSet() &&
          grants.all { it.source == describers.thisExpression }
  ) {
    val valuesByResource = linkedMapOf<Expression, Int>()
    grants.forEach { grant ->
      valuesByResource.merge(grant.resource, grant.value, Int::plus)
    }
    val value =
        valuesByResource.values.distinct().singleOrNull() ?: return emptySet<Effect>() to null
    val singleResource = valuesByResource.keys.singleOrNull()
    if (singleResource != null) {
      val acceptance =
          effects
              .filterNot { it in resourceValueEffects }
              .singleOrNull { effect ->
                paymentResourceGain(
                        effect.instruction,
                        ComponentDescriber.PaymentRole.ACCEPTANCE,
                        describers,
                    )
                    ?.resource == singleResource.className
              }
      val integrated = acceptance?.let {
        renderAcceptedResourceValue(it, singleResource.className, value, describers)
      }
      if (acceptance != null && integrated != null) {
        return (grants.mapTo(linkedSetOf(), ResourceValueGrant::effect) + acceptance) to integrated
      }
    }
    val nouns = valuesByResource.keys.map { describers.componentNoun(it.className, 1) }
    val resources = if (nouns.size == 1) nouns.single() else englishAlternatives(nouns)
    return grants.mapTo(linkedSetOf(), ResourceValueGrant::effect) to
        completeSentence("each $resources you pay is worth $value M€ extra")
  }

  val penalty =
      resourceValueEffects.singleOrNull { effect ->
        effect.automatic &&
            effect.trigger == WhenGain &&
            effect.singleBaseRemoval(describers) != null
      } ?: return emptySet<Effect>() to null
  val restoration =
      resourceValueEffects.singleOrNull { effect ->
        effect.automatic &&
            effect.trigger == WhenRemove &&
            effect.singleBaseGain(describers) != null
      } ?: return emptySet<Effect>() to null
  val removed = penalty.singleBaseRemoval(describers) ?: return emptySet<Effect>() to null
  if (restoration.singleBaseGain(describers) != removed) return emptySet<Effect>() to null
  val resource = describers.componentNoun(removed.className, 1)
  return setOf(penalty, restoration) to completeSentence("your $resource is worth 1 M€ less")
}

private fun isResourceValueChange(instruction: Instruction): Boolean =
    when (instruction) {
      is Gain -> instruction.gaining.className in RESOURCE_VALUE_CLASSES
      is Remove -> instruction.removing.className == BASE_RESOURCE_VALUE
      else -> false
    }

private fun Gain.resourceValue(effect: Effect, describers: Describers): ResourceValueGrant? {
  val expression = gaining
  if (
      expression.className != GRANTED_RESOURCE_VALUE ||
          intensity.modality() != Modality.REQUIRED ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  val value = count.fixedQuantity()?.takeIf { it > 0 } ?: return null
  val resource =
      expression.arguments.firstOrNull()?.let(describers::representedClassArgument) ?: return null
  val source = expression.arguments.lastOrNull() ?: return null
  return ResourceValueGrant(effect, resource, source, value)
}

private data class ResourceValueGrant(
    val effect: Effect,
    val resource: Expression,
    val source: Expression,
    val value: Int,
)

private fun Effect.singleBaseGain(describers: Describers): Expression? =
    (InstructionGroup.of(instruction).instructions.singleOrNull() as? Gain)?.baseResource(
        describers
    )

private fun Effect.singleBaseRemoval(describers: Describers): Expression? =
    (InstructionGroup.of(instruction).instructions.singleOrNull() as? Remove)?.baseResource(
        describers
    )

private fun Instruction.Change.baseResource(describers: Describers): Expression? {
  val expression = gaining ?: removing ?: return null
  if (
      expression.className != BASE_RESOURCE_VALUE ||
          intensity.modality() != Modality.REQUIRED ||
          count.fixedQuantity() != 1 ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  return expression.arguments.firstOrNull()?.let(describers::representedClassArgument)
}

private val BASE_RESOURCE_VALUE = cn("BaseResourceValue")
private val GRANTED_RESOURCE_VALUE = cn("GrantedResourceValue")
private val RESOURCE_VALUE_CLASSES = setOf(BASE_RESOURCE_VALUE, GRANTED_RESOURCE_VALUE)
