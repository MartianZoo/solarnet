package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.types.Class

/** English component descriptions, inherited independently one fact at a time. */
internal object Describers {
  private operator fun get(className: ClassName): ComponentDescriber {
    val componentClass = Canon.classTable.findClass(className) ?: return ComponentDescriber()
    return effectiveDescribers.getValue(componentClass)
  }

  internal fun hasBehaviorBearingExtraClass(card: CardDefinition): Boolean =
      card.extraClasses.any { it.className != card.resourceType }

  internal fun renderCost(cost: Cost): String? {
    val spend = cost as? Cost.Spend ?: return null
    val expression = spend.scaledEx.expression
    if (!expression.simple) return null
    val count = (spend.scaledEx.scalar as? ActualScalar)?.value ?: return null
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

  internal fun renderChange(instruction: Instruction, card: CardDefinition?): String? =
      when (instruction) {
        is Gain ->
            renderDirectGain(instruction)
                ?: renderCardResourceGain(instruction, card)
                ?: renderTrackChange(instruction)
                ?: renderPlacement(instruction)
        is Remove -> renderStandardResourceRemoval(instruction) ?: renderTrackChange(instruction)
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
        return "$prefix $noun you have"
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
    return when (this[expression.className].requirement) {
      ComponentDescriber.Requirement.CITY_TILES_IN_PLAY -> {
        val tiles = "$target city ${if (target == 1) "tile" else "tiles"}"
        when {
          inPlay(expression) -> "Requires $tiles in play."
          expression.simple -> "Requires that you have $tiles."
          else -> null
        }
      }
      ComponentDescriber.Requirement.COLONIES -> {
        if (!expression.simple) return null
        "Requires $target ${if (target == 1) "colony" else "colonies"}."
      }
      ComponentDescriber.Requirement.GREENERY_TILES -> {
        if (!expression.simple) return null
        "Requires that you have $target greenery ${if (target == 1) "tile" else "tiles"}."
      }
      ComponentDescriber.Requirement.OCEAN_TILES -> {
        if (!expression.simple) return null
        "Requires $target ocean ${if (target == 1) "tile" else "tiles"}."
      }
      ComponentDescriber.Requirement.OXYGEN_PERCENT -> {
        if (!expression.simple) return null
        "Requires $target% oxygen."
      }
      ComponentDescriber.Requirement.TEMPERATURE -> {
        if (!expression.simple) return null
        "Requires ${temperature(target)} or warmer."
      }
      ComponentDescriber.Requirement.TERRAFORM_RATING -> {
        if (!expression.simple) return null
        "Requires that you have at least $target terraform rating."
      }
      ComponentDescriber.Requirement.VENUS_PERCENT -> {
        if (!expression.simple) return null
        "Requires Venus ${target * 2}%."
      }
      null ->
          renderProductionRequirement(requirement)
              ?: renderCardResourceRequirement(requirement)
              ?: renderTagRequirement(requirement)
    }
  }

  internal fun renderMaximum(requirement: Requirement.Max): String? {
    val metric = requirement.metric as? Metric.Count ?: return null
    val expression = metric.expression
    if (!expression.simple) return null
    val target = requirement.target
    return when (this[expression.className].requirement) {
      ComponentDescriber.Requirement.COLONIES ->
          "You must have no more than $target ${if (target == 1) "colony" else "colonies"}."
      ComponentDescriber.Requirement.OCEAN_TILES -> "There must be $target or fewer ocean tiles."
      ComponentDescriber.Requirement.OXYGEN_PERCENT -> "Oxygen must be $target% or less."
      ComponentDescriber.Requirement.TEMPERATURE ->
          "Temperature must be ${temperature(target)} or colder."
      ComponentDescriber.Requirement.VENUS_PERCENT -> "Venus must be ${target * 2}% or less."
      ComponentDescriber.Requirement.CITY_TILES_IN_PLAY,
      ComponentDescriber.Requirement.GREENERY_TILES,
      ComponentDescriber.Requirement.TERRAFORM_RATING,
      null -> null
    }
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

  private fun renderCardResourceGain(
      instruction: Instruction,
      card: CardDefinition?,
  ): String? {
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
    return "add $count $noun to $target"
  }

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
    val scope =
        when {
          expression.simple -> placement.unqualifiedMetricScope
          expression.arguments == listOf(anyoneExpression) -> placement.anyoneMetricScope
          else -> null
        } ?: return null
    val scopePhrase =
        when (scope) {
          ComponentDescriber.MetricScope.OWNED -> "you own"
          ComponentDescriber.MetricScope.IN_PLAY -> "in play"
        }
    return "${if (count == 1) placement.singular else placement.plural} $scopePhrase"
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
          when (this[metric.expression.className].requirement) {
            ComponentDescriber.Requirement.CITY_TILES_IN_PLAY ->
                "${minimum.target} city ${if (minimum.target == 1) "tile" else "tiles"}"
            ComponentDescriber.Requirement.COLONIES ->
                "${minimum.target} ${if (minimum.target == 1) "colony" else "colonies"}"
            else -> return null
          }
        }
    return "Requires that you have ${englishList(nouns)} in play."
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

  private fun inPlay(expression: Expression): Boolean =
      expression.arguments == listOf(anyoneExpression) &&
          expression.refinement == null &&
          !expression.complement

  private fun concrete(className: ClassName): Boolean {
    val componentClass = Canon.classTable.findClass(className) ?: return false
    return !componentClass.abstract
  }

  private fun temperature(steps: Int): String {
    val degreesCelsius = -30 + 2 * steps
    return "${if (degreesCelsius > 0) "+" else ""}${degreesCelsius}°C"
  }

  private fun indefiniteArticle(noun: String): String = if (noun.first() in "aeiou") "an" else "a"

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

  private val declarations: Map<Class, ComponentDescriber> by lazy {
    mapOf(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
            ),
        klass("StandardResource") to ComponentDescriber(standardResource = true),
        klass("Megacredit") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("M€")),
        klass("Plant") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Counted("plant", "plants")),
        klass("CardResource") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("resource", "resources"),
                cardResource = ComponentDescriber.CardResource.SUFFIXED,
            ),
        klass("CardFront") to ComponentDescriber(cardResourceHolder = "card"),
        klass("Animal") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Asteroid") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Floater") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Microbe") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Tag") to ComponentDescriber(tag = ComponentDescriber.Tag.ORDINARY),
        klass("PlanetTag") to ComponentDescriber(tag = ComponentDescriber.Tag.PLANET),
        klass("OxygenStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("oxygen"),
                requirement = ComponentDescriber.Requirement.OXYGEN_PERCENT,
            ),
        klass("TemperatureStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("temperature"),
                requirement = ComponentDescriber.Requirement.TEMPERATURE,
            ),
        klass("VenusStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("Venus"),
                requirement = ComponentDescriber.Requirement.VENUS_PERCENT,
            ),
        klass("TerraformRating") to
            ComponentDescriber(
                track = ComponentDescriber.Track("your terraform rating"),
                requirement = ComponentDescriber.Requirement.TERRAFORM_RATING,
            ),
        klass("OceanTile") to
            ComponentDescriber(
                placement = ComponentDescriber.Placement("an", "ocean tile", "ocean tiles"),
                requirement = ComponentDescriber.Requirement.OCEAN_TILES,
            ),
        klass("GreeneryTile") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        "a",
                        "greenery tile",
                        "greenery tiles",
                        consequence = "and raise oxygen 1 step",
                        allowsMultiple = false,
                    ),
                requirement = ComponentDescriber.Requirement.GREENERY_TILES,
            ),
        klass("CityTile") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        "a",
                        "city tile",
                        "city tiles",
                        anyoneMetricScope = ComponentDescriber.MetricScope.IN_PLAY,
                    ),
                requirement = ComponentDescriber.Requirement.CITY_TILES_IN_PLAY,
            ),
        klass("Colony") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        "a",
                        "colony",
                        "colonies",
                        unqualifiedMetricScope = ComponentDescriber.MetricScope.OWNED,
                        anyoneMetricScope = ComponentDescriber.MetricScope.IN_PLAY,
                    ),
                requirement = ComponentDescriber.Requirement.COLONIES,
            ),
        klass("ReserveTradeFleet") to
            ComponentDescriber(directGain = ComponentDescriber.DirectGain("Trade Fleet", 1)),
        klass("VictoryPoint") to ComponentDescriber(score = ComponentDescriber.Score("VP", "VPs")),
        klass("End") to ComponentDescriber(endTrigger = true),
    )
  }

  private val effectiveDescribers: Map<Class, ComponentDescriber> by lazy {
    Canon.classTable.allClasses().associateWith(::resolve)
  }

  private fun resolve(componentClass: Class): ComponentDescriber =
      ComponentDescriber(
          noun = resolveFact(componentClass, ComponentDescriber::noun),
          standardResource = resolveFact(componentClass, ComponentDescriber::standardResource),
          cardResource = resolveFact(componentClass, ComponentDescriber::cardResource),
          cardResourceHolder = resolveFact(componentClass, ComponentDescriber::cardResourceHolder),
          tag = resolveFact(componentClass, ComponentDescriber::tag),
          track = resolveFact(componentClass, ComponentDescriber::track),
          placement = resolveFact(componentClass, ComponentDescriber::placement),
          requirement = resolveFact(componentClass, ComponentDescriber::requirement),
          directGain = resolveFact(componentClass, ComponentDescriber::directGain),
          score = resolveFact(componentClass, ComponentDescriber::score),
          endTrigger = resolveFact(componentClass, ComponentDescriber::endTrigger),
      )

  private fun <T> resolveFact(
      componentClass: Class,
      fact: (ComponentDescriber) -> T?,
  ): T? {
    val providers =
        componentClass.allSuperclasses().mapNotNull { superclass ->
          declarations[superclass]?.let(fact)?.let { superclass to it }
        }
    val nearest = providers.filter { (provider) ->
      providers.none { (other) ->
        other !== provider && other.isSubtypeOf(provider)
      }
    }
    val values = nearest.map { (_, value) -> value }.distinct()
    check(values.size <= 1) {
      "${componentClass.className} inherits conflicting English component knowledge from " +
          nearest.joinToString { (provider) -> provider.className.toString() }
    }
    return values.singleOrNull()
  }

  private fun klass(name: String): Class = Canon.classTable.getClass(cn(name))
}
