package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.types.Class

/** Looks up the English description supplied for each component Class. */
internal class Describers(private val descriptions: Map<Class, ComponentDescriber>) {
  private val classesByName = descriptions.keys.associateBy { it.className }

  private operator fun get(className: ClassName): ComponentDescriber {
    val componentClass = classesByName[className] ?: return ComponentDescriber()
    return descriptions.getValue(componentClass)
  }

  internal fun hasBehaviorBearingExtraClass(card: CardDefinition): Boolean =
      card.extraClasses.any { it.className != card.resourceType }

  internal fun renderCost(cost: Cost): String? {
    val spend = cost as? Cost.Spend ?: return null
    val expression = spend.scaledEx.expression
    val count = (spend.scaledEx.scalar as? ActualScalar)?.value ?: return null
    if (expression.refinement == null && !expression.complement) {
      cardResourceNoun(expression.className, count)?.let { noun ->
        val holder =
            when (expression.arguments) {
              listOf(thisExpression) -> "this card"
              listOf(anyoneExpression) -> "ANY PLAYER'S CARD"
              emptyList<Expression>() -> "any of your cards"
              else -> return null
            }
        return "remove $count $noun from $holder"
      }
    }
    standardResourceProduction(expression)?.let { (ownerArguments, resourceClassName) ->
      if (ownerArguments.isNotEmpty()) return null
      val steps = if (count == 1) "step" else "steps"
      return "decrease your ${componentNoun(resourceClassName, 1)} production $count $steps"
    }
    if (!expression.simple) return null
    val noun = standardResourceNoun(expression.className, count) ?: return null
    return "spend $count $noun"
  }

  internal fun renderInstructionRun(instructions: List<Instruction>): InstructionRun? {
    val first = instructions.firstOrNull() ?: return null
    if (standardResourceGain(first) != null) {
      val gains = instructions.takeWhile { standardResourceGain(it) != null }
      val objects = gains.map { instruction ->
        val (className, count) = checkNotNull(standardResourceGain(instruction))
        "$count ${componentNoun(className, count)}"
      }
      return InstructionRun("gain ${englishList(objects)}", gains.size)
    }
    if (productionChange(first) != null) {
      val changes =
          instructions
              .takeWhile { productionChange(it) != null }
              .map { checkNotNull(productionChange(it)) }
      return InstructionRun(renderProductionChanges(changes), changes.size)
    }
    return null
  }

  internal fun renderGainAlternatives(
      instructions: List<Instruction>,
      card: CardDefinition?,
  ): String? {
    if (instructions.size < 2) return null
    val gains = instructions.map { it as? Gain ?: return null }
    val standardGains = gains.map { standardResourceGain(it) }
    if (standardGains.all { it != null }) {
      val objects =
          standardGains.filterNotNull().map { (className, count) ->
            "$count ${componentNoun(className, count)}"
          }
      return "gain ${englishAlternatives(objects)}"
    }
    val cardGains = gains.map { cardResourceGain(it, card) ?: return null }
    val target = cardGains.map { it.target }.distinct().singleOrNull() ?: return null
    val objects = englishAlternatives(cardGains.map { "${it.count} ${it.noun}" })
    return "add $objects to $target"
  }

  internal fun renderEventTrigger(trigger: Trigger): String? {
    val events =
        when (trigger) {
          is Trigger.Or -> trigger.triggers.map { renderEvent(it) ?: return null }
          else -> listOf(renderEvent(trigger) ?: return null)
        }
    val kind = events.map { it.kind }.distinct().singleOrNull() ?: return null
    val objectPhrases = events.map { it.objectPhrase }
    val objects =
        if (objectPhrases.size == 1) objectPhrases.single() else englishAlternatives(objectPhrases)
    return when (kind) {
      EventKind.PLAY -> "when you play $objects"
      EventKind.PLACE -> "when you place $objects"
      EventKind.PLACE_ANY -> "when $objects is placed"
      EventKind.PLACE_BY_ANYONE -> "when any player places $objects"
      EventKind.ADD_TO_CARD -> "when you add $objects to any card"
    }
  }

