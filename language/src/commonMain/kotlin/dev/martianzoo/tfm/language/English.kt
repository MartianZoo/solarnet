package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.PROD

/**
 * English card text, derived from canonical card definitions or read from the backing data file.
 */
public object English {
  /** Returns the text printed above the artwork on [cardFront]. */
  public fun topText(cardFront: ClassName): String {
    val card = cardsByClassName[cardFront] ?: return text(cardFront).top
    return topText(card) { text(cardFront).top }
  }

  /** Returns the text printed below the artwork on [cardFront]. */
  public fun bottomText(cardFront: ClassName): String {
    val card = cardsByClassName[cardFront] ?: return text(cardFront).bottom
    return bottomText(card) { text(cardFront).bottom }
  }

  internal fun topText(card: CardDefinition, fallback: () -> String): String =
      if (hasTopTextElement(card)) fallback() else ""

  internal fun bottomText(card: CardDefinition, fallback: () -> String): String =
      derivedBottomText(card) ?: if (hasBottomTextElement(card)) fallback() else ""

  private fun text(cardFront: ClassName): EnglishCardTextData.Text =
      EnglishCardTextData.byCardFront[cardFront] ?: error("No English text for $cardFront")

  // Immediate instructions are still treated as possible top elements. Of the card's Effects,
  // only End-triggered scoring is printed below the artwork.
  private fun hasTopTextElement(card: CardDefinition): Boolean =
      card.immediate != null || card.actions.isNotEmpty() || card.effects.isNotEmpty()

  private fun hasBottomTextElement(card: CardDefinition): Boolean =
      card.requirement != null || card.immediate != null || card.effects.any(::isEndEffect)

