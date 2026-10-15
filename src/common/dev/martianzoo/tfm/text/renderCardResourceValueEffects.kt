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
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement

/** Renders card-owned resource-value changes as the persistent effect they implement. */
internal fun renderCardResourceValueEffects(
    invariants: Set<Requirement>,
    effects: List<Effect>,
    describers: Describers,
): Pair<Set<PetNode>, EnglishText?> {
  val resourceValueEffects = effects.filter { effect ->
    val instructions = InstructionGroup.of(effect.instruction).instructions
    instructions.isNotEmpty() && instructions.all(::isResourceValueChange)
  }
  val grants =
      Requirement.split(invariants)
          .filterIsInstance<Requirement.Exact>()
          .mapNotNull { invariant ->
            val expression =
                (invariant.countedMetric as? Metric.Count)?.expression ?: return@mapNotNull null
            if (
                expression.className != GRANTED_RESOURCE_VALUE ||
                    expression.arguments.lastOrNull() != describers.thisExpression ||
                    expression.refinement != null ||
                    invariant.expected <= 0
            )
                return@mapNotNull null
            val resource =
                expression.arguments.firstOrNull()?.let(describers::representedClassArgument)
                    ?: return@mapNotNull null
            invariant to (resource to invariant.expected)
          }
          .toMap()
  if (grants.isNotEmpty()) {
    val valuesByResource = linkedMapOf<Expression, Int>()
    grants.values.forEach { grant ->
      valuesByResource[grant.first] = valuesByResource.getOrElse(grant.first) { 0 } + grant.second
    }
    val value =
        valuesByResource.values.distinct().singleOrNull() ?: return emptySet<Effect>() to null
    val singleResource = valuesByResource.keys.singleOrNull()
    if (singleResource != null) {
      val acceptance =
          effects
              .filterNot { it in resourceValueEffects }
              .mapNotNull { effect ->
                paymentResourceGain(
                        effect.instruction,
                        ComponentDescriber.PaymentRole.ACCEPTANCE,
                        describers,
                    )
                    ?.takeIf { it.resource == singleResource.className }
                    ?.let { effect to it }
              }
              .singleOrNull()
      val integrated = acceptance?.let { (effect, accepted) ->
        renderAcceptedResourceValue(
            effect.trigger,
            accepted,
            NounPhrase("M€", count = value),
            describers,
        )
      }
      if (acceptance != null && integrated != null) {
        return (grants.keys + acceptance.first) to integrated
      }
    }
    val nouns = valuesByResource.keys.map { describers.componentNoun(it.className, 1) }
    val resources = if (nouns.size == 1) nouns.single() else englishAlternatives(nouns)
    return grants.keys to
        Sentence(NounPhrase.text("each $resources you pay is worth $value M€ extra")).asText()
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
  return setOf(penalty, restoration) to
      Sentence(NounPhrase.text("your $resource is worth 1 M€ less")).asText()
}

private fun isResourceValueChange(instruction: Instruction): Boolean =
    when (instruction) {
      is Gain -> instruction.gaining.className in RESOURCE_VALUE_CLASSES
      is Remove -> instruction.removing.className == BASE_RESOURCE_VALUE
      else -> false
    }

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
          quantifier.modality() != Modality.REQUIRED ||
          count.fixedQuantity() != 1 ||
          expression.refinement != null
  ) {
    return null
  }
  return expression.arguments.firstOrNull()?.let(describers::representedClassArgument)
}

private val BASE_RESOURCE_VALUE = cn("BaseResourceValue")
private val GRANTED_RESOURCE_VALUE = cn("GrantedResourceValue")
private val RESOURCE_VALUE_CLASSES = setOf(BASE_RESOURCE_VALUE, GRANTED_RESOURCE_VALUE)