  internal fun renderOwedReduction(instruction: InstructionTree): String? {
    val removal = instruction as? Remove ?: return null
    if (removal.intensity != null && removal.intensity != MANDATORY) return null
    val expression = removal.removing
    if (expression.refinement != null || expression.complement) return null
    if (this[expression.className].owedPayment != true) return null
    val resource = representedClass(expression) ?: return null
    val count = (removal.count as? ActualScalar)?.value ?: return null
    val noun = standardResourceNoun(resource.className, count) ?: return null
    return "you pay $count $noun less for it"
  }

  private fun renderEvent(trigger: Trigger): Event? {
    if (trigger is ByTrigger) {
      if (trigger.by != anyoneExpression) return null
      val expression = (trigger.inner as? OnGainOf)?.expression ?: return null
      val placement = placementEvent(expression, EventKind.PLACE_BY_ANYONE) ?: return null
      return placement
    }
    val expression = (trigger as? OnGainOf)?.expression ?: return null
    if (expression.refinement != null || expression.complement) return null
    when (this[expression.className].playTrigger) {
      ComponentDescriber.PlayTrigger.CARD -> {
        if (!expression.simple) return null
        return Event(EventKind.PLAY, "a card")
      }
      ComponentDescriber.PlayTrigger.TAG -> {
        val tag = representedClass(expression) ?: return null
        val (name) = tagName(tag.className) ?: return null
        return Event(EventKind.PLAY, "${indefiniteArticle(name)} $name tag")
      }
      null -> Unit
    }
    if (expression.simple) {
      tagName(expression.className)?.let { (name) ->
        return Event(EventKind.PLAY, "${indefiniteArticle(name)} $name tag")
      }
      placementEvent(expression, EventKind.PLACE)?.let {
        return it
      }
      cardResourceNoun(expression.className, 1)?.let { noun ->
        return Event(EventKind.ADD_TO_CARD, "${indefiniteArticle(noun)} $noun")
      }
    }
    if (expression.arguments == listOf(anyoneExpression)) {
      placementEvent(expression, EventKind.PLACE_ANY)?.let {
        return it
      }
    }
    return null
  }

  private fun placementEvent(expression: Expression, kind: EventKind): Event? {
    if (expression.refinement != null || expression.complement) return null
    if (kind == EventKind.PLACE_BY_ANYONE && !expression.simple) return null
    val placement = this[expression.className].placement ?: return null
    val phrase =
        when (kind) {
          EventKind.PLACE_ANY -> "any ${placement.singular}"
          EventKind.PLACE,
          EventKind.PLACE_BY_ANYONE -> "${placement.article} ${placement.singular}"
          EventKind.PLAY,
          EventKind.ADD_TO_CARD -> return null
        }
    return Event(kind, phrase)
  }

  private data class Event(val kind: EventKind, val objectPhrase: String)

  private enum class EventKind {
    PLAY,
    PLACE,
    PLACE_ANY,
    PLACE_BY_ANYONE,
    ADD_TO_CARD,
  }

  internal fun renderChange(instruction: Instruction, card: CardDefinition?): String? =
      when (instruction) {
        is Gain ->
            renderDirectGain(instruction)
                ?: renderCardResourceGain(instruction, card)
                ?: renderTrackChange(instruction)
                ?: renderPlacement(instruction)
        is Remove ->
            renderStandardResourceRemoval(instruction)
                ?: renderCardResourceRemoval(instruction)
                ?: renderTrackChange(instruction)
        else -> null
      }

  internal fun renderMetric(expression: Expression, unit: Int? = null): String? {
    val count = unit ?: 1
    val prefix = unit?.let { "every $it" } ?: "each"
    if (expression.simple) {
      tagName(expression.className)?.let { (name) ->
        return "$prefix $name ${if (unit == null) "tag" else "tags"} you have"
      }
      cardResourceNoun(expression.className, count)?.let { noun ->
        return "$prefix $noun"
      }
      placementCountPhrase(expression, count)?.let { phrase ->
        return "$prefix $phrase"
      }
      return null
    }
    placementCountPhrase(expression, count)?.let { phrase ->
      return "$prefix $phrase"
    }
    if (
        expression.arguments != listOf(thisExpression) ||
            expression.refinement != null ||
            expression.complement
    ) {
      return null
    }
    val noun = cardResourceNoun(expression.className, count) ?: return null
    return "$prefix $noun on this card"
  }

