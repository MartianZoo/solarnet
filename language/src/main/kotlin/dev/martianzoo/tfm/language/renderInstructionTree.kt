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
    drawFilter: EnglishDrawFilter? = null,
): Rendering<String> {
  val rendered = renderInstructions(instructionTree, describers, drawFilter)
  return Rendering(rendered.asSentences(), rendered.unresolved)
}

internal fun renderInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
    drawFilter: EnglishDrawFilter? = null,
): RenderedInstructions =
    renderLoweredInstructions(lowerProductionSyntax(instructionTree), describers, drawFilter)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
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
            ?: Clause.RawPets(
                Unresolved(instruction, RefusalReason.LEGACY_INSTRUCTION_RENDERER_DECLINED)
            ))
  }
  return RenderedInstructions(coalesceAdjacentChanges(rendered, describers))
}

private fun renderInstruction(
    instruction: Instruction,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
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
              ?: renderDiscardCostSequence(instruction, describers, drawFilter)
              ?: renderCardResourceCostSequence(instruction, describers, drawFilter)
              ?: renderSequentialThen(instruction, describers, drawFilter)
      is NoOp -> Clause.Simple(Predicate("do", Coordination.one(NounPhrase.text("nothing"))))
      is Instruction.By,
      is Instruction.Transform -> null
    }

private fun renderDiscardCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  val discarded = renderChange(removal, describers, drawFilter) as? Clause.Simple ?: return null
  if (describers.fact(removal.removing.className, ComponentDescriber::discardable) != true) {
    return null
  }
  val result =
      renderLoweredInstructions(instruction.continuation, describers, drawFilter)
          .clauses
          .singleOrNull() ?: return null
  return discarded.withModifier(Modifier.Phrase("to ${result.linearize()}"))
}

private fun renderSequentialThen(
    instruction: Instruction.Then,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
): Clause? {
  val clauses =
      (instruction.stages + instruction.continuation).map { part ->
        renderLoweredInstructions(part, describers, drawFilter).clauses.singleOrNull()
            ?: return null
      }
  return Clause.Coordinated(Coordination(clauses, Conjunction.THEN))
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
    drawFilter: EnglishDrawFilter?,
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
    drawFilter: EnglishDrawFilter?,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers, drawFilter).clauses.singleOrNull()
          ?: return null
  val selectedClass =
      (instruction.gate as? Requirement.Min)
          ?.takeIf { it.minimum == 1 }
          ?.countedMetric
          ?.let { it as? Metric.Count }
          ?.expression
          ?.let(describers::representedClassArgument)
  if (
      selectedClass != null &&
          describers.fact(selectedClass.className, ComponentDescriber::directChange) is
              ComponentDescriber.DirectChange.Imperative
  ) {
    return clause
  }
  val condition = describers.renderGateCondition(instruction.gate) ?: return null
  return Clause.Prefaced(condition, clause)
}

private fun renderPer(
    instruction: Instruction.Per,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
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
    drawFilter: EnglishDrawFilter?,
): Clause? {
  renderPlacementSiteFallback(instruction, describers)?.let {
    return it
  }
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, describers, drawFilter).clauses.singleOrNull()
            ?: return null
      }
  if (alternatives.size == 2) {
    val firstAction = alternatives.singleOrNull {
      it is Clause.Prefaced && it.preface == "as your first action"
    }
    val decline = alternatives.singleOrNull { it !== firstAction }
    if (firstAction != null && decline.isDoNothing()) return firstAction
  }
  val simpleAlternatives = alternatives.map { it as? Clause.Simple }
  if (simpleAlternatives.all { it != null }) {
    coordinateClauseObjects(simpleAlternatives.filterNotNull(), Conjunction.OR)?.let {
      return it
    }
  }
  val conjunction =
      if (
          InstructionGroup.of(instruction.instructions.last()).instructions.isEmpty() ||
              alternatives.any { it is Clause.Prefaced }
      ) {
        Conjunction.COMMA_OR
      } else {
        Conjunction.OR
      }
  return Clause.Coordinated(Coordination(alternatives, conjunction))
}

private fun Clause?.isDoNothing(): Boolean =
    this is Clause.Simple &&
        predicate.verb == "do" &&
        predicate.objects?.members?.singleOrNull()?.linearize() == "nothing"

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

internal fun Describers.renderGateCondition(requirement: Requirement): String? {
  val counting = requirement as? Requirement.Counting ?: return null
  val metric = counting.metric as? Metric.Count ?: return null
  val expression = metric.expression
  if (expression.arguments.isNotEmpty() || expression.refinement != null || expression.complement) {
    return null
  }
  if (
      requirement is Requirement.Exact &&
          requirement.expected == 1 &&
          fact(expression.className, ComponentDescriber::gameParticipant) == true
  ) {
    return "if this is a solo game"
  }
  if (requirement !is Requirement.Min) return null
  fact(expression.className, ComponentDescriber::presenceCondition)?.let { condition ->
    if (requirement.minimum != 1) return null
    val scope =
        if (fact(expression.className, ComponentDescriber::generationScoped) == true) {
          " this generation"
        } else {
          ""
        }
    return "if $condition$scope"
  }
  fact(expression.className, ComponentDescriber::requirement)?.minimum?.let { bound ->
    if (
        bound is ComponentDescriber.Requirement.Bound.Count &&
            bound.syntax == ComponentDescriber.Requirement.CountSyntax.REQUIRES_COUNT
    ) {
      val noun = if (requirement.minimum == 1) bound.noun.singular else bound.noun.plural
      return "if there are ${requirement.minimum} $noun"
    }
  }
  val (name) = tagName(expression.className) ?: return null
  val tags = if (requirement.minimum == 1) "tag" else "tags"
  return "if you have ${requirement.minimum} $name $tags"
}
