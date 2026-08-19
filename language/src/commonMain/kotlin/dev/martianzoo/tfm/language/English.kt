package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
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
    if (
        card.extraClasses.any { it.className != card.resourceType } ||
            Describers[card.className].deriveBottomText == false
    ) {
      return null
    }
    val requirement = card.requirement?.let { renderRequirement(it) ?: return null }
    val instructions =
        card.immediate?.instructions?.let {
          derivedInstructions(it, card.resourceType) ?: return null
        }
    val scoring = card.effects.filter(::isEndEffect).map { renderEndEffect(it) ?: return null }
    return (listOfNotNull(requirement, instructions) + scoring)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" ")
  }

  private fun derivedInstructions(
      instructions: List<Instruction>,
      cardResourceType: ClassName?,
  ): String? {
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
                ?: derivedDirectGain(instructions[index])
                ?: derivedCardResourceGain(instructions[index], cardResourceType)
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
    standardResourceRemoval(instruction)?.let { (className, count) ->
      return "Remove $count ${componentNoun(className, count)}."
    }
    val removal = standardResourceRemovalFromAnyPlayer(instruction) ?: return null
    if (removal.intensity != OPTIONAL) return null
    return "Remove up to ${removal.count} ${componentNoun(removal.className, removal.count)} from any player."
  }

  private fun derivedDirectGain(instruction: Instruction): String? {
    val (className, count) = concreteMandatoryGain(instruction) ?: return null
    val gain = Describers[className].directGain ?: return null
    if (count != gain.count) return null
    return "Gain $count ${gain.noun}."
  }

  private fun derivedCardResourceGain(
      instruction: Instruction,
      cardResourceType: ClassName?,
  ): String? {
    val gain = instruction as? Gain ?: return null
    if (gain.intensity != null && gain.intensity != MANDATORY) return null
    val expression = gain.gaining
    if (expression.refinement != null || expression.complement) {
      return null
    }
    val count = (gain.count as? ActualScalar)?.value ?: return null
    val noun = cardResourceNoun(expression.className, count) ?: return null
    val target =
        when {
          expression.arguments == listOf(thisExpression) -> "this card"
          expression.arguments.isEmpty() ->
              if (cardResourceType == expression.className) "ANY card" else "ANOTHER card"
          else -> return null
        }
    return "Add $count $noun to $target."
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
    standardResourceRemovalFromAnyPlayer(instruction)?.let { removal ->
      if (removal.intensity != null && removal.intensity != MANDATORY) return null
      return ResourceProductionChange(false, "any player's", removal.className, removal.count)
    }
    return null
  }

  private fun derivedProductionClause(changes: List<ResourceProductionChange>): String {
    val verb = if (changes.first().gaining) "increase" else "decrease"
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
    val track = Describers[className].track ?: return null
    val steps = if (count == 1) "step" else "steps"
    val verb = if (gain != null) "Raise" else "Lower"
    return "$verb ${track.subject} $count $steps."
  }

  private fun derivedTilePlacement(instruction: Instruction): String? {
    val (className, count) = concreteMandatoryGain(instruction) ?: return null
    val placement = Describers[className].placement ?: return null
    if (count != 1 && !placement.allowsMultiple) return null
    val nounPhrase =
        if (count == 1) {
          "${placement.article} ${placement.singular}"
        } else {
          "$count ${placement.plural}"
        }
    val consequence = placement.consequence?.let { " ($it)" }.orEmpty()
    return "Place $nounPhrase$consequence."
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
  ): TargetedResourceRemoval? {
    val removal = instruction as? Remove ?: return null
    val expression = removal.removing
    if (expression.complement || expression.refinement != null) return null
    if (expression.arguments != listOf(anyoneExpression)) return null
    if (!isStandardResource(expression.className)) return null
    val count = (removal.count as? ActualScalar)?.value ?: return null
    return TargetedResourceRemoval(expression.className, count, removal.intensity)
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

  private val cardsByClassName: Map<ClassName, CardDefinition> by lazy {
    Canon.cardDefinitions.associateBy { it.className }
  }

  private val anyoneExpression = cn("Anyone").expression
  private val thisExpression = cn("This").expression

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
}
