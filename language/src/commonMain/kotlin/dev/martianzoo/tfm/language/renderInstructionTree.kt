package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderInstructionTree(
    instructionTree: InstructionTree,
    describers: Describers,
    drawFilter: ClassName? = null,
): String = renderInstructions(instructionTree, describers, drawFilter).asSentences()

internal fun renderInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
    drawFilter: ClassName? = null,
): RenderedInstructions =
    renderLoweredInstructions(lowerProductionSyntax(instructionTree), describers, drawFilter)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
    drawFilter: ClassName?,
): RenderedInstructions {
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) {
    return RenderedInstructions(
        listOf(Clause.Simple(Predicate("do", Coordination.one(NounPhrase.text("nothing")))))
    )
  }
  val rendered = instructions.map { instruction ->
    instruction to
        (renderInstruction(instruction, describers, drawFilter)
            ?: Clause.RawPets(instruction.toString()))
  }
  return RenderedInstructions(coalesceAdjacentChanges(rendered, describers))
}

private fun renderInstruction(
    instruction: Instruction,
    describers: Describers,
    drawFilter: ClassName?,
): Clause? =
    when (instruction) {
      is Gain,
      is Remove,
      is Instruction.Transmute -> renderChange(instruction, describers, drawFilter)
      is Instruction.Or -> renderAlternatives(instruction, describers, drawFilter)
      is Instruction.Per -> renderPer(instruction, describers, drawFilter)
      is Instruction.Gated -> renderGated(instruction, describers, drawFilter)
      is Instruction.Then ->
          renderPlacementBonusProductionSequence(instruction, describers)
              ?: renderCardPlaySequence(instruction, describers)
              ?: renderCardResourceCostSequence(instruction, describers, drawFilter)
      is NoOp,
      is Instruction.By,
      is Instruction.Transform -> null
    }

private fun renderPlacementBonusProductionSequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause? {
  val placement = instruction.stages.singleOrNull() as? Gain ?: return null
  if (
      (placement.intensity != null && placement.intensity != MANDATORY) ||
          placement.gaining.refinement != null ||
          placement.gaining.complement ||
          (placement.count as? ActualScalar)?.value != 1
  ) {
    return null
  }
  val placementDescription =
      describers.fact(placement.gaining.className, ComponentDescriber::placement) ?: return null
  val alternatives = (instruction.continuation as? Instruction.Or)?.instructions ?: return null
  val bonuses = alternatives.map { alternative ->
    val gated = alternative as? Instruction.Gated ?: return null
    val minimum = gated.gate as? Requirement.Min ?: return null
    if (minimum.target != 1) return null
    val site = (minimum.metric as? Metric.Count)?.expression ?: return null
    if (site.arguments.isNotEmpty() || site.complement) return null
    val requirements =
        (site.refinement?.takeIf { !it.forgiving }?.requirement as? Requirement.And)?.requirements
            ?: return null
    val expressions = requirements.map { child ->
      val childMinimum = child as? Requirement.Min ?: return null
      if (childMinimum.target != 1) return null
      (childMinimum.metric as? Metric.Count)?.expression ?: return null
    }
    if (expressions.none { it.simple && it.className == placement.gaining.className }) return null
    val bonus =
        expressions.singleOrNull {
          describers.fact(it.className, ComponentDescriber::placementBonus) != null
        } ?: return null
    val bonusDescription =
        describers.fact(bonus.className, ComponentDescriber::placementBonus) ?: return null
    val resource = describers.representedClass(bonus) ?: return null
    val production =
        InstructionGroup.of(gated.inner).instructions.singleOrNull() as? Gain ?: return null
    if (
        (production.intensity != null && production.intensity != MANDATORY) ||
            (production.count as? ActualScalar)?.value != 1
    ) {
      return null
    }
    val (owners, producedResource) =
        describers.productionExpression(production.gaining) ?: return null
    if (owners.isNotEmpty() || producedResource != resource.className) return null
    PlacementBonusProduction(site.className, resource.className, bonusDescription.noun)
  }
  val siteClassName =
      bonuses.map(PlacementBonusProduction::siteClassName).distinct().singleOrNull() ?: return null
  val bonusNoun =
      bonuses.map(PlacementBonusProduction::bonusNoun).distinct().singleOrNull() ?: return null
  val siteModifiers =
      renderPlacementSites(placement.gaining.arguments, describers)?.takeIf { it.isNotEmpty() }
          ?: run {
            val description = describers.placementSite(siteClassName) ?: return null
            val noun = describers.describedNoun(siteClassName, description.noun, 1)
            val article = description.article ?: describers.indefiniteArticle(noun)
            listOf(Modifier.Phrase("on $article $noun"))
          }
  val resourceNames = bonuses.map { describers.componentNoun(it.resource, 1) }
  val resourceAlternatives = englishAlternatives(resourceNames)
  val bonusModifier =
      Modifier.Phrase(
          "with ${describers.indefiniteArticle(resourceAlternatives)} $resourceAlternatives " +
              bonusNoun.singular
      )
  val placed =
      Clause.Simple(
          Predicate(
              "place",
              Coordination.one(
                  NounPhrase(
                      placementDescription.singular,
                      placementDescription.plural,
                      determiner = placementDescription.article,
                  )
              ),
              siteModifiers.take(1) + bonusModifier + siteModifiers.drop(1),
          )
      )
  val production =
      Clause.Simple(
          Predicate(
              "increase",
              Coordination.one(NounPhrase.text("the matching production 1 step")),
          )
      )
  return Clause.Coordinated(Coordination(listOf(placed, production), Conjunction.AND))
}