  internal fun renderMinimum(requirement: Requirement.Min): String? {
    val metric = requirement.metric as? Metric.Count ?: return null
    val expression = metric.expression
    val target = requirement.target
    val componentRequirement = this[expression.className].requirement
    if (componentRequirement != null) {
      return componentRequirement.renderMinimum(expression, target)
    }
    return renderProductionRequirement(requirement)
        ?: renderCardResourceRequirement(requirement)
        ?: renderTagRequirement(requirement)
  }

  internal fun renderMaximum(requirement: Requirement.Max): String? {
    val metric = requirement.metric as? Metric.Count ?: return null
    val expression = metric.expression
    val target = requirement.target
    return this[expression.className].requirement?.renderMaximum(expression, target)
  }

  internal fun renderRequirementGroup(requirement: Requirement.And): String? =
      renderTagRequirementGroup(requirement) ?: renderOwnedPlacementRequirementGroup(requirement)

  internal fun renderScoringCondition(requirement: Requirement): String? {
    val minimum = requirement as? Requirement.Min ?: return null
    val metric = minimum.metric as? Metric.Count ?: return null
    val expression = metric.expression
    if (
        expression.arguments != listOf(thisExpression) ||
            expression.refinement != null ||
            expression.complement
    ) {
      return null
    }
    val noun = cardResourceNoun(expression.className, minimum.target) ?: return null
    return "if you have at least ${minimum.target} $noun on this card"
  }

  internal fun renderGateCondition(requirement: Requirement): String? {
    val minimum = requirement as? Requirement.Min ?: return null
    val metric = minimum.metric as? Metric.Count ?: return null
    if (!metric.expression.simple) return null
    val (name) = tagName(metric.expression.className) ?: return null
    val tags = if (minimum.target == 1) "tag" else "tags"
    return "if you have ${minimum.target} $name $tags"
  }

  internal fun isEndTrigger(expression: Expression): Boolean =
      expression.simple && this[expression.className].endTrigger == true

  internal fun renderFixedScore(instruction: InstructionTree): String? {
    val (className, count, penalty) =
        when (instruction) {
          is Gain -> {
            if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
            if (!instruction.gaining.simple) return null
            Triple(
                instruction.gaining.className,
                (instruction.count as? ActualScalar)?.value ?: return null,
                false,
            )
          }
          is Remove -> {
            if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
            if (!instruction.removing.simple) return null
            Triple(
                instruction.removing.className,
                (instruction.count as? ActualScalar)?.value ?: return null,
                true,
            )
          }
          else -> return null
        }
    val score = this[className].score ?: return null
    val noun = if (count == 1) score.singular else score.plural
    return "${if (penalty) "-" else ""}$count $noun"
  }

  internal data class InstructionRun(val clause: String, val count: Int)

  private fun renderStandardResourceRemoval(instruction: Instruction): String? {
    standardResourceRemoval(instruction)?.let { (className, count) ->
      return "remove $count ${componentNoun(className, count)}"
    }
    val removal = standardResourceRemovalFromAnyPlayer(instruction) ?: return null
    if (removal.intensity != OPTIONAL) return null
    return "remove up to ${removal.count} ${componentNoun(removal.className, removal.count)} from any player"
  }

  private fun renderDirectGain(instruction: Instruction): String? {
    val (className, count) = concreteMandatoryGain(instruction) ?: return null
    val gain = this[className].directGain ?: return null
    if (count != gain.count) return null
    return "gain $count ${gain.noun}"
  }

  private fun renderCardResourceRemoval(instruction: Instruction): String? {
    val (className, count) = concreteMandatoryRemoval(instruction) ?: return null
    val noun = cardResourceNoun(className, count) ?: return null
    return "remove $count $noun from any card"
  }

  private fun renderCardResourceGain(
      instruction: Instruction,
      card: CardDefinition?,
  ): String? =
      cardResourceGain(instruction, card)?.let { gain ->
        "add ${gain.count} ${gain.noun} to ${gain.target}"
      }

  private fun cardResourceGain(
      instruction: Instruction,
      card: CardDefinition?,
  ): CardResourceGain? {
    val gain = instruction as? Gain ?: return null
    if (gain.intensity != null && gain.intensity != MANDATORY) return null
    val expression = gain.gaining
    if (expression.refinement != null || expression.complement) return null
    val count = (gain.count as? ActualScalar)?.value ?: return null
    val noun = cardResourceNoun(expression.className, count) ?: return null
    val target =
        when {
          expression.arguments == listOf(thisExpression) -> "this card"
          expression.arguments.size == 1 ->
              renderCardResourceHolder(expression.arguments.single()) ?: return null
          expression.arguments.isNotEmpty() -> return null
          card == null -> "an eligible card"
          card.resourceType == expression.className -> "ANY card"
          else -> "ANOTHER card"
        }
    return CardResourceGain(count, noun, target)
  }

