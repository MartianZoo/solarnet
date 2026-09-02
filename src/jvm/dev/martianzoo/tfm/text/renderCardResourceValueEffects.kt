package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionGroup

/** Renders card-granted resource-value components as the persistent effect they implement. */
internal fun renderCardResourceValueEffects(
    effects: List<Effect>,
    describers: Describers,
): Pair<Set<Effect>, String?> {
  val resourceValueEffects = effects.filter { effect ->
    val instructions = InstructionGroup.of(effect.instruction).instructions
    instructions.isNotEmpty() && instructions.all(::isGrantedResourceValueGain)
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
      grants.mapTo(linkedSetOf(), ResourceValueGrant::effect) != resourceValueEffects.toSet() ||
          grants.any { it.source != describers.thisExpression }
  ) {
    return emptySet<Effect>() to null
  }

  val valuesByResource = linkedMapOf<Expression, Int>()
  grants.forEach { grant ->
    valuesByResource.merge(grant.resource, grant.value, Int::plus)
  }
  val value = valuesByResource.values.distinct().singleOrNull() ?: return emptySet<Effect>() to null
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

private fun isGrantedResourceValueGain(instruction: Instruction): Boolean =
    instruction is Gain && instruction.gaining.className == GRANTED_RESOURCE_VALUE

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

private val GRANTED_RESOURCE_VALUE = cn("GrantedResourceValue")