private data class PlacementBonusProduction(
    val siteClassName: ClassName,
    val resource: ClassName,
    val bonusNoun: ComponentDescriber.Noun.Counted,
)

private fun renderCardResourceCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
    drawFilter: ClassName?,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  if (
      (removal.intensity != null && removal.intensity != MANDATORY) ||
          removal.removing.arguments != listOf(describers.thisExpression) ||
          removal.removing.refinement != null ||
          removal.removing.complement
  ) {
    return null
  }
  val count = (removal.count as? ActualScalar)?.value ?: return null
  val resource = describers.cardResourceNounPhrase(removal.removing.className, count) ?: return null
  val result =
      renderLoweredInstructions(instruction.continuation, describers, drawFilter)
          .clauses
          .singleOrNull() ?: return null
  return Clause.Simple(
      Predicate(
          "remove",
          Coordination.one(resource),
          listOf(
              Modifier.Phrase("from this card"),
              Modifier.Phrase("to ${result.linearize()}"),
          ),
      )
  )
}

private fun renderCardPlaySequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause.Simple? {
  val play = instruction.stages.singleOrNull() as? Gain ?: return null
  if (
      (play.intensity != null && play.intensity != MANDATORY) ||
          !play.gaining.simple ||
          (play.count as? ActualScalar)?.value != 1 ||
          describers.fact(play.gaining.className, ComponentDescriber::playTrigger) !=
              ComponentDescriber.PlayTrigger.CARD
  ) {
    return null
  }
  val modifier =
      when (val continuation = instruction.continuation) {
        is Instruction.Per -> {
          val removal = continuation.inner as? Remove ?: return null
          val counted = continuation.metric as? Metric.Count ?: return null
          if (
              (removal.intensity != null && removal.intensity != MANDATORY) ||
                  !removal.removing.simple ||
                  removal.removing != counted.expression ||
                  (removal.count as? ActualScalar)?.value != 1 ||
                  describers.fact(
                      removal.removing.className,
                      ComponentDescriber::requirementShortfall,
                  ) != true
          ) {
            return null
          }
          "ignoring global requirements"
        }
        else -> {
          val reduction = maximumOwedReduction(continuation, describers) ?: return null
          "reducing its cost by ${reduction.count} ${reduction.noun}"
        }
      }
  return Clause.Simple(
      Predicate(
          "play",
          Coordination.one(NounPhrase.text("a card from hand")),
          listOf(Modifier.Supplement(modifier)),
      )
  )
}

private fun renderGated(
    instruction: Instruction.Gated,
    describers: Describers,
    drawFilter: ClassName?,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers, drawFilter).clauses.singleOrNull()
          ?: return null
  val condition = describers.renderGateCondition(instruction.gate) ?: return null
  return (clause as? Clause.Simple)?.withModifier(Modifier.Phrase(condition))
}

private fun renderPer(
    instruction: Instruction.Per,
    describers: Describers,
    drawFilter: ClassName?,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers, drawFilter).clauses.singleOrNull()
          ?: return null
  val metric = renderMetricPhrase(instruction.metric, describers) ?: return null
  return (clause as? Clause.Simple)?.withModifier(Modifier.Phrase("for $metric"))
}