  private data class CardResourceGain(val count: Int, val noun: String, val target: String)

  private fun renderCardResourceHolder(expression: Expression): String? {
    if (expression.arguments.isNotEmpty() || expression.complement) return null
    val holder = this[expression.className].cardResourceHolder ?: return null
    val refinement = expression.refinement ?: return null
    if (refinement.forgiving) return null
    val minimum = refinement.requirement as? Requirement.Min ?: return null
    if (minimum.target != 1) return null
    val metric = minimum.metric as? Metric.Count ?: return null
    if (!metric.expression.simple) return null
    val (tag) = tagName(metric.expression.className) ?: return null
    return "${indefiniteArticle(holder)} $holder with ${indefiniteArticle(tag)} $tag tag"
  }

  private fun renderProductionChanges(changes: List<ResourceProductionChange>): String {
    val clauses = mutableListOf<String>()
    var index = 0
    while (index < changes.size) {
      val run = changes.drop(index).takeWhile { it.gaining == changes[index].gaining }
      clauses += renderProductionClause(run)
      index += run.size
    }
    return clauses.joinToString(" and ")
  }

  private fun productionChange(instruction: Instruction): ResourceProductionChange? {
    val change = instruction as? Instruction.Change ?: return null
    if (change.intensity != null && change.intensity != MANDATORY) return null
    val gaining = change is Gain
    if (!gaining && change !is Remove) return null
    val expression = change.gaining ?: change.removing ?: return null
    val (ownerArguments, resourceClassName) = standardResourceProduction(expression) ?: return null
    val owner =
        when {
          ownerArguments.isEmpty() -> "your"
          !gaining && ownerArguments == listOf(anyoneExpression) -> "any player's"
          else -> return null
        }
    val count = (change.count as? ActualScalar)?.value ?: return null
    return ResourceProductionChange(gaining, owner, resourceClassName, count)
  }

  private fun renderProductionClause(changes: List<ResourceProductionChange>): String {
    val verb = if (changes.first().gaining) "increase" else "decrease"
    val productions = changes.map {
      val steps = if (it.count == 1) "step" else "steps"
      "${it.owner} ${componentNoun(it.className, 1)} production ${it.count} $steps"
    }
    return "$verb ${englishList(productions)}"
  }

  private fun renderTrackChange(instruction: Instruction): String? {
    val gain = concreteMandatoryGain(instruction)
    val removal = concreteMandatoryRemoval(instruction)
    val (className, count) = gain ?: removal ?: return null
    val track = this[className].track ?: return null
    val steps = if (count == 1) "step" else "steps"
    val verb = if (gain != null) "raise" else "lower"
    return "$verb ${track.subject} $count $steps"
  }

  private fun renderPlacement(instruction: Instruction): String? {
    val gain = instruction as? Gain ?: return null
    if (gain.intensity != null && gain.intensity != MANDATORY) return null
    val expression = gain.gaining
    if (
        expression.arguments.isNotEmpty() || expression.refinement != null || expression.complement
    ) {
      return null
    }
    val count = (gain.count as? ActualScalar)?.value ?: return null
    val placement = this[expression.className].placement ?: return null
    if (count != 1 && !placement.allowsMultiple) return null
    val nounPhrase =
        if (count == 1) {
          "${placement.article} ${placement.singular}"
        } else {
          "$count ${placement.plural}"
        }
    val consequence = placement.consequence?.let { " ($it)" }.orEmpty()
    return "place $nounPhrase$consequence"
  }

  private fun standardResourceGain(instruction: Instruction): Pair<ClassName, Int>? {
    val gain = concreteMandatoryGain(instruction) ?: return null
    return gain.takeIf { (className) -> standardResourceNoun(className, 1) != null }
  }

  private fun standardResourceRemoval(instruction: Instruction): Pair<ClassName, Int>? {
    val removal = concreteMandatoryRemoval(instruction) ?: return null
    return removal.takeIf { (className) -> standardResourceNoun(className, 1) != null }
  }