  private fun derivedBottomText(card: CardDefinition): String? {
    if (card.extraClasses.isNotEmpty()) return null
    val requirement = card.requirement?.let { renderRequirement(it) ?: return null }
    val instructions = card.immediate?.instructions?.let { derivedInstructions(it) ?: return null }
    val scoring = card.effects.filter(::isEndEffect).map { renderEndEffect(it) ?: return null }
    return (listOfNotNull(requirement, instructions) + scoring)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" ")
  }

  private fun derivedInstructions(instructions: List<Instruction>): String? {
    if (instructions.isEmpty()) return null
    val sentences = mutableListOf<String>()
    var index = 0
    while (index < instructions.size) {
      if (standardResourceGain(instructions[index]) != null) {
        val gains = instructions.drop(index).takeWhile { standardResourceGain(it) != null }
        sentences += derivedStandardResourceGains(gains)
        index += gains.size
      } else {
        sentences +=
            derivedStandardResourceRemoval(instructions[index])
                ?: derivedTradeFleetGain(instructions[index])
                ?: derivedProductionChange(instructions[index])
                ?: derivedTrackChange(instructions[index])
                ?: derivedTilePlacement(instructions[index])
                ?: return null
        index++
      }
    }
    return sentences.joinToString(" ")
  }

  private fun derivedStandardResourceGains(instructions: List<Instruction>): String {
    val objects = instructions.map { instruction ->
      val (className, count) = checkNotNull(standardResourceGain(instruction))
      "$count ${componentNoun(className, count)}"
    }
    return "Gain ${englishList(objects)}."
  }

  private fun derivedStandardResourceRemoval(instruction: Instruction): String? {
    val (className, count) = standardResourceRemoval(instruction) ?: return null
    return "Remove $count ${componentNoun(className, count)}."
  }

  private fun derivedTradeFleetGain(instruction: Instruction): String? {
    val (className, count) = concreteMandatoryGain(instruction) ?: return null
    if (className != reserveTradeFleet || count != 1) return null
    return "Gain 1 Trade Fleet."
  }

  private fun derivedProductionChange(instruction: Instruction): String? {
    val transform = instruction as? Transform ?: return null
    if (transform.transformKind != PROD) return null
    val instructions = InstructionGroup.of(transform.instruction).instructions
    val changes = instructions.map { productionChange(it) ?: return null }
    val clauses = mutableListOf<String>()
    var index = 0
    while (index < changes.size) {
      val run = changes.drop(index).takeWhile { it.gaining == changes[index].gaining }
      clauses += derivedProductionClause(run)
      index += run.size
    }
    return clauses.joinToString(" and ").replaceFirstChar(Char::uppercaseChar) + "."
  }

  private fun productionChange(instruction: Instruction): ResourceProductionChange? {
    standardResourceGain(instruction)?.let { (className, count) ->
      return ResourceProductionChange(true, "your", className, count)
    }
    standardResourceRemoval(instruction)?.let { (className, count) ->
      return ResourceProductionChange(false, "your", className, count)
    }
    standardResourceRemovalFromAnyPlayer(instruction)?.let { (className, count) ->
      return ResourceProductionChange(false, "any player's", className, count)
    }
    return null
  }

  private fun derivedProductionClause(changes: List<ResourceProductionChange>): String {
    val verb = if (changes.first().gaining) "increase" else "decrease"
    val sharedCount = changes.map { it.count }.distinct().singleOrNull()
    if (sharedCount != null) {
      val steps = if (sharedCount == 1) "step" else "steps"
      val productions = changes.map {
        "${it.owner} ${componentNoun(it.className, 1)} production"
      }
      return if (productions.size == 1) {
        "$verb ${productions.single()} $sharedCount $steps"
      } else {
        "$verb ${englishList(productions)} $sharedCount $steps each"
      }
    }
    val productions = changes.map {
      val steps = if (it.count == 1) "step" else "steps"
      "${it.owner} ${componentNoun(it.className, 1)} production ${it.count} $steps"
    }
    return "$verb ${englishList(productions)}"
  }

  private fun standardResourceGain(instruction: Instruction): Pair<ClassName, Int>? {
    val gain = concreteMandatoryGain(instruction) ?: return null
    return gain.takeIf { (className) -> isStandardResource(className) }
  }

  private fun derivedTrackChange(instruction: Instruction): String? {
    val gain = concreteMandatoryGain(instruction)
    val removal = concreteMandatoryRemoval(instruction)
    val (className, count) = gain ?: removal ?: return null
    val subject =
        when (className) {
          oxygenStep -> "oxygen"
          temperatureStep -> "temperature"
          terraformRating -> "your TR"
          venusStep -> "Venus"
          else -> return null
        }
    val steps = if (count == 1) "step" else "steps"
    val verb = if (gain != null) "Raise" else "Lower"
    return "$verb $subject $count $steps."
  }

  private fun derivedTilePlacement(instruction: Instruction): String? {
    val (className, count) = concreteMandatoryGain(instruction) ?: return null
    if (className == greeneryTile && count == 1) {
      return "Place a greenery tile (and raise oxygen 1 step)."
    }
    val (article, singular) =
        when (className) {
          cityTile -> "a" to "city tile"
          colony -> "a" to "colony"
          oceanTile -> "an" to "ocean tile"
          else -> return null
        }
    val nounPhrase =
        if (count == 1) {
          "$article $singular"
        } else {
          "$count ${if (className == colony) "colonies" else "${singular}s"}"
        }
    return "Place $nounPhrase."
  }

  private fun concreteMandatoryGain(instruction: Instruction): Pair<ClassName, Int>? {
    val gain = instruction as? Gain ?: return null
    if (gain.intensity != null && gain.intensity != MANDATORY) return null
    if (!gain.gaining.simple) return null
    val count = (gain.count as? ActualScalar)?.value ?: return null
    return gain.gaining.className to count
  }

  private fun standardResourceRemoval(instruction: Instruction): Pair<ClassName, Int>? {
    val removal = concreteMandatoryRemoval(instruction) ?: return null
    return removal.takeIf { (className) -> isStandardResource(className) }
  }

  private fun standardResourceRemovalFromAnyPlayer(
      instruction: Instruction
  ): Pair<ClassName, Int>? {
    val removal = instruction as? Remove ?: return null
    if (removal.intensity != null && removal.intensity != MANDATORY) return null
    val expression = removal.removing
    if (expression.complement || expression.refinement != null) return null
    if (expression.arguments != listOf(anyoneExpression)) return null
    if (!isStandardResource(expression.className)) return null
    val count = (removal.count as? ActualScalar)?.value ?: return null
    return expression.className to count
  }

  private fun concreteMandatoryRemoval(instruction: Instruction): Pair<ClassName, Int>? {
    val removal = instruction as? Remove ?: return null
    if (removal.intensity != null && removal.intensity != MANDATORY) return null
    if (!removal.removing.simple) return null
    val count = (removal.count as? ActualScalar)?.value ?: return null
    return removal.removing.className to count
  }

  private fun englishList(parts: List<String>): String =
      when (parts.size) {
        1 -> parts.single()
        2 -> parts.joinToString(" and ")
        else -> parts.dropLast(1).joinToString(", ") + ", and " + parts.last()
      }

  private fun isEndEffect(effect: Effect): Boolean {
    val cardTrigger = effect.trigger
    return cardTrigger is OnGainOf && cardTrigger.expression == endExpression
  }

  private val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    Canon.cardDefinitions.associateBy { it.className }
  }

  private val endExpression = cn("End").expression
  private val anyoneExpression = cn("Anyone").expression
  private val cityTile = cn("CityTile")
  private val colony = cn("Colony")
  private val greeneryTile = cn("GreeneryTile")
  private val oceanTile = cn("OceanTile")
  private val oxygenStep = cn("OxygenStep")
  private val reserveTradeFleet = cn("ReserveTradeFleet")
  private val temperatureStep = cn("TemperatureStep")
  private val terraformRating = cn("TerraformRating")
  private val venusStep = cn("VenusStep")

  private data class ResourceProductionChange(
      val gaining: Boolean,
      val owner: String,
      val className: ClassName,
      val count: Int,
  )
}