private fun renderAlternatives(
    instruction: Instruction.Or,
    describers: Describers,
    drawFilter: ClassName?,
): Clause? {
  renderPlacementSiteFallback(instruction, describers)?.let {
    return it
  }
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, describers, drawFilter).clauses.singleOrNull()
            ?: return null
      }
  val simpleAlternatives = alternatives.map { it as? Clause.Simple }
  if (simpleAlternatives.all { it != null }) {
    coordinateClauseObjects(simpleAlternatives.filterNotNull(), Conjunction.OR)?.let {
      return it
    }
  }
  return Clause.Coordinated(Coordination(alternatives, Conjunction.OR))
}

private fun renderPlacementSiteFallback(
    instruction: Instruction.Or,
    describers: Describers,
): Clause.Simple? {
  if (instruction.instructions.size != 2) return null
  val preferred =
      InstructionGroup.of(instruction.instructions.first()).instructions.singleOrNull() as? Gain
          ?: return null
  val fallback =
      InstructionGroup.of(instruction.instructions.last()).instructions.singleOrNull()
          as? Instruction.Gated ?: return null
  val unrestricted =
      InstructionGroup.of(fallback.inner).instructions.singleOrNull() as? Gain ?: return null
  if (
      preferred.gaining.refinement != null ||
          preferred.gaining.complement ||
          preferred.gaining.className != unrestricted.gaining.className ||
          unrestricted.gaining.arguments.isNotEmpty() ||
          unrestricted.gaining.refinement != null ||
          unrestricted.gaining.complement ||
          preferred.intensity != unrestricted.intensity ||
          preferred.count != unrestricted.count ||
          (preferred.count as? ActualScalar)?.value != 1
  ) {
    return null
  }
  val site = preferred.gaining.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
  if (describers.placementSite(site.className) == null) return null
  val placement =
      describers.fact(preferred.gaining.className, ComponentDescriber::placement) ?: return null
  if (placement.consequence != null) return null
  val absence = fallback.gate as? Requirement.Max ?: return null
  val countedSite = absence.countedMetric as? Metric.Count ?: return null
  if (absence.maximum != 0 || countedSite.expression != site) return null

  val preferredClause = renderChange(preferred, describers) as? Clause.Simple ?: return null
  if (renderChange(unrestricted, describers) !is Clause.Simple) return null
  return preferredClause
      .withModifier(Modifier.Phrase("if using a board that has one"))
      .withModifier(Modifier.Supplement("otherwise place it normally"))
}

private fun coalesceAdjacentChanges(
    rendered: List<Pair<Instruction, Clause>>,
    describers: Describers,
): List<Clause> {
  val result = mutableListOf<Clause>()
  var index = 0
  while (index < rendered.size) {
    val (instruction, renderedClause) = rendered[index]
    if (isProductionChange(instruction, describers)) {
      val run =
          rendered.drop(index).takeWhile { (candidate) ->
            isProductionChange(candidate, describers)
          }
      val clauses = factorAdjacentPredicates(run.map { it.second })
      result +=
          if (clauses.size == 1) clauses.single()
          else Clause.Coordinated(Coordination(clauses, Conjunction.AND))
      index += run.size
      continue
    }
    if (isCoalescibleStandardResourceGain(instruction, describers)) {
      val run =
          rendered.drop(index).takeWhile { (candidate) ->
            isCoalescibleStandardResourceGain(candidate, describers)
          }
      val clauses = factorAdjacentPredicates(run.map { it.second })
      result += clauses
      index += run.size
      continue
    }
    result += renderedClause
    index++
  }
  return result
}

private fun factorAdjacentPredicates(clauses: List<Clause>): List<Clause> {
  val result = mutableListOf<Clause>()
  clauses.forEach { clause ->
    val previous = result.lastOrNull() as? Clause.Simple
    val current = clause as? Clause.Simple
    val factored =
        if (previous != null && current != null) {
          coordinateClauseObjects(listOf(previous, current), Conjunction.AND)
        } else {
          null
        }
    if (factored != null) {
      result[result.lastIndex] = factored
    } else {
      result += clause
    }
  }
  return result
}

private fun Describers.renderGateCondition(requirement: Requirement): String? {
  val minimum = requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  val (name) = tagName(metric.expression.className) ?: return null
  val tags = if (minimum.target == 1) "tag" else "tags"
  return "if you have ${minimum.target} $name $tags"
}