  private fun standardResourceRemovalFromAnyPlayer(
      instruction: Instruction
  ): TargetedResourceRemoval? {
    val removal = instruction as? Remove ?: return null
    val expression = removal.removing
    if (expression.complement || expression.refinement != null) return null
    if (expression.arguments != listOf(anyoneExpression)) return null
    if (standardResourceNoun(expression.className, 1) == null) return null
    val count = (removal.count as? ActualScalar)?.value ?: return null
    return TargetedResourceRemoval(expression.className, count, removal.intensity)
  }

  private fun concreteMandatoryGain(instruction: Instruction): Pair<ClassName, Int>? {
    val gain = instruction as? Gain ?: return null
    if (gain.intensity != null && gain.intensity != MANDATORY) return null
    if (!gain.gaining.simple) return null
    val count = (gain.count as? ActualScalar)?.value ?: return null
    return gain.gaining.className to count
  }

  private fun concreteMandatoryRemoval(instruction: Instruction): Pair<ClassName, Int>? {
    val removal = instruction as? Remove ?: return null
    if (removal.intensity != null && removal.intensity != MANDATORY) return null
    if (!removal.removing.simple) return null
    val count = (removal.count as? ActualScalar)?.value ?: return null
    return removal.removing.className to count
  }

  private fun placementCountPhrase(expression: Expression, count: Int): String? {
    if (expression.refinement != null || expression.complement) return null
    val placement = this[expression.className].placement ?: return null
    val (owner, location) =
        when {
          expression.simple -> (placement.unqualifiedMetricOwner ?: return null) to null
          expression.arguments == listOf(anyoneExpression) ->
              (placement.anyoneMetricOwner ?: return null) to null
          expression.arguments.size == 2 && expression.arguments.last() == anyoneExpression -> {
            val location = expression.arguments.first()
            if (!location.simple) return null
            (placement.anyoneMetricOwner ?: return null) to
                (this[location.className].metricLocation ?: return null)
          }
          else -> null
        } ?: return null
    val ownerPhrase =
        when (owner) {
          ComponentDescriber.MetricOwner.YOU -> " you own"
          ComponentDescriber.MetricOwner.ANY_PLAYER -> ""
        }
    return "${if (count == 1) placement.singular else placement.plural}$ownerPhrase${location?.let { " $it" }.orEmpty()}"
  }

  private fun renderProductionRequirement(minimum: Requirement.Min): String? {
    if (minimum.target != 1) return null
    val metric = minimum.metric as? Metric.Count ?: return null
    val (ownerArguments, resourceClassName) =
        standardResourceProduction(metric.expression) ?: return null
    if (ownerArguments.isNotEmpty()) return null
    return "Requires that you have ${componentNoun(resourceClassName, 1)} production."
  }

  private fun renderCardResourceRequirement(requirement: Requirement.Min): String? {
    val metric = requirement.metric as? Metric.Count ?: return null
    if (!metric.expression.simple) return null
    val noun = cardResourceNoun(metric.expression.className, requirement.target) ?: return null
    return "Requires that you have ${requirement.target} $noun."
  }

  private fun renderTagRequirement(requirement: Requirement.Min): String? {
    val (name) = tagName(requirement) ?: return null
    return if (requirement.target == 1) {
      "Requires ${indefiniteArticle(name)} $name tag."
    } else {
      "Requires ${requirement.target} $name tags."
    }
  }

  private fun renderTagRequirementGroup(requirement: Requirement.And): String? {
    val tags =
        requirement.requirements.map { child ->
          val minimum = child as? Requirement.Min ?: return null
          if (minimum.target != 1) return null
          tagName(minimum) ?: return null
        }
    val allPlanetTags = tags.all { (_, planet) -> planet }
    if (!allPlanetTags && tags.any { (_, planet) -> planet }) return null
    val nouns =
        if (allPlanetTags) {
          tags.map { (name) -> name }
        } else {
          tags.map { (name) -> "${indefiniteArticle(name)} $name tag" }
        }
    return "Requires ${englishList(nouns)}${if (allPlanetTags) " tags" else ""}."
  }

  private fun renderOwnedPlacementRequirementGroup(requirement: Requirement.And): String? {
    val nouns =
        requirement.requirements.map { child ->
          val minimum = child as? Requirement.Min ?: return null
          val metric = minimum.metric as? Metric.Count ?: return null
          if (!metric.expression.simple) return null
          this[metric.expression.className].requirement?.renderOwnedCount(minimum.target)
              ?: return null
        }
    return "Requires that you have ${englishList(nouns)}."
  }

  private fun tagName(requirement: Requirement.Min): Pair<String, Boolean>? {
    val metric = requirement.metric as? Metric.Count ?: return null
    if (!metric.expression.simple) return null
    return tagName(metric.expression.className)
  }

  private fun componentNoun(className: ClassName, count: Int): String =
      when (val noun = this[className].noun) {
        is ComponentDescriber.Noun.Counted -> if (count == 1) noun.singular else noun.plural
        is ComponentDescriber.Noun.Fixed -> noun.text
        ComponentDescriber.Noun.ClassName,
        null -> unCamelCase(className.toString())
      }

  private fun standardResourceNoun(className: ClassName, count: Int): String? =
      componentNoun(className, count).takeIf {
        concrete(className) && this[className].standardResource == true
      }

  private fun cardResourceNoun(className: ClassName, count: Int): String? {
    val style = this[className].cardResource ?: return null
    if (!concrete(className)) {
      val noun = this[className].noun as? ComponentDescriber.Noun.Counted ?: return null
      return if (count == 1) noun.singular else noun.plural
    }
    val noun = unCamelCase(className.toString())
    return when (style) {
      ComponentDescriber.CardResource.ORDINARY -> noun + if (count == 1) "" else "s"
      ComponentDescriber.CardResource.SUFFIXED ->
          noun + if (count == 1) " resource" else " resources"
    }
  }

  private fun tagName(className: ClassName): Pair<String, Boolean>? {
    if (!concrete(className)) return null
    val style = this[className].tag ?: return null
    val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
    val isPlanetTag = style == ComponentDescriber.Tag.PLANET
    val name = if (isPlanetTag) ordinaryName.replaceFirstChar(Char::uppercaseChar) else ordinaryName
    return name to isPlanetTag
  }

  private fun standardResourceProduction(
      expression: Expression,
  ): Pair<List<Expression>, ClassName>? {
    if (
        expression.className != PRODUCTION || expression.refinement != null || expression.complement
    ) {
      return null
    }
    val resourceDependency = expression.arguments.lastOrNull() ?: return null
    if (
        resourceDependency.className != CLASS ||
            resourceDependency.arguments.size != 1 ||
            resourceDependency.refinement != null ||
            resourceDependency.complement
    ) {
      return null
    }
    val resource = resourceDependency.arguments.single()
    if (!resource.simple || standardResourceNoun(resource.className, 1) == null) return null
    return expression.arguments.dropLast(1) to resource.className
  }

  private fun representedClass(expression: Expression): Expression? {
    if (expression.arguments.size != 1) return null
    val classExpression = expression.arguments.single()
    if (
        classExpression.className != CLASS ||
            classExpression.arguments.size != 1 ||
            classExpression.refinement != null ||
            classExpression.complement
    ) {
      return null
    }
    return classExpression.arguments.single().takeIf { it.simple }
  }

  private fun concrete(className: ClassName): Boolean {
    val componentClass = classesByName[className] ?: return false
    return !componentClass.abstract
  }

  private fun indefiniteArticle(noun: String): String =
      if (noun.first().lowercaseChar() in "aeiou") "an" else "a"

  private fun unCamelCase(name: String): String = buildString {
    name.forEachIndexed { index, character ->
      val previous = name.getOrNull(index - 1)
      val next = name.getOrNull(index + 1)
      if (character == '_') {
        append(' ')
      } else {
        val startsWord =
            previous != null &&
                character.isUpperCase() &&
                (previous.isLowerCase() ||
                    previous.isDigit() ||
                    (previous.isUpperCase() && next?.isLowerCase() == true))
        if (startsWord) append(' ')
        append(character.lowercaseChar())
      }
    }
  }

  private data class ResourceProductionChange(
      val gaining: Boolean,
      val owner: String,
      val className: ClassName,
      val count: Int,
  )

  private data class TargetedResourceRemoval(
      val className: ClassName,
      val count: Int,
      val intensity: Instruction.Intensity?,
  )

  private val anyoneExpression = cn("Anyone").expression
  private val thisExpression = cn("This").expression
}
